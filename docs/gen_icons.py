from PIL import Image, ImageDraw
import pathlib

SRC = pathlib.Path("C:/rsp/docs/icon.PNG")
RES = pathlib.Path("C:/rsp/app/src/main/res")

TEAL = (25, 175, 177, 255)   # #19AFB1

DENSITIES = [
    ("mipmap-mdpi",     48,  108),
    ("mipmap-hdpi",     72,  162),
    ("mipmap-xhdpi",    96,  216),
    ("mipmap-xxhdpi",  144,  324),
    ("mipmap-xxxhdpi", 192,  432),
]

src = Image.open(SRC).convert("RGBA")

for dir_name, legacy_px, fg_px in DENSITIES:
    d = RES / dir_name
    d.mkdir(exist_ok=True)

    # Legacy icon: icon on teal circle
    canvas = Image.new("RGBA", (legacy_px, legacy_px), (0, 0, 0, 0))
    circle = Image.new("RGBA", (legacy_px, legacy_px), (0, 0, 0, 0))
    ImageDraw.Draw(circle).ellipse([0, 0, legacy_px - 1, legacy_px - 1], fill=TEAL)
    canvas.paste(circle, (0, 0), circle)
    slot = int(legacy_px * 0.70)
    icon = src.resize((slot, int(slot * src.height / src.width)), Image.LANCZOS)
    canvas.paste(icon, ((legacy_px - icon.width) // 2, (legacy_px - icon.height) // 2), icon)
    canvas.save(str(d / "ic_launcher.png"))
    canvas.save(str(d / "ic_launcher_round.png"))
    print(f"  {dir_name}: {legacy_px}px legacy written")

    # Adaptive foreground: icon centred in safe zone, transparent bg
    fg = Image.new("RGBA", (fg_px, fg_px), (0, 0, 0, 0))
    safe = int(fg_px * (72 / 108))
    icon_fg = src.resize((safe, int(safe * src.height / src.width)), Image.LANCZOS)
    fg.paste(icon_fg, ((fg_px - icon_fg.width) // 2, (fg_px - icon_fg.height) // 2), icon_fg)
    fg.save(str(d / "ic_launcher_foreground.png"))
    print(f"  {dir_name}: {fg_px}px adaptive foreground written")

print("All icon assets generated.")
