"""读取 APK 的 versionCode / versionName。

不依赖 aapt —— 直接解出 AndroidManifest.xml 的二进制 XML 再取属性，
避免额外依赖 Android SDK 组件。

用法：python apk_version.py <apk> [--name]
输出：versionCode（默认）或 versionName（--name）
"""
import struct
import sys
import zipfile


def _decode_utf16(buf: bytes) -> str:
    length = struct.unpack_from("<H", buf, 0)[0]
    return buf[2: 2 + length * 2].decode("utf-16-le", errors="replace")


def _decode_utf8(buf: bytes) -> str:
    # 长度前缀是「字符数」，UTF-8 变长，先读 2 字节长度再按字节截
    len_chars = buf[0]
    if len_chars & 0x80:
        len_chars = ((len_chars & 0x7F) << 8) | buf[1]
        start = 2
    else:
        start = 1
    raw = buf[start:]
    return raw[: len_chars * 4].split(b"\x00")[0].decode("utf-8", errors="replace")


def read_manifest(apk_path: str) -> bytes:
    with zipfile.ZipFile(apk_path) as z:
        return z.read("AndroidManifest.xml")


def parse(data: bytes) -> dict:
    """返回 {versionCode: str, versionName: str}。"""
    out = {}
    # 二进制 XML：头部 8 字节，之后是一串 chunk
    pos = 8
    string_pool: list[str] = []
    while pos + 8 <= len(data):
        ctype, _hdr_size, csize = struct.unpack_from("<HHI", data, pos)
        if ctype == 0x0001:  # RES_STRING_POOL_TYPE
            str_count, _style_count, flags, str_start = struct.unpack_from("<IIII", data, pos + 8)
            utf8 = bool(flags & (1 << 8))
            offsets = struct.unpack_from(f"<{str_count}I", data, pos + 28)
            base = pos + str_start
            for off in offsets:
                buf = data[base + off:]
                try:
                    string_pool.append(_decode_utf8(buf) if utf8 else _decode_utf16(buf))
                except Exception:
                    string_pool.append("")
        elif ctype == 0x0102:  # RES_XML_START_ELEMENT_TYPE
            # 只关心 manifest 元素
            name_idx = struct.unpack_from("<I", data, pos + 20)[0]
            if name_idx < len(string_pool) and string_pool[name_idx] == "manifest":
                attr_count = struct.unpack_from("<H", data, pos + 28)[0]
                ap = pos + 36
                for _ in range(attr_count):
                    a_ns, a_name, a_raw, _a_typed = struct.unpack_from("<IIIH", data, ap)
                    a_data = struct.unpack_from("<I", data, ap + 16)[0]
                    key = string_pool[a_name] if a_name < len(string_pool) else ""
                    if key == "versionCode":
                        out["versionCode"] = str(a_data)
                    elif key == "versionName":
                        out["versionName"] = (
                            string_pool[a_raw] if a_raw < len(string_pool) else str(a_data)
                        )
                    ap += 20
                break
        pos += csize
    return out


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: apk_version.py <apk> [--name]", file=sys.stderr)
        raise SystemExit(2)
    info = parse(read_manifest(sys.argv[1]))
    if "--name" in sys.argv:
        print(info.get("versionName", "?"))
    else:
        print(info.get("versionCode", "?"))
