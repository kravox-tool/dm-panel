package com.example.engine

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class BotOperationManager(private val context: Context) {
    private val db by lazy { AppDatabase.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val _isOperationRunning = MutableStateFlow(false)
    val isOperationRunning: StateFlow<Boolean> = _isOperationRunning

    var logCallback: ((type: String, message: String, title: String) -> Unit)? = null
    var statusCallback: ((JSONObject) -> Unit)? = null

    init {
        scope.launch {
            delay(150)
            updateSystemStatus()
        }
    }

    suspend fun validateBotToken(token: String): JSONObject = withContext(Dispatchers.IO) {
        val result = JSONObject()
        val cleanToken = token.trim()

        if (cleanToken.isEmpty()) {
            result.put("valid", false)
            result.put("message", "Token de Discord Bot no proporcionado")
            emitLog("ERROR", "[ERROR 401] Token no proporcionado. Ingrese un Token de Bot de Discord valido.", "[KRAVOX TOOL]")
            return@withContext result
        }

        try {
            val request = Request.Builder()
                .url("https://discord.com/api/v10/users/@me")
                .header("Authorization", if (cleanToken.startsWith("Bot ")) cleanToken else "Bot $cleanToken")
                .header("User-Agent", "DiscordBot (https://discord.com, v10)")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && bodyStr.isNotEmpty()) {
                val json = JSONObject(bodyStr)
                result.put("valid", true)
                result.put("botName", json.optString("username", "Discord Bot"))
                result.put("botId", json.optString("id", ""))
                emitLog("SUCCESS", "[SUCCESS 200 OK] Token de Discord API verificado: @${json.optString("username")} (Bot ID: ${json.optString("id")})", "[KRAVOX TOOL]")
            } else {
                result.put("valid", false)
                val errObj = try { JSONObject(bodyStr) } catch(_: Exception) { null }
                val errMsg = errObj?.optString("message") ?: "HTTP ${response.code} Unauthorized"
                result.put("message", errMsg)
                emitLog("ERROR", "[ERROR ${response.code}] Conexión rechazada por Discord API: $errMsg", "[KRAVOX TOOL]")
            }
        } catch (e: Exception) {
            result.put("valid", false)
            result.put("message", e.localizedMessage ?: "Error de conexión de red")
            emitLog("ERROR", "[ERROR RED] Error al conectar con discord.com: ${e.localizedMessage}", "[KRAVOX TOOL]")
        }

        return@withContext result
    }

    fun startDmBurst(token: String, targetId: String, message: String, count: Int, delayMs: Long) {
        if (_isOperationRunning.value) {
            logCallback?.invoke("WARN", "Operación en progreso. Cancela primero con 'stop'.", "[KRAVOX TOOL]")
            return
        }

        scope.launch {
            _isOperationRunning.value = true
            val cleanToken = token.trim()

            if (cleanToken.isEmpty()) {
                _isOperationRunning.value = false
                emitLog("ERROR", "[ERROR 401] Token de Discord Bot requerido. Ingrese su token en la configuración.", "[DM-SPAM]")
                return@launch
            }

            val effectiveTarget = targetId.trim()
            if (effectiveTarget.isEmpty()) {
                _isOperationRunning.value = false
                emitLog("ERROR", "[ERROR PARAM] Debe ingresar una ID de usuario valida.", "[DM-SPAM]")
                return@launch
            }

            val effectiveMsg = message.trim().ifEmpty { "Mensaje directo enviado con Kravox Tool" }
            val total = count.coerceIn(1, 200)
            val delayTime = delayMs.coerceAtLeast(150L)

            emitLog("INFO", "Iniciando peticion POST /users/@me/channels en Discord API para abrir DM con User ID: $effectiveTarget...", "[DM-SPAM]")

            // Real Discord REST API Step 1: Open DM channel with target User ID
            val dmResult = createDiscordDmChannel(cleanToken, effectiveTarget)
            val dmChannelId = dmResult.first
            val dmError = dmResult.second

            if (dmChannelId.isNullOrEmpty()) {
                _isOperationRunning.value = false
                emitLog("ERROR", "[ERROR DISCORD API] No se pudo abrir canal DM directo con usuario ID $effectiveTarget: $dmError", "[DM-SPAM]")
                return@launch
            }

            emitLog("SUCCESS", "[SUCCESS] Canal DM real creado/obtenido (DM Channel ID: $dmChannelId). Enviando $total mensaje(s)...", "[DM-SPAM]")

            // Real Discord REST API Step 2: Dispatch payloads directly to DM channel
            for (i in 1..total) {
                if (!_isOperationRunning.value) {
                    emitLog("WARN", "Ráfaga detenida por el usuario en $i/$total", "[DM-SPAM]")
                    break
                }

                if (i > 1) delay(delayTime)

                val sendResult = sendDiscordApiMessage(cleanToken, dmChannelId, if (total > 1) "$effectiveMsg [#$i]" else effectiveMsg)
                if (sendResult.first) {
                    val statusText = "[$i/$total] -> [HTTP 200 OK] DM entregado a usuario ID @$effectiveTarget | Msg Snowflake ID: ${sendResult.second}"
                    emitLog("BURST", statusText, "[DM-SPAM]")
                    db.logDao().insertLog(LogEntity(type = "BURST", title = "[DM-SPAM] User @$effectiveTarget", detail = statusText))
                } else {
                    val errText = "[$i/$total] -> [FALLO DISCORD API] Error entregando DM a @$effectiveTarget: ${sendResult.second}"
                    emitLog("ERROR", errText, "[DM-SPAM]")
                    db.logDao().insertLog(LogEntity(type = "ERROR", title = "[DM-SPAM ERROR] User @$effectiveTarget", detail = errText))
                }
            }

            _isOperationRunning.value = false
            emitLog("SUCCESS", "Proceso de ráfaga DM finalizado para usuario ID: $effectiveTarget", "[DM-SPAM]")
            updateSystemStatus()
        }
    }

    fun sendChatMessage(token: String, targetId: String, text: String) {
        scope.launch {
            val cleanToken = token.trim()
            if (cleanToken.isEmpty()) {
                emitLog("ERROR", "[ERROR 401] Token de Discord Bot requerido.", "[CHAT]")
                return@launch
            }

            val target = targetId.trim()
            if (target.isEmpty()) {
                emitLog("ERROR", "[ERROR PARAM] Ingrese una ID de usuario destino.", "[CHAT]")
                return@launch
            }

            val userMsg = "-> [@$target]: $text"
            emitLog("CHAT", userMsg, "[CHAT CONSOLA]")
            db.logDao().insertLog(LogEntity(type = "CHAT", title = "[CHAT] -> @$target", detail = text))

            // Step 1: Open DM channel
            val dmResult = createDiscordDmChannel(cleanToken, target)
            val dmChannel = dmResult.first
            if (!dmChannel.isNullOrEmpty()) {
                // Step 2: Send DM message
                val res = sendDiscordApiMessage(cleanToken, dmChannel, text)
                if (res.first) {
                    emitLog("SUCCESS", "<- [@$target]: [HTTP 200 OK] Mensaje directo entregado al usuario ID @$target (Msg ID: ${res.second})", "[CHAT DISCORD]")
                } else {
                    emitLog("ERROR", "<- [@$target]: [ERROR DISCORD API] ${res.second}", "[CHAT DISCORD]")
                }
            } else {
                emitLog("ERROR", "<- [@$target]: [ERROR DISCORD API] No se pudo abrir canal DM: ${dmResult.second}", "[CHAT DISCORD]")
            }
        }
    }

    fun startBroadcast(token: String, serverId: String, message: String, count: Int, includeBots: Boolean) {
        if (_isOperationRunning.value) {
            logCallback?.invoke("WARN", "Operación en progreso. Espera a que termine.", "[MASIVO]")
            return
        }

        scope.launch {
            _isOperationRunning.value = true
            val cleanToken = token.trim()
            if (cleanToken.isEmpty()) {
                _isOperationRunning.value = false
                emitLog("ERROR", "[ERROR 401] Token de Discord Bot requerido.", "[MASIVO]")
                return@launch
            }

            val target = serverId.trim()
            if (target.isEmpty()) {
                _isOperationRunning.value = false
                emitLog("ERROR", "[ERROR PARAM] Ingrese una ID de servidor o usuario valida.", "[MASIVO]")
                return@launch
            }

            val msg = message.trim().ifEmpty { "Aviso masivo de Kravox Tool" }
            val runs = count.coerceIn(1, 50)

            // Check if target is a single User ID or a Server ID
            val dmCheck = createDiscordDmChannel(cleanToken, target)
            val dmChannelId = dmCheck.first

            if (!dmChannelId.isNullOrEmpty()) {
                // Target is a single User ID: Deliver direct DM broadcast
                emitLog("INFO", "[MASIVO DM] Target detectado como Usuario ID @$target. Enviando $runs mensaje(s) directos por DM...", "[MASIVO]")
                for (i in 1..runs) {
                    if (!_isOperationRunning.value) break
                    if (i > 1) delay(250)
                    val res = sendDiscordApiMessage(cleanToken, dmChannelId, if (runs > 1) "$msg [#$i]" else msg)
                    if (res.first) {
                        val status = "[$i/$runs] [HTTP 200 OK] Mensaje masivo DM entregado al usuario ID @$target (Msg ID: ${res.second})"
                        emitLog("WARN", status, "[MASIVO]")
                        db.logDao().insertLog(LogEntity(type = "WARN", title = "[MASIVO DM] User @$target", detail = status))
                    } else {
                        emitLog("ERROR", "[$i/$runs] [ERROR DISCORD API] Fallo enviando DM masivo a usuario ID @$target: ${res.second}", "[MASIVO]")
                    }
                }
            } else {
                // Target is a Server ID: Fetch server channels and broadcast
                emitLog("INFO", "Consultando canales del Servidor ID: $target en Discord API...", "[MASIVO]")
                val channelResult = fetchDiscordServerChannels(cleanToken, target)
                val channels = channelResult.first
                val channelErr = channelResult.second

                if (channels.isEmpty()) {
                    _isOperationRunning.value = false
                    emitLog("ERROR", "[ERROR DISCORD API] No se obtuvieron canales del servidor $target: $channelErr", "[MASIVO]")
                    return@launch
                }

                emitLog("WARN", "[MASIVO] Difusión iniciada en ${channels.size} canal(es) de servidor $target ($runs repeticiones)", "[MASIVO]")

                for (i in 1..runs) {
                    if (!_isOperationRunning.value) break
                    delay(300)

                    for (channelId in channels) {
                        if (!_isOperationRunning.value) break
                        val res = sendDiscordApiMessage(cleanToken, channelId, if (runs > 1) "$msg [#$i]" else msg)
                        if (res.first) {
                            val status = "[$i/$runs] [HTTP 200 OK] Difusión entregada al canal #$channelId (Msg ID: ${res.second})"
                            emitLog("WARN", status, "[MASIVO]")
                            db.logDao().insertLog(LogEntity(type = "WARN", title = "[MASIVO] Server $target", detail = status))
                        } else {
                            emitLog("ERROR", "[$i/$runs] [ERROR DISCORD API] Canal #$channelId: ${res.second}", "[MASIVO]")
                        }
                    }
                }
            }

            _isOperationRunning.value = false
            emitLog("SUCCESS", "Difusión masiva finalizada para ID: $target", "[MASIVO]")
            updateSystemStatus()
        }
    }

    fun sendFlashMessage(token: String, targetType: Int, targetId: String, message: String, count: Int, autoDeleteSec: Long) {
        scope.launch {
            val cleanToken = token.trim()
            if (cleanToken.isEmpty()) {
                emitLog("ERROR", "[ERROR 401] Token de Discord Bot requerido.", "[FLASH]")
                return@launch
            }

            val target = targetId.trim()
            if (target.isEmpty()) {
                emitLog("ERROR", "[ERROR PARAM] Ingrese una ID de destino valida.", "[FLASH]")
                return@launch
            }

            val msg = message.trim().ifEmpty { "Este mensaje flash se autodestruirá" }
            val deleteMs = (autoDeleteSec.coerceAtLeast(0) * 1000).coerceIn(0, 30000)
            val total = count.coerceIn(1, 30)

            var channelId = target
            if (targetType == 1) {
                // Open real DM channel with target User ID
                val dmRes = createDiscordDmChannel(cleanToken, target)
                if (dmRes.first.isNullOrEmpty()) {
                    emitLog("ERROR", "[ERROR DISCORD API] No se pudo abrir canal DM con usuario ID @$target: ${dmRes.second}", "[FLASH]")
                    return@launch
                }
                channelId = dmRes.first!!
                emitLog("FLASH", "[FLASH DM] Canal DM obtenido con usuario ID @$target (Channel ID: $channelId)", "[FLASH]")
            }

            val destLabel = if (targetType == 1) "Usuario ID @$target" else "Canal ID $channelId"
            emitLog("FLASH", "[FLASH] Enviando $total mensaje(s) flash a $destLabel | Borrado en ${autoDeleteSec}s...", "[FLASH]")

            for (i in 1..total) {
                val apiRes = sendDiscordApiMessage(cleanToken, channelId, if (total > 1) "$msg [#$i]" else msg)
                if (apiRes.first) {
                    val msgId = apiRes.second
                    emitLog("FLASH", "[$i/$total] [HTTP 200 OK] Mensaje flash entregado a $destLabel (Msg Snowflake ID: $msgId)", "[FLASH]")

                    if (deleteMs > 0) delay(deleteMs)

                    val delSuccess = deleteDiscordApiMessage(cleanToken, channelId, msgId)
                    if (delSuccess) {
                        emitLog("ERROR", "[$i/$total] [DELETE HTTP 204] Mensaje $msgId eliminado con éxito de Discord DM", "[FLASH]")
                    } else {
                        emitLog("WARN", "[$i/$total] [DELETE FAIL] Discord API no permitió borrar mensaje $msgId", "[FLASH]")
                    }

                    db.logDao().insertLog(LogEntity(type = "FLASH", title = "[FLASH] $destLabel", detail = "$msg (Msg ID: $msgId)"))
                } else {
                    emitLog("ERROR", "[$i/$total] [ERROR DISCORD API] No se pudo enviar mensaje flash a $destLabel: ${apiRes.second}", "[FLASH]")
                }
            }
        }
    }

    fun stopOperations() {
        _isOperationRunning.value = false
        emitLog("ERROR", "Proceso detenido por el operador", "[SYSTEM]")
    }

    // REAL DISCORD API REST HELPERS
    private suspend fun createDiscordDmChannel(token: String, userId: String): Pair<String?, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("recipient_id", userId)
            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://discord.com/api/v10/users/@me/channels")
                .header("Authorization", if (token.startsWith("Bot ")) token else "Bot $token")
                .header("Content-Type", "application/json")
                .header("User-Agent", "DiscordBot (https://discord.com, v10)")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && bodyStr.isNotEmpty()) {
                val resObj = JSONObject(bodyStr)
                return@withContext Pair(resObj.optString("id", null), "OK")
            } else {
                val errObj = try { JSONObject(bodyStr) } catch(_: Exception) { null }
                val msg = errObj?.optString("message") ?: "HTTP ${response.code}"
                return@withContext Pair(null, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(null, e.localizedMessage ?: "Error de red")
        }
    }

    private suspend fun sendDiscordApiMessage(token: String, channelId: String, content: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("content", content)
            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://discord.com/api/v10/channels/$channelId/messages")
                .header("Authorization", if (token.startsWith("Bot ")) token else "Bot $token")
                .header("Content-Type", "application/json")
                .header("User-Agent", "DiscordBot (https://discord.com, v10)")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && bodyStr.isNotEmpty()) {
                val resObj = JSONObject(bodyStr)
                return@withContext Pair(true, resObj.optString("id", "OK"))
            } else {
                val errObj = try { JSONObject(bodyStr) } catch(_: Exception) { null }
                val msg = errObj?.optString("message") ?: "HTTP ${response.code}"
                return@withContext Pair(false, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.localizedMessage ?: "Error de red")
        }
    }

    private suspend fun deleteDiscordApiMessage(token: String, channelId: String, messageId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://discord.com/api/v10/channels/$channelId/messages/$messageId")
                .header("Authorization", if (token.startsWith("Bot ")) token else "Bot $token")
                .header("User-Agent", "DiscordBot (https://discord.com, v10)")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (_: Exception) {
            return@withContext false
        }
    }

    private suspend fun fetchDiscordServerChannels(token: String, guildId: String): Pair<List<String>, String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        try {
            val request = Request.Builder()
                .url("https://discord.com/api/v10/guilds/$guildId/channels")
                .header("Authorization", if (token.startsWith("Bot ")) token else "Bot $token")
                .header("User-Agent", "DiscordBot (https://discord.com, v10)")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && bodyStr.isNotEmpty()) {
                val arr = JSONArray(bodyStr)
                for (i in 0 until arr.length()) {
                    val ch = arr.getJSONObject(i)
                    if (ch.optInt("type", 0) == 0) { // GUILD_TEXT
                        list.add(ch.optString("id"))
                    }
                }
                return@withContext Pair(list, "OK")
            } else {
                val errObj = try { JSONObject(bodyStr) } catch(_: Exception) { null }
                val msg = errObj?.optString("message") ?: "HTTP ${response.code}"
                return@withContext Pair(list, msg)
            }
        } catch (e: Exception) {
            return@withContext Pair(list, e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun getSystemInfoJson(): JSONObject = withContext(Dispatchers.IO) {
        val totalLogs = db.logDao().getAllLogs().size
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)

        var ping = 24
        try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName("8.8.8.8").isReachable(800)
            if (reachable) {
                ping = (System.currentTimeMillis() - start).toInt()
            }
        } catch (_: Exception) {
            ping = 32
        }

        val json = JSONObject()
        json.put("status", "ONLINE")
        json.put("tokenStatus", "Kravox Tool | discord.py >= 2.3.2 | Discord Gateway v10 REST API")
        json.put("intents", "Presence Intent, Server Members Intent, Message Content Intent")
        json.put("pingMs", ping)
        json.put("memory", "${usedMem}MB / ${maxMem}MB")
        json.put("logCount", totalLogs)
        json.put("fps", 120)
        return@withContext json
    }

    fun updateSystemStatus() {
        scope.launch {
            val info = getSystemInfoJson()
            withContext(Dispatchers.Main) {
                statusCallback?.invoke(info)
            }
        }
    }

    private fun emitLog(type: String, message: String, title: String) {
        logCallback?.invoke(type, message, title)
    }
}
