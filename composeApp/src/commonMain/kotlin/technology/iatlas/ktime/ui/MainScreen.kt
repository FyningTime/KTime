package technology.iatlas.ktime.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import technology.iatlas.ktime.ui.migration.MigrationTab
import technology.iatlas.ktime.ui.settings.SettingsTab
import technology.iatlas.ktime.ui.vacation.VacationTab
import technology.iatlas.ktime.ui.worktime.WorkTimeTab

/**
 * Main screen of the application with tab navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(database: org.jetbrains.exposed.sql.Database) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KTime - Work Time Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Work Time") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Vacation") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Settings") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Migrate") }
                )
            }

            when (selectedTab) {
                0 -> WorkTimeTab(database)
                1 -> VacationTab(database)
                2 -> SettingsTab(database)
                3 -> MigrationTab(database)
                else -> WorkTimeTab(database) // Default to work time tab
            }
        }
    }
}
