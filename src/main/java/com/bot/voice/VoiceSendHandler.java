package com.bot.voice;

import com.bot.utils.FormattingUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class VoiceSendHandler extends AudioEventAdapter implements AudioSendHandler {
    // Max Duration is in seconds
    public static long MAX_DURATION = 36009;

    private long requester;
    private String requesterName;
    private TextChannel lastUsedChannel;
    private QueuedAudioTrack nowPlaying;
    private Queue<QueuedAudioTrack> tracks;
    private AudioPlayer player;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;
    private boolean repeat;

    public VoiceSendHandler(AudioPlayer player) {
        this.player = player;
        this.tracks = new LinkedBlockingQueue<>();
        this.nowPlaying = null;
        this.repeat = false;

        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
    }

    public void queueTrack(AudioTrack track, long user, String requesterName, TextChannel channel) {
        if (player.getPlayingTrack() == null) {
            player.playTrack(track);
            requester = user;
            lastUsedChannel = channel;
            this.requesterName = requesterName;
            nowPlaying = new QueuedAudioTrack(track, requesterName, user);

            sendNowPlayingUpdate();
        }
        else {
            tracks.add(new QueuedAudioTrack(track, requesterName, user));
        }
        lastUsedChannel = channel;
    }

    public boolean skipTrack() {
        if (player.getPlayingTrack() == null) {
            return false;
        }
        else if (tracks.size() == 0) {
            return false;
        }
        else {
            QueuedAudioTrack nextTrack = tracks.poll();
            if (nextTrack == null) {
                stop();
            }
            else {
                requester = nextTrack.getRequesterID();
                player.playTrack(nextTrack.getTrack());
                nowPlaying = nextTrack;

                sendNowPlayingUpdate();
            }
            return true;
        }
    }

    public void stop() {
        tracks.clear();
        player.setPaused(false);
        player.stopTrack();
        player.destroy();
        repeat = false;
        nowPlaying = null;
    }

    public AudioPlayer getPlayer()  {
        return player;
    }

    @Override
    public boolean canProvide() {
        return player.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return (ByteBuffer) buffer.flip();
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.FINISHED && repeat) {
            queueTrack(track.makeClone(), requester, requesterName, lastUsedChannel);
            return;
        }
        else if (endReason != AudioTrackEndReason.FINISHED) {
            return;
        }

        QueuedAudioTrack nextTrack = tracks.poll();
        // If nextTrack is null then we are dont
        if (nextTrack == null) {
            stop();
        }
        else if (endReason.mayStartNext){
            requester = nextTrack.getRequesterID();
            requesterName = nextTrack.getRequesterName();
            player.playTrack(nextTrack.getTrack());
            nowPlaying = nextTrack;

            sendNowPlayingUpdate();
        }
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public static boolean isSongTooLong(AudioTrack track) {
        return track.getDuration() >= MAX_DURATION * 1000 && !track.getInfo().isStream;
    }

    public QueuedAudioTrack getNowPlaying() {
        return nowPlaying;
    }

    public boolean isPlaying() {
        return nowPlaying != null && !player.isPaused();
    }

    public Queue<QueuedAudioTrack> getTracks() {
        return tracks;
    }

    public void setTracks(Queue<QueuedAudioTrack> queue) {
        this.tracks = queue;
    }
  
    private void sendNowPlayingUpdate() {
        // Refresh the last channel to make sure we dont have a stale one
        lastUsedChannel = lastUsedChannel.getJDA().getTextChannelById(lastUsedChannel.getId());

        // If we sent the last message in the channel then just edit it
        lastUsedChannel.getHistory().retrievePast(1).queue(m -> {
            Message lastMessage = m.get(0);
            if (lastMessage.getAuthor().getId().equals(lastUsedChannel.getJDA().getSelfUser().getId())) {
                lastMessage.editMessage(FormattingUtils.getAudioTrackEmbed(nowPlaying, player.getVolume())).queue();
            } else {
                lastUsedChannel.sendMessage(FormattingUtils.getAudioTrackEmbed(nowPlaying, player.getVolume())).queue();
            }
        });
    }
}
