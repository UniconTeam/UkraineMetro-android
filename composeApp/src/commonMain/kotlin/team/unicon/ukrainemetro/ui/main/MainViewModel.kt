package team.unicon.ukrainemetro.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import team.unicon.ukrainemetro.entities.Subway
import team.unicon.ukrainemetro.entities.SubwayInfo
import team.unicon.ukrainemetro.repositories.SubwaysRepository

class MainViewModel(
    private val subwaysRepository: SubwaysRepository
) : ViewModel() {
    sealed interface UIState {
        data object Loading : UIState
        data class Present(val subwayInfo: SubwayInfo) : UIState
    }

    private val _uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    suspend fun loadSubway() {
        val subwayInfo = subwaysRepository.getSubwayInfo(Subway.Kharkiv)
        _uiState.value = UIState.Present(subwayInfo)
    }
}