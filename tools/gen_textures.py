"""Procedural 16x16 pixel art for High Life. Run: python tools/gen_textures.py [preview.png]"""
import math, os, random, sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "highlife", "textures")
T = (0, 0, 0, 0)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


class Canvas:
    def __init__(self, w=16, h=16):
        self.img = Image.new("RGBA", (w, h), T)
        self.w, self.h = w, h

    def put(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), c)

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, c)

    def ellipse(self, cx, cy, rx, ry, base, light=1.25, dark=0.7):
        for y in range(self.h):
            for x in range(self.w):
                dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
                d = dx * dx + dy * dy
                if d <= 1.0:
                    f = 1.0 + (light - 1) * max(0, -dx - dy) * 0.7 - (1 - dark) * max(0, dx + dy) * 0.7
                    self.put(x, y, shade(base, f))

    def line(self, x0, y0, x1, y1, c):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            t = i / max(1, n)
            self.put(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, c)

    def leaflet(self, x, y, ang, length, c, width=1.0):
        """Tapered leaf from (x,y) in direction ang (degrees, 0 = up)."""
        a = math.radians(ang)
        dx, dy = math.sin(a), -math.cos(a)
        steps = int(length * 2) + 1
        for i in range(steps):
            t = i / max(1, steps - 1)
            px, py = x + dx * length * t, y + dy * length * t
            wdt = width * math.sin(math.pi * min(1, t * 1.15))
            self.put(px, py, c)
            if wdt > 0.45:
                self.put(px - dy * 0.8, py + dx * 0.8, shade(c, 0.8))

    def outline(self, color=None, factor=0.45):
        """Darken transparent pixels next to filled ones -> crisp item outline."""
        src = self.img.copy()
        for y in range(self.h):
            for x in range(self.w):
                if src.getpixel((x, y))[3] == 0:
                    for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                        if 0 <= nx < self.w and 0 <= ny < self.h and src.getpixel((nx, ny))[3] > 200:
                            n = src.getpixel((nx, ny))
                            self.img.putpixel((x, y), color or shade(n, factor))
                            break
        return self

    def save(self, rel):
        path = os.path.join(ROOT, rel + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)
        OUT.append((rel, self.img))


OUT = []
R = random.Random(420)

G1, G2, G3 = hexc("3E8E2F"), hexc("57B03E"), hexc("2A6B22")
ORANGE, FROST = hexc("E08A2C"), hexc("D8F0C8")
BROWN, DBROWN = hexc("7A5230"), hexc("4E3420")


# ---------------- cannabis ----------------
def fan_leaf(c, x, y, size, col, spread=36, n=5, width=0.4):
    half = n // 2
    for a in range(-half, half + 1):
        length = size * (1.0 - abs(a) * (0.5 / half))
        c.leaflet(x, y, a * spread, length, col if a else shade(col, 1.12), width=width)


def cannabis_plant(stage):
    c = Canvas()
    heights = [4, 8, 12, 14]
    top = 16 - heights[stage]
    c.line(8, 15, 8, top + 1, hexc("4F7A2A"))
    if stage == 0:
        c.leaflet(8, 13, -55, 2.5, G2); c.leaflet(8, 13, 55, 2.5, G2); c.leaflet(8, 13, 0, 2, G1)
        return c
    nodes = {1: [(12, 3)], 2: [(13, 4), (9, 3.5)], 3: [(13, 4.5), (10, 4), (7, 3.2)]}[stage]
    for i, (ny, size) in enumerate(nodes):
        side = -1 if i % 2 == 0 else 1
        fan_leaf(c, 8 - 1, ny, size, G1 if i % 2 else G2, spread=40)
        fan_leaf(c, 8 + 1, ny - 1, size * 0.9, G2 if i % 2 else G1, spread=40)
        c.leaflet(8, ny, -95, size * 0.9, G3); c.leaflet(8, ny, 95, size * 0.9, G3)
    fan_leaf(c, 8, top + 3, 3.2 if stage < 3 else 2.5, G2, spread=34)
    if stage == 3:  # flowering colas
        c.ellipse(8, top + 3, 1.7, 3.4, hexc("78C452"))
        for (bx, by) in ((5, 8), (11, 7)):
            c.ellipse(bx, by, 1.2, 2.0, hexc("6DBA4A"))
        for _ in range(9):
            x, y = R.randint(3, 12), R.randint(top, 13)
            p = c.get(x, y)
            if p[3] and p[1] > 150:
                c.put(x, y, ORANGE if R.random() < 0.6 else FROST)
    return c


