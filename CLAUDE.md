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
  ui-components/            design system: AppotatoTheme, Buttons, TextFields, Texts, Loaders, Images (Coil)
  dispatchers/              CoroutineDispatchers (expect/actual per platform)
  serialization/{api,implementation,fake}
  storage/{api,implementation}          KeyValueStorage — impl is TODO stub, nothing consumes it
  database/                 Room KMP: AppotatoDatabase, entities, DAOs, databaseModule()
  telemetry/{api,implementation,fake}   Telemetry — analytics + crash reporting, Firebase-backed
  remote-config/{api,implementation,fake}  RemoteConfig — server-controlled values, Firebase-backed
  app-update/{api,implementation,fake}  AppUpdateChecker — version gate on top of remote-config
  billing/{api,implementation,fake}     Billing — subscriptions and entitlements; impl is a no-op stub
features/                   one submodule per screen or per flow
  login/{api,implementation}            no sources yet, module shell only
  pantry/implementation                 main screen: item list, add/delete, free-tier gate
  paywall/implementation                Pro subscription screen
```

Every module is registered in `settings.gradle.kts`. Typesafe project accessors are on:
depend as `implementation(projects.shared.serialization.api)`, `projects.shared.uiComponents`,
`projects.features.pantry.implementation`.

`composeApp/App.kt` is the whole navigation: `PantryRoute`, with `PaywallRoute` swapped in on
`PantryEffect.PaywallRequested`. There is no navigation library — one destination does not justify
guessing at one.

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

`android.library` derives the namespace from the **Gradle path**, so sibling `api`/`implementation`
modules never collide: `:shared:ui-components` → `com.appotato.shared.ui.components`. Keep the
Kotlin package equal to the namespace.

It declares the three iOS targets but **no `binaries.framework`**. Xcode links exactly one
framework, `ComposeApp`, and it statically embeds every klib below it — a framework per module was
8 extra Kotlin/Native link tasks each (debug/release × arm64, x64, simulatorArm64, fat) that
nothing consumed. Only `:composeApp` declares a framework.
Per-module coverage gates: call `setKoverMinLineCoverage(n)` / `setKoverMinInstructionCoverage(n)`
at the top level of the module's `build.gradle.kts` (see `shared/serialization/implementation`, set to 100).

## Dependency injection (Koin)

Koin 4.1, BOM-managed. The graph is assembled in `:composeApp`:

- `composeApp/src/commonMain/.../di/Modules.kt` — `expect fun platformModules(): List<Module>` +
  `appModules()`, which appends the platform-independent modules: `coreModule()` (the
  `CoroutineDispatchers` binding), `databaseModule()`, `appUpdateModule()`, `billingModule()`,
  `pantryModule()`, `paywallModule()`.
- Android: `AppotatoApplication.onCreate()` → `setupKoin(this)`, which sets `androidContext` and
  loads `telemetryModule()` + `remoteConfigModule()` from the `implementation` modules.
- iOS: `iOSApp.init()` → `FirebaseApp.configure()` then
  `KoinIosKt.setupKoin(telemetry:remoteConfig:)` with the Swift implementations.

A shared module contributes bindings by exposing a `public fun <name>Module(): Module`; the classes
behind it stay `internal`. Nothing outside `:composeApp` calls `startKoin`.

ViewModels are bound with `viewModelOf(::X)` (`koin-core-viewmodel`) and resolved in Compose with
`koinViewModel()` (`koin-compose-viewmodel`). Both artifacts are BOM-managed — do not pin them.

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

## Persistence (Room)

One Room database for the whole app, in `:shared:database` — one file, one connection, one migration
history. A feature that owned its own would hand the next feature a second `.db`.

- `AppotatoDatabase` is `@ConstructedBy(AppotatoDatabaseConstructor::class)`; the `expect object`
  is declared by hand and KSP writes the `actual` per target. The `@Suppress("KotlinNoActualForExpect")`
  on it is required, not optional.
- KSP has no multiplatform configuration. The processor is added **per target** in
  `dependencies { add("kspAndroid", …); add("kspIosArm64", …); … }` — a missing target compiles
  until something references the generated code, and a target that no longer exists fails
  configuration outright with `Configuration with name 'kspIosX64' not found`. This list has to
  track whatever targets the convention plugins declare.
- Dates are stored as epoch-day `Long`, not through a `TypeConverter`, so `ORDER BY` and
  "expires within N days" stay SQL rather than a filter over the whole table.
- Schemas are exported to `shared/database/schemas` — commit them, they are the input to migration
  tests.
- Entities live in `shared/` even when they describe one feature's data, because `@Database` has to
  list them. The layering rule is held at the **repository** instead: `:shared:database` contains
  rows and no meaning, and the feature maps them onto its own domain model.

## Subscriptions and the free tier

`Billing` (`:shared:billing:api`) is the vendor-neutral contract: a `StateFlow<SubscriptionStatus>`
plus `plans()` / `purchase()` / `restore()` returning `Result`. Store product ids never leave the
implementation module.

- Gate on `Entitlement`, never on a plan or product id — `observeAccessTo(Entitlement.Pro)` and
  `hasAccessTo(...)` are the only two things a gated feature should need.
- `BillingError.Cancelled` is separate on purpose: backing out of the store sheet is the most common
  outcome of tapping "Subscribe", and showing an error for it makes the paywall look broken.
- `SubscriptionStatus.Active` carries `willRenew`, `isTrial` and `isInGracePeriod`. Grace period
  means the renewal charge failed but access continues — ask the user to fix their payment method,
  do not show a paywall.
- The free tier is `FREE_TIER_ITEM_LIMIT` in `:features:pantry:implementation`. The check runs
  through `PantryRepository.count()`, not off the rendered list, so a fast double tap cannot slip
  past it.
- `:shared:billing:implementation` currently binds `NoOpBilling` — everyone is on the free tier and
  every purchase fails with `Unavailable`. That file is the only place a billing vendor may be
  named; swapping it is the whole integration.

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
  <Name>Screen.kt        <Name>Route (public entry point) + stateless <Name>Screen
  <Name>ViewModel.kt     androidx.lifecycle.ViewModel (KMP artifact)
  <Name>State.kt         sealed interface or @Immutable data class
  <Name>Intent.kt        sealed interface — user actions
  <Name>Effect.kt        sealed interface — one-shot events (navigation, snackbar)
  <Name>Module.kt        public fun <name>Module(): Module
  domain/ data/          use cases and sources, all `internal`
```

