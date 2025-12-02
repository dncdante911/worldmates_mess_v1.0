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
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private val iceServers = listOf(
        // Google STUN (БЕСПЛАТНО)
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        // TURN сервер (ваш собственный - после установки coturn)
        // PeerConnection.IceServer.builder("turn:your-turn-server.com:3478")
        //     .setUsername("username")
        //     .setPassword("password")
        //     .createIceServer()
    )

    var onIceCandidateListener: ((IceCandidate) -> Unit)? = null
    var onAddStreamListener: ((MediaStream) -> Unit)? = null
    var onRemoveStreamListener: (() -> Unit)? = null
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
                        Log.d("WebRTCManager", "Remote stream added, tracks: ${stream.audioTracks.size + stream.videoTracks.size}")
                        onAddStreamListener?.invoke(stream)
                    }

                    override fun onRemoveStream(stream: MediaStream) {
                        Log.d("WebRTCManager", "Remote stream removed")
                        onRemoveStreamListener?.invoke()
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
                localAudioTrack?.let { mediaStream.addTrack(it) }
                Log.d("WebRTCManager", "Audio track added")
            }

            // Создать видео трек (если нужно)
            if (videoEnabled) {
                val videoSource = peerConnectionFactory.createVideoSource(false)
                localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource)
                localVideoTrack?.let { mediaStream.addTrack(it) }
                Log.d("WebRTCManager", "Video track added")
            }

            // Добавить в peer connection
            peerConnection?.addStream(mediaStream)

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
            MediaConstraints()
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
            peerConnection?.close()
            peerConnection = null
            localMediaStream?.let {
                it.audioTracks.forEach { track -> track.dispose() }
                it.videoTracks.forEach { track -> track.dispose() }
            }
            localMediaStream = null
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

    companion object {
        private const val TAG = "WebRTCManager"

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
