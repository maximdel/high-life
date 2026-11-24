"""High Life mod icon in the Hopper release-kit style: 32x32 pixel art on a rounded tile, upscaled to 128x128."""
import math, os, sys
from PIL import Image, ImageDraw

N = 32
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "highlife_logo.png")

im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.rounded_rectangle([0, 0, N - 1, N - 1], radius=5, fill=(40, 22, 66))
d.rounded_rectangle([1, 1, N - 2, N - 2], radius=4, fill=(74, 40, 120))
# rainbow glow rings behind the leaf
cx, cy = 15.5, 15.5
for y in range(2, N - 2):
    for x in range(2, N - 2):
        r = math.hypot(x - cx, y - cy)
        if 10.5 < r < 13.2:
            hue = (math.degrees(math.atan2(y - cy, x - cx)) + 360) % 360 / 360
            import colorsys
            c = colorsys.hsv_to_rgb(hue, 0.55, 0.95)
            im.putpixel((x, y), tuple(int(v * 255) for v in c) + (255,))

# 7-leaflet fan leaf
def leaflet(ang, length, col):
    a = math.radians(ang)
    dx, dy = math.sin(a), -math.cos(a)
    for i in range(int(length * 3) + 1):
        t = i / (length * 3)
        px, py = cx + dx * length * t, 21 + dy * length * t
        w = 1.3 * math.sin(math.pi * min(1, t * 1.1))
        for k in (-1, 0, 1):
            if abs(k) <= w:
                qx, qy = int(round(px - dy * k)), int(round(py + dx * k))
                if 0 <= qx < N and 0 <= qy < N:
                    im.putpixel((qx, qy), col if k <= 0 else tuple(int(v * 0.8) for v in col[:3]) + (255,))
for ang, length in ((0, 15), (-32, 12.5), (32, 12.5), (-64, 9), (64, 9), (-96, 5.5), (96, 5.5)):
    leaflet(ang, length, (98, 200, 72, 255) if ang == 0 else (78, 176, 58, 255))
d.line([(15, 21), (15, 28)], fill=(50, 110, 40))
d.line([(16, 21), (16, 28)], fill=(40, 90, 32))
im.resize((128, 128), Image.NEAREST).save(OUT)
print("icon ->", OUT)
