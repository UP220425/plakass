package PlateRecognitionApp.plakass.data.socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketHandler {

    private var socket: Socket? = null
    private var url: String? = null

    // Guardar user_id para reenviar join si se reconecta
    private var savedUserId: String? = null

    fun init(serverUrl: String) {
        url = serverUrl

        if (socket == null) {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                transports = arrayOf("websocket")  // Forzar WebSocket
            }

            socket = IO.socket(serverUrl, opts)

            setupBaseListeners()
        }
    }

    fun getSocket(): Socket {
        if (socket == null)
            throw IllegalStateException("SocketHandler.init(url) no fue llamado")
        return socket!!
    }

    fun connect() {
        if (socket?.connected() == false) {
            Log.d("SOCKET_HANDLER", "Conectando socket…")
            socket?.connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }

    // --------------------------------------------------------
    //          Guardar user_id para auto join
    // --------------------------------------------------------
    fun registerUser(userId: String) {
        savedUserId = userId
        sendJoin()
    }

    private fun sendJoin() {
        val socket = socket ?: return
        val uid = savedUserId ?: return

        if (!socket.connected()) return

        val json = JSONObject().put("user_id", uid)

        Log.d("SOCKET_HANDLER", "Enviando JOIN automático → $json")
        socket.emit("join", json)
    }

    // --------------------------------------------------------
    //           Listeners base del socket
    // --------------------------------------------------------
    private fun setupBaseListeners() {
        val socket = getSocket()

        socket.on(Socket.EVENT_CONNECT) {
            Log.d("SOCKET_HANDLER", "Socket conectado ✔")

            // Enviar join automático si había user_id guardado
            sendJoin()
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SOCKET_HANDLER", "ERROR de conexión → ${args.joinToString()}")
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Log.e("SOCKET_HANDLER", "Socket desconectado ❌")
        }
    }
}
