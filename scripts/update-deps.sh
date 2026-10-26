#!/bin/zsh
# A checksum that changes while the version stays the same means that a published artifact was
# replaced: the script then restores the previous file and fails.
set -euo pipefail
source "${0:A:h}/lib.sh"

setup_java
setup_sdk

local metadata="$ROOT_DIR/gradle/verification-metadata.xml"
local -a tasks
tasks=(ktlintCheck lintDebug lintRelease testDebugUnitTest assembleDebug assembleRelease bundleRelease assembleDebugAndroidTest)

[[ -f "$metadata" ]] || die "Missing $metadata"
local work
work="$(mktemp -d)"
cp "$metadata" "$work/previous.xml"

restore() {
    cp "$work/previous.xml" "$metadata"
}

# One line per artifact: group:name:version|file|sha256
flatten() {
    awk '
        /<component / {
            match($0, /group="[^"]*"/); group = substr($0, RSTART + 7, RLENGTH - 8)
            match($0, /name="[^"]*"/); name = substr($0, RSTART + 6, RLENGTH - 7)
            match($0, /version="[^"]*"/); version = substr($0, RSTART + 9, RLENGTH - 10)
        }
        /<artifact / { match($0, /name="[^"]*"/); artifact = substr($0, RSTART + 6, RLENGTH - 7) }
        /<sha256 / {
            match($0, /value="[^"]*"/)
            print group ":" name ":" version "|" artifact "|" substr($0, RSTART + 7, RLENGTH - 8)
        }
    ' "$1" | sort -u
}

# A warm Gradle cache resolves some metadata differently (a .pom instead of a .module, parent POMs
# skipped): the checksums are recorded with an empty one, like a fresh clone or the F-Droid build server.
info "Regenerating the checksums from scratch, with an empty Gradle cache"
rm "$metadata"
if ! GRADLE_USER_HOME="$work/gradle-home" gradle --no-configuration-cache --write-verification-metadata sha256 "${tasks[@]}"; then
    restore
    die "Build failed while regenerating the checksums; previous file restored"
fi

flatten "$work/previous.xml" > "$work/previous.txt"
flatten "$metadata" > "$work/current.txt"

# Same component and file, same version, different checksum: the artifact was replaced.
local tampered
tampered="$(awk -F'|' '
    NR == FNR { known[$1 "|" $2] = $3; next }
    ($1 "|" $2) in known && known[$1 "|" $2] != $3 { print $1 " " $2 }
' "$work/previous.txt" "$work/current.txt")"
if [[ -n "$tampered" ]]; then
    print -r -- "$tampered" | while read -r line; do error "Checksum changed without a version change: $line"; done
    restore
    rm -rf "$work"
    die "Possible tampered artifact; previous file restored. Investigate before trusting these files."
fi
ok "No checksum changed for an unchanged version"

components() { cut -d'|' -f1 "$1" | sort -u; }
components "$work/previous.txt" > "$work/previous-components.txt"
components "$work/current.txt" > "$work/current-components.txt"
modules() { sed 's/:[^:]*$//' "$1" | sort -u; }

local added removed
added="$(comm -13 "$work/previous-components.txt" "$work/current-components.txt")"
removed="$(comm -23 "$work/previous-components.txt" "$work/current-components.txt")"

if [[ -z "$added" && -z "$removed" ]]; then
    ok "No dependency change"
else
    print -r -- "$added" | modules /dev/stdin > "$work/added-modules.txt"
    print -r -- "$removed" | modules /dev/stdin > "$work/removed-modules.txt"
    local module old new
    comm -12 "$work/added-modules.txt" "$work/removed-modules.txt" | while read -r module; do
        [[ -n "$module" ]] || continue
        old="$(print -r -- "$removed" | grep "^$module:" | sed 's/.*://' | paste -sd, -)"
        new="$(print -r -- "$added" | grep "^$module:" | sed 's/.*://' | paste -sd, -)"
        warn "Updated: $module $old -> $new"
    done
    comm -23 "$work/added-modules.txt" "$work/removed-modules.txt" | while read -r module; do
        [[ -n "$module" ]] && warn "New dependency: $module $(print -r -- "$added" | grep "^$module:" | sed 's/.*://' | paste -sd, -)"
    done
    comm -13 "$work/added-modules.txt" "$work/removed-modules.txt" | while read -r module; do
        [[ -n "$module" ]] && info "Removed dependency: $module"
    done
    warn "Review every new or updated dependency, then: git diff gradle/verification-metadata.xml"
fi

rm -rf "$work"
ok "Checksums regenerated"
