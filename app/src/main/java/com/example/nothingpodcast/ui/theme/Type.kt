package com.example.nothingpodcast.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.example.nothingpodcast.R

// ── Nothing Official Font Families ───────────────────────────────────────────

// NType82 — Standard Sans for UI and Body
val NType82Family = FontFamily(
    Font(R.font.ntype82_regular, FontWeight.Normal),
    Font(R.font.ntype82_regular, FontWeight.Bold),
    Font(R.font.ntype82_regular, FontWeight.Medium)
)

// NDot55 — Signature Dot Matrix for Headers
val NDot55Family = FontFamily(
    Font(R.font.ndot55, FontWeight.Normal)
)

// NType82Mono — Monospace for technical data
val NType82MonoFamily = FontFamily(
    Font(R.font.ntype82mono_regular, FontWeight.Normal)
)

// ── Nothing Typography System ────────────────────────────────────────────────

val NothingTypography = Typography(
    // ── Display: Large Titles (NDot55 for branding impact) ──────────────────
    displayLarge = TextStyle(
        fontFamily = NDot55Family,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = NDot55Family,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = NDot55Family,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline: Section Headers (NType82 Bold) ────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // ── Title: Content Titles (NType82 Medium) ───────────────────────────────
    titleLarge = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body: General Text (NType82 Regular) ────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NType82Family,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ── Label: Technical info (NType82Mono) ─────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = NType82MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NType82MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NType82MonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)