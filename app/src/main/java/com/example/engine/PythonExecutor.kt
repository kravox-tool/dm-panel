package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PythonExecutor(
    private val onLogOutput: (String, String) -> Unit, // type, text
    private val onActionTriggered: (String, Map<String, String>) -> Unit // action, params
) {
    suspend fun executeScript(scriptCode: String): String = withContext(Dispatchers.IO) {
        val lines = scriptCode.lines()
        val variables = mutableMapOf<String, String>()
        val outputBuilder = StringBuilder()

        outputBuilder.appendLine("[PYTHON ENGINE] Starting execution v3.8-termux...")
        onLogOutput("INFO", "[PYTHON ENGINE] Execution started")

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val rawLine = lines[lineIndex].trim()
            lineIndex++

            if (rawLine.isEmpty() || rawLine.startsWith("#")) continue

            try {
                // Variable assignment: x = "hello" or count = 10
                if (rawLine.contains("=") && !rawLine.startsWith("if ") && !rawLine.startsWith("print") && !rawLine.startsWith("send_")) {
                    val parts = rawLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        var valStr = parts[1].trim().trim('"', '\'')
                        // Expand variables
                        variables.forEach { (k, v) ->
                            valStr = valStr.replace("{$k}", v).replace(k, v)
                        }
                        variables[key] = valStr
                        continue
                    }
                }

                // print(...)
                if (rawLine.startsWith("print(") && rawLine.endsWith(")")) {
                    val content = rawLine.substring(6, rawLine.length - 1).trim()
                    val resolved = evaluateString(content, variables)
                    outputBuilder.appendLine(resolved)
                    onLogOutput("SUCCESS", resolved)
                    continue
                }

                // sleep(...)
                if (rawLine.startsWith("sleep(") || rawLine.startsWith("time.sleep(")) {
                    val openParen = rawLine.indexOf("(")
                    val numStr = rawLine.substring(openParen + 1, rawLine.length - 1).trim()
                    val sec = numStr.toDoubleOrNull() ?: 1.0
                    onLogOutput("INFO", "[SLEEP] Waiting ${sec}s...")
                    delay((sec * 1000).toLong())
                    continue
                }

                // send_dm(target_id, message)
                if (rawLine.startsWith("send_dm(")) {
                    val argsStr = rawLine.substring(8, rawLine.length - 1)
                    val args = parseArgs(argsStr, variables)
                    val targetId = args.getOrNull(0) ?: "998877"
                    val message = args.getOrNull(1) ?: "Test direct message"
                    onActionTriggered("send_dm", mapOf("targetId" to targetId, "message" to message))
                    outputBuilder.appendLine("[ACTION DM] Sent to $targetId: $message")
                    onLogOutput("BURST", "[DM-SPAM] Target: $targetId | Msg: $message")
                    continue
                }

                // broadcast(message, count)
                if (rawLine.startsWith("broadcast(")) {
                    val argsStr = rawLine.substring(10, rawLine.length - 1)
                    val args = parseArgs(argsStr, variables)
                    val msg = args.getOrNull(0) ?: "Server Alert"
                    val count = args.getOrNull(1) ?: "3"
                    onActionTriggered("broadcast", mapOf("message" to msg, "count" to count))
                    outputBuilder.appendLine("[ACTION BROADCAST] $msg ($count times)")
                    onLogOutput("WARN", "[MASIVO] $msg x$count")
                    continue
                }

                // flash_msg(target_id, message, delete_ms)
                if (rawLine.startsWith("flash_msg(")) {
                    val argsStr = rawLine.substring(10, rawLine.length - 1)
                    val args = parseArgs(argsStr, variables)
                    val targetId = args.getOrNull(0) ?: "12345"
                    val msg = args.getOrNull(1) ?: "Flash Message"
                    val deleteMs = args.getOrNull(2) ?: "1000"
                    onActionTriggered("flash_msg", mapOf("targetId" to targetId, "message" to msg, "deleteMs" to deleteMs))
                    outputBuilder.appendLine("[ACTION FLASH] $targetId -> $msg (Auto-delete: ${deleteMs}ms)")
                    onLogOutput("FLASH", "[FLASH] $targetId | $msg | ${deleteMs}ms")
                    continue
                }

                // Default info
                onLogOutput("INFO", "[EXEC] $rawLine")
                outputBuilder.appendLine("[EXEC] $rawLine")
            } catch (e: Exception) {
                val err = "[ERROR] Line failed: $rawLine (${e.localizedMessage})"
                outputBuilder.appendLine(err)
                onLogOutput("ERROR", err)
            }
        }

        outputBuilder.appendLine("[PYTHON ENGINE] Execution completed successfully.")
        onLogOutput("SUCCESS", "[PYTHON ENGINE] Execution finished")
        return@withContext outputBuilder.toString()
    }

    private fun evaluateString(expr: String, vars: Map<String, String>): String {
        var clean = expr.trim()
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length - 1)
        }
        vars.forEach { (k, v) ->
            clean = clean.replace("{$k}", v).replace("$$k", v)
        }
        return clean
    }

    private fun parseArgs(argsStr: String, vars: Map<String, String>): List<String> {
        return argsStr.split(",").map { raw ->
            var item = raw.trim()
            if ((item.startsWith("\"") && item.endsWith("\"")) || (item.startsWith("'") && item.endsWith("'"))) {
                item = item.substring(1, item.length - 1)
            }
            vars.forEach { (k, v) ->
                item = item.replace("{$k}", v).replace("$$k", v)
            }
            item
        }
    }
}
