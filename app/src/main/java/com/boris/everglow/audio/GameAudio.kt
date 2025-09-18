package com.boris.everglow.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class GameAudio(context: Context) {
    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(4)
        .build()

    private val loadedSampleIds = mutableSetOf<Int>()
    private val soundIds: Map<SoundEffect, Int?> = SoundEffect.values().associateWith { effect ->
        loadEffect(effect.resourceName)
    }

    private val backgroundPlayer: MediaPlayer? = createBackgroundPlayer(BackgroundTrack.GameLoop.resourceName)

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSampleIds += sampleId
            }
        }
    }

    fun playLaneShift() {
        playEffect(SoundEffect.LaneShift)
    }

    fun playCollision() {
        playEffect(SoundEffect.Collision)
    }

    fun playHighScore() {
        playEffect(SoundEffect.HighScore)
    }

    fun playUiConfirm() {
        playEffect(SoundEffect.UiConfirm)
    }

    fun playUiCancel() {
        playEffect(SoundEffect.UiCancel)
    }

    fun ensureMusicPlaying() {
        backgroundPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun pauseMusic(resetPosition: Boolean = false) {
        backgroundPlayer?.takeIf { it.isPlaying }?.pause()
        if (resetPosition) {
            backgroundPlayer?.seekTo(0)
        }
    }

    fun stopMusic() {
        pauseMusic(resetPosition = true)
    }

    fun release() {
        backgroundPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        soundPool.release()
        loadedSampleIds.clear()
    }

    private fun playEffect(effect: SoundEffect) {
        val sampleId = soundIds[effect] ?: return
        if (!loadedSampleIds.contains(sampleId)) return
        val volume = effect.volume
        soundPool.play(sampleId, volume, volume, effect.priority, 0, effect.rate)
    }

    private fun loadEffect(resourceName: String): Int? {
        val resId = appContext.resources.getIdentifier(resourceName, RESOURCE_TYPE, appContext.packageName)
        if (resId == 0) return null
        return soundPool.load(appContext, resId, DEFAULT_PRIORITY)
    }

    private fun createBackgroundPlayer(resourceName: String): MediaPlayer? {
        val resId = appContext.resources.getIdentifier(resourceName, RESOURCE_TYPE, appContext.packageName)
        if (resId == 0) return null
        return MediaPlayer.create(appContext, resId)?.apply {
            isLooping = true
            setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
        }
    }

    private enum class SoundEffect(
        val resourceName: String,
        val volume: Float,
        val priority: Int = DEFAULT_PRIORITY,
        val rate: Float = 1f
    ) {
        LaneShift(resourceName = "sfx_lane_shift", volume = 0.55f),
        Collision(resourceName = "sfx_collision", volume = 0.9f),
        HighScore(resourceName = "sfx_high_score", volume = 0.8f),
        UiConfirm(resourceName = "sfx_ui_confirm", volume = 0.7f),
        UiCancel(resourceName = "sfx_ui_back", volume = 0.6f)
    }

    private enum class BackgroundTrack(val resourceName: String) {
        GameLoop(resourceName = "game_music_loop")
    }

    private companion object {
        const val MUSIC_VOLUME = 0.38f
        const val DEFAULT_PRIORITY = 1
        const val RESOURCE_TYPE = "raw"
    }
}
