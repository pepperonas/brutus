package com.pepperonas.brutus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Expressive shape scale: rounder than stock M3 across the board so cards and
 * sheets read soft while the hard red does the shouting. Interaction hotspots
 * (press-morphs on the big alarm CTAs) build on top of these per component.
 */
val BrutusExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
