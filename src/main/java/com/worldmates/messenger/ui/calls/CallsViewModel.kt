package com.worldmates.messenger.ui.calls

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.worldmates.messenger.data.model.*
import com.worldmates.messenger.network.RetrofitClient
import com.worldmates.messenger.network.SocketManager
import com.worldmates.messenger.network.WebRTCManager
import kotlinx.coroutines.*
import org.webrtc.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject

data class CallData(
    val callId: Int,
    val fromId: Int,
    val fromName: String,
    val fromAvatar: String,
    val toId: Int? = null,
    val groupId: Int? = null,
    val callType: String, // "audio" или "video"
    val roomName: String,
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null
)

data class IceCandidateData(
    val roomName: String,
    val candidate: String,
    val sdpMLineIndex: Int,
    val sdpMid: String
)

class CallsViewModel(application: Application) : AndroidViewModel(application), SocketManager.SocketListener {

    private val webRTCManager = WebRTCManager(application)
    private val socketManager = SocketManager(this)
    private val gson = Gson()

    // LiveData для UI
    val incomingCall = MutableLiveData<CallData>()
    val callConnected = MutableLiveData<Boolean>()
    val callEnded = MutableLiveData<Boolean>()
    val callError = MutableLiveData<String>()
    val remoteStreamAdded = MutableLiveData<MediaStream>()
    val connectionState = MutableLiveData<String>()

    private var currentCallData: CallData? = null
    private var currentCallId: Int = 0
    private var isInitiator = false

    init {
        socketManager.connect()
        setupWebRTCListeners()
    }

