import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add rememberLazyListState to the imports if missing
if 'import androidx.compose.foundation.lazy.rememberLazyListState' not in content:
    content = content.replace('import androidx.compose.foundation.lazy.LazyColumn', 'import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.rememberLazyListState')

# Add val listState = rememberLazyListState() inside ExpenseScreen
if 'val listState = rememberLazyListState()' not in content:
    content = content.replace('    val shouldShowAddDialog = showLocalAddDialog || showAddDialogTrigger', '    val shouldShowAddDialog = showLocalAddDialog || showAddDialogTrigger\n    val listState = rememberLazyListState()')

# Pass listState to LazyColumn
content = content.replace('            LazyColumn(\n                modifier = Modifier\n                    .fillMaxSize()', '            LazyColumn(\n                state = listState,\n                modifier = Modifier\n                    .fillMaxSize()')

# Calculate alpha and apply it to the stickyHeader background
# Look for stickyHeader { Column( modifier = Modifier.fillMaxWidth().background(Color.Transparent)
# Note: we need to import animateFloatAsState if it's missing, but it's likely there.
# Let's replace the sticky header's background color logic
bg_replacement = """                // 4 & 5. Sticky Transactions List Header and Filters
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

# Replace old stickyHeader
pattern = re.compile(r'                // 4 & 5\. Sticky Transactions List Header and Filters\s*stickyHeader \{\s*Column\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.background\(Color\.Transparent\)\s*\) \{', re.DOTALL)
content = pattern.sub(bg_replacement, content)

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("LazyListState and stickyHeader patched!")
