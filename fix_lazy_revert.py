import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I will revert the entire `LazyColumn` and `stickyHeader` changes!

old_lazy_1 = """            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {"""
new_lazy_1 = """            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {"""
content = content.replace(old_lazy_1, new_lazy_1)

# I also added `Column(modifier = Modifier.padding(horizontal = 16.dp)) {` in Header Area.
old_header_area = """                // Header Area
                item {
                  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.statusBarsPadding())"""
new_header_area = """                // Header Area
                item {
                    Spacer(modifier = Modifier.statusBarsPadding())"""
content = content.replace(old_header_area, new_header_area)

# And I added `}` before `// 1. Unified Panoramic Month Expense`
old_closing = """                  }
                }
                // 1. Unified Panoramic Month Expense"""
new_closing = """                }
                // 1. Unified Panoramic Month Expense"""
content = content.replace(old_closing, new_closing)

# For stickyHeader, revert back to normal `fillMaxWidth()` and no bleed,
# but because LazyColumn has 16.dp padding, we DO need the bleed if we want edge-to-edge background!
# The user said: "收支明细记录区域显示回退到上一版本，注意收支明细记录文字和下方按钮位置不变，知识他们的背景宽度与屏幕一致"
# Wait, "注意收支明细记录文字和下方按钮位置不变，知识他们的背景宽度与屏幕一致"
# This means THEY DO WANT the background to span the whole screen width, but the text to maintain the 16dp margins!

# If LazyColumn has 16.dp padding, its width is screen - 32dp.
# To make the background span the whole screen, we can use `requiredWidth` + `offset` trick OR `drawBehind` bleed.
# Let's fix stickyHeader using `drawBehind` bleed, but since LazyColumn has 16.dp padding, 
# the `Column` inside `stickyHeader` inherently has 16.dp margins from the screen edges!
# So we don't need any padding on the `Column` itself, we just need `drawBehind` to bleed out -16.dp left and right.

old_sticky_col = """                    Column(
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

new_sticky_col = """                    Column(
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
# So stickyHeader doesn't need to change if I just revert LazyColumn padding!
# Wait! If LazyColumn has `padding(horizontal = 16.dp)`, then `fillMaxWidth()` inside `stickyHeader` gives it the width of (screenWidth - 32.dp).
# The content inside it (text, buttons) will naturally align with the bounds of this Column, which is 16.dp from the screen edge.
# The `drawBehind` will draw a rect from x = -16.dp, with width = size.width + 32.dp. This perfectly covers the screen edges!

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Reverted LazyColumn padding")
