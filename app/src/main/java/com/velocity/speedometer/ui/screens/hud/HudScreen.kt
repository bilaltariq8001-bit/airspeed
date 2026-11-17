package com.velocity.speedometer.ui.screens.hud

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velocity.speedometer.R
import com.velocity.speedometer.ui.screens.speedometer.SpeedometerViewModel
import com.velocity.speedometer.ui.screens.speedometer.unitDisplayLabel
import com.velocity.speedometer.ui.theme.HudBackground
import com.velocity.speedometer.ui.theme.HudCyan
import com.velocity.speedometer.ui.theme.HudDanger
import com.velocity.speedometer.ui.theme.HudSpeedTextStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HudScreen(
    onExitHud: () -> Unit,
    viewModel: SpeedometerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    // Windshield reflection mirrors vertically by default; long-press switches to unmirrored/horizontal
    var mirrorVertically by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        viewModel.onScreenVisible()
        val activity = context as? Activity
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            viewModel.onScreenHidden()
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val displayedSpeed by animateIntAsState(
        targetValue = uiState.currentSpeedConverted.roundToInt().coerceAtLeast(0),
        animationSpec = tween(durationMillis = 300),
        label = "hud_speed"
    )
    val unitLabel = unitDisplayLabel(uiState.unit)
    val hudColor = if (uiState.isOverLimit) HudDanger else HudCyan
    val exitCd = stringResource(R.string.cd_hud_exit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground)
            .combinedClickable(
                onClick = onExitHud,
                onLongClick = { mirrorVertically = !mirrorVertically }
            )
            .semantics { contentDescription = exitCd }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleY = if (mirrorVertically) -1f else 1f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayedSpeed.toString(),
                style = HudSpeedTextStyle,
                color = hudColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = unitLabel.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = hudColor.copy(alpha = 0.85f)
            )
        }

        Text(
            text = stringResource(R.string.hud_tap_to_exit),
            style = MaterialTheme.typography.labelSmall,
            color = hudColor.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}
