package com.example.nothingpodcast.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.nothingpodcast.R
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontWeight
import com.example.nothingpodcast.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource

private enum class SettingsMenu { MAIN, ADVANCED, PERMISSIONS, OPML, ABOUT, CLOUD, LOGS, ACCOUNT }

@Composable
fun SettingsScreen(
    onBack:    () -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authViewModel = hiltViewModel<com.example.nothingpodcast.ui.auth.AuthViewModel>()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    var currentMenu by remember { mutableStateOf(SettingsMenu.MAIN) }

    LaunchedEffect(uiState.loggingEnabled) {
        com.example.nothingpodcast.util.AppLogger.isEnabled = uiState.loggingEnabled
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val opmlData = viewModel.getOpmlData()
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(opmlData.toByteArray())
                    }
                    snackbarHostState.showSnackbar("Esportazione completata con successo")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Errore durante l'esportazione")
                }
            }
        }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.let { input ->
                    viewModel.importOpml(input) { count ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Importazione completata: $count podcast aggiunti")
                        }
                    }
                }
            } catch (_: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Errore nell'apertura del file")
                }
            }
        }
    }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                scope.launch { 
                    snackbarHostState.showSnackbar("Accesso Google effettuato")
                    viewModel.refreshCloudSummary(account)
                    // Se siamo nel menu Cloud, forziamo il refresh dello stato (re-composing)
                    if (currentMenu == SettingsMenu.CLOUD) {
                        currentMenu = SettingsMenu.MAIN
                        currentMenu = SettingsMenu.CLOUD
                    }
                }
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            scope.launch { snackbarHostState.showSnackbar("Errore accesso Google: ${e.message}") }
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NothingSurfaceHigh,
                    contentColor = NothingWhite,
                    actionColor = NothingWhite,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        },
        containerColor = NothingBlack
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentMenu) {
                SettingsMenu.ADVANCED -> {
                    AdvancedSettingsMenu(
                        onBack = { currentMenu = SettingsMenu.MAIN },
                        onNavigateToLogs = { currentMenu = SettingsMenu.LOGS }
                    )
                }
                SettingsMenu.LOGS -> {
                    LogsSettingsMenu(
                        uiState         = uiState,
                        onToggleLogging = viewModel::setLoggingEnabled,
                        onOpenLog       = {
                            val file = com.example.nothingpodcast.util.AppLogger.getLogFile(context)
                            if (file.exists() && file.length() > 0) {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "text/plain")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Apri log"))
                                } catch (_: Exception) {
                                    scope.launch { snackbarHostState.showSnackbar("Errore nell'apertura del log") }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Il file di log è vuoto") }
                            }
                        },
                        onBack = { currentMenu = SettingsMenu.ADVANCED }
                    )
                }
                SettingsMenu.PERMISSIONS -> {
                    PermissionsMenu(
                        onBack = { currentMenu = SettingsMenu.MAIN }
                    )
                }
                SettingsMenu.OPML -> {
                    OpmlSettingsMenu(
                        uiState  = uiState,
                        onExport = { exportLauncher.launch("subscriptions.opml") },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                        onClearError = { viewModel.clearImportError() },
                        onBack   = { currentMenu = SettingsMenu.MAIN }
                    )
                }
                SettingsMenu.CLOUD -> {
                    var account by remember { mutableStateOf(com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)) }
                    CloudSyncMenu(
                        account = account,
                        uiState = uiState,
                        onSignIn = { 
                            googleSignInLauncher.launch(viewModel.driveService.getGoogleSignInClient().signInIntent) 
                        },
                        onSignOut = {
                            viewModel.driveService.getGoogleSignInClient().signOut().addOnCompleteListener {
                                account = null
                            }
                        },
                        onUpload = {
                            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                            if (account != null) {
                                viewModel.uploadToDrive(account) { res ->
                                    scope.launch {
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("Caricato su Drive")
                                            viewModel.refreshCloudSummary(account)
                                        }
                                        else snackbarHostState.showSnackbar("Errore caricamento")
                                    }
                                }
                            } else {
                                googleSignInLauncher.launch(viewModel.driveService.getGoogleSignInClient().signInIntent)
                            }
                        },
                        onDownload = {
                            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                            if (account != null) {
                                viewModel.downloadFromDrive(account) { res ->
                                    scope.launch {
                                        res.onSuccess { count -> snackbarHostState.showSnackbar("Sincronizzati $count podcast") }
                                           .onFailure { snackbarHostState.showSnackbar("Errore sincronizzazione") }
                                    }
                                }
                            } else {
                                googleSignInLauncher.launch(viewModel.driveService.getGoogleSignInClient().signInIntent)
                            }
                        },
                        onBack = { currentMenu = SettingsMenu.MAIN }
                    )
                    
                    // Trigger refresh when entering the menu
                    LaunchedEffect(account) {
                        account?.let { viewModel.refreshCloudSummary(it) }
                    }
                }
                SettingsMenu.ABOUT -> {
                    AboutSettingsMenu(
                        onBack = { currentMenu = SettingsMenu.MAIN }
                    )
                }
                SettingsMenu.ACCOUNT -> {
                    AccountSettingsMenu(
                        authState = authState,
                        onSignOut = { authViewModel.signOut() },
                        onResetOnboarding = onResetOnboarding,
                        onBack = { currentMenu = SettingsMenu.MAIN }
                    )
                }
                SettingsMenu.MAIN -> {
                    MainSettingsMenu(
                        uiState = uiState,
                        authState = authState,
                        onBack = onBack,
                        onNavigateToAdvanced = { currentMenu = SettingsMenu.ADVANCED },
                        onNavigateToOpml = { currentMenu = SettingsMenu.OPML },
                        onNavigateToCloud = { currentMenu = SettingsMenu.CLOUD },
                        onNavigateToAbout = { currentMenu = SettingsMenu.ABOUT },
                        onNavigateToPermissions = { currentMenu = SettingsMenu.PERMISSIONS },
                        onNavigateToAccount = { currentMenu = SettingsMenu.ACCOUNT },
                        permissionLauncher = permissionLauncher,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSettingsMenu(
    uiState: SettingsUiState,
    authState: com.example.nothingpodcast.data.auth.AuthState,
    onBack: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToOpml: () -> Unit,
    onNavigateToCloud: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAccount: () -> Unit,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Indietro",
                    tint               = NothingWhite
                )
            }
            Text(
                text  = stringResource(R.string.header_settings),
                style = MaterialTheme.typography.displaySmall,
                color = NothingWhite
            )
        }
        
        // ── 1. Account e Gestione Backup ──────────────────────────────────
        SettingsSection("Account e Gestione Backup")
        SettingsGroup {
            val email = when (val state = authState) {
                is com.example.nothingpodcast.data.auth.AuthState.LoggedIn -> state.email
                else -> "Ospite (Non loggato)"
            }
            SettingsClickRow(
                label    = "Gestione account",
                subtitle = email,
                onClick  = onNavigateToAccount
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsClickRow(
                label    = "Gestione Backup",
                subtitle = "Backup e sync via Google Drive",
                onClick  = onNavigateToCloud
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsClickRow(
                label    = stringResource(R.string.header_opml),
                subtitle = "Importa/Esporta file OPML",
                onClick  = onNavigateToOpml
            )
        }

        // ── 2. Playback ───────────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.header_playback))
        SettingsGroup {
            SettingsChipRow(
                label    = "Salta avanti",
                options  = listOf(10, 15, 30, 45, 60),
                selected = uiState.skipForwardSeconds,
                onSelect = viewModel::setSkipForward,
                suffix   = "s"
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsChipRow(
                label    = "Salta indietro",
                options  = listOf(10, 15, 30, 45, 60),
                selected = uiState.skipBackwardSeconds,
                onSelect = viewModel::setSkipBackward,
                suffix   = "s"
            )
        }

        // ── 3. Verifica Nuovi Episodi ────────────────────────────────────────
        SettingsSection("Verifica Nuovi Episodi")
        SettingsGroup {
            SettingsToggleRow(
                label    = "Controllo automatico",
                subtitle = "Verifica nuovi episodi in background",
                checked  = uiState.autoUpdateEnabled,
                onToggle = viewModel::setAutoUpdateEnabled
            )
            
            if (uiState.autoUpdateEnabled) {
                HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
                
                SettingsChipRow(
                    label    = "Frequenza di verifica",
                    options  = listOf(1, 3, 6, 12, 24),
                    selected = uiState.updateIntervalHours,
                    onSelect = viewModel::setUpdateInterval,
                    suffix   = "h"
                )

                HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

                SettingsToggleRow(
                    label    = "Solo tramite WiFi (Verifica)",
                    subtitle = "Risparmia dati mobili durante la verifica in background",
                    checked  = uiState.updateWifiOnly,
                    onToggle = viewModel::setUpdateWifiOnly
                )
            }

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsToggleRow(
                label    = "Avvisi nuovi episodi",
                subtitle = "Ricevi una notifica quando esce un nuovo episodio",
                checked  = uiState.notificationsEnabled,
                onToggle = { enabled: Boolean ->
                    if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setNotificationsEnabled(enabled)
                    }
                }
            )
        }

        // ── 4. Download Automatico ───────────────────────────────────────────
        SettingsSection("Download Automatico")
        SettingsGroup {
            SettingsToggleRow(
                label    = "Download automatico",
                subtitle = "Avvia il download all'uscita di un nuovo episodio",
                checked  = uiState.autoDownloadEnabled,
                onToggle = viewModel::setAutoDownloadEnabled
            )

            if (uiState.autoDownloadEnabled) {
                HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

                SettingsToggleRow(
                    label    = "Solo tramite WiFi (Download)",
                    subtitle = "Scarica i nuovi episodi solo in presenza di rete WiFi",
                    checked  = uiState.autoDownloadWifiOnly,
                    onToggle = viewModel::setAutoDownloadWifiOnly
                )

                HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

                SettingsToggleRow(
                    label    = "Notifica completamento download",
                    subtitle = "Mostra una notifica al termine del download",
                    checked  = uiState.downloadCompletedNotificationEnabled,
                    onToggle = viewModel::setDownloadCompletedNotificationEnabled
                )
            }
        }
        
        // ── Integrazione Nothing OS ──────────────────────────────────────────
        if (android.os.Build.MANUFACTURER.contains("Nothing", ignoreCase = true)) {
            SettingsSection("Integrazione Nothing OS")
            SettingsGroup {
                SettingsToggleRow(
                    label    = "Glyph Interface",
                    subtitle = "Feedback luminosi discreti durante la riproduzione",
                    checked  = uiState.isGlyphEnabled,
                    onToggle = viewModel::setGlyphEnabled,
                    icon     = Icons.Outlined.Lightbulb
                )
            }
        }

        // ── 5. Info & System ─────────────────────────────────────────────────
        SettingsSection("Informazioni e Sistema")
        SettingsGroup {
            SettingsClickRow(
                label    = "Autorizzazioni",
                subtitle = "Stato dei permessi dell'app",
                onClick  = onNavigateToPermissions
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsClickRow(
                label    = "Avanzate",
                subtitle = "Log e strumenti di debug",
                onClick  = onNavigateToAdvanced
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsClickRow(
                label    = "Informazioni",
                subtitle = "Versione e riconoscimenti",
                onClick  = onNavigateToAbout
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AdvancedSettingsMenu(
    onBack:          () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NothingWhite)
            }
            Text(
                text       = "Impostazioni avanzate",
                style      = MaterialTheme.typography.displaySmall,
                color      = NothingWhite
            )
        }
        
        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)

        SettingsClickRow(
            label    = "Log di sistema",
            subtitle = "Abilita, visualizza o elimina i file di log dell'app.",
            icon     = Icons.Outlined.BugReport,
            onClick  = onNavigateToLogs
        )

        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LogsSettingsMenu(
    uiState:         SettingsUiState,
    onToggleLogging: (Boolean) -> Unit,
    onOpenLog:       () -> Unit,
    onBack:          () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NothingWhite)
            }
            Text(
                text       = "Log di sistema",
                style      = MaterialTheme.typography.displaySmall,
                color      = NothingWhite
            )
        }
        
        Text(
            text = "LOG",
            style = MaterialTheme.typography.labelSmall,
            color = NothingOnSurfaceDim,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
        )

        SettingsGroup {
            SettingsToggleRow(
                label = "Registrazione eventi",
                subtitle = "Registra errori e attività in background per scopi di debug.",
                checked = uiState.loggingEnabled,
                onToggle = onToggleLogging
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsClickRow(
                label = "Apri file di log",
                subtitle = "Visualizza i dettagli tecnici esportati",
                onClick = onOpenLog
            )

            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            SettingsClickRow(
                label = "Cancella log",
                subtitle = "Elimina tutta la cronologia registrata",
                onClick = {
                    com.example.nothingpodcast.util.AppLogger.clearLogs(context)
                    android.widget.Toast.makeText(context, "Log cancellati", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionsMenu(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var isNotificationGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        isNotificationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Funzione per aprire le impostazioni dell'app
    val openAppSettings = {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NothingWhite)
            }
            Text(
                text = "Gestisci autorizzazioni",
                style = MaterialTheme.typography.displaySmall,
                color = NothingWhite
            )
        }

        // Permesso Notifiche
        PermissionItemCard(
            title = "Notifiche",
            description = "Utilizzato per mostrare avvisi di nuovi episodi o lo stato dei download.",
            status = if (isNotificationGranted) "Autorizzato" else "Non autorizzato",
            onClick = openAppSettings
        )

        // Permesso Background (Dati)
        PermissionItemCard(
            title = "Dati in background",
            description = "Utilizzato per la sincronizzazione dei podcast in background e il download.",
            status = "Autorizzato",
            onClick = openAppSettings
        )
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    status: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = NothingSurfaceHigh,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, NothingBorderDim)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NothingWhite,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingOnSurfaceDim
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = NothingOnSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = NothingOnSurfaceDim,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun OpmlSettingsMenu(
    uiState: SettingsUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearError: () -> Unit,
    onBack:   () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NothingWhite)
            }
            Text(
                text       = "Gestione OPML",
                style      = MaterialTheme.typography.displaySmall,
                color      = NothingWhite
            )
        }
        HorizontalDivider(color = NothingBorder)

        if (uiState.isImporting) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { if (uiState.importTotal > 0) uiState.importProgress.toFloat() / uiState.importTotal else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = NothingWhite,
                    trackColor = NothingSurfaceHigh
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Importazione: ${uiState.importProgress} di ${uiState.importTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = NothingOnSurfaceDim
                )
            }
            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
        }

        uiState.importError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(NothingRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onClearError() }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = NothingRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(err, style = MaterialTheme.typography.bodyMedium, color = NothingRed)
                }
            }
            HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
        }

        SettingsClickRow(
            label    = stringResource(R.string.setting_opml_export_title),
            subtitle = stringResource(R.string.setting_opml_export_desc),
            icon     = Icons.Outlined.FileUpload,
            onClick  = onExport
        )

        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)

        SettingsClickRow(
            label    = stringResource(R.string.setting_opml_import_title),
            subtitle = stringResource(R.string.setting_opml_import_desc),
            icon     = if (uiState.isImporting) null else Icons.Outlined.FileDownload,
            onClick  = { if (!uiState.isImporting) onImport() }
        )

        HorizontalDivider(color = NothingBorder)
    }
}

@Composable
private fun AboutSettingsMenu(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NothingWhite)
            }
            Text(
                text       = stringResource(R.string.header_about),
                style      = MaterialTheme.typography.displaySmall,
                color      = NothingWhite
            )
        }
        HorizontalDivider(color = NothingBorder)

        SettingsInfoRow("App", "Nothing Podcast")
        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
        SettingsInfoRow("Version", "1.1.1-alfa")
        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
        SettingsInfoRow("Data source", "iTunes Search API + RSS")
        HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp)
        SettingsInfoRow("Developer", "Lpzz")
        
        HorizontalDivider(color = NothingBorder)
    }
}

@Composable
private fun SettingsClickRow(
    label:    String,
    subtitle: String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector? = Icons.Outlined.ChevronRight,
    onClick:  () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = NothingWhite)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = NothingOnSurfaceVariant)
        }
        icon?.let {
            Icon(it, null, tint = NothingOnSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) NothingWhite else NothingOnSurfaceDim,
                modifier = Modifier.padding(end = 16.dp).size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = NothingWhite)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = NothingOnSurfaceDim)
            }
        }
        NothingSwitch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.sp,
        color    = NothingOnSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = NothingSurfaceHigh,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, NothingBorderDim)
    ) {
        Column(content = content)
    }
}

@Composable
private fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackWidth = 48.dp
    val trackHeight = 26.dp
    val thumbSize = 18.dp
    val padding = 4.dp
    
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - padding else padding,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .size(trackWidth, trackHeight)
            .clip(CircleShape) // Pill shape
            .background(if (checked) NothingWhite else NothingBlack)
            .border(
                width = 1.dp,
                color = if (checked) NothingWhite else NothingBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape) // Circular thumb
                .background(if (checked) NothingBlack else NothingWhite)
        )
    }
}

@Composable
private fun SettingsChipRow(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    suffix: String = ""
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = NothingWhite)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val isSelected = opt == selected
                Box(
                    modifier         = Modifier
                        .clip(RoundedCornerShape(8.dp)) // Added rounding
                        .border(1.dp, if (isSelected) NothingWhite else NothingBorder, RoundedCornerShape(8.dp))
                        .background(if (isSelected) NothingWhite else NothingBlack)
                        .clickable { onSelect(opt) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$opt$suffix",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) NothingBlack else NothingOnSurfaceDim
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = NothingWhite)
        Text(value, style = MaterialTheme.typography.labelLarge, color = NothingOnSurfaceVariant)
    }
}
@Composable
private fun CloudSyncMenu(
    account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?,
    uiState: SettingsUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBack, "Indietro", tint = NothingWhite)
            }
            Text(
                text = "Gestione Backup",
                style = MaterialTheme.typography.displaySmall,
                color = NothingWhite
            )
        }
        HorizontalDivider(color = NothingBorder)

        Text(
            text = "Esegui il backup delle tue iscrizioni e dello stato di ascolto sul tuo spazio personale Google Drive.",
            style = MaterialTheme.typography.bodyMedium,
            color = NothingOnSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )

        if (account == null) {
            SettingsClickRow(
                label = "Accedi con Google",
                subtitle = "Collega il tuo account per la sincronizzazione",
                icon = Icons.Outlined.Login,
                onClick = onSignIn
            )
        } else {
            SettingsClickRow(
                label = "Disconnetti",
                subtitle = "Loggato come ${account.email}",
                icon = Icons.Outlined.Logout,
                onClick = onSignOut
            )
        }

        HorizontalDivider(color = NothingBorderDim, modifier = Modifier.padding(vertical = 8.dp))

        // ── Comparison Card ──────────────────────────────────────────────────
        if (account != null) {
            SyncComparisonCard(
                localCount = uiState.localPodcastCount,
                cloudCount = uiState.cloudSyncSummary?.podcastCount ?: 0,
                lastSync = uiState.cloudSyncSummary?.lastSyncTimestamp,
                totalEpisodes = uiState.totalEpisodeCount,
                playedEpisodes = uiState.playedEpisodeCount,
                cloudPlayedEpisodes = uiState.cloudSyncSummary?.playedEpisodeCount ?: 0,
                cloudTotalEpisodes = uiState.cloudSyncSummary?.totalEpisodeCount ?: 0
            )
            Spacer(Modifier.height(16.dp))
        }

        SettingsClickRow(
            label = "Carica su Drive",
            subtitle = "Salva lo stato attuale sul cloud",
            onClick = onUpload
        )

        SettingsClickRow(
            label = "Scarica da Drive",
            subtitle = "Recupera i dati salvati su questo dispositivo",
            onClick = onDownload
        )
    }
}

