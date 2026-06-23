package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScheduleViewModel
import com.example.ui.ScheduleCategory
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InputFormScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.AnalyticScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

enum class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    ANALYTIC("Analytic", Icons.Filled.PieChart, Icons.Outlined.PieChart, "tab_analytic"),
    TAMBAH("Tambah", Icons.Filled.AddCircle, Icons.Outlined.AddCircle, "tab_tambah"),
    PENGATURAN("Setting", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(TabItem.DASHBOARD) }
                val viewmodel: ScheduleViewModel = viewModel()
                
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "RW Cipete Scheduling",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                            )
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            
                            // Navigation Drawer Items as beautiful list
                            val selectedCat by viewmodel.selectedCategory.collectAsState()
                            
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("Dashboard Utama") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.SEMUA,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.SEMUA
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.ElectricBolt, contentDescription = null) },
                                label = { Text("Jadwal Aktif") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.AKTIF,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.AKTIF
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                label = { Text("Jadwal Selesai") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.SELESAI,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.SELESAI
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                label = { Text("Task Pending") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.TASK,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.TASK
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                                label = { Text("Tambah Jadwal Baru") },
                                selected = currentTab == TabItem.TAMBAH,
                                onClick = {
                                    currentTab = TabItem.TAMBAH
                                    viewmodel.resetForm()
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Setting API Sheets") },
                                selected = currentTab == TabItem.PENGATURAN,
                                onClick = {
                                    currentTab = TabItem.PENGATURAN
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Versi V1.3",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                val selectedCat by viewmodel.selectedCategory.collectAsState()
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
                        val contentModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Versi Update Banner Alert
                            val updateState by viewmodel.appUpdateState.collectAsState()
                            if (updateState != null) {
                                val u = updateState!!
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .padding(top = innerPadding.calculateTopPadding()),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Versi Baru ${u.version} Tersedia!",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            if (u.changelog.isNotBlank()) {
                                                Text(
                                                    u.changelog,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        if (u.downloadUrl.isNotBlank()) {
                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u.downloadUrl))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Update", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentTab) {
                                    TabItem.DASHBOARD -> DashboardScreen(
                                        viewModel = viewmodel,
                                        onNavigateToForm = { currentTab = TabItem.TAMBAH },
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = contentModifier
                                    )
                                    TabItem.ANALYTIC -> AnalyticScreen(
                                        viewModel = viewmodel,
                                        onNavigateBack = { currentTab = TabItem.DASHBOARD },
                                        modifier = contentModifier
                                    )
                                    TabItem.TAMBAH -> InputFormScreen(
                                        viewModel = viewmodel,
                                        onNavigateBack = { currentTab = TabItem.DASHBOARD },
                                        modifier = contentModifier
                                    )
                                    TabItem.PENGATURAN -> SettingsScreen(
                                        viewModel = viewmodel,
                                        onNavigateBack = { currentTab = TabItem.DASHBOARD },
                                        modifier = contentModifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
