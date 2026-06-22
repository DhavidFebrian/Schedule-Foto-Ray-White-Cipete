package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScheduleViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InputFormScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    TAMBAH("Tambah", Icons.Filled.AddCircle, Icons.Outlined.AddCircle, "tab_tambah"),
    PENGATURAN("Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(TabItem.DASHBOARD) }
                val viewmodel: ScheduleViewModel = viewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            TabItem.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { 
                                        currentTab = tab
                                        if (tab == TabItem.TAMBAH) {
                                            viewmodel.resetForm()
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    when (currentTab) {
                        TabItem.DASHBOARD -> DashboardScreen(
                            viewModel = viewmodel,
                            onNavigateToForm = { currentTab = TabItem.TAMBAH },
                            modifier = contentModifier
                        )
                        TabItem.TAMBAH -> InputFormScreen(
                            viewModel = viewmodel,
                            onNavigateBack = { currentTab = TabItem.DASHBOARD },
                            modifier = contentModifier
                        )
                        TabItem.PENGATURAN -> SettingsScreen(
                            viewModel = viewmodel,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
