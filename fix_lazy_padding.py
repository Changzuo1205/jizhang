import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add LocalConfiguration import if not exists
if 'import androidx.compose.ui.platform.LocalConfiguration' not in content:
    content = content.replace('import androidx.compose.ui.platform.LocalDensity', 'import androidx.compose.ui.platform.LocalDensity\nimport androidx.compose.ui.platform.LocalConfiguration')

# Add val screenWidth = LocalConfiguration.current.screenWidthDp.dp inside ExpenseScreen
if 'val screenWidth = LocalConfiguration.current.screenWidthDp.dp' not in content:
    content = content.replace('    val listState = rememberLazyListState()', '    val listState = rememberLazyListState()\n    val screenWidth = LocalConfiguration.current.screenWidthDp.dp')

old_header_col = """                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(stickyBgColor)
                    ) {"""

new_header_col = """                    Column(
                        modifier = Modifier
                            .requiredWidth(screenWidth)
                            .offset(x = (-16).dp)
                            .background(stickyBgColor)
                            .padding(horizontal = 16.dp)
                    ) {"""

content = content.replace(old_header_col, new_header_col)

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Applied bleed trick to sticky header")
