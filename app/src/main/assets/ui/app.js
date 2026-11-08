// DM PANEL - KRAVOX TOOL REACTIVE CONTROLLER

let activeOption = 0;
let savedToken = "";

document.addEventListener("DOMContentLoaded", () => {
    const input = document.getElementById("cli-input");
    if (input) {
        input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                submitCliInput();
            }
        });
    }

    if (window.AndroidBridge && typeof window.AndroidBridge.loadConfigs === 'function') {
        window.AndroidBridge.loadConfigs();
    }
});

function goBackToMenu() {
    activeOption = 0;
    if (window.AndroidBridge && typeof window.AndroidBridge.triggerHaptic === 'function') {
        window.AndroidBridge.triggerHaptic();
    }

    const mainMenu = document.getElementById("main-menu");
    const quickNav = document.querySelector(".quick-nav");
    const panel = document.getElementById("action-panel");
    const content = document.getElementById("panel-content");

    if (mainMenu) mainMenu.classList.remove("hidden");
    if (quickNav) quickNav.classList.remove("hidden");
    if (panel) panel.classList.add("hidden");
    if (content) content.innerHTML = "";

    appendLogLine("SYS", "[KRAVOX TOOL] Regreso al menú principal.");
}

function selectOption(optNum) {
    activeOption = optNum;
    if (window.AndroidBridge && typeof window.AndroidBridge.triggerHaptic === 'function') {
        window.AndroidBridge.triggerHaptic();
    }

    const mainMenu = document.getElementById("main-menu");
    const quickNav = document.querySelector(".quick-nav");
    const panel = document.getElementById("action-panel");
    const content = document.getElementById("panel-content");

    if (optNum === 0) {
        if (panel) panel.classList.add("hidden");
        appendLogLine("ERROR", "[KRAVOX TOOL] Cerrando el lanzador...");
        if (window.AndroidBridge && typeof window.AndroidBridge.exitApp === 'function') {
            window.AndroidBridge.exitApp();
        }
        return;
    }

    if (mainMenu) mainMenu.classList.add("hidden");
    if (quickNav) quickNav.classList.add("hidden");
    if (panel) panel.classList.remove("hidden");

    const backButtonHtml = `
        <button onclick="goBackToMenu()" class="btn-back-menu">
            <span>&lt;-</span> VOLVER AL MENÚ PRINCIPAL
        </button>
    `;

    switch(optNum) {
        case 1: // DM-SPAM :: RAFAGA A UNA ID
            content.innerHTML = `
                ${backButtonHtml}
                <div class="divider">====================================================================</div>
                <div class="panel-sub-header">DM-SPAM :: RAFAGA A UNA ID</div>
                <div class="divider">====================================================================</div>

                <div class="section-label">[ CONFIGURACION DE CONEXION ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Coloca el token del bot:</span>
                    <input type="text" id="dm-token" class="form-input" style="flex:1;" value="${savedToken}" placeholder="Token de Discord Bot">
                </div>

                <div class="section-label">[ CONFIGURACION DEL OBJETIVO ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Coloca la ID del usuario:</span>
                    <input type="text" id="dm-target" class="form-input" style="flex:1;" value="" placeholder="ID del usuario objetivo">
                </div>

                <div class="section-label">[ CONFIGURACION DEL MENSAJE ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Escribe tu mensaje personalizado:</span>
                    <input type="text" id="dm-msg" class="form-input" style="flex:1;" value="" placeholder="Mensaje exacto a enviar">
                </div>

                <div class="section-label">[ CONFIGURACION DE CANTIDAD ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Cuantos mensajes enviar? (ENTER = 1):</span>
                    <input type="number" id="dm-count" class="form-input" style="flex:1;" value="1" min="1" max="200">
                </div>

                <button onclick="launchDmBurst()" class="btn-primary">⚡ INICIAR RÁFAGA DM DISCORD API</button>
                <div style="margin-top:10px;">${backButtonHtml}</div>
            `;
            break;

        case 2: // CHAT MEDIA (LIVE 2-WAY DM CHAT)
            content.innerHTML = `
                ${backButtonHtml}
                <pre class="ascii-art ascii-white">
  ██████╗ ███╗   ███╗    ██████╗  █████╗ ███╗   ██╗███████╗██╗
  ██╔══██╗████╗ ████║    ██╔══██╗██╔══██╗████╗  ██║██╔════╝██║
  ██║  ██║██╔████╔██║    ██████╔╝███████║██╔██╗ ██║█████╗  ██║
  ██║  ██║██║╚██╔╝██║    ██╔═══╝ ██╔══██║██║╚██╗██║██╔══╝  ██║
  ██████╔╝██║ ╚═╝ ██║    ██║     ██║  ██║██║ ╚████║███████╗███████╗
  ╚═════╝ ╚═╝     ╚═╝    ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═══╝╚══════╝╚══════╝</pre>

                <div class="cyan-box">PARAMETROS DE ACCESO CHAT MEDIA</div>

                <div class="wizard-step-box step-green">
                    <div class="section-label-green">PASO 1: TOKEN DEL BOT</div>
                    <div>Ingresa el Token:</div>
                    <div class="wizard-prompt-line">
                        <span class="prompt-symbol">-&gt;</span>
                        <input type="text" id="chat-token" class="form-input" style="flex:1;" value="${savedToken}" placeholder="Token de Discord Bot">
                    </div>
                </div>

                <div class="wizard-step-box step-cyan">
                    <div class="section-label-cyan">PASO 2: ID DEL USUARIO OBJETIVO</div>
                    <div>Ingresa la ID:</div>
                    <div class="wizard-prompt-line">
                        <span class="prompt-symbol">-&gt;</span>
                        <input type="text" id="chat-target" class="form-input" style="flex:1;" value="" placeholder="ID del usuario">
                    </div>
                </div>

                <div class="wizard-step-box step-purple">
                    <div class="section-label-purple">PASO 3: MODO DE SESION</div>
                    <div>1 -&gt; Entrar directamente al chat en vivo por DM</div>
                    <div>2 -&gt; Consultar historial guardado con esta ID</div>
                    <div class="wizard-prompt-line" style="margin-top:6px;">
                        <span class="prompt-symbol">└─&gt; Elige opcion (1 o 2):</span>
                        <input type="number" id="chat-mode" class="form-input" style="width:70px;" value="1" min="1" max="2">
                    </div>
                </div>

                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">Mensaje a enviar por DM:</span>
                    <input type="text" id="chat-msg" class="form-input" style="flex:1;" placeholder="Escribe tu respuesta al usuario...">
                </div>

                <button onclick="launchChatSession()" class="btn-primary">[*] ENVIAR Y ESCUCHAR CHAT EN VIVO POR CONSOLA</button>
                <div style="margin-top:10px;">${backButtonHtml}</div>
            `;
            break;

        case 3: // MASIVO :: TODOS LOS USUARIOS DEL SERVIDOR POR DM
            content.innerHTML = `
                ${backButtonHtml}
                <div class="divider">====================================================================</div>
                <div class="panel-sub-header">MASIVO :: DM A TODOS LOS MIEMBROS DEL SERVIDOR</div>
                <div class="divider">====================================================================</div>

                <div class="section-label">[ CONFIGURACION DE CONEXION ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Coloca el token del bot:</span>
                    <input type="text" id="masivo-token" class="form-input" style="flex:1;" value="${savedToken}" placeholder="Token de Discord Bot">
                </div>

                <div class="section-label">[ CONFIGURACION DEL SERVIDOR ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Coloca la ID del servidor:</span>
                    <input type="text" id="masivo-server" class="form-input" style="flex:1;" value="" placeholder="ID del servidor de Discord">
                </div>

                <div class="section-label">[ CONFIGURACION DEL MENSAJE ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Escribe tu mensaje personalizado:</span>
                    <input type="text" id="masivo-msg" class="form-input" style="flex:1;" value="" placeholder="Mensaje masivo para DMs de miembros">
                </div>

                <div class="section-label">[ CONFIGURACION DE CANTIDAD ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Cuantos mensajes por usuario? (ENTER = 1):</span>
                    <input type="number" id="masivo-count" class="form-input" style="flex:1;" value="1" min="1" max="50">
                </div>

                <div class="section-label">[ OPCIONES ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Incluir bots tambien? (s/N):</span>
                    <input type="text" id="masivo-bots" class="form-input" style="width:70px;" value="N" maxlength="1">
                </div>

                <button onclick="launchMasivo()" class="btn-primary">🚀 INICIAR DIFUSIÓN DM A MIEMBROS DEL SERVIDOR</button>
                <div style="margin-top:10px;">${backButtonHtml}</div>
            `;
            break;

        case 4: // FLASH :: ENVIAR Y BORRAR
            content.innerHTML = `
                ${backButtonHtml}
                <div class="divider">====================================================================</div>
                <div class="panel-sub-header">FLASH :: ENVIAR Y BORRAR</div>
                <div class="divider">====================================================================</div>

                <div class="section-label">[ CONFIGURACION DE CONEXION ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Coloca el token del bot:</span>
                    <input type="text" id="flash-token" class="form-input" style="flex:1;" value="${savedToken}" placeholder="Token de Discord Bot">
                </div>

                <div class="section-label">[ DESTINO ]</div>
                <div>[1] Un usuario por DM (por User ID)</div>
                <div>[2] Un canal de servidor (por Channel ID)</div>
                <div class="wizard-prompt-line" style="margin-top:6px;">
                    <span class="prompt-symbol">-&gt; Elige 1 o 2:</span>
                    <input type="number" id="flash-type" class="form-input" style="width:70px;" value="1" min="1" max="2" onchange="updateFlashLabel()">
                </div>
                <div class="wizard-prompt-line">
                    <span id="flash-id-label" class="prompt-symbol">-&gt; ID del usuario destino:</span>
                    <input type="text" id="flash-target" class="form-input" style="flex:1;" value="" placeholder="ID de destino">
                </div>

                <div class="section-label">[ CONFIGURACION DEL MENSAJE ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Escribe tu mensaje:</span>
                    <input type="text" id="flash-msg" class="form-input" style="flex:1;" value="" placeholder="Mensaje flash a borrar">
                </div>

                <div class="section-label">[ CONFIGURACION DE CANTIDAD ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Cuantos mensajes enviar? (ENTER = 1):</span>
                    <input type="number" id="flash-count" class="form-input" style="flex:1;" value="1" min="1" max="30">
                </div>

                <div class="section-label">[ TIEMPO DE BORRADO ]</div>
                <div class="wizard-prompt-line">
                    <span class="prompt-symbol">-&gt; Segundos antes de borrar (0 = inmediato):</span>
                    <input type="number" id="flash-delete-sec" class="form-input" style="flex:1;" value="0" min="0" max="60">
                </div>

                <button onclick="launchFlash()" class="btn-primary">🔥 ENVIAR Y AUTODESTRUIR MENSAJE FLASH</button>
                <div style="margin-top:10px;">${backButtonHtml}</div>
            `;
            break;

        case 5: // INFORMACION / REQUISITOS
            content.innerHTML = `
                ${backButtonHtml}
                <div class="divider">====================================================================</div>
                <div class="panel-sub-header">INFORMACION / REQUISITOS - KRAVOX TOOL</div>
                <div class="divider">====================================================================</div>

                <div class="info-block">
                    <div class="info-title">FUNCIONES DEL PANEL</div>
                    <div class="info-list">
                        <div>- <b>DM-SPAM</b>  rafaga de DMs directos a una ID</div>
                        <div>- <b>CHAT</b>     chat en vivo bidireccional por consola con una ID de usuario</div>
                        <div>- <b>MASIVO</b>   mensaje masivo por DM a todos los miembros de un servidor</div>
                        <div>- <b>FLASH</b>    enviar mensaje por DM o canal y borrar rapido</div>
                    </div>

                    <div class="info-title">REQUISITOS</div>
                    <div class="info-list">
                        <div>- Token de Bot de Discord valido con Privileged Gateway Intents</div>
                        <div>- Python 3.10 o superior / discord.py &gt;= 2.3.2</div>
                        <div>- Conexión a Internet para la API REST de Discord</div>
                    </div>

                    <div class="info-title">COMO OBTENER EL TOKEN DEL BOT</div>
                    <div class="info-list">
                        <div>- Entra a <u>https://discord.com/developers/applications</u></div>
                        <div>- Selecciona tu aplicación &gt; Bot &gt; Reset Token &gt; Copia el token</div>
                    </div>

                    <div class="info-title">INTENTS NECESARIOS EN EL PORTAL</div>
                    <div class="info-list">
                        <div>- Presence Intent</div>
                        <div>- Server Members Intent</div>
                        <div>- Message Content Intent</div>
                    </div>

                    <div class="info-title">COMO OBTENER UNA ID</div>
                    <div class="info-list">
                        <div>- Ajustes de Discord &gt; Avanzado &gt; Modo desarrollador</div>
                        <div>- Clic derecho sobre el usuario o servidor &gt; Copiar ID</div>
                    </div>
                </div>

                <div style="margin-top:14px;">${backButtonHtml}</div>
            `;
            break;
    }

    if (window.AndroidBridge && typeof window.AndroidBridge.executeOption === 'function') {
        window.AndroidBridge.executeOption(optNum, "");
    }
}

