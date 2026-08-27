import sys

def main():
    with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r') as f:
        content = f.read()

    # Find the target block to replace
    # From "// 4. Transactions List Header" to the end of "// 5. Quick Filter Buttons Row..." block

    start_str = """                // 4. Transactions List Header
                item {"""
    
    end_str = """                        }
                    }
                }

                // 6. Items List or Empty State"""

    start_idx = content.find(start_str)
    end_idx = content.find(end_str) + len(end_str)

    if start_idx == -1 or end_idx == -1:
        print("Could not find start or end index.")
        return

    replacement = """                // 4 & 5. Sticky Transactions List Header and Filters
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgConfig.solidColor.copy(alpha = 0.95f))
                    ) {
                        Spacer(modifier = Modifier.statusBarsPadding())
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedCalendarDay != null) "${selectedCalendarDay}日收支明细" else "收支明细记录",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                                if (selectedCalendarDay != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "· 清除筛选",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { selectedCalendarDay = null }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("clear_day_filter_btn")
                                    )
                                }
                            }
                            Text(
                                text = "共 ${displayedExpenses.size} 条",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textTertiary
                            )
                        }

                        // 5. Quick Filter Buttons Row (Positioned between transaction title and cards for filtering records)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Type filter chips
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GlassChip(
                                        selected = filterType == "ALL",
                                        onClick = { onSetFilterType("ALL") },
                                        selectedGlowColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                        modifier = Modifier.testTag("filter_type_all")
                                    ) {
                                        Text(
                                            text = "全部",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (filterType == "ALL") (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan) else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                    GlassChip(
                                        selected = filterType == "EXPENSE",
                                        onClick = { onSetFilterType("EXPENSE") },
                                        selectedGlowColor = colorScheme.expenseColor,
                                        modifier = Modifier.testTag("filter_type_expense")
                                    ) {
                                        Text(
                                            text = "支出",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (filterType == "EXPENSE") colorScheme.expenseColor else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                    GlassChip(
                                        selected = filterType == "INCOME",
                                        onClick = { onSetFilterType("INCOME") },
                                        selectedGlowColor = colorScheme.incomeColor,
                                        modifier = Modifier.testTag("filter_type_income")
                                    ) {
                                        Text(
                                            text = "收入",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (filterType == "INCOME") colorScheme.incomeColor else bgConfig.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Items List or Empty State"""

    new_content = content[:start_idx] + replacement + content[end_idx:]

    with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w') as f:
        f.write(new_content)

if __name__ == "__main__":
    main()
