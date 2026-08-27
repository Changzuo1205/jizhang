import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# We need to move pinnedAlpha and stickyBgColor INSIDE stickyHeader {
bad_code = """                // 4 & 5. Sticky Transactions List Header and Filters
                val pinnedAlpha by animateFloatAsState(
                    targetValue = if (listState.firstVisibleItemIndex >= 4) 0.95f else 0f,
                    label = "pinnedAlpha"
                )
                val stickyBgColor = if (bgConfig.isLight) Color.White.copy(alpha = pinnedAlpha) else Color(0xFF131C35).copy(alpha = pinnedAlpha)
                
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(stickyBgColor)
                    ) {"""

good_code = """                // 4 & 5. Sticky Transactions List Header and Filters
                stickyHeader {
                    val pinnedAlpha by animateFloatAsState(
                        targetValue = if (listState.firstVisibleItemIndex >= 4) 0.95f else 0f,
                        label = "pinnedAlpha"
                    )
                    val stickyBgColor = if (bgConfig.isLight) Color.White.copy(alpha = pinnedAlpha) else Color(0xFF131C35).copy(alpha = pinnedAlpha)
                
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(stickyBgColor)
                    ) {"""

content = content.replace(bad_code, good_code)

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed LazyColumn scope error")
