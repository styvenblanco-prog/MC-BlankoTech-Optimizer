package com.blankotech.optimizer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OptimizerUiState(
    val currentStep: OptimizationStep = OptimizationStep.IDLE,
    val statusText: String = "Dispositivo listo para la optimización",
    val killedProcesses: Int = 0,
    val gameModeStatus: String = "",
    val isOptimizing: Boolean = false,
    val errorMessage: String? = null
)

enum class OptimizationStep {
    IDLE,
    WAKELOCK,
    RAM_CLEANING,
    GAMEMODE,
    LAUNCHING,
    FINISHED,
    ERROR
}

class OptimizerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizerUiState())
    val uiState: StateFlow<OptimizerUiState> = _uiState.asStateFlow()

    private val optimizerEngine = OptimizationEngine()
    private val minecraftPackage = "com.mojang.minecraftpe"

    fun startOptimization(context: Context, onLaunchSuccess: () -> Unit) {
        if (_uiState.value.isOptimizing) return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isOptimizing = true, 
                    currentStep = OptimizationStep.WAKELOCK,
                    statusText = "Adquiriendo WakeLock...",
                    errorMessage = null
                ) 
            }
            delay(600)

            // 1. WakeLock
            optimizerEngine.acquireWakeLock(context)
            
            // 2. RAM Cleaner
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.RAM_CLEANING,
                    statusText = "Limpiando procesos de fondo..."
                ) 
            }
            delay(800)
            
            val killed = withContext(Dispatchers.IO) {
                optimizerEngine.clearBackgroundProcesses(context)
            }
            
            _uiState.update { 
                it.copy(
                    killedProcesses = killed,
                    statusText = "Procesos cerrados: $killed"
                ) 
            }
            delay(1000)

            // 3. GameMode
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.GAMEMODE,
                    statusText = "Configurando modo de juego..."
                ) 
            }
            delay(800)
            
            val gameModeResult = optimizerEngine.requestPerformanceMode(context, minecraftPackage)
            _uiState.update { 
                it.copy(
                    gameModeStatus = gameModeResult,
                    statusText = gameModeResult
                ) 
            }
            delay(1000)

            // 4. Launching
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.LAUNCHING,
                    statusText = "Iniciando Minecraft..."
                ) 
            }
            delay(1000)

            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(minecraftPackage)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    _uiState.update { 
                        it.copy(
                            currentStep = OptimizationStep.FINISHED,
                            statusText = "¡Minecraft iniciado!",
                            isOptimizing = false
                        ) 
                    }
                    onLaunchSuccess()
                } else {
                    throw Exception("Minecraft no está instalado")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        currentStep = OptimizationStep.ERROR,
                        statusText = "Error: ${e.message}",
                        isOptimizing = false,
                        errorMessage = e.message
                    ) 
                }
            }
        }
    }
}
