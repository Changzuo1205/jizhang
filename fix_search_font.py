import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

bad_placeholder = """                            placeholder = {
                                Text(
                                    "搜索分类、账户、金额或备注说明...",
                                    color = bgConfig.textTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },"""

# Use 10.sp to ensure it fits on all devices
good_placeholder = """                            placeholder = {
                                Text(
                                    "搜索分类、账户、金额或备注说明...",
                                    color = bgConfig.textTertiary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1
                                )
                            },"""

content = content.replace(bad_placeholder, good_placeholder)

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed search box font size")
