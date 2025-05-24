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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import team.unicon.ukrainemetro.ui.settings.subitems.SettingDropdownItem
import team.unicon.ukrainemetro.ui.settings.subitems.SettingDropdownSelection
import team.unicon.ukrainemetro.ui.settings.subitems.SettingSwitchItem

sealed interface SettingItem {
    data class Switch(
        val title: String,
        val description: String,
        val key: String,
        val defaultValue: Boolean
    ) : SettingItem

    data class Dropdown(
        val title: String,
        val description: String,
        val key: String,
        val selections: List<SettingDropdownSelection>
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val settingItems = remember { listOf(
        SettingItem.Dropdown(
            title = "App language",
            description = "Alternate app language.",
            key = "language",
            selections = listOf(
                SettingDropdownSelection(
                    title = "English (System)",
                    key = "abc1",
                ),
                SettingDropdownSelection(
                    title = "Ukrainian",
                    key = "abc2",
                ),
                SettingDropdownSelection(
                    title = "German",
                    key = "abc3",
                ),
            )
        ),
        SettingItem.Switch(
            title = "Alternative branch colors",
            description = "Use styled colors for branches.",
            key = "alternative_colors",
            defaultValue = false
        ),
        SettingItem.Switch(
            title = "Simplified map",
            description = "Use lightweight version of map.",
            key = "lightweight_map",
            defaultValue = false
        ),
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
                        var value by remember { mutableStateOf(item.defaultValue) }

                        SettingSwitchItem(
                            isEnabled = true,
                            title = item.title,
                            description = item.description,
                            value = value,
                            onChange = { value = it }
                        )
                    }

                    is SettingItem.Dropdown -> {
                        var selectedIndex by remember { mutableStateOf(0) }

                        SettingDropdownItem(
                            isEnabled = true,
                            title = item.title,
                            description = item.description,
                            values = item.selections,
                            selectedIndex = selectedIndex,
                            onChange = { selectedIndex = it }
                        )
                    }
                }
            }
        }
    }
}