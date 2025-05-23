package team.unicon.ukrainemetro.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import team.unicon.ukrainemetro.LocalStrings
import ukrainemetro.composeapp.generated.resources.Res
import ukrainemetro.composeapp.generated.resources.compose_multiplatform

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()

    LaunchedEffect(Unit)  {
        viewModel.loadSubway()
    }

    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                modifier = Modifier,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                placeholder = { Text("Search here") },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                // trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
            )
        }

    Scaffold(
        topBar = {
            TopSearchBar(
                state = searchBarState,
                inputField = inputField,
            )
            ExpandedFullScreenSearchBar(
                state = searchBarState,
                inputField = inputField,
            ) {
                LazyColumn {
                    items(listOf("Kharkiv", "Kyiv", "Dnipro", "Kriviy Rig", "Benchmark")) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {  }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = it
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            when (uiState) {
                is MainViewModel.UIState.Loading -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is MainViewModel.UIState.Present -> {
                    val presentUiState = (uiState as MainViewModel.UIState.Present)

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        SubwayMap(
                            modifier = Modifier.fillMaxSize(),
                            elements = presentUiState.subwayInfo.elements,
                            onStationClick = {
                                println(it.name?.resolve())
                            }
                        )
                    }
                }
            }

            HorizontalFloatingToolbar(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-16).dp),
                expanded = true,
                leadingContent = { },
                trailingContent = {
                    AppBarRow(
                        overflowIndicator = { menuState ->
                            IconButton(
                                onClick = {
                                    if (menuState.isExpanded) {
                                        menuState.dismiss()
                                    } else {
                                        menuState.show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Localized description"
                                )
                            }
                        }
                    ) {
                        clickableItem(
                            onClick = { /* doSomething() */ },
                            icon = {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = "Localized description"
                                )
                            },
                            label = "Download"
                        )
                        clickableItem(
                            onClick = { /* doSomething() */ },
                            icon = {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = "Localized description"
                                )
                            },
                            label = "Favorite"
                        )
                        clickableItem(
                            onClick = { /* doSomething() */ },
                            icon = {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Localized description"
                                )
                            },
                            label = "ArrowUpward"
                        )
                    }
                },
                content = {
                    FilledIconButton(
                        modifier = Modifier.width(64.dp),
                        onClick = { /* doSomething() */ }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Localized description")
                    }
                }
            )
        }
    }
}