# Appotato — AI context

Kotlin Multiplatform (Android + iOS) app for tracking food expiration dates. Compose Multiplatform UI.
Root package: `com.appotato`. Gradle 8.13, Kotlin 2.2.10, JDK/JVM target 21, minSdk 26 / compileSdk 36.

## Module map

```
composeApp/                 app entry (Android app + iOS framework "ComposeApp"), App.kt, MainActivity, MainViewController
iosApp/                     Xcode project wrapper
build-logic/                included build — convention plugins (see below)
config/detekt/detekt.yml    single detekt ruleset for all modules
scripts/new-module.sh       module generator — see "Adding a new module"
shared/
  ui-components/            design system: AppotatoTheme, Buttons, TextFields, Images (Coil)
  dispatchers/              CoroutineDispatchers (expect/actual per platform)
  serialization/{api,implementation,fake}
  storage/{api,implementation}          KeyValueStorage — impl is TODO stub
features/                   one submodule per screen or per flow
  login/{api,implementation}            no sources yet, module shell only
```

Every module is registered in `settings.gradle.kts`. Typesafe project accessors are on:
depend as `implementation(projects.shared.serialization.api)`, `projects.shared.uiComponents`,
`projects.features.login.implementation`.

## Layering rules

- `shared/*` = cross-cutting capabilities consumed by many features. No feature logic there.
- `features/*` = screen/flow specific. Features may depend on `shared/*`; **never** feature → feature
  implementation (go through the other feature's `api` module if truly needed).
- `api` submodule: public contracts only (interfaces, models). Marked `public`, no implementation detail.
  A type used in a public signature must come from a dependency declared with `api(...)`, not
  `implementation(...)` — otherwise consumers get `Unresolved reference` on that type.
- `implementation` submodule: `internal` classes only. Nothing outside consumes them directly.
- `fake` submodule: test doubles implementing the `api` contract, published for other modules' tests.

## Convention plugins (build-logic/src/main/kotlin)

| id | target set | use for |
|---|---|---|
| `android.library` | KMP + androidTarget + ios{X64,Arm64,SimulatorArm64} + Compose + serialization | anything with UI or platform code (`implementation`, `ui-components`, feature modules) |
| `api.library` | KMP: ios triple + jvm, no Compose | `api` modules |
| `fake.library` | same as api.library | `fake` modules |
| `detekt.library` | detekt + formatting, config from root | **every** module |
| `kover.library` | Kover reports + coverage verification | modules with meaningful tests |

`android.library` derives both names from the **Gradle path**, so sibling `api`/`implementation` modules
never collide: `:shared:ui-components` → namespace `com.appotato.shared.ui.components`, framework
`SharedUiComponentsKit`. Keep the Kotlin package equal to the namespace.
Per-module coverage gates: call `setKoverMinLineCoverage(n)` / `setKoverMinInstructionCoverage(n)`
at the top level of the module's `build.gradle.kts` (see `shared/serialization/implementation`, set to 100).

## Feature module architecture (MVI)

Each feature module owns one screen or one flow. Inside `implementation`:

```
com/appotato/features/<name>/
  <Name>Screen.kt        @Composable, stateless — takes State, emits Intent
  <Name>ViewModel.kt     androidx.lifecycle.ViewModel (KMP artifact)
  <Name>State.kt         sealed interface or @Immutable data class
  <Name>Intent.kt        sealed interface — user actions
  <Name>Effect.kt        sealed interface — one-shot events (navigation, snackbar)
  domain/ data/          use cases and sources, all `internal`
```

Contract:
- `StateFlow<State>` exposed from the ViewModel, collected in Compose via `collectAsStateWithLifecycle()`.
- `Channel`/`SharedFlow<Effect>` for one-shot events — never model navigation as state.
- Single `fun onIntent(intent: Intent)` entry point; ViewModel reduces Intent + result → new State.
- All suspending work goes through injected `CoroutineDispatchers` (`shared/dispatchers`) — never
  reference `Dispatchers.X` directly, it breaks `runTest`.
- Composables in features must build on `shared/ui-components` wrappers, not raw material3.

## Code conventions

- `api`/`fake` declarations are explicitly `public`; `implementation` classes are `internal`.
- Suspend functions that can fail return `Result<T>`; catch the specific exception, not `Exception`.
- Tests: `commonTest`, kotlin.test, `runTest`, backticked names in
  `` `Given <x> When <y> Then <z>` `` form. Fakes over mocking libraries.
- Commit messages: `POTATO-<n>: <Sentence describing change>.`

## Adding a new module

Use the generator — don't hand-write the files:

```bash
./scripts/new-module.sh features:pantry --api --impl --mvi        # feature screen with a contract
./scripts/new-module.sh shared:clock --flat                       # small shared capability
```

It writes the directories, package path, stub `build.gradle.kts`, `settings.gradle.kts` includes and
(with `--mvi`) the MVI file set. `--help` lists the flags; `.claude/skills/new-module/SKILL.md` covers
which to pick and what to do afterwards. Then: fill in the `api` contract, replace the generated
`TODO(...)` in `onIntent` and the placeholder `Screen` body, and wire the module into its consumer
via the typesafe accessor — the generator does not add that dependency for you.

No module needs an `androidMain/AndroidManifest.xml`: AGP generates one from `namespace`. Add the file
only for real manifest entries (permission, provider, activity).

## Known gotchas

- `KeyValueStorageImpl.get()` is a `TODO()` stub.
- `AndroidLibraryPlugin` sets `kotlinOptions.jvmTarget = "23"` while compileOptions/detekt use 21
  (detekt tasks pin their own jvmTarget independently). Write code to 21 semantics.
- Configuration cache is disabled (`gradle.properties`).

## Commands

```bash
./gradlew build                       # everything
./gradlew detekt                      # lint all modules
./gradlew koverVerify                 # coverage gates
./gradlew :composeApp:assembleDebug   # Android app
```
