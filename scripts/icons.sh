#!/bin/zsh
# Usage: [ICON_EXPORT_DIR=<dir>] scripts/icons.sh [logo.svg] [font.otf]
# The French logo is used as is. For the other languages, its text is removed and set again with the
# same font, stroke and position; a line too long is scaled down to the width of "MANIF".
set -euo pipefail
source "${0:A:h}/lib.sh"

local logo="${1:-$ROOT_DIR/art/logo.svg}"
local font="${2:-$HOME/Library/Fonts/BentonSans Comp Bold Regular.otf}"
local chrome="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
[[ -f "$logo" ]] || die "Missing logo: $logo"
[[ -f "$font" ]] || die "Missing font: $font"
[[ -x "$chrome" ]] || die "Google Chrome is needed to render the SVG"
command -v magick >/dev/null || die "ImageMagick is needed (brew install imagemagick)"

local res="$ROOT_DIR/app/src/main/res"
local work
work="$(mktemp -d)"

info "Measuring the words to set"
local -A widths
local word
for word in PROTEST MODE MANIF MODO MANI MODALITÀ MANIFESTAZIONE; do
    widths[$word]="$(magick -background white -fill black -font "$font" -pointsize 1000 label:"$word" -trim -format %w info:)"
done

info "Composing one logo per language"
python3 -I - "$logo" "$work" "${widths[PROTEST]}" "${widths[MODE]}" "${widths[MANIF]}" "${widths[MODO]}" "${widths[MANI]}" \
    "${widths[MODALITÀ]}" "${widths[MANIFESTAZIONE]}" <<'PY'
import re, sys
logo, work = sys.argv[1], sys.argv[2]
w = dict(zip(["PROTEST", "MODE", "MANIF", "MODO", "MANI", "MODALITÀ", "MANIFESTAZIONE"], map(int, sys.argv[3:])))
source = open(logo).read()

# Text of the master logo: black glyphs drawn from a path, white outline drawn through a mask.
art = re.sub(r'<use xlink:href="#(stroke0_[0-9_]+)" fill="black"/>\n', "", source)
art = re.sub(r'<mask id="stroke1_[0-9_]+".*?</mask>\n', "", art, flags=re.S)
art = re.sub(r'<path d="[^"]*" stroke-width="18"[^>]*mask="url\(#stroke1_[0-9_]+\)"/>\n', "", art)
assert "stroke1_" not in art, "unexpected logo structure"

CAP, MAX_WIDTH, SIZE, BOTTOM, GAP, CENTER = 0.740, 444, 200, 928, 54, 503
LINES = {"en": ("PROTEST", "MODE"), "es": ("MODO", "MANI"), "it": ("MODALITÀ", "MANIFESTAZIONE")}

def text(lines, fill, stroke, centered=False):
    s1, s2 = (min(SIZE, MAX_WIDTH / (w[line] / 1000)) for line in lines)
    gap = GAP * min(s1, s2) / SIZE
    b1 = BOTTOM - CAP * s2 - gap
    # The text alone (monochrome layer) is centred vertically instead of keeping its place in the logo.
    shift = 500 - (b1 - CAP * s1 + BOTTOM) / 2 if centered else 0
    outline = 'stroke="white" stroke-width="18" stroke-linejoin="round" paint-order="stroke"' if stroke else ""
    return (f'<g transform="translate(0 {shift:.1f})" font-family="BentonSans Comp Bold" text-anchor="middle" fill="{fill}" {outline}>'
            f'<text x="{CENTER}" y="{b1:.1f}" font-size="{s1:.1f}">{lines[0]}</text>'
            f'<text x="{CENTER}" y="{BOTTOM}" font-size="{s2:.1f}">{lines[1]}</text></g>\n')

def save(name, content):
    open(f"{work}/{name}.svg", "w").write(content)

save("full-fr", source)
for loc, lines in LINES.items():
    save(f"full-{loc}", art.replace("</g>\n<defs>", text(lines, "black", True) + "</g>\n<defs>", 1))

# Monochrome layer (themed icons): the text alone, as a shape.
mono = '<svg width="1000" height="1000" viewBox="0 0 1000 1000" xmlns="http://www.w3.org/2000/svg">{}</svg>'
for loc, lines in dict(LINES, fr=("MODE", "MANIF")).items():
    save(f"mono-{loc}", mono.format(text(lines, "black", False, centered=True)))
PY

local svg
for svg in "$work"/*.svg; do
    "$chrome" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --window-size=1000,1000 \
        --default-background-color=00000000 --screenshot="${svg%.svg}.png" "file://$svg" >/dev/null 2>&1 \
        || die "Rendering failed: $svg"
done
ok "Rendered $(ls "$work"/*.png | wc -l | tr -d ' ') images"

# Adaptive icons: 108 dp layers whose inner 66 dp circle is the only area every launcher shows.
# The artwork is scaled to that circle; the background layer repeats its yellow.
local -A legacy foreground
legacy=(mdpi 48 hdpi 72 xhdpi 96 xxhdpi 144 xxxhdpi 192)
foreground=(mdpi 108 hdpi 162 xhdpi 216 xxhdpi 324 xxxhdpi 432)
local -A qualifier
qualifier=(en "" fr "-fr" es "-es" it "-it")
local loc density size inner dir
for loc in en fr es it; do
    for density in ${(k)foreground}; do
        dir="$res/mipmap${qualifier[$loc]}-$density"
        mkdir -p "$dir"
        size=${foreground[$density]}
        inner=$(( size * 66 / 108 ))
        magick "$work/full-$loc.png" -resize ${inner}x${inner} -background none -gravity center -extent ${size}x${size} \
            -quality 92 "$dir/ic_launcher_foreground.webp"
        magick "$work/mono-$loc.png" -resize ${inner}x${inner} -background none -gravity center -extent ${size}x${size} \
            -alpha extract -background white -alpha shape -define webp:lossless=true "$dir/ic_launcher_monochrome.webp"
        magick "$work/full-$loc.png" -resize ${legacy[$density]}x${legacy[$density]} -quality 92 "$dir/ic_launcher.webp"
    done
    # Resource selection matches the language before the API level: without a localized
    # anydpi-v26 copy, the localized legacy icon would win over the adaptive one on Android 8+.
    mkdir -p "$res/mipmap${qualifier[$loc]}-anydpi-v26"
    cat > "$res/mipmap${qualifier[$loc]}-anydpi-v26/ic_launcher.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/icons.sh; one copy per language folder. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
XML
    ok "Launcher icons: $loc"
done

if [[ -n "${ICON_EXPORT_DIR:-}" ]]; then
    mkdir -p "$ICON_EXPORT_DIR"
    for loc in en fr es it; do
        magick "$work/full-$loc.png" -resize 512x512 -strip -define png:exclude-chunks=date,time "PNG32:$ICON_EXPORT_DIR/icon-$loc.png"
    done
    ok "512 px icons in $ICON_EXPORT_DIR"
fi

rm -rf "$work"
ok "Icons generated"