def bud(base, hair, frost, stem):
    c = Canvas()
    c.line(8, 15, 8, 12, stem)
    for (x, y, rx, ry) in ((8, 7, 4.2, 5.6), (5.5, 9.5, 2.5, 3), (10.5, 9.5, 2.5, 3), (8, 3.5, 2.4, 2.6)):
        c.ellipse(x, y, rx, ry, base)
    for _ in range(26):
        x, y = R.randint(3, 12), R.randint(2, 13)
        if c.get(x, y)[3]:
            c.put(x, y, hair if R.random() < 0.55 else frost)
    c.leaflet(4, 6, -60, 3, shade(base, 0.8)); c.leaflet(12, 6, 60, 3, shade(base, 0.8))
    return c.outline()


for s in range(4):
    cannabis_plant(s).save(f"block/cannabis_stage{s}")
bud(hexc("5DAA3E"), ORANGE, FROST, hexc("4F7A2A")).save("item/cannabis_bud")
bud(hexc("7C8A3A"), hexc("B5702A"), hexc("C9D3A0"), hexc("6B5A2E")).save("item/dried_cannabis")

c = Canvas()  # ground cannabis pile
for y in range(8, 15):
    half = int(6.5 * math.sqrt(max(0, 1 - ((y - 14.5) / 6.5) ** 2)))
    for x in range(8 - half, 8 + half + 1):
        r = R.random()
        c.put(x, y, hexc("6F8B35") if r < 0.45 else hexc("87A346") if r < 0.75 else hexc("556B28") if r < 0.93 else hexc("B5702A"))
c.outline().save("item/ground_cannabis")

c = Canvas()  # seeds (generic helper)
def seeds(col, stripe, positions, rx=1.6, ry=2.1):
    c = Canvas()
    for (x, y) in positions:
        c.ellipse(x, y, rx, ry, col)
        c.put(x - 0.5, y - 0.5, stripe)
    return c.outline()

seeds(hexc("8A7B5E"), hexc("4E4434"), [(5, 6), (10, 5), (7.5, 11), (12, 10)]).save("item/cannabis_seeds")
seeds(hexc("6B4A2A"), hexc("3B2814"), [(4, 5), (8, 4), (11, 7), (6, 9), (10, 11), (5, 13), (13, 12)], 1.1, 1.3).save("item/tobacco_seeds")
seeds(hexc("C2312B"), hexc("F08070"), [(5.5, 6), (10.5, 6.5), (8, 11)], 2.2, 2.4).save("item/coca_seeds")

# grinder (top view)
c = Canvas()
c.ellipse(8, 8.5, 6.6, 6.2, hexc("8C9196"), light=1.4, dark=0.6)
c.ellipse(8, 8, 5.2, 4.8, hexc("A7ADB3"), light=1.3, dark=0.75)
for a in range(0, 360, 45):
    x, y = 8 + math.cos(math.radians(a)) * 6.2, 8.5 + math.sin(math.radians(a)) * 5.8
    c.put(x, y, hexc("5E6368"))
c.ellipse(8, 8, 1.6, 1.5, hexc("4FAE3C"))
c.outline().save("item/grinder")


