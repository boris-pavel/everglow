package com.boris.everglow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.boris.everglow.audio.GameAudio
import com.boris.everglow.game.GameState
import com.boris.everglow.ui.theme.EverglowTheme
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            EverglowTheme {
                GameRoot()
            }
        }
    }
}

@Composable
private fun GameRoot() {
    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences(HIGH_SCORE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    var gameState by remember {
        mutableStateOf(GameState(highScore = sharedPrefs.getInt(HIGH_SCORE_PREF_KEY, 0)))
    }
    var showMenu by remember { mutableStateOf(true) }
    var latestHighScoreSession by rememberSaveable { mutableStateOf(-1) }
    var isHighlightingHighScore by remember { mutableStateOf(false) }
    var previousHighScore by remember { mutableStateOf(gameState.highScore) }
    val random = remember { Random(System.currentTimeMillis()) }
    val audio = remember { GameAudio(context) }
    DisposableEffect(audio) {
        onDispose { audio.release() }
    }

    LaunchedEffect(gameState.session, showMenu) {
        if (showMenu) return@LaunchedEffect
        var lastFrame = withFrameMillis { it }
        while (isActive) {
            if (showMenu) break
            if (!gameState.isRunning) break
            val frameTime = withFrameMillis { it }
            val deltaSeconds = (frameTime - lastFrame) / 1000f
            lastFrame = frameTime
            val nextState = gameState.advance(deltaSeconds, random)
            if (gameState.isRunning && !nextState.isRunning) {
                audio.playCollision()
            }
            gameState = nextState
        }
    }

    LaunchedEffect(showMenu, gameState.isRunning) {
        when {
            showMenu -> audio.stopMusic()
            gameState.isRunning -> audio.ensureMusicPlaying()
            else -> audio.pauseMusic()
        }
    }

    LaunchedEffect(gameState.highScore) {
        if (gameState.highScore > previousHighScore) {
            latestHighScoreSession = gameState.session
            sharedPrefs.edit().putInt(HIGH_SCORE_PREF_KEY, gameState.highScore).apply()
            audio.playHighScore()
        }
        previousHighScore = gameState.highScore
    }

    LaunchedEffect(latestHighScoreSession, gameState.session, showMenu) {
        if (showMenu) {
            isHighlightingHighScore = false
        } else if (latestHighScoreSession == gameState.session && gameState.score > 0) {
            isHighlightingHighScore = true
            delay(1400)
            isHighlightingHighScore = false
        }
    }

    if (showMenu) {
        MainMenu(
            highScore = gameState.highScore,
            onStart = {
                audio.playUiConfirm()
                gameState = GameState(highScore = gameState.highScore)
                showMenu = false
            }
        )
    } else {
        val gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Scoreboard(
                        score = gameState.score,
                        highScore = gameState.highScore,
                        highlightHighScore = isHighlightingHighScore,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        audio.playUiCancel()
                        showMenu = true
                    }) {
                        Text(text = stringResource(id = R.string.open_menu))
                    }
                }

                GameCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = gameState
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                    ControlRow(
                        onMoveLeft = {
                            val updated = gameState.move(-1)
                            if (updated.playerLane != gameState.playerLane) {
                                audio.playLaneShift()
                            }
                            gameState = updated
                        },
                        onMoveRight = {
                            val updated = gameState.move(1)
                            if (updated.playerLane != gameState.playerLane) {
                                audio.playLaneShift()
                            }
                            gameState = updated
                        }
                    )
                }
            }

            if (!gameState.isRunning) {
                GameOverOverlay(
                    score = gameState.score,
                    highScore = gameState.highScore,
                    isNewHighScore = latestHighScoreSession == gameState.session,
                    onRestart = {
                        audio.playUiConfirm()
                        gameState = gameState.restart()
                    },
                    onBackToMenu = {
                        audio.playUiCancel()
                        showMenu = true
                    }
                )
            }
        }
    }
}

@Composable
private fun MainMenu(highScore: Int, onStart: () -> Unit) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 36.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                if (highScore > 0) {
                    Text(
                        text = stringResource(id = R.string.high_score_header, highScore),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                Button(onClick = onStart) {
                    Text(text = stringResource(id = R.string.start_run))
                }
            }
        }
    }
}

@Composable
private fun Scoreboard(score: Int, highScore: Int, highlightHighScore: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScoreCard(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.score_label),
            value = score.toString()
        )
        ScoreCard(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.high_score_label),
            value = highScore.toString(),
            highlight = highlightHighScore
        )
    }
}

