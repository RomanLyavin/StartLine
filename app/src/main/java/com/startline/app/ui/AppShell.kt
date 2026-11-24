package com.startline.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.startline.app.AppViewModel
import com.startline.app.ui.settings.AboutScreen
import com.startline.app.ui.settings.SettingsBackupScreen
import com.startline.app.ui.settings.TrainingScheduleScreen

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun StartLineApp(vm: AppViewModel) {
    if (vm.user == null) {
        AuthScreen(vm)
        return
    }
    if (!vm.onboarded) {
        OnboardingScreen(vm)
        return
    }
    MainShell(vm)
}

@Composable
private fun MainShell(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAi by rememberSaveable { mutableStateOf(false) }
    var showSchedule by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showTaper by rememberSaveable { mutableStateOf(false) }
    var showWatches by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(vm.message) {
        val text = vm.message
        if (text != null) {
            snackbar.showSnackbar(text)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(Unit) { vm.maybeAutoSync() }

    val tabs = listOf(
        TabItem("Обзор", Icons.Filled.Home),
        TabItem("Тренировки", Icons.Filled.DirectionsRun),
        TabItem("Календарь", Icons.Filled.CalendarMonth),
        TabItem("Модель", Icons.Filled.Insights),
        TabItem("Профиль", Icons.Filled.Person),
    )

    Scaffold(
        containerColor = Bg,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Surface3,
                    contentColor = TextPrimary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                )
            }
        },
        bottomBar = {
            if (detailId == null && !showAi && !showSchedule && !showAbout && !showTaper && !showWatches && !showBackup) {
                NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(item.icon, item.label) },
                            label = {
                                Text(item.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Bg,
                                selectedTextColor = Lime,
                                indicatorColor = Lime,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            val id = detailId
            val activity = id?.let { vm.activityById(it) }
            when {
                id != null && activity != null -> {
                    ActivityDetailScreen(
                        vm = vm,
                        activity = activity,
                        onBack = {
                            detailId = null
                            vm.closeActivity()
                        },
                    )
                }
                id != null -> {
                    LaunchedEffect(id) { detailId = null }
                    LoadingBlock()
                }
                showAi -> {
                    AiScreen(
                        vm = vm,
                        onBack = { showAi = false },
                        onOpenProfile = {
                            showAi = false
                            tab = 4
                        },
                    )
                }
                showSchedule -> TrainingScheduleScreen(vm = vm, onBack = { showSchedule = false })
                showAbout -> AboutScreen(vm = vm, onBack = { showAbout = false })
                showTaper -> TaperScreen(
                    vm = vm,
                    onBack = { showTaper = false },
                    onOpenCalendar = {
                        showTaper = false
                        tab = 2
                    },
                )
                showWatches -> WatchConnectionsScreen(vm = vm, onBack = { showWatches = false })
                showBackup -> SettingsBackupScreen(vm = vm, onBack = { showBackup = false })
                tab == 0 -> DashboardScreen(
                    vm = vm,
                    onOpenCalendar = { tab = 2 },
                    onOpenAi = { showAi = true },
                    onOpenProfile = { tab = 4 },
                    onOpenActivities = { tab = 1 },
                    onOpenAbout = { showAbout = true },
                    onOpenSupport = { AppLinks.openUrl(context, AppLinks.SUPPORT_URL) },
                )
                tab == 1 -> ActivitiesScreen(
                    vm = vm,
                    onOpenActivity = { detailId = it },
                )
                tab == 2 -> CalendarScreen(vm)
                tab == 3 -> ModelScreen(vm, onOpenProfile = { tab = 4 })
                else -> ProfileScreen(
                    vm = vm,
                    onOpenSchedule = { showSchedule = true },
                    onOpenAbout = { showAbout = true },
                    onOpenTaper = { showTaper = true },
                    onOpenWatches = { showWatches = true },
                    onOpenBackup = { showBackup = true },
                )
            }
        }
    }
}
