import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import androidx.compose.ui.draw.drawBehind' not in content:
    content = content.replace('import androidx.compose.ui.draw.clipToBounds', 'import androidx.compose.ui.draw.clipToBounds\nimport androidx.compose.ui.draw.drawBehind\nimport androidx.compose.ui.geometry.Offset\nimport androidx.compose.ui.geometry.Size')

old_header = """                    Column(
                        modifier = Modifier
                            .requiredWidth(screenWidth)
                            .offset(x = (-16).dp)
                            .background(stickyBgColor)
                            .padding(horizontal = 16.dp)
                    ) {"""

new_header = """                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(
                                    color = stickyBgColor,
                                    topLeft = Offset(-16.dp.toPx(), 0f),
                                    size = Size(size.width + 32.dp.toPx(), size.height)
                                )
                            }
                    ) {"""

content = content.replace(old_header, new_header)

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
