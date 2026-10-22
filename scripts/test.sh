#!/bin/zsh
# Usage: [ANDROID_SERIAL=<serial>] scripts/test.sh
set -euo pipefail
source "${0:A:h}/lib.sh"

setup_java
setup_sdk

run_checks

if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    warn "No ANDROID_SERIAL: instrumented tests skipped. To run them: ANDROID_SERIAL=<serial> scripts/test.sh"
    exit 0
fi
select_device || die "Instrumented tests not run"

info "Instrumented tests on $TARGET_MODEL (API $TARGET_API)"
adb logcat -c || true
gradle connectedDebugAndroidTest || die "Instrumented tests failed on $TARGET_MODEL (API $TARGET_API)"
ok "Instrumented tests passed on $TARGET_MODEL (API $TARGET_API)"
mkdir -p "$ROOT_DIR/build/compat"
local report="$ROOT_DIR/build/compat/$TARGET_MODEL-api$TARGET_API.log"
adb logcat -d -s ProtestModeCompat:I > "$report" || true
info "Compatibility report: $report"
