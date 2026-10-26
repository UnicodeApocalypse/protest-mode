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

# aapt2 ships one jar per host OS and Gradle only records the one of this machine; the other hosts
# (the F-Droid build server runs Linux) get theirs, kept only if it matches the SHA-256 Google publishes.
info "Adding the aapt2 jars of the other host systems"
if ! python3 -I - "$metadata" "$work" <<'PY'
import hashlib, re, sys, urllib.request
metadata, work = sys.argv[1], sys.argv[2]
text = open(metadata).read()
pattern = re.compile(r'(<component group="com\.android\.tools\.build" name="aapt2" version="([^"]+)">\n)(.*?)(\s*</component>)', re.S)

def complete(match):
    head, version, body, tail = match.groups()
    indent = re.match(r"\s*", body).group(0)
    blocks = {name: block for block, name in re.findall(r'(<artifact name="([^"]+)">.*?</artifact>)', body, re.S)}
    for host in ("linux", "osx", "windows"):
        name = f"aapt2-{version}-{host}.jar"
        if name in blocks:
            continue
        url = f"https://dl.google.com/android/maven2/com/android/tools/build/aapt2/{version}/{name}"
        digest = hashlib.sha256(urllib.request.urlopen(url, timeout=120).read()).hexdigest()
        published = urllib.request.urlopen(url + ".sha256", timeout=60).read().decode().split()[0]
        if digest != published:
            sys.exit(f"{name}: downloaded {digest}, published {published}")
        blocks[name] = (f'<artifact name="{name}">\n{indent}   <sha256 value="{digest}" '
                        f'origin="Google Maven, checked against the published SHA-256"/>\n{indent}</artifact>')
        print(f"added {name}")
    return head + indent + ("\n" + indent).join(blocks[k] for k in sorted(blocks)) + tail

text, count = pattern.subn(complete, text)
if count == 0:
    sys.exit("no aapt2 component found")
open(metadata, "w").write(text)
PY
then
    restore
    die "Could not add the aapt2 jars of the other hosts; previous file restored"
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
