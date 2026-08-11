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
  telemetry/{api,implementation,fake}   Telemetry — analytics + crash reporting, Firebase-backed
  remote-config/{api,implementation,fake}  RemoteConfig — server-controlled values, Firebase-backed
  app-update/{api,implementation,fake}  AppUpdateChecker — version gate on top of remote-config
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

## Dependency injection (Koin)

Koin 4.1, BOM-managed. The graph is assembled in `:composeApp`:

- `composeApp/src/commonMain/.../di/Modules.kt` — `expect fun platformModules(): List<Module>` +
  `appModules()`, which appends the platform-independent `appUpdateModule()`.
- Android: `AppotatoApplication.onCreate()` → `setupKoin(this)`, which sets `androidContext` and
  loads `telemetryModule()` + `remoteConfigModule()` from the `implementation` modules.
- iOS: `iOSApp.init()` → `FirebaseApp.configure()` then
  `KoinIosKt.setupKoin(telemetry:remoteConfig:)` with the Swift implementations.

A shared module contributes bindings by exposing a `public fun <name>Module(): Module`; the classes
behind it stay `internal`. Nothing outside `:composeApp` calls `startKoin`.

## Firebase

Split deliberately: **Android via Kotlin, iOS via Swift.**

- Android — `FirebaseTelemetry` / `FirebaseRemoteConfig` in the `implementation` modules'
  `androidMain`, on the native Firebase SDK. No KMP wrapper. SDK auto-inits via
  `FirebaseInitProvider`.
- iOS — Swift implements the Kotlin `Telemetry` and `RemoteConfig` interfaces directly
  (`iosApp/iosApp/FirebaseTelemetry.swift`, `FirebaseRemoteConfigBinding.swift`) and they are
  injected into Koin at startup. No cinterop, no CocoaPods in Gradle; `:composeApp`
  `export(projects.shared.<name>.api)` makes each contract visible in the framework, and a Swift
  class adopting one must subclass `NSObject`. Each SPM product also has to be added to the Xcode
  target (`packageProductDependencies` in `project.pbxproj`) — Swift files themselves are picked up
  automatically by the file-system synchronized group.
- A contract Swift has to implement cannot have `suspend` members: Kotlin suspend functions can't
  be overridden from Swift. Take a callback in the interface and add a `suspend` extension for
  Kotlin callers — see `RemoteConfig.refresh`.

Vendor names must not appear in `api` modules — the whole point is that swapping Firebase for
PostHog is a change in one `implementation` module.

For Auth/Firestore later, use a wrapper (GitLive/KFire) rather than hand-rolled bridges, and keep
the boundary **domain-level** (`PantryRepository`), never `Collection`/`Document` — otherwise the
move to an own backend is a rewrite instead of a swap.

## Remote config and forced updates

`RemoteConfig` (`:shared:remote-config:api`) is the generic getter — string/boolean/long by key,
plus `refresh()`. Keys are plain strings owned by whoever reads them; unknown or unfetched keys
return the zero value of their type. Debuggable builds use a 0s minimum fetch interval, release
builds 1h.

`AppUpdateChecker` (`:shared:app-update:api`) is the one consumer that ships with it. It reads four
parameters — publish them in the Firebase console, per-platform differences go on *conditions*, not
on extra keys:

| parameter | example | effect |
|---|---|---|
| `app_minimum_version` | `1.4.0` | installed version below it → `AppUpdateStatus.Required` (block) |
| `app_latest_version` | `1.6.0` | below it → `AppUpdateStatus.Available` (dismissible) |
| `app_update_message` | `Time to update` | optional copy for the prompt |
| `app_update_url` | store link | optional, opened from the prompt |

Versions are compared numerically per segment (`AppVersion`), never as strings — `"1.10.0"` sorts
below `"1.9.0"` lexicographically, which would lock out exactly the users who did update. Anything
unparseable, including an empty parameter, means `UpToDate`: a typo in the console must not be able
to brick the app. The installed version is `versionName` on Android and `CFBundleShortVersionString`
on iOS, so both platforms have to be bumped for a gate to mean the same thing.

## Environments

Three environments, one per Firebase project's worth of config. The suffix lives on the Android
flavor only — adding one to the `debug` build type too would double the number of package names
to register.

| Environment | Android variant | applicationId | iOS scheme / configuration | bundle id |
|---|---|---|---|---|
| dev | `devDebug` / `devRelease` | `com.appotato.dev` | `Appotato-dev` / `Debug-dev`, `Release-dev` | `com.appotato.Appotato.dev` |
| staging | `staging…` | `com.appotato.staging` | `Appotato-staging` / … | `com.appotato.Appotato.staging` |
| prod | `prod…` | `com.appotato` | `Appotato-prod` / … | `com.appotato.Appotato` |

- Android: `composeApp/src/<flavor>/google-services.json` (verified search path), plus
  `src/<flavor>/res/values/strings.xml` for the launcher label.
- iOS: `iosApp/Configuration/Config-<env>.xcconfig` sets `APP_ENVIRONMENT`, the bundle id and the
  display name; `Info.plist` surfaces `AppEnvironment`, and `iOSApp.swift` loads
  `GoogleService-Info-<env>.plist` through `FirebaseOptions(contentsOfFile:)`. No file-copying
  build phase.
- Custom Xcode configuration names mean the Kotlin plugin cannot infer debug vs release, so every
  configuration sets `KOTLIN_FRAMEWORK_BUILD_TYPE` explicitly. Adding a configuration without it
  fails the "Compile Kotlin Framework" phase.

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
- The Crashlytics dSYM upload phase **must** pass `-gsp`: its default is a file literally named
  `GoogleService-Info.plist`, and this app ships one plist per environment. Without it the build
  fails with `Could not get GOOGLE_APP_ID in Google Services file from build environment`.
- Firebase iOS comes from SPM (`firebase-ios-sdk`, upToNextMajor from 12.17.0) declared in the
  Xcode project — not from Gradle. `./gradlew` never resolves it.

## Commands

```bash
./gradlew build                       # everything
./gradlew detekt                      # lint all modules
./gradlew koverVerify                 # coverage gates
./gradlew :composeApp:assembleDebug   # Android app
```
