package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.ui.theme.LocalAppBackgroundConfig
import java.util.Locale

@Composable
fun AccountPickerSheet(
    accounts: List<AccountEntity>,
    selectedAccountId: Long,
    recentAccountIds: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onSelectAccount: (AccountEntity) -> Unit,
    onAddAccount: (() -> Unit)? = null,
    accentColor: Color = Color(0xFFF97316)
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts
        else accounts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.cardSuffix.contains(searchQuery) }
    }

    // 常用账户分组：优先取近期高频使用的账户，展示最多 4 个
    val frequentAccounts = remember(filteredAccounts, recentAccountIds, searchQuery) {
        if (searchQuery.isNotBlank()) {
            emptyList()
        } else {
            val orderedByRecent = recentAccountIds.mapNotNull { id -> filteredAccounts.find { it.id == id } }
            if (orderedByRecent.isNotEmpty()) {
                orderedByRecent.distinctBy { it.id }.take(4)
            } else {
                filteredAccounts.take(4)
            }
        }
    }

    val onlineWalletAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { isOnlineWalletAccount(it.type, it.name) }
    }

    val bankAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { isBankCardAccount(it.type, it.name) && !isOnlineWalletAccount(it.type, it.name) }
    }

    val cashAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { isCashAccount(it.type, it.name) && !isOnlineWalletAccount(it.type, it.name) && !isBankCardAccount(it.type, it.name) }
    }

    val otherAccounts = remember(filteredAccounts) {
        filteredAccounts.filter {
            !isOnlineWalletAccount(it.type, it.name) &&
            !isBankCardAccount(it.type, it.name) &&
            !isCashAccount(it.type, it.name)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = bgConfig.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "选择账户",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )

                    IconButton(
                        onClick = { isSearching = !isSearching },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = bgConfig.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索账户名称或尾号", fontSize = 14.sp, color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgConfig.inputFieldBg,
                            unfocusedContainerColor = bgConfig.inputFieldBg,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = bgConfig.inputFieldBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section 1: 常用账户网格 (至多 4 个高频账户)
                    if (searchQuery.isBlank() && frequentAccounts.isNotEmpty()) {
                        item {
                            Text(
                                text = "常用账户",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = bgConfig.textTertiary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val chunked = frequentAccounts.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                chunked.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { acc ->
                                            val isSelected = acc.id == selectedAccountId
                                            val iconColor = getAccountIconColor(acc.type, acc.name)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        if (isSelected) accentColor.copy(alpha = 0.15f)
                                                        else if (bgConfig.isLight) Color(0xFFF8FAFC) else Color(0xFF282C37)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) accentColor else (if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable { onSelectAccount(acc) }
                                                    .padding(10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(iconColor.copy(alpha = 0.18f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = getAccountIcon(acc.type, acc.name),
                                                            contentDescription = null,
                                                            tint = iconColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = acc.name,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = bgConfig.textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "¥${String.format(Locale.CHINA, "%.2f", acc.balance)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isSelected) accentColor else bgConfig.textSecondary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: 网络钱包 (微信 / 支付宝)
                    if (onlineWalletAccounts.isNotEmpty()) {
                        item {
                            AccountSectionHeader("网络钱包")
                        }
                        items(onlineWalletAccounts, key = { it.id }) { acc ->
                            AccountRowItem(
                                account = acc,
                                isSelected = acc.id == selectedAccountId,
                                accentColor = accentColor,
                                onClick = { onSelectAccount(acc) }
                            )
                        }
                    }

                    // Section 3: 储蓄卡 / 信用卡
                    if (bankAccounts.isNotEmpty()) {
                        item {
                            AccountSectionHeader("银行卡 / 信用卡")
                        }
                        items(bankAccounts, key = { it.id }) { acc ->
                            AccountRowItem(
                                account = acc,
                                isSelected = acc.id == selectedAccountId,
                                accentColor = accentColor,
                                onClick = { onSelectAccount(acc) }
                            )
                        }
                    }

                    // Section 4: 现金
                    if (cashAccounts.isNotEmpty()) {
                        item {
                            AccountSectionHeader("现金")
                        }
                        items(cashAccounts, key = { it.id }) { acc ->
                            AccountRowItem(
                                account = acc,
                                isSelected = acc.id == selectedAccountId,
                                accentColor = accentColor,
                                onClick = { onSelectAccount(acc) }
                            )
                        }
                    }

                    // Section 5: 其他账户
                    if (otherAccounts.isNotEmpty()) {
                        item {
                            AccountSectionHeader("其他账户")
                        }
                        items(otherAccounts, key = { it.id }) { acc ->
                            AccountRowItem(
                                account = acc,
                                isSelected = acc.id == selectedAccountId,
                                accentColor = accentColor,
                                onClick = { onSelectAccount(acc) }
                            )
                        }
                    }

                    if (filteredAccounts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有找到匹配的账户",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = bgConfig.textTertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Add Account Action
                if (onAddAccount != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddAccount() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "添加账户",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "添加账户",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

private fun isCashAccount(type: String, name: String): Boolean {
    val t = type.uppercase()
    return t == "CASH" || t == "现金" || name.contains("现金")
}

private fun isBankCardAccount(type: String, name: String): Boolean {
    val t = type.uppercase()
    return t == "BANK_CARD" || t == "BANK" || t == "DEBIT_CARD" || t == "CREDIT_CARD" ||
            t == "储蓄卡" || t == "借记卡" || t == "信用卡" || t == "银行卡" ||
            name.contains("卡") || name.contains("银行") || name.contains("花呗")
}

private fun isOnlineWalletAccount(type: String, name: String): Boolean {
    val t = type.uppercase()
    return t == "WECHAT" || t == "ALIPAY" || t == "QQ" || t == "ONLINE" || t == "DIGITAL_WALLET" ||
            t == "微信" || t == "微信钱包" || t == "支付宝" || t == "网络账户" ||
            name.contains("微信") || name.contains("支付宝") || name.contains("云闪付")
}

@Composable
private fun AccountSectionHeader(title: String) {
    val bgConfig = LocalAppBackgroundConfig.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = bgConfig.textTertiary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun AccountRowItem(
    account: AccountEntity,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val iconColor = getAccountIconColor(account.type, account.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAccountIcon(account.type, account.name),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (account.cardSuffix.isNotBlank()) "${account.name} ${account.cardSuffix}" else account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = bgConfig.textPrimary
                )
                Text(
                    text = "¥${String.format(Locale.CHINA, "%,.2f", account.balance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) accentColor else bgConfig.textSecondary
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun getAccountIcon(type: String, name: String = ""): ImageVector {
    val t = type.uppercase()
    val n = name
    return when {
        t == "WECHAT" || t == "微信" || t == "微信钱包" || n.contains("微信") -> Icons.Default.Payment
        t == "ALIPAY" || t == "支付宝" || n.contains("支付宝") -> Icons.Default.CreditCard
        t == "BANK" || t == "BANK_CARD" || t == "DEBIT_CARD" || t == "储蓄卡" || t == "借记卡" || t == "银行卡" || n.contains("银行") -> Icons.Default.AccountBalance
        t == "CREDIT_CARD" || t == "信用卡" || n.contains("花呗") -> Icons.Default.CreditCard
        t == "CASH" || t == "现金" || n.contains("现金") -> Icons.Default.AttachMoney
        t == "INVESTMENT" || t == "投资" || t == "理财" || n.contains("基金") || n.contains("股票") -> Icons.Default.TrendingUp
        else -> Icons.Default.AccountBalanceWallet
    }
}

fun getAccountIconColor(type: String, name: String = ""): Color {
    val t = type.uppercase()
    val n = name
    return when {
        t == "WECHAT" || t == "微信" || t == "微信钱包" || n.contains("微信") -> Color(0xFF07C160)
        t == "ALIPAY" || t == "支付宝" || n.contains("支付宝") -> Color(0xFF1677FF)
        t == "BANK" || t == "BANK_CARD" || t == "DEBIT_CARD" || t == "储蓄卡" || t == "借记卡" || t == "银行卡" || n.contains("银行") -> Color(0xFFEF4444)
        t == "CREDIT_CARD" || t == "信用卡" || n.contains("花呗") -> Color(0xFFF59E0B)
        t == "CASH" || t == "现金" || n.contains("现金") -> Color(0xFF10B981)
        t == "INVESTMENT" || t == "投资" || t == "理财" -> Color(0xFF8B5CF6)
        else -> Color(0xFF6366F1)
    }
}
