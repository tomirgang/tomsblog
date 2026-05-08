#!/usr/bin/env bash
set -euo pipefail

# Release script for Toms Blog
# Bumps version, updates CHANGELOG, builds, tags, and pushes.
# Usage: ./scripts/release.sh [--rebuild]
#   --rebuild  Re-run build/tests without version bump (use after fixing test failures)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

die() { echo -e "${RED}ERROR: $1${NC}" >&2; exit 1; }
info() { echo -e "${GREEN}$1${NC}"; }
warn() { echo -e "${YELLOW}$1${NC}"; }

# Ensure we're in the project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

REBUILD=false
if [[ "${1:-}" == "--rebuild" ]]; then
    REBUILD=true
fi

# Ensure clean working directory
if [[ -n "$(git status --porcelain)" ]]; then
    die "Working directory is not clean. Commit or stash changes first."
fi

# Ensure we're on main branch
BRANCH="$(git branch --show-current)"
if [[ "$BRANCH" != "main" && "$BRANCH" != "master" ]]; then
    warn "WARNING: You are on branch '$BRANCH', not main/master."
    read -rp "Continue anyway? [y/N] " confirm
    [[ "$confirm" =~ ^[Yy]$ ]] || exit 0
fi

# Sync with both remotes to ensure we have the latest refs
git fetch origin || die "Failed to fetch from origin."
git fetch github || die "Failed to fetch from github."

# Rebase local branch on top of github to avoid merge commits
git pull --rebase github "$BRANCH" || die "Failed to rebase on github/$BRANCH. Resolve conflicts and try again."

if [[ "$REBUILD" == true ]]; then
    # Rebuild mode: just run tests without version changes
    info "Starting local infrastructure..."
    docker compose -f infra/docker/docker-compose.yml up -d --wait || die "Failed to start local infrastructure."
    info "Rebuild mode: running clean build and verification..."
    ./mvnw clean verify --batch-mode --no-transfer-progress
    info "Build and verification successful."
    info "Running E2E tests (Selenium)..."
    ./mvnw verify -Pe2e --batch-mode --no-transfer-progress || die "E2E tests failed."
    info "E2E tests passed."
    exit 0
fi

# Read current version from parent pom.xml
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout -N 2>/dev/null)
echo ""
info "Current version: $CURRENT_VERSION"

# Strip -SNAPSHOT suffix for parsing
BASE_VERSION="${CURRENT_VERSION%-SNAPSHOT}"
IFS='.' read -r MAJOR MINOR PATCH <<< "$BASE_VERSION"

if [[ -z "$MAJOR" || -z "$MINOR" || -z "$PATCH" ]]; then
    die "Could not parse version '$CURRENT_VERSION'. Expected format: MAJOR.MINOR.PATCH[-SNAPSHOT]"
fi

# Ask user for bump type
echo ""
echo "Version bump options:"
echo "  1) major  -> $((MAJOR + 1)).0.0"
echo "  2) minor  -> $MAJOR.$((MINOR + 1)).0"
echo "  3) patch  -> $MAJOR.$MINOR.$((PATCH + 1))"
echo ""
read -rp "Select bump type [1/2/3]: " BUMP_TYPE

case "$BUMP_TYPE" in
    1|major)
        NEW_VERSION="$((MAJOR + 1)).0.0"
        ;;
    2|minor)
        NEW_VERSION="$MAJOR.$((MINOR + 1)).0"
        ;;
    3|patch)
        NEW_VERSION="$MAJOR.$MINOR.$((PATCH + 1))"
        ;;
    *)
        die "Invalid selection: '$BUMP_TYPE'"
        ;;
esac

info "New version: $NEW_VERSION"
echo ""
read -rp "Proceed with release $NEW_VERSION? [y/N] " confirm
[[ "$confirm" =~ ^[Yy]$ ]] || exit 0

# Start local infrastructure (PostgreSQL, Redis, Kafka, RabbitMQ)
info "Starting local infrastructure..."
docker compose -f infra/docker/docker-compose.yml up -d --wait || die "Failed to start local infrastructure."
info "Local infrastructure is ready."

