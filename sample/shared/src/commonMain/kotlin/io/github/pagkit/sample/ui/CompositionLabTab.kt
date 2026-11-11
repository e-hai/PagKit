package io.github.pagkit.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pagkit.compose.PagView
import io.github.pagkit.compose.rememberPagState
import io.github.pagkit.composition.PagComposition
import io.github.pagkit.composition.PagFile
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource
import pagkit.sample.shared.generated.resources.Res

@Composable
fun CompositionLabTab() {
    var rawBytes by remember { mutableStateOf<ByteArray?>(null) }
    var parentComp by remember { mutableStateOf<PagComposition?>(null) }
    var layer1 by remember { mutableStateOf<PagFile?>(null) }
    var layer2 by remember { mutableStateOf<PagFile?>(null) }

    var layer1Visible by remember { mutableStateOf(true) }
    var layer2Visible by remember { mutableStateOf(true) }
    var layer2DelayMs by remember { mutableFloatStateOf(500f) }
    var statusText by remember { mutableStateOf("正在初始化合成画布...") }

    val pagState = rememberPagState(
        source = null,
        autoPlay = true,
        repeatCount = 0,
        scaleMode = PagScaleMode.LetterBox
    )

    // 构建合成容器
    fun setupComposition(bytes: ByteArray) {
        val f1 = PagFile.loadFromBytes(bytes) ?: return
        val f2 = PagFile.loadFromBytes(bytes) ?: return

        val comp = PagComposition.make(720, 720)

        // 调整图层 1：居左上，缩小为 0.7 倍
        f1.setScale(0.7f, 0.7f)
        f1.setTranslation(-80f, 0f)

        // 调整图层 2：居右下，缩小为 0.7 倍，并延迟启动时间轴
        f2.setScale(0.7f, 0.7f)
        f2.setTranslation(120f, 0f)
        f2.startTimeUs = (layer2DelayMs * 1000).toLong()

        comp.addLayer(f1)
        comp.addLayer(f2)

        layer1 = f1
        layer2 = f2
        parentComp = comp

        // 将合成容器挂载为数据源
        pagState.source = PagSource.Composition(comp)
        pagState.play()
        statusText = "已合成双动效：图层1(左)与图层2(右，延迟${layer2DelayMs.toInt()}ms)"
    }

    LaunchedEffect(Unit) {
        try {
            val bytes = Res.readBytes("files/like.pag")
            rawBytes = bytes
            setupComposition(bytes)
        } catch (t: Throwable) {
            statusText = "加载失败: ${t.message}"
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
            text = "多 PAG 组合编排实验室",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 合成画布预览
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PagView(
                    state = pagState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { pagState.play() }) {
                Text("播放")
            }
            Button(onClick = { pagState.pause() }) {
                Text("暂停")
            }
            OutlinedButton(onClick = { pagState.stop() }) {
                Text("停止")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 图层可见性控制开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("图层 1 可见性 (左侧)", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = layer1Visible,
                onCheckedChange = {
                    layer1Visible = it
                    layer1?.visible = it
                    pagState.flush()
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("图层 2 可见性 (右侧)", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = layer2Visible,
                onCheckedChange = {
                    layer2Visible = it
                    layer2?.visible = it
                    pagState.flush()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 图层 2 时间轴启动偏移
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("图层 2 启动延迟 (startTimeUs)", style = MaterialTheme.typography.bodyMedium)
                Text("${layer2DelayMs.toInt()} ms", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = layer2DelayMs,
                onValueChange = {
                    layer2DelayMs = it
                    layer2?.startTimeUs = (it * 1000).toLong()
                    pagState.flush()
                },
                valueRange = 0f..2000f
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 图层层级交换按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val p = parentComp
                    if (p != null && p.numChildren >= 2) {
                        p.swapLayerAt(0, 1)
                        pagState.flush()
                        statusText = "已交换图层 0 与图层 1 的层叠顺序"
                    }
                }
            ) {
                Text("交换图层层叠顺序")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    rawBytes?.let { setupComposition(it) }
                }
            ) {
                Text("重新合成")
            }
        }
    }
}
