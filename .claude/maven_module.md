# Reusable prompt: Maven multi-module "modular monolith" organization

Paste this into a new project's CLAUDE.md (or hand it to an agent doing initial
scaffolding) to set up the same module-boundary convention used in this repo.
Replace `<root-package>`, `<groupId>`, and the module list with the new
project's values.

---

## Prompt

We're building a **modular monolith**: a single deployable process, built as
a multi-module Maven reactor, where each business capability is its own
module with an enforced internal/api split. Set it up as follows.

### Reactor shape

- Root `pom.xml` is `packaging: pom` — the aggregator and parent. It holds
  `<dependencyManagement>` for every third-party version and for the
  inter-module artifact versions (`${project.version}`), plus shared
  `<properties>` (language version, framework version). No business code
  lives at the root.
- Every business capability gets its own top-level module directory with its
  own `pom.xml` (`parent` pointing at the root, `relativePath: ../pom.xml`).
  Each module owns its own `src/main`, `src/test`, its own DB
  schema/connection pool/migrations, and its own health indicator — never
  shared across modules.
- One assembly module (name it `app`) depends on every business module,
  contains the `main` class and environment-level config, and has **no
  business logic of its own**. It is the only module with every other
  module's compiled classes on one classpath, which matters for the next
  point.

### The api/internal split (this is the part that must be enforced, not just documented)

Inside each module's base package (e.g. `<root-package>.<module>`), split
code into two subpackages:

- `<root-package>.<module>.api` — the only surface other modules may import.
  Interfaces, DTOs, ports meant for cross-module use.
- `<root-package>.<module>.internal` — everything else. Implementation
  details, entities, repositories. **Nothing outside the module's own
  package may import from here.**

Enforce this with an ArchUnit test that lives in the `app` module (since it's
the only place with every module on one classpath):

```kotlin
@AnalyzeClasses(packages = ["<root-package>"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ModuleBoundaryArchTest {
    @ArchTest
    fun internal_packages_are_only_accessed_from_within_their_own_module(classes: JavaClasses) {
        val modules = classes.asSequence()
            .map { it.packageName }
            .filter { it.startsWith("<root-package>.") }
            .mapNotNull { pkg -> pkg.removePrefix("<root-package>.").substringBefore('.').takeIf(String::isNotBlank) }
            .toSet()

        for (module in modules) {
            noClasses()
                .that().resideOutsideOfPackage("<root-package>.$module..")
                .should().dependOnClassesThat().resideInAPackage("<root-package>.$module.internal..")
                .because("<root-package>.$module.internal is private to the $module module")
                .check(classes)
        }
    }
}
```

This derives the module list from whatever packages are actually on the
classpath, so adding a new module needs zero edits to the test itself.

### Inter-module dependencies

- Declare each module's artifact once in the root `dependencyManagement`
  (versioned with `${project.version}`, i.e. released together with the
  reactor — not independently versioned).
- A module that needs another module's functionality depends on its `api`
  package only, never its `internal`. If a module needs no cross-module
  calls yet, don't add a dependency preemptively.
- Keep a stub module (empty `pom.xml`, no sources) if you know a capability
  is coming later — this avoids a build-structure change when that work
  starts. Only do this when there's a concrete near-term plan for it, not
  speculatively.

### Verification

Add a single command that runs the full reactor build, all module tests, and
the ArchUnit boundary check together (e.g. `./mvnw verify`). Treat this as
the gate before considering any cross-module change done — a module-boundary
violation is a build failure, not a code-review comment.

### Module list for this project

`<module-1>`, `<module-2>`, ..., `app`

---

## Origin

Extracted from `moveon`'s actual structure: root `pom.xml` (aggregator),
`auth` / `notification` / `payments` / `trip` / ... business modules each
with `internal` + (where cross-module use exists) `api` packages, and `app`
as the assembly module owning `app/src/test/kotlin/.../ModuleBoundaryArchTest.kt`.