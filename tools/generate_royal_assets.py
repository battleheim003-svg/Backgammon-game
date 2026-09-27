#!/usr/bin/env python3
"""
Renders the royal theme's surfaces: gold plaques, carved walnut panels and the
chapter emblems.

Why render instead of stacking XML shapes. A <shape> with a <gradient> and a
<stroke> is a flat fill with a line around it — it reads as an app, not as an
object. What makes a game control look like a thing you could pick up is
lighting: a bevel that catches a highlight along its top edge, a shadow that
gathers along the bottom, a specular streak across the face, and grain that the
light plays over. None of that exists in the XML drawable vocabulary, so it is
computed here.

The method is a small renderer. Each surface starts as a mask; the distance to
the mask edge becomes a height field; the gradient of that height field gives
surface normals; the normals are lit with one key light and one specular lobe.
Material grain (hammered gold, walnut figure) is added to the height field
before lighting, so the light reacts to the texture rather than the texture
being painted flat on top.

Everything stretchable is written as a nine-patch, so one rendered plaque serves
every button size in the app.

Run from the repo root:  python3 tools/generate_royal_assets.py
"""

import math
import pathlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
from scipy import ndimage

RES = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res"
OUT = RES / "drawable-xhdpi"
FONTS = RES / "font"

# Titles are art, so they are rendered per language and dropped into the
# matching resource folder. English falls back to the default folder.
TITLES = {
    None: {
        "title_app": "Royal Backgammon",
        "title_journey": "Journey",
    },
    "fa": {
        "title_app": "تخته‌نرد سلطنتی",
        "title_journey": "سفر",
    },
}
SS = 4  # supersampling for the mask, so edges are not stair-stepped

# One key light, upper left, slightly in front. Everything is lit by this, which
# is what makes separate assets look like they sit in the same room.
LIGHT = np.array([-0.42, -0.76, 0.50])
LIGHT /= np.linalg.norm(LIGHT)
VIEW = np.array([0.0, 0.0, 1.0])
HALF = (LIGHT + VIEW) / np.linalg.norm(LIGHT + VIEW)


# ── helpers ──────────────────────────────────────────────────────────────────

def rounded_mask(w, h, radius):
    """Anti-aliased rounded-rectangle coverage in 0..1."""
    img = Image.new("L", (w * SS, h * SS), 0)
    ImageDraw.Draw(img).rounded_rectangle(
        [0, 0, w * SS - 1, h * SS - 1], radius=radius * SS, fill=255)
    return np.asarray(img.resize((w, h), Image.LANCZOS), dtype=np.float64) / 255.0


def smoothstep(x):
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3.0 - 2.0 * x)


def bevel_height(mask, bevel):
    """
    Height field: flat across the face, falling away over `bevel` pixels at the
    edge. The distance transform is what turns any silhouette into a bevel, so
    this works for a pill, a rounded rectangle or a star without special cases.
    """
    inside = ndimage.distance_transform_edt(mask > 0.5)
    return smoothstep(inside / max(bevel, 1e-6))


def shade(height, albedo, mask, *, relief=2.6, ambient=0.52, key=0.62,
          spec_strength=0.55, spec_power=28, spec_color=(1.0, 0.97, 0.88)):
    """Light a height field and return an RGBA image."""
    gy, gx = np.gradient(height * relief)
    nz = np.ones_like(gx)
    norm = np.sqrt(gx * gx + gy * gy + nz * nz)
    nx, ny, nz = -gx / norm, -gy / norm, nz / norm

    diffuse = np.clip(nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2], 0, 1)
    specular = np.clip(nx * HALF[0] + ny * HALF[1] + nz * HALF[2], 0, 1) ** spec_power

    lit = albedo * (ambient + key * diffuse)[..., None]
    lit = lit + spec_strength * specular[..., None] * np.array(spec_color)

    rgba = np.zeros(albedo.shape[:2] + (4,), dtype=np.float64)
    rgba[..., :3] = np.clip(lit, 0, 1)
    rgba[..., 3] = mask
    return rgba


def to_image(rgba):
    return Image.fromarray((rgba * 255).round().astype(np.uint8), mode="RGBA")


def vertical_gradient(h, w, stops):
    """stops: [(position 0..1, (r,g,b) 0..255)] — linear between stops."""
    ys = np.linspace(0, 1, h)
    out = np.zeros((h, 3))
    positions = [p for p, _ in stops]
    channels = np.array([c for _, c in stops], dtype=np.float64) / 255.0
    for i in range(3):
        out[:, i] = np.interp(ys, positions, channels[:, i])
    return np.repeat(out[:, None, :], w, axis=1)


def grain(h, w, scale, seed, blur=1.0):
    """Smooth random field in -1..1, used to perturb height."""
    rng = np.random.default_rng(seed)
    small = rng.normal(size=(max(2, int(h / scale)), max(2, int(w / scale))))
    field = np.asarray(Image.fromarray(small).resize((w, h), Image.BICUBIC))
    field = ndimage.gaussian_filter(field, blur)
    peak = np.abs(field).max()
    return field / peak if peak else field


def walnut(h, w, seed=11, *, contrast=1.0, scale=1.0):
    """
    Walnut figure: growth rings pulled into long bands by a plank cut along the
    trunk. Real walnut has wide, wandering bands of low contrast, not the even
    high-contrast stripes a plain sine gives — so the ring wavelength is long,
    the wander is large and slow, and only a thin fibre adds the fine detail.

    `contrast` drops the figure for surfaces that carry text; `scale` widens the
    bands for large panels, so the grain does not look shrunk.

    Returns (albedo, height perturbation).
    """
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    wander = (grain(h, w, 46, seed, blur=4.0) * 74
              + grain(h, w, 16, seed + 1, blur=2.0) * 20)
    # The bands run across the plaque, not down it. A nine-patch stretches
    # horizontally, and a band that varies along x smears into vertical streaks
    # when it does; a band that varies along y survives the stretch untouched.
    rings = np.sin((yy + wander) * (2 * math.pi / (88.0 * scale)))
    rings = np.sign(rings) * np.abs(rings) ** 1.5  # soft bands, sharp only at the core
    fibre = grain(h, w, 1.4, seed + 2, blur=0.5) * 0.5
    blotch = grain(h, w, 30, seed + 3, blur=3.0) * 0.5

    tone = np.clip(0.52 + contrast * (0.17 * rings + 0.06 * fibre + 0.10 * blotch), 0, 1)
    dark = np.array([0x34, 0x20, 0x11]) / 255.0
    light = np.array([0x74, 0x4C, 0x2A]) / 255.0
    albedo = dark + (light - dark) * tone[..., None]
    return albedo, (rings * 0.035 + fibre * 0.045) * contrast