function updateFlashLabel() {
    const type = document.getElementById("flash-type")?.value;
    const label = document.getElementById("flash-id-label");
    if (label) {
        if (type === "1") {
            label.textContent = "-> ID del usuario destino:";
        } else {
            label.textContent = "-> ID del canal destino:";
        }
    }
}

function submitCliInput() {
    const input = document.getElementById("cli-input");
    if (!input) return;
    const val = input.value.trim().toLowerCase();
    input.value = "";

    if (!val) return;

    appendLogLine("INFO", `-> ${val}`);

    if (val === "1" || val === "dm") selectOption(1);
    else if (val === "2" || val === "chat") selectOption(2);
    else if (val === "3" || val === "masivo") selectOption(3);
    else if (val === "4" || val === "flash") selectOption(4);
    else if (val === "5" || val === "info") selectOption(5);
    else if (val === "0" || val === "salir" || val === "exit") selectOption(0);
    else if (val === "clear" || val === "cls") clearTerminalLog();
    else if (val === "matrix") triggerMatrix();
    else if (val === "history") loadHistory();
    else if (val === "help") {
        appendLogLine("WARN", "Comandos: 1 (DM-SPAM), 2 (CHAT), 3 (MASIVO), 4 (FLASH), 5 (INFO), 0 (SALIR), clear, history, matrix, stop");
    } else if (val === "stop") {
        stopActiveProcess();
    } else {
        appendLogLine("ERROR", `Comando no reconocido: '${val}'. Escribe 'help' o elige 0-5.`);
    }
}

