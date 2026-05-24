package com.example.data.repository

import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.db.SavingsDao
import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val savingsDao: SavingsDao) {

    // Customer operations
    val allCustomers: Flow<List<CustomerEntity>> = savingsDao.getAllCustomers()

    fun getCustomer(id: Int): Flow<CustomerEntity?> = savingsDao.getCustomerById(id)

    suspend fun insertCustomer(customer: CustomerEntity): Long {
        return savingsDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        savingsDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        savingsDao.deleteCustomer(customer)
    }

    // Payment operations
    val allPayments: Flow<List<PaymentEntity>> = savingsDao.getAllPayments()

    fun getPaymentsForCustomer(customerId: Int): Flow<List<PaymentEntity>> {
        return savingsDao.getPaymentsForCustomer(customerId)
    }

    suspend fun addPayment(payment: PaymentEntity) {
        savingsDao.addPaymentAndUpdateCustomer(payment)
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        savingsDao.removePaymentAndUpdateCustomer(payment)
    }
}
