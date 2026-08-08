#!/usr/bin/env bash
#
# Scaffolds a new Gradle module that matches this repo's conventions.
#
# The convention plugins in build-logic/ already carry the build configuration, so all
# this does is the mechanical part: directories, the package path (which MUST equal the
# namespace AndroidLibraryPlugin derives from the Gradle path), a stub build.gradle.kts,
# and the settings.gradle.kts include.
#
# See .claude/skills/new-module/SKILL.md for how to choose the flags.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SETTINGS="$ROOT/settings.gradle.kts"

usage() {
    cat <<'EOF'
Usage: ./scripts/new-module.sh <gradle-path> [options]

  <gradle-path>   Colon-separated, no leading colon.  e.g. features:login, shared:dispatchers

Options:
  --api           create <gradle-path>:api             (api.library)
  --impl          create <gradle-path>:implementation  (android.library)
  --fake          create <gradle-path>:fake            (fake.library)
  --flat          create <gradle-path> itself          (android.library, no submodules)
  --mvi           scaffold the MVI file set in the implementation module
  --kover [N]     apply kover.library to the implementation/flat module;
                  N (0-100) also writes the coverage gates
  --dry-run       print what would happen, write nothing
  -h, --help      this message

At least one of --api / --impl / --fake / --flat is required.

Examples:
  ./scripts/new-module.sh features:login --api --impl --mvi
  ./scripts/new-module.sh features:pantry --api --impl --fake --mvi --kover 80
  ./scripts/new-module.sh shared:clock --flat
EOF
}

# ":"/"-" both become "." — matches AndroidLibraryPlugin.modulePackageSuffix.
# features:login:implementation -> features.login.implementation
pkg_of() { printf '%s' "$1" | tr ':-' '..'; }

# features:login:implementation -> features/login/implementation  (hyphens survive)
dir_of() { printf '%s' "$1" | tr ':' '/'; }

# features:login:implementation -> com/appotato/features/login/implementation
pkgdir_of() { printf 'com/appotato/'; pkg_of "$1" | tr '.' '/'; }

# shared:ui-components -> projects.shared.uiComponents
accessor_of() {
    printf 'projects'
    printf '%s' "$1" | awk -F':' '{
        for (i = 1; i <= NF; i++) {
            n = split($i, part, "-")
            printf "." part[1]
            for (j = 2; j <= n; j++) printf toupper(substr(part[j], 1, 1)) substr(part[j], 2)
        }
    }'
}

# login -> Login ; add-item -> AddItem
pascal_of() {
    printf '%s' "$1" | awk -F'-' '{ for (i = 1; i <= NF; i++) printf toupper(substr($i, 1, 1)) substr($i, 2) }'
}

log() { printf '  %s\n' "$1"; }

write_file() {
    local path="$1" body="$2"
    if [[ -e "$path" ]]; then
        printf 'refusing to overwrite existing file: %s\n' "${path#"$ROOT"/}" >&2
        exit 1
    fi
    if $DRY_RUN; then
        log "create ${path#"$ROOT"/}"
        return
    fi
    mkdir -p "$(dirname "$path")"
    printf '%s' "$body" > "$path"
    log "create ${path#"$ROOT"/}"
}

add_include() {
    local gradle_path="$1" line
    line="include(\":$gradle_path\")"
    if grep -qxF "$line" "$SETTINGS"; then
        log "include already present: :$gradle_path"
        return
    fi
    if $DRY_RUN; then
        log "settings.gradle.kts += $line"
        return
    fi
    printf '%s\n' "$line" >> "$SETTINGS"
    log "settings.gradle.kts += $line"
}

