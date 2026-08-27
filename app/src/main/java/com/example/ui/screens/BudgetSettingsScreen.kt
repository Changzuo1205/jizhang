package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import com.example.ui.viewmodel.BudgetConfig
import com.example.ui.viewmodel.BudgetPeriod
import com.example.ui.viewmodel.BudgetProgressInfo
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSettingsScreen(
    budgetConfig: BudgetConfig,
    budgetProgress: BudgetProgressInfo,
    thisMonthExpense: Double,
    onSelectPeriod: (BudgetPeriod) -> Unit,
    onUpdateBudgetLimits: (monthly: Double, quarterly: Double, yearly: Double) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware / gesture back navigation to return home
    BackHandler(enabled = true) {
        onBack()
    }

    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    var selectedPeriod by remember { mutableStateOf(budgetConfig.activePeriod) }
    var monthlyInput by remember { mutableStateOf(if (budgetConfig.monthlyLimit > 0) budgetConfig.monthlyLimit.toInt().toString() else "5000") }
    var quarterlyInput by remember { mutableStateOf(if (budgetConfig.quarterlyLimit > 0) budgetConfig.quarterlyLimit.toInt().toString() else "15000") }
    var yearlyInput by remember { mutableStateOf(if (budgetConfig.yearlyLimit > 0) budgetConfig.yearlyLimit.toInt().toString() else "60000") }

    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Quick presets for monthly budget
    val monthlyPresets = listOf(2000, 3000, 5000, 8000, 10000, 15000, 20000)

    val currentLimit = when (selectedPeriod) {
        BudgetPeriod.MONTH -> monthlyInput.toDoubleOrNull() ?: budgetConfig.monthlyLimit
        BudgetPeriod.QUARTER -> quarterlyInput.toDoubleOrNull() ?: budgetConfig.quarterlyLimit
        BudgetPeriod.YEAR -> yearlyInput.toDoubleOrNull() ?: budgetConfig.yearlyLimit
    }

    val daysLeftInMonth = remember {
        val cal = Calendar.getInstance()
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        (totalDays - currentDay + 1).coerceAtLeast(1)
    }

    val remainingMonthly = (monthlyInput.toDoubleOrNull() ?: 0.0) - thisMonthExpense
    val suggestedDailySpend = if (remainingMonthly > 0) remainingMonthly / daysLeftInMonth else 0.0

    GlassBackgroundWithGlow(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(8.dp))

                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (bgConfig.isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f))
                                .testTag("budget_settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = bgConfig.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "预算管理与设置",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = bgConfig.textPrimary
                            )
                            Text(
                                text = "合理规划支出 · 避免超额消费",
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }

                    // Top Tune Icon Badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Current Budget Health Status Card (Panoramic Glass Card, No Heavy Border)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = if (budgetProgress.isOverBudget) {
                        if (bgConfig.isLight) Color(0xFFFEF2F2).copy(alpha = 0.95f) else Color(0xFF3B151E).copy(alpha = 0.70f)
                    } else {
                        if (bgConfig.isLight) Color.White.copy(alpha = 0.90f) else Color(0xFF161F38).copy(alpha = 0.70f)
                    },
                    borderColor = Brush.linearGradient(
                        listOf(Color.Transparent, Color.Transparent) // Borderless aesthetic as requested
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (budgetProgress.isOverBudget) Color(0xFFEF4444).copy(alpha = 0.2f)
                                            else Color(0xFF6366F1).copy(alpha = 0.2f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (budgetProgress.isOverBudget) Icons.Default.Warning else Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = if (budgetProgress.isOverBudget) Color(0xFFEF4444) else (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedPeriod.title}实时健康度",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                            }

                            Text(
                                text = if (budgetProgress.isOverBudget) "⚠️ 已超支" else "正常良好",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetProgress.isOverBudget) Color(0xFFEF4444) else (if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { budgetProgress.progressPercent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (budgetProgress.isOverBudget) Color(0xFFEF4444) else if (budgetProgress.progressPercent > 0.8f) Color(0xFFF59E0B) else (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan),
                            trackColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Grid Info Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Spent
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Text(text = "已支出", style = MaterialTheme.typography.labelSmall, color = bgConfig.textTertiary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "¥${String.format(Locale.CHINA, "%,.1f", budgetProgress.spentAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary,
                                    maxLines = 1
                                )
                            }

                            // Limit
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Text(text = "预算目标", style = MaterialTheme.typography.labelSmall, color = bgConfig.textTertiary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "¥${String.format(Locale.CHINA, "%,.1f", currentLimit)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary,
                                    maxLines = 1
                                )
                            }

                            // Remaining
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (budgetProgress.isOverBudget) "超支额" else "剩余可用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (budgetProgress.isOverBudget) Color(0xFFEF4444) else bgConfig.textTertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "¥${String.format(Locale.CHINA, "%,.1f", if (budgetProgress.isOverBudget) budgetProgress.overAmount else budgetProgress.remainingAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (budgetProgress.isOverBudget) Color(0xFFEF4444) else (if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald),
                                    maxLines = 1
                                )
                            }
                        }

                        // Daily spending suggestion
                        if (selectedPeriod == BudgetPeriod.MONTH && remainingMonthly > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (bgConfig.isLight) Color(0xFFEEF2FF) else Color(0xFF312E81).copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "本月剩余 ${daysLeftInMonth} 天，建议每日开销不超过 ¥${String.format(Locale.CHINA, "%.1f", suggestedDailySpend)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Default Active Period Selector
                Text(
                    text = "主展示预算周期",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BudgetPeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        val selectColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) {
                                        if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.35f)
                                    } else {
                                        if (bgConfig.isLight) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.06f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) selectColor else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedPeriod = period
                                    onSelectPeriod(period)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = period.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) selectColor else bgConfig.textSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (period) {
                                        BudgetPeriod.MONTH -> "¥$monthlyInput"
                                        BudgetPeriod.QUARTER -> "¥$quarterlyInput"
                                        BudgetPeriod.YEAR -> "¥$yearlyInput"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) bgConfig.textPrimary else bgConfig.textTertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 3. Customize Budget Limits (Inputs)
                Text(
                    text = "设置各周期预算额度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "可手动输入具体数额，或点击下方快捷选项快速填入",
                    style = MaterialTheme.typography.bodySmall,
                    color = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly Input
                OutlinedTextField(
                    value = monthlyInput,
                    onValueChange = {
                        monthlyInput = it
                        // Auto calculate quarterly and yearly recommendations if empty or standard
                        val mVal = it.toDoubleOrNull()
                        if (mVal != null && mVal > 0) {
                            quarterlyInput = (mVal * 3).toInt().toString()
                            yearlyInput = (mVal * 12).toInt().toString()
                        }
                    },
                    label = { Text("月度预算限额 (元)", color = bgConfig.textSecondary) },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold, color = bgConfig.textPrimary, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Monthly Presets FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    monthlyPresets.forEach { preset ->
                        val isCurrent = monthlyInput == preset.toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) (if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.35f))
                                    else (if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.07f))
                                )
                                .clickable {
                                    monthlyInput = preset.toString()
                                    quarterlyInput = (preset * 3).toString()
                                    yearlyInput = (preset * 12).toString()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "¥$preset",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) (if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan) else bgConfig.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quarterly Input
                OutlinedTextField(
                    value = quarterlyInput,
                    onValueChange = { quarterlyInput = it },
                    label = { Text("季度预算限额 (元)", color = bgConfig.textSecondary) },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold, color = bgConfig.textPrimary, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Yearly Input
                OutlinedTextField(
                    value = yearlyInput,
                    onValueChange = { yearlyInput = it },
                    label = { Text("年度预算限额 (元)", color = bgConfig.textSecondary) },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold, color = bgConfig.textPrimary, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444)
                    )
                }

                if (saveSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = saveSuccessMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        val m = monthlyInput.toDoubleOrNull()
                        val q = quarterlyInput.toDoubleOrNull()
                        val y = yearlyInput.toDoubleOrNull()

                        if (m == null || m <= 0) {
                            errorMessage = "请输入有效的月度预算金额"
                            return@Button
                        }
                        val safeQ = q ?: (m * 3)
                        val safeY = y ?: (m * 12)

                        errorMessage = null
                        onSelectPeriod(selectedPeriod)
                        onUpdateBudgetLimits(m, safeQ, safeY)
                        saveSuccessMessage = "预算设置已成功保存！"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_budget_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存并应用预算", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
