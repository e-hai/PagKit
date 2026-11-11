package io.github.pagkit.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pagkit.compose.PagView
import io.github.pagkit.compose.rememberPagState
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource
import pagkit.sample.shared.generated.resources.Res

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BasicPlaybackTab() {
    var pagBytes by remember { mutableStateOf<ByteArray?>(null) }
    val logs = remember { mutableStateListOf<String>() }

    fun addLog(msg: String) {
        if (logs.size > 20) logs.removeAt(0)
        logs.add(msg)
    }

    LaunchedEffect(Unit) {
        try {
            val bytes = Res.readBytes("files/like.pag")
            pagBytes = bytes
            addLog("Loaded like.pag (${bytes.size} bytes)")
        } catch (t: Throwable) {
            addLog("Error reading like.pag: ${t.message}")
        }
    }

    val pagState = rememberPagState(
        source = pagBytes?.let { PagSource.Bytes(it) },
        autoPlay = true,
        repeatCount = 0,
        scaleMode = PagScaleMode.LetterBox
    )

    LaunchedEffect(pagBytes) {
        val b = pagBytes
        if (b != null) {
            pagState.source = PagSource.Bytes(b)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PagView GPU 渲染测试",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 动画预览卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PagView(
                    state = pagState,
                    modifier = Modifier.fillMaxSize(),
                    onCompositionLoaded = { file ->
                        val markerInfo = if (file.markers.isNotEmpty()) "${file.markers.size} 个 Markers" else "无 Marker"
                        val audioInfo = if (file.audioBytes() != null) "含伴音" else "无伴音"
                        addLog(">> 动效就绪: 时长 ${file.durationUs / 1000}ms, $markerInfo, $audioInfo")
                    },
                    onAnimationStart = { addLog(">> onAnimationStart") },
                    onAnimationEnd = { addLog(">> onAnimationEnd") },
                    onAnimationCancel = { addLog(">> onAnimationCancel") },
                    onAnimationRepeat = { addLog(">> onAnimationRepeat") },
                    onAnimationUpdate = { /* 进度自动更新到 pagState.progress */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 播放控制按钮栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { pagState.play(); addLog("Clicked Play") }) {
                Text("播放")
            }
            Button(onClick = { pagState.pause(); addLog("Clicked Pause") }) {
                Text("暂停")
            }
            OutlinedButton(onClick = { pagState.stop(); addLog("Clicked Stop") }) {
                Text("停止")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 播放进度拖拽 Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("播放进度 (seekTo)", style = MaterialTheme.typography.bodyMedium)
                Text("${(pagState.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = pagState.progress.toFloat(),
                onValueChange = { newProg ->
                    pagState.seekTo(newProg.toDouble())
                },
                valueRange = 0f..1f
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ScaleMode 模式切换
        Text("缩放模式 (ScaleMode)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PagScaleMode.entries.forEach { mode ->
                FilterChip(
                    selected = pagState.scaleMode == mode,
                    onClick = {
                        pagState.scaleMode = mode
                        addLog("ScaleMode -> $mode")
                    },
                    label = { Text(mode.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 循环次数切换
        Text("循环播放控制", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = pagState.repeatCount == 0,
                onClick = { pagState.repeatCount = 0; addLog("Repeat -> 无限循环") },
                label = { Text("无限循环") }
            )
            FilterChip(
                selected = pagState.repeatCount == 1,
                onClick = { pagState.repeatCount = 1; addLog("Repeat -> 播放 1 次") },
                label = { Text("播放 1 次") }
            )
            FilterChip(
                selected = pagState.repeatCount == 3,
                onClick = { pagState.repeatCount = 3; addLog("Repeat -> 播放 3 次") },
                label = { Text("播放 3 次") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 事件日志监控控制台
        Text("事件日志输出", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (logs.isEmpty()) {
                    Text("暂无日志", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    logs.forEach { log ->
                        Text(log, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