# Rarity is a material, not a coloured bar. A common thing is cast in bronze, a
# rare one in silver, an epic one glazed in Persian turquoise and a legendary one
# struck in gold — so what a player owns is legible from across the screen, and
# it is legible in this game's own terms rather than in a stock palette's.
MATERIALS = {
    "gold": [
        (0.00, (0xFF, 0xF0, 0xC8)), (0.16, (0xF7, 0xD4, 0x88)),
        (0.52, (0xDF, 0xAD, 0x4B)), (0.80, (0xB5, 0x7E, 0x28)),
        (1.00, (0x8C, 0x5D, 0x1A)),
    ],
    "bronze": [
        (0.00, (0xF4, 0xD3, 0xAE)), (0.18, (0xD8, 0xA2, 0x6A)),
        (0.55, (0xA9, 0x70, 0x2F)), (0.82, (0x7B, 0x4C, 0x1E)),
        (1.00, (0x51, 0x30, 0x12)),
    ],
    "silver": [
        (0.00, (0xFF, 0xFF, 0xFF)), (0.18, (0xE6, 0xEC, 0xF2)),
        (0.52, (0xBD, 0xC7, 0xD3)), (0.82, (0x84, 0x90, 0x9E)),
        (1.00, (0x57, 0x61, 0x6D)),
    ],
    "turquoise": [
        (0.00, (0xDC, 0xFA, 0xF2)), (0.18, (0x8C, 0xE6, 0xD6)),
        (0.52, (0x34, 0xC5, 0xAC)), (0.82, (0x18, 0x7E, 0x6E)),
        (1.00, (0x0C, 0x4C, 0x42)),
    ],
    # The materials a real set is made of, rather than a tier ladder.
    "bone": [
        (0.00, (0xFF, 0xFB, 0xF0)), (0.20, (0xF3, 0xE7, 0xCF)),
        (0.55, (0xDD, 0xCC, 0xAA)), (0.84, (0xB3, 0x9E, 0x79)),
        (1.00, (0x8A, 0x77, 0x56)),
    ],
    "walnut": [
        (0.00, (0x9A, 0x6C, 0x42)), (0.22, (0x7A, 0x50, 0x2C)),
        (0.58, (0x5A, 0x39, 0x1E)), (0.86, (0x3B, 0x25, 0x13)),
        (1.00, (0x26, 0x17, 0x0C)),
    ],
    "nacre": [
        (0.00, (0xFF, 0xFF, 0xFF)), (0.16, (0xE8, 0xF4, 0xFF)),
        (0.40, (0xF6, 0xE4, 0xFF)), (0.64, (0xDE, 0xF7, 0xEC)),
        (0.84, (0xB9, 0xC6, 0xD8)), (1.00, (0x8A, 0x95, 0xA6)),
    ],
    "agate": [
        (0.00, (0xFF, 0xD9, 0xCE)), (0.20, (0xE0, 0x84, 0x6C)),
        (0.55, (0xA8, 0x36, 0x2C)), (0.84, (0x6E, 0x1E, 0x1A)),
        (1.00, (0x42, 0x10, 0x0E)),
    ],
    "ebony": [
        (0.00, (0x6B, 0x66, 0x63)), (0.22, (0x44, 0x40, 0x3E)),
        (0.58, (0x2A, 0x27, 0x26)), (0.86, (0x18, 0x16, 0x15)),
        (1.00, (0x0C, 0x0B, 0x0B)),
    ],
}


def material_character(h, w, material, seed):
    """
    What a material looks like close up, beyond its colour ramp.

    A gradient alone gives plastic. Real Neyshabur turquoise is crossed by the
    dark matrix it grew in; agate is banded because it formed in layers; nacre
    shifts colour because light interferes in its plates; bone has a fine
    longitudinal grain; ebony is nearly featureless but not quite. Each of those
    is a different function, so each gets one.

    Returns (albedo multiplier, height perturbation).
    """
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    tint = np.ones((h, w, 3))
    relief = np.zeros((h, w))

    if material == "turquoise":
        # Matrix: a dark web of the host rock, thin and irregular.
        # Sparse and thin: a good stone shows a few veins, not a crazed glaze.
        veins = grain(h, w, 20, seed, blur=2.0)
        web = np.clip(1.0 - np.abs(veins) / 0.055, 0, 1) ** 2.2
        tint = tint * (1.0 - 0.55 * web)[..., None]
        relief -= web * 0.18

    elif material == "agate":
        # Bands: concentric, following a slowly wandering centre.
        cx, cy = w * 0.42, h * 0.55
        wobble = grain(h, w, 26, seed, blur=3.0) * 16
        rings = np.sin((np.hypot(xx - cx, yy - cy) + wobble) * (2 * math.pi / 13.0))
        band = 0.5 + 0.5 * np.sign(rings) * np.abs(rings) ** 0.8
        tint = tint * (0.80 + 0.34 * band)[..., None]
        relief += (band - 0.5) * 0.10

    elif material == "nacre":
        # Interference: hue slides across the surface instead of brightness.
        phase = (xx * 0.035 + yy * 0.052 + grain(h, w, 18, seed, blur=2.0) * 3.2)
        tint = np.stack([
            0.90 + 0.16 * np.sin(phase),
            0.92 + 0.14 * np.sin(phase + 2.1),
            0.94 + 0.16 * np.sin(phase + 4.2),
        ], axis=-1)
        relief += grain(h, w, 3, seed + 1, blur=0.6) * 0.05

    elif material == "bone":
        # Fine grain along the length, plus the faint mottle of real bone.
        lines = np.sin((yy + grain(h, w, 30, seed, blur=3.0) * 12) * (2 * math.pi / 3.4))
        mottle = grain(h, w, 14, seed + 2, blur=1.6)
        tint = tint * (1.0 + 0.045 * lines + 0.06 * mottle)[..., None]
        relief += lines * 0.03

    elif material == "walnut":
        figure, _ = walnut(h, w, seed=seed, contrast=1.0, scale=0.45)
        luma = figure.mean(axis=-1)
        tint = tint * (0.72 + 0.7 * luma)[..., None]
        relief += (luma - luma.mean()) * 0.30

    elif material == "ebony":
        streaks = grain(h, w, 20, seed, blur=2.2)
        tint = tint * (1.0 + 0.10 * streaks)[..., None]

    return np.clip(tint, 0, 2), relief