BASE=""
MAKE_API=false
MAKE_IMPL=false
MAKE_FAKE=false
MAKE_FLAT=false
MAKE_MVI=false
USE_KOVER=false
KOVER_MIN=""
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --api)     MAKE_API=true; shift ;;
        --impl)    MAKE_IMPL=true; shift ;;
        --fake)    MAKE_FAKE=true; shift ;;
        --flat)    MAKE_FLAT=true; shift ;;
        --mvi)     MAKE_MVI=true; shift ;;
        --dry-run) DRY_RUN=true; shift ;;
        --kover)
            USE_KOVER=true
            shift
            if [[ ${1:-} =~ ^[0-9]+$ ]]; then
                KOVER_MIN="$1"
                shift
            fi
            ;;
        -h|--help) usage; exit 0 ;;
        -*) printf 'unknown option: %s\n\n' "$1" >&2; usage >&2; exit 2 ;;
        *)
            if [[ -n "$BASE" ]]; then
                printf 'unexpected argument: %s\n\n' "$1" >&2; usage >&2; exit 2
            fi
            BASE="$1"; shift ;;
    esac
done

[[ -n "$BASE" ]] || { printf 'missing <gradle-path>\n\n' >&2; usage >&2; exit 2; }

if [[ "$BASE" == :* ]]; then
    printf 'drop the leading colon: use %s\n' "${BASE#:}" >&2
    exit 2
fi
if [[ ! "$BASE" =~ ^[a-z0-9]+([-a-z0-9]*[a-z0-9])?(:[a-z0-9]+([-a-z0-9]*[a-z0-9])?)*$ ]]; then
    printf 'invalid gradle path %q — use lowercase segments, e.g. features:login\n' "$BASE" >&2
    exit 2
fi
if [[ -n "$KOVER_MIN" ]] && (( KOVER_MIN > 100 )); then
    printf 'coverage must be 0-100, got %s\n' "$KOVER_MIN" >&2
    exit 2
fi

if $MAKE_FLAT && { $MAKE_API || $MAKE_IMPL || $MAKE_FAKE; }; then
    printf -- '--flat cannot be combined with --api/--impl/--fake\n' >&2
    exit 2
fi
if ! $MAKE_FLAT && ! $MAKE_API && ! $MAKE_IMPL && ! $MAKE_FAKE; then
    printf 'pick at least one of --api / --impl / --fake / --flat\n\n' >&2
    usage >&2
    exit 2
fi
if $MAKE_MVI && ! $MAKE_IMPL && ! $MAKE_FLAT; then
    printf -- '--mvi needs a module to put the files in (--impl or --flat)\n' >&2
    exit 2
fi
if $MAKE_FAKE && ! $MAKE_API; then
    printf -- '--fake without --api: a fake implements an api contract, so create one too\n' >&2
    exit 2
fi

API_PATH="$BASE:api"
IMPL_PATH="$BASE:implementation"
FAKE_PATH="$BASE:fake"
$MAKE_FLAT && IMPL_PATH="$BASE"

NAME="$(pascal_of "${BASE##*:}")"

$DRY_RUN && printf 'DRY RUN — nothing will be written\n'
printf 'Scaffolding :%s\n' "$BASE"

# ---------------------------------------------------------------- api module

if $MAKE_API; then
    write_file "$ROOT/$(dir_of "$API_PATH")/build.gradle.kts" 'plugins {
    id("api.library")
    id("detekt.library")
}
'
    write_file "$ROOT/$(dir_of "$API_PATH")/src/commonMain/kotlin/$(pkgdir_of "$API_PATH")/.gitkeep" ''
    add_include "$API_PATH"
fi

# ------------------------------------------------- implementation / flat module

if $MAKE_IMPL || $MAKE_FLAT; then
    impl_plugins='plugins {
    id("android.library")
    id("detekt.library")'
    $USE_KOVER && impl_plugins="$impl_plugins
    id(\"kover.library\")"
    impl_plugins="$impl_plugins
}
"

    impl_kover=""
    if [[ -n "$KOVER_MIN" ]]; then
        impl_kover="
