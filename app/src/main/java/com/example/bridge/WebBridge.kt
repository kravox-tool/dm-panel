package com.example.bridge

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.ConfigEntity
import com.example.data.ScriptEntity
import com.example.engine.BotOperationManager
import com.example.engine.PythonExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WebBridge(
    private val context: Context,
    private val webView: WebView,
    private val botManager: BotOperationManager
) {
    private val db by lazy { AppDatabase.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val pythonExecutor by lazy {
        PythonExecutor(
            onLogOutput = { type, text ->
                emitJsLog(type, text, "[PYTHON]")
            },
            onActionTriggered = { action, params ->
                val token = params["token"] ?: ""
                when (action) {
                    "send_dm" -> botManager.startDmBurst(
                        token,
                        params["targetId"] ?: "",
                        params["message"] ?: "",
                        1,
                        500
                    )
                    "broadcast" -> botManager.startBroadcast(
                        token,
                        params["serverId"] ?: "Python-Server",
                        params["message"] ?: "",
                        params["count"]?.toIntOrNull() ?: 3,
                        false
                    )
                    "flash_msg" -> botManager.sendFlashMessage(
                        token,
                        1,
                        params["targetId"] ?: "",
                        params["message"] ?: "",
                        1,
                        params["deleteSec"]?.toLongOrNull() ?: 1L
                    )
                }
            }
        )
    }

    init {
        botManager.logCallback = { type, message, title ->
            emitJsLog(type, message, title)
        }
        botManager.statusCallback = { infoJson ->
            emitJs("window.onStatusUpdated(${infoJson})")
        }
    }

    @JavascriptInterface
    fun executeOption(optionNumber: Int, paramsJsonStr: String?) {
        triggerHaptic()
        val params = try {
            if (!paramsJsonStr.isNullOrEmpty()) JSONObject(paramsJsonStr) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }

        val token = params.optString("token", "")

        when (optionNumber) {
            1 -> { // DM-SPAM
                val targetId = params.optString("targetId", "8849201")
                val message = params.optString("message", "Aviso directo")
                val count = params.optInt("count", 1)
                val delayMs = params.optLong("delayMs", 300L)
                botManager.startDmBurst(token, targetId, message, count, delayMs)
            }
            2 -> { // CHAT
                val targetId = params.optString("targetId", "7788")
                val message = params.optString("message", "Hola consola")
                botManager.sendChatMessage(token, targetId, message)
            }
            3 -> { // MASIVO
                val serverId = params.optString("serverId", "556")
                val message = params.optString("message", "Difusion masiva activa")
                val count = params.optInt("count", 1)
                val includeBots = params.optBoolean("includeBots", false)
                botManager.startBroadcast(token, serverId, message, count, includeBots)
            }
            4 -> { // FLASH
                val targetType = params.optInt("targetType", 1)
                val targetId = params.optString("targetId", "78")
                val message = params.optString("message", "Este mensaje expira")
                val count = params.optInt("count", 1)
                val autoDeleteSec = params.optLong("autoDeleteSec", 0L)
                botManager.sendFlashMessage(token, targetType, targetId, message, count, autoDeleteSec)
            }
            5 -> { // INFO
                scope.launch {
                    val info = botManager.getSystemInfoJson()
                    emitJs("window.onStatusUpdated(${info})")
                    emitJsLog("INFO", "Requisitos verificados: Termux, Python 3.10+, discord.py >= 2.3.2, prompt_toolkit.", "[REQUISITOS]")
                }
            }
            6 -> { // PYTHON
                val code = params.optString("code", "")
                if (code.isNotEmpty()) {
                    runPythonScript(code)
                }
            }
            0 -> { // SALIR
                exitApp()
            }
        }
    }

    @JavascriptInterface
    fun validateBotToken(token: String) {
        scope.launch {
            val res = botManager.validateBotToken(token)
            withContext(Dispatchers.Main) {
                emitJs("window.onTokenValidated(${res})")
            }
        }
    }

    @JavascriptInterface
    fun startDmBurst(token: String, targetId: String, message: String, count: Int, delayMs: Long) {
        triggerHaptic()
        botManager.startDmBurst(token, targetId, message, count, delayMs)
    }

    @JavascriptInterface
    fun sendChatMessage(token: String, targetId: String, message: String) {
        triggerHaptic()
        botManager.sendChatMessage(token, targetId, message)
    }

    @JavascriptInterface
    fun startBroadcast(token: String, serverId: String, message: String, count: Int, includeBots: Boolean) {
        triggerHaptic()
        botManager.startBroadcast(token, serverId, message, count, includeBots)
    }

    @JavascriptInterface
    fun sendFlashMessage(token: String, targetType: Int, targetId: String, message: String, count: Int, autoDeleteSec: Long) {
        triggerHaptic()
        botManager.sendFlashMessage(token, targetType, targetId, message, count, autoDeleteSec)
    }

    @JavascriptInterface
    fun stopOperations() {
        triggerHaptic()
        botManager.stopOperations()
    }

    @JavascriptInterface
    fun runPythonScript(scriptCode: String) {
        triggerHaptic()
        scope.launch {
            emitJsLog("INFO", "Ejecutando script discord.py...", "[PYTHON ENGINE]")
            val output = pythonExecutor.executeScript(scriptCode)
            db.scriptDao().insertScript(
                ScriptEntity(title = "Script #${System.currentTimeMillis() % 1000}", code = scriptCode)
            )
            withContext(Dispatchers.Main) {
                emitJs("window.onScriptFinished(${JSONObject.quote(output)})")
            }
        }
    }

    @JavascriptInterface
    fun saveConfig(key: String, value: String) {
        triggerHaptic()
        scope.launch {
            db.configDao().saveConfig(ConfigEntity(key = key, value = value))
            emitJsLog("SUCCESS", "Configuración guardada en Room DB: $key", "[SYSTEM CONFIG]")
            showToast("Configuración '$key' guardada")
        }
    }

    @JavascriptInterface
    fun loadConfigs() {
        scope.launch {
            val list = db.configDao().getAllConfigs()
            val jsonArr = JSONArray()
            list.forEach { item ->
                val obj = JSONObject()
                obj.put("key", item.key)
                obj.put("value", item.value)
                jsonArr.put(obj)
            }
            emitJs("window.onConfigsLoaded(${jsonArr})")
        }
    }

    @JavascriptInterface
    fun loadLogs() {
        scope.launch {
            val logs = db.logDao().getAllLogs()
            val jsonArr = JSONArray()
            logs.forEach { log ->
                val obj = JSONObject()
                obj.put("id", log.id)
                obj.put("type", log.type)
                obj.put("title", log.title)
                obj.put("detail", log.detail)
                obj.put("timestamp", log.timestamp)
                jsonArr.put(obj)
            }
            emitJs("window.onHistoryLoaded(${jsonArr})")
        }
    }

    @JavascriptInterface
    fun clearLogs() {
        triggerHaptic()
        scope.launch {
            db.logDao().clearAll()
            emitJsLog("INFO", "Historial de logs limpiado de Room DB.", "[SYSTEM]")
            showToast("Logs eliminados")
        }
    }

    @JavascriptInterface
    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun exitApp() {
        scope.launch(Dispatchers.Main) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun emitJsLog(type: String, text: String, title: String) {
        val json = JSONObject()
        json.put("type", type)
        json.put("text", text)
        json.put("title", title)
        json.put("time", System.currentTimeMillis())
        emitJs("window.onLogAdded(${json})")
    }

    private fun emitJs(script: String) {
        scope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(script, null)
        }
    }
}