def hammered(h, w, material="gold", seed=5):
    """A metal over a hammered ground: its gradient plus shallow dents."""
    albedo = vertical_gradient(h, w, MATERIALS[material])
    dents = grain(h, w, 9, seed, blur=1.1) * 0.5 + grain(h, w, 3.2, seed + 1, blur=0.6) * 0.3

    # A slow diagonal swell across the face. Polished metal is never evenly
    # bright; this is the reflection of the room sliding over it.
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    sweep = np.sin((xx / max(w, 1) * 1.3 + yy / max(h, 1) * 0.7) * math.pi)
    albedo = np.clip(albedo * (0.93 + 0.14 * sweep[..., None]), 0, 1)

    tint, relief = material_character(h, w, material, seed + 101)
    albedo = np.clip(albedo * tint, 0, 1)

    return albedo, dents * 0.10 + sweep * 0.05 + relief


def hammered_gold(h, w, seed=5):
    """The gold case, kept as its own name because most callers want only gold."""
    return hammered(h, w, "gold", seed)


def engrave(height, mask, inset, width=2.2, depth=0.42):
    """
    Cut a groove parallel to the silhouette, `inset` pixels in from the edge.
    It is what separates a struck plaque from a filled rectangle: the light
    breaks along it twice, once down and once back up.
    """
    inside = ndimage.distance_transform_edt(mask > 0.5)
    band = np.clip(1.0 - np.abs(inside - inset) / width, 0, 1)
    return height - ndimage.gaussian_filter(band, 0.7) * depth


def studs(albedo, height, mask, centres, radius, rgb):
    """Set small enamel cabochons into a surface — the turquoise of the tilework."""
    h, w = mask.shape
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    colour = np.array(rgb, dtype=np.float64) / 255.0
    for cx, cy in centres:
        dist = np.hypot(xx - cx, yy - cy)
        disc = np.clip(radius - dist, 0, 1.6) / 1.6
        dome = np.clip(1.0 - (dist / max(radius, 1e-6)) ** 2, 0, 1) ** 0.5
        albedo = albedo * (1 - disc[..., None]) + colour * disc[..., None]
        height = height + disc * dome * 0.55
    return albedo, height


def add_rim(rgba, mask, colour=(0.33, 0.20, 0.06), width=1.6):
    """A dark contact line right at the silhouette, so the object has an edge."""
    outside = ndimage.distance_transform_edt(mask > 0.5)
    band = np.clip(1.0 - outside / width, 0, 1) * (mask > 0.1)
    rgba[..., :3] = rgba[..., :3] * (1 - band[..., None] * 0.85) \
        + np.array(colour) * (band[..., None] * 0.85)
    return rgba


def drop_shadow(rgba, offset=3, blur=3.5, opacity=0.5):
    """Composite the surface over its own soft shadow, inside the same canvas."""
    h, w = rgba.shape[:2]
    shadow = np.zeros((h, w))
    shadow[offset:, :] = rgba[:h - offset, :, 3]
    shadow = ndimage.gaussian_filter(shadow, blur) * opacity

    out = np.zeros_like(rgba)
    out[..., 3] = np.clip(shadow + rgba[..., 3], 0, 1)
    with np.errstate(invalid="ignore", divide="ignore"):
        src_a = rgba[..., 3:4]
        out[..., :3] = np.where(
            out[..., 3:4] > 0,
            (rgba[..., :3] * src_a) / np.maximum(out[..., 3:4], 1e-6),
            0)
    return out


def nine_patch(image, stretch, padding):
    """
    Wrap a rendered surface in the one-pixel nine-patch border: black marks on
    the top and left say which rows and columns may stretch, marks on the right
    and bottom say where content is allowed to sit.
    """
    w, h = image.size
    canvas = Image.new("RGBA", (w + 2, h + 2), (0, 0, 0, 0))
    canvas.paste(image, (1, 1))
    draw = ImageDraw.Draw(canvas)
    black = (0, 0, 0, 255)
    (sx0, sx1), (sy0, sy1) = stretch
    draw.line([(1 + sx0, 0), (1 + sx1, 0)], fill=black)
    draw.line([(0, 1 + sy0), (0, 1 + sy1)], fill=black)
    (px0, px1), (py0, py1) = padding
    draw.line([(1 + px0, h + 1), (1 + px1, h + 1)], fill=black)
    draw.line([(w + 1, 1 + py0), (w + 1, 1 + py1)], fill=black)
    return canvas


# ── the surfaces ─────────────────────────────────────────────────────────────

def gold_plaque(w=260, h=104, radius=26, bevel=11, pressed=False):
    mask = rounded_mask(w, h, radius)
    albedo, dents = hammered_gold(h, w, seed=5 if not pressed else 6)
    if pressed:
        # Sunk: the face darkens and the lit edge moves to the bottom.
        albedo = albedo[::-1] * 0.88
    height = bevel_height(mask, bevel) + dents * mask
    height = engrave(height, mask, inset=bevel + 4, width=2.4,
                     depth=0.38 if not pressed else 0.24)
    rgba = shade(height, albedo, mask,
                 relief=2.9 if not pressed else 2.2,
                 ambient=0.50 if not pressed else 0.44,
                 spec_strength=0.62 if not pressed else 0.34)
    rgba = add_rim(rgba, mask, colour=(0.30, 0.18, 0.04), width=2.0)
    return to_image(drop_shadow(rgba, offset=0 if pressed else 3,
                                blur=3.2, opacity=0.0 if pressed else 0.45))


