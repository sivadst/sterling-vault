package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val monthlyAmount: Double,
    val amountPaid: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val maturityDate: Long = System.currentTimeMillis() + (11L * 30L * 24L * 60L * 60L * 1000L), // 11 months Standard
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, LAPSED
    val initialSilverRate: Double = 90.0, // locked silver rate in INR/gram at start
    val notes: String = ""
) {
    // Computed property: balance remaining
    val pendingBalance: Double
        get() = (monthlyAmount * 11) - amountPaid

    // Computed property: equivalent silver saved in grams (calculated based on locked rate per installment)
    val silverSavedGrams: Double
        get() = if (initialSilverRate > 0) amountPaid / initialSilverRate else 0.0

    // Maturity progress (0.00 to 1.00)
    val maturityProgress: Float
        get() {
            val totalTime = (maturityDate - startDate).toFloat()
            if (totalTime <= 0) return 1f
            val elapsed = (System.currentTimeMillis() - startDate).toFloat()
            return (elapsed / totalTime).coerceIn(0f, 1f)
        }
}
