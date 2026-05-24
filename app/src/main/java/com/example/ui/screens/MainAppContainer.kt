package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SavingsViewModel
import com.example.ui.viewmodel.SyncState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: SavingsViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    // Seeds sample data when first started
    LaunchedEffect(customers) {
        viewModel.seedSampleDataIfEmpty()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavPill(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        // Premium sliding-fade transition between screen sections
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeEffect()
            },
            modifier = Modifier
                .fillMaxSize()
                .background(PremiumBg)
                .padding(innerPadding),
            label = "screen_transition"
        ) { targetScreen ->
            when (targetScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel)
                AppScreen.CUSTOMERS -> CustomersScreen(viewModel)
                AppScreen.PAYMENTS -> ReceiptsScreen(viewModel)
                AppScreen.ADMIN -> AdminControlsScreen(viewModel)
            }
        }
    }
}

// Custom CRED style transition builder
fun fadeEffect(): ContentTransform {
    return (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220))) togetherWith
           (fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.96f, animationSpec = tween(150)))
}

// Sleek Pill Bottom Navigation recreating CRED elegance
@Composable
fun BottomNavPill(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    Surface(
        color = PremiumCardBg.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Dashboard,
                label = "Status",
                isActive = currentScreen == AppScreen.DASHBOARD,
                onClick = { onNavigate(AppScreen.DASHBOARD) }
            )
            NavItem(
                icon = Icons.Default.People,
                label = "Ledger",
                isActive = currentScreen == AppScreen.CUSTOMERS,
                onClick = { onNavigate(AppScreen.CUSTOMERS) }
            )
            NavItem(
                icon = Icons.Default.Receipt,
                label = "Receipts",
                isActive = currentScreen == AppScreen.PAYMENTS,
                onClick = { onNavigate(AppScreen.PAYMENTS) }
            )
            NavItem(
                icon = Icons.Default.Lock,
                label = "Admin",
                isActive = currentScreen == AppScreen.ADMIN,
                onClick = { onNavigate(AppScreen.ADMIN) }
            )
        }
    }
}

@Composable
fun RowScope.NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val duration = 250
    val scale = if (isActive) 1.05f else 0.92f
    val iconColor = if (isActive) NeonCyan else SilverSecondary

    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = iconColor,
                    letterSpacing = 0.4.sp
                )
            )
        }
    }
}

// ---------------- DASHBOARD COMPONENT ----------------

@Composable
fun DashboardScreen(viewModel: SavingsViewModel) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val logs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val latency by viewModel.simulatedLatencyMs.collectAsStateWithLifecycle()

    // Metrics calculation
    val activeCustomers = customers.filter { it.status == "ACTIVE" }
    val totalCollections = customers.sumOf { it.amountPaid }
    val totalPending = customers.sumOf { it.pendingBalance }
    val silverReserveGrams = customers.sumOf { it.silverSavedGrams }

    // Aggregate monthly collections
    val monthlyData = remember(payments) {
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        val map = TreeMap<Long, Double>()
        // Initialize last 4 months
        val cal = Calendar.getInstance()
        for (i in 0..3) {
            val key = cal.timeInMillis - (i * 30L * 24 * 60 * 60 * 1000)
            map[key] = 0.0
        }
        payments.forEach { p ->
            val pCal = Calendar.getInstance().apply { timeInMillis = p.paymentDate }
            // Find matched month key
            val matchKey = map.keys.find {
                val calMatch = Calendar.getInstance().apply { timeInMillis = it }
                calMatch.get(Calendar.YEAR) == pCal.get(Calendar.YEAR) &&
                calMatch.get(Calendar.MONTH) == pCal.get(Calendar.MONTH)
            }
            if (matchKey != null) {
                map[matchKey] = (map[matchKey] ?: 0.0) + p.amount
            }
        }
        map.map { (key, amount) ->
            sdf.format(Date(key)) to amount
        }.reversed()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header and Sync status ticker (Immersive UI edition)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Avatar matching high-end spec: AG (Sterling Vault Active Gold Portfolio)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F0F12))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Column {
                        Text(
                            text = "PORTFOLIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SilverSecondary,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "Sterling Vault",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
                
                // Realtime Sync indicator card disguised as navigation/action circle button
                Surface(
                    color = Color.Transparent,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { viewModel.triggerCloudSync() }
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.01f)
                                    )
                                )
                            )
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PulsingGlowDot(state = syncState, isOffline = isOffline)
                    }
                }
            }
        }

        // Vault Premium Giant Indicator Card (Immersive UI Style)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background radial glow representing the custom overlay blur and lights
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2E2E38).copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL SAVINGS VALUE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = SilverSecondary,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Large Elegant Minimalist Silver Text display
                    Text(
                        text = "₹${"%,.0f".format(totalCollections)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp,
                            color = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silver Reserve: %.2f g".format(silverReserveGrams),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SilverSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Glass badge: Live status / Month increase matching Tailwind class lists
                    Row(
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Small glowing dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeonEmerald, CircleShape)
                        )
                        Text(
                            text = "+12.4% THIS MONTH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }

        // Double mini indicators: Active accounts & Outstanding liabilities
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Card
                Box(modifier = Modifier.weight(1f)) {
                    GlassmorphicCard {
                        Text(
                            text = "ACTIVE SAVERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${activeCustomers.size}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SilverPrimary
                            )
                        )
                        Text(
                            text = "Accounts open",
                            style = MaterialTheme.typography.bodySmall.copy(color = SilverSecondary)
                        )
                    }
                }
                
                // Right Card
                Box(modifier = Modifier.weight(1f)) {
                    GlassmorphicCard {
                        Text(
                            text = "DUE OUTSTANDING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${"%,.0f".format(totalPending)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGlow
                            )
                        )
                        Text(
                            text = "Maturity deficit",
                            style = MaterialTheme.typography.bodySmall.copy(color = SilverSecondary)
                        )
                    }
                }
            }
        }

        // Custom chart representing monthly collections
        item {
            GlassmorphicCard {
                Text(
                    text = "MONTHLY REALIZED COLLECTIONS (INR)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SilverSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                GlanceChart(dataPoints = monthlyData)
            }
        }

        // Automated Firebase Live Replication Center Logs (CRED design depth)
        item {
            GlassmorphicCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CLOUD SECURITY ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Simulated Firestore replication cluster active.",
                            style = MaterialTheme.typography.bodySmall.copy(color = SilverSecondary.copy(alpha = 0.7f))
                        )
                    }
                    IconButton(
                        onClick = { viewModel.triggerCloudSync() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = GlassBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Manual sync", tint = SilverPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Logging display console
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = false
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (log.contains("Complete") || log.contains("Successful") || log.contains("Online")) NeonEmerald else SilverSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

// ---------------- CUSTOMERS LEDGER COMPONENT ----------------

@Composable
fun CustomersScreen(viewModel: SavingsViewModel) {
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<CustomerEntity?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CUSTOMER LEDGER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SilverSecondary,
                        letterSpacing = 2.5.sp
                    )
                )
                Text(
                    text = "Savings Profiles",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SilverPrimary
                    )
                )
            }
            
            // Large floating-level round button to add customers
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SilverPrimary),
                colors = IconButtonDefaults.iconButtonColors(contentColor = PremiumBg)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Enroll customer", modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Input Field
        PremiumTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = "Search customer, mobile, email...",
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SilverSecondary) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Customers Roster
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "Empty",
                        tint = SilverSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No customers matches.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SilverSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredCustomers) { customer ->
                    CustomerCard(
                        customer = customer,
                        onClick = { selectedCustomerForDetails = customer }
                    )
                }
                item { Spacer(modifier = Modifier.height(110.dp)) }
            }
        }
    }

    // Modal dialog trigger for customer additions
    if (showAddDialog) {
        CustomerAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, email, monthly, rate, note ->
                viewModel.addCustomer(name, phone, email, monthly, rate, note)
                showAddDialog = false
            }
        )
    }

    // Custom details sheet overlay for customer statistics
    if (selectedCustomerForDetails != null) {
        CustomerDetailsSheet(
            customer = selectedCustomerForDetails!!,
            viewModel = viewModel,
            onDismiss = { selectedCustomerForDetails = null }
        )
    }
}

