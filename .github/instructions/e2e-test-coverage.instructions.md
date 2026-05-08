---
description: "Use when creating or modifying Thymeleaf templates, web controllers, or any user-facing views. Enforces that all views must have corresponding E2E tests."
applyTo: ["**/templates/**", "**/adapter/inbound/web/**", "e2e-tests/**"]
---
# E2E Test Coverage for Views

## Rule

Every Thymeleaf view (template) and its corresponding web controller MUST be fully covered by E2E tests in `e2e-tests/`.

## When This Applies

This instruction applies whenever:
- A new Thymeleaf template is created under `**/templates/**`
- An existing template is modified with new UI elements (forms, buttons, lists, navigation)
- A new web controller (`@Controller`) is created or extended with new endpoints
- New model attributes are added that affect the rendered view

## Required E2E Artifacts

### 1. Page Object (mandatory for each view)

Every distinct view must have a corresponding Page Object in `e2e-tests/src/test/java/de/tomsblog/e2e/page/`.

Structure:
- Blog views → `page/blog/` (e.g., `PostListPage`, `PostDetailPage`, `PostFormPage`)
- Admin views → `page/admin/` (e.g., `TagAdminPage`, `UserManagementPage`)
- Auth views → `page/auth/` (e.g., `LoginPage`, `RegisterPage`)

Page Object rules:
- Extend `BasePage`
- Provide `open()` method navigating to the view URL
- Expose methods for every interactive element (form fields, buttons, links)
- Expose query methods for every dynamic content area (lists, labels, tables)
- Return other Page Objects for cross-page navigation
- Use `@req SWR-xxx` Javadoc referencing the corresponding requirement

### 2. E2E Test Class (mandatory)

Every view workflow must be covered in a `*E2ETest.java` class under `e2e-tests/src/test/java/de/tomsblog/e2e/test/`.

Test coverage must include:
- **Authentication**: Verify unauthenticated access is redirected (for protected views)
- **Page Load**: Verify the view loads correctly for authorized users
- **Content Display**: Verify dynamic content (lists, labels, model attributes) renders correctly
- **Form Submission**: Verify forms can be submitted with valid data and produce the expected result
- **Validation**: Verify form validation errors are shown for invalid input (where applicable)
- **Navigation**: Verify links and navigation between views work correctly
- **CRUD Workflows**: For admin views, test create, read, update, and delete operations end-to-end

Test rules:
- Use `@DisplayName("SWR-xxx: ...")` for requirement tracing
- Follow the existing pattern: `@SpringBootTest` + Testcontainers (PostgreSQL + Chrome)
- Use HTTP Basic Auth via URL for authenticated access (e.g., `http://user:pass@host:port/path`)

### 3. Extending Existing Page Objects

When new UI elements are added to an existing view:
- Add corresponding methods to the existing Page Object
- Add new test methods to the existing or a new E2E test class
- Mark new methods with `@req SWR-xxx` Javadoc

## Completion Checklist

Before considering a view change complete, verify:

- [ ] Page Object exists for the view with methods for all interactive/dynamic elements
- [ ] E2E test class exists with tests for auth, load, content, forms, and navigation
- [ ] All test methods have `@DisplayName("SWR-xxx: ...")` requirement tracing
- [ ] E2E tests compile: `./mvnw compile test-compile -Pe2e -pl e2e-tests -am`
- [ ] Main build still passes: `./mvnw spotless:apply verify`

## Build Commands

```bash
# Compile E2E tests (quick check)
./mvnw compile test-compile -Pe2e -pl e2e-tests -am

# Run E2E tests (requires Docker for Testcontainers)
./mvnw verify -Pe2e
```

## Module Structure Reference

```
e2e-tests/src/test/java/de/tomsblog/e2e/
├── config/           # WebDriverProvider, ScreenshotOnFailureExtension
├── page/             # Page Objects
│   ├── BasePage.java
│   ├── admin/        # Admin view Page Objects
│   ├── auth/         # Auth view Page Objects
│   └── blog/         # Blog view Page Objects
└── test/             # E2E Test Classes (*E2ETest.java)
```
