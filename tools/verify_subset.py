"""穷尽验证：字符集里每个字符是否都真的进了子集字体。"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from fontTools.ttLib import TTFont  # noqa: E402
import subset_fonts as sf  # noqa: E402

PROJ = sf.PROJ
OUT = sf.OUT_DIR

chars = set()
chars |= sf.gb2312_chars()
chars |= sf.project_chars()
chars |= sf.symbols()
chars |= sf.divination_chars()
chars = {c for c in chars if ord(c) >= 0x20 and not (0xD800 <= ord(c) <= 0xDFFF)}
print(f"字符集总数: {len(chars)}")

for name in ("lxgw_zhisong_screen.ttf", "lxgw_xihei_screen.ttf"):
    p = os.path.join(OUT, name)
    t = TTFont(p, lazy=True)
    cm = set(t.getBestCmap().keys())
    missing = sorted(c for c in chars if ord(c) not in cm)
    print(f"\n{name}  {os.path.getsize(p)/1048576:.2f} MB")
    print(f"  缺失 {len(missing)} 个")
    if missing:
        # 只关心汉字与卦符，ASCII/拉丁可能被 drop 但会走系统 fallback
        han = [c for c in missing if 0x4E00 <= ord(c) <= 0x9FFF or 0x3400 <= ord(c) <= 0x4DBF]
        gua = [c for c in missing if 0x4DC0 <= ord(c) <= 0x4DFF or 0x2630 <= ord(c) <= 0x2637]
        print(f"    其中汉字 {len(han)}: {''.join(han[:60])}")
        print(f"    其中卦符 {len(gua)}: {''.join(gua[:20])}")
    t.close()
