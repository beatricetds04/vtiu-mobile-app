package com.example.vtiu.data.remote

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LiveKitManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var room: Room? = null

    // Track remote video tracks
    private val _videoTracks = mutableStateListOf<VideoTrack>()
    val videoTracks: List<VideoTrack> = _videoTracks

    fun joinRoom(url: String, token: String) {
        scope.launch {
            try {
                if (room == null) {
                    room = LiveKit.create(context)
                }
                
                room?.connect(url, token)
                
                // Collect track events
                launch {
                    room?.events?.collect { event ->
                        when (event) {
                            is RoomEvent.TrackSubscribed -> {
                                val track = event.track
                                if (track is VideoTrack) {
                                    Log.d("LiveKitManager", "Track subscribed: ${track.sid}")
                                    if (!_videoTracks.contains(track)) {
                                        _videoTracks.add(track)
                                    }
                                }
                            }
                            is RoomEvent.TrackUnsubscribed -> {
                                val track = event.track
                                if (track is VideoTrack) {
                                    Log.d("LiveKitManager", "Track unsubscribed: ${track.sid}")
                                    _videoTracks.remove(track)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LiveKitManager", "Failed to connect: ${e.message}")
            }
        }
    }

    fun leaveRoom() {
        scope.launch {
            room?.disconnect()
            room = null
            _videoTracks.clear()
        }
    }

    suspend fun publishVideo() {
        room?.localParticipant?.setCameraEnabled(true)
    }

    suspend fun publishAudio() {
        room?.localParticipant?.setMicrophoneEnabled(true)
    }

    fun release() {
        leaveRoom()
    }
}
