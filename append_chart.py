import os

with open("app/src/main/java/com/example/ui/screens/AccountsScreen.kt", "r") as f:
    content = f.read()

# Add the new composable at the end
new_composable = """

@Composable
fun MonthlyIncomeExpenseStatisticsPanel(
    expenses: List<ExpenseEntity>,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var excludeCapitalFlow by remember { mutableStateOf(true) }

    val format = java.text.SimpleDateFormat("MM月", java.util.Locale.CHINA)
    
    val monthlyStats = remember(expenses, excludeCapitalFlow) {
        val stats = mutableMapOf<String, Pair<Double, Double>>() // "MM月" -> Pair(Income, Expense)
        
        // Initialize last 6 months to 0 to maintain order
        val cal = java.util.Calendar.getInstance()
        val monthsList = mutableListOf<String>()
        for (i in 5 downTo 0) {
            val m = cal.clone() as java.util.Calendar
            m.add(java.util.Calendar.MONTH, -i)
            val monthStr = format.format(m.time)
            monthsList.add(monthStr)
            stats[monthStr] = Pair(0.0, 0.0)
        }

        expenses.forEach { expense ->
            val isCapitalFlow = expense.category == "资金流转" || expense.category == "应收款" || expense.category == "报销款"
            if (!excludeCapitalFlow || !isCapitalFlow) {
                val monthStr = format.format(java.util.Date(expense.dateTimestamp))
                if (stats.containsKey(monthStr)) {
                    val current = stats[monthStr]!!
                    if (expense.type == "INCOME") {
                        stats[monthStr] = current.copy(first = current.first + expense.amount)
                    } else {
                        stats[monthStr] = current.copy(second = current.second + expense.amount)
                    }
                }
            }
        }
        
        monthsList.map { Pair(it, stats[it]!!) }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f),
        borderColor = Brush.linearGradient(
            if (bgConfig.isLight) listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)) 
            else listOf(Color.White.copy(alpha = 0.45f), GlowViolet.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))
        ),
        borderWidth = 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (bgConfig.isLight) Color(0xFF8B5CF6) else GlowViolet, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "月度收支统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "排除垫付/报销",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                    Checkbox(
                        checked = excludeCapitalFlow,
                        onCheckedChange = { excludeCapitalFlow = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (bgConfig.isLight) Color(0xFF8B5CF6) else GlowViolet,
                            uncheckedColor = bgConfig.textTertiary
                        ),
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar Chart
            val maxAmount = monthlyStats.maxOfOrNull { maxOf(it.second.first, it.second.second) }?.coerceAtLeast(1.0) ?: 1.0
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyStats.forEach { (month, amounts) ->
                    val (income, expense) = amounts
                    val incomeHeight = (income / maxAmount).toFloat()
                    val expenseHeight = (expense / maxAmount).toFloat()
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight().weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Income Bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(incomeHeight.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Expense Bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(expenseHeight.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = bgConfig.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("收入", style = MaterialTheme.typography.labelSmall, color = bgConfig.textSecondary)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(modifier = Modifier.size(8.dp).background(if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("支出", style = MaterialTheme.typography.labelSmall, color = bgConfig.textSecondary)
            }
        }
    }
}
"""

if "fun MonthlyIncomeExpenseStatisticsPanel" not in content:
    content += new_composable
    with open("app/src/main/java/com/example/ui/screens/AccountsScreen.kt", "w") as f:
        f.write(content)