`<Name>Route` and `<name>Module()` are the only `public` declarations in a feature's
`implementation` module — the same exception `remoteConfigModule()` already is. The ViewModel stays
`internal` and never appears in `Route`'s signature; the Route takes plain lambdas for its effects
(`onPaywallRequested`, `onDismissed`) so features never depend on each other.

Contract:
- `StateFlow<State>` exposed from the ViewModel, collected in Compose via `collectAsStateWithLifecycle()`.
- `Channel`/`SharedFlow<Effect>` for one-shot events — never model navigation as state.
- Single `fun onIntent(intent: Intent)` entry point; ViewModel reduces Intent + result → new State.
- All suspending work goes through injected `CoroutineDispatchers` (`shared/dispatchers`) — never
  reference `Dispatchers.X` directly, it breaks `runTest`.
- Composables in features must build on `shared/ui-components` wrappers, not raw material3.

## Code conventions

- `api`/`fake` declarations are explicitly `public`; `implementation` classes are `internal`.
- Top-level `const val` must be `UPPER_SNAKE` (detekt `TopLevelPropertyNaming`), private included.
  Non-const private top-level vals may be PascalCase — which is why `private val ScreenPadding =
  24.dp` passes and `private const val Threshold = 5` does not.
- Suspend functions that can fail return `Result<T>`; catch the specific exception, not `Exception`.
- Tests: `commonTest`, kotlin.test, `runTest`, backticked names in
  `` `Given <x> When <y> Then <z>` `` form. Fakes over mocking libraries.
- Commit messages: `POTATO-<n>: <Sentence describing change>.`

## Adding a new module

Use the generator — don't hand-write the files:

```bash
./scripts/new-module.sh features:shopping-list --api --impl --mvi   # feature screen with a contract
./scripts/new-module.sh shared:clock --flat                         # small shared capability
```

