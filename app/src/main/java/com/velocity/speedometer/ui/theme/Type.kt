package com.velocity.speedometer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.velocity.speedometer.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val orbitronGoogleFont = GoogleFont("Orbitron")
private val interGoogleFont = GoogleFont("Inter")

val OrbitronFontFamily = FontFamily(
    Font(googleFont = orbitronGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = orbitronGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = orbitronGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium)
)

val InterFontFamily = FontFamily(
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interGoogleFont, fontProvider = googleFontProvider, weight = FontWeight.Bold)
)

val DigitalSpeedTextStyle = TextStyle(
    fontFamily = OrbitronFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 96.sp,
    lineHeight = 100.sp,
    fontFeatureSettings = "tnum"
)

val AnalogSpeedTextStyle = TextStyle(
    fontFamily = OrbitronFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 56.sp,
    lineHeight = 60.sp,
    fontFeatureSettings = "tnum"
)

val HudSpeedTextStyle = TextStyle(
    fontFamily = OrbitronFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 112.sp,
    lineHeight = 116.sp,
    fontFeatureSettings = "tnum"
)

val StatValueTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 22.sp,
    fontFeatureSettings = "tnum"
)

val VelocityTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
)