# joint
def joint(lit):
    c = Canvas()
    for i in range(11):  # diagonal cone, thin at bottom-left (filter) to fat at tip
        x, y = 3 + i, 13 - i
        wdt = 0 if i < 3 else 1
        col = hexc("F2EEE2") if i >= 3 else hexc("D9C7A0")
        c.put(x, y, col)
        if wdt: c.put(x + 1, y + 1, shade(col, 0.85)); c.put(x - 0.0, y + 1, shade(col, 0.92))
    c.put(13, 2, hexc("E8E0CC")); c.put(14, 1, hexc("E8E0CC"))  # twisted tip
    if lit:
        c.put(13, 3, hexc("8C8C8C")); c.put(14, 2, hexc("FF7A1A")); c.put(13, 2, hexc("FFB347")); c.put(14, 3, hexc("D2401A"))
        c.put(15, 0, hexc("B0B0B0", 160)); c.put(15, 1, hexc("A0A0A0", 120))
    for i in range(4):
        c.put(6 + i * 2, 11 - i * 2, hexc("7E9A45"))  # weed showing through
    return c.outline(factor=0.5)

joint(False).save("item/joint")
joint(True).save("item/lit_joint")

# bong
c = Canvas()
glass, gdark, water = hexc("B9E3F0", 220), hexc("7FB8CC", 230), hexc("4A8FD0", 230)
c.rect(7, 1, 9, 10, glass); c.line(7, 1, 7, 10, gdark)
c.ellipse(8, 12.3, 4.4, 3.2, glass, light=1.1, dark=0.85)
for y in range(12, 15):
    for x in range(4, 13):
        if c.get(x, y)[3]: c.put(x, y, water)
c.line(5, 8, 3, 6, gdark); c.rect(1, 4, 3, 5, hexc("6F8B35")); c.put(2, 6, gdark)  # downstem + bowl with weed
c.put(8, 1, hexc("E6F7FC")); c.put(9, 3, hexc("FFFFFF"))
c.outline(factor=0.6).save("item/bong")

# cannabutter
c = Canvas()
top, side, front = hexc("E8E07A"), hexc("B9B356"), hexc("D2CA62")
for y in range(5, 9):
    for x in range(3 + (8 - y), 13 + (8 - y) - 3):
        c.put(x, y, top)
c.rect(2, 9, 11, 13, front); c.rect(12, 6, 13, 12, side); c.put(12, 13, side)
for (x, y) in ((5, 10), (8, 11), (10, 9), (7, 6), (10, 7)):
    c.put(x, y, hexc("7FA040"))
c.outline().save("item/cannabutter")

# pot brownie
c = Canvas()
c.rect(2, 6, 13, 13, hexc("5B3A22")); c.rect(2, 5, 13, 6, hexc("6E4930"))
for x in range(2, 14):
    c.put(x, 13, hexc("432A18"))
for _ in range(14):
    c.put(R.randint(3, 12), R.randint(6, 12), hexc("7A5236") if R.random() < .6 else hexc("3E2615"))
for (x, y) in ((5, 7), (10, 9), (7, 11)):
    c.put(x, y, hexc("6FA042"))
c.outline().save("item/pot_brownie")


# magic mushroom (cross block + item)
def mushroom(c, x, base_y, h, cap_w, cap):
    c.line(x, base_y, x, base_y - h, hexc("EEE6D2")); c.put(x + 1, base_y, hexc("D8CCB0"))
    c.ellipse(x + 0.5, base_y - h - 0.5, cap_w, 2.2, cap, light=1.35)
    c.put(x - 1, base_y - h - 1, hexc("FFF2C0"))
    c.put(x + cap_w - 1, base_y - h, hexc("5A86C8"))  # blue bruising

c = Canvas()
mushroom(c, 6, 15, 7, 3.4, hexc("C8913C"))
mushroom(c, 11, 15, 4, 2.6, hexc("B87F35"))
c.save("block/magic_mushroom")

# mushroom tea (bowl)
c = Canvas()
c.ellipse(8, 9.5, 6.5, 2.2, hexc("A06AD8"), light=1.3)
for x in range(2, 15):
    for y in range(10, 15):
        dx = (x + 0.5 - 8) / 6.5
        if (y - 9.5) <= 4.5 * math.sqrt(max(0, 1 - dx * dx)):
            c.put(x, y, hexc("8A5A32") if y > 10 else hexc("A06AD8"))
c.line(3, 11, 13, 11, hexc("6E4628"))
c.put(6, 9, hexc("D7B0FF")); c.put(10, 9, hexc("C8913C")); c.put(9, 10, hexc("C8913C"))
c.outline().save("item/mushroom_tea")

# tobacco
def broad_leaf(c, x0, y0, x1, y1, col, rib):
    length = math.dist((x0, y0), (x1, y1))
    ang = math.atan2(y1 - y0, x1 - x0)
    for i in range(int(length * 2) + 1):
        t = i / (length * 2)
        px, py = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        wdt = 3.2 * math.sin(math.pi * t) ** 0.8
        for k in range(-int(wdt * 2), int(wdt * 2) + 1):
            o = k / 2
            c.put(px - math.sin(ang) * o, py + math.cos(ang) * o, col if o < 0 else shade(col, 0.85))
    c.line(x0, y0, x1, y1, rib)

c = Canvas(); broad_leaf(c, 3, 14, 13, 2, hexc("5C9A3A"), hexc("A8C978")); c.outline().save("item/tobacco_leaf")
c = Canvas(); broad_leaf(c, 3, 14, 13, 2, hexc("A9742E"), hexc("D9B070"))
for _ in range(10):
    x, y = R.randint(3, 13), R.randint(2, 14)
    if c.get(x, y)[3]: c.put(x, y, hexc("8A5A22"))
c.outline().save("item/dried_tobacco")