setKoverMinLineCoverage($KOVER_MIN)
setKoverMinInstructionCoverage($KOVER_MIN)
"
    fi

    impl_deps=""
    if $MAKE_MVI; then
        impl_deps="$impl_deps
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(projects.shared.uiComponents)"
    fi
    $MAKE_API && impl_deps="$impl_deps
                implementation($(accessor_of "$API_PATH"))"

    impl_body="$impl_plugins$impl_kover"
    if [[ -n "$impl_deps" ]]; then
        impl_body="$impl_body
kotlin {
    sourceSets {
        commonMain {
            dependencies {${impl_deps}
            }
        }
    }
}
"
    fi

    write_file "$ROOT/$(dir_of "$IMPL_PATH")/build.gradle.kts" "$impl_body"
    add_include "$IMPL_PATH"

    IMPL_PKG="$(pkg_of "$IMPL_PATH")"
    IMPL_SRC="$ROOT/$(dir_of "$IMPL_PATH")/src/commonMain/kotlin/$(pkgdir_of "$IMPL_PATH")"

    if $MAKE_MVI; then
        write_file "$IMPL_SRC/${NAME}State.kt" "package com.appotato.$IMPL_PKG

import androidx.compose.runtime.Immutable

@Immutable
internal data class ${NAME}State(
    val isLoading: Boolean = false
)
"
        write_file "$IMPL_SRC/${NAME}Intent.kt" "package com.appotato.$IMPL_PKG

internal sealed interface ${NAME}Intent {
    data object ScreenShown : ${NAME}Intent
}
"
        write_file "$IMPL_SRC/${NAME}Effect.kt" "package com.appotato.$IMPL_PKG

internal sealed interface ${NAME}Effect {
    data object NavigateBack : ${NAME}Effect
}
"
        write_file "$IMPL_SRC/${NAME}ViewModel.kt" "package com.appotato.$IMPL_PKG

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

internal class ${NAME}ViewModel : ViewModel() {

    private val _state = MutableStateFlow(${NAME}State())
    val state: StateFlow<${NAME}State> = _state.asStateFlow()

    private val _effects = Channel<${NAME}Effect>(Channel.BUFFERED)
    val effects: Flow<${NAME}Effect> = _effects.receiveAsFlow()

    fun onIntent(intent: ${NAME}Intent): Unit = TODO(\"Reduce \$intent into the next state.\")
}
"
        write_file "$IMPL_SRC/${NAME}Screen.kt" "package com.appotato.$IMPL_PKG

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ${NAME}Route(
    viewModel: ${NAME}ViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ${NAME}Screen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun ${NAME}Screen(
    state: ${NAME}State,
    onIntent: (${NAME}Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(${NAME}Intent.ScreenShown)
    }

    // Placeholder: render the state with the wrappers in shared/ui-components, not raw material3.
    Column(modifier = modifier) {
        if (state.isLoading) {
            Spacer(modifier = Modifier.fillMaxSize())
        }
    }
}
"
    else
        write_file "$IMPL_SRC/.gitkeep" ''
    fi
fi

# --------------------------------------------------------------- fake module

if $MAKE_FAKE; then
    write_file "$ROOT/$(dir_of "$FAKE_PATH")/build.gradle.kts" "plugins {
    id(\"fake.library\")
    id(\"detekt.library\")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // api: the fake publicly implements the api contract.
                api($(accessor_of "$API_PATH"))
            }
        }
    }
}
"
    write_file "$ROOT/$(dir_of "$FAKE_PATH")/src/commonMain/kotlin/$(pkgdir_of "$FAKE_PATH")/.gitkeep" ''
    add_include "$FAKE_PATH"
fi

printf '\nDone. Next:\n'
$MAKE_API  && printf '  - declare the public contract in :%s\n' "$API_PATH"
$MAKE_MVI  && printf '  - fill in %sViewModel.onIntent and %sScreen\n' "$NAME" "$NAME"
printf '  - wire the module into its consumer, e.g. implementation(%s)\n' "$(accessor_of "$IMPL_PATH")"
printf '  - ./gradlew :%s:build detekt\n' "$IMPL_PATH"
