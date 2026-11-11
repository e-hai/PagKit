package io.github.pagkit.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pagkit.cache.PagDiskCache
import io.github.pagkit.compose.PagImageView
import io.github.pagkit.compose.PagView
import io.github.pagkit.compose.rememberPagState
import io.github.pagkit.model.PagScaleMode
import io.github.pagkit.model.PagSource

private const val SAMPLE_REMOTE_URL = "https://pag.io/file/like.pag"

@Composable
fun NetworkAndListTab() {
    var maxDiskMb by remember { mutableLongStateOf(PagDiskCache.getMaxDiskSize() / (1024 * 1024)) }
    var cacheActionStatus by remember { mutableStateOf("磁盘缓存正常") }

    val networkPagState = rememberPagState(
        source = PagSource.Url(SAMPLE_REMOTE_URL),
        autoPlay = true,
        repeatCount = 0,
        scaleMode = PagScaleMode.LetterBox
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "网络异步加载与 PagImageView 列表",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 网络 PAG 渲染卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!networkPagState.isLoaded && networkPagState.loadError == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在从网络下载 PAG 动效...", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (networkPagState.loadError != null) {
                    Text(
                        text = "下载失败: ${networkPagState.loadError?.message}",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                PagView(
                    state = networkPagState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 磁盘缓存管理
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("磁盘缓存上限: $maxDiskMb MB", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = {
                PagDiskCache.removeAll()
                cacheActionStatus = "已清空全部本地磁盘缓存"
            }) {
                Text("清空缓存")
            }
        }
        Text(
            text = cacheActionStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "PagImageView 列表复用性能对比 (序列帧缓存)",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(4.dp))

        // 6 个轻量 PagImageView 网格列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(6) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val itemState = rememberPagState(
                        source = PagSource.Url(SAMPLE_REMOTE_URL),
                        autoPlay = true,
                        repeatCount = 0,
                        scaleMode = PagScaleMode.LetterBox
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (!itemState.isLoaded && itemState.loadError == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (itemState.loadError != null) {
                            Text(
                                text = "加载失败",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red
                            )
                        }
                        PagImageView(
                            state = itemState,
                            modifier = Modifier.size(80.dp),
                            cacheAllFramesInMemory = true
                        )
                        Text(
                            text = "#$index",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
