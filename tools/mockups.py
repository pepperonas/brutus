#!/usr/bin/env python3
"""Compose the README's screenshot strip from raw device captures.

Ported from Flipper the Ripper's tools/mockups.py, in Brutus's colours. Regenerating the strip is one
command, so it can move with the app instead of aging.

    python3 tools/mockups.py out.png shot.png:"Caption" shot2.png:"Another"

Frames are drawn, not photographed: a rounded dark bezel with a hairline edge, the capture inset so
the corners of the screen are rounded too. Nothing here needs a device — it works on whatever PNGs
it is handed.
"""
from __future__ import annotations

import sys
from PIL import Image, ImageDraw, ImageFont

BG = (20, 17, 16)          # the product page background (#141110)
BEZEL = (43, 40, 39)
EDGE = (83, 67, 65)
CAPTION = (237, 224, 221)

PANEL_W = 420          # width of the screen area inside a frame
BEZEL_PAD = 14         # bezel thickness around the screen
FRAME_RADIUS = 46
SCREEN_RADIUS = 32
GAP = 34               # between frames
MARGIN = 40
CAPTION_GAP = 26
CAPTION_SIZE = 26


def _font(size: int) -> ImageFont.FreeTypeFont:
    for path in (
        "/System/Library/Fonts/SFNS.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/Library/Fonts/Arial.ttf",
    ):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default(size)


def _rounded(image: Image.Image, radius: int) -> Image.Image:
    """Round the corners of a capture so it sits in the bezel like a real screen."""
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, *image.size), radius=radius, fill=255)
    out = Image.new("RGBA", image.size)
    out.paste(image, (0, 0), mask)
    return out


def frame(shot: Image.Image) -> Image.Image:
    screen_h = round(shot.height * PANEL_W / shot.width)
    screen = _rounded(shot.convert("RGB").resize((PANEL_W, screen_h), Image.LANCZOS), SCREEN_RADIUS)
    w, h = PANEL_W + 2 * BEZEL_PAD, screen_h + 2 * BEZEL_PAD
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((0, 0, w - 1, h - 1), radius=FRAME_RADIUS, fill=BEZEL, outline=EDGE, width=2)
    canvas.paste(screen, (BEZEL_PAD, BEZEL_PAD), screen)
    return canvas


def compose(pairs: list[tuple[str, str]], out: str) -> None:
    frames = [(frame(Image.open(path)), caption) for path, caption in pairs]
    fh = max(f.height for f, _ in frames)
    font = _font(CAPTION_SIZE)
    total_w = MARGIN * 2 + sum(f.width for f, _ in frames) + GAP * (len(frames) - 1)
    total_h = MARGIN * 2 + fh + CAPTION_GAP + CAPTION_SIZE + 10
    sheet = Image.new("RGB", (total_w, total_h), BG)
    draw = ImageDraw.Draw(sheet)
    x = MARGIN
    for img, caption in frames:
        sheet.paste(img, (x, MARGIN), img)
        text_w = draw.textlength(caption, font=font)
        draw.text((x + (img.width - text_w) / 2, MARGIN + fh + CAPTION_GAP), caption, font=font, fill=CAPTION)
        x += img.width + GAP
    if out.lower().endswith((".jpg", ".jpeg")):
        sheet.save(out, quality=88, optimize=True, progressive=True)
    else:
        sheet.save(out, optimize=True)
    print(f"{out}: {sheet.width}x{sheet.height} from {len(frames)} captures")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    compose([tuple(a.split(":", 1)) for a in sys.argv[2:]], sys.argv[1])
