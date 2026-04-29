package com.example.nothingpodcast.ui.theme

import androidx.compose.ui.graphics.Color

// ── Nothing Core Palette ──────────────────────────────────────────────────
val NothingBlack       = Color(0xFF000000)
val NothingWhite       = Color(0xFFFFFFFF)

// Surfaces (AMOLED-first layering)
val NothingSurface     = Color(0xFF0D0D0D)
val NothingSurfaceHigh = Color(0xFF1A1A1A)
val NothingSurfaceMid  = Color(0xFF141414)

// Borders & dividers (Nothing X uses thin 1dp outlines)
val NothingBorder      = Color(0xFF2C2C2C)
val NothingBorderDim   = Color(0xFF1F1F1F)

// Typography hierarchy
val NothingOnBackground      = Color(0xFFFFFFFF)
val NothingOnSurface         = Color(0xFFFFFFFF)
val NothingOnSurfaceVariant  = Color(0xFF8A8A8A)  // secondary/hint text
val NothingOnSurfaceDim      = Color(0xFF5A5A5A)  // disabled / caption

// Accent — Nothing X uses white as the sole accent; keep a subtle warm tone for badges
val NothingAccent      = Color(0xFFFFFFFF)
val NothingAccentDim   = Color(0xFFCCCCCC)
val NothingRed         = Color(0xFFFF2A2A) // Nothing OS signature red accent

// Semantic
val NothingError       = Color(0xFFFF3B30)   // iOS-red, matches Nothing alert red
val NothingOnError     = Color(0xFFFFFFFF)

// Badge (unplayed episode count)
val NothingBadge       = NothingRed
val NothingOnBadge     = NothingWhite

// Progress / scrubber
val NothingProgress    = Color(0xFFFFFFFF)
val NothingProgressBg  = Color(0xFF2C2C2C)

// Light mode (minimal support)
val NothingLightSurface = Color(0xFFF5F5F5)