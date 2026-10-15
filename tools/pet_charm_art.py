#!/usr/bin/env python3
"""Item art for pets and charms (config: pets, charms): five Ascension Collars, the Pet Whistle and the charms.
Drawn as 16x16 masks and finished like every other item (make_textures.hd_item: Scale2x, rim light, outline).
Run from the repo root (make_textures.py runs it):  python3 tools/pet_charm_art.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import make_textures as T  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), "..", "kubejs", "assets", "kubejs", "textures", "item")

COLLAR = [
    "................",
    "................",
    "....########....",
    "...##......##...",
    "..##........##..",
    "..#..........#..",
    "..#..........#..",
    "..#..........#..",
    "..##........##..",
    "...##......##...",
    "....###gg###....",
    ".......gg.......",
    "......gwgg......",
    "......gggg......",
    ".......gg.......",
    "................",
]
CHARM = [
    "....c......c....",
    ".....c....c.....",
    "......c..c......",
    ".......cc.......",
    "......####......",
    "....##mmmm##....",
    "...#mmmmmmmm#...",
    "...#mwmssmmm#...",
    "...#mmssssmm#...",
    "...#mmmssmmm#...",
    "...#mmmmmmmm#...",
    "....##mmmm##....",
    "......####......",
    "................",
    "................",
    "................",
]
WHISTLE = [
    "................",
    "................",
    "................",
    "................",
    "..........###...",
    ".........#ggg#..",
    "..########gggg#.",
    ".#gwgggggggggg#.",
    ".#gggggggggggg#.",
    ".#gggg##gggggg#.",
    "..####..######..",
    "................",
    "........cc......",
    ".......c..c.....",
    "........cc......",
    "................",
]
TIERS = {   # collar id: band color
    "ascendite_collar": (64, 220, 200), "voidsteel_collar": (96, 70, 140), "celestium_collar": (170, 210, 250),
    "ascendium_collar": (232, 176, 58), "singularity_collar": (170, 96, 255),
}
CHARMS = {  # charm id: (medallion, symbol)
    "haste_charm": ((230, 180, 60), (255, 250, 200)), "swift_charm": ((120, 200, 255), (240, 250, 255)),
    "owl_charm": ((40, 50, 110), (255, 240, 120)), "ember_charm": ((220, 80, 30), (255, 210, 90)),
    "tide_charm": ((40, 130, 200), (180, 240, 255)), "iron_charm": ((150, 150, 160), (230, 230, 240)),
    "luck_charm": ((60, 170, 80), (220, 255, 200)), "leap_charm": ((150, 220, 120), (255, 255, 255)),
    "magnet_charm": ((200, 50, 60), (220, 220, 230)), "ascended_charm": ((170, 96, 255), (64, 232, 212)),
}
GOLD = (232, 176, 58)
CORD = (70, 50, 40)


def draw(mask, colors):
    px = T.blank()
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == "w":
                px[y][x] = (255, 255, 255, 255)
            elif ch in colors:
                c = colors[ch]
                px[y][x] = T._shade(c + (255,), 0.18 - (x + y) / 80.0)
    return px


def main():
    os.makedirs(OUT, exist_ok=True)
    n = 0
    for cid, band in TIERS.items():
        T.png(os.path.join(OUT, cid + ".png"), T.hd_item(draw(COLLAR, {"#": band, "g": GOLD}), band, cid)); n += 1
    T.png(os.path.join(OUT, "pet_whistle.png"), T.hd_item(draw(WHISTLE, {"#": (140, 90, 30), "g": GOLD, "c": CORD}), GOLD, "pet_whistle")); n += 1
    for cid, (med, sym) in CHARMS.items():
        T.png(os.path.join(OUT, cid + ".png"), T.hd_item(draw(CHARM, {"c": CORD, "#": GOLD, "m": med, "s": sym}), med, cid)); n += 1
    print("wrote %d pet and charm items" % n)


if __name__ == "__main__":
    main()
