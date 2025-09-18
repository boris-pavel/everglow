package com.boris.everglow.game

import kotlin.math.max
import kotlin.random.Random

data class Obstacle(
    val id: Int,
    val lane: Int,
    val top: Float
)

data class GameState(
    val playerLane: Int = laneCount / 2,
    val obstacles: List<Obstacle> = emptyList(),
    val elapsed: Float = 0f,
    val score: Int = 0,
    val highScore: Int = 0,
    val level: Int = 1,
    val spawnTimer: Float = spawnStartInterval * 0.6f,
    val isRunning: Boolean = true,
    val session: Int = 0,
    val nextObstacleId: Int = 0
) {
    fun move(deltaLane: Int): GameState {
        if (!isRunning || deltaLane == 0) return this
        val newLane = (playerLane + deltaLane).coerceIn(0, laneCount - 1)
        if (newLane == playerLane) return this
        return copy(playerLane = newLane)
    }

    fun restart(): GameState = GameState(session = session + 1, highScore = highScore)

    fun advance(deltaSeconds: Float, random: Random): GameState {
        if (!isRunning) return this

        val timeStep = deltaSeconds.coerceIn(0f, 0.16f)
        val updatedElapsed = elapsed + timeStep
        val speed = 0.55f + (level - 1) * 0.09f

        var newScore = score
        val progressed = obstacles.map { it.copy(top = it.top + speed * timeStep) }
        val survivors = mutableListOf<Obstacle>()
        for (obstacle in progressed) {
            if (obstacle.top >= 1f) {
                newScore += 1
            } else {
                survivors += obstacle
            }
        }

        val newHighScore = max(highScore, newScore)
        val newLevel = (newScore / 8) + 1
        val interval = (spawnStartInterval - (newLevel - 1) * 0.05f).coerceAtLeast(0.32f)
        var timer = spawnTimer - timeStep
        var nextId = nextObstacleId
        while (timer <= 0f) {
            survivors += Obstacle(
                id = nextId++,
                lane = random.nextInt(laneCount),
                top = -obstacleHeightFraction
            )
            timer += interval
        }

        val playerTop = playerCenterY - playerRadiusFraction
        val playerBottom = playerCenterY + playerRadiusFraction
        val collision = survivors.any { obstacle ->
            obstacle.lane == playerLane &&
                obstacle.top <= playerBottom &&
                obstacle.top + obstacleHeightFraction >= playerTop
        }

        return if (collision) {
            copy(
                obstacles = survivors,
                elapsed = updatedElapsed,
                score = newScore,
                highScore = newHighScore,
                level = newLevel,
                spawnTimer = timer,
                isRunning = false,
                nextObstacleId = nextId
            )
        } else {
            copy(
                obstacles = survivors,
                elapsed = updatedElapsed,
                score = newScore,
                highScore = newHighScore,
                level = newLevel,
                spawnTimer = timer,
                nextObstacleId = nextId
            )
        }
    }

    companion object {
        const val laneCount: Int = 3
        const val obstacleHeightFraction: Float = 0.18f
        const val playerRadiusFraction: Float = 0.08f
        const val playerCenterY: Float = 0.82f
        private const val spawnStartInterval: Float = 0.65f
    }
}
