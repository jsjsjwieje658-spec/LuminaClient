/**
 * © Project Lumina 2026 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.data.CustomServer
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.Services
import com.project.lumina.client.ui.component.AddServerDialog
import com.project.lumina.client.ui.component.RealmsSelector
import com.project.lumina.client.ui.component.ServerSelector
import com.project.lumina.client.ui.component.SubServerInfo
import com.project.lumina.client.model.CaptureModeModel
import com.project.lumina.client.util.InjectNeko
import com.project.lumina.client.util.MCPackUtils
import com.project.lumina.client.util.RealmErrorHandler
import com.project.lumina.client.util.ServerInit
import com.project.lumina.client.viewmodel.MainScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.lenni0451.commons.httpclient.HttpClient
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.service.realms.BedrockRealmsService
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession
import net.raphimc.minecraftauth.util.MicrosoftConstants
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.concurrent.CompletableFuture

// ── Consolidated state ────────────────────────────────────────────────

private data class HomeScreenState(
    val selectedView: String = "ServerSelector",
    val showCustomNotification: Boolean = false,
    val customNotificationMessage: String = "",
    val customNotificationType: NotificationType = NotificationType.INFO,
    val isLaunchingMinecraft: Boolean = false,
    val showProgressDialog: Boolean = false,
    val downloadProgress: Float = 0f,
    val currentPackName: String = "",
    val showZeqaBottomSheet: Boolean = false,
    val showAddServerDialog: Boolean = false,
    val editingServer: CustomServer? = null,
    val serverRefreshTrigger: Int = 0,
    val injectNekoPack: Boolean = false,
    val bedrockSession: StepFullBedrockSession.FullBedrockSession? = null,
    val realms: List<RealmsWorld> = emptyList(),
    val isFetchingRealms: Boolean = false,
    val progress: Float = 0f,
    val showBottomSheet: Boolean = false,
    val showConnectionDialog: Boolean = false
)

// ── Main screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartToggle: () -> Unit
) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val httpClient = remember { HttpClient() }

    // Single state holder
    var state by remember { mutableStateOf(HomeScreenState()) }

    // Cache screen layout params (only recompute on config change)
    val configuration = LocalConfiguration.current
    val isCompactScreen = remember(configuration.screenWidthDp) {
        configuration.screenWidthDp.dp < 600.dp
    }
    val leftColumnWidth = if (isCompactScreen) 0.4f else 0.5f
    val localIp = "localhost"

    // SharedPrefs
    val sharedPreferences = remember {
        context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    }

    val showNotification: (String, NotificationType) -> Unit = remember {
        { message, type ->
            SimpleOverlayNotification.show(message = message, type = type, durationMs = 3000)
        }
    }

    // Notification colors - computed directly in composable context
    val notificationContainerColor = remember(state.customNotificationType) {
        when (state.customNotificationType) {
            NotificationType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
            NotificationType.ERROR -> MaterialTheme.colorScheme.errorContainer
            NotificationType.INFO -> MaterialTheme.colorScheme.surfaceContainerHigh
            NotificationType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        }
    }
    val notificationContentColor = remember(state.customNotificationType) {
        when (state.customNotificationType) {
            NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
            NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
            NotificationType.INFO -> MaterialTheme.colorScheme.onSurface
            NotificationType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        }
    }

    // Derived: is account logged in
    val disableAuthRequired = remember {
        sharedPreferences.getBoolean("disableAuthRequiredEnabled", false)
    }
    val isAccountLoggedIn by remember(AccountManager.currentAccount, disableAuthRequired) {
        derivedStateOf { AccountManager.currentAccount != null || disableAuthRequired }
    }

    // Service activation → dismiss bottom sheet
    LaunchedEffect(Services.isActive) {
        if (Services.isActive) {
            delay(600)
            state = state.copy(showBottomSheet = false)
        } else {
            state = state.copy(showBottomSheet = false)
        }
    }

    // Load realms session on mount
    LaunchedEffect(Unit) {
        loadRealmsSession(context, httpClient) { session ->
            state = state.copy(bedrockSession = session)
        }
    }

    // SharedPrefs listener for injectNekoPack
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "injectNekoPackEnabled") {
                state = state.copy(injectNekoPack = prefs.getBoolean("injectNekoPackEnabled", false))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // ── Layout ────────────────────────────────────────────────────────

    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // LEFT PANEL
        LeftPanel(
            state = state,
            isCompactScreen = isCompactScreen,
            leftColumnWidth = leftColumnWidth,
            captureModeModel = captureModeModel,
            showNotification = showNotification,
            onTabSelected = { tab -> state = state.copy(selectedView = tab) },
            onShowZeqa = { state = state.copy(showZeqaBottomSheet = true) },
            onShowAddServer = { state = state.copy(editingServer = null, showAddServerDialog = true) },
            onShowEditServer = { server -> state = state.copy(editingServer = server, showAddServerDialog = true) }
        )

        // DIVIDER
        if (!isCompactScreen) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(0.97f).width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        }

        // RIGHT PANEL
        RightPanel(
            state = state,
            isCompactScreen = isCompactScreen,
            context = context,
            scope = scope,
            captureModeModel = captureModeModel,
            httpClient = httpClient,
            mainScreenViewModel = mainScreenViewModel,
            sharedPreferences = sharedPreferences,
            isAccountLoggedIn = isAccountLoggedIn,
            notificationContainerColor = notificationContainerColor,
            notificationContentColor = notificationContentColor,
            localIp = localIp,
            showNotification = showNotification,
            onStartToggle = onStartToggle,
            onStateUpdate = { state = it }
        )
    }

    // ── Dialogs & Bottom Sheets ───────────────────────────────────────

    if (state.showZeqaBottomSheet) {
        ZeqaSubServerBottomSheet(
            onDismiss = { state = state.copy(showZeqaBottomSheet = false) },
            onSelect = { subServer ->
                mainScreenViewModel.selectCaptureModeModel(
                    captureModeModel.copy(
                        serverHostName = subServer.serverAddress,
                        serverPort = subServer.serverPort
                    )
                )
                state = state.copy(showZeqaBottomSheet = false)
            }
        )
    }

    if (state.showAddServerDialog) {
        AddServerDialog(
            editingServer = state.editingServer,
            onDismiss = { state = state.copy(showAddServerDialog = false, editingServer = null) },
            onSave = { server ->
                val customServerManager = com.project.lumina.client.data.CustomServerManager.getInstance()
                customServerManager.saveServer(server)
                state = state.copy(
                    serverRefreshTrigger = state.serverRefreshTrigger + 1,
                    showAddServerDialog = false,
                    editingServer = null
                )
            }
        )
    }

    if (state.showProgressDialog) {
        DownloadProgressDialog(
            packName = state.currentPackName,
            progress = state.downloadProgress
        )
    }
}

// ── Left Panel ────────────────────────────────────────────────────────

@Composable
private fun LeftPanel(
    state: HomeScreenState,
    isCompactScreen: Boolean,
    leftColumnWidth: Float,
    captureModeModel: CaptureModeModel,
    showNotification: (String, NotificationType) -> Unit,
    onTabSelected: (String) -> Unit,
    onShowZeqa: () -> Unit,
    onShowAddServer: () -> Unit,
    onShowEditServer: (CustomServer) -> Unit
) {
    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(leftColumnWidth)
            .padding(
                start = if (isCompactScreen) 12.dp else 24.dp,
                end = if (isCompactScreen) 8.dp else 24.dp,
                top = if (isCompactScreen) 16.dp else 24.dp,
                bottom = if (isCompactScreen) 16.dp else 24.dp
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Tab bar
        TabBar(
            selectedView = state.selectedView,
            isCompactScreen = isCompactScreen,
            onTabSelected = onTabSelected
        )

        // Tab content (instant switch, no animation)
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            when (state.selectedView) {
                "ServerSelector" -> ServerSelector(
                    onShowZeqaBottomSheet = onShowZeqa,
                    onShowAddServerDialog = onShowAddServer,
                    onShowEditServerDialog = onShowEditServer,
                    refreshTrigger = state.serverRefreshTrigger
                )
                "View2" -> AccountScreen(showNotification)
                "View3" -> PacksScreen()
                "View4" -> RealmsSelector()
            }
        }
    }
}

// ── Tab Bar ───────────────────────────────────────────────────────────

@Composable
private fun TabBar(
    selectedView: String,
    isCompactScreen: Boolean,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("ServerSelector", "View2", "View3", "View4")
    val tabNames = listOf(R.string.servers, R.string.accounts, R.string.packs, R.string.realms)

    if (isCompactScreen) {
        // Vertical tabs
        Column(
            Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            tabs.forEachIndexed { index, tab ->
                FilterChip(
                    selected = selectedView == tab,
                    onClick = { onTabSelected(tab) },
                    label = {
                        Text(
                            stringResource(tabNames[index]),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    interactionSource = remember { MutableInteractionSource() }
                )
            }
        }
    } else {
        // Horizontal tabs
        Row(
            Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top
        ) {
            tabs.forEachIndexed { index, tab ->
                Box(modifier = Modifier.weight(1f)) {
                    FilterChip(
                        selected = selectedView == tab,
                        onClick = { onTabSelected(tab) },
                        label = {
                            Text(
                                stringResource(tabNames[index]),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                }
            }
        }
    }
}

// ── Right Panel ───────────────────────────────────────────────────────

@Composable
private fun RightPanel(
    state: HomeScreenState,
    isCompactScreen: Boolean,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    captureModeModel: CaptureModeModel,
    httpClient: HttpClient,
    mainScreenViewModel: MainScreenViewModel,
    sharedPreferences: SharedPreferences,
    isAccountLoggedIn: Boolean,
    notificationContainerColor: androidx.compose.ui.graphics.Color,
    notificationContentColor: androidx.compose.ui.graphics.Color,
    localIp: String,
    showNotification: (String, NotificationType) -> Unit,
    onStartToggle: () -> Unit,
    onStateUpdate: (HomeScreenState) -> Unit
) {
    Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(
                start = if (isCompactScreen) 8.dp else 16.dp,
                end = if (isCompactScreen) 12.dp else 16.dp,
                top = if (isCompactScreen) 16.dp else 16.dp,
                bottom = if (isCompactScreen) 16.dp else 16.dp
            )
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: account banner + notification + selected server
            Column(
                Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.End
            ) {
                // Account banner (instant, no animation)
                AccountBanner(
                    isCompactScreen = isCompactScreen
                )

                Spacer(modifier = Modifier.height(if (isCompactScreen) 8.dp else 16.dp))

                // Custom notification (instant, no animation)
                if (state.showCustomNotification) {
                    Card(
                        modifier = Modifier
                            .width(if (isCompactScreen) 240.dp else 320.dp)
                            .wrapContentHeight(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = notificationContainerColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompactScreen) 12.dp else 16.dp,
                                    vertical = if (isCompactScreen) 8.dp else 12.dp
                                ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.customNotificationMessage,
                                style = if (isCompactScreen) MaterialTheme.typography.bodySmall
                                else MaterialTheme.typography.bodyMedium,
                                color = notificationContentColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(if (isCompactScreen) 8.dp else 16.dp))
                }

                // Selected server card (instant, no animation)
                if (captureModeModel.serverHostName.isNotBlank()) {
                    SelectedServerCard(
                        captureModeModel = captureModeModel,
                        isCompactScreen = isCompactScreen
                    )
                }
            }

            // SPACER to push button to bottom
            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM: Start/Stop button (instant, no animation)
            StartStopButton(
                isActive = Services.isActive,
                isCompactScreen = isCompactScreen,
                isAccountLoggedIn = isAccountLoggedIn,
                isLaunchingMinecraft = state.isLaunchingMinecraft,
                context = context,
                scope = scope,
                mainScreenViewModel = mainScreenViewModel,
                sharedPreferences = sharedPreferences,
                httpClient = httpClient,
                localIp = localIp,
                showNotification = showNotification,
                onStartToggle = onStartToggle,
                onStateUpdate = onStateUpdate,
                currentState = state
            )
        }
    }
}

// ── Account Banner ────────────────────────────────────────────────────

@Composable
private fun AccountBanner(isCompactScreen: Boolean) {
    val accountRemark = AccountManager.currentAccount?.remark

    if (accountRemark != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (isCompactScreen) 0.2f else 0.25f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isCompactScreen) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(2.dp).size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Hello!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = accountRemark,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp).size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hello!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                modifier = Modifier.padding(start = 4.dp),
                                text = accountRemark,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Thin,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Selected Server Card ──────────────────────────────────────────────

@Composable
private fun SelectedServerCard(
    captureModeModel: CaptureModeModel,
    isCompactScreen: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isCompactScreen) 70.dp else 90.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(if (isCompactScreen) 12.dp else 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 4.dp else 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompactScreen) 4.dp else 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(if (isCompactScreen) 16.dp else 20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.selected_server),
                    style = if (isCompactScreen) MaterialTheme.typography.bodyLarge
                    else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = captureModeModel.serverHostName,
                style = if (isCompactScreen) MaterialTheme.typography.bodyLarge
                else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.port, captureModeModel.serverPort),
                style = if (isCompactScreen) MaterialTheme.typography.bodySmall
                else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Start / Stop Button ───────────────────────────────────────────────

@Composable
private fun StartStopButton(
    isActive: Boolean,
    isCompactScreen: Boolean,
    isAccountLoggedIn: Boolean,
    isLaunchingMinecraft: Boolean,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    mainScreenViewModel: MainScreenViewModel,
    sharedPreferences: SharedPreferences,
    httpClient: HttpClient,
    localIp: String,
    showNotification: (String, NotificationType) -> Unit,
    onStartToggle: () -> Unit,
    onStateUpdate: (HomeScreenState) -> Unit,
    currentState: HomeScreenState
) {
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = if (isCompactScreen) 12.dp else 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (isActive) {
            // STOP button
            Button(
                onClick = {
                    onStateUpdate(currentState.copy(isLaunchingMinecraft = false))
                    onStartToggle()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompactScreen) 48.dp else 56.dp),
                shape = RoundedCornerShape(if (isCompactScreen) 12.dp else 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "Stop",
                        modifier = Modifier.size(if (isCompactScreen) 20.dp else 24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.stop),
                        style = if (isCompactScreen) MaterialTheme.typography.bodyLarge
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            // START / LOCKED button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompactScreen) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompactScreen) 48.dp else 56.dp),
                    onClick = {
                        val disableAuthRequired = sharedPreferences.getBoolean("disableAuthRequiredEnabled", false)
                        if (AccountManager.currentAccount == null && !disableAuthRequired) {
                            Toast.makeText(
                                context,
                                "Please login and select an account first to start the service",
                                Toast.LENGTH_LONG
                            ).show()
                            return@ExtendedFloatingActionButton
                        }

                        scope.launch {
                            delay(100)
                            onStateUpdate(currentState.copy(isLaunchingMinecraft = true))
                            Services.isLaunchingMinecraft = true
                            onStartToggle()

                            delay(2500)
                            if (!Services.isActive) {
                                onStateUpdate(currentState.copy(isLaunchingMinecraft = false))
                                Services.isLaunchingMinecraft = false
                                return@launch
                            }

                            val disableAutoStartEnabled = sharedPreferences.getBoolean("disableAutoStartEnabled", false)
                            if (!disableAutoStartEnabled) {
                                val selectedGame = mainScreenViewModel.selectedGame.value
                                if (selectedGame != null) {
                                    val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                                    if (intent != null && Services.isActive) {
                                        context.startActivity(intent)

                                        delay(3000)
                                        onStateUpdate(currentState.copy(isLaunchingMinecraft = false))
                                        Services.isLaunchingMinecraft = false

                                        try {
                                            val injectNekoPack = currentState.injectNekoPack
                                            val selectedPack = PackSelectionManager.selectedPack
                                            when {
                                                injectNekoPack && selectedPack != null -> {
                                                    val updatedState = currentState.copy(
                                                        currentPackName = selectedPack.name,
                                                        showProgressDialog = true,
                                                        downloadProgress = 0f
                                                    )
                                                    onStateUpdate(updatedState)

                                                    try {
                                                        MCPackUtils.downloadAndOpenPack(context, selectedPack) { progress ->
                                                            onStateUpdate(updatedState.copy(downloadProgress = progress))
                                                        }
                                                        onStateUpdate(updatedState.copy(showProgressDialog = false))
                                                    } catch (e: Exception) {
                                                        onStateUpdate(updatedState.copy(showProgressDialog = false))
                                                        showNotification(
                                                            "Failed to download pack: ${e.message}",
                                                            NotificationType.ERROR
                                                        )
                                                    }
                                                }

                                                injectNekoPack -> {
                                                    try {
                                                        InjectNeko.injectNeko(context = context) { p ->
                                                            onStateUpdate(currentState.copy(progress = p))
                                                        }
                                                    } catch (e: Exception) {
                                                        showNotification(
                                                            "Failed to inject Neko: ${e.message}",
                                                            NotificationType.ERROR
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    if (selectedGame == "com.mojang.minecraftpe") {
                                                        try {
                                                            ServerInit.addMinecraftServer(context, localIp)
                                                        } catch (e: Exception) {
                                                            showNotification(
                                                                "Failed to initialize server: ${e.message}",
                                                                NotificationType.ERROR
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            showNotification(
                                                "An unexpected error occurred: ${e.message}",
                                                NotificationType.ERROR
                                            )
                                        }
                                    } else {
                                        showNotification("Failed to launch game", NotificationType.ERROR)
                                    }
                                }
                            } else {
                                onStateUpdate(currentState.copy(isLaunchingMinecraft = false))
                                Services.isLaunchingMinecraft = false
                            }
                        }
                    },
                    containerColor = if (isAccountLoggedIn)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    contentColor = if (isAccountLoggedIn)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(if (isCompactScreen) 12.dp else 16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    icon = {
                        if (isAccountLoggedIn) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(if (isCompactScreen) 20.dp else 24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(if (isCompactScreen) 18.dp else 22.dp)
                            )
                        }
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.start),
                            style = if (isCompactScreen) MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }
    }
}

// ── Download Progress Dialog ──────────────────────────────────────────

@Composable
private fun DownloadProgressDialog(packName: String, progress: Float) {
    Dialog(onDismissRequest = { /* Prevent dismissal during download */ }) {
        Card(
            modifier = Modifier.padding(16.dp).wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Downloading: $packName",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(48.dp),
                    trackColor = ProgressIndicatorDefaults.circularTrackColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (progress < 1f) "Downloading..." else "Launching Minecraft...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ── Zeqa Sub-Server Bottom Sheet ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeqaSubServerBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (SubServerInfo) -> Unit
) {
    val subServers = listOf(
        SubServerInfo("NA1", "North America", "na.zeqa.net", 10001),
        SubServerInfo("NA2", "North America", "na.zeqa.net", 10002),
        SubServerInfo("NA3", "North America", "na.zeqa.net", 10003),
        SubServerInfo("NA4", "North America", "na.zeqa.net", 10004),
        SubServerInfo("NA5", "North America", "na.zeqa.net", 10005),
        SubServerInfo("NA6", "North America", "na.zeqa.net", 10006),
        SubServerInfo("EU1", "Europe", "eu.zeqa.net", 10001),
        SubServerInfo("EU2", "Europe", "eu.zeqa.net", 10002),
        SubServerInfo("EU3", "Europe", "eu.zeqa.net", 10003),
        SubServerInfo("EU4", "Europe", "eu.zeqa.net", 10004),
        SubServerInfo("EU5", "Europe", "eu.zeqa.net", 10005),
        SubServerInfo("EU6", "Europe", "eu.zeqa.net", 10006),
        SubServerInfo("AS1", "Asia", "as.zeqa.net", 10001),
        SubServerInfo("AS2", "Asia", "as.zeqa.net", 10002),
        SubServerInfo("AS3", "Asia", "as.zeqa.net", 10003),
        SubServerInfo("AS4", "Asia", "as.zeqa.net", 10004),
        SubServerInfo("AS5", "Asia", "as.zeqa.net", 10005),
        SubServerInfo("AS6", "Asia", "as.zeqa.net", 10006),
        SubServerInfo("SA1", "South America", "sa.zeqa.net", 10001),
        SubServerInfo("AU1", "Australia", "au.zeqa.net", 10001),
        SubServerInfo("ZA1", "South Africa", "za.zeqa.net", 10001)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Zeqa Sub-Server",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "Choose a sub-server based on your region",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subServers) { subServer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onSelect(subServer) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = subServer.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subServer.region,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = subServer.serverAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Port: ${subServer.serverPort}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Realms helpers (unchanged) ────────────────────────────────────────

private const val REALMS_TAG = "RealmsView"
private const val REALMS_SESSION_FILE = "bedrock_session.json"
private const val REALMS_BEDROCK_CLIENT_VERSION = "1.21.100"

private val REALMS_BEDROCK_AUTH_FLOW = MinecraftAuth.builder()
    .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID)
    .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
    .deviceCode()
    .withDeviceToken("Android")
    .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
    .buildMinecraftBedrockChainStep(true, true)

@Composable
fun RealmsView(
    bedrockSession: StepFullBedrockSession.FullBedrockSession?,
    realms: List<RealmsWorld>,
    httpClient: HttpClient,
    isFetchingRealms: Boolean,
    onFetchRealms: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompactScreen = remember(configuration.screenWidthDp) {
        configuration.screenWidthDp.dp < 600.dp
    }
    var realmAddresses by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var statusMessage by remember { mutableStateOf("") }

    val isLoggedIn = bedrockSession != null && bedrockSession.realmsXsts != null

    if (isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompactScreen) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Realms",
                    style = if (isCompactScreen) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onFetchRealms,
                    enabled = !isFetchingRealms,
                    modifier = Modifier.wrapContentSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(if (isCompactScreen) 8.dp else 12.dp)
                ) {
                    if (isFetchingRealms) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(if (isCompactScreen) 14.dp else 16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isFetchingRealms) "Fetching..." else "Fetch Realms",
                        style = if (isCompactScreen) MaterialTheme.typography.bodySmall
                        else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (realms.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "No Realms Found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Use the Fetch Realms button above to load your realms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(realms) { world ->
                        RealmCard(
                            world = world,
                            realmAddress = realmAddresses[world.id.toInt()],
                            onJoinRealm = {
                                statusMessage = "Joining Realm: ${world.name}..."
                                joinRealm(httpClient, bedrockSession, world) { address, error ->
                                    if (error != null) {
                                        statusMessage = "Error joining Realm: $error"
                                    } else if (address != null) {
                                        realmAddresses = realmAddresses + (world.id.toInt() to address)
                                        statusMessage = "Joined Realm: ${world.name}"
                                    }
                                }
                            },
                            enabled = world.isCompatible && !world.isExpired && bedrockSession?.realmsXsts != null
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Login Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Please login to your Microsoft account in the main Realms section to view your realms here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun RealmCard(
    world: RealmsWorld,
    realmAddress: String?,
    onJoinRealm: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                world.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Owner: ${world.ownerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                "Max Players: ${world.maxPlayers}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            realmAddress?.let { address ->
                Text(
                    "Address: $address",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = onJoinRealm,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join Realm")
            }
        }
    }
}

// ── Private helpers (unchanged business logic) ────────────────────────

private fun loadRealmsSession(
    context: Context,
    httpClient: HttpClient,
    callback: (StepFullBedrockSession.FullBedrockSession?) -> Unit
) {
    CompletableFuture.supplyAsync {
        try {
            val file = File(context.filesDir, REALMS_SESSION_FILE)
            if (!file.exists()) {
                Log.d(REALMS_TAG, "Session file does not exist")
                return@supplyAsync null
            }

            val jsonString = file.readText()
            if (jsonString.isBlank()) {
                Log.e(REALMS_TAG, "Session file is empty")
                return@supplyAsync null
            }

            val json = JsonParser.parseString(jsonString) as JsonObject
            val session = REALMS_BEDROCK_AUTH_FLOW.fromJson(json)
            Log.d(REALMS_TAG, "Session loaded successfully")

            if (session.realmsXsts == null) {
                Log.e(REALMS_TAG, "Session missing realmsXsts token")
                return@supplyAsync null
            }

            session
        } catch (e: Exception) {
            Log.e(REALMS_TAG, "Error loading session", e)
            null
        }
    }.thenAccept { session ->
        callback(session)
    }
}

private fun fetchRealms(
    httpClient: HttpClient,
    session: StepFullBedrockSession.FullBedrockSession?,
    callback: (List<RealmsWorld>?, String?) -> Unit
) {
    Log.d(REALMS_TAG, "Fetching Realms")

    if (session?.realmsXsts == null) {
        callback(null, "No authentication session available")
        return
    }

    try {
        val realmsService = BedrockRealmsService(httpClient, REALMS_BEDROCK_CLIENT_VERSION, session.realmsXsts)

        realmsService.getWorlds()
            .thenAccept { worlds ->
                Log.d(REALMS_TAG, "Successfully fetched ${worlds.size} Realms")
                callback(worlds, null)
            }
            .exceptionally { throwable ->
                Log.e(REALMS_TAG, "Error fetching worlds", throwable)
                callback(null, RealmErrorHandler.translateFetchError(throwable))
                null
            }
    } catch (e: Exception) {
        Log.e(REALMS_TAG, "Exception creating BedrockRealmsService", e)
        callback(null, RealmErrorHandler.translateFetchError(e))
    }
}

private fun joinRealm(
    httpClient: HttpClient,
    session: StepFullBedrockSession.FullBedrockSession?,
    world: RealmsWorld,
    callback: (String?, String?) -> Unit
) {
    Log.d(REALMS_TAG, "Joining Realm: ${world.name}")

    if (session?.realmsXsts == null) {
        callback(null, RealmErrorHandler.translateJoinError(RuntimeException("No authentication session available")))
        return
    }

    try {
        val realmsService = BedrockRealmsService(httpClient, REALMS_BEDROCK_CLIENT_VERSION, session.realmsXsts)

        realmsService.joinWorld(world)
            .thenApply { address -> address.toString() }
            .thenAccept { addressString ->
                Log.d(REALMS_TAG, "Successfully joined Realm ${world.name}, address: $addressString")

                if (isNethernetAddress(addressString)) {
                    Log.w(REALMS_TAG, "Realm ${world.name} uses Nethernet protocol (address: $addressString)")
                    callback(null, "Nethernet Realms are not supported yet")
                    return@thenAccept
                }

                callback(addressString, null)
            }
            .exceptionally { throwable ->
                Log.e(REALMS_TAG, "Error joining Realm ${world.name}", throwable)
                callback(null, RealmErrorHandler.translateJoinError(throwable))
                null
            }
    } catch (e: Exception) {
        Log.e(REALMS_TAG, "Exception creating BedrockRealmsService", e)
        callback(null, RealmErrorHandler.translateJoinError(e))
    }
}

private fun isNethernetAddress(address: String): Boolean {
    val uuidPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
    return uuidPattern.matches(address)
}
