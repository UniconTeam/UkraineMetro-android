package team.unicon.ukrainemetro.settings

import team.unicon.ukrainemetro.ui.settings.subitems.SettingDropdownSelection

sealed interface Setting {
    data class GroupSeparator(
        val title: String,
    ) : Setting

    data class Switch(
        val title: String,
        val description: String,
        val key: String,
        val defaultValue: Boolean
    ) : Setting

    data class Dropdown(
        val title: String,
        val description: String,
        val key: String,
        val selections: List<SettingDropdownSelection>
    ) : Setting
}