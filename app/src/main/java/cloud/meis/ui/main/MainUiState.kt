package cloud.meis.ui.main

import cloud.meis.data.local.entity.ServerEntity

data class MainUiState(
    val servers: List<ServerEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isChecking: Boolean = false,
    val debugLogs: List<String> = emptyList()
)
