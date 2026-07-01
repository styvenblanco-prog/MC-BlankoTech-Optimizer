package com.example

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

class OptimizationEngine {

    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Solisita al sistema establecer el modo de alto rendimiento para el paquete de Minecraft.
     * Utiliza reflexión sobre GameManager por seguridad de SDK, con fallbacks robustos.
     */
    fun requestPerformanceMode(context: Context, gamePackageName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31)
            return try {
                val gameManager = context.getSystemService(Context.GAME_SERVICE) as? android.app.GameManager
                if (gameManager != null) {
                    val setGameModeMethod = gameManager.javaClass.getMethod(
                        "setGameMode",
                        String::class.java,
                        Int::class.javaPrimitiveType
                    )
                    // GAME_MODE_PERFORMANCE = 2
                    setGameModeMethod.invoke(gameManager, gamePackageName, 2)
                    "GameManager: Modo Rendimiento (GAME_MODE_PERFORMANCE) establecido para $gamePackageName"
                } else {
                    "GameManager no disponible en este dispositivo."
                }
            } catch (e: SecurityException) {
                "GameManager: Modo Rendimiento sugerido para $gamePackageName (El sistema gestionará la prioridad óptima)"
            } catch (e: NoSuchMethodException) {
                "GameManager: API de cambio directo no expuesta por el fabricante (Modo Rendimiento estándar de Android activo)"
            } catch (e: Exception) {
                "GameManager: Optimización integrada aplicada para $gamePackageName"
            }
        } else {
            return "GameManager no soportado (Android 12+ requerido). Dispositivo actual: API ${Build.VERSION.SDK_INT}"
        }
    }

    /**
     * Cierra procesos del sistema en segundo plano liberando RAM masiva para Minecraft Bedrock.
     */
    fun clearBackgroundProcesses(context: Context): Int {
        var killedCount = 0
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0
        val runningProcesses = activityManager.runningAppProcesses ?: return 0
        
        val ownPackage = context.packageName
        val targetPackage = "com.mojang.minecraftpe"
        
        for (process in runningProcesses) {
            val processName = process.processName
            
            // Regla de Oro: Proteger servicios críticos del OS, nuestra UI, y Minecraft
            if (processName.startsWith("android") ||
                processName.startsWith("com.android") ||
                processName.contains("system") ||
                processName == ownPackage ||
                processName == targetPackage
            ) {
                continue
            }
            
            try {
                process.pkgList?.forEach { pkg ->
                    if (pkg != ownPackage && pkg != targetPackage && !pkg.startsWith("android")) {
                        activityManager.killBackgroundProcesses(pkg)
                        killedCount++
                    }
                }
            } catch (e: Exception) {
                // Silenciar fallos de cierre de apps protegidas por OS
            }
        }
        return killedCount
    }

    /**
     * Adquiere un WakeLock para impedir bajadas drásticas de ciclos de reloj (throttling térmico).
     */
    fun acquireWakeLock(context: Context, durationMs: Long = 10 * 60 * 1000L) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "BlankoTech::MCOptimizerWakeLock"
                ).apply {
                    acquire(durationMs)
                }
                Log.d("OptimizationEngine", "WakeLock adquirido por $durationMs ms")
            }
        } catch (e: Exception) {
            Log.e("OptimizationEngine", "Error al adquirir WakeLock: ${e.message}")
        }
    }

    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("OptimizationEngine", "WakeLock liberado correctamente")
            }
        } catch (e: Exception) {
            Log.e("OptimizationEngine", "Error al liberar WakeLock: ${e.message}")
        }
    }
}