import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

progress_bar = """                            LinearProgressIndicator(
                                progress = { animatedBudgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = budgetBarColor,
                                trackColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round
                            )"""

if progress_bar in content:
    # We want to add a Row below the progress bar
    # "本月结余" calculation: budgetLimit - spentAmount
    addition = """                            LinearProgressIndicator(
                                progress = { animatedBudgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = budgetBarColor,
                                trackColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "本月结余 ¥${String.format(java.util.Locale.CHINA, "%,.2f", budgetProgress.budgetLimit - budgetProgress.spentAmount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = bgConfig.textSecondary
                                )
                            }"""
    content = content.replace(progress_bar, addition)
    print("Patched budget progress bar to add '本月结余'")
else:
    print("Could not find progress bar to patch")

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