# Clean build with full verification BEFORE any file modifications
# This ensures failed builds don't leave the working directory dirty
info "Running clean build and verification with current SNAPSHOT version..."
./mvnw clean verify --batch-mode --no-transfer-progress
info "Build and verification successful."

# Run E2E tests (Selenium browser tests) as mandatory release gate
info "Running E2E tests (Selenium)..."
./mvnw verify -Pe2e --batch-mode --no-transfer-progress || die "E2E tests failed. Fix failures before releasing."
info "E2E tests passed."

# Update version in all pom.xml files
info "Updating Maven version to $NEW_VERSION..."
./mvnw versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false --batch-mode --no-transfer-progress -q

# Update CHANGELOG.md: replace [Unreleased] header with version and date
RELEASE_DATE="$(date +%Y-%m-%d)"
info "Updating CHANGELOG.md..."

if grep -q '## \[Unreleased\]' CHANGELOG.md; then
    sed -i "s/## \[Unreleased\]/## [$NEW_VERSION] - $RELEASE_DATE/" CHANGELOG.md
    # Add new Unreleased section at the top
    sed -i "/^## \[$NEW_VERSION\]/i ## [Unreleased]\n" CHANGELOG.md
else
    die "Could not find '## [Unreleased]' section in CHANGELOG.md"
fi

# Update Antora documentation versions (remove prerelease marker for release)
info "Updating Antora documentation versions..."
for antora_file in docs/arc42/antora.yml docs/design/antora.yml; do
    if [[ -f "$antora_file" ]]; then
        sed -i '/^prerelease: -dev$/d' "$antora_file"
    fi
done

# Update version in arc42 index page
ARC42_INDEX="docs/arc42/modules/ROOT/pages/index.adoc"
if [[ -f "$ARC42_INDEX" ]]; then
    sed -i "s/| Version | .*/| Version | $NEW_VERSION/" "$ARC42_INDEX"
fi

# Commit the release
info "Committing release..."
git add -A
git commit -m "release: v$NEW_VERSION"

# Create annotated tag
info "Creating tag v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Release $NEW_VERSION"

# Rebase on github to incorporate any upstream changes (e.g. from Flux/CI)
info "Rebasing on github/$BRANCH before push..."
git pull --rebase github "$BRANCH" || die "Failed to rebase on github/$BRANCH. Resolve conflicts and try again."

# Push to both remotes (atomic ensures branch + tag arrive together)
info "Pushing to origin (force)..."
git push --force --atomic origin "$BRANCH" "v$NEW_VERSION"

info "Pushing to github..."
git push --atomic github "$BRANCH" "v$NEW_VERSION"

# Set version back to SNAPSHOT for development
NEXT_SNAPSHOT="$NEW_VERSION-SNAPSHOT"
info "Setting development version to $NEXT_SNAPSHOT..."
./mvnw versions:set -DnewVersion="$NEXT_SNAPSHOT" -DgenerateBackupPoms=false --batch-mode --no-transfer-progress -q

# Restore Antora prerelease marker for development
for antora_file in docs/arc42/antora.yml docs/design/antora.yml; do
    if [[ -f "$antora_file" ]]; then
        sed -i '/^version:/a prerelease: -dev' "$antora_file"
    fi
done

# Update version in arc42 index page to SNAPSHOT
if [[ -f "$ARC42_INDEX" ]]; then
    sed -i "s/| Version | .*/| Version | $NEXT_SNAPSHOT/" "$ARC42_INDEX"
fi

git add -A
git commit -m "chore: set development version $NEXT_SNAPSHOT"

# Rebase again in case CI/Flux pushed between release push and now
git pull --rebase github "$BRANCH" || die "Failed to rebase SNAPSHOT commit. Resolve conflicts and try again."

info "Pushing development version to origin (force)..."
git push --force origin "$BRANCH"

info "Pushing development version to github..."
git push github "$BRANCH"

echo ""
info "Release v$NEW_VERSION complete!"
info "  Tag: v$NEW_VERSION"
info "  Pushed to: origin, github"
