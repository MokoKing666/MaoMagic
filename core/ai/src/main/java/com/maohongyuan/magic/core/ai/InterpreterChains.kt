package com.maohongyuan.magic.core.ai

import android.content.Context
import com.maohongyuan.magic.core.interpret.DivinationInterpreter
import com.maohongyuan.magic.core.interpret.InterpreterChain

/**
 * 降级链工厂。
 *
 * ── 为什么要有它 ──
 * 每个术数页面各自拼一遍链，会导致「六爻有智能模式、梅花没有」这种不一致 ——
 * 用户在一个页面看到 AI 解读，换到另一个页面又变回规则文案，很困惑。
 *
 * 统一从这里装配：**所有系统共享同一条链的构造方式**，只有规则引擎不同。
 *
 * 顺序固定：`端上模型 → 云端 AI → 规则引擎`。
 * 端上模型文件不存在时 `isAvailable()` 为 false，链自动跳过，不需要调用方判断。
 */
object InterpreterChains {

    /**
     * @param context 用于取 `filesDir` 定位模型、以及创建推理引擎
     * @param ruleEngine 本系统的规则引擎（六爻/梅花/八字各不同），**永远兜底**
     * @param cloudConfig 云端 AI 配置。默认返回未配置的实例，即云端档不可用
     */
    fun standard(
        context: Context,
        ruleEngine: DivinationInterpreter,
        cloudConfig: () -> CloudAiConfig = { CloudAiConfig() },
    ): InterpreterChain = InterpreterChain(
        listOf(
            OnDeviceInterpreter.fromDevice(context),
            CloudAiInterpreter(cloudConfig),
            ruleEngine,
        )
    )
}
