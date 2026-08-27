package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseDao
import com.example.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ToolboxRepository(
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao
) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getTotalExpenseFlow(type: String): Flow<Double?> = expenseDao.getTotalAmountByType(type)
    fun getTotalExpenseFlowSince(type: String, start: Long): Flow<Double?> = expenseDao.getTotalAmountByTypeSince(type, start)
    fun getTotalExpenseFlowBetween(type: String, start: Long, end: Long): Flow<Double?> = expenseDao.getTotalAmountByTypeAndDate(type, start, end)

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
        // Update account balance
        val delta = if (expense.type == "EXPENSE") -expense.amount else expense.amount
        if (expense.accountId > 0) {
            accountDao.updateBalance(expense.accountId, delta)
        }
    }

    suspend fun updateExpense(oldExpense: ExpenseEntity, newExpense: ExpenseEntity) {
        // Revert old effect
        val oldDelta = if (oldExpense.type == "EXPENSE") oldExpense.amount else -oldExpense.amount
        if (oldExpense.accountId > 0) {
            accountDao.updateBalance(oldExpense.accountId, oldDelta)
        }

        // Apply new effect
        val newDelta = if (newExpense.type == "EXPENSE") -newExpense.amount else newExpense.amount
        if (newExpense.accountId > 0) {
            accountDao.updateBalance(newExpense.accountId, newDelta)
        }

        expenseDao.updateExpense(newExpense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        val delta = if (expense.type == "EXPENSE") expense.amount else -expense.amount
        if (expense.accountId > 0) {
            accountDao.updateBalance(expense.accountId, delta)
        }
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
    suspend fun updateAccountBalance(accountId: Long, delta: Double) = accountDao.updateBalance(accountId, delta)

    suspend fun updateAccountWithDiscrepancy(
        newAccount: AccountEntity,
        oldBalance: Double,
        saveAsMissedRecord: Boolean
    ) {
        accountDao.updateAccount(newAccount)
        val balanceDiff = newAccount.balance - oldBalance
        if (saveAsMissedRecord && kotlin.math.abs(balanceDiff) > 0.001) {
            val isIncome = balanceDiff > 0
            val amount = kotlin.math.abs(balanceDiff)
            val type = if (isIncome) "INCOME" else "EXPENSE"
            val subCategory = if (isIncome) "余额补录" else "余额校准"
            val noteText = "账户「${newAccount.name}」余额调整(${if (isIncome) "补录增加" else "扣减校准"})"
            val discrepancyExpense = ExpenseEntity(
                type = type,
                category = "漏记款",
                subCategory = subCategory,
                amount = amount,
                note = noteText,
                dateTimestamp = System.currentTimeMillis(),
                accountId = newAccount.id,
                accountName = newAccount.name
            )
            // Insert directly into expenseDao without double-adjusting account balance
            expenseDao.insertExpense(discrepancyExpense)
        }
    }
}
