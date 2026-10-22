#!/bin/zsh

ROOT_DIR="${0:A:h:h}"

info()  { print -P "%F{blue}[INFO]%f $*"; }
ok()    { print -P "%F{green}[OK]%f $*"; }
warn()  { print -P "%F{yellow}[WARN]%f $*"; }
error() { print -P "%F{red}[ERROR]%f $*" >&2; }

die() {
    error "$*"
    exit 1
}

# AGP does not support the newest JDKs: the JDK bundled with Android Studio is preferred.
setup_java() {
    local studio_jbr="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    if [[ -n "${PROTESTMODE_JAVA_HOME:-}" ]]; then
        export JAVA_HOME="$PROTESTMODE_JAVA_HOME"
    elif [[ -d "$studio_jbr" ]]; then
        export JAVA_HOME="$studio_jbr"
    elif [[ -z "${JAVA_HOME:-}" ]]; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 17 2>/dev/null)"
    fi
    [[ -x "$JAVA_HOME/bin/java" ]] || die "No JDK 17 or 21 found. Set PROTESTMODE_JAVA_HOME."
    info "JDK: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
}

setup_sdk() {
    if [[ -z "${ANDROID_HOME:-}" ]]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
    [[ -d "$ANDROID_HOME" ]] || die "Android SDK not found. Set ANDROID_HOME."
    if [[ ! -f "$ROOT_DIR/local.properties" ]]; then
        print "sdk.dir=$ANDROID_HOME" > "$ROOT_DIR/local.properties"
        info "local.properties created"
    fi
    path=("$ANDROID_HOME/platform-tools" $path)
}

build_tools_dir() {
    local latest
    latest="$(ls -1 "$ANDROID_HOME/build-tools" | sort -V | tail -1)"
    [[ -n "$latest" ]] || die "No build-tools installed in $ANDROID_HOME"
    print "$ANDROID_HOME/build-tools/$latest"
}

gradle() {
    "$ROOT_DIR/gradlew" --console=plain -p "$ROOT_DIR" "$@"
}

run_checks() {
    info "Static checks (ktlint, Android lint)"
    gradle ktlintCheck lintDebug || die "Static checks failed"
    ok "Static checks passed"

    info "Unit tests (JVM + Robolectric)"
    gradle testDebugUnitTest || die "Unit tests failed"
    ok "Unit tests passed"
}

connected_devices() {
    adb devices 2>/dev/null | awk 'NR > 1 && $2 == "device" { print $1 }'
}

device_kind() {
    local features
    features="$(adb -s "$1" shell pm list features 2>/dev/null | tr -d '\r')"
    case "$features" in
        *android.software.leanback*) print tv ;;
        *android.hardware.type.watch*) print watch ;;
        *android.hardware.type.automotive*) print car ;;
        *android.hardware.type.embedded*) print embedded ;;
        *) print phone ;;
    esac
}

describe_device() {
    local serial="$1"
    local manufacturer model release api
    manufacturer="$(adb -s "$serial" shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r')"
    model="$(adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
    release="$(adb -s "$serial" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')"
    api="$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
    print "$manufacturer $model, Android $release (API $api), $(device_kind "$serial")"
}

# Gradle runs connected tasks on every connected device, so the target must be the only one.
# --allow-others is reserved to scripts that pass -s to every adb command and never run Gradle connected tasks.
select_device() {
    local allow_others=false
    [[ "${1:-}" == "--allow-others" ]] && allow_others=true
    local -a serials
    serials=(${(f)"$(connected_devices)"})
    local serial
    if (( ${#serials} == 0 )); then
        error "No device connected (adb devices)"
        return 1
    fi
    info "Connected devices:"
    for serial in $serials; do
        info "  $serial: $(describe_device "$serial")"
    done
    if [[ -z "${ANDROID_SERIAL:-}" ]]; then
        error "No target device. Name it explicitly: ANDROID_SERIAL=<serial> scripts/${ZSH_ARGZERO:t}"
        return 1
    fi
    if (( ! ${serials[(Ie)$ANDROID_SERIAL]} )); then
        error "Device $ANDROID_SERIAL is not connected"
        return 1
    fi
    if (( ${#serials} > 1 )) && [[ $allow_others == false ]]; then
        error "Several devices are connected: disconnect every device but $ANDROID_SERIAL"
        return 1
    fi
    local kind
    kind="$(device_kind "$ANDROID_SERIAL")"
    if [[ "$kind" != phone ]]; then
        error "Device $ANDROID_SERIAL is a $kind, not a phone: refused"
        return 1
    fi
    export ANDROID_SERIAL
    TARGET_MODEL="$(adb shell getprop ro.product.model | tr -d '\r' | tr ' ' '_')"
    TARGET_API="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
    ok "Target device: $ANDROID_SERIAL, $(describe_device "$ANDROID_SERIAL")"
}
