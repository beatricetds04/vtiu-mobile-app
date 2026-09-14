package com.example.vtiu.server.utils

import io.livekit.server.*

object LiveKitTokenBuilder {
    fun buildToken(
        apiKey: String,
        apiSecret: String,
        roomName: String,
        identity: String,
        name: String,
        isPublisher: Boolean = false
    ): String {
        val token = AccessToken(apiKey, apiSecret)
        token.identity = identity
        token.name = name
        
        token.addGrants(
            RoomJoin(true),
            RoomName(roomName),
            CanPublish(isPublisher),
            CanSubscribe(true),
            CanPublishData(true)
        )
        
        return token.toJwt()
    }
}
