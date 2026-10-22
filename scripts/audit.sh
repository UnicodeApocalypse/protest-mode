#!/bin/zsh
set -euo pipefail
source "${0:A:h}/lib.sh"

setup_java
setup_sdk

local apk="${1:-$ROOT_DIR/app/build/outputs/apk/release/app-release.apk}"
[[ -f "$apk" ]] || apk="$ROOT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
[[ -f "$apk" ]] || die "No release APK: run ./gradlew assembleRelease first"
info "Auditing $apk"

local tools
tools="$(build_tools_dir)"
local failed=0

info "Permissions"
local -a allowed
allowed=(${(f)"$(grep -v '^\s*#' "$ROOT_DIR/config/allowed-permissions.txt" | grep -v '^\s*$')"})
local permission
for permission in $("$tools/aapt2" dump permissions "$apk" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p"); do
    if (( ${allowed[(Ie)$permission]} )); then
        ok "$permission"
    else
        error "Unexpected permission: $permission"
        failed=1
    fi
done

info "Components"
local manifest
manifest="$("$tools/aapt2" dump xmltree --file AndroidManifest.xml "$apk")"
local components
components="$(print -r -- "$manifest" | grep -E "E: (activity|service|receiver|provider)" -A3 | sed -n 's/.*android:name.*="\([^"]*\)".*/\1/p' | sort -u)"
print -r -- "$components" | while read -r component; do
    case "$component" in
        app.protestmode.MainActivity|androidx.startup.InitializationProvider) ok "$component" ;;
        *) error "Unexpected component: $component"; failed=1 ;;
    esac
done
if print -r -- "$manifest" | grep -q "EmojiCompatInitializer\|ProfileInstaller"; then
    error "Removed library initializers are back"
    failed=1
fi

info "Code"
local work
work="$(mktemp -d)"
unzip -q "$apk" 'classes*.dex' -d "$work"
local dump="$work/dump.txt"
for dex in "$work"/classes*.dex; do "$tools/dexdump" -d "$dex" >> "$dump" 2>/dev/null; done
local pattern
for pattern in "Ljava/net/URL;" "Ljava/net/HttpURLConnection" "Ljava/net/Socket;" "Ljavax/net/ssl/" \
    "Landroid/webkit/WebView" "Landroid/app/DownloadManager" "com/google/android/gms" "com/google/firebase" "okhttp"; do
    if grep -q -- "$pattern" "$dump"; then
        error "Network or third-party code found: $pattern"
        failed=1
    else
        ok "No $pattern"
    fi
done
rm -rf "$work"

(( failed == 0 )) || die "Audit failed"
ok "Audit passed"
