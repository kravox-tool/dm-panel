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
                val targetId = params.optString("targetId", "")
                val message = params.optString("message", "")
                val count = params.optInt("count", 1)
                val delayMs = params.optLong("delayMs", 300L)
                if (token.isNotEmpty() && targetId.isNotEmpty()) {
                    botManager.startDmBurst(token, targetId, message, count, delayMs)
                }
            }
            2 -> { // CHAT
                val targetId = params.optString("targetId", "")
                val message = params.optString("message", "")
                if (token.isNotEmpty() && targetId.isNotEmpty()) {
                    botManager.sendChatMessage(token, targetId, message)
                }
            }
            3 -> { // MASIVO
                val serverId = params.optString("serverId", "")
                val message = params.optString("message", "")
                val count = params.optInt("count", 1)
                val includeBots = params.optBoolean("includeBots", false)
                if (token.isNotEmpty() && serverId.isNotEmpty()) {
                    botManager.startBroadcast(token, serverId, message, count, includeBots)
                }
            }
            4 -> { // FLASH
                val targetType = params.optInt("targetType", 1)
                val targetId = params.optString("targetId", "")
                val message = params.optString("message", "")
                val count = params.optInt("count", 1)
                val autoDeleteSec = params.optLong("autoDeleteSec", 0L)
                if (token.isNotEmpty() && targetId.isNotEmpty()) {
                    botManager.sendFlashMessage(token, targetType, targetId, message, count, autoDeleteSec)
                }
            }
            5 -> { // INFO
                scope.launch {
                    val info = botManager.getSystemInfoJson()
                    emitJs("window.onStatusUpdated(${info})")
                    emitJsLog("INFO", "Requisitos verificados: Kravox Tool | Python 3.10+ | discord.py >= 2.3.2 | Discord Gateway v10 REST API.", "[KRAVOX TOOL]")
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
    fun saveConfig(key: String, value: String) {
        triggerHaptic()
        scope.launch {
            db.configDao().saveConfig(ConfigEntity(key = key, value = value))
            emitJsLog("SUCCESS", "Configuración guardada en Room DB: $key", "[KRAVOX TOOL]")
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
            emitJsLog("INFO", "Historial de logs limpiado de Room DB.", "[KRAVOX TOOL]")
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
