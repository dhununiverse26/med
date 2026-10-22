package com.example.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PomodoroManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var appContext: Context? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(25 * 60)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private val _totalWorkTimeSeconds = MutableStateFlow(25 * 60)
    val totalWorkTimeSeconds: StateFlow<Int> = _totalWorkTimeSeconds.asStateFlow()

    private val _sessionName = MutableStateFlow("Focus Session")
    val sessionName: StateFlow<String> = _sessionName.asStateFlow()

    private val _completedSessions = MutableStateFlow(0)
    val completedSessions: StateFlow<Int> = _completedSessions.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun dismissCompleted() {
        _isCompleted.value = false
    }

    fun toggleTimer() {
        if (_isRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun startTimer() {
        _isRunning.value = true
        _isCompleted.value = false
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timeLeftSeconds.value > 0 && _isRunning.value) {
                delay(1000L)
                _timeLeftSeconds.value -= 1
            }
            if (_timeLeftSeconds.value <= 0) {
                _isRunning.value = false
                _timeLeftSeconds.value = _totalWorkTimeSeconds.value
                _completedSessions.value += 1
                _isCompleted.value = true
                triggerAlarm()
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _timeLeftSeconds.value = _totalWorkTimeSeconds.value
    }

    fun setCustomTime(minutes: Int, name: String = "Focus Session") {
        val seconds = (minutes.coerceAtLeast(1)) * 60
        _totalWorkTimeSeconds.value = seconds
        _timeLeftSeconds.value = seconds
        _sessionName.value = name
        pauseTimer()
    }

    fun adjustTime(deltaMinutes: Int) {
        val newTotal = (_totalWorkTimeSeconds.value + deltaMinutes * 60).coerceIn(60, 180 * 60)
        _totalWorkTimeSeconds.value = newTotal
        _timeLeftSeconds.value = (_timeLeftSeconds.value + deltaMinutes * 60).coerceIn(1, newTotal)
    }

    private fun triggerAlarm() {
        try {
            appContext?.let { ctx ->
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 400, 200, 400, 200, 600)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (_: Exception) {}
    }
}

