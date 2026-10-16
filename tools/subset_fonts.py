"""为「猫仙幻术」构建字体子集。

字符集来源：
1. GB2312 全部（6763 汉字 + 682 符号）—— 覆盖现代中文 99.99%，兜住用户输入
2. 项目源码里出现的所有字符 —— 兜住术数专用字
3. 卦符 / 八卦 / 术数补充词表
4. ASCII + 常用标点

用法：python subset_fonts.py [--gbk]
  --gbk  用 GBK 字符集替代 GB2312（约 2.2 万字，体积更大，对生僻人名更友好）
"""
import os
import re
import subprocess
import sys

FONT_DIR = os.path.expandvars(r"%LOCALAPPDATA%\Microsoft\Windows\Fonts")
PROJ = r"C:\Users\67262\WorkBuddy AI\2026-10-16-14-25-15"
OUT_DIR = os.path.join(PROJ, "app", "src", "main", "res", "font")
VENV_PY = r"C:\Users\67262\.workbuddy-ai\binaries\python\envs\default\Scripts\python.exe"

SOURCES = [
    ("LXGWNeoZhiSongScreenFull.ttf", "lxgw_zhisong_screen.ttf"),
    ("LXGWNeoXiHeiScreenFull.ttf", "lxgw_xihei_screen.ttf"),
]


def gb2312_chars() -> set:
    """GB2312 全字符集：一级 16-55 区、二级 56-87 区，另含 1-9 区符号。"""
    out = set()
    for row in range(0xA1, 0xF8):
        for col in range(0xA1, 0xFF):
            try:
                ch = bytes([row, col]).decode("gb2312")
                if len(ch) == 1:
                    out.add(ch)
            except UnicodeDecodeError:
                pass
    return out


def gbk_chars() -> set:
    """GBK 全字符集（含 GB2312 + 扩展）。"""
    out = set()
    for row in range(0x81, 0xFF):
        for col in range(0x40, 0xFF):
            if col == 0x7F:
                continue
            try:
                ch = bytes([row, col]).decode("gbk")
                if len(ch) == 1:
                    out.add(ch)
            except UnicodeDecodeError:
                pass
    return out


def project_chars() -> set:
    """扫源码里出现的所有非 ASCII 字符。"""
    out = set()
    for root, dirs, files in os.walk(PROJ):
        dirs[:] = [d for d in dirs if d not in ("build", ".git", ".gradle")]
        for fn in files:
            if not fn.endswith((".kt", ".kts", ".xml", ".md", ".json", ".toml", ".properties")):
                continue
            try:
                text = open(os.path.join(root, fn), encoding="utf-8").read()
            except (UnicodeDecodeError, OSError):
                continue
            out.update(ch for ch in text if ord(ch) > 127)
    return out


def symbols() -> set:
    """符号与图形字符。"""
    out = set()
    # ASCII 可见 + 拉丁补充
    out.update(chr(c) for c in range(0x20, 0x7F))
    out.update(chr(c) for c in range(0xA0, 0x100))
    # 全角形式、CJK 标点、通用标点
    for lo, hi in [(0x2000, 0x206F), (0x2190, 0x21FF), (0x2460, 0x24FF),
                   (0x25A0, 0x25FF), (0x2600, 0x27BF), (0x3000, 0x303F),
                   (0x4DC0, 0x4DFF), (0xFE10, 0xFE1F), (0xFF00, 0xFFEF)]:
        out.update(chr(c) for c in range(lo, hi + 1))
    # 常用几何/箭头/对勾，逐个点名（避免整块拉进来）
    out.update("✓✕⚠●○◆◇■□▲△→←↑↓·—…“”‘’《》〈〉【】「」")
    return out