// Beautifully crafted profile Card listing for customers list
@Composable
fun CustomerCard(customer: CustomerEntity, onClick: () -> Unit) {
    val progressPercent = (customer.amountPaid / (customer.monthlyAmount * 11).coerceAtLeast(1.0)) * 100
    
    Surface(
        color = PremiumCardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium.copy(color = SilverSecondary)
                    )
                }
                
                // Silver locked rate pill
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "₹${customer.initialSilverRate.toInt()}/g",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = GlassBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Savings Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MONTHLY PLAN",
                        style = MaterialTheme.typography.labelSmall.copy(color = SilverSecondary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "₹${customer.monthlyAmount.toInt()}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = SilverPrimary, fontWeight = FontWeight.Bold)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SAVED VOLUME",
                        style = MaterialTheme.typography.labelSmall.copy(color = SilverSecondary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "%.2f g".format(customer.silverSavedGrams),
                        style = MaterialTheme.typography.bodyLarge.copy(color = NeonEmerald, fontWeight = FontWeight.Bold)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL SAVINGS",
                        style = MaterialTheme.typography.labelSmall.copy(color = SilverSecondary, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "₹${customer.amountPaid.toInt()}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Linear Progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (progressPercent / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonCyan,
                    trackColor = GlassBorder,
                )
                Text(
                    text = "${progressPercent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SilverPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ---------------- ADD CUSTOMER DIALOG COMPONENT ----------------

@Composable
fun CustomerAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, monthly: Double, rate: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var monthlyAmount by remember { mutableStateOf("") }
    var initialRate by remember { mutableStateOf("92") } // standard silver rate preset in India
    var notes by remember { mutableStateOf("") }

    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PremiumSurface,
        modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        title = {
            Text(
                "ENLIST NEW SAVER",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                PremiumTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Mobile Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                PremiumTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address (Optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                PremiumTextField(
                    value = monthlyAmount,
                    onValueChange = { monthlyAmount = it },
                    label = "Monthly Savings Commitment (INR)",
                    placeholder = "e.g. 5000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                PremiumTextField(
                    value = initialRate,
                    onValueChange = { initialRate = it },
                    label = "Locked Silver Rate (INR/g)",
                    placeholder = "e.g. 92",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                PremiumTextField(value = notes, onValueChange = { notes = it }, label = "Additional Notes")
                
                if (hasError) {
                    Text(
                        "Please fill all mandatory fields correctly.",
                        color = NeonRose,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            GlassButton(
                text = "Activate Ledger",
                onClick = {
                    val amt = monthlyAmount.toDoubleOrNull()
                    val rt = initialRate.toDoubleOrNull()
                    if (name.isNotBlank() && phone.isNotBlank() && amt != null && rt != null) {
                        onConfirm(name, phone, email, amt, rt, notes)
                    } else {
                        hasError = true
                    }
                }
            )
        },
        dismissButton = {
            GlassOutlinedButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

// ---------------- CUSTOMER DETAILS OVERLAY COMPONENT ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsSheet(
    customer: CustomerEntity,
    viewModel: SavingsViewModel,
    onDismiss: () -> Unit
) {
    val payments by viewModel.getPaymentsForCustomer(customer.id).collectAsStateWithLifecycle(emptyList())

    var showPaymentDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    // Edit fields holders
    var editName by remember { mutableStateOf(customer.name) }
    var editPhone by remember { mutableStateOf(customer.phone) }
    var editEmail by remember { mutableStateOf(customer.email) }
    var editNotes by remember { mutableStateOf(customer.notes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PremiumSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SilverSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (!editMode) {
                // View Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Joined ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(customer.startDate))}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = SilverSecondary)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { editMode = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = GlassBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", tint = SilverPrimary)
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteCustomer(customer)
                                onDismiss()
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = GlassBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Archive Profile", tint = NeonRose)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Detail Box Metrics
                GlassmorphicCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pending Balance", style = MaterialTheme.typography.labelSmall, color = SilverSecondary)
                            Text("₹${customer.pendingBalance.toInt()}", style = MaterialTheme.typography.titleLarge, color = AmberGlow, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Maturity Target", style = MaterialTheme.typography.labelSmall, color = SilverSecondary)
                            Text("₹${customer.monthlyAmount.toInt() * 11}", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GlassBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Contact: ${customer.phone}", style = MaterialTheme.typography.bodyMedium, color = SilverPrimary)
                    if (customer.email.isNotBlank()) {
                        Text("Email: ${customer.email}", style = MaterialTheme.typography.bodyMedium, color = SilverPrimary)
                    }
                    if (customer.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reference Memo: ${customer.notes}", style = MaterialTheme.typography.bodySmall, color = SilverSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Installments receipt tracking list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTALLMENT SLIPS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SilverSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                    
                    TextButton(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Add Receipt")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Book Installment", fontWeight = FontWeight.Bold)
                    }
                }

                if (payments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No payment installments booked yet.", color = SilverSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    payments.forEach { p ->
                        Surface(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald)
                                    Column {
                                        Text("Installment #${p.installmentIndex}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(p.paymentDate)), style = MaterialTheme.typography.bodySmall, color = SilverSecondary)
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("₹${p.amount.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { viewModel.removePayment(p) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = "Revoke Payment", tint = NeonRose.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Edit Profile Mode
                Text("Edit Account Specifications", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(14.dp))
                
                PremiumTextField(value = editName, onValueChange = { editName = it }, label = "Full Name")
                Spacer(modifier = Modifier.height(10.dp))
                PremiumTextField(value = editPhone, onValueChange = { editPhone = it }, label = "Mobile Number")
                Spacer(modifier = Modifier.height(10.dp))
                PremiumTextField(value = editEmail, onValueChange = { editEmail = it }, label = "Email Address")
                Spacer(modifier = Modifier.height(10.dp))
                PremiumTextField(value = editNotes, onValueChange = { editNotes = it }, label = "Reference Notes")
                
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassOutlinedButton(
                        text = "Cancel",
                        onClick = { editMode = false },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Save Changes",
                        onClick = {
                            viewModel.editCustomer(
                                customer.copy(
                                    name = editName,
                                    phone = editPhone,
                                    email = editEmail,
                                    notes = editNotes
                                )
                            )
                            editMode = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Modal dialog trigger to insert customer due installment payments
    if (showPaymentDialog) {
        val nextInstallment = (payments.maxOfOrNull { it.installmentIndex } ?: 0) + 1
        PaymentBookDialog(
            nextIndex = nextInstallment,
            suggestedAmount = customer.monthlyAmount,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { index, amount, method, ref, notes ->
                viewModel.recordPayment(customer.id, index, amount, method, ref, notes)
                showPaymentDialog = false
            }
        )
    }
}

// ---------------- BOOK INSTALLMENT DIALOG COMPONENT ----------------

@Composable
fun PaymentBookDialog(
    nextIndex: Int,
    suggestedAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (index: Int, amount: Double, method: String, ref: String, notes: String) -> Unit
) {
    var indexText by remember { mutableStateOf(nextIndex.toString()) }
    var amountText by remember { mutableStateOf(suggestedAmount.toInt().toString()) }
    var method by remember { mutableStateOf("UPI") } // UPI default
    var txnRef by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val methods = listOf("UPI", "CASH", "CARD", "BANK")
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PremiumSurface,
        modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        title = {
            Text(
                "BOOK INSTALLMENT PAYMENT",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumTextField(
                    value = indexText,
                    onValueChange = { indexText = it },
                    label = "Installment Index (1-11)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                PremiumTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Amount Booked (INR)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // Segmented buttons / selectors for cash method
                Text("Select Method", style = MaterialTheme.typography.labelSmall, color = SilverSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { m ->
                        val isSel = method == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) SilverPrimary else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isSel) Color.Transparent else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable { method = m }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(m, color = if (isSel) PremiumBg else SilverSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                PremiumTextField(value = txnRef, onValueChange = { txnRef = it }, label = "Transaction Ref (Optional)", placeholder = "e.g. UPI8831")
                PremiumTextField(value = note, onValueChange = { note = it }, label = "Memo Notes")

                if (hasError) {
                    Text("Invalid inputs. Ensure correct installment index & value.", color = NeonRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            GlassButton(
                text = "Record Payment",
                onClick = {
                    val idx = indexText.toIntOrNull()
                    val amt = amountText.toDoubleOrNull()
                    if (idx != null && idx in 1..24 && amt != null && amt > 0) {
                        onConfirm(idx, amt, method, txnRef, note)
                    } else {
                        hasError = true
                    }
                }
            )
        },
        dismissButton = {
            GlassOutlinedButton(text = "Dismiss", onClick = onDismiss)
        }
    )
}

// ---------------- RECEIPTS LOG OVERVIEW COMPONENT ----------------

@Composable
fun ReceiptsScreen(viewModel: SavingsViewModel) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val filteredPayments = remember(payments, customers, searchQuery) {
        if (searchQuery.isBlank()) {
            payments
        } else {
            payments.filter { p ->
                val name = customers.find { it.id == p.customerId }?.name ?: ""
                name.contains(searchQuery, ignoreCase = true) ||
                p.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                p.referenceNo.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        
        Text(
            text = "PAYMENT HISTORIES",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SilverSecondary,
                letterSpacing = 2.5.sp
            )
        )
        Text(
            text = "Receipts Audits",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SilverPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Input
        PremiumTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search receipts by client, method...",
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SilverSecondary) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredPayments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching transaction entries booked.", color = SilverSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPayments) { p ->
                    val customer = customers.find { it.id == p.customerId }
                    val name = customer?.name ?: "Unknown Saver"
                    
                    Surface(
                        color = PremiumCardBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Instalment #${p.installmentIndex}",
                                            fontSize = 10.sp,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = p.paymentMethod,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SilverSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (p.referenceNo.isNotBlank()) {
                                    Text("Ref: ${p.referenceNo}", style = MaterialTheme.typography.labelSmall, color = SilverSecondary.copy(alpha = 0.6f))
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${p.amount.toInt()}", color = NeonEmerald, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(p.paymentDate)), style = MaterialTheme.typography.labelSmall, color = SilverSecondary)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(110.dp)) }
            }
        }
    }
}

// ---------------- ADMIN CONTROLS SCREEN with PIN LOCK ----------------

@Composable
fun AdminControlsScreen(viewModel: SavingsViewModel) {
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val enteredPin by viewModel.enteredPin.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()

    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    if (!isAdminUnlocked) {
        // Futuristic Dot Security Keypad View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.width(320.dp)
            ) {
                // Pin view Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ADMINISTRATOR SHIELD", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Enter secure administrative PIN passcode to access analytics & settings.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = SilverSecondary)
                }

                // Interactive Pin Display Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0..3) {
                        val digitEntered = enteredPin.length > i
                        val circleColor = if (digitEntered) NeonCyan else Color.White.copy(alpha = 0.12f)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(1.5.dp, if (digitEntered) Color.Transparent else GlassBorder, CircleShape)
                        )
                    }
                }

                if (pinError != null) {
                    Text(pinError!!, color = NeonRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                } else {
                    Text("Default trial PIN: 8888", style = MaterialTheme.typography.labelSmall, color = SilverSecondary.copy(alpha = 0.5f))
                }

                // Nothing OS Keyboard Dial layout
                val digits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    digits.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { char ->
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(1.dp, GlassBorder, CircleShape)
                                        .clickable {
                                            when (char) {
                                                "C" -> viewModel.clearPin()
                                                "⌫" -> viewModel.deletePinDigit()
                                                else -> viewModel.enterPinDigit(char)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (char == "C" || char == "⌫") NeonCyan else Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Admin Dashboard Settings Board
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REGULATORY SYSTEMS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SilverSecondary,
                                letterSpacing = 2.5.sp
                            )
                        )
                        Text(
                            text = "Admin Controls",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SilverPrimary
                            )
                        )
                    }
                    IconButton(
                        onClick = { viewModel.lockAdmin() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = GlassBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock controls", tint = NeonCyan)
                    }
                }
            }

            // Analytical backup details
            item {
                GlassmorphicCard {
                    Text("AGGREGATED LIABILITY INSIGHTS", style = MaterialTheme.typography.labelSmall, color = SilverSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val outstandingSum = customers.sumOf { it.pendingBalance }
                    val assetLiabilityFraction = if (payments.isEmpty()) 0f else (payments.sumOf { it.amount } / (customers.sumOf { it.monthlyAmount } * 11)).toFloat()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Scheme Assets", style = MaterialTheme.typography.bodyMedium, color = SilverSecondary)
                            Text("₹${"%,.0f".format(customers.sumOf { it.amountPaid })}", style = MaterialTheme.typography.headlineSmall, color = NeonEmerald, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Realized Ratio", style = MaterialTheme.typography.bodyMedium, color = SilverSecondary)
                            Text("${(assetLiabilityFraction * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Database Simulation Control Settings
            item {
                GlassmorphicCard {
                    Text("REALTIME SIMULATION UTILITIES", style = MaterialTheme.typography.labelSmall, color = SilverSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    // Offline switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local-First Mock Offline State", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Toggle network offline state to test immediate background queuing database integrity.", style = MaterialTheme.typography.bodySmall, color = SilverSecondary)
                        }
                        Switch(
                            checked = isOffline,
                            onCheckedChange = { viewModel.toggleOfflineMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonEmerald,
                                checkedTrackColor = NeonEmerald.copy(alpha = 0.4f),
                                uncheckedThumbColor = SilverSecondary,
                                uncheckedTrackColor = GlassBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = GlassBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Reset & Trial Seeds", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Reset database parameters or load an instantly filled ledger featuring pre-built graphs.", style = MaterialTheme.typography.bodySmall, color = SilverSecondary, modifier = Modifier.padding(bottom = 10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = "Seed Extra Data",
                            onClick = {
                                viewModel.seedSampleDataIfEmpty()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(110.dp)) }
        }
    }
}
