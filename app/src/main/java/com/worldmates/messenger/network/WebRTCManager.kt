package com.worldmates.messenger.network

import android.content.Context
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * WebRTCManager - управление WebRTC соединениями для аудио/видео вызовов
 * Поддерживает личные вызовы (1-на-1) и групповые вызовы
 */
class WebRTCManager(private val context: Context) {

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localMediaStream: MediaStream? = null
    private var remoteMediaStream: MediaStream? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null

    private var iceServers: List<PeerConnection.IceServer> = listOf(
        // Базовые STUN серверы Google (работают без аутентификации)
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    /**
     * Установить ICE servers (включая TURN с credentials от сервера)
     * Вызывается из CallsViewModel при получении credentials от сервера
     */
    fun setIceServers(servers: List<PeerConnection.IceServer>) {
        iceServers = servers
        Log.d("WebRTCManager", "ICE servers updated: ${servers.size} servers")
    }

    var onIceCandidateListener: ((IceCandidate) -> Unit)? = null
    var onTrackListener: ((MediaStream) -> Unit)? = null
    var onRemoveTrackListener: (() -> Unit)? = null
    var onConnectionStateChangeListener: ((PeerConnection.PeerConnectionState) -> Unit)? = null
    var onIceConnectionStateChangeListener: ((PeerConnection.IceConnectionState) -> Unit)? = null

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .setFieldTrials("")
                    .createInitializationOptions()
            )

