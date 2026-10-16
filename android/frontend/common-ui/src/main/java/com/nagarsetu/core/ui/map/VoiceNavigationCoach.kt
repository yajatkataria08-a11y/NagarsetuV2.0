package com.nagarsetu.core.ui.map

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import org.osmdroid.util.GeoPoint
import java.util.Locale

/**
 * Manages Google Maps-style spoken turn-by-turn navigation.
 *
 * Announcement strategy (mirrors Google Maps):
 *   • "In 500 metres, turn left onto MG Road"   — triggered at ~500 m from step
 *   • "In 200 metres, turn left"                — triggered at ~200 m
 *   • "Turn left now"                           — triggered at ~30 m
 *   • "You have arrived at your destination"    — on final step
 *
 * Call [updateLocation] on every GPS update while navigating.
 * Call [shutdown] when navigation ends.
 */
class VoiceNavigationCoach(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    // Tracks which (stepIndex, phase) combos have already been announced
    // to avoid repeating the same instruction
    private val announced = mutableSetOf<String>()

    // Current step index being tracked
    private var currentStepIndex = 0

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("hi", "IN"))
                    ?: TextToSpeech.LANG_MISSING_DATA
                // Fallback to English if Hindi TTS not installed
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(0.92f)   // slightly slower = clearer for navigation
                tts?.setPitch(1.0f)
                isReady = true
                Log.d("VoiceCoach", "TTS ready")
            } else {
                Log.e("VoiceCoach", "TTS init failed: $status")
            }
        }
    }

    /**
     * Call this every time the user's location updates while navigation is active.
     * Handles all announcement logic automatically.
     */
    fun updateLocation(userLocation: GeoPoint, steps: List<RouteStep>) {
        if (!isReady || steps.isEmpty()) return

        // Advance step pointer — skip steps whose start is now behind the user
        while (currentStepIndex < steps.size - 1) {
            val stepStart = steps[currentStepIndex].startPoint
            val distToCurrentStepStart = userLocation.distanceToAsDouble(stepStart)
            // If user is more than 80 m past where this step starts AND we've
            // already announced the "now" phase, move to the next step
            if (distToCurrentStepStart > 80 &&
                announced.contains("${currentStepIndex}_now")) {
                currentStepIndex++
                Log.d("VoiceCoach", "Advanced to step $currentStepIndex")
            } else {
                break
            }
        }

        val step = steps.getOrNull(currentStepIndex) ?: return
        val nextStepStart = steps.getOrNull(currentStepIndex + 1)?.startPoint ?: return

        // Distance from user to the START of the NEXT step (= where the turn happens)
        val distToTurn = userLocation.distanceToAsDouble(nextStepStart)

        val instruction = step.instruction

        when {
            // ~500 m warning — only for longer steps
            distToTurn in 400.0..600.0 &&
            step.distanceMeters > 300 &&
            !announced.contains("${currentStepIndex}_500") -> {
                speak("In 500 metres, $instruction")
                announced.add("${currentStepIndex}_500")
            }

            // ~200 m warning
            distToTurn in 150.0..250.0 &&
            !announced.contains("${currentStepIndex}_200") -> {
                val distWord = if (step.maneuverType == "arrive") ""
                               else "In 200 metres, "
                speak("${distWord}$instruction")
                announced.add("${currentStepIndex}_200")
            }

            // ~30 m — "now" announcement
            distToTurn in 0.0..50.0 &&
            !announced.contains("${currentStepIndex}_now") -> {
                val nowInstruction = when {
                    step.maneuverType == "arrive"          -> "You have arrived at your destination"
                    step.maneuverModifier.contains("left") -> instruction.replace("Turn left", "Turn left now")
                        .replace("Keep left", "Keep left now")
                        .replace("Take a sharp left", "Take a sharp left now")
                    step.maneuverModifier.contains("right")-> instruction.replace("Turn right", "Turn right now")
                        .replace("Keep right", "Keep right now")
                        .replace("Take a sharp right", "Take a sharp right now")
                    else -> "$instruction now"
                }
                speak(nowInstruction)
                announced.add("${currentStepIndex}_now")
            }
        }
    }

    /** Announce the first instruction when navigation starts. */
    fun announceStart(steps: List<RouteStep>, totalDistanceText: String) {
        if (!isReady || steps.isEmpty()) return
        val firstStep = steps.firstOrNull() ?: return
        val nextStep  = steps.getOrNull(1)
        currentStepIndex = 0
        announced.clear()
        val firstInstruction = nextStep?.let {
            "Navigation started. Total distance $totalDistanceText. " +
            "In ${firstStep.distanceText}, ${it.instruction}"
        } ?: "Navigation started. Total distance $totalDistanceText."
        speak(firstInstruction)
    }

    /** Call when user taps Stop or navigation finishes. */
    fun announceStop() {
        speak("Navigation ended.")
    }

    /** Mute / unmute without shutting down TTS. */
    fun setMuted(muted: Boolean) {
        if (muted) tts?.stop()
        _muted = muted
    }
    private var _muted = false

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        announced.clear()
    }

    private fun speak(text: String) {
        if (_muted || !isReady) return
        Log.d("VoiceCoach", "Speaking: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav_${System.currentTimeMillis()}")
    }
}