def wood_plaque(w=260, h=104, radius=26, bevel=10, inlay=7, pressed=False,
                figure_contrast=1.0, figure_scale=0.55, darkness=1.0,
                stud_radius=0):
    """Carved walnut with a gold inlay frame set into its face."""
    mask = rounded_mask(w, h, radius)
    albedo, figure = walnut(h, w, contrast=figure_contrast, scale=figure_scale)
    albedo = albedo * darkness
    if pressed:
        albedo = albedo * 0.82

    height = bevel_height(mask, bevel) + figure * mask

    # The inlay: a narrow ring standing proud of the wood, lit as gold.
    inner = rounded_mask(w - 2 * inlay, h - 2 * inlay, max(radius - inlay, 2))
    inner_full = np.zeros_like(mask)
    inner_full[inlay:h - inlay, inlay:w - inlay] = inner
    ring = np.clip(mask - inner_full, 0, 1)
    ring_band = ndimage.gaussian_filter(ring, 0.6)
    gold_albedo, gold_dents = hammered_gold(h, w, seed=9)

    albedo = albedo * (1 - ring_band[..., None]) + gold_albedo * ring_band[..., None]
    height = height + ring_band * 0.55 + gold_dents * ring_band

    height = engrave(height, mask, inset=inlay + 5, width=2.0, depth=0.30)

    if stud_radius:
        # Turquoise set into the four corners of the frame, the way the menu
        # artwork already sets it into its corner medallions.
        offset = inlay + stud_radius + 5
        albedo, height = studs(
            albedo, height, mask,
            [(offset, offset), (w - offset, offset),
             (offset, h - offset), (w - offset, h - offset)],
            stud_radius, (0x2E, 0x9E, 0x8C))

    rgba = shade(height, albedo, mask,
                 relief=3.0 if not pressed else 2.3,
                 ambient=0.48 if not pressed else 0.40,
                 spec_strength=0.40 if not pressed else 0.22,
                 spec_power=22)
    rgba = add_rim(rgba, mask, colour=(0.12, 0.07, 0.03), width=2.0)
    return to_image(drop_shadow(rgba, offset=0 if pressed else 3,
                                blur=3.0, opacity=0.0 if pressed else 0.5))


def vector_mask(name, size):
    """
    Coverage of one of the app's vector drawables. android:pathData is SVG path
    syntax, so the drawable can be wrapped in an <svg> and rasterised; the alpha
    that comes back is the mask this renderer needs.
    """
    import io
    import re

    import cairosvg

    source = (RES / "drawable" / f"{name}.xml").read_text()
    paths = re.findall(r'android:pathData="([^"]+)"', source)
    viewport = re.search(r'android:viewportWidth="([\d.]+)"', source)
    box = float(viewport.group(1)) if viewport else 24.0
    body = "".join(f'<path d="{d}" fill="#FFFFFF"/>' for d in paths)
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {box} {box}">{body}</svg>')
    png = cairosvg.svg2png(bytestring=svg.encode(), output_width=size, output_height=size)
    icon = Image.open(io.BytesIO(png)).convert("RGBA")
    return np.asarray(icon.getchannel("A"), dtype=np.float64) / 255.0


def gold_icon(name, size=96):
    """
    An icon as a small struck object rather than a flat silhouette. Same light,
    same metal and same bevel as the buttons it sits on, so an icon reads as
    part of the same made thing instead of a symbol printed on top of it.
    """
    mask = vector_mask(name, size)
    albedo, dents = hammered_gold(size, size, seed=31)
    height = bevel_height(mask, max(size * 0.075, 2.0)) + dents * mask
    rgba = shade(height, albedo, mask, relief=3.6, ambient=0.50,
                 key=0.68, spec_strength=0.78, spec_power=22)
    rgba = add_rim(rgba, mask, colour=(0.24, 0.13, 0.03), width=1.5)
    return to_image(drop_shadow(rgba, offset=2, blur=2.2, opacity=0.5))


def title_plate(text, font_path, size_px, *, pad=26, outline=5):
    """
    A title struck in gold rather than typed in gold.

    The same renderer that lights the buttons is pointed at letterforms: the
    glyph coverage becomes the mask, the distance into the glyph becomes the
    bevel, and the shared key light falls across the strokes. A dark outline is
    dilated behind the letters so the title holds on any ground, the way a
    stamped plate sits proud of the surface it is struck into.

    Localised: one file per language, because the rendered strokes are the art.
    """
    font = ImageFont.truetype(str(font_path), size_px)
    probe = Image.new("L", (8, 8))
    box = ImageDraw.Draw(probe).textbbox((0, 0), text, font=font)
    w = box[2] - box[0] + pad * 2
    h = box[3] - box[1] + pad * 2

    glyphs = Image.new("L", (w, h), 0)
    ImageDraw.Draw(glyphs).text((pad - box[0], pad - box[1]), text, font=font, fill=255)
    mask = np.asarray(glyphs, dtype=np.float64) / 255.0

    albedo, dents = hammered_gold(h, w, seed=17)
    height = bevel_height(mask, max(size_px * 0.17, 4.0)) + dents * mask
    rgba = shade(height, albedo, mask, relief=4.4, ambient=0.46,
                 key=0.70, spec_strength=0.85, spec_power=20)
    rgba = add_rim(rgba, mask, colour=(0.28, 0.16, 0.03), width=1.8)

    # The dark plate the letters stand on, grown out from the glyph shapes.
    grown = ndimage.gaussian_filter(mask, outline * 0.26)
    plate_alpha = np.clip(grown * 6.0, 0, 1)
    plate = np.zeros_like(rgba)
    plate[..., :3] = np.array([0.07, 0.04, 0.02])
    plate[..., 3] = plate_alpha * 0.92

    out = np.zeros_like(rgba)
    src_a = rgba[..., 3:4]
    out[..., 3] = np.clip(rgba[..., 3] + plate[..., 3] * (1 - rgba[..., 3]), 0, 1)
    with np.errstate(invalid="ignore", divide="ignore"):
        out[..., :3] = np.where(
            out[..., 3:4] > 0,
            (rgba[..., :3] * src_a
             + plate[..., :3] * plate[..., 3:4] * (1 - src_a)) / np.maximum(out[..., 3:4], 1e-6),
            0)
    return to_image(drop_shadow(out, offset=3, blur=4.0, opacity=0.55))


