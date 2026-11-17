package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.TacticalVoiceCommsManager
import com.example.ai.VoiceCommsState
import com.example.ai.configureAppCheck
import com.example.data.repository.GameRepository
import com.example.data.repository.GuestSessionManager
import com.example.data.repository.LocalGuestRepository
import com.example.ui.arena.BattleArenaScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOut
import com.example.ui.components.AppSignatureInfo
import com.example.ui.components.DevWatermarkOverlay
import com.example.ui.components.DevWatermarkPrefs
import com.example.ui.components.SpinRewardDialog
import com.example.ui.components.SquadVoiceDialog
import com.example.ui.components.TopCurrencyBar
import com.example.ui.components.readAppSignatureInfo
import com.example.ui.screens.HudAndSensitivityScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.LuckRoyaleScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlitzFireTheme
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class MainTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LOBBY("lobby", "Lobby", Icons.Filled.Home, Icons.Outlined.Home),
    STORE("store", "Store", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    LUCK_ROYALE("luck_royale", "Royale", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    VAULT("vault", "Vault", Icons.Filled.Backpack, Icons.Outlined.Backpack)
}

sealed interface AppDestination {
    data class Tab(val tab: MainTab) : AppDestination
    data object Profile : AppDestination
    data object BattleArena : AppDestination
    data object HudSettings : AppDestination
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            BlitzFireTheme {
                BlitzFireAppRoot()
            }
        }
    }
}