            val audioDeviceModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(EglBaseProvider.context))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(EglBaseProvider.context, true, true))
                .setOptions(PeerConnectionFactory.Options().apply {
                    disableEncryption = false
                    networkIgnoreMask = 0
                })
                .createPeerConnectionFactory()

            Log.d("WebRTCManager", "PeerConnectionFactory initialized successfully")
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Failed to initialize PeerConnectionFactory", e)
        }
    }

    /**
     * Создать PeerConnection для вызова
     */
    fun createPeerConnection(): PeerConnection? {
        return try {
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }

            peerConnection = peerConnectionFactory.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onSignalingChange(newState: PeerConnection.SignalingState) {
                        Log.d("WebRTCManager", "SignalingState: $newState")
                    }

                    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                        Log.d("WebRTCManager", "IceConnectionState: $newState")
                        onIceConnectionStateChangeListener?.invoke(newState)
                    }

                    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                        Log.d("WebRTCManager", "PeerConnectionState: $newState")
                        onConnectionStateChangeListener?.invoke(newState)
                    }

                    override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState) {}

                    override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                        Log.d("WebRTCManager", "IceGatheringState: $newState")
                    }

                    override fun onIceCandidate(candidate: IceCandidate) {
                        Log.d("WebRTCManager", "IceCandidate: ${candidate.sdp}")
                        onIceCandidateListener?.invoke(candidate)
                    }

                    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}

                    override fun onAddStream(stream: MediaStream) {
                        // Deprecated in Unified Plan - use onTrack instead
                    }

                    override fun onRemoveStream(stream: MediaStream) {
                        // Deprecated in Unified Plan
                    }

                    override fun onTrack(transceiver: RtpTransceiver) {
                        val track = transceiver.receiver.track()
                        Log.d("WebRTCManager", "Remote track received: ${track?.kind()}")

                        // Создать remote stream если его еще нет
                        if (remoteMediaStream == null) {
                            remoteMediaStream = peerConnectionFactory.createLocalMediaStream("REMOTE_STREAM")
                        }

                        // Добавить track в remote stream
                        track?.let {
                            when (it) {
                                is AudioTrack -> remoteMediaStream?.addTrack(it)
                                is VideoTrack -> remoteMediaStream?.addTrack(it)
                            }
                            Log.d("WebRTCManager", "Track added to remote stream: ${it.kind()}")
                        }

                        // Уведомить listener
                        remoteMediaStream?.let { onTrackListener?.invoke(it) }
                    }

                    override fun onDataChannel(dataChannel: DataChannel) {}
                    override fun onRenegotiationNeeded() {
                        Log.d("WebRTCManager", "Renegotiation needed")
                    }
                }
            )

            peerConnection
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Failed to create PeerConnection", e)
            null
        }
    }

    /**
     * Создать локальный медиа поток (аудио + опционально видео)
     */
    fun createLocalMediaStream(
        audioEnabled: Boolean = true,
        videoEnabled: Boolean = false
    ): MediaStream? {
        return try {
            val mediaStream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM")

            // Создать аудио трек
            if (audioEnabled) {
                val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
                localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource)
                localAudioTrack?.let {
                    it.setEnabled(true)  // ✅ Явно включить аудио трек
                    mediaStream.addTrack(it)
                    // ✅ UNIFIED_PLAN: addTrack вместо addStream
                    peerConnection?.addTrack(it, listOf("LOCAL_STREAM"))
                }
                Log.d("WebRTCManager", "Audio track added and enabled")
            }

            // Создать видео трек (если нужно)
            if (videoEnabled) {
                // Создать CameraVideoCapturer для доступа к камере
                videoCapturer = createCameraVideoCapturer()
                videoSource = peerConnectionFactory.createVideoSource(videoCapturer?.isScreencast ?: false)

                // Запустить камеру с разрешением 1280x720 при 30 fps
                videoCapturer?.initialize(
                    SurfaceTextureHelper.create("CaptureThread", EglBaseProvider.context),
                    context,
                    videoSource?.capturerObserver
                )
                videoCapturer?.startCapture(1280, 720, 30)

                localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
                localVideoTrack?.let {
                    it.setEnabled(true)  // ✅ Явно включить видео трек
                    mediaStream.addTrack(it)
                    // ✅ UNIFIED_PLAN: addTrack вместо addStream
                    peerConnection?.addTrack(it, listOf("LOCAL_STREAM"))
                }
                Log.d("WebRTCManager", "Video track added with camera capturer and enabled")
            }

            localMediaStream = mediaStream
            mediaStream
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Failed to create local media stream", e)
            null
        }
    }

    /**
     * Создать offer для инициатора вызова
     */
    fun createOffer(onSuccess: (SessionDescription) -> Unit, onError: (String) -> Unit) {
        peerConnection?.createOffer(
            object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    peerConnection?.setLocalDescription(
                        object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                onSuccess(sessionDescription)
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(error: String?) {
                                onError(error ?: "Failed to set local description")
                            }
                        },
                        sessionDescription
                    )
                }

                override fun onCreateFailure(error: String?) {
                    onError(error ?: "Failed to create offer")
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(p0: String?) {}
            },
            MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
        )
    }

    /**
     * Создать answer для получателя вызова
     */
    fun createAnswer(onSuccess: (SessionDescription) -> Unit, onError: (String) -> Unit) {
        peerConnection?.createAnswer(
            object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    peerConnection?.setLocalDescription(
                        object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                onSuccess(sessionDescription)
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(error: String?) {
                                onError(error ?: "Failed to set local description")
                            }
                        },
                        sessionDescription
                    )
                }

                override fun onCreateFailure(error: String?) {
                    onError(error ?: "Failed to create answer")
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(p0: String?) {}
            },
            MediaConstraints().apply {
                // ✅ КРИТИЧНО: Указать что хотим получать audio/video!
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
        )
    }

    /**
     * Установить remote description (offer или answer от другого юзера)
     */
    fun setRemoteDescription(sessionDescription: SessionDescription, onError: (String) -> Unit) {
        peerConnection?.setRemoteDescription(
            object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    Log.d("WebRTCManager", "Remote description set successfully")
                }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(error: String?) {
                    onError(error ?: "Failed to set remote description")
                }
            },
            sessionDescription
        )
    }

    /**
     * Добавить ICE candidate
     */
    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    /**
     * Закрыть соединение
     */
    fun close() {
        try {
            // Остановить камеру
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            // Очистить видео источник
            videoSource?.dispose()
            videoSource = null

            peerConnection?.close()
            peerConnection = null

            // Очистить локальный stream
            localMediaStream?.let {
                it.audioTracks.forEach { track -> track.dispose() }
                it.videoTracks.forEach { track -> track.dispose() }
            }
            localMediaStream = null

            // Очистить remote stream
            remoteMediaStream?.let {
                it.audioTracks.forEach { track -> track.dispose() }
                it.videoTracks.forEach { track -> track.dispose() }
            }
            remoteMediaStream = null

            Log.d("WebRTCManager", "PeerConnection closed")
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Error closing PeerConnection", e)
        }
    }

    /**
     * Отключить/включить микрофон
     */
    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d("WebRTCManager", "Audio enabled: $enabled")
    }

    /**
     * Отключить/включить видео
     */
    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Log.d("WebRTCManager", "Video enabled: $enabled")
    }

    /**
     * Получить локальный поток
     */
    fun getLocalMediaStream(): MediaStream? = localMediaStream

    /**
     * 📷 Создать CameraVideoCapturer (по умолчанию фронтальная камера)
     */
    private fun createCameraVideoCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Попробовать фронтальную камеру сначала
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Using front camera: $deviceName")
                    return capturer
                }
            }
        }

        // Если нет фронтальной, попробовать заднюю
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Using back camera: $deviceName")
                    return capturer
                }
            }
        }

        Log.e(TAG, "No camera found")
        return null
    }

    /**
     * 🔄 Переключить камеру (фронтальная ↔ задняя)
     */
    fun switchCamera() {
        videoCapturer?.let { capturer ->
            if (capturer is CameraVideoCapturer) {
                capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(isFrontFacing: Boolean) {
                        Log.d(TAG, "Camera switched to ${if (isFrontFacing) "front" else "back"}")
                    }

                    override fun onCameraSwitchError(errorDescription: String?) {
                        Log.e(TAG, "Camera switch error: $errorDescription")
                    }
                })
            }
        } ?: run {
            Log.w(TAG, "Cannot switch camera - videoCapturer is null")
        }
    }

    companion object {
        private const val TAG = "WebRTCManager"

        /**
         * Получить EGL контекст для инициализации SurfaceViewRenderer
         * Публичная функция для доступа из других классов
         */
        fun getEglContext(): EglBase.Context {
            return EglBaseProvider.context
        }

        // Помощник для инициализации EGL контекста
        object EglBaseProvider {
            private var eglBase: EglBase? = null

            val context: EglBase.Context
                get() {
                    if (eglBase == null) {
                        eglBase = EglBase.create()
                    }
                    return eglBase!!.eglBaseContext
                }

            fun release() {
                eglBase?.release()
                eglBase = null
            }
        }
    }
}
