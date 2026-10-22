#!/bin/zsh
set -euo pipefail
source "${0:A:h}/lib.sh"

setup_java
setup_sdk

run_checks

info "Debug build"
gradle assembleDebug || die "Build failed"
ok "APK: $ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
