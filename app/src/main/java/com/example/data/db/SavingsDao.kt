package com.example.data.db

import androidx.room.*
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    // Customer Queries
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Int): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdOneShot(id: Int): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // Payment Queries
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY installmentIndex ASC")
    fun getPaymentsForCustomer(customerId: Int): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY installmentIndex ASC")
    suspend fun getPaymentsForCustomerOneShot(customerId: Int): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    // Transactional operation: Insert payment and update customer amountPaid in a clean, robust way.
    @Transaction
    suspend fun addPaymentAndUpdateCustomer(payment: PaymentEntity) {
        insertPayment(payment)
        val customer = getCustomerByIdOneShot(payment.customerId)
        if (customer != null) {
            val payments = getPaymentsForCustomerOneShot(payment.customerId)
            val totalPaid = payments.sumOf { it.amount }
            val updated = customer.copy(amountPaid = totalPaid)
            updateCustomer(updated)
        }
    }

    @Transaction
    suspend fun removePaymentAndUpdateCustomer(payment: PaymentEntity) {
        deletePayment(payment)
        val customer = getCustomerByIdOneShot(payment.customerId)
        if (customer != null) {
            val payments = getPaymentsForCustomerOneShot(payment.customerId)
            val totalPaid = payments.sumOf { it.amount }
            val updated = customer.copy(amountPaid = totalPaid)
            updateCustomer(updated)
        }
    }
}