It writes the directories, package path, stub `build.gradle.kts`, `settings.gradle.kts` includes and
(with `--mvi`) the MVI file set. `--help` lists the flags; `.claude/skills/new-module/SKILL.md` covers
which to pick and what to do afterwards. Then: fill in the `api` contract, replace the generated
`TODO(...)` in `onIntent` and the placeholder `Screen` body, and wire the module into its consumer
via the typesafe accessor — the generator does not add that dependency for you.

No module needs an `androidMain/AndroidManifest.xml`: AGP generates one from `namespace`. Add the file
only for real manifest entries (permission, provider, activity).

## Known gotchas

- **Check the klib ABI before pinning any KMP dependency to "latest".** Kotlin 2.2.10 consumes
  klibs with `abi_version` ≤ 2.2.0. Android compiles regardless, so **only the iOS link fails**,
  with `KLIB resolver: Skipping … having incompatible ABI version` — which reads like a source
  error and is not. Check an artifact before raising it:

  ```bash
  curl -sO https://dl.google.com/dl/android/maven2/androidx/sqlite/sqlite-bundled-iosarm64/2.6.2/sqlite-bundled-iosarm64-2.6.2.klib
  unzip -p sqlite-bundled-iosarm64-2.6.2.klib '*/manifest' | grep -E 'abi_version|compiler_version'
  ```

  Room 2.8.4 (ABI 2.2.0) and the `androidx.sqlite` 2.6.2 it asks for are both fine. `sqlite-bundled`
  **2.7.0** is ABI 2.3.0 and is not — never raise `sqlite` past the version Room's POM requires,
  because nothing else forces it and the failure surfaces two modules away.
- **Icons in `composeResources` must be XML vector drawables, never SVG.** compose-resources
  supports SVG everywhere except Android, where `painterResource` throws
  `IllegalStateException: Android platform doesn't support SVG format` — **at runtime**, so the
  build, detekt and the unit tests all pass and the app dies on first composition. XML vector
  drawables work on both platforms; the `pathData` is the same string as an SVG `d` attribute.
- `kotlinx.datetime` 0.7 hands `Clock` and `Instant` back to the standard library, where they are
  still experimental. `Clock.System` needs `@OptIn(kotlin.time.ExperimentalTime::class)` — it is
  confined to `SystemToday` on purpose.
- detekt `TooManyFunctions` fires at **11** functions in a class, ViewModels included. Pure helpers
  move to top level rather than being merged away.
- `KeyValueStorageImpl.get()` is a `TODO()` stub and the interface has no write method. Nothing uses
  it; persistence goes through Room.
- `AndroidLibraryPlugin` sets `kotlinOptions.jvmTarget = "23"` while compileOptions/detekt use 21
  (detekt tasks pin their own jvmTarget independently). Write code to 21 semantics.
- Never run two `./gradlew build` invocations at once. They clobber each other's native output and
  fail with `dsymutil … cannot parse the debug map`, which looks like a source error and is not.
  Killing one mid-link is not free either: the next invocation can sit on its lock for hours.
- The Crashlytics dSYM upload phase **must** pass `-gsp`: its default is a file literally named
  `GoogleService-Info.plist`, and this app ships one plist per environment. Without it the build
  fails with `Could not get GOOGLE_APP_ID in Google Services file from build environment`.
- Firebase iOS comes from SPM (`firebase-ios-sdk`, upToNextMajor from 12.17.0) declared in the
  Xcode project — not from Gradle. `./gradlew` never resolves it.

## Commands

```bash
./gradlew :features:pantry:implementation:build   # one module: compile + detekt + kover + tests
./gradlew assembleDevDebug detekt koverVerify     # pre-commit: no iOS, one Android variant
./gradlew build                                   # everything, iOS linking included
./gradlew assembleDevDebug                        # Android app (dev flavor)
```

Reach for the full `build` only when touching `shared/*` or `build-logic`, or before pushing — it is
every module × Android debug+release × two iOS targets × detekt + kover + tests, plus six
`:composeApp` variants (3 flavors × 2 build types), and ~110 Android Lint tasks. Measured on a
12-core machine: `clean build` **4m29s**, warm full `build` ~15s, `assembleDevDebug detekt
koverVerify` ~3s, a module-scoped `build` under a second.

Configuration cache and parallel execution are both **on** (`gradle.properties`); turning them off
costs 7m12s versus 4m29s on the same tree. If a change makes the configuration cache fail, fix the
offending task rather than switching the flag off — the message names the task and usually the line.
