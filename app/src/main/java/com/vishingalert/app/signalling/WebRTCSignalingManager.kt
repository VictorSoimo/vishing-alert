package com.example.vishingalert.voip

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Manages WebRTC signaling via Firebase Realtime Database
 * Simplest approach: no external signaling server needed
 */
class WebRTCSignalingManager(private val roomId: String) {

    companion object {
        private const val TAG = "WebRTCSignaling"
    }

    interface SignalingListener {
        fun onOfferReceived(sdp: SessionDescription)
        fun onAnswerReceived(sdp: SessionDescription)
        fun onIceCandidateReceived(iceCandidate: IceCandidate)
    }

    private val database = FirebaseDatabase.getInstance().reference
    private var listener: SignalingListener? = null
    private val gson = Gson()

    /**
     * Send offer (caller side)
     */
    fun sendOffer(offer: SessionDescription, localUserId: String) {
        val offerData = mapOf(
            "type" to "offer",
            "sdp" to offer.description
        )
        database.child("rooms").child(roomId).child("offer")
            .setValue(offerData)
            .addOnFailureListener { Log.e(TAG, "Failed to send offer", it) }
    }

    /**
     * Send answer (callee side)
     */
    fun sendAnswer(answer: SessionDescription, localUserId: String) {
        val answerData = mapOf(
            "type" to "answer",
            "sdp" to answer.description
        )
        database.child("rooms").child(roomId).child("answer")
            .setValue(answerData)
            .addOnFailureListener { Log.e(TAG, "Failed to send answer", it) }
    }

    /**
     * Send ICE candidate
     */
    fun sendIceCandidate(iceCandidate: IceCandidate, localUserId: String) {
        val candidateData = mapOf(
            "candidate" to iceCandidate.candidate,
            "sdpMLineIndex" to iceCandidate.sdpMLineIndex,
            "sdpMid" to iceCandidate.sdpMid
        )
        database.child("rooms").child(roomId).child("candidates")
            .child(System.currentTimeMillis().toString())
            .setValue(candidateData)
    }

    /**
     * Listen for offer
     */
    fun listenForOffer(localUserId: String) {
        database.child("rooms").child(roomId).child("offer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdpString = snapshot.child("sdp").value as? String ?: return
                    val offer = SessionDescription(
                        SessionDescription.Type.OFFER,
                        sdpString
                    )
                    listener?.onOfferReceived(offer)
                    Log.d(TAG, "Offer received")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error listening for offer", error.toException())
                }
            })
    }

    /**
     * Listen for answer
     */
    fun listenForAnswer(localUserId: String) {
        database.child("rooms").child(roomId).child("answer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdpString = snapshot.child("sdp").value as? String ?: return
                    val answer = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        sdpString
                    )
                    listener?.onAnswerReceived(answer)
                    Log.d(TAG, "Answer received")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error listening for answer", error.toException())
                }
            })
    }

    /**
     * Listen for ICE candidates
     */
    fun listenForIceCandidates(localUserId: String) {
        database.child("rooms").child(roomId).child("candidates")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (candidateSnapshot in snapshot.children) {
                        val candidate = candidateSnapshot.child("candidate").value as? String ?: continue
                        val sdpMLineIndex = (candidateSnapshot.child("sdpMLineIndex").value as? Long)?.toInt() ?: 0
                        val sdpMid = candidateSnapshot.child("sdpMid").value as? String

                        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                        listener?.onIceCandidateReceived(iceCandidate)
                    }
                    Log.d(TAG, "ICE candidates received")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error listening for candidates", error.toException())
                }
            })
    }

    fun setSignalingListener(listener: SignalingListener) {
        this.listener = listener
    }
}