# ── 术数专用词表 ──────────────────────────────────────────────
# GB2312 覆盖现代中文，但**缺术数用字**。已实测确认缺失的例如「夬」（泽天夬）。
# 缺字在界面上表现为豆腐块，对术数 App 是硬伤，因此必须显式补齐。
DIVINATION_VOCAB = """
乾兑离震巽坎艮坤
乾为天坤为地水雷屯山水蒙水天需天水讼地水师水地比风天小畜天泽履地天泰天地否
天火同人火天大有地山谦雷地豫泽雷随山风蛊地泽临风地观火雷噬嗑山火贲山地剥地雷复
天雷无妄山天大畜山雷颐泽风大过坎为水离为火泽山咸雷风恒天山遁雷天大壮火地晋地火明夷
风火家人火泽睽水山蹇雷水解山泽损风雷益泽天夬天风姤泽地萃地风升泽水困水风井
泽火革火风鼎震为雷艮为山风山渐雷泽归妹雷火丰火山旅巽为风兑为泽风水涣水泽节
风泽中孚雷山小过水火既济火水未济
甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥
金木水火土阴阳
比肩劫财食神伤官偏财正财七杀正官偏印正印
父母兄弟官鬼妻财子孙
青龙朱雀勾陈腾蛇白虎玄武
元亨利贞悔吝咎吉凶贞悔亡孚厉无咎
潜见龙田君子终日夕惕若跃渊飞亢群首
初九六二三四五上用九用六
天乙太极天德月德三奇福星文昌国印学堂词馆德秀驿马华盖将星金舆天医
禄神羊刃飞刃亡神劫煞灾煞天煞地煞指背咸池月煞孤辰寡宿元辰空亡旬空
十恶大败阴差阳错魁罡天罗地网童子红艳流霞血刃丧门吊客披麻天哭天虚
五鬼官符病符死符小耗大耗岁破月破日破伏神飞神
祭祀祈福求嗣开光塑绘齐醮出行解除出火拆卸修造动土起基上梁竖柱安门作灶
安床栽种牧养纳畜造仓出货财纳财开市交易立券挂匾纳采订盟嫁娶移徙入宅安香
谢土破土启攒安葬修坟立碑除服成服移柩入殓沐浴理发整手足甲扫舍宇伐木作梁
掘井造船平治道涂修饰垣墙塞穴补垣筑堤防开池作厕求医治病针灸会亲友进人口
裁衣冠笄竖柱上梁经络酝酿
建除满平定执破危成收开闭
角亢氐房心尾箕斗牛女虚危室壁奎娄胃昴毕觜参井鬼柳星张翼轸
立春雨水惊蛰春分清明谷雨立夏小满芒种夏至小暑大暑立秋处暑白露秋分寒露霜降
立冬小雪大雪冬至小寒大寒
子时丑时寅时卯时辰时巳时午时未时申时酉时戌时亥时
纳音纳甲世应卦爻彖象辞变互错综
印绶枭神偏正财官杀食伤
旬首值符腾蛇太阴六合勾陈
胎神胎元命宫身宫大运流年小限
旺相休囚死墓绝长生沐浴冠带临官帝旺衰病
喜神福神财神阳贵阴贵
彭祖百忌
"""


def divination_chars() -> set:
    """术数词表里的所有汉字。"""
    return {c for c in DIVINATION_VOCAB if c.strip()}


def main() -> int:
    use_gbk = "--gbk" in sys.argv

    chars = set()
    if use_gbk:
        g = gbk_chars()
        print(f"GBK 字符集      : {len(g)}")
        chars |= g
    else:
        g = gb2312_chars()
        print(f"GB2312 字符集   : {len(g)}")
        chars |= g

    p = project_chars()
    print(f"项目源码字符    : {len(p)}")
    chars |= p

    s = symbols()
    print(f"符号补充        : {len(s)}")
    chars |= s

    d = divination_chars()
    print(f"术数词表        : {len(d)}")
    chars |= d

    # 去掉控制字符与代理区
    chars = {c for c in chars if ord(c) >= 0x20 and not (0xD800 <= ord(c) <= 0xDFFF)}
    print(f"合计            : {len(chars)}")

    os.makedirs(OUT_DIR, exist_ok=True)
    text_file = os.path.join(os.environ["TEMP"], "font_chars.txt")
    with open(text_file, "w", encoding="utf-8") as f:
        f.write("".join(sorted(chars)))

    for src_name, dst_name in SOURCES:
        src = os.path.join(FONT_DIR, src_name)
        dst = os.path.join(OUT_DIR, dst_name)
        cmd = [
            VENV_PY, "-m", "fontTools.subset", src,
            f"--text-file={text_file}",
            f"--output-file={dst}",
            "--layout-features=*",
            "--no-hinting",
            "--desubroutinize",
            "--name-IDs=*",
            "--drop-tables+=DSIG",
        ]
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode != 0:
            print(f"失败 {src_name}: {r.stderr[:600]}")
            return 1
        print(f"{src_name} → {dst_name}  "
              f"{os.path.getsize(src)/1048576:.1f} MB → {os.path.getsize(dst)/1048576:.2f} MB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
