package com.example.esp32zero.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.esp32zero.ui.navigation.AppDestination

/**
 * Uygulamanın gezinme iskeleti: tek bir BleViewModel örneği burada
 * oluşturulup NavHost boyunca tüm sayfalara aynı örnek geçiliyor (böylece
 * sayfalar arası geçişte bağlantı/yanıt durumu kaybolmuyor). Her özellik
 * artık ayrı bir sayfa — eskiden hepsi tek bir uzun, kaydırılamayan
 * Column'da olduğu için yeni özellikler eklendikçe alttaki tarama
 * sonuçları ekran dışına taşıp görünmez oluyordu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: BleViewModel = viewModel(factory = bleViewModelFactory(context))
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = AppDestination.entries.find { it.route == backStackEntry?.destination?.route }
        ?: AppDestination.HOME

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(currentDestination.title) },
                navigationIcon = {
                    if (currentDestination != AppDestination.HOME) {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("← Geri")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.HOME.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigate = { destination -> navController.navigate(destination.route) },
                )
            }
            composable(AppDestination.WIFI_SCAN.route) { WifiScanScreen(viewModel) }
            composable(AppDestination.BLE_SCAN.route) { BleScanScreen(viewModel) }
            composable(AppDestination.SUB_GHZ.route) { SubGhzScreen(viewModel) }
            composable(AppDestination.WIFI_CAPTURE.route) { WifiCaptureScreen(viewModel) }
            composable(AppDestination.DEAUTH.route) { DeauthScreen(viewModel) }
            composable(AppDestination.OLED_TEXT.route) { OledTextScreen(viewModel) }
            composable(AppDestination.RESPONSE_LOG.route) { ResponseLogScreen(viewModel) }
        }
    }
}
