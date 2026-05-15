package com.example.nothingpodcast.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontRes
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.example.nothingpodcast.R

/**
 * Nothing Tech typography system.
 *
 * Hierarchy (replicates Nothing OS usage):
 *  - NType82   → Display & Headline: grandi titoli schermo (es. "Nothing Podcast", "IMPOSTAZIONI")
 *  - Outfit     → Title & Body: nomi podcast, titoli episodi, testi descrittivi
 *  - SpaceMono  → Label: timestamp, badge, chip, info tecniche monospace
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// NType82 — Official Nothing Brand Font (display only)
val NType82Family = FontFamily(
    Font(R.font.ntype82_regular, FontWeight.Normal),
    Font(R.font.ntype82_regular, FontWeight.Bold)
)

val PlayfairFamily = FontFamily.Serif

// SpaceMono — Monospace for technical labels (timestamps, badges)
private val SpaceMonoFont = GoogleFont("Space Mono")
val SpaceMonoFamily = FontFamily(
    GoogleFontRes(googleFont = SpaceMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    GoogleFontRes(googleFont = SpaceMonoFont, fontProvider = provider, weight = FontWeight.Bold),
)

// Outfit — Clean sans-serif for body & content titles
private val OutfitFont = GoogleFont("Outfit")
val OutfitFamily = FontFamily(
    GoogleFontRes(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Light),
    GoogleFontRes(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Normal),
    GoogleFontRes(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Medium),
    GoogleFontRes(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.SemiBold),
    GoogleFontRes(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Bold),
)

val NothingTypography = Typography(
    // ── Display: grandi titoli schermata (NType82) ────────────────────────
    displayLarge = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline: intestazioni sezione ALL-CAPS (NType82) ─────────────────
    headlineLarge = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 1.sp
    ),

    // ── Title: nomi podcast/episodi nei elenchi (Outfit — leggibile) ──────
    titleLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body: descrizioni, note episodio (Outfit) ─────────────────────────
    bodyLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),

    // ── Label: timestamp, badge, chip tecnici (SpaceMono monospace) ───────
    labelLarge = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SpaceMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
)