@Composable
fun BlitzFireAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by remember { mutableStateOf(GuestSessionManager.isGuestActive(context)) }
    val signatureInfo = remember(context) { readAppSignatureInfo(context) }
    var watermarkEnabled by remember { mutableStateOf(DevWatermarkPrefs.isEnabled(context)) }
    var devWatermarkLabel by remember { mutableStateOf(DevWatermarkPrefs.getLabel(context)) }
    var isFullBleedDestination by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && isGuestMode) {
            GuestSessionManager.setGuestActive(context, false)
            isGuestMode = false
        }
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null && !isGuestMode) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        when {
            user != null -> {
                key(user.uid) {
                    val viewModel: GameViewModel = viewModel(
                        key = user.uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                GameViewModel(
                                    repository = GameRepository(db, auth),
                                    currentUserId = user.uid,
                                    initialDisplayName = user.displayName
                                )
                            }
                        }
                    )

                    AuthenticatedGameShell(
                        viewModel = viewModel,
                        isGuestSession = false,
                        signatureInfo = signatureInfo,
                        devWatermarkLabel = devWatermarkLabel,
                        watermarkEnabled = watermarkEnabled,
                        onToggleWatermark = { enabled ->
                            DevWatermarkPrefs.setEnabled(context, enabled)
                            watermarkEnabled = enabled
                        },
                        onUpdateDevWatermarkLabel = { label ->
                            DevWatermarkPrefs.setLabel(context, label)
                            devWatermarkLabel = DevWatermarkPrefs.getLabel(context)
                        },
                        onLinkGoogleAccount = {},
                        onDestinationFullBleedChanged = { isFullBleedDestination = it },
                        onSignOut = {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }

            isGuestMode -> {
                key(LocalGuestRepository.GUEST_UID) {
                    val viewModel: GameViewModel = viewModel(
                        key = LocalGuestRepository.GUEST_UID,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                GameViewModel(
                                    repository = LocalGuestRepository(app),
                                    currentUserId = LocalGuestRepository.GUEST_UID,
                                    initialDisplayName = "Guest_Blaze"
                                )
                            }
                        }
                    )

                    AuthenticatedGameShell(
                        viewModel = viewModel,
                        isGuestSession = true,
                        signatureInfo = signatureInfo,
                        devWatermarkLabel = devWatermarkLabel,
                        watermarkEnabled = watermarkEnabled,
                        onToggleWatermark = { enabled ->
                            DevWatermarkPrefs.setEnabled(context, enabled)
                            watermarkEnabled = enabled
                        },
                        onUpdateDevWatermarkLabel = { label ->
                            DevWatermarkPrefs.setLabel(context, label)
                            devWatermarkLabel = DevWatermarkPrefs.getLabel(context)
                        },
                        onLinkGoogleAccount = {
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    GuestSessionManager.setGuestActive(context, false)
                                    isGuestMode = false
                                },
                                onAuthError = { _ -> },
                                scope = scope
                            )
                        },
                        onDestinationFullBleedChanged = { isFullBleedDestination = it },
                        onSignOut = {
                            GuestSessionManager.setGuestActive(context, false)
                            isGuestMode = false
                        }
                    )
                }
            }

            else -> {
                AuthScreen(
                    onAuthSuccess = {
                        GuestSessionManager.setGuestActive(context, false)
                        isGuestMode = false
                    },
                    onPlayAsGuest = {
                        GuestSessionManager.setGuestActive(context, true)
                        isGuestMode = true
                    },
                    signatureInfo = signatureInfo,
                    devWatermarkLabel = devWatermarkLabel
                )
            }
        }

        if (watermarkEnabled && (user != null || isGuestMode)) {
            DevWatermarkOverlay(
                signatureInfo = signatureInfo,
                devLabel = devWatermarkLabel,
                modifier = if (isFullBleedDestination) {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 4.dp)
                } else {
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedGameShell(
    viewModel: GameViewModel,
    isGuestSession: Boolean,
    signatureInfo: AppSignatureInfo,
    devWatermarkLabel: String,
    watermarkEnabled: Boolean,
    onToggleWatermark: (Boolean) -> Unit,
    onUpdateDevWatermarkLabel: (String) -> Unit,
    onLinkGoogleAccount: () -> Unit,
    onDestinationFullBleedChanged: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryMap.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val royaleHistory by viewModel.royaleHistory.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var destination by remember { mutableStateOf<AppDestination>(AppDestination.Tab(MainTab.LOBBY)) }
    val snackbarHostState = remember { SnackbarHostState() }

    val voiceCommsManager = remember { TacticalVoiceCommsManager() }
    var showVoiceDialog by remember { mutableStateOf(false) }
    val voiceState by voiceCommsManager.state.collectAsStateWithLifecycle()

    DisposableEffect(voiceCommsManager) {
        onDispose { voiceCommsManager.release() }
    }

    LaunchedEffect(destination) {
        onDestinationFullBleedChanged(
            destination == AppDestination.BattleArena || destination == AppDestination.HudSettings
        )
    }

    LaunchedEffect(actionState.statusMessage) {
        val msg = actionState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = BlazeOrange)
                    Text(
                        text = "DEPLOYING DROPZONE VAULT...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = pState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Button(onClick = onSignOut) {
                        Text("SIGN OUT & RETRY")
                    }
                }
            }
        }

        is UiState.Success -> {
            val profile = pState.data

            if (destination == AppDestination.BattleArena) {
                BattleArenaScreen(
                    mode = actionState.selectedMode,
                    mapId = actionState.selectedMap,
                    profile = profile,
                    inventory = inventory,
                    commsManager = voiceCommsManager,
                    onOpenVoiceComms = { showVoiceDialog = true },
                    savedMatchRecord = actionState.lastMatchRecord,
                    onMatchEnded = { outcome ->
                        viewModel.recordMatchResult(
                            mode = actionState.selectedMode,
                            placement = outcome.placement,
                            kills = outcome.kills,
                            headshots = outcome.headshots,
                            damageDealt = outcome.damageDealt,
                            survivalSeconds = outcome.survivalSeconds
                        )
                    },
                    onExitToLobby = {
                        viewModel.dismissMatchSummary()
                        destination = AppDestination.Tab(MainTab.LOBBY)
                    }
                )
            } else if (destination == AppDestination.HudSettings) {
                HudAndSensitivityScreen(
                    onClose = { destination = AppDestination.Tab(MainTab.LOBBY) }
                )
            } else {
                // BGMI Landscape NavigationRail + Main Content Scaffold
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg)
                ) {
                    if (destination is AppDestination.Tab) {
                        val currentTab = (destination as AppDestination.Tab).tab
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .displayCutoutPadding(),
                            header = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { showVoiceDialog = true },
                                        modifier = Modifier.testTag("nav_rail_voice_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Squad Voice Radio",
                                            tint = if (voiceState is VoiceCommsState.Live) TacticalGreen else DiamondCyan
                                        )
                                    }
                                    IconButton(
                                        onClick = { destination = AppDestination.HudSettings },
                                        modifier = Modifier.testTag("nav_rail_hud_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Custom HUD & Sensitivity",
                                            tint = BlazeOrange
                                        )
                                    }
                                }
                            }
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { destination = AppDestination.Tab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = BlazeOrange,
                                        selectedTextColor = BlazeOrange,
                                        indicatorColor = ObsidianElevated,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = ObsidianBg,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            TopCurrencyBar(
                                profile = profile,
                                onRefillClicked = { viewModel.refillUnlimitedCurrency() },
                                onProfileClicked = { destination = AppDestination.Profile },
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .displayCutoutPadding()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val dest = destination) {
                                is AppDestination.Tab -> when (dest.tab) {
                                    MainTab.LOBBY -> LobbyScreen(
                                        profile = profile,
                                        selectedMode = actionState.selectedMode,
                                        selectedMap = actionState.selectedMap,
                                        matchHistory = matchHistory,
                                        commsManager = voiceCommsManager,
                                        onSelectMode = viewModel::selectMode,
                                        onSelectMap = viewModel::selectMap,
                                        onStartMatch = {
                                            viewModel.dismissMatchSummary()
                                            destination = AppDestination.BattleArena
                                        },
                                        onOpenVoiceComms = { showVoiceDialog = true },
                                        onNavigateToRoyale = { destination = AppDestination.Tab(MainTab.LUCK_ROYALE) },
                                        onNavigateToStore = { destination = AppDestination.Tab(MainTab.STORE) },
                                        onNavigateToVault = { destination = AppDestination.Tab(MainTab.VAULT) },
                                        onOpenHudSettings = { destination = AppDestination.HudSettings },
                                        onRefillCurrency = viewModel::refillUnlimitedCurrency
                                    )

                                    MainTab.STORE -> StoreScreen(
                                        profile = profile,
                                        inventory = inventory,
                                        selectedCategory = actionState.selectedStoreCategory,
                                        searchQuery = actionState.storeSearchQuery,
                                        isBusy = actionState.isBusy,
                                        onSelectCategory = viewModel::selectStoreCategory,
                                        onSearchChange = viewModel::updateStoreSearch,
                                        onPurchaseItem = viewModel::purchaseItem,
                                        onEquipItem = viewModel::equipItem
                                    )

                                    MainTab.LUCK_ROYALE -> LuckRoyaleScreen(
                                        profile = profile,
                                        inventory = inventory,
                                        selectedBanner = actionState.selectedRoyaleBanner,
                                        isSpinning = actionState.isSpinning,
                                        royaleHistory = royaleHistory,
                                        onSelectBanner = viewModel::selectRoyaleBanner,
                                        onSpinSingle = { viewModel.spinLuckRoyale(isMulti = false) },
                                        onSpinMulti = { viewModel.spinLuckRoyale(isMulti = true) }
                                    )

                                    MainTab.VAULT -> VaultScreen(
                                        profile = profile,
                                        inventory = inventory,
                                        onEquipItem = viewModel::equipItem,
                                        onNavigateToStore = { destination = AppDestination.Tab(MainTab.STORE) }
                                    )
                                }

                                AppDestination.Profile -> ProfileScreen(
                                    profile = profile,
                                    matchHistory = matchHistory,
                                    isGuestSession = isGuestSession,
                                    signatureInfo = signatureInfo,
                                    devWatermarkLabel = devWatermarkLabel,
                                    watermarkEnabled = watermarkEnabled,
                                    onToggleWatermark = onToggleWatermark,
                                    onUpdateDevWatermarkLabel = onUpdateDevWatermarkLabel,
                                    onLinkGoogleAccount = onLinkGoogleAccount,
                                    onBack = { destination = AppDestination.Tab(MainTab.LOBBY) },
                                    onUpdateCallsign = viewModel::updateCallsign,
                                    onOpenHudSettings = { destination = AppDestination.HudSettings },
                                    onRefillCurrency = viewModel::refillUnlimitedCurrency,
                                    onSignOut = onSignOut
                                )

                                AppDestination.BattleArena, AppDestination.HudSettings -> Unit
                            }
                        }
                    }
                }
            }

            if (showVoiceDialog) {
                SquadVoiceDialog(
                    commsManager = voiceCommsManager,
                    mapTitle = actionState.selectedMap.title,
                    modeTitle = if (actionState.selectedMode == "battle_royale") "Classic BR 3D" else "Arena Blitz 3D",
                    onDismiss = { showVoiceDialog = false }
                )
            }

            val spinOutcome = actionState.lastSpinOutcome
            if (spinOutcome != null) {
                SpinRewardDialog(
                    outcome = spinOutcome,
                    onEquipItem = viewModel::equipItem,
                    onDismiss = viewModel::dismissSpinOutcome
                )
            }
        }
    }
}
