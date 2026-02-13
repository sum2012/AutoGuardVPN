package com.autoguard.vpn.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoguard.vpn.R
import com.autoguard.vpn.data.model.VpnServer
import com.autoguard.vpn.ui.components.CountryHeader
import com.autoguard.vpn.ui.components.ServerCard
import com.autoguard.vpn.ui.theme.ErrorRed
import com.autoguard.vpn.ui.viewmodel.MainViewModel

/**
 * Server List Screen
 * Displays all available VPN servers
 *
 * @param viewModel Main interface ViewModel
 * @param onDismiss Dismiss callback
 * @param onServerSelected Server selected callback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
    onServerSelected: (VpnServer) -> Unit = {}
) {
    val serverList by viewModel.serverList.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Filtered server list
    val filteredServers = remember(searchQuery, serverList) {
        if (searchQuery.isBlank()) {
            serverList
        } else {
            serverList.filter {
                it.city.contains(searchQuery, ignoreCase = true) ||
                        it.country.contains(searchQuery, ignoreCase = true) ||
                        it.endpoint.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Group by country
    val groupedServers = remember(filteredServers) {
        filteredServers.groupBy { it.country }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.server_select_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        LoadingContent()
                    }
                    error != null -> {
                        ErrorContent(error = error!!)
                    }
                    serverList.isEmpty() -> {
                        EmptyContent()
                    }
                    else -> {
                        ServerListContent(
                            groupedServers = groupedServers,
                            selectedServerId = selectedServer?.id,
                            onServerClick = onServerSelected
                        )
                    }
                }
            }
        }
    }
}

/**
 * Search Bar component
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = stringResource(R.string.server_search_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

/**
 * Server List content
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerListContent(
    groupedServers: Map<String, List<VpnServer>>,
    selectedServerId: String?,
    onServerClick: (VpnServer) -> Unit
) {
    // 透過 NestedScrollConnection 強力攔截所有傳向父層的滾動事件
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 這裡可以選擇不處理 pre-scroll，讓列表本身先滑動
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 當列表已經滾動到盡頭（頂部或底部），將剩餘的 available 偏移量全部消耗掉。
                // 這樣父組件就收不到任何滑動量，無法觸發過度滾動或彈回效果。
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // 同樣消耗掉慣性滑動的剩餘速度
                return available
            }
        }
    }

    // 禁用 Compose 內建的 Overscroll 視覺效果
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // 注入 NestedScrollConnection
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedServers.forEach { (country, servers) ->
                item(key = "header_$country") {
                    CountryHeader(
                        country = country,
                        serverCount = servers.size
                    )
                }

                items(
                    items = servers,
                    key = { "server_${it.id}" } // Use prefix to avoid any potential collision with headers
                ) { server ->
                    ServerCard(
                        server = server,
                        isSelected = server.id == selectedServerId,
                        onClick = { onServerClick(server) }
                    )
                }

                item(key = "spacer_$country") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Loading content
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.server_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error content
 */
@Composable
private fun ErrorContent(error: String) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Empty content
 */
@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.server_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
