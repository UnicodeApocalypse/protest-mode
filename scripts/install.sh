#!/bin/zsh
# Usage: ANDROID_SERIAL=<serial> scripts/install.sh
set -euo pipefail
source "${0:A:h}/lib.sh"

setup_java
setup_sdk

select_device || die "Nothing installed"

info "Installing the debug build on $TARGET_MODEL"
gradle installDebug || die "Installation failed"
ok "Installed"

info "Launching the app"
adb shell am start -n app.protestmode/.MainActivity >/dev/null || die "Launch failed"
ok "Protest Mode launched"