// STRICT LAUNCHERS WITH EXACT TOKEN PASSING
function launchDmBurst() {
    const token = document.getElementById("dm-token")?.value.trim() || "";
    const target = document.getElementById("dm-target")?.value.trim() || "";
    const msg = document.getElementById("dm-msg")?.value.trim() || "";
    const count = parseInt(document.getElementById("dm-count")?.value || "1");

    if (!token) {
        appendLogLine("ERROR", "[ERROR 401] Token de Bot requerido. Ingrese su Discord Bot Token.");
        return;
    }

    if (!target) {
        appendLogLine("ERROR", "[ERROR PARAM] Ingrese una ID de usuario destino.");
        return;
    }

    savedToken = token;
    if (window.AndroidBridge && typeof window.AndroidBridge.saveConfig === 'function') {
        window.AndroidBridge.saveConfig("BOT_TOKEN", token);
    }

    if (window.AndroidBridge && typeof window.AndroidBridge.startDmBurst === 'function') {
        window.AndroidBridge.startDmBurst(token, target, msg, count, 300);
    }
}

function launchChatSession() {
    const token = document.getElementById("chat-token")?.value.trim() || "";
    const target = document.getElementById("chat-target")?.value.trim() || "";
    const mode = parseInt(document.getElementById("chat-mode")?.value || "1");
    const msg = document.getElementById("chat-msg")?.value.trim() || "";

    if (!token) {
        appendLogLine("ERROR", "[ERROR 401] Token de Bot requerido.");
        return;
    }

    if (!target) {
        appendLogLine("ERROR", "[ERROR PARAM] Ingrese una ID de usuario destino.");
        return;
    }

    savedToken = token;

    if (mode === 2) {
        appendLogLine("INFO", `[*] Consultando historial guardado con ID @${target}...`);
        loadHistory();
        return;
    }

    if (msg && window.AndroidBridge && typeof window.AndroidBridge.sendChatMessage === 'function') {
        window.AndroidBridge.sendChatMessage(token, target, msg);
        const input = document.getElementById("chat-msg");
        if (input) input.value = "";
    }
}

