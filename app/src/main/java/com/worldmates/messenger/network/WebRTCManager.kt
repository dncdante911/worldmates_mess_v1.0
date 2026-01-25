package com.worldmates.messenger.network

import android.content.Context
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * 📹 Качество видео для разных условий сети
 */
enum class VideoQuality(val width: Int, val height: Int, val fps: Int, val label: String) {
    LOW(320, 240, 15, "Низкое (240p)"),           // Для очень медленного интернета
    MEDIUM(640, 480, 24, "Среднее (480p)"),       // Для мобильного интернета
    HIGH(1280, 720, 30, "Высокое (720p)"),        // Стандартное качество
    FULL_HD(1920, 1080, 30, "Full HD (1080p)")    // Для быстрого WiFi
}

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
    private var remoteVideoTrack: VideoTrack? = null  // ✅ Окремий трек для remote video
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null  // ✅ Зберігаємо для правильного cleanup

    // 📹 Текущее качество видео (по умолчанию HIGH - 720p)
    private var currentVideoQuality: VideoQuality = VideoQuality.HIGH

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
                        Log.d("WebRTCManager", "📡 Remote track received: ${track?.kind()}, enabled: ${track?.enabled()}")

                        // Создать remote stream если его еще нет
                        if (remoteMediaStream == null) {
                            remoteMediaStream = peerConnectionFactory.createLocalMediaStream("REMOTE_STREAM")
                        }

                        // Добавить track в remote stream
                        track?.let {
                            when (it) {
                                is AudioTrack -> {
                                    it.setEnabled(true)
                                    remoteMediaStream?.addTrack(it)
                                    Log.d("WebRTCManager", "📡 Remote AUDIO track added")
                                }
                                is VideoTrack -> {
                                    it.setEnabled(true)
                                    remoteVideoTrack = it  // ✅ Зберігаємо для відстеження
                                    remoteMediaStream?.addTrack(it)
                                    Log.d("WebRTCManager", "📡 Remote VIDEO track added - notifying listener")
                                    // ✅ КРИТИЧНО: Сповістити listener ТІЛЬКИ коли є відео трек
                                    remoteMediaStream?.let { stream -> onTrackListener?.invoke(stream) }
                                }
                            }
                        }
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

                // ✅ Зберігаємо SurfaceTextureHelper для правильного cleanup
                surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", EglBaseProvider.context)

                // Запустити камеру
                videoCapturer?.initialize(
                    surfaceTextureHelper,
                    context,
                    videoSource?.capturerObserver
                )
                videoCapturer?.startCapture(currentVideoQuality.width, currentVideoQuality.height, currentVideoQuality.fps)

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
            // ✅ Остановить камеру
            try {
                videoCapturer?.stopCapture()
            } catch (e: Exception) {
                Log.w("WebRTCManager", "Error stopping capture: ${e.message}")
            }
            videoCapturer?.dispose()
            videoCapturer = null

            // ✅ Очистить SurfaceTextureHelper
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            // Очистить видео источник
            videoSource?.dispose()
            videoSource = null

            // ✅ Очистить remote video track
            remoteVideoTrack = null

            peerConnection?.close()
            peerConnection = null

            // Очистить локальный stream
            localMediaStream?.let {
                it.audioTracks.forEach { track -> track.dispose() }
                it.videoTracks.forEach { track -> track.dispose() }
            }
            localMediaStream = null
            localVideoTrack = null
            localAudioTrack = null

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
     * Если видео трек не существует, просто выключаем (не создаем новый)
     */
    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Log.d("WebRTCManager", "Video enabled: $enabled (track exists: ${localVideoTrack != null})")
    }

    /**
     * 📹 Включить видео динамически (создать камеру и видеотрек если их нет)
     * Возвращает true если видео успешно включено
     */
    fun enableVideo(): Boolean {
        // Если видеотрек уже есть - просто включаем его
        if (localVideoTrack != null) {
            localVideoTrack?.setEnabled(true)
            Log.d("WebRTCManager", "Video track already exists, enabling it")
            return true
        }

        // Создаем видео если PeerConnection существует
        if (peerConnection == null) {
            Log.e("WebRTCManager", "Cannot enable video: PeerConnection is null")
            return false
        }

        return try {
            // ✅ Якщо камера вже існує але зупинена - просто перезапустити
            if (videoCapturer != null && videoSource != null && localVideoTrack != null) {
                Log.d("WebRTCManager", "Restarting existing camera...")
                videoCapturer?.startCapture(currentVideoQuality.width, currentVideoQuality.height, currentVideoQuality.fps)
                localVideoTrack?.setEnabled(true)
                Log.d("WebRTCManager", "✅ Camera restarted")
                return true
            }

            // 1. Создать CameraVideoCapturer
            videoCapturer = createCameraVideoCapturer()
            if (videoCapturer == null) {
                Log.e("WebRTCManager", "Failed to create camera capturer")
                return false
            }

            // 2. Создать VideoSource
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer?.isScreencast ?: false)

            // 3. ✅ Зберігаємо SurfaceTextureHelper
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", EglBaseProvider.context)

            // 4. Инициализировать и запустить камеру
            videoCapturer?.initialize(
                surfaceTextureHelper,
                context,
                videoSource?.capturerObserver
            )
            videoCapturer?.startCapture(currentVideoQuality.width, currentVideoQuality.height, currentVideoQuality.fps)

            // 5. Создать видеотрек
            localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
            localVideoTrack?.setEnabled(true)

            // 6. Добавить видеотрек в localMediaStream
            localMediaStream?.addTrack(localVideoTrack!!)

            // 7. Добавить видеотрек в PeerConnection (UNIFIED_PLAN)
            peerConnection?.addTrack(localVideoTrack!!, listOf("LOCAL_STREAM"))

            Log.d("WebRTCManager", "✅ Video enabled dynamically - camera started")
            true
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Failed to enable video dynamically", e)
            false
        }
    }

    /**
     * 📹 Выключить видео (остановить камеру, НЕ удалять ресурсы для возможности перезапуска)
     */
    fun disableVideo() {
        try {
            // Выключить трек (но не удалять)
            localVideoTrack?.setEnabled(false)

            // ✅ Остановить камеру для экономии батареи
            // НЕ вызываем dispose() чтобы можно было перезапустить
            try {
                videoCapturer?.stopCapture()
            } catch (e: InterruptedException) {
                Log.w("WebRTCManager", "Interrupted while stopping capture: ${e.message}")
            }

            Log.d("WebRTCManager", "📹 Video disabled, camera paused (can be restarted)")
        } catch (e: Exception) {
            Log.e("WebRTCManager", "Error disabling video", e)
        }
    }

    /**
     * 📹 Проверить есть ли видеотрек
     */
    fun hasVideoTrack(): Boolean = localVideoTrack != null

    /**
     * 📹 Получить текущее качество видео
     */
    fun getVideoQuality(): VideoQuality = currentVideoQuality

    /**
     * 📹 Изменить качество видео на лету
     * Перезапускает камеру с новым разрешением
     */
    fun setVideoQuality(quality: VideoQuality): Boolean {
        if (currentVideoQuality == quality) {
            Log.d(TAG, "Video quality already set to ${quality.label}")
            return true
        }

        currentVideoQuality = quality
        Log.d(TAG, "📹 Changing video quality to ${quality.label} (${quality.width}x${quality.height}@${quality.fps}fps)")

        // Если камера уже запущена - перезапустить с новым качеством
        if (videoCapturer != null) {
            return try {
                videoCapturer?.stopCapture()
                videoCapturer?.startCapture(quality.width, quality.height, quality.fps)
                Log.d(TAG, "✅ Video quality changed to ${quality.label}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to change video quality", e)
                false
            }
        }

        return true
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
