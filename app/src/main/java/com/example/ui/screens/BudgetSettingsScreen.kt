package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AmountFormatter
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.viewmodel.BudgetConfig
import com.example.ui.viewmodel.BudgetPeriod
import com.example.ui.viewmodel.BudgetProgressInfo
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

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
    BackHandler(enabled = true) {
        onBack()
    }

    val globalBgConfig = LocalAppBackgroundConfig.current
    val isLight = globalBgConfig.isLight

    // 配色令牌（与 EditorialPreviewScreen 严格统一）
    val canvasBg = if (isLight) Color(0xFFFAFAF7) else Color(0xFF242E24)
    val dividerColor = if (isLight) Color(0xFFE4DFD3) else Color(0xFF374637)
    val cardBg = if (isLight) Color(0xFFF2EFE8) else Color(0xFF1B231B)
    val inkPrimary = if (isLight) Color(0xFF141414) else Color(0xFFFAFAF7)
    val inkSecondary = if (isLight) Color(0xFF5A5852) else Color(0xFFB5B3AA)
    val inkMuted = if (isLight) Color(0xFF8A8780) else Color(0xFF889689)
    val clayAccent = Color(0xFFC4623D)
    val forestGreen = if (isLight) Color(0xFF2D6A4F) else Color(0xFF52B788)
    val warningAmber = Color(0xFFD97706)

    var selectedPeriod by remember { mutableStateOf(budgetConfig.activePeriod) }
    var monthlyInput by remember { mutableStateOf(if (budgetConfig.monthlyLimit > 0) budgetConfig.monthlyLimit.toInt().toString() else "5000") }
    var quarterlyInput by remember { mutableStateOf(if (budgetConfig.quarterlyLimit > 0) budgetConfig.quarterlyLimit.toInt().toString() else "15000") }
    var yearlyInput by remember { mutableStateOf(if (budgetConfig.yearlyLimit > 0) budgetConfig.yearlyLimit.toInt().toString() else "60000") }

    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    val isOver = budgetProgress.isOverBudget
    val animatedProgress by animateFloatAsState(
        targetValue = budgetProgress.progressPercent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "budget_setting_progress"
    )
    val progressColor = if (isOver) clayAccent else if (budgetProgress.progressPercent > 0.8f) warningAmber else inkPrimary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color(0xFFECE7DC) else Color(0xFF1F291F))
                            .testTag("budget_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = inkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Budget",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal,
                            color = inkPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "合理规划收支 · 保持财务从容",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = inkMuted,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0xFFECE7DC) else Color(0xFF1F291F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = clayAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            Spacer(modifier = Modifier.height(18.dp))

            // 2. Budget Health Status Card (报刊手帐风格卡片)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(0.5.dp, dividerColor, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPeriod.title.uppercase(Locale.getDefault())} HEALTH",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = inkMuted
                        )

                        Text(
                            text = if (isOver) "⚠️ 已超支" else "● 状态良好",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOver) clayAccent else forestGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "已用金额",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = inkMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(budgetProgress.spentAmount))}",
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Normal,
                                color = inkPrimary,
                                letterSpacing = (-0.8).sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "目标上限 ¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(currentLimit))}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = inkSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已用 ${(budgetProgress.progressPercent * 100).toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = progressColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(dividerColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(3.dp)
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Stat Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 周期支出
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFE9E4D8) else Color(0xFF242E24))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(text = "已支出", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = inkMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(budgetProgress.spentAmount))}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = inkPrimary,
                                maxLines = 1
                            )
                        }

                        // 预算目标
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFE9E4D8) else Color(0xFF242E24))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(text = "预算目标", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = inkMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(currentLimit))}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = inkPrimary,
                                maxLines = 1
                            )
                        }

                        // 剩余可用/超支
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFE9E4D8) else Color(0xFF242E24))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isOver) "超支额" else "剩余可用",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isOver) clayAccent else inkMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val amt = if (isOver) budgetProgress.overAmount else budgetProgress.remainingAmount
                            Text(
                                text = "¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(amt))}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) clayAccent else forestGreen,
                                maxLines = 1
                            )
                        }
                    }

                    if (selectedPeriod == BudgetPeriod.MONTH && remainingMonthly > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLight) Color(0xFFE5ECE7) else Color(0xFF1D2B22))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = forestGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "本月剩余 $daysLeftInMonth 天 · 建议每日开销不超过 ¥${AmountFormatter.formatCentsAsYuan(AmountFormatter.yuanToCents(suggestedDailySpend))}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = forestGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3. 周期选择 (Period Selector)
            Text(
                text = "DISPLAY PERIOD · 展示周期",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = inkMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BudgetPeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) clayAccent.copy(alpha = 0.12f)
                                else (if (isLight) Color(0xFFF2EFE8) else Color(0xFF1B231B))
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) clayAccent else dividerColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedPeriod = period
                                onSelectPeriod(period)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = period.title,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) clayAccent else inkSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (period) {
                                    BudgetPeriod.MONTH -> "¥$monthlyInput"
                                    BudgetPeriod.QUARTER -> "¥$quarterlyInput"
                                    BudgetPeriod.YEAR -> "¥$yearlyInput"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) inkPrimary else inkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. 额度配置 (Customize Limits)
            Text(
                text = "BUDGET LIMITS · 额度设置",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = inkMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "可手动输入具体数额，或点击下方快捷选项快速填入",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = inkMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Monthly Input
            OutlinedTextField(
                value = monthlyInput,
                onValueChange = {
                    monthlyInput = it
                    val mVal = it.toDoubleOrNull()
                    if (mVal != null && mVal > 0) {
                        quarterlyInput = (mVal * 3).toInt().toString()
                        yearlyInput = (mVal * 12).toInt().toString()
                    }
                },
                label = { Text("月度预算限额 (元)", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = inkMuted) },
                leadingIcon = { Text("¥", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = inkPrimary, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = inkPrimary,
                    unfocusedTextColor = inkPrimary,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = clayAccent,
                    unfocusedBorderColor = dividerColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Monthly Presets
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                monthlyPresets.forEach { preset ->
                    val isCurrent = monthlyInput == preset.toString()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) clayAccent.copy(alpha = 0.15f)
                                else (if (isLight) Color(0xFFECE7DC) else Color(0xFF1F291F))
                            )
                            .border(
                                width = if (isCurrent) 1.dp else 0.dp,
                                color = if (isCurrent) clayAccent else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                monthlyInput = preset.toString()
                                quarterlyInput = (preset * 3).toString()
                                yearlyInput = (preset * 12).toString()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "¥$preset",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) clayAccent else inkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quarterly Input
            OutlinedTextField(
                value = quarterlyInput,
                onValueChange = { quarterlyInput = it },
                label = { Text("季度预算限额 (元)", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = inkMuted) },
                leadingIcon = { Text("¥", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = inkPrimary, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = inkPrimary,
                    unfocusedTextColor = inkPrimary,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = clayAccent,
                    unfocusedBorderColor = dividerColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Yearly Input
            OutlinedTextField(
                value = yearlyInput,
                onValueChange = { yearlyInput = it },
                label = { Text("年度预算限额 (元)", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = inkMuted) },
                leadingIcon = { Text("¥", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = inkPrimary, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = inkPrimary,
                    unfocusedTextColor = inkPrimary,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = clayAccent,
                    unfocusedBorderColor = dividerColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = clayAccent
                )
            }

            if (saveSuccessMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = forestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = saveSuccessMessage ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = forestGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Save Button
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
                    containerColor = clayAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_budget_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "保存并应用预算",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