@Composable
private fun SyncComparisonCard(
    localCount: Int,
    cloudCount: Int,
    lastSync: Long?,
    totalEpisodes: Int,
    playedEpisodes: Int,
    cloudPlayedEpisodes: Int,
    cloudTotalEpisodes: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = NothingSurfaceHigh,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NothingBorderDim)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Locale
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LOCALE",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 2.sp,
                        color = NothingOnSurfaceVariant
                    )
                    Text(
                        text = localCount.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = NothingWhite
                    )
                    Text(
                        text = "podcast",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingOnSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$playedEpisodes / $totalEpisodes riprodotti",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed,
                        fontSize = 11.sp
                    )
                }

                // Divider (Vertical)
                Box(modifier = Modifier.width(1.dp).height(64.dp).background(NothingBorderDim))

                // Cloud
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CLOUD",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 2.sp,
                        color = NothingOnSurfaceVariant
                    )
                    Text(
                        text = if (lastSync != null) cloudCount.toString() else "--",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (lastSync != null) NothingWhite else NothingOnSurfaceVariant
                    )
                    Text(
                        text = "podcast",
                        style = MaterialTheme.typography.labelMedium,
                        color = NothingOnSurfaceVariant
                    )
                    if (lastSync != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (cloudTotalEpisodes > 0) "$cloudPlayedEpisodes / $cloudTotalEpisodes riprodotti" else "$cloudPlayedEpisodes riprodotti",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingRed,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (lastSync != null) {
                Spacer(Modifier.height(20.dp))
                val date = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.ITALIAN).format(java.util.Date(lastSync))
                Text(
                    text = "ULTIMO BACKUP: $date",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp,
                    color = NothingOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountSettingsMenu(
    authState: com.example.nothingpodcast.data.auth.AuthState,
    onSignOut: () -> Unit,
    onResetOnboarding: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Indietro",
                    tint               = NothingWhite
                )
            }
            Text(
                text  = "ACCOUNT",
                style = MaterialTheme.typography.displaySmall,
                color = NothingWhite
            )
        }

        SettingsSection("Dettagli account")
        SettingsGroup {
            when (authState) {
                is com.example.nothingpodcast.data.auth.AuthState.LoggedIn -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "ACCESSO EFFETTUATO COME",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingOnSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = authState.email,
                            style = MaterialTheme.typography.titleMedium,
                            color = NothingWhite
                        )
                    }

                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

                    SettingsClickRow(
                        label = "Esci",
                        subtitle = "Disconnettiti da questo dispositivo",
                        icon = Icons.Outlined.Logout,
                        onClick = {
                            onSignOut()
                            onBack()
                        }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "MODALITÀ OSPITE",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingOnSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Stai usando l'app come ospite. Accedi o crea un account per sincronizzare le tue iscrizioni sul cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NothingOnSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }

                    HorizontalDivider(color = NothingBorderDim, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

                    SettingsClickRow(
                        label = "Accedi o Registrati",
                        subtitle = "Crea un account o effettua l'accesso",
                        icon = Icons.Outlined.PersonAdd,
                        onClick = onResetOnboarding
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
