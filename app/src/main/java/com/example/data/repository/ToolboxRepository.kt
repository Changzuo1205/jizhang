package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseDao
import com.example.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * 业务数据仓库层 (Repository Pattern)
 *
 * 统一协调记账明细 ([ExpenseDao]) 与资产账户 ([AccountDao]) 之间的数据交互，
 * 保证资金记账与对应账户余额联动的原子性和一致性。
 *
 * 核心职责：
 * 1. 暴露响应式数据流（Flow<List<ExpenseEntity>> / Flow<List<AccountEntity>>）。
 * 2. 记账插入/更新/删除时，自动联动更新关联资产账户余额。
 * 3. 支持账户余额校准时一键生成“漏记款”冲平账目（避免破坏历史收支平衡）。
 */
class ToolboxRepository(
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao
) {
    /** 全部记账明细实时流（按时间倒序） */
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    /** 全部资产账户实时流 */
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    /** 按类型获取总收支汇总流 */
    fun getTotalExpenseFlow(type: String): Flow<Double?> = expenseDao.getTotalAmountByType(type)

    /** 获取指定时间以来的收支汇总流（如本月度聚合） */
    fun getTotalExpenseFlowSince(type: String, start: Long): Flow<Double?> = expenseDao.getTotalAmountByTypeSince(type, start)

    /** 获取时间区间内的收支汇总流（如特定月份或自定义日期） */
    fun getTotalExpenseFlowBetween(type: String, start: Long, end: Long): Flow<Double?> = expenseDao.getTotalAmountByTypeAndDate(type, start, end)

    /**
     * 新增一笔记账明细，并联动扣减/增加对应资产账户余额
     */
    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
        // 支出则扣减账户余额，收入则增加账户余额
        val delta = if (expense.type == "EXPENSE") -expense.amount else expense.amount
        if (expense.accountId > 0) {
            accountDao.updateBalance(expense.accountId, delta)
        }
    }

    /**
     * 编辑更新已有记账记录：回滚旧记录对账户余额的影响，并施加新记录的余额影响
     */
    suspend fun updateExpense(oldExpense: ExpenseEntity, newExpense: ExpenseEntity) {
        // 1. 回滚旧记录的影响
        val oldDelta = if (oldExpense.type == "EXPENSE") oldExpense.amount else -oldExpense.amount
        if (oldExpense.accountId > 0) {
            accountDao.updateBalance(oldExpense.accountId, oldDelta)
        }

        // 2. 应用新记录的影响
        val newDelta = if (newExpense.type == "EXPENSE") -newExpense.amount else newExpense.amount
        if (newExpense.accountId > 0) {
            accountDao.updateBalance(newExpense.accountId, newDelta)
        }

        // 3. 更新数据库记录
        expenseDao.updateExpense(newExpense)
    }

    /**
     * 删除单条记账记录，并回滚其对关联账户余额的影响
     */
    suspend fun deleteExpense(expense: ExpenseEntity) {
        val delta = if (expense.type == "EXPENSE") expense.amount else -expense.amount
        if (expense.accountId > 0) {
            accountDao.updateBalance(expense.accountId, delta)
        }
        expenseDao.deleteExpense(expense)
    }

    /** 插入新资产账户 */
    suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)

    /** 更新已有资产账户属性 */
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)

    /** 删除资产账户 */
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    /** 直接增减账户余额 */
    suspend fun updateAccountBalance(accountId: Long, delta: Double) = accountDao.updateBalance(accountId, delta)

    /**
     * 账户余额校准并可选生成“漏记款”平账记录
     *
     * @param newAccount 修改后的新账户信息（包含新余额）
     * @param oldBalance 修改前的原始余额
     * @param saveAsMissedRecord 是否将差额作为一笔“漏记款”记入明细
     */
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
            // 直接插入明细表，避免重复调整账户余额（账户余额已在 updateAccount 中更新为最新值）
            expenseDao.insertExpense(discrepancyExpense)
        }
    }
}

