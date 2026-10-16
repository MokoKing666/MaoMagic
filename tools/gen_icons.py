"""由布偶猫原图生成 Android 各密度图标。

策略：
- 白底转为透明（按最小通道值做软阈值，避免边缘出现白色锯齿）
- 主体裁到包围盒后按「安全区比例」缩放居中
- 自适应图标前景留出安全区（可见区约为画布的 66%），否则被圆形遮罩切到耳朵
- 同时生成旧版方形 / 圆形图标（Android 8 以下与部分启动器仍会取用）
"""
from PIL import Image, ImageDraw
import os

SRC = r"C:\Users\67262\.workbuddy-ai\clipboard-images\clipboard-2026-10-16T10-25-54-946Z-5fda38ab.jpg"
RES = r"C:\Users\67262\WorkBuddy AI\2026-10-16-14-25-15\app\src\main\res"

# 自适应图标画布 108dp；可见安全区约 72dp（66%）。主体取画布的 62%，留一点呼吸。
SAFE_FRACTION = 0.66

# 旧版图标 48dp
LEGACY_FRACTION = 0.86

BG = (247, 241, 232)   # 暖米白，比猫身浅一档：不刺眼，也不会在白壁纸上消失

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def cutout(img: Image.Image) -> Image.Image:
    """把背景转成透明。

    判据用「**中性且亮**」而不是单纯看亮度：
    - 背景是纯白 (252,252,252)，三个通道几乎相等（spread ≈ 0）
    - 猫身是暖米色 (250,243,235)，spread ≈ 15

    早期只按最小通道值做阈值，结果背景里 JPEG 压缩留下的 ±1 波动
    （min 落在 245..249）被算成半透明，把包围盒撑到整张图，
    主体因此被缩得很小。加上 spread 判据后背景干净归零。
    """
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            m = min(r, g, b)
            spread = max(r, g, b) - m
            if spread <= 8:
                if m >= 242:
                    a = 0
                elif m >= 238:
                    a = int(255 * (242 - m) / 4)
                else:
                    a = 255
            else:
                a = 255
            if a < 32:
                a = 0
            px[x, y] = (r, g, b, a)
    return img


def bbox_of_alpha(img: Image.Image):
    return img.getchannel("A").getbbox()


def place(subject: Image.Image, canvas_px: int, fraction: float,
          background=None, circle=False) -> Image.Image:
    """把主体按 fraction 缩放并居中放进 canvas_px 的画布。"""
    target = int(canvas_px * fraction)
    sw, sh = subject.size
    scale = target / max(sw, sh)
    new = subject.resize((max(1, int(sw * scale)), max(1, int(sh * scale))), Image.LANCZOS)

    if background is None:
        canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    else:
        canvas = Image.new("RGBA", (canvas_px, canvas_px), background + (255,))

    canvas.alpha_composite(new, ((canvas_px - new.width) // 2,
                                 (canvas_px - new.height) // 2))

    if circle:
        mask = Image.new("L", (canvas_px, canvas_px), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, canvas_px - 1, canvas_px - 1), fill=255)
        canvas.putalpha(Image.composite(canvas.getchannel("A"),
                                        Image.new("L", (canvas_px, canvas_px), 0), mask))
    return canvas


def main():
    src = Image.open(SRC).convert("RGBA")
    cut = cutout(src)
    box = bbox_of_alpha(cut)
    print("主体包围盒:", box)
    subject = cut.crop(box)
    print("裁后尺寸:", subject.size)

    for name, mult in DENSITIES.items():
        d = os.path.join(RES, f"mipmap-{name}")
        os.makedirs(d, exist_ok=True)

        fg_px = round(108 * mult)
        legacy_px = round(48 * mult)

        # 自适应前景（透明底，留安全区）
        fg = place(subject, fg_px, SAFE_FRACTION)
        fg.save(os.path.join(d, "ic_launcher_foreground.png"))

        # 旧版方形 / 圆形（暖底）
        place(subject, legacy_px, LEGACY_FRACTION, BG).convert("RGB").save(
            os.path.join(d, "ic_launcher.png"))
        place(subject, legacy_px, LEGACY_FRACTION, BG, circle=True).save(
            os.path.join(d, "ic_launcher_round.png"))

        print(f"  {name}: foreground {fg_px}px, legacy {legacy_px}px")

    # 预览：模拟圆形遮罩下的实际观感
    prev = round(108 * 3)
    p = place(subject, prev, SAFE_FRACTION, BG)
    mask = Image.new("L", (prev, prev), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, prev - 1, prev - 1), fill=255)
    round_preview = Image.new("RGBA", (prev, prev), (0, 0, 0, 0))
    round_preview.paste(p, (0, 0), mask)
    out = r"C:\Users\67262\AppData\Local\Temp\logo-preview.png"
    round_preview.save(out)
    print("预览:", out)


if __name__ == "__main__":
    main()
