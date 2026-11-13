package dev.a3ro.betterola.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.a3ro.betterola.R

/** Archivo is a variable font with weight and width axes (SIL OFL, see legal/fonts). */
@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int, width: Float) = FontFamily(
    Font(
        R.font.archivo,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width))
    )
)

val Archivo = archivo(400, 100f)
private val ArchivoMedium = archivo(550, 100f)
private val ArchivoSemi = archivo(620, 100f)
/** Expanded cut for numbers and screen titles, the same voice as the HUD. */
val ArchivoWide = archivo(700, 125f)
val ArchivoWideSemi = archivo(650, 125f)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = ArchivoWide, fontSize = 88.sp, lineHeight = 84.sp, letterSpacing = (-0.03).em),
    headlineMedium = TextStyle(fontFamily = ArchivoWideSemi, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em),
    titleMedium = TextStyle(fontFamily = ArchivoSemi, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Archivo, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Archivo, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = ArchivoMedium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = ArchivoMedium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Archivo, fontSize = 12.sp, lineHeight = 16.sp),
)
