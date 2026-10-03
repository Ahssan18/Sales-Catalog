package com.example.data.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TextScale(val label: String, val scaleFactor: Float, val titleSize: TextUnit, val subtitleSize: TextUnit) {
    SMALL("Small", 0.85f, 16.sp, 12.sp),
    MEDIUM("Medium", 1.0f, 18.sp, 14.sp),
    LARGE("Large", 1.15f, 21.sp, 16.sp),
    EXTRA_LARGE("Extra Large", 1.3f, 24.sp, 18.sp)
}

enum class ImageScale(val label: String, val companyLogoSize: Dp, val productImageSize: Dp) {
    COMPACT("Compact", 64.dp, 60.dp),
    STANDARD("Standard", 88.dp, 80.dp),
    LARGE("Large", 112.dp, 104.dp),
    EXTRA_LARGE("Extra Large", 140.dp, 128.dp)
}

enum class RowSpacing(val label: String, val paddingDp: Dp) {
    TIGHT("Tight", 6.dp),
    COMFORTABLE("Comfortable", 12.dp),
    SPACIOUS("Spacious", 20.dp)
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

enum class SortOrder(val label: String) {
    NAME_ASC("A to Z"),
    NAME_DESC("Z to A"),
    RECENT("Recently Added")
}

data class DisplaySettings(
    val textScale: TextScale = TextScale.MEDIUM,
    val imageScale: ImageScale = ImageScale.STANDARD,
    val rowSpacing: RowSpacing = RowSpacing.COMFORTABLE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
