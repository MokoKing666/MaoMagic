# 贡献指南

感谢你愿意为「猫仙幻术」出一份力。

---

## 最欢迎的贡献

### 1. 算法准确性修正（最高优先级）

本项目最看重**算法正确性**。如果你发现排盘结果与古籍或权威资料不符，请提交 Issue 并附上：

- 具体输入（生辰 / 卦象 / 日期）
- 本项目的输出
- 你认为正确的输出
- **依据**：古籍原文、页码或权威资料来源链接

> 例：「天风姤卦，六亲应缺妻财，妻财甲寅应伏于第二爻之下，本项目输出为 XXX，依据《增删卜易》……」

**不接受**没有依据的「我觉得应该是这样」。

### 2. 新增黄金测试

`core/hexagram` 与 `core/calendar` 的测试是项目的正确性基石。欢迎补充：

- 经典命例的八字四柱 / 十神断言
- 六爻装卦全表（八宫卦序、世应、纳甲、伏神、空亡）
- 边界日期（闰月、节气交接时刻、跨年）

### 3. 起卦取数规则的流派讨论

时间起卦、数字起卦、报字起卦的取数规则在不同流派间存在差异。本项目把规则集中定义在 `docs/DIVINATION_RULES.md` 并写成可替换的常量。

如果你主张另一套规则，请提交 Issue 说明**流派来源与依据**，而不是直接改代码。

### 4. UI / 无障碍改进

- 流体玻璃的性能优化（低端机帧率）
- 玻璃表面的可读性与对比度
- TalkBack 支持、大字号适配、色盲友好

---

## 开发约定

### 模块依赖方向（单向，不可违反）

```
:app ──> :feature:* ──> :core:interpret ──> :core:hexagram ──> :core:model
                    │                     └─> :core:calendar ──> :core:model
                    └─> :designsystem
:core:ai ──> :core:interpret / :core:model
:core:data ──> :core:model
```

### 硬约束

1. **`:core:model` / `:core:calendar` / `:core:hexagram` / `:core:interpret` 内禁止出现任何 `android.*` 导入。**
   这四个模块是纯 JVM 模块，是算法可测试、可复用、CI 可跑的前提。PR 中若引入 `android.*` 会被直接拒绝。

2. **feature 模块之间禁止互相依赖。**

3. **禁止硬编码 64 卦数据表。**
   卦宫、卦序、世应必须由 `HexagramTable` 按京房八宫变爻规则算法生成，并保留 `check(size == 64)` 自检。

4. **流体玻璃必须遵守三条铁律**（详见 `designsystem`）：
   - 必须调用 `Modifier.layerBackdrop(backdrop)`
   - **绝不允许在 `drawBackdrop` 之后再加 `layerBackdrop`** —— 会造成渲染死循环并崩溃（`SIGSEGV in RenderThread`）。嵌套玻璃必须改用 `exportedBackdrop` 参数
   - 底色要先 `drawRect` 进 backdrop

5. **模型文件绝不入库。** 不要为了「方便」把模型权重提交进仓库或 `assets/`。

### 代码风格

- Kotlin 官方风格（`kotlin.code.style=official`）
- 中文注释用于解释**业务规则与古籍依据**，英文注释用于解释技术细节
- 算法关键处必须写明口诀或出处，例如纳甲表旁注明「乾金甲子外壬午」

### 提交前自检

```bash
# 算法内核单测（纯 JVM，必过）
./gradlew :core:model:test :core:calendar:test :core:hexagram:test :core:interpret:test

# 构建
./gradlew assembleDebug
```

---

## 提交流程

1. Fork 本仓库
2. 从 `main` 切出分支：`git checkout -b fix/liuyao-fushen`
3. 提交：使用清晰的 commit message，算法修正请附依据
4. 确保单测通过
5. 发起 Pull Request，说明**改了什么、为什么改、依据是什么**

---

## 不接受的内容

- 无依据的算法「修正」
- 引入广告、统计、推送 SDK
- 引入账号体系或云端数据收集
- 把「仅供娱乐」的免责声明改成功效性宣传（如「精准预测」「改运」「消灾」）
- 提交模型权重文件

---

## 许可

你的贡献将以 [MIT](LICENSE) 协议授权。
