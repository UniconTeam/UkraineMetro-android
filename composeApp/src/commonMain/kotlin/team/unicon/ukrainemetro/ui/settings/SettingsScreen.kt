package team.unicon.ukrainemetro.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import team.unicon.ukrainemetro.ui.settings.subitems.SettingSwitchItem

sealed interface SettingItem {
    data class Switch(
        val title: String,
        val description: String,
        val key: String,
        val defaultValue: Boolean
    ) : SettingItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val settingItems = remember { listOf(
        SettingItem.Switch(
            title = "Alternative branch colors",
            description = "Use theme provided colors for branches instead of vanilla.",
            key = "alternative_colors",
            defaultValue = false
        )
    ) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            items(settingItems) { item ->
                when(item) {
                    is SettingItem.Switch -> {
                        SettingSwitchItem(
                            isEnabled = true,
                            title = item.title,
                            description = item.description,
                            value = false,
                            onChange = { }
                        )
                    }
                }
            }
        }
    }
}