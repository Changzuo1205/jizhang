package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import com.example.ui.viewmodel.BudgetConfig
import com.example.ui.viewmodel.BudgetPeriod
import com.example.ui.viewmodel.BudgetProgressInfo
import java.util.Locale

@Composable
fun BudgetProgressBarCard(
    budgetProgress: BudgetProgressInfo,
    budgetConfig: BudgetConfig,
    onSelectPeriod: (BudgetPeriod) -> Unit,
    onUpdateBudgetLimits: (monthly: Double, quarterly: Double, yearly: Double) -> Unit,
    onOpenBudgetSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    val animatedProgress by animateFloatAsState(
        targetValue = budgetProgress.progressPercent.coerceIn(0f, 1f),
        label = "budgetProgress"
    )

    val isOver = budgetProgress.isOverBudget
    val warningColor = Color(0xFFEF4444)
    val safeColor = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan

    val barColor by animateColorAsState(
        targetValue = if (isOver) warningColor else if (budgetProgress.progressPercent > 0.8f) Color(0xFFF59E0B) else safeColor,
        label = "barColor"
    )

    // Borderless card that opens budget settings on direct click
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenBudgetSettings() }
            .testTag("budget_progress_card"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (isOver) {
            if (bgConfig.isLight) Color(0xFFFEF2F2).copy(alpha = 0.95f) else Color(0xFF3B151E).copy(alpha = 0.65f)
        } else {
            if (bgConfig.isLight) Color.White.copy(alpha = 0.90f) else Color(0xFF161F38).copy(alpha = 0.60f)
        },
        borderColor = Brush.linearGradient(
            listOf(Color.Transparent, Color.Transparent) // Borderless
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Main Info Row: Spent vs Budget & Remaining Daily Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${budgetProgress.period.title}进度",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = bgConfig.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "已用 ${(budgetProgress.progressPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.spentAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = bgConfig.textPrimary
                        )
                        Text(
                            text = " / ¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.budgetLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = bgConfig.textSecondary,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }

                // Right Status: Overspend Badge or 剩余日均
                if (isOver) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(warningColor.copy(alpha = 0.15f))
                            .border(1.dp, warningColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = warningColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "超支 ¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.overAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = warningColor
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "剩余日均",
                            style = MaterialTheme.typography.labelSmall,
                            color = bgConfig.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "¥${String.format(Locale.CHINA, "%,.2f", budgetProgress.remainingDailyAverage)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bgConfig.isLight) Color(0xFF059669) else Color(0xFF34D399)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Progress Bar (Clicking directly triggers onOpenBudgetSettings via parent card)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun BudgetEditDialog(
    config: BudgetConfig,
    onDismiss: () -> Unit,
    onConfirm: (monthly: Double, quarterly: Double, yearly: Double) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var monthlyText by remember { mutableStateOf(if (config.monthlyLimit > 0) config.monthlyLimit.toInt().toString() else "") }
    var quarterlyText by remember { mutableStateOf(if (config.quarterlyLimit > 0) config.quarterlyLimit.toInt().toString() else "") }
    var yearlyText by remember { mutableStateOf(if (config.yearlyLimit > 0) config.yearlyLimit.toInt().toString() else "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E293B),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "自定义预算限额",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "设置各周期的支出预算目标，超支时将醒目提醒",
                    style = MaterialTheme.typography.bodySmall,
                    color = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Monthly Input
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = { Text("月度预算限额 (元)") },
                    placeholder = { Text("例如 5000") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold, color = bgConfig.textPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Quarterly Input
                OutlinedTextField(
                    value = quarterlyText,
                    onValueChange = { quarterlyText = it },
                    label = { Text("季度预算限额 (元)") },
                    placeholder = { Text("例如 15000") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold, color = bgConfig.textPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Yearly Input
                OutlinedTextField(
                    value = yearlyText,
                    onValueChange = { yearlyText = it },
                    label = { Text("年度预算限额 (元)") },
                    placeholder = { Text("例如 60000") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold, color = bgConfig.textPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = bgConfig.textSecondary
                        )
                    ) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val m = monthlyText.toDoubleOrNull()
                            val q = quarterlyText.toDoubleOrNull()
                            val y = yearlyText.toDoubleOrNull()

                            if (m == null || m <= 0) {
                                errorText = "请输入有效的月度预算金额"
                                return@Button
                            }
                            val safeQ = q ?: (m * 3)
                            val safeY = y ?: (m * 12)

                            onConfirm(m, safeQ, safeY)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存设置", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
