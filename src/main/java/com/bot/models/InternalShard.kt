package com.bot.models

import com.bot.voice.VoiceSendHandler
import net.dv8tion.jda.api.JDA

class InternalShard(val jda: JDA) {
    val id: Int = jda.shardInfo.shardId
    var activeVoiceConnectionsCount = 0
        private set
    var idleVoiceConnectionCount = 0
        private set
    var queuedTracksCount = 0
        private set
    var usersInVoiceCount = 0
        private set

    val serverCount: Int
        get() = jda.guilds.size

    val userCount: Int
        get() = jda.users.size

    fun updateStatistics() {
        activeVoiceConnectionsCount = 0
        idleVoiceConnectionCount = 0
        usersInVoiceCount = 0
        queuedTracksCount = 0

        try {
            for (manager in jda.audioManagers) {
                val handler = manager.sendingHandler as? VoiceSendHandler ?: continue

                // Update active connections
                if (manager.isConnected && handler.isPlaying) {
                    activeVoiceConnectionsCount++
                    usersInVoiceCount += manager.connectedChannel!!.members.size - 1
                }

                // Update idle connection count
                if (manager.isConnected && !handler.isPlaying) {
                    idleVoiceConnectionCount++
                    usersInVoiceCount += manager.connectedChannel!!.members.size - 1
                }

                if (handler != null) {
                    if (handler.nowPlaying != null)
                        queuedTracksCount++

                    queuedTracksCount += handler.tracks.size
                }
            }
        } catch (e : NullPointerException) {

        }
    }
}
