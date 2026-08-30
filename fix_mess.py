with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'r') as f:
    content = f.read()

settings_dialog_code = """
            // ── 5. 设置键盘布局对话框 ───────────────────────────────────
            if (showSettingsDialog) {
                Dialog(onDismissRequest = { showSettingsDialog = false }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(paperBg)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("键盘布局", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = inkPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 布局 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        keyboardLayout = 0
                                        prefs.edit().putInt("keyboard_layout", 0).apply()
                                        showSettingsDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = keyboardLayout == 0,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = activeAccentColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("计算器标准布局", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = inkPrimary)
                                    Text("7-8-9 在上，1-2-3 在下", fontSize = 12.sp, color = inkSecondary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 布局 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        keyboardLayout = 1
                                        prefs.edit().putInt("keyboard_layout", 1).apply()
                                        showSettingsDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = keyboardLayout == 1,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = activeAccentColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("拨号盘布局", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = inkPrimary)
                                    Text("1-2-3 在上，7-8-9 在下", fontSize = 12.sp, color = inkSecondary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showSettingsDialog = false }) {
                                    Text("关闭", color = inkSecondary)
                                }
                            }
                        }
                    }
                }
            }"""

content = content.replace(settings_dialog_code, "")

with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'w') as f:
    f.write(content)