function launchMasivo() {
    const token = document.getElementById("masivo-token")?.value.trim() || "";
    const server = document.getElementById("masivo-server")?.value.trim() || "";
    const msg = document.getElementById("masivo-msg")?.value.trim() || "";
    const count = parseInt(document.getElementById("masivo-count")?.value || "1");
    const includeBotsStr = document.getElementById("masivo-bots")?.value || "N";
    const includeBots = includeBotsStr.toLowerCase() === "s";

    if (!token) {
        appendLogLine("ERROR", "[ERROR 401] Token de Bot requerido.");
        return;
    }

    if (!server) {
        appendLogLine("ERROR", "[ERROR PARAM] Ingrese una ID de servidor valida.");
        return;
    }

    savedToken = token;

    if (window.AndroidBridge && typeof window.AndroidBridge.startBroadcast === 'function') {
        window.AndroidBridge.startBroadcast(token, server, msg, count, includeBots);
    }
}

function launchFlash() {
    const token = document.getElementById("flash-token")?.value.trim() || "";
    const targetType = parseInt(document.getElementById("flash-type")?.value || "1");
    const target = document.getElementById("flash-target")?.value.trim() || "";
    const msg = document.getElementById("flash-msg")?.value.trim() || "";
    const count = parseInt(document.getElementById("flash-count")?.value || "1");
    const autoDeleteSec = parseInt(document.getElementById("flash-delete-sec")?.value || "0");

    if (!token) {
        appendLogLine("ERROR", "[ERROR 401] Token de Bot requerido. Por favor rellene el token.");
        return;
    }

    if (!target) {
        appendLogLine("ERROR", "[ERROR PARAM] Ingrese una ID de destino.");
        return;
    }

    savedToken = token;

    if (window.AndroidBridge && typeof window.AndroidBridge.sendFlashMessage === 'function') {
        window.AndroidBridge.sendFlashMessage(token, targetType, target, msg, count, autoDeleteSec);
    } else {
        appendLogLine("ERROR", "[ERROR] Interfaz nativa sendFlashMessage no encontrada.");
    }
}