@Composable
private fun ScoreCard(modifier: Modifier = Modifier, title: String, value: String, highlight: Boolean = false) {
    val shape = RoundedCornerShape(24.dp)
    val baseColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
    val highlightedColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
    val containerColor by animateColorAsState(
        targetValue = if (highlight) highlightedColor else baseColor,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 180f),
        label = "scoreCardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else baseColor.copy(alpha = 0.55f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f),
        label = "scoreCardBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (highlight) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 220f),
        label = "scoreCardScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (highlight) 0.28f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 240f),
        label = "scoreCardGlow"
    )
    val glowColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha),
                        cornerRadius = CornerRadius(size.minDimension * 0.32f, size.minDimension * 0.32f)
                    )
                }
            },
        shape = shape,
        tonalElevation = if (highlight) 20.dp else 12.dp,
        shadowElevation = if (highlight) 14.dp else 8.dp,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun GameCanvas(modifier: Modifier = Modifier, state: GameState) {
    val colorScheme = MaterialTheme.colorScheme
    val surfaceTint = colorScheme.surface.copy(alpha = 0.65f)
    val borderColor = colorScheme.onSurface.copy(alpha = 0.12f)
    val playerCoreColor = colorScheme.secondary
    val playerGlowMid = playerCoreColor.copy(alpha = 0.55f)
    val playerGlowOuter = playerCoreColor.copy(alpha = 0.25f)
    val obstacleTopColor = colorScheme.primary.copy(alpha = 0.85f)
    val obstacleBottomColor = colorScheme.primary.copy(alpha = 0.45f)

    val infiniteTransition = rememberInfiniteTransition(label = "playerMotion")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playerGlowPulse"
    )
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -0.18f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playerBobOffset"
    )
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "playerRipple"
    )

    val borderShape = RoundedCornerShape(36.dp)
    Surface(
        modifier = modifier
            .shadow(20.dp, borderShape, clip = false)
            .clip(borderShape),
        color = surfaceTint
    ) {
        Canvas(
            modifier = Modifier
                .background(Color.Transparent)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = borderShape
                )
        ) {
            val laneCount = GameState.laneCount
            val laneWidth = size.width / laneCount
            val obstacleHeight = size.height * GameState.obstacleHeightFraction
            val obstacleWidth = laneWidth * 0.6f
            val obstacleXPadding = (laneWidth - obstacleWidth) / 2f

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3300BCD4),
                        Color(0x55312E81),
                        Color(0x660E7490)
                    )
                ),
                size = size
            )

            for (lane in 0..laneCount) {
                val x = lane * laneWidth
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = max(size.width, size.height) * 0.005f,
                    cap = StrokeCap.Round
                )
            }

            val playerX = laneWidth * (state.playerLane + 0.5f)
            val basePlayerY = size.height * GameState.playerCenterY
            val playerRadius = size.height * GameState.playerRadiusFraction
            val bobbingY = basePlayerY + bobOffset * playerRadius

            val rippleRadius = playerRadius * (1.2f + rippleProgress * 2.1f)
            val rippleAlpha = (1f - rippleProgress) * 0.32f
            if (rippleAlpha > 0.01f) {
                drawCircle(
                    color = playerGlowOuter.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = Offset(playerX, bobbingY)
                )
            }

            drawCircle(
                color = playerGlowOuter,
                radius = playerRadius * 2.2f * glowPulse,
                center = Offset(playerX, bobbingY)
            )
            drawCircle(
                color = playerGlowMid,
                radius = playerRadius * 1.35f * glowPulse,
                center = Offset(playerX, bobbingY)
            )
            drawCircle(
                color = playerCoreColor,
                radius = playerRadius * (0.95f + glowPulse * 0.05f),
                center = Offset(playerX, bobbingY)
            )

            state.obstacles.forEach { obstacle ->
                val top = obstacle.top * size.height
                val left = obstacle.lane * laneWidth + obstacleXPadding
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            obstacleTopColor,
                            obstacleBottomColor
                        )
                    ),
                    topLeft = Offset(left, top),
                    size = Size(obstacleWidth, obstacleHeight),
                    cornerRadius = CornerRadius(obstacleWidth * 0.32f, obstacleWidth * 0.32f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(left, top),
                    size = Size(obstacleWidth, obstacleHeight),
                    cornerRadius = CornerRadius(obstacleWidth * 0.32f, obstacleWidth * 0.32f),
                    style = Stroke(width = obstacleWidth * 0.05f)
                )
            }
        }
    }
}

@Composable
private fun ControlRow(onMoveLeft: () -> Unit, onMoveRight: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ControlButton(
            title = stringResource(id = R.string.move_left),
            display = "<",
            onClick = onMoveLeft
        )
        ControlButton(
            title = stringResource(id = R.string.move_right),
            display = ">",
            onClick = onMoveRight
        )
    }
}

@Composable
private fun RowScope.ControlButton(
    title: String,
    display: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "controlButtonScale"
    )
    val inactiveColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
    val activeColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) activeColor else inactiveColor,
        label = "controlButtonColor"
    )

    Surface(
        modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .pointerInput(onClick) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    try {
                        onClick()
                        waitForUpOrCancellation()
                    } finally {
                        isPressed = false
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = title) {
                    onClick()
                    true
                }
            },
        shape = shape,
        color = containerColor,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, containerColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = display,
                style = MaterialTheme.typography.displayMedium,
                fontSize = 48.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    isNewHighScore: Boolean,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(28.dp)),
            tonalElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.run_complete_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(id = R.string.score_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(id = R.string.high_score_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = highScore.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isNewHighScore,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            text = stringResource(id = R.string.new_high_score),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBackToMenu
                    ) {
                        Text(text = stringResource(id = R.string.back_to_menu))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRestart
                    ) {
                        Text(text = stringResource(id = R.string.restart_run))
                    }
                }
            }
        }
    }
}

private const val HIGH_SCORE_PREFS_NAME = "everglow_prefs"
private const val HIGH_SCORE_PREF_KEY = "high_score"
