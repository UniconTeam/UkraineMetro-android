package team.unicon.ukrainemetro.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.unicon.ukrainemetro.ui.settings.subitems.SettingDropdownItem
import team.unicon.ukrainemetro.ui.settings.subitems.SettingDropdownSelection
import team.unicon.ukrainemetro.ui.settings.subitems.SettingSwitchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val settingItems = remember { listOf(
        // ====
        Setting.GroupSeparator("Common"),

        Setting.Dropdown(
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

        // ====
        Setting.GroupSeparator("Appearance"),
        Setting.Switch(
            title = "Alternative branch colors",
            description = "Use styled colors for branches.",
            key = "alternative_colors",
            defaultValue = false
        ),
        Setting.Switch(
            title = "Simplified map",
            description = "Use lightweight version of map.",
            key = "lightweight_map",
            defaultValue = false
        ),
    ) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            items(settingItems) { item ->
                when(item) {
                    is Setting.GroupSeparator -> {
                        Text(
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                            text = item.title,
                            lineHeight = 8.sp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is Setting.Switch -> {
                        var value by remember { mutableStateOf(item.defaultValue) }

                        SettingSwitchItem(
                            isEnabled = true,
                            title = item.title,
                            description = item.description,
                            value = value,
                            onChange = { value = it }
                        )
                    }

                    is Setting.Dropdown -> {
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