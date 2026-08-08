---
name: new-module
description: Add a new Gradle module to the Appotato project — a feature screen/flow under features/, or a cross-cutting capability under shared/. Use whenever the user asks to create, add, or scaffold a module, feature, or screen (e.g. "add a pantry feature", "new shared module for X", "scaffold the settings screen").
---

# Adding a module

Do **not** hand-write the module files. Run the generator, then fill in the real code.
The generator owns the mechanical part; this skill owns the decisions.

```bash
./scripts/new-module.sh <gradle-path> [--api] [--impl] [--fake] [--flat] [--mvi] [--kover [N]] [--dry-run]
```

It creates the directories and the package path, writes stub `build.gradle.kts` files with the
right convention-plugin ids, appends the `settings.gradle.kts` includes, and (with `--mvi`)
scaffolds the MVI file set. It refuses to overwrite existing files, so it is safe to re-run
after a failure. Use `--dry-run` first if the shape is uncertain.

## Decide the flags before running

**Where does it live?**
- A screen or a user-facing flow → `features:<name>`.
- A capability many features consume (no feature logic) → `shared:<name>`.

**Which submodules?**

| Situation | Flags |
|---|---|
| Feature screen, nothing else depends on it | `--impl --mvi` |
| Feature other code must talk to | `--api --impl --mvi` |
| Feature whose contract needs a test double | `--api --impl --fake --mvi` |
| Shared capability with a contract | `--api --impl` |
| Small shared capability, no api/impl split | `--flat` |

`shared:dispatchers` and `shared:ui-components` are the `--flat` precedents; `shared:serialization`
is the `--api --impl --fake` precedent.

**Coverage?** Add `--kover` only when the module will have meaningful tests. Pass a number
(`--kover 80`) to write the gates immediately; without a number the plugin applies with a 0
threshold you can raise later. Don't set a gate you can't meet — `koverVerify` runs in `build`.

## After the generator runs

1. **Declare the contract** in the `api` module if you created one — `public` interfaces and models
   only. A type used in a public signature must come from an `api(...)` dependency, not
   `implementation(...)`, or consumers get `Unresolved reference`.
2. **Fill in `<Name>ViewModel.onIntent`** — the generated body is `TODO(...)` and will throw on the
   first intent. Replace it.
3. **Replace the `<Name>Screen` placeholder** — the generated `Column` is empty scaffolding. Build the
   real UI from `shared/ui-components` wrappers, not raw material3.
4. **Add the intents and effects** the screen actually needs. The generator emits one starter member
   each (`ScreenShown`, `NavigateBack`); keep or delete them.
5. **Inject `CoroutineDispatchers`** from `shared/dispatchers` for any suspending work — never
   reference `Dispatchers.X` directly, it breaks `runTest`.
6. **Wire it into its consumer** (usually `composeApp`) via the typesafe accessor the generator prints.
   The generator does not do this — nothing consumes a new module until you add the dependency.
7. **Verify**: `./gradlew :<path>:build` runs compile, detekt, and koverVerify together.

## Conventions the generator already encodes — don't re-derive them

- The Kotlin package equals the namespace `AndroidLibraryPlugin` derives from the Gradle path:
  `:features:add-item:implementation` → `com.appotato.features.add.item.implementation`
  (both `:` and `-` become `.`). Files must sit in the matching directory.
- Typesafe accessors camel-case kebab segments: `:shared:ui-components` → `projects.shared.uiComponents`.
- **No `AndroidManifest.xml`.** AGP generates one from `namespace`. Add the file only if the module
  needs real manifest entries (a permission, provider, or activity).
- Every module gets `id("detekt.library")`.
- `implementation` modules are `internal`; `api` and `fake` are explicitly `public`.

## Detekt rules that bite generated code

- `ForbiddenComment` bans the literal `TODO:` **in comments**. The `TODO("...")` function call is fine.
- `EmptyFunctionBlock` is active — no empty `{}` bodies, comments don't count as content.
- `FunctionNaming` requires camelCase but has `ignoreAnnotated: ['Composable']`, so `LoginScreen` is fine.
- Max line length 120.

## Removing or moving a module

Delete the directory, delete its `include(...)` lines from `settings.gradle.kts`, and drop any
`projects.*` references to it. To move one, `git mv` the directory and rewrite the include — the
namespace and framework name follow the Gradle path automatically, so nothing else needs editing.
