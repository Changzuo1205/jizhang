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

    // Grouping accounts - use recently used accounts
    val frequentAccounts = remember(filteredAccounts, recentAccountIds) {
        if (recentAccountIds.isNotEmpty()) {
            recentAccountIds.mapNotNull { id -> filteredAccounts.find { it.id == id } }.take(2)
        } else {
            filteredAccounts.take(2)
        }
    }

    val cashAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { it.type == "CASH" }
    }

    val bankAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { it.type == "BANK_CARD" || it.type == "BANK" || it.type == "CREDIT_CARD" }
    }

    val otherAccounts = remember(filteredAccounts) {
        filteredAccounts.filter { it.type != "CASH" && it.type != "BANK_CARD" && it.type != "BANK" && it.type != "CREDIT_CARD" }
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
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section 1: 常用
                    if (searchQuery.isBlank() && frequentAccounts.isNotEmpty()) {
                        item {
                            Text(
                                text = "常用",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = bgConfig.textTertiary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                frequentAccounts.forEach { acc ->
                                    val isSelected = acc.id == selectedAccountId
                                    val iconColor = getAccountIconColor(acc.type)
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
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getAccountIcon(acc.type),
                                                    contentDescription = null,
                                                    tint = iconColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = bgConfig.textPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "¥${String.format(Locale.CHINA, "%.2f", acc.balance)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = bgConfig.textSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: 现金
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

                    // Section 3: 储蓄卡/借记卡/银行卡
                    if (bankAccounts.isNotEmpty()) {
                        item {
                            AccountSectionHeader("储蓄卡 / 借记卡")
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

                    // Section 4: 网络钱包 / 其他
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
    val iconColor = getAccountIconColor(account.type)

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
                    imageVector = getAccountIcon(account.type),
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

fun getAccountIcon(type: String): ImageVector {
    return when (type) {
        "WECHAT" -> Icons.Default.Payment
        "ALIPAY" -> Icons.Default.CreditCard
        "BANK", "BANK_CARD" -> Icons.Default.AccountBalance
        "CREDIT_CARD" -> Icons.Default.CreditCard
        "CASH" -> Icons.Default.AttachMoney
        "INVESTMENT" -> Icons.Default.TrendingUp
        else -> Icons.Default.AccountBalanceWallet
    }
}

fun getAccountIconColor(type: String): Color {
    return when (type) {
        "WECHAT" -> Color(0xFF07C160)
        "ALIPAY" -> Color(0xFF1677FF)
        "BANK", "BANK_CARD" -> Color(0xFFEF4444)
        "CREDIT_CARD" -> Color(0xFFF59E0B)
        "CASH" -> Color(0xFF10B981)
        "INVESTMENT" -> Color(0xFF8B5CF6)
        else -> Color(0xFF6366F1)
    }
}
