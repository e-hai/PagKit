package io.github.pagkit.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.pagkit.sample.ui.BasicPlaybackTab
import io.github.pagkit.sample.ui.CompositionLabTab
import io.github.pagkit.sample.ui.DynamicEditTab
import io.github.pagkit.sample.ui.NetworkAndListTab

enum class SampleTab(val label: String) {
    Basic("基础播放"),
    DynamicEdit("图文替换"),
    Composition("多层编排"),
    NetworkList("网络/列表")
}

@Composable
fun SampleApp() {
    MaterialTheme {
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = SampleTab.entries

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            label = { Text(tab.label) },
                            icon = { }
                        )
                    }
                }
            }
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (tabs[selectedTabIndex]) {
                    SampleTab.Basic -> BasicPlaybackTab()
                    SampleTab.DynamicEdit -> DynamicEditTab()
                    SampleTab.Composition -> CompositionLabTab()
                    SampleTab.NetworkList -> NetworkAndListTab()
                }
            }
        }
    }
}
