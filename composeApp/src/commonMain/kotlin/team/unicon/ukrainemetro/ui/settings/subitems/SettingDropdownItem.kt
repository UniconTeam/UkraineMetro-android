package team.unicon.ukrainemetro.ui.settings.subitems

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import team.unicon.ukrainemetro.ui.settings.SettingsConstants

data class SettingDropdownSelection(
    val title: String,
    val key: String
)

@Composable
fun SettingDropdownItem(
    isEnabled: Boolean = true,
    title: String,
    description: String,
    values: List<SettingDropdownSelection>,
    selectedIndex: Int,
    onChange: (selectedIndex: Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.clickable(onClick = { isExpanded = true })
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = SettingsConstants.itemsPaddingsVertical,
                horizontal = SettingsConstants.itemsPaddingsHorizontal,
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    modifier = Modifier.alpha(if(isEnabled) 1f else 0.5f),
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.alpha(if(isEnabled) 1f else 0.5f),
                    text = if(isEnabled) description else "Feature not available on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Box {
                Box(
                    modifier = Modifier.clickable { isExpanded = true }
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = values[selectedIndex].title
                    )
                }
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    var index = 0
                    for (selection in values) {
                        DropdownMenuItem(
                            text = { Text(text = selection.title) },
                            onClick = {
                                onChange(index++)
                            }
                        )
                    }
                }
            }
        }
    }
}