package com.maohongyuan.magic.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.maohongyuan.magic.designsystem.theme.LocalMagicColors
import com.maohongyuan.magic.designsystem.theme.LocalMagicShapes
import com.maohongyuan.magic.designsystem.theme.MagicSpacing
import com.maohongyuan.magic.designsystem.theme.MagicType

/**
 * 卡片内的文本输入框。
 *
 * 用 `BasicTextField` 而不是 Material 的 `OutlinedTextField`：
 * 后者的容器、标签、下划线都是 Material 语言，放进玻璃卡片里会「两张皮」。
 * 这里只保留光标与文字，容器由外层卡片负责。
 */
@Composable
fun MagicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = LocalMagicColors.current
    val shapes = LocalMagicShapes.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(fieldFill(), shapes.controlInCard)
            .padding(
                horizontal = MagicSpacing.cardPadding,
                vertical = MagicSpacing.row,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                fontSize = MagicType.subheadline.sp,
                color = colors.tertiaryText,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = MagicType.subheadline.sp, color = colors.primaryText),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}

/**
 * 输入框底色。
 *
 * 亮色用半透明白、深色用半透明黑 —— 与六爻页所问输入框保持同一套取值。
 * 输入框是卡片**内部**控件，必须与卡片表面拉开一档才看得出是「可输入区域」。
 */
@Composable
private fun fieldFill(): Color {
    val colors = LocalMagicColors.current
    return if (colors.isLight) Color.White.copy(alpha = 0.72f) else Color.Black.copy(alpha = 0.40f)
}
