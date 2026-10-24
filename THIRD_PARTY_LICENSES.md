# 第三方开源许可声明

本项目（猫仙幻术 / com.maohongyuan.magic）以 **MIT** 协议开源。
以下为项目直接依赖的第三方组件及其许可信息。

> ⚠️ **其中字体部分（第 3 节）有强制义务，不是普通声明。**
> 嵌入 IPA Font License 1.0 的字体时，必须随 App 提供协议全文/URL，
> **并且**提供让使用者恢复到原始 IPA 字体的方法。
> 本项目已在设置页「开源许可」内实现这两项（`LicenseScreen.kt`）。
> 删改该页面前请先读第 3 节。

---

## 1. lunar (cn.6tail:lunar)

- **用途**：历法内核 —— 公历/农历互转、干支、节气、纳音、彭祖百忌、吉神方位、建除十二值星、黄道吉日、八字、五行、十神
- **版本**：1.7.7
- **仓库**：https://github.com/6tail/lunar-java
- **许可**：MIT License
- **版权**：Copyright (c) 6tail

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 2. Backdrop / AndroidLiquidGlass (io.github.kyant0:backdrop)

- **用途**：iOS 流体玻璃（Liquid Glass）效果引擎 —— 模糊、透镜折射、高光、内外阴影
- **版本**：2.0.1
- **仓库**：https://github.com/Kyant0/AndroidLiquidGlass
- **文档**：https://kyant.gitbook.io/backdrop
- **许可**：Apache License 2.0
- **版权**：Copyright 2025 Kyant

本项目按 Apache-2.0 第 4 条要求：
- 保留本许可声明与版权声明；
- 未修改该库源码（以预编译 AAR 形式依赖），故无需标注修改；
- 该库未随附 NOTICE 文件。

完整许可文本：https://www.apache.org/licenses/LICENSE-2.0

```
Copyright 2025 Kyant

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 3. 字体（IPA Font License 1.0 —— **有强制义务**）

### 霞鹜新致宋 屏幕阅读版 / 霞鹜新晰黑 屏幕阅读版

- **用途**：正文与标题（新致宋，宋体）；Tab 标签、按钮等 UI 小字（新晰黑，黑体）
- **作者**：落霞孤鹜（lxgw）
- **仓库**：https://github.com/lxgw/LxgwNeoXiZhi-Screen
- **许可**：**IPA Font License 1.0**（原始授权字体为 IPAmj明朝）
- **协议全文**：https://opensource.org/licenses/ipa ｜ https://moji.or.jp/ipafont/license
- **已做子集化**：原始 20.7 MB + 11.0 MB → 3.00 MB + 1.83 MB。
  子集 = GB2312 全字符集 + 项目源码字符 + 术数专用词表 + 64 卦符 / 8 八卦符号。
  子集化属于协议允许的「派生程序」。重新生成见 `tools/subset_fonts.py`。

### ⚠️ 嵌入 App 的两项强制义务

IPA Font License 1.0 第 3 条第 1 款第 2 项 + FAQ §3.3.2：

> 利用者が派生プログラム利用時に、自身の意志でオリジナル状態の IPA フォントに
> 戻せない派生プログラムの再配布は禁止されております。
>
> （使用者无法自愿将字体恢复为原始 IPA 字体时，禁止再分发该派生程序。）

因此嵌入 App **必须同时满足两项**，缺一即为违规分发：

| # | 义务 | 本项目实现位置 |
|---|---|---|
| 1 | 附上协议全文或 URL | `app/.../LicenseScreen.kt` → 「字体」小节 |
| 2 | 提供恢复到 IPA 原始字体的方法 | `app/.../LicenseScreen.kt` → 「字体的恢复方法」小节 |

第 2 项采用的官方方案一（在许可页附英文说明）：

```
To replace this font with the original IPA font, please get the original font from
https://moji.or.jp/ipafont/ipafontdownload/ and replace the font file in <directory>.
```

> **删改 `LicenseScreen.kt` 前请先确认这两项仍然存在。**
> 另：字体文件位于 `designsystem/src/main/res/font/`，不得从仓库中移除授权声明后单独分发。

---

## 4. 其它运行时依赖

以下组件通过 AndroidX / Kotlin 官方渠道以 Apache-2.0 协议发布：

| 组件 | 许可 |
|---|---|
| AndroidX（core-ktx / activity-compose / lifecycle / navigation / datastore） | Apache-2.0 |
| Jetpack Compose（ui / foundation / material3） | Apache-2.0 |
| Kotlin 标准库与 kotlinx（coroutines / serialization） | Apache-2.0 |
| OkHttp (com.squareup.okhttp3:okhttp) | Apache-2.0 |

---

## 5. 关于端上模型

本项目的**仓库与 APK 均不包含任何模型权重文件**。

用户可在 App 内按需下载的模型，其许可条款由模型发布方单独约定，与本项目 MIT 协议无关。
详见 `models_manifest.json` 中每个模型条目的 `license` 字段。

本项目不对第三方模型的许可合规性作任何担保，请用户自行确认后再下载使用。

---

## 6. laozi-perspective（老子视角提示词）

- **用途**：设置页「解读风格 → 老子视角」的提示词。**仅改编文本，不引入代码依赖**
- **来源**：https://github.com/chmxn/laozi-perspective
- **许可**：MIT License
- **版权**：Copyright (c) chmxn
- **改编范围**：原 SKILL.md 约 24 KB（6 个心智模型 + 8 条决策启发式 + 表达 DNA）。
  本项目只取其「五个过账问题」（反者道之动 / 无为 / 柔弱胜刚强 / 知足知止 / 返朴归真）
  与表达规则，压缩到 1 KB 以内，因为端上是 0.8B 小模型，整份塞进去会稀释指令。
  原文的 Agentic 联网研究流程（Step 2 的 WebSearch）已删除 —— 端上模型无网络。
  原文主张的「不把话说满」与本项目「不许模棱两可」的要求冲突，
  改为「结论必须明确，留白只用于正文展开」。
- **实现在**：`core/ai/src/main/java/com/maohongyuan/magic/core/ai/ChartPrompt.kt`
  的 `LAOZI_VOICE`

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