function stopActiveProcess() {
    if (window.AndroidBridge && typeof window.AndroidBridge.stopOperations === 'function') {
        window.AndroidBridge.stopOperations();
    }
}

function loadHistory() {
    if (window.AndroidBridge && typeof window.AndroidBridge.loadLogs === 'function') {
        window.AndroidBridge.loadLogs();
    }
}

function clearTerminalLog() {
    const logContainer = document.getElementById("terminal-log");
    if (logContainer) {
        logContainer.innerHTML = '<div class="log-line log-sys">[KRAVOX TOOL] Consola de terminal limpiada.</div>';
    }
    if (window.AndroidBridge && typeof window.AndroidBridge.clearLogs === 'function') {
        window.AndroidBridge.clearLogs();
    }
}

function appendLogLine(type, text, title = "") {
    const logContainer = document.getElementById("terminal-log");
    if (!logContainer) return;

    const line = document.createElement("div");
    let cls = "log-info";
    const upper = type.toUpperCase();
    if (upper === "BURST") cls = "log-burst";
    else if (upper === "CHAT") cls = "log-chat";
    else if (upper === "FLASH") cls = "log-flash";
    else if (upper === "WARN") cls = "log-warn";
    else if (upper === "SUCCESS") cls = "log-success";
    else if (upper === "ERROR") cls = "log-error";
    else if (upper === "SYS" || upper === "SYSTEM") cls = "log-sys";

    line.className = `log-line ${cls}`;
    line.textContent = `${title ? title + " " : ""}${text}`;
    logContainer.appendChild(line);

    logContainer.scrollTop = logContainer.scrollHeight;
}

function triggerMatrix() {
    appendLogLine("SYS", "00101101 01001011 01010010 01000001 01010110 01001111 01011000");
    appendLogLine("SUCCESS", "[MATRIX MODE] KRAVOX TOOL - discord.py 2.3.2 activo.");
}

// ANDROID SYSTEM BACK BUTTON HANDLER
window.onSystemBackPressed = function() {
    if (activeOption !== 0) {
        goBackToMenu();
        return true;
    }
    return false;
};

// GLOBAL KOTLIN CALLBACKS
window.onLogAdded = function(logObj) {
    if (logObj) {
        appendLogLine(logObj.type || "INFO", logObj.text || "", logObj.title || "");
    }
};

window.onStatusUpdated = function(infoObj) {
    if (infoObj) {
        const pingVal = document.getElementById("ping-val");
        if (pingVal && infoObj.pingMs !== undefined) {
            pingVal.textContent = infoObj.pingMs;
        }
    }
};

window.onTokenValidated = function(resObj) {
    if (resObj && resObj.valid) {
        appendLogLine("SUCCESS", `[TOKEN VALIDATED] Bot: @${resObj.botName} (ID: ${resObj.botId})`);
    }
};

window.onConfigsLoaded = function(configsArr) {
    configsArr.forEach(c => {
        if (c.key === "BOT_TOKEN") {
            savedToken = c.value;
        }
    });
    appendLogLine("INFO", `[CONFIG] Configuración cargada de Room DB.`);
};

window.onHistoryLoaded = function(logsArr) {
    appendLogLine("WARN", `=== HISTORIAL GUARDADO EN ROOM DB (${logsArr.length} REGISTROS) ===`);
    logsArr.forEach(item => {
        appendLogLine(item.type || "INFO", item.detail || "", item.title || "");
    });
};

window.showToast = function(message) {
    console.log("Toast: " + message);
};