def flanked(plate, gap=18):
    """Set a small rosette either side of a title, as an illuminated heading is."""
    ornament = rosette(int(plate.height * 0.52), 8, seed=77, enamel_rgb=(0x2E, 0x9E, 0x8C))
    width = plate.width + (ornament.width + gap) * 2
    canvas = Image.new("RGBA", (width, plate.height), (0, 0, 0, 0))
    middle = (plate.height - ornament.height) // 2
    canvas.alpha_composite(ornament, (0, middle))
    canvas.alpha_composite(plate, (ornament.width + gap, 0))
    canvas.alpha_composite(ornament, (width - ornament.width, middle))
    return canvas


def rosette(size, points, seed, enamel_rgb):
    """
    A chapter emblem. Each chapter gets a different fold count, so nine seals
    are nine distinct marks rather than nine numbers in nine identical circles.
    """
    s = size * SS
    img = Image.new("L", (s, s), 0)
    draw = ImageDraw.Draw(img)
    cx = cy = s / 2
    outer, inner = s * 0.46, s * 0.46 * 0.52
    pts = []
    for k in range(points * 2):
        r = outer if k % 2 == 0 else inner
        a = k * math.pi / points - math.pi / 2
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    draw.polygon(pts, fill=255)
    draw.ellipse([cx - s * 0.20, cy - s * 0.20, cx + s * 0.20, cy + s * 0.20], fill=255)
    mask = np.asarray(img.resize((size, size), Image.LANCZOS), dtype=np.float64) / 255.0

    # The enamel stone set into the middle of the seal.
    stone = Image.new("L", (s, s), 0)
    r = s * 0.155
    ImageDraw.Draw(stone).ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    stone = np.asarray(stone.resize((size, size), Image.LANCZOS), dtype=np.float64) / 255.0

    albedo, dents = hammered_gold(size, size, seed=seed)
    enamel = np.array(enamel_rgb, dtype=np.float64) / 255.0
    albedo = albedo * (1 - stone[..., None]) + enamel * stone[..., None]

    height = bevel_height(mask, size * 0.085) + dents * mask
    # The stone sits below the gold rim that holds it.
    height = height - stone * 0.35 + bevel_height(stone, size * 0.05) * stone * 0.25
    rgba = shade(height, albedo, mask, relief=3.4, ambient=0.46, spec_strength=0.7)
    rgba = add_rim(rgba, mask, colour=(0.26, 0.15, 0.03), width=1.6)
    return to_image(drop_shadow(rgba, offset=2, blur=2.4, opacity=0.45))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    written = []

    def save(name, image):
        path = OUT / name
        image.save(path, optimize=True)
        written.append((name, path.stat().st_size))

    # Buttons. Stretch through the middle only, so the bevelled corners and the
    # inlay frame keep their shape at every width.
    for name, surface in (
        ("btn_gold.9.png", gold_plaque()),
        ("btn_gold_pressed.9.png", gold_plaque(pressed=True)),
        ("btn_wood.9.png", wood_plaque()),
        ("btn_wood_pressed.9.png", wood_plaque(pressed=True)),
    ):
        w, h = surface.size
        save(name, nine_patch(surface,
                              stretch=((w // 2 - 2, w // 2 + 2), (h // 2 - 2, h // 2 + 2)),
                              padding=((12, w - 12), (10, h - 14))))

    # The panel: a larger, quieter wood field with the same inlay frame.
    # Quiet figure and wide bands: this surface has a menu printed on it.
    # Darker than the board behind it, so gold type lifts off it, with the
    # quiet figure and wide bands a surface carrying a menu needs.
    panel = wood_plaque(w=420, h=300, radius=34, bevel=13, inlay=10,
                        figure_contrast=0.42, figure_scale=1.9,
                        darkness=0.62, stud_radius=7)
    pw, ph = panel.size
    save("bg_royal_panel.9.png", nine_patch(
        panel,
        stretch=((pw // 2 - 2, pw // 2 + 2), (ph // 2 - 2, ph // 2 + 2)),
        padding=((16, pw - 16), (14, ph - 20))))

    # Nine chapter emblems: a different fold count and a different enamel for
    # each, so the nine seals are nine marks rather than nine numbered circles.
    enamels = [
        (0x2E, 0x8B, 0x7A),  # 1 home board — turquoise
        (0x9C, 0x3B, 0x2A),  # 2 tea house — brick
        (0x1F, 0x4E, 0x8C),  # 3 Istanbul — deep blue
        (0x2C, 0x6E, 0x4F),  # 4 Athens — olive green
        (0x6B, 0x2F, 0x6B),  # 5 Crete — plum
        (0x0F, 0x6B, 0x74),  # 6 Thessaloniki — sea
        (0x8A, 0x5A, 0x14),  # 7 the Caspian — amber
        (0x25, 0x4C, 0x7A),  # 8 the harbour — harbour blue
        (0x7E, 0x1C, 0x1C),  # 9 the grand master — garnet
    ]
    for index, enamel in enumerate(enamels):
        save(f"emblem_chapter_{index + 1}.png",
             rosette(112, 6 + index, seed=40 + index * 7, enamel_rgb=enamel))

    save("muqarnas_cornice.png", muqarnas())

    # Rarity, cast four ways. The card and the medallion share a material, so a
    # legendary item is gold all the way through rather than gold-labelled.
    for rarity, material in (("common", "bronze"), ("rare", "silver"),
                             ("epic", "turquoise"), ("legendary", "gold")):
        plaque = rarity_plaque(material)
        pw, ph = plaque.size
        save(f"card_{rarity}.9.png", nine_patch(
            plaque,
            stretch=((pw // 2 - 2, pw // 2 + 2), (ph // 2 - 2, ph // 2 + 2)),
            padding=((14, pw - 14), (12, ph - 16))))

        for mark in ("ic_royal_crown", "ic_royal_dice", "ic_royal_trophy",
                     "ic_royal_shop", "ic_royal_medal", "ic_royal_book"):
            save(f"medal_{mark.replace('ic_royal_', '')}_{rarity}.png",
                 product_medallion(mark, material))

    for icon in ("ic_royal_play", "ic_royal_duel", "ic_royal_road", "ic_royal_book",
                 "ic_royal_trophy", "ic_royal_gear", "ic_royal_coin", "ic_royal_medal",
                 "ic_royal_shop", "ic_royal_crown", "ic_royal_lock", "ic_royal_back",
                 "ic_royal_chevron", "ic_royal_pause", "ic_royal_restart",
                 "ic_royal_exit", "ic_royal_sound", "ic_royal_dice"):
        save(icon.replace("ic_royal_", "gold_") + ".png", gold_icon(icon))

    # One seal per season, in that season's own metal.
    for number, material in ((1, "ebony"), (2, "turquoise"),
                             (3, "nacre"), (4, "agate")):
        save(f"seal_season_{number}.png", season_seal(number, material))

    # Checker sets, named for what they are made of.
    for name, material in (("walnut", "walnut"), ("bone", "bone"),
                           ("khatam", "ebony"), ("nacre", "nacre"),
                           ("turquoise", "turquoise"), ("agate", "agate"),
                           ("gold", "gold")):
        save(f"checkers_{name}.png", checker_stack(material))

    # Dice, in the stones a real pair is cut from.
    for name, material in (("bone", "bone"), ("walnut", "walnut"), ("ebony", "ebony"),
                           ("turquoise", "turquoise"), ("agate", "agate"), ("gold", "gold")):
        save(f"dice_set_{name}.png", die(material))

    # Struck titles, one per language.
    for language, strings in TITLES.items():
        folder = OUT if language is None else RES / f"drawable-{language}-xhdpi"
        folder.mkdir(parents=True, exist_ok=True)
        for name, text in strings.items():
            plate = title_plate(text, FONTS / "mirza_bold.ttf",
                                112 if name == "title_app" else 96)
            if name == "title_app":
                plate = flanked(plate)
            path = folder / f"{name}.png"
            plate.save(path, optimize=True)
            written.append((f"{folder.name}/{name}.png", path.stat().st_size))

    total = sum(size for _, size in written)
    for name, size in written:
        print(f"{name:26} {size / 1024:6.1f} KB")
    print(f"{'total':26} {total / 1024:6.1f} KB")




# ── muqarnas ─────────────────────────────────────────────────────────────────

def pointed_arch(shape_hw, cx, base_y, half_width, sharpness=0.78):
    """
    A two-centred pointed arch, drawn the way a mason strikes one: two arcs of
    equal radius whose centres sit on the springing line, inside each other's
    circle. Their intersection above that line is the arch, and it comes to a
    real point rather than the rounded top an ellipse gives.

    Returns (mask, apex height).
    """
    h, w = shape_hw
    # r = 1.6a strikes the arch Persian masons use: tall, clearly pointed, and
    # still wide enough at the springing to read as a niche rather than a spike.
    radius = half_width * 1.6
    offset = radius - half_width
    apex = math.sqrt(max(radius * radius - offset * offset, 0.0))

    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    left = np.hypot(xx - (cx - offset), yy - base_y) <= radius
    right = np.hypot(xx - (cx + offset), yy - base_y) <= radius
    return (left & right & (yy <= base_y) & (yy >= base_y - apex)), apex


def product_medallion(icon_name, material, size=160):
    """
    A shop product as a struck medallion: the item's own mark raised on a disc
    cast in the material its rarity earns. This replaces the emoji the shop used
    to show, which rendered differently on every handset and matched nothing.
    """
    disc = Image.new("L", (size * SS, size * SS), 0)
    pad = size * SS * 0.04
    ImageDraw.Draw(disc).ellipse([pad, pad, size * SS - pad, size * SS - pad], fill=255)
    mask = np.asarray(disc.resize((size, size), Image.LANCZOS), dtype=np.float64) / 255.0

    albedo, dents = hammered(size, size, material, seed=13)
    height = bevel_height(mask, size * 0.075) + dents * mask
    # A rim standing around the field, as a struck coin has.
    height = engrave(height, mask, inset=size * 0.13, width=size * 0.022, depth=0.5)

    # The mark itself, raised out of the field.
    glyph = vector_mask(icon_name, int(size * 0.56))
    inset = (size - glyph.shape[0]) // 2
    raised = np.zeros_like(mask)
    raised[inset:inset + glyph.shape[0], inset:inset + glyph.shape[1]] = glyph
    raised = raised * mask
    height = height + bevel_height(raised, size * 0.03) * raised * 0.85

    rgba = shade(height, albedo, mask, relief=3.4, ambient=0.48,
                 key=0.70, spec_strength=0.72, spec_power=22)
    rgba = add_rim(rgba, mask, colour=(0.18, 0.11, 0.03), width=1.8)
    return to_image(drop_shadow(rgba, offset=3, blur=3.0, opacity=0.5))


def checker_stack(material, motif="ic_shamsa", size=170, seed=29):
    """
    A pair of playing pieces, which is what a backgammon game should be selling
    and was not. A checker is a turned disc with a motif cut into its face, so it
    is exactly what this renderer is for: the same lighting, a different metal or
    wood, and the shamsa sunk into the top.

    Two of them, the back one offset, because a single disc reads as a coin.
    """
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    disc_size = int(size * 0.70)

    def one(scale, dim):
        s = disc_size
        art = Image.new("L", (s * SS, s * SS), 0)
        pad = s * SS * 0.03
        ImageDraw.Draw(art).ellipse([pad, pad, s * SS - pad, s * SS - pad], fill=255)
        mask = np.asarray(art.resize((s, s), Image.LANCZOS), dtype=np.float64) / 255.0

        albedo, grain_field = hammered(s, s, material, seed=seed)
        albedo = albedo * dim
        height = bevel_height(mask, s * 0.10) + grain_field * mask
        # The turned rings of a lathe, and the seat the motif sits in.
        height = engrave(height, mask, inset=s * 0.10, width=s * 0.018, depth=0.45)
        height = engrave(height, mask, inset=s * 0.16, width=s * 0.012, depth=0.25)

        glyph = vector_mask(motif, int(s * 0.44))
        inset = (s - glyph.shape[0]) // 2
        cut = np.zeros_like(mask)
        cut[inset:inset + glyph.shape[0], inset:inset + glyph.shape[1]] = glyph
        cut = cut * mask
        # Sunk, not raised: an inlay is set into the face.
        height = height - bevel_height(cut, s * 0.025) * cut * 0.7

        rgba = shade(height, albedo, mask, relief=3.6, ambient=0.46,
                     key=0.70, spec_strength=0.66, spec_power=24)
        rgba = add_rim(rgba, mask, colour=(0.14, 0.09, 0.04), width=1.6)
        return to_image(drop_shadow(rgba, offset=3, blur=3.0, opacity=0.5))

    back = one(1.0, 0.72)
    front = one(1.0, 1.0)
    canvas.alpha_composite(back, (int(size * 0.28), int(size * 0.04)))
    canvas.alpha_composite(front, (int(size * 0.02), int(size * 0.26)))
    return canvas


def die(material, size=180, seed=37, faces=(5, 3, 6)):
    """
    A die as an object rather than a symbol: a cube in axonometric projection,
    three faces visible, pips sunk into them.

    Each face gets its own constant normal, which is what a flat face has, so
    the three read at three different brightnesses under the one key light this
    whole theme is lit by — top brightest, then the left cheek, then the right.
    The pips are drilled, not printed: a depression with the light caught on its
    far wall.
    """
    canvas = np.zeros((size, size, 4))
    s = size * 0.40          # edge length in projection
    # Both axes run down-and-out from the top vertex; with a negative rise the
    # top face folds behind the cube and you get a banner instead of a die.
    ux, uy = math.cos(math.radians(30)), math.sin(math.radians(30))
    vx, vy = -ux, uy
    cx, cy = size * 0.5, size * 0.17

    apex = np.array([cx, cy])
    right = apex + np.array([ux, uy]) * s
    left = apex + np.array([vx, vy]) * s
    front = right + np.array([vx, vy]) * s
    down = np.array([0.0, s * 1.02])

    def polygon_mask(points):
        img = Image.new("L", (size * SS, size * SS), 0)
        ImageDraw.Draw(img).polygon([(p[0] * SS, p[1] * SS) for p in points], fill=255)
        return np.asarray(img.resize((size, size), Image.LANCZOS), dtype=np.float64) / 255.0

    # Brightness of each face under the shared key light, top lit most.
    faces_geometry = [
        ("top", [apex, right, front, left], 1.00,
         (np.array([ux, uy]) * s, np.array([vx, vy]) * s)),
        ("left", [left, front, front + down, left + down], 0.78,
         (np.array([ux, uy]) * s, down)),
        ("right", [front, right, right + down, front + down], 0.58,
         (np.array([vx, vy]) * -s, down)),
    ]

    albedo_full, _ = hammered(size, size, material, seed=seed)

    for (name, points, lit, (axis_a, axis_b)), pip_count in zip(faces_geometry, faces):
        mask = polygon_mask(points)
        if not mask.any():
            continue
        origin = np.array(points[0], dtype=float)

        # Pips, in the face's own coordinates, then projected onto the screen.
        pip_layout = {
            1: [(0.5, 0.5)],
            2: [(0.28, 0.28), (0.72, 0.72)],
            3: [(0.24, 0.24), (0.5, 0.5), (0.76, 0.76)],
            4: [(0.28, 0.28), (0.72, 0.28), (0.28, 0.72), (0.72, 0.72)],
            5: [(0.26, 0.26), (0.74, 0.26), (0.5, 0.5), (0.26, 0.74), (0.74, 0.74)],
            6: [(0.26, 0.22), (0.26, 0.5), (0.26, 0.78),
                (0.74, 0.22), (0.74, 0.5), (0.74, 0.78)],
        }[pip_count]

        pips = Image.new("L", (size * SS, size * SS), 0)
        artist = ImageDraw.Draw(pips)
        radius = s * 0.115 * SS
        for a, b in pip_layout:
            centre = origin + axis_a * a + axis_b * b
            artist.ellipse([centre[0] * SS - radius, centre[1] * SS - radius,
                            centre[0] * SS + radius, centre[1] * SS + radius], fill=255)
        pip_mask = np.asarray(pips.resize((size, size), Image.LANCZOS),
                              dtype=np.float64) / 255.0 * mask

        height = bevel_height(mask, size * 0.022)
        height = height - bevel_height(pip_mask, size * 0.020) * pip_mask * 1.35

        face = shade(height, albedo_full * lit, mask,
                     relief=3.0, ambient=0.50, key=0.62,
                     spec_strength=0.55 if name == "top" else 0.30, spec_power=26)
        canvas = np.where(face[..., 3:4] > 0.01, face, canvas)

    rgba = add_rim(canvas, canvas[..., 3], colour=(0.14, 0.08, 0.03), width=1.6)
    return to_image(drop_shadow(rgba, offset=4, blur=3.6, opacity=0.5))


def rarity_plaque(material, w=280, h=150, radius=22, bevel=9):
    """The card a product sits on, cast in the same material as its medallion."""
    mask = rounded_mask(w, h, radius)
    metal, dents = hammered(h, w, material, seed=21)
    # Quiet field: the product is what should be looked at, not the grain.
    wood, figure = walnut(h, w, contrast=0.28, scale=2.2)
    wood = wood * 0.72

    # A metal frame around a wood field: the product is mounted, not printed.
    inlay = 6
    inner = rounded_mask(w - 2 * inlay, h - 2 * inlay, max(radius - inlay, 2))
    inner_full = np.zeros_like(mask)
    inner_full[inlay:h - inlay, inlay:w - inlay] = inner
    frame = np.clip(mask - inner_full, 0, 1)
    frame_band = ndimage.gaussian_filter(frame, 0.6)

    albedo = wood * (1 - frame_band[..., None]) + metal * frame_band[..., None]
    height = bevel_height(mask, bevel) + figure * mask
    height = height + frame_band * 0.6 + dents * frame_band
    height = engrave(height, mask, inset=inlay + 4, width=1.8, depth=0.26)

    rgba = shade(height, albedo, mask, relief=3.0, ambient=0.46,
                 key=0.64, spec_strength=0.42, spec_power=20)
    rgba = add_rim(rgba, mask, colour=(0.12, 0.07, 0.03), width=1.8)
    return to_image(drop_shadow(rgba, offset=3, blur=3.0, opacity=0.5))


def season_seal(number, material, size=150, seed=53):
    """
    The seal stamped on anything from a season: a scalloped wax stamp with the
    season's number struck into it.

    It is what makes a retired item worth owning — a piece from a closed season
    carries a mark nobody can earn any more, and a mark has to look pressed into
    the thing rather than printed on it, so the number is sunk, not raised.
    """
    s = size * SS
    art = Image.new("L", (s, s), 0)
    artist = ImageDraw.Draw(art)
    cx = cy = s / 2
    outer = s * 0.45

    # A scalloped edge, the way wax spreads under a stamp.
    lobes = 12
    points = []
    for step in range(lobes * 24):
        angle = 2 * math.pi * step / (lobes * 24)
        radius = outer * (0.92 + 0.08 * math.cos(angle * lobes))
        points.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    artist.polygon(points, fill=255)
    mask = np.asarray(art.resize((size, size), Image.LANCZOS), dtype=np.float64) / 255.0

    albedo, dents = hammered(size, size, material, seed=seed)
    height = bevel_height(mask, size * 0.09) + dents * mask
    height = engrave(height, mask, inset=size * 0.11, width=size * 0.018, depth=0.4)

    # The number, pressed in.
    font = ImageFont.truetype(str(FONTS / "lalezar_regular.ttf"), int(size * 0.42))
    glyphs = Image.new("L", (size, size), 0)
    drawer = ImageDraw.Draw(glyphs)
    text = str(number)
    box = drawer.textbbox((0, 0), text, font=font)
    drawer.text(((size - (box[2] - box[0])) / 2 - box[0],
                 (size - (box[3] - box[1])) / 2 - box[1]), text, font=font, fill=255)
    pressed = np.asarray(glyphs, dtype=np.float64) / 255.0 * mask
    height = height - bevel_height(pressed, size * 0.025) * pressed * 0.9

    rgba = shade(height, albedo, mask, relief=3.6, ambient=0.46,
                 key=0.72, spec_strength=0.68, spec_power=22)
    rgba = add_rim(rgba, mask, colour=(0.16, 0.09, 0.03), width=1.6)
    return to_image(drop_shadow(rgba, offset=3, blur=2.8, opacity=0.5))


def muqarnas(width=880, tiers=4, base_cell=118, seed=3):
    """
    A muqarnas cornice — the stalactite vaulting over a doorway in Persian
    architecture, and the richest thing in this visual language that can be
    built rather than painted.

    It is honest geometry. Each tier is a row of pointed niches; each niche is
    concave, so its head falls away into shadow while its lower lip catches the
    key light — which is exactly how the real thing reads. Tiers step inward and
    are offset by half a cell, so the niches nest into the row above and the
    whole thing corbels out as it descends.

    The concavity comes from the distance transform of each niche: depth grows
    with distance from the niche's own edge, so the bowl follows the arch
    instead of being a sphere pasted inside it.
    """
    # Tall enough for every tier: each contributes most of its own apex.
    half0 = base_cell * 0.5
    height = int(sum(half0 * (0.82 ** k) * 1.48 * 0.72 for k in range(tiers)) + half0 * 0.9)
    field = np.zeros((height, width))
    mask = np.zeros((height, width))

    base_y = float(height - 2)
    for tier in range(tiers):
        half = base_cell * 0.5 * (0.82 ** tier)
        step = half * 2.0
        offset = half if tier % 2 else 0.0
        # Each tier is shallower than the one below it.
        relief_scale = 1.0 - tier * 0.13

        apex_used = 0.0
        columns = int(width / step) + 3
        for index in range(-1, columns):
            cx = index * step + offset + half
            niche, apex = pointed_arch((height, width), cx, base_y, half)
            if not niche.any():
                continue
            apex_used = apex

            inside = ndimage.distance_transform_edt(niche)
            reach = max(inside.max(), 1e-6)
            bowl = inside / reach

            # Sunk in the middle, with the arch edge standing proud as its rim.
            depth = (bowl ** 0.55) * 1.9 * relief_scale
            rim = np.clip(1.0 - inside / 3.0, 0, 1) * niche * 1.1 * relief_scale

            cell = depth + rim + tier * 0.18
            field = np.where(niche, np.maximum(field, cell), field)
            mask = np.where(niche, 1.0, mask)

        base_y -= apex_used * 0.72

    # Behind the cells sits the ground they are corbelled off, set well back so
    # it reads as the shadow between stalactites rather than as a hole.
    carved = mask > 0.5
    field = np.where(carved, field, -1.6)
    mask = np.ones((height, width))

    albedo, dents = hammered_gold(height, width, seed=seed)
    albedo = np.where(carved[..., None], albedo, albedo * 0.45)
    field = ndimage.gaussian_filter(field, 0.7) + dents * mask
    rgba = shade(field, albedo, mask, relief=4.2, ambient=0.34,
                 key=0.84, spec_strength=0.58, spec_power=14)
    rgba = add_rim(rgba, mask, colour=(0.18, 0.10, 0.02), width=1.4)
    return to_image(drop_shadow(rgba, offset=3, blur=3.0, opacity=0.55))

if __name__ == "__main__":
    main()
