package com.blankotech.optimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
  private val viewModel: OptimizerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF0D0D0D))
      ) { innerPadding ->
        OptimizerScreen(
          viewModel = viewModel,
          onLaunchSuccess = { finish() },
          modifier = Modifier.padding(innerPadding)
        )
      }
    }
  }
}

@Composable
fun OptimizerScreen(
  viewModel: OptimizerViewModel,
  onLaunchSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val buttonScale by animateFloatAsState(
    targetValue = if (uiState.isOptimizing) 0.94f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "scale"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0D0D0D))
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = "MC Optimizer",
      fontSize = 32.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF00D700),
      textAlign = TextAlign.Center
    )

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = uiState.statusText,
        fontSize = 16.sp,
        color = Color(0xFFB0B0B0),
        textAlign = TextAlign.Center
      )

      if (uiState.killedProcesses > 0) {
        Text(
          text = "Procesos cerrados: ${uiState.killedProcesses}",
          fontSize = 14.sp,
          color = Color(0xFF00FF00)
        )
      }
    }

    Button(
      onClick = { viewModel.startOptimization(context, onLaunchSuccess) },
      enabled = !uiState.isOptimizing,
      modifier = Modifier
        .size(120.dp)
        .scale(buttonScale),
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF00D700),
        disabledContainerColor = Color(0xFF006600)
      )
    ) {
      if (uiState.isOptimizing) {
        CircularProgressIndicator(
          color = Color(0xFF000000),
          modifier = Modifier.size(60.dp)
        )
      } else {
        Icon(
          imageVector = Icons.Default.Rocket,
          contentDescription = "Iniciar optimización",
          tint = Color(0xFF000000),
          modifier = Modifier.size(60.dp)
        )
      }
    }
  }
}
