#!/usr/bin/env python
"""Render assets/brand/<name>.svg -> PNG icon set for iTalase launcher icons.

Render path: headless Edge (Chromium) because ImageMagick/rsvg are absent on this host.
Skipped: adaptive-icon XML wiring, vector-drawable conversion, Play Store listing art.
Add when: the icon is approved and needs to ship in androidApp/src/main/res.

Usage: python assets/brand/render_icons.py italase-icon-v2.svg
"""
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw

EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
HERE = Path(__file__).resolve().parent
CANVAS = 512
# Android legacy launcher buckets: 48/72/96/144/192 dp at 1x..4x
DENSITIES = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]


def render_svg(svg_name: str) -> Image.Image:
    """Screenshot the SVG through headless Edge at exactly 512x512."""
    wrapper = HERE / "_render_tmp.html"
    wrapper.write_text(
        "<!doctype html><meta charset='utf-8'>"
        "<style>html,body{margin:0;padding:0;width:%dpx;height:%dpx;overflow:hidden}"
        "img{display:block;width:%dpx;height:%dpx}</style>"
        "<img src='%s'></body>" % (CANVAS, CANVAS, CANVAS, CANVAS, svg_name),
        encoding="utf-8",
    )
    out = HERE / "icon-master-512.png"
    out.unlink(missing_ok=True)
    subprocess.run(
        [
            EDGE, "--headless=new", "--disable-gpu", "--hide-scrollbars",
            "--force-device-scale-factor=1", f"--window-size={CANVAS},{CANVAS}",
            f"--screenshot={out}", (HERE / "_render_tmp.html").as_uri(),
        ],
        check=True, capture_output=True, timeout=90,
    )
    wrapper.unlink(missing_ok=True)
    if not out.exists():
        raise SystemExit(f"headless Edge produced no PNG for {svg_name}")
    return Image.open(out).convert("RGBA")


def masked(img: Image.Image, size: int, shape: str) -> Image.Image:
    """Downscale then clip to the launcher mask shape, keeping transparency."""
    base = img.resize((size, size), Image.LANCZOS)
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    if shape == "circle":
        d.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:  # Android squircle approximates a 22% corner radius
        d.rounded_rectangle((0, 0, size - 1, size - 1), radius=int(size * 0.22), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), m)
    return out


def small_size_strip(img: Image.Image) -> Image.Image:
    """Legibility proof: 32/48/64 px on light paper and on dark ink."""
    sizes = (32, 48, 64)
    pad, gap = 16, 12
    w = pad * 2 + sum(sizes) + gap * (len(sizes) - 1) + 24
    h = max(sizes) + pad * 2
    strip = Image.new("RGBA", (w, h), (250, 246, 238, 255))
    draw = ImageDraw.Draw(strip)
    dark_x = pad + sum(sizes) + gap * (len(sizes) - 1) + 24
    draw.rectangle((dark_x - 12, 0, w - 1, h - 1), fill=(20, 18, 12, 255))
    for band_x in (pad, dark_x):
        x = band_x
        for s in sizes:
            tile = masked(img, s, "squircle")
            strip.paste(tile, (x, (h - s) // 2), tile)
            x += s + gap
    return strip


def main() -> None:
    svg = sys.argv[1] if len(sys.argv) > 1 else "italase-icon-v2.svg"
    master = render_svg(svg)
    master.save(HERE / "italase-icon-512.png")
    for shape in ("squircle", "circle"):
        masked(master, 512, shape).save(HERE / f"preview-{shape}.png")
    for name, px in DENSITIES:
        masked(master, px, "squircle").save(HERE / f"ic_launcher-{name}.png")
    small_size_strip(master).save(HERE / "preview-small-sizes.png")
    print(f"rendered {svg} -> master 512px, {len(DENSITIES)} densities, 2 previews")


if __name__ == "__main__":
    main()
