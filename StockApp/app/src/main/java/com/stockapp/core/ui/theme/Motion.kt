package com.stockapp.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * Motion system for StockApp with Material Design 3 animation specifications.
 */
@Immutable
data class Motion(
    // Easing (MD3 cubic bezier curves)
    val emphasized: CubicBezierEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val emphasizedDecelerate: CubicBezierEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f),
    val emphasizedAccelerate: CubicBezierEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f),
    val standard: CubicBezierEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
    val standardDecelerate: CubicBezierEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
    val standardAccelerate: CubicBezierEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f),

    // Durations (ms)
    val durationShort1: Int = 50,
    val durationShort2: Int = 100,
    val durationShort3: Int = 150,
    val durationShort4: Int = 200,
    val durationMedium1: Int = 250,
    val durationMedium2: Int = 300,
    val durationMedium3: Int = 350,
    val durationMedium4: Int = 400,
    val durationLong1: Int = 450,
    val durationLong2: Int = 500,
    val durationLong3: Int = 550,
    val durationLong4: Int = 600,
    val durationExtraLong1: Int = 700,
    val durationExtraLong2: Int = 800,
    val durationExtraLong3: Int = 900,
    val durationExtraLong4: Int = 1000
) {
    // Pre-built specs for Float animations
    val quick: DurationBasedAnimationSpec<Float>
        get() = tween(durationMillis = durationShort4, easing = standard)

    val default: DurationBasedAnimationSpec<Float>
        get() = tween(durationMillis = durationMedium2, easing = standard)

    val emphasizedSpec: DurationBasedAnimationSpec<Float>
        get() = tween(durationMillis = durationMedium4, easing = emphasized)

    val springSpec: AnimationSpec<Float>
        get() = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    val expressiveSpring: AnimationSpec<Float>
        get() = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)

    // Generic tween builders
    fun <T> quickTween(): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationShort4, easing = standard)

    fun <T> defaultTween(): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMedium2, easing = standard)

    fun <T> emphasizedTween(): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMedium4, easing = emphasized)

    fun <T> customTween(durationMillis: Int, easing: CubicBezierEasing = standard): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = easing)

    fun <T> enterTween(durationMillis: Int = durationMedium2): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = emphasizedDecelerate)

    fun <T> exitTween(durationMillis: Int = durationShort4): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = emphasizedAccelerate)
}

/**
 * CompositionLocal for accessing Motion throughout the app.
 */
val LocalMotion = compositionLocalOf { Motion() }
