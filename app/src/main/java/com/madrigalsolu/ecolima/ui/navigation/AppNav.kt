package com.madrigalsolu.ecolima.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.madrigalsolu.ecolima.data.local.EcolimDatabase
import com.madrigalsolu.ecolima.data.local.UserSessionManager
import com.madrigalsolu.ecolima.data.remote.firebase.FirebaseSyncManager
import com.madrigalsolu.ecolima.ui.screens.HistoryScreen
import com.madrigalsolu.ecolima.ui.screens.HomeDashboardScreen
import com.madrigalsolu.ecolima.ui.screens.LoginScreen
import com.madrigalsolu.ecolima.ui.screens.NewRecordScreen
import com.madrigalsolu.ecolima.ui.screens.ReportsScreen
import com.madrigalsolu.ecolima.ui.theme.EntregableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simple Navigation without external navigation-compose dependency.
 * Sealed class Screen + state-based NavHost with bottom navigation.
 * Keeps prototype build-light and offline-first friendly.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Login : Screen("login", "Login", Icons.Filled.Eco)
    data object Home : Screen("home", "Inicio", Icons.Filled.Home)
    data object History : Screen("history", "Historial", Icons.Filled.History)
    data object Reports : Screen("reports", "Reportes", Icons.Filled.Assessment)
    data object Profile : Screen("profile", "Perfil", Icons.Filled.Person)
    data object NewRecord : Screen("new_record", "Nuevo", Icons.Filled.Add)
}

private val bottomNavItems = listOf(Screen.Home, Screen.History, Screen.Reports)

@Composable
fun AppNav() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { EcolimDatabase.getInstance(context) }
    val registroDao = remember { db.registroDao() }
    val sessionManager = remember { UserSessionManager.getInstance(context) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (!sessionManager.hasPurgedGhostRecords()) {
                registroDao.clearAll()
                try {
                    com.google.android.gms.tasks.Tasks.await(
                        com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("Registros")
                            .removeValue()
                    )
                } catch (_: Exception) {}
                sessionManager.markGhostRecordsPurged()
            }
        }
        FirebaseSyncManager.startRealtimeSync(registroDao, coroutineScope)
        FirebaseSyncManager.syncPendingRecords(registroDao, coroutineScope)
    }

    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val initialUser = auth.currentUser
    val hasSession = initialUser != null || sessionManager.isLoggedIn

    var currentRoute by remember { mutableStateOf(if (hasSession) Screen.Home.route else Screen.Login.route) }
    var showLogin by remember { mutableStateOf(!hasSession) }
    var userName by remember {
        mutableStateOf(
            initialUser?.displayName?.ifBlank { null }
                ?: (if (sessionManager.isLoggedIn) sessionManager.userName else null)
                ?: initialUser?.email?.substringBefore("@")
                    ?.replace(".", " ")
                    ?.replace("_", " ")
                    ?.split(" ")
                    ?.filter { it.isNotBlank() }
                    ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                ?: "Usuario"
        )
    }
    var userEmail by remember {
        mutableStateOf(
            initialUser?.email
                ?: (if (sessionManager.isLoggedIn) sessionManager.userEmail else "usuario@ecolim.pe")
        )
    }
    var historyStatusFilter by remember { mutableStateOf<String?>(null) }

    if (showLogin) {
        LoginScreen(
            onLoginClick = { name, email ->
                userName = name
                userEmail = email
                showLogin = false
                currentRoute = Screen.Home.route
            },
        )
        return
    }

    val isSubScreen = currentRoute == Screen.NewRecord.route || currentRoute == Screen.Profile.route

    BackHandler(enabled = currentRoute != Screen.Home.route && !showLogin) {
        currentRoute = Screen.Home.route
    }

    Scaffold(
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentRoute = screen.route },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label,
                                )
                            },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.Home.route) {
                ExtendedFloatingActionButton(
                    onClick = { currentRoute = Screen.NewRecord.route },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo Registro", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (currentRoute) {
                Screen.Home.route -> HomeDashboardScreen(
                    registroDao = registroDao,
                    userName = userName,
                    onNewRecord = { currentRoute = Screen.NewRecord.route },
                    onProfileClick = { currentRoute = Screen.Profile.route },
                    onViewAll = {
                        historyStatusFilter = null
                        currentRoute = Screen.History.route
                    },
                    onViewPending = {
                        historyStatusFilter = "pending"
                        currentRoute = Screen.History.route
                    },
                )
                Screen.History.route -> HistoryScreen(
                    registroDao = registroDao,
                    initialStatusFilter = historyStatusFilter,
                )
                Screen.Reports.route -> ReportsScreen(
                    registroDao = registroDao,
                )
                Screen.Profile.route -> ProfileScreen(
                    userName = userName,
                    userEmail = userEmail,
                    onBack = { currentRoute = Screen.Home.route },
                    onClearRecords = {
                        FirebaseSyncManager.purgeCloudAndLocal(registroDao, coroutineScope)
                    },
                    onLogout = {
                        auth.signOut()
                        sessionManager.clearSession()
                        showLogin = true
                        currentRoute = Screen.Login.route
                    },
                )
                Screen.NewRecord.route -> NewRecordScreen(
                    userEmail = userEmail,
                    onBack = { currentRoute = Screen.Home.route },
                    onRegister = { entity ->
                        FirebaseSyncManager.pushRegistro(
                            entity = entity,
                            dao = registroDao,
                            scope = coroutineScope
                        )
                        historyStatusFilter = null
                        currentRoute = Screen.History.route
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    userName: String = "Usuario",
    userEmail: String = "usuario@ecolim.pe",
    onBack: () -> Unit = {},
    onClearRecords: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Limpiar todos los registros",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas vaciar el historial y los registros en Firebase? Se eliminarán los datos de prueba y se iniciará en blanco.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showClearDialog = false
                        onClearRecords()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Eliminar todo")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showClearDialog = false },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Usuario", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "ECOLIM S.A.C. • Zona Norte",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = userEmail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(28.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = { showClearDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpiar registros (vaciar base)", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            CardButton(label = "Cerrar sesión", onClick = onLogout)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ECOLIM S.A.C. • v1.0 • Gestión Digital de Residuos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun CardButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.height(48.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun AppNavPreview() {
    EntregableTheme {
        AppNav()
    }
}
