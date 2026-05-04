#!/bin/bash
# Converts ADR Markdown files to HTML with a navigation bar.
# Uses pandoc for Markdown rendering (syntax highlighting, GFM tables, etc.)
# and a Lua filter for Mermaid / PlantUML diagram support.
#
# Prerequisites: pandoc >= 3.x
# Usage: ./render-adrs.sh <source-dir> <output-dir>

set -euo pipefail

SRC="${1:-docs/adr}"
OUT="${2:-build/site/adrs}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEMPLATE="$SCRIPT_DIR/pandoc/adr-template.html"
LUA_FILTER="$SCRIPT_DIR/pandoc/diagram-filter.lua"

mkdir -p "$OUT"

NAV_BAR='<div style="background:#343a40;padding:8px 16px;font-size:14px;font-family:sans-serif"><a href="../index.html" style="color:#fff;text-decoration:none;margin-right:16px">&larr; Dokumentation</a><a href="../tomsblog-arc42/main/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Architektur (arc42)</a><a href="../tomsblog-design/main/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Software Detail Design</a><a href="../requirements/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Requirements</a><a href="index.html" style="color:#fff;text-decoration:none">ADRs</a></div>'

# ---------------------------------------------------------------------------
# Index page
# ---------------------------------------------------------------------------
cat > "$OUT/index.html" << 'HEADER'
<!DOCTYPE html>
<html>
<head>
  <title>Architecture Decision Records</title>
  <meta charset="utf-8" />
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; padding: 0; }
    .content { max-width: 900px; margin: 0 auto; padding: 24px; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
    th { background: #f5f5f5; }
    a { color: #0366d6; text-decoration: none; }
    a:hover { text-decoration: underline; }
  </style>
</head>
<body>
HEADER

echo "$NAV_BAR" >> "$OUT/index.html"

cat >> "$OUT/index.html" << 'MID'
<div class="content">
<h1>Architecture Decision Records</h1>
<table>
<tr><th>ADR</th><th>Titel</th><th>Status</th></tr>
MID

# ---------------------------------------------------------------------------
# Render each ADR
# ---------------------------------------------------------------------------
for md in "$SRC"/*.md; do
  [ -f "$md" ] || continue
  filename=$(basename "$md" .md)
  title=$(grep -m1 "^# " "$md" | sed 's/^# //')
  status=$(grep -A2 "^## Status" "$md" | tail -1 | tr -d '[:space:]')
  adr_num=$(echo "$filename" | grep -oP '^\d+')

  echo "<tr><td><a href=\"${filename}.html\">${adr_num}</a></td><td>${title}</td><td>${status}</td></tr>" >> "$OUT/index.html"

  pandoc "$md" \
    --from gfm \
    --to html5 \
    --template "$TEMPLATE" \
    --lua-filter "$LUA_FILTER" \
    --highlight-style tango \
    --metadata title="$title" \
    --variable nav-bar="$NAV_BAR" \
    --standalone \
    --output "$OUT/${filename}.html"
done

cat >> "$OUT/index.html" << 'FOOTER'
</table>
</div>
</body>
</html>
FOOTER

echo "Rendered $(ls "$OUT"/*.html | wc -l) ADR pages to $OUT/"
