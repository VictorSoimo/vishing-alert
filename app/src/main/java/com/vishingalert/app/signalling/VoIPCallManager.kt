package com.example.vishingalert.voip

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription

/**
 * Manages P2P VoIP call with minimal setup
 * Captures audio from both local mic and remote participant
 */
class VoIPCallManager(
    private val context: Context,
    private val roomId: String,
    private val localUserId: String
) {

    companion object {
        private const val TAG = "VoIPCallManager"
        private const val AUDIO_ECHO_CANCELLATION = true
        private const val AUDIO_NOISE_SUPPRESSION = true
    }

    interface CallListener {
        fun onCallConnected()
        fun onCallDisconnected()
        fun onRemoteAudioStreamAdded(mediaStream: MediaStream)
        fun onError(error: String)
    }

    interface AudioFrameListener {
        fun onLocalAudioFrame(audioData: ByteArray)
        fun onRemoteAudioFrame(audioData: ByteArray)
    }

    private val signalingManager = WebRTCSignalingManager(roomId)
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var callListener: CallListener? = null
    private var audioFrameListener: AudioFrameListener? = null

    /**
     * Initialize WebRTC and create peer connection
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        try {
            // Initialize PeerConnectionFactory
            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            // Create audio device module
            val audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()

            // Build factory
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory()

            if (peerConnectionFactory == null) {
                Log.e(TAG, "Failed to create PeerConnectionFactory")
                return@withContext false
            }

            // Create peer connection
            val rtcConfig = PeerConnection.RTCConfiguration(listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            ))

            peerConnection = peerConnectionFactory!!.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}

                    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                        Log.d(TAG, "ICE connection state: $p0")
                        if (p0 == PeerConnection.IceConnectionState.CONNECTED) {
                            callListener?.onCallConnected()
                        } else if (p0 == PeerConnection.IceConnectionState.DISCONNECTED) {
                            callListener?.onCallDisconnected()
                        }
                    }

                    override fun onIceConnectionReceivingChange(p0: Boolean) {
                        TODO("Not yet implemented")
                    }

                    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
                        Log.d(TAG, "Selected candidate pair changed: $event")
                    }

                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}

                    override fun onIceCandidate(iceCandidate: IceCandidate?) {
                        iceCandidate?.let {
                            Log.d(TAG, "ICE candidate: $it")
                            signalingManager.sendIceCandidate(it, localUserId)
                        }
                    }

                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                    override fun onAddStream(mediaStream: MediaStream?) {
                        Log.d(TAG, "Remote stream added")
                        mediaStream?.let { callListener?.onRemoteAudioStreamAdded(it) }
                    }

                    override fun onRemoveStream(p0: MediaStream?) {}

                    override fun onDataChannel(p0: org.webrtc.DataChannel?) {}

                    override fun onRenegotiationNeeded() {
                        Log.d(TAG, "Renegotiation needed")
                    }

                    override fun onAddTrack(
                        p0: RtpReceiver?,
                        p1: Array<out MediaStream>?
                    ) {
                        Log.d(TAG, "Track added")
                    }
                }
            )

            if (peerConnection == null) {
                Log.e(TAG, "Failed to create PeerConnection")
                return@withContext false
            }

            // Add local audio track
            createAndAddLocalAudioTrack()

            // Set up signaling listener
            signalingManager.setSignalingListener(object : WebRTCSignalingManager.SignalingListener {
                override fun onOfferReceived(sdp: SessionDescription) {
                    handleOfferReceived(sdp)
                }

                override fun onAnswerReceived(sdp: SessionDescription) {
                    handleAnswerReceived(sdp)
                }

                override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
                    peerConnection?.addIceCandidate(iceCandidate)
                }
            })

            Log.d(TAG, "VoIPCallManager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            callListener?.onError("Initialization failed: ${e.message}")
            false
        }
    }

    /**
     * Create and add local audio track
     */
    private fun createAndAddLocalAudioTrack() {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION.toString(), AUDIO_ECHO_CANCELLATION.toString()))
            mandatory.add(MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION.toString(), AUDIO_NOISE_SUPPRESSION.toString()))
        }

        val audioSource: AudioSource? = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio0", audioSource)

        peerConnection?.addTrack(localAudioTrack!!, listOf("stream0"))
        Log.d(TAG, "Local audio track added")
    }

    /**
     * Start the call (caller side)
     */
    suspend fun startCall() = withContext(Dispatchers.Default) {
        try {
            // Listen for answer
            signalingManager.listenForAnswer(localUserId)
            signalingManager.listenForIceCandidates(localUserId)

            // Create and send offer
            val sdpConstraints = MediaConstraints()
            peerConnection?.createOffer(
                object : org.webrtc.SdpObserver {
                    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                        peerConnection?.setLocalDescription(
                            object : org.webrtc.SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    sessionDescription?.let { signalingManager.sendOffer(it, localUserId) }
                                    Log.d(TAG, "Offer sent")
                                }
                                override fun onCreateFailure(p0: String?) {
                                    Log.e(TAG, "Set local description failed: $p0")
                                }
                                override fun onSetFailure(p0: String?) {
                                    Log.e(TAG, "Set local description failed: $p0")
                                }
                            },
                            sessionDescription
                        )
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {
                        Log.e(TAG, "Create offer failed: $p0")
                    }
                    override fun onSetFailure(p0: String?) {}
                },
                sdpConstraints
            )

            Log.d(TAG, "Call started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call", e)
            callListener?.onError("Call start failed: ${e.message}")
        }
    }

    /**
     * Join an existing call (callee side)
     */
    suspend fun joinCall() = withContext(Dispatchers.Default) {
        try {
            // Listen for offer
            signalingManager.listenForOffer(localUserId)
            signalingManager.listenForIceCandidates(localUserId)

            Log.d(TAG, "Waiting for offer...")
        } catch (e: Exception) {
            Log.e(TAG, "Error joining call", e)
            callListener?.onError("Join failed: ${e.message}")
        }
    }

    /**
     * Handle incoming offer (answer with answer)
     */
    private fun handleOfferReceived(offer: SessionDescription) {
        peerConnection?.setRemoteDescription(
            object : org.webrtc.SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    // Create answer
                    peerConnection?.createAnswer(
                        object : org.webrtc.SdpObserver {
                            override fun onCreateSuccess(answer: SessionDescription?) {
                                peerConnection?.setLocalDescription(
                                    object : org.webrtc.SdpObserver {
                                        override fun onCreateSuccess(p0: SessionDescription?) {}
                                        override fun onSetSuccess() {
                                            answer?.let { signalingManager.sendAnswer(it, localUserId) }
                                            Log.d(TAG, "Answer sent")
                                        }
                                        override fun onCreateFailure(p0: String?) {}
                                        override fun onSetFailure(p0: String?) {}
                                    },
                                    answer
                                )
                            }
                            override fun onSetSuccess() {}
                            override fun onCreateFailure(p0: String?) {
                                Log.e(TAG, "Create answer failed: $p0")
                            }
                            override fun onSetFailure(p0: String?) {}
                        },
                        MediaConstraints()
                    )
                }
                override fun onCreateFailure(p0: String?) {
                    Log.e(TAG, "Set remote description failed: $p0")
                }
                override fun onSetFailure(p0: String?) {}
            },
            offer
        )
    }

    /**
     * Handle incoming answer
     */
    private fun handleAnswerReceived(answer: SessionDescription) {
        peerConnection?.setRemoteDescription(
            object : org.webrtc.SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetSuccess() {
                    Log.d(TAG, "Remote description set (answer)")
                }
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            },
            answer
        )
    }

    /**
     * End the call
     */
    fun endCall() {
        try {
            peerConnection?.close()
            peerConnection = null
            callListener?.onCallDisconnected()
            Log.d(TAG, "Call ended")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
        }
    }

    fun setCallListener(listener: CallListener) {
        this.callListener = listener
    }

    fun setAudioFrameListener(listener: AudioFrameListener) {
        this.audioFrameListener = listener
    }
}
