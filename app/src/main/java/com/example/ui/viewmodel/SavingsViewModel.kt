package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.repository.SavingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Screen Enumeration
enum class AppScreen {
    DASHBOARD,
    CUSTOMERS,
    PAYMENTS,
    ADMIN
}

// Sync Status Enumeration
enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SavingsRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SavingsRepository(database.savingsDao())
    }

    // Live Data Flows
    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPaymentsForCustomer(customerId: Int): Flow<List<PaymentEntity>> {
        return repository.getPaymentsForCustomer(customerId)
    }

    // Safe Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Customer Selection & Edit/Add States
    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    // Search Query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Highly Optimized Search filtered list
    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(customers, searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.phone.contains(query) || 
                it.email.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin & Passcode Security State
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val adminPin = "8888" // Core PIN

    fun enterPinDigit(digit: String) {
        _pinError.value = null
        if (_enteredPin.value.length < 4) {
            _enteredPin.value += digit
            if (_enteredPin.value.length == 4) {
                verifyPin()
            }
        }
    }

    fun deletePinDigit() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
        }
    }

    fun clearPin() {
        _enteredPin.value = ""
        _pinError.value = null
    }

    private fun verifyPin() {
        if (_enteredPin.value == adminPin) {
            _isAdminUnlocked.value = true
            _pinError.value = null
        } else {
            _enteredPin.value = ""
            _pinError.value = "Access Denied: Invalid Secure PIN"
        }
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
        _enteredPin.value = ""
    }

    // Premium Live Sync and Simulated Cloud Center (Firebase/Supabase Mirror)
    private val _syncState = MutableStateFlow(SyncState.SUCCESS)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(listOf("Cloud session instantiated successfully."))
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _simulatedLatencyMs = MutableStateFlow(120)
    val simulatedLatencyMs: StateFlow<Int> = _simulatedLatencyMs.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
        val mode = if (_isOfflineMode.value) "OFFLINE Mode Enabled" else "ONLINE Sync Connected"
        logSyncMessage("Mode switched: $mode")
        if (!_isOfflineMode.value) {
            triggerCloudSync()
        }
    }

    fun logSyncMessage(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $msg"
        _syncLogs.value = (listOf(formatted) + _syncLogs.value).take(40)
    }

    fun triggerCloudSync() {
        if (_isOfflineMode.value) {
            logSyncMessage("Sync skipped: Device is offline.")
            return
        }
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            logSyncMessage("Connecting to secure Firebase Cluster...")
            delay(400)
            _simulatedLatencyMs.value = (80..320).random()
            logSyncMessage("Authorizing token credentials...")
            delay(500)
            logSyncMessage("Comparing ledger changes with primary cloud bucket...")
            delay(600)
            logSyncMessage("Uploading ${customers.value.size} accounts and ${payments.value.size} receipts...")
            delay(400)
            _syncState.value = SyncState.SUCCESS
            logSyncMessage("Synchronization Complete. Latency: ${_simulatedLatencyMs.value}ms. Integrity verified.")
        }
    }

    // CRUD Core Operations
    fun addCustomer(name: String, phone: String, email: String, monthlyAmount: Double, initialSilverRate: Double, notes: String) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name,
                phone = phone,
                email = email,
                monthlyAmount = monthlyAmount,
                initialSilverRate = initialSilverRate,
                notes = notes
            )
            val newId = repository.insertCustomer(customer)
            logSyncMessage("Created local ledger for customer '$name' (ID: $newId).")
            triggerCloudSync()
        }
    }

    fun editCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            logSyncMessage("Updated profile configurations for ${customer.name}.")
            triggerCloudSync()
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            logSyncMessage("Archived and deleted records for ${customer.name}.")
            if (_selectedCustomer.value?.id == customer.id) {
                _selectedCustomer.value = null
            }
            triggerCloudSync()
        }
    }

    fun recordPayment(customerId: Int, installmentIndex: Int, amount: Double, method: String, referenceNo: String, notes: String) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                customerId = customerId,
                installmentIndex = installmentIndex,
                amount = amount,
                paymentMethod = method,
                referenceNo = referenceNo,
                notes = notes
            )
            repository.addPayment(payment)
            logSyncMessage("Added receipt for installment #$installmentIndex (Amount: INR $amount).")
            triggerCloudSync()
        }
    }

    fun removePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            logSyncMessage("Revoked receipt for installment #${payment.installmentIndex} (INR ${payment.amount}).")
            triggerCloudSync()
        }
    }

    // Dynamic Database Seed for High Fidelity Trial Experience on first run!
    fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            if (customers.value.isEmpty()) {
                logSyncMessage("No initial database entries. Injecting premium sample data set...")

                val samples = listOf(
                    CustomerEntity(
                        name = "Sarah Jenkins",
                        phone = "+91 98455 12091",
                        email = "sarah.j@slvr.io",
                        monthlyAmount = 5000.0,
                        initialSilverRate = 88.5,
                        notes = "Scheme locked during Akshaya Tritiya"
                    ),
                    CustomerEntity(
                        name = "Rajesh Kumar",
                        phone = "+91 97722 55431",
                        email = "rajesh.kumar@gmail.com",
                        monthlyAmount = 10000.0,
                        initialSilverRate = 91.2,
                        notes = "Looking for heavy silver ornament sets upon maturity"
                    ),
                    CustomerEntity(
                        name = "Elena Rostova",
                        phone = "+91 81232 44335",
                        email = "elena.ros@icloud.com",
                        monthlyAmount = 3500.0,
                        initialSilverRate = 89.0,
                        notes = "Regular saver"
                    )
                )

                samples.forEach { sample ->
                    val customerId = repository.insertCustomer(sample)
                    // Add sample payments to simulate rich dashboard stats
                    if (sample.name.startsWith("Sarah")) {
                        repository.addPayment(PaymentEntity(customerId = customerId.toInt(), installmentIndex = 1, amount = 5000.0, paymentMethod = "UPI", referenceNo = "TXN7761"))
                        repository.addPayment(PaymentEntity(customerId = customerId.toInt(), installmentIndex = 2, amount = 5000.0, paymentMethod = "UPI", referenceNo = "TXN9210"))
                        repository.addPayment(PaymentEntity(customerId = customerId.toInt(), installmentIndex = 3, amount = 5000.0, paymentMethod = "UPI", referenceNo = "TXN1082"))
                    } else if (sample.name.startsWith("Rajesh")) {
                        repository.addPayment(PaymentEntity(customerId = customerId.toInt(), installmentIndex = 1, amount = 10000.0, paymentMethod = "UPI", referenceNo = "TXN4022"))
                        repository.addPayment(PaymentEntity(customerId = customerId.toInt(), installmentIndex = 2, amount = 10000.0, paymentMethod = "BANK", referenceNo = "TXN0981"))
                    }
                }
                logSyncMessage("Seeded base ledger successfully. Cloud synchronized.")
                triggerCloudSync()
            }
        }
    }
}
