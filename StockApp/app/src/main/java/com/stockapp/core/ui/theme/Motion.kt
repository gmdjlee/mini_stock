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
 * Motion system for StockApp.
 * Based on DESIGN_SYSTEM_SPEC.md v1.0.0
 *
 * Provides Material Design 3 animation specifications.
 *
 * Usage:
 * ```
 * animateFloatAsState(
 *     targetValue = 1f,
 *     animationSpec = MaterialTheme.motion.default
 * )
 * ```
 */
@Immutable
data class Motion(
    // ==========================================================================
    // Easing Functions - Material Design 3 cubic bezier curves
    // ==========================================================================

    /** Dynamic content changes - (0.2, 0.0, 0.0, 1.0) */
    val emphasized: CubicBezierEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    /** Elements entering - (0.05, 0.7, 0.1, 1.0) */
    val emphasizedDecelerate: CubicBezierEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f),
    /** Elements exiting - (0.3, 0.0, 0.8, 0.15) */
    val emphasizedAccelerate: CubicBezierEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f),
    /** Most UI transitions - (0.4, 0.0, 0.2, 1.0) */
    val standard: CubicBezierEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
    /** Standard entering - (0.0, 0.0, 0.2, 1.0) */
    val standardDecelerate: CubicBezierEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
    /** Standard exiting - (0.4, 0.0, 1.0, 1.0) */
    val standardAccelerate: CubicBezierEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f),

    // ==========================================================================
    // Duration Tokens (milliseconds)
    // ==========================================================================

    // Short durations
    val durationShort1: Int = 50,
    val durationShort2: Int = 100,
    val durationShort3: Int = 150,
    val durationShort4: Int = 200,

    // Medium durations
    val durationMedium1: Int = 250,
    val durationMedium2: Int = 300,
    val durationMedium3: Int = 350,
    val durationMedium4: Int = 400,

    // Long durations
    val durationLong1: Int = 450,
    val durationLong2: Int = 500,
    val durationLong3: Int = 550,
    val durationLong4: Int = 600,

    // Extra long durations
    val durationExtraLong1: Int = 700,
    val durationExtraLong2: Int = 800,
    val durationExtraLong3: Int = 900,
    val durationExtraLong4: Int = 1000
) {
    // ==========================================================================
    // Animation Specs
    // ==========================================================================

    /** Quick transitions - 200ms, standard easing (ripples, state changes) */
    val quick: DurationBasedAnimationSpec<Float>
        get() = tween(durationMillis = durationShort4, easing = standard)

    /** Default transitions - 300ms, standard easing (most transitions) */
    val default: DurationBasedAnimationSpec<Float>
        get() = tween(durationMillis = durationMedium2, easing = standard)

    /** Emphasized transitions - 400ms, emphasized easing (important actions) */
    val emphasizedSpec: DurationBasedAnimationSpec<Float>
        get() = tween(durationMillis = durationMedium4, easing = emphasized)

    /** Spring animation - medium bouncy (natural motion) */
    val springSpec: AnimationSpec<Float>
        get() = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )

    /** Expressive spring - low bouncy (playful interactions) */
    val expressiveSpring: AnimationSpec<Float>
        get() = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )

    // ==========================================================================
    // Convenience methods for common patterns
    // ==========================================================================

    /** Create a tween animation with quick duration */
    fun <T> quickTween(): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationShort4, easing = standard)

    /** Create a tween animation with default duration */
    fun <T> defaultTween(): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMedium2, easing = standard)

    /** Create a tween animation with emphasized duration */
    fun <T> emphasizedTween(): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMedium4, easing = emphasized)

    /** Create a tween animation with custom duration */
    fun <T> customTween(durationMillis: Int, easing: CubicBezierEasing = standard): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = easing)

    /** Create an enter animation spec (decelerate) */
    fun <T> enterTween(durationMillis: Int = durationMedium2): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = emphasizedDecelerate)

    /** Create an exit animation spec (accelerate) */
    fun <T> exitTween(durationMillis: Int = durationShort4): DurationBasedAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = emphasizedAccelerate)
}

/**
 * CompositionLocal for accessing Motion throughout the app.
 */
val LocalMotion = compositionLocalOf { Motion() }