def tobacco_plant(stage):
    c = Canvas()
    hgt = [4, 8, 12, 15][stage]
    for x in (4, 12):
        c.line(x, 15, x, 16 - hgt, hexc("5F8A3A"))
        leaves = max(1, hgt // 4)
        for i in range(leaves):
            y = 15 - i * 4 - 1
            sz = 3.5 - i * 0.5
            broad_leaf(c, x, y, x - sz, y - sz * 0.8, hexc("5C9A3A"), hexc("A8C978"))
            broad_leaf(c, x, y - 1, x + sz, y - 1 - sz * 0.8, hexc("4E8C33"), hexc("A8C978"))
        if stage == 3:
            for (dx, dy) in ((0, 0), (-1, 1), (1, 1)):
                c.put(x + dx, 16 - hgt + dy, hexc("F29BC4"))
    return c

for s in range(4):
    tobacco_plant(s).save(f"block/tobacco_stage{s}")

# coca
def coca_leaves(c, positions, col):
    for (x, y, a) in positions:
        c.leaflet(x, y, a, 3, col, width=1.3)

c = Canvas(); coca_leaves(c, [(8, 13, -40), (8, 13, 40), (8, 9, -10), (6, 9, -70), (10, 9, 70), (8, 6, 0)], hexc("4FA83A"))
c.line(8, 15, 8, 6, hexc("7A5A3A")); c.outline().save("item/coca_leaf")
c = Canvas(); coca_leaves(c, [(8, 13, -40), (8, 13, 40), (8, 9, -10), (6, 9, -70), (10, 9, 70), (8, 6, 0)], hexc("8C9A4A"))
c.line(8, 15, 8, 6, hexc("6B5A3A")); c.outline().save("item/dried_coca_leaf")


def coca_plant(stage):
    c = Canvas()
    hgt = [4, 8, 11, 14][stage]
    c.line(8, 15, 8, 16 - hgt, hexc("6B4A2E"))
    c.line(8, 12, 4, 16 - hgt + 2, hexc("6B4A2E")); c.line(8, 12, 12, 16 - hgt + 2, hexc("6B4A2E"))
    rr = random.Random(stage)
    for _ in range(4 + stage * 6):
        x = rr.uniform(3, 13); y = rr.uniform(16 - hgt, 13)
        c.leaflet(x, y, rr.uniform(-80, 80), 2, hexc("4FA83A") if rr.random() < .6 else hexc("3D8C2E"), width=1.2)
    if stage == 3:
        for _ in range(7):
            c.put(rr.randint(4, 12), rr.randint(16 - hgt + 1, 12), hexc("D8322A"))
    return c

for s in range(4):
    coca_plant(s).save(f"block/coca_stage{s}")

# bag of cocaine
c = Canvas()
c.rect(4, 3, 11, 13, hexc("DDE8EE", 150))
c.rect(4, 8, 11, 13, hexc("F7F7F7")); c.rect(5, 7, 10, 7, hexc("F0F0F0"))
c.line(4, 4, 11, 4, hexc("D23C3C")); c.line(4, 3, 11, 3, hexc("F2F6F8", 200))
c.put(10, 9, hexc("FFFFFF")); c.put(5, 12, hexc("E2E2E2"))
c.outline(factor=0.55).save("item/cocaine")


# ---------------- effect icons (18x18) ----------------
def icon():
    return Canvas(18, 18)

c = icon(); fan_leaf(c, 9, 12, 8, hexc("5DBB3F"), spread=33, n=7, width=0.9); c.line(9, 13, 9, 17, hexc("3E7A2A")); c.outline().save("mob_effect/high")
c = icon()
for i, col in enumerate(("FF4D4D", "FFA54D", "FFF04D", "5DFF6A", "4DC3FF", "B44DFF")):
    for a in range(0, 360, 6):
        r = 7.5 - i * 1.1
        c.put(9 + math.cos(math.radians(a + i * 20)) * r, 9 + math.sin(math.radians(a + i * 20)) * r, hexc(col))
c.save("mob_effect/tripping")
c = icon()
for (x0, y0, x1, y1) in ((11, 1, 6, 9), (6, 9, 12, 9), (12, 9, 7, 17)):
    c.line(x0, y0, x1, y1, hexc("FFFFFF")); c.line(x0 + 1, y0, x1 + 1, y1, hexc("D8E4EC"))
c.outline(color=hexc("6B7A88")).save("mob_effect/wired")
c = icon(); c.ellipse(9, 6, 6, 3.5, hexc("7A7A88")); c.ellipse(6, 7, 3.5, 2.5, hexc("8A8A98"))
for x in (5, 9, 13):
    c.line(x, 11, x - 1, 15, hexc("6A8AB0"))
c.outline().save("mob_effect/comedown")
c = icon(); c.rect(3, 6, 14, 14, hexc("5B3A22")); c.rect(3, 5, 14, 6, hexc("6E4930"))
c.put(7, 9, hexc("6FA042")); c.put(11, 11, hexc("6FA042")); c.outline().save("mob_effect/edible_onset")

# ---------------- v1.1: salvia + party drugs ----------------
SAGE, SAGE_D = hexc("6E9E62"), hexc("4C7A46")

def salvia_leaf(c, x0, y0, x1, y1, col):
    broad_leaf(c, x0, y0, x1, y1, col, shade(col, 1.35))

def salvia_plant(stage):
    c = Canvas()
    hgt = [4, 8, 11, 14][stage]
    for x in (4, 11):
        c.line(x, 15, x, 16 - hgt, hexc("5D7E4A"))
        for i in range(max(1, hgt // 4)):
            y = 15 - i * 4
            c.leaflet(x, y, -60, 3 - i * 0.3, SAGE, width=1.2)
            c.leaflet(x, y - 1, 60, 3 - i * 0.3, SAGE_D, width=1.2)
        if stage == 3:
            for k in range(3):
                c.put(x, 16 - hgt - k, hexc("9A6AD8")); c.put(x + (1 if k % 2 else -1), 16 - hgt - k + 1, hexc("B990F0"))
    return c

for s in range(4):
    salvia_plant(s).save(f"block/salvia_stage{s}")
c = Canvas(); salvia_leaf(c, 4, 14, 12, 3, SAGE); c.leaflet(6, 12, -70, 3, SAGE_D, width=1.3); c.outline().save("item/salvia_leaf")
c = Canvas(); salvia_leaf(c, 4, 14, 12, 3, hexc("8C8A4E"))
for _ in range(8):
    x, y = R.randint(4, 12), R.randint(3, 14)
    if c.get(x, y)[3]: c.put(x, y, hexc("C9A24A"))
c.outline().save("item/dried_salvia")
seeds(hexc("3B3530"), hexc("6A625A"), [(5, 6), (9, 4), (11, 9), (6, 11), (10, 13)], 1.2, 1.5).save("item/salvia_seeds")

# ergot: dark purple-black grain spurs
c = Canvas()
for (x0, y0, x1, y1) in ((4, 13, 7, 4), (8, 14, 10, 3), (11, 13, 13, 6)):
    c.line(x0, y0, x1, y1, hexc("3A1F3D")); c.line(x0 + 1, y0, x1 + 1, y1, hexc("54305A"))
    c.put(x1, y1, hexc("7A4A80"))
c.outline(factor=0.5).save("item/ergot")

# LSD blotter: perforated square sheet with a smiley pattern
c = Canvas()
c.rect(2, 2, 13, 13, hexc("F6F1E4"))
for i in range(2, 14):
    for j in range(2, 14):
        hue = ((i + j) / 24.0) % 1
        import colorsys
        r, g, b = colorsys.hsv_to_rgb(hue, 0.55, 1.0)
        if (i + j) % 2 == 0: c.put(i, j, (int(r * 255), int(g * 255), int(b * 255), 255))
for k in range(2, 14):
    c.put(k, 7, hexc("D8D0BE")); c.put(7, k, hexc("D8D0BE"))  # perforations
for (x, y) in ((4, 4), (5, 4), (4, 5), (10, 10), (11, 10), (10, 11)):
    c.put(x, y, hexc("FFD84A"))
c.outline(factor=0.6).save("item/lsd_blotter")

def pills(cols, positions, logo=None):
    c = Canvas()
    for (x, y), col in zip(positions, cols):
        c.ellipse(x, y, 2.8, 2.3, col, light=1.35, dark=0.75)
        if logo: c.put(x, y, logo)
    return c.outline()

pills([hexc("F2F2F2"), hexc("E8E8E8"), hexc("F7F7F7")], [(5, 6), (10.5, 8), (6.5, 11.5)], hexc("C8C8C8")).save("item/speed_pills")
c = Canvas()
c.ellipse(8, 8, 5.5, 5, hexc("FF7AC8"), light=1.3, dark=0.7)
for (x, y) in ((6, 6), (7, 7), (8, 8), (9, 7), (10, 6), (8, 9), (8, 10)):  # little heart/dove stamp
    c.put(x, y, hexc("FFC2E6"))
c.outline().save("item/ecstasy_pill")

# ketamine: small glass vial with liquid + powder baggie
c = Canvas()
c.rect(6, 3, 9, 13, hexc("DCEFF5", 170)); c.rect(6, 8, 9, 13, hexc("F2F7FA")); c.rect(5, 2, 10, 3, hexc("8A5ACF"))
c.put(7, 5, hexc("FFFFFF")); c.outline(factor=0.55).save("item/ketamine")

# nitrous balloon: round balloon with knot + string
c = Canvas()
c.ellipse(8, 6.5, 5, 5.5, hexc("5AB4F0"), light=1.4, dark=0.7)
c.put(6, 4, hexc("D8F0FF")); c.put(5, 5, hexc("B8E2FF"))
c.put(8, 12, hexc("3A8AC8")); c.line(8, 13, 9, 15, hexc("C8C8C8"))
c.outline().save("item/nitrous_balloon")

# DMT: amber/orange crystals
c = Canvas()
for (x, y, hgt) in ((5, 13, 6), (8, 14, 9), (11, 13, 5)):
    for k in range(hgt):
        wdt = 1 if k > hgt - 3 else 2
        for dx in range(-wdt + 1, wdt):
            c.put(x + dx, y - k, hexc("F0A23A") if dx <= 0 else hexc("C77A20"))
    c.put(x, y - hgt + 1, hexc("FFE0A0"))
c.outline(factor=0.5).save("item/dmt")

# effect icons
c = icon(); salvia_leaf(c, 3, 15, 15, 3, SAGE); c.outline().save("mob_effect/salvia")
c = icon()
for (x0, y0) in ((2, 5), (2, 9), (2, 13)):
    c.line(x0, y0, x0 + 6, y0, hexc("4FD8FF"))
c.ellipse(12, 9, 4, 4, hexc("4FD8FF"), light=1.3); c.outline().save("mob_effect/speeding")
c = icon()
for (x, y) in ((6, 6), (12, 6)):
    c.ellipse(x, y, 3.6, 3.2, hexc("FF5CC8"))
for y in range(7, 16):
    half = int((15 - y) * 0.95)
    c.line(9 - half, y, 9 + half, y, hexc("FF5CC8"))
c.outline().save("mob_effect/rolling")
c = icon(); c.rect(3, 3, 14, 14, hexc("FFFFFF"))
import colorsys
for i in range(3, 15):
    for j in range(3, 15):
        r, g, b = colorsys.hsv_to_rgb(((i * 2 + j) / 40) % 1, 0.8, 1)
        c.put(i, j, (int(r * 255), int(g * 255), int(b * 255), 255))
c.ellipse(9, 9, 3.5, 3.5, hexc("FFE14D")); c.put(8, 8, hexc("000000")); c.put(10, 8, hexc("000000"))
c.line(7, 10, 11, 10, hexc("000000")); c.outline().save("mob_effect/acid_trip")
c = icon()
for r_ in (7, 5, 3):
    for ang in range(0, 360, 5):
        c.put(9 + math.cos(math.radians(ang)) * r_, 9 + math.sin(math.radians(ang)) * r_, hexc("6A4CB0") if r_ != 5 else hexc("2A1A50"))
c.put(9, 9, hexc("000000")); c.save("mob_effect/k_hole")
c = icon(); c.ellipse(9, 9, 7, 7, hexc("FFE066"))
c.line(5, 7, 7, 6, hexc("3A2A10")); c.line(11, 6, 13, 7, hexc("3A2A10"))
for x in range(5, 14):
    c.put(x, 11 + (1 if 6 < x < 12 else 0), hexc("3A2A10"))
c.rect(7, 12, 11, 13, hexc("C83A3A")); c.outline().save("mob_effect/giggles")
c = icon()
for ang in range(0, 720, 8):
    rr = ang / 720 * 8
    c.put(9 + math.cos(math.radians(ang)) * rr, 9 + math.sin(math.radians(ang)) * rr, hexc("9B5CFF") if ang % 32 else hexc("FFFFFF"))
c.save("mob_effect/breakthrough")

# ---------------- preview ----------------
if len(sys.argv) > 1:
    cols = 10
    rows = (len(OUT) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 20, rows * 20), (60, 60, 60, 255))
    for i, (name, im) in enumerate(OUT):
        sheet.alpha_composite(im, ((i % cols) * 20 + 1, (i // cols) * 20 + 1))
    sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(sys.argv[1])
    for i, (name, _) in enumerate(OUT):
        print(i, name)
