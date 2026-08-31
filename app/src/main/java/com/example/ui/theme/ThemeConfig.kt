package com.example.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ColorSchemeOption(
    val title: String,
    val description: String,
    val expenseColor: Color,
    val expenseText: Color,
    val expenseContainer: Color,
    val incomeColor: Color,
    val incomeText: Color,
    val incomeContainer: Color
) {
    TRADITIONAL(
        title = "经典红绿",
        description = "支出为红 · 收入为绿",
        expenseColor = Color(0xFFEF4444),
        expenseText = Color(0xFFDC2626),
        expenseContainer = Color(0xFFEF4444).copy(alpha = 0.15f),
        incomeColor = Color(0xFF10B981),
        incomeText = Color(0xFF059669),
        incomeContainer = Color(0xFF10B981).copy(alpha = 0.15f)
    ),
    INTERNATIONAL(
        title = "国际绿红",
        description = "支出为绿 · 收入为红",
        expenseColor = Color(0xFF10B981),
        expenseText = Color(0xFF059669),
        expenseContainer = Color(0xFF10B981).copy(alpha = 0.15f),
        incomeColor = Color(0xFFEF4444),
        incomeText = Color(0xFFDC2626),
        incomeContainer = Color(0xFFEF4444).copy(alpha = 0.15f)
    ),
    CYBER_NEON(
        title = "赛博霓虹",
        description = "支出玫粉 · 收入电光青",
        expenseColor = Color(0xFFF43F5E),
        expenseText = Color(0xFFE11D48),
        expenseContainer = Color(0xFFF43F5E).copy(alpha = 0.15f),
        incomeColor = Color(0xFF06B6D4),
        incomeText = Color(0xFF0891B2),
        incomeContainer = Color(0xFF06B6D4).copy(alpha = 0.15f)
    ),
    AMBER_SUNSET(
        title = "金橘琉璃",
        description = "支出日落橙 · 收入琥珀金",
        expenseColor = Color(0xFFF97316),
        expenseText = Color(0xFFEA580C),
        expenseContainer = Color(0xFFF97316).copy(alpha = 0.15f),
        incomeColor = Color(0xFFF59E0B),
        incomeText = Color(0xFFD97706),
        incomeContainer = Color(0xFFF59E0B).copy(alpha = 0.15f)
    )
}

enum class FontScaleOption(
    val title: String,
    val scale: Float,
    val description: String
) {
    STANDARD("标准大小", 1.0f, "系统默认字体比例，精炼紧凑"),
    COMFORTABLE("舒适大字", 1.12f, "字体适度放大，阅读清晰舒心"),
    LARGE("长辈特大", 1.25f, "特大字体排版，醒目省心不费眼")
}

enum class BackgroundOptionType {
    GRAY_WHITE,      // 默认灰白纯色
    WARM_IVORY,      // 暖阳米白
    PURE_WHITE,      // 极简纯白
    MINT_LIGHT,      // 淡雅薄荷
    LILAC_LIGHT,     // 暮光雾紫
    SKY_LIGHT,       // 晴空浅蓝
    DEEP_COSMIC,     // 深空微光 (暗色发光)
    AURORA_NIGHT,    // 极光幻彩 (暗色渐变)
    SLATE_DARK,      // 玄武深岩 (暗色纯黑岩)
    CUSTOM_SOLID,    // 自定义纯色
    CUSTOM_IMAGE     // 自定义上传图片/精选壁纸
}

data class PresetWallpaper(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val isLight: Boolean
)

data class BackgroundConfig(
    val type: BackgroundOptionType = BackgroundOptionType.PURE_WHITE,
    val title: String = "极简纯白 (默认)",
    val subtitle: String = "极致纯粹净白，无暇纯色",
    val solidColor: Color = Color(0xFFFFFFFF),
    val isLight: Boolean = true,
    val customHex: String = "#FFFFFF",
    val imageUri: String? = null,
    val cardAlpha: Float = 0.95f,
    val blurRadius: Float = 0f,
    val frostAlpha: Float = 0.0f
) {
    // Dynamic styling tokens
    val textPrimary: Color
        get() = if (isLight) Color(0xFF0F172A) else Color(0xFFFFFFFF)

    val textSecondary: Color
        get() = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.78f)

    val textTertiary: Color
        get() = if (isLight) Color(0xFF94A3B8) else Color.White.copy(alpha = 0.50f)

    val cardBackground: Color
        get() = if (isLight) {
            Color(0xFFFFFFFF).copy(alpha = cardAlpha.coerceIn(0.10f, 0.98f))
        } else {
            Color(0xFF11192E).copy(alpha = (cardAlpha * 0.85f).coerceIn(0.12f, 0.96f))
        }

    val cardBorder: Brush
        get() = if (isLight) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = (cardAlpha + 0.15f).coerceAtMost(0.9f)),
                    Color(0xFFCBD5E1).copy(alpha = 0.55f),
                    Color(0xFFF1F5F9).copy(alpha = 0.35f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.08f),
                    Color(0xFF818CF8).copy(alpha = 0.32f),
                    Color(0xFF38BDF8).copy(alpha = 0.16f)
                )
            )
        }

    val chipUnselectedBg: Color
        get() = if (isLight) {
            Color(0xFFE2E8F0).copy(alpha = (cardAlpha * 0.7f).coerceIn(0.2f, 0.8f))
        } else {
            Color.White.copy(alpha = 0.10f)
        }

    val chipUnselectedBorder: Brush
        get() = if (isLight) {
            Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0)))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f)))
        }

    val navBarBackground: Color
        get() = if (isLight) {
            Color(0xFFFFFFFF).copy(alpha = (cardAlpha + 0.08f).coerceIn(0.20f, 0.98f))
        } else {
            Color(0xFF0D1527).copy(alpha = (cardAlpha + 0.12f).coerceIn(0.25f, 0.96f))
        }

    val navBarBorder: Brush
        get() = if (isLight) {
            Brush.linearGradient(listOf(Color.White, Color(0xFFCBD5E1).copy(alpha = 0.6f)))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color(0xFF818CF8).copy(alpha = 0.30f), Color.White.copy(alpha = 0.10f)))
        }

    val inputFieldBg: Color
        get() = if (isLight) Color(0xFFF8FAFC).copy(alpha = 0.95f) else Color(0xFF1E293B).copy(alpha = 0.70f)

    val inputFieldBorder: Color
        get() = if (isLight) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.20f)

    val dialogBackground: Color
        get() = if (isLight) Color(0xFFFFFFFF) else Color(0xFF111827)

    val dividerColor: Color
        get() = if (isLight) Color(0xFFDCD5C0) else Color.White.copy(alpha = 0.08f)
}

val LocalAppColorScheme = compositionLocalOf { ColorSchemeOption.INTERNATIONAL }
val LocalAppFontScale = compositionLocalOf { FontScaleOption.STANDARD }
val LocalAppBackgroundConfig = compositionLocalOf { BackgroundConfig() }