    private fun setupWebRTCListeners() {
        webRTCManager.onIceCandidateListener = { candidate ->
            currentCallData?.let {
                val iceCandidateData = IceCandidateData(
                    roomName = it.roomName,
                    candidate = candidate.sdp,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    sdpMid = candidate.sdpMid ?: ""
                )
                socketManager.emit("ice:candidate", gson.toJsonTree(iceCandidateData))
            }
        }

        webRTCManager.onAddStreamListener = { stream ->
            remoteStreamAdded.postValue(stream)
            Log.d("CallsViewModel", "Remote stream added with ${stream.audioTracks.size + stream.videoTracks.size} tracks")
        }

        webRTCManager.onConnectionStateChangeListener = { state ->
            connectionState.postValue(state.toString())
            when (state) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    callConnected.postValue(true)
                    Log.d("CallsViewModel", "Call connected!")
                }
                PeerConnection.PeerConnectionState.FAILED -> {
                    callError.postValue("Connection failed")
                    endCall()
                }
                PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    callEnded.postValue(true)
                }
                else -> {}
            }
        }

        webRTCManager.onIceConnectionStateChangeListener = { state ->
            Log.d("CallsViewModel", "ICE Connection State: $state")
        }
    }

    /**
     * Ініціювати вызов користувачу (1-на-1)
     */
    fun initiateCall(recipientId: Int, recipientName: String, recipientAvatar: String, callType: String = "audio") {
        viewModelScope.launch {
            try {
                // 1. Создать PeerConnection
                webRTCManager.createPeerConnection()

                // 2. Создать локальный медиа стрим
                val audioEnabled = true
                val videoEnabled = (callType == "video")
                webRTCManager.createLocalMediaStream(audioEnabled, videoEnabled)

                // 3. Создать offer
                webRTCManager.createOffer(
                    onSuccess = { offer ->
                        // 4. Отправить через Socket.IO
                        val roomName = generateRoomName()
                        currentCallData = CallData(
                            callId = 0,
                            fromId = getUserId(),
                            fromName = getUserName(),
                            fromAvatar = getUserAvatar(),
                            toId = recipientId,
                            callType = callType,
                            roomName = roomName,
                            sdpOffer = offer.description
                        )
                        isInitiator = true

                        val callEvent = JsonObject().apply {
                            addProperty("fromId", getUserId())
                            addProperty("toId", recipientId)
                            addProperty("callType", callType)
                            addProperty("roomName", roomName)
                            addProperty("fromName", getUserName())
                            add("sdpOffer", gson.toJsonTree(offer.description))
                        }

                        socketManager.emit("call:initiate", callEvent)
                        Log.d("CallsViewModel", "Call initiated to user $recipientId")
                    },
                    onError = { error ->
                        callError.postValue(error)
                        Log.e("CallsViewModel", "Failed to create offer: $error")
                    }
                )
            } catch (e: Exception) {
                callError.postValue(e.message ?: "Unknown error")
                Log.e("CallsViewModel", "Error initiating call", e)
            }
        }
    }

    /**
     * Ініціювати групповой вызов
     */
    fun initiateGroupCall(groupId: Int, groupName: String, callType: String = "audio") {
        viewModelScope.launch {
            try {
                webRTCManager.createPeerConnection()
                webRTCManager.createLocalMediaStream(audioEnabled = true, videoEnabled = (callType == "video"))

                webRTCManager.createOffer(
                    onSuccess = { offer ->
                        val roomName = generateRoomName()
                        currentCallData = CallData(
                            callId = 0,
                            fromId = getUserId(),
                            fromName = getUserName(),
                            fromAvatar = getUserAvatar(),
                            groupId = groupId,
                            callType = callType,
                            roomName = roomName,
                            sdpOffer = offer.description
                        )
                        isInitiator = true

                        val groupCallEvent = JsonObject().apply {
                            addProperty("groupId", groupId)
                            addProperty("initiatedBy", getUserId())
                            addProperty("callType", callType)
                            addProperty("roomName", roomName)
                            add("sdpOffer", gson.toJsonTree(offer.description))
                        }

                        socketManager.emit("group_call:initiate", groupCallEvent)
                    },
                    onError = { error ->
                        callError.postValue(error)
                    }
                )
            } catch (e: Exception) {
                callError.postValue(e.message)
            }
        }
    }

    /**
     * Прийняти вхідний вызов
     */
    fun acceptCall(callData: CallData) {
        viewModelScope.launch {
            try {
                currentCallData = callData
                isInitiator = false

                // 1. Создать PeerConnection
                webRTCManager.createPeerConnection()

                // 2. Создать локальный стрим
                val videoEnabled = (callData.callType == "video")
                webRTCManager.createLocalMediaStream(audioEnabled = true, videoEnabled = videoEnabled)

                // 3. Установить remote description (offer от другого юзера)
                callData.sdpOffer?.let { offerSdp ->
                    val remoteDescription = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                    webRTCManager.setRemoteDescription(remoteDescription) { error ->
                        callError.postValue(error)
                    }
                }

                // 4. Создать answer
                webRTCManager.createAnswer(
                    onSuccess = { answer ->
                        val acceptEvent = JsonObject().apply {
                            addProperty("roomName", callData.roomName)
                            addProperty("fromId", getUserId())
                            add("sdpAnswer", gson.toJsonTree(answer.description))
                        }
                        socketManager.emit("call:accept", acceptEvent)
                        Log.d("CallsViewModel", "Call accepted")
                    },
                    onError = { error ->
                        callError.postValue(error)
                    }
                )
            } catch (e: Exception) {
                callError.postValue(e.message)
            }
        }
    }

    /**
     * Отклонить вызов
     */
    fun rejectCall(roomName: String) {
        val rejectEvent = JsonObject().apply {
            addProperty("roomName", roomName)
        }
        socketManager.emit("call:reject", rejectEvent)
        incomingCall.postValue(null)
    }

    /**
     * Завершить вызов
     */
    fun endCall() {
        currentCallData?.let { callData ->
            val endEvent = JsonObject().apply {
                addProperty("roomName", callData.roomName)
                addProperty("reason", "user_ended")
            }
            socketManager.emit("call:end", endEvent)
        }

        webRTCManager.close()
        callEnded.postValue(true)
        currentCallData = null
    }

    /**
     * Socket.IO слушатели
     */
    // Required implementation from SocketListener
    override fun onNewMessage(messageJson: JSONObject) {
        // Not used for calls, but required by interface
    }

    override fun onSocketConnected() {
        Log.i("CallsViewModel", "Socket connected for calls")
    }

    override fun onSocketDisconnected() {
        Log.w("CallsViewModel", "Socket disconnected")
    }

    override fun onSocketError(error: String) {
        Log.e("CallsViewModel", "Socket error: $error")
        callError.postValue(error)
    }

    // Call-specific handlers (not part of SocketListener interface)
    fun onIncomingCall(data: JsonObject) {
        try {
            val callData = CallData(
                callId = data.get("callId").asInt,
                fromId = data.get("fromId").asInt,
                fromName = data.get("fromName").asString,
                fromAvatar = data.get("fromAvatar").asString,
                toId = getUserId(),
                callType = data.get("callType").asString,
                roomName = data.get("roomName").asString,
                sdpOffer = data.get("sdpOffer")?.asString
            )
            incomingCall.postValue(callData)
            Log.d("CallsViewModel", "Incoming call from ${callData.fromName}")
        } catch (e: Exception) {
            Log.e("CallsViewModel", "Error parsing incoming call", e)
        }
    }

    fun onCallAnswer(data: JsonObject) {
        try {
            val sdpAnswer = data.get("sdpAnswer").asString
            val remoteDescription = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
            webRTCManager.setRemoteDescription(remoteDescription) { error ->
                callError.postValue(error)
            }
            Log.d("CallsViewModel", "Received answer")
        } catch (e: Exception) {
            Log.e("CallsViewModel", "Error handling answer", e)
        }
    }

    fun onIceCandidate(data: JsonObject) {
        try {
            val candidate = data.get("candidate").asString
            val sdpMLineIndex = data.get("sdpMLineIndex").asInt
            val sdpMid = data.get("sdpMid").asString

            val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
            webRTCManager.addIceCandidate(iceCandidate)
        } catch (e: Exception) {
            Log.e("CallsViewModel", "Error adding ICE candidate", e)
        }
    }

    fun onCallEnded(data: JsonObject) {
        webRTCManager.close()
        callEnded.postValue(true)
        currentCallData = null
    }

    // Вспомогательные функции
    private fun getUserId(): Int {
        // Получить из UserSession
        return 1 // Заменить на реальный ID
    }

    private fun getUserName(): String {
        // Получить из UserSession
        return "Current User"
    }

    private fun getUserAvatar(): String {
        // Получить из UserSession
        return ""
    }

    private fun generateRoomName(): String {
        return "room_${System.currentTimeMillis()}"
    }

    fun toggleAudio(enabled: Boolean) {
        webRTCManager.setAudioEnabled(enabled)
    }

    fun toggleVideo(enabled: Boolean) {
        webRTCManager.setVideoEnabled(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager.close()
        socketManager.disconnect()
    }
}
