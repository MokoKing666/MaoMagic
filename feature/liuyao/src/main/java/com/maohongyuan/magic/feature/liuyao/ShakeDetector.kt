package com.maohongyuan.magic.feature.liuyao

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 摇一摇。
 *
 * 铜钱起卦的仪式感有一半在「摇」这个动作上 —— 用手指点六下是操作，
 * 摇手机才是摇卦。所以除了按钮，也支持真的摇。
 *
 * 判定用**加速度计**而不是陀螺仪：摇动是平移加速度的变化，
 * 陀螺仪测的是角速度，握着手机晃不一定有明显转动。两者都试过，
 * 加速度计对「摇」这个动作更敏感、误触也更少。
 *
 * 阈值取 18 m/s²（重力约 9.8，即约 1.8g 的额外冲击）。
 * 低于 12 时走路颠簸就会触发；高于 25 得使劲甩，手感很差。
 *
 * @param enabled 为 false 时不监听（例如六爻已摇满，摇也不该再动）
 */
@Composable
fun rememberShakeDetector(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    // 用 rememberUpdatedState 包一层：onShake 每次重组都是新 lambda，
    // 直接把它塞进 SensorEventListener 会让监听器持有旧引用。
    val callback by rememberUpdatedState(onShake)
    val enabledState by rememberUpdatedState(enabled)

    val manager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensor = remember(manager) {
        manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    DisposableEffect(manager, sensor) {
        if (manager == null || sensor == null) {
            // 没有加速度计（部分平板/模拟器）就静默降级到按钮操作，不报错
            return@DisposableEffect onDispose { }
        }

        var lastShakeAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!enabledState) return
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                // 去掉重力后的净加速度
                val net = sqrt(gx * gx + gy * gy + gz * gz) - SensorManager.GRAVITY_EARTH
                if (abs(net) < SHAKE_THRESHOLD) return

                // 防抖：一次摇动会在几十毫秒内连发多帧，不加这个会一摇连出好几爻
                val now = System.currentTimeMillis()
                if (now - lastShakeAt < SHAKE_COOLDOWN_MS) return
                lastShakeAt = now
                callback()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { manager.unregisterListener(listener) }
    }
}

/** 触发摇动的净加速度阈值（m/s²），已扣除重力 */
private const val SHAKE_THRESHOLD = 18f

/** 两次摇动之间的最小间隔。一次摇动会连续触发多帧，必须防抖。 */
private const val SHAKE_COOLDOWN_MS = 700L
