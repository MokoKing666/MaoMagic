# 第三方开源许可声明

本项目（猫仙幻术 / com.maohongyuan.magic）以 **MIT** 协议开源。
以下为项目直接依赖的第三方组件及其许可信息。

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

## 3. 其它运行时依赖

以下组件通过 AndroidX / Kotlin 官方渠道以 Apache-2.0 协议发布：

| 组件 | 许可 |
|---|---|
| AndroidX（core-ktx / activity-compose / lifecycle / navigation / datastore） | Apache-2.0 |
| Jetpack Compose（ui / foundation / material3） | Apache-2.0 |
| Kotlin 标准库与 kotlinx（coroutines / serialization） | Apache-2.0 |
| OkHttp (com.squareup.okhttp3:okhttp) | Apache-2.0 |

---

## 4. 关于端上模型

本项目的**仓库与 APK 均不包含任何模型权重文件**。

用户可在 App 内按需下载的模型，其许可条款由模型发布方单独约定，与本项目 MIT 协议无关。
详见 `models_manifest.json` 中每个模型条目的 `license` 字段。

本项目不对第三方模型的许可合规性作任何担保，请用户自行确认后再下载使用。
