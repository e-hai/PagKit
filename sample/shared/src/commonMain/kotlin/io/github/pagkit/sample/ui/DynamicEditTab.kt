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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pagkit.compose.PagView
import io.github.pagkit.compose.rememberPagState
import io.github.pagkit.composition.PagFile
import io.github.pagkit.composition.PagImage
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource
import io.github.pagkit.model.PagTextData
import pagkit.sample.shared.generated.resources.Res

// 1x1 纯红 PNG 示例二进制
private val SAMPLE_RED_PNG = byteArrayOf(
    0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
    0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x15.toByte(), 0xC4.toByte(),
    0x89.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(),
    0x54.toByte(), 0x78.toByte(), 0x9C.toByte(), 0x63.toByte(), 0xF8.toByte(), 0xCF.toByte(), 0xC0.toByte(), 0xF0.toByte(),
    0x1F.toByte(), 0x00.toByte(), 0x05.toByte(), 0xFE.toByte(), 0x02.toByte(), 0xFE.toByte(), 0xDC.toByte(), 0xCC.toByte(),
    0x59.toByte(), 0xE7.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x49.toByte(), 0x45.toByte(),
    0x4E.toByte(), 0x44.toByte(), 0xAE.toByte(), 0x42.toByte(), 0x60.toByte(), 0x82.toByte()
)

@Composable
fun DynamicEditTab() {
    var rawBytes by remember { mutableStateOf<ByteArray?>(null) }
    var currentFile by remember { mutableStateOf<PagFile?>(null) }
    var userText by remember { mutableStateOf("KMP 动态文本") }
    var fontSize by remember { mutableFloatStateOf(36f) }
    var textColorArgb by remember { mutableIntStateOf(0xFFFFD700.toInt()) } // 金黄色
    var layerAlpha by remember { mutableFloatStateOf(1f) }
    var layerScale by remember { mutableFloatStateOf(1f) }
    var statusMessage by remember { mutableStateOf("正在加载动效...") }

    val pagState = rememberPagState(
        source = null,
        autoPlay = true,
        repeatCount = 0,
        scaleMode = PagScaleMode.LetterBox
    )

    LaunchedEffect(Unit) {
        try {
            val bytes = Res.readBytes("files/like.pag")
            rawBytes = bytes
            pagState.source = PagSource.Bytes(bytes)
            statusMessage = "动效已加载，支持动态文本/图层属性实时调整"
        } catch (t: Throwable) {
            statusMessage = "加载错误: ${t.message}"
        }
    }

    // 应用文本图层修改
    fun applyTextReplacement() {
        val file = currentFile ?: return
        val numTexts = file.numTexts
        if (numTexts > 0) {
            val textData = PagTextData(
                text = userText,
                fontSize = fontSize,
                fillColorArgb = textColorArgb,
                strokeColorArgb = 0xFF000000.toInt(),
                strokeWidth = 2f,
                fauxBold = true
            )
            file.replaceText(0, textData)
            pagState.flush()
            statusMessage = "成功替换文本图层[0] -> $userText"
        } else {
            statusMessage = "提示: 当前 like.pag 包含 ${file.numImages} 个图片图层，0 个文本图层。replaceText 接口调用完成。"
        }
    }

    // 应用图片图层修改 (replaceImage)
    fun applyImageReplacement() {
        val file = currentFile ?: return
        if (file.numImages > 0) {
            val img = PagImage.fromBytes(SAMPLE_RED_PNG)
            if (img != null) {
                file.replaceImage(0, img)
                pagState.flush()
                statusMessage = "成功替换图片图层[0] (红点位图)"
            } else {
                statusMessage = "创建 PagImage 失败"
            }
        } else {
            statusMessage = "当前 PAG 文件无图片图层"
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
            text = "图层动态编辑实验室",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 动画渲染卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PagView(
                    state = pagState,
                    modifier = Modifier.fillMaxSize(),
                    onCompositionLoaded = { file ->
                        currentFile = file
                        statusMessage = "文件就绪: 文本图层=${file.numTexts}, 图片图层=${file.numImages}, 子图层=${file.numChildren}"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 图层透明度调整
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("图层透明度 (alpha): ${(layerAlpha * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = layerAlpha,
            onValueChange = { newAlpha ->
                layerAlpha = newAlpha
                currentFile?.let { file ->
                    file.alpha = newAlpha
                    pagState.flush()
                }
            },
            valueRange = 0f..1f
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 图层缩放调整
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("图层缩放 (scale): ${(layerScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = layerScale,
            onValueChange = { newScale ->
                layerScale = newScale
                currentFile?.let { file ->
                    file.resetMatrix()
                    file.setScale(newScale, newScale)
                    pagState.flush()
                }
            },
            valueRange = 0.2f..2f
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 文本内容输入框
        OutlinedTextField(
            value = userText,
            onValueChange = { userText = it },
            label = { Text("替换文本内容 (replaceText)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 字号调整 Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("字号大小: ${fontSize.toInt()} px", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 16f..72f
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 填充颜色快速选择
        Text("文本填充颜色", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val colors = listOf(
                "金色" to 0xFFFFD700.toInt(),
                "亮红" to 0xFFFF3B30.toInt(),
                "翠绿" to 0xFF34C759.toInt(),
                "纯白" to 0xFFFFFFFF.toInt()
            )
            colors.forEach { (name, colorVal) ->
                FilterChip(
                    selected = textColorArgb == colorVal,
                    onClick = { textColorArgb = colorVal },
                    label = { Text(name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { applyTextReplacement() }
            ) {
                Text("替换文本")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { applyImageReplacement() }
            ) {
                Text("替换图片[0]")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    layerAlpha = 1f
                    layerScale = 1f
                    val copy = currentFile?.copyOriginal()
                    if (copy != null) {
                        currentFile = copy
                        pagState.source = PagSource.Composition(copy)
                        pagState.flush()
                        statusMessage = "已通过 copyOriginal() 重置为初始动效"
                    } else {
                        rawBytes?.let {
                            pagState.source = PagSource.Bytes(it)
                            statusMessage = "已从原始字节重新装载模板"
                        }
                    }
                }
            ) {
                Text("重置模板")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val file = currentFile ?: return@OutlinedButton
                val decoder = io.github.pagkit.composition.PagDecoder.make(file)
                if (decoder != null) {
                    statusMessage = "PagDecoder 测试通过: 共 ${decoder.numFrames} 帧, 尺寸 ${decoder.width}x${decoder.height}, 帧率 ${decoder.frameRate}fps"
                } else {
                    statusMessage = "PagDecoder.make 失败"
                }
            }
        ) {
            Text("离线解码分析 (PagDecoder)")
        }
    }
}
