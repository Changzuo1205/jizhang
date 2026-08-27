import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

btn_start = content.find('                            // Circular Search Icon Button')
btn_end = content.find('                            // Toggle Category Stats Button')

if btn_start != -1 and btn_end != -1:
    search_button_code = content[btn_start:btn_end]
    content = content[:btn_start] + content[btn_end:]
else:
    print("Failed to extract button")
    exit(1)

box_start = content.find('                // Expandable Search Box')
box_end = content.find('                // 1. Unified Panoramic Month Expense')

if box_start != -1 and box_end != -1:
    search_box_code = content[box_start:box_end]
    content = content[:box_start] + content[box_end:]
else:
    print("Failed to extract box")
    exit(1)

# Modify search box code
search_box_code = search_box_code.replace('placeholder = {', 'placeholder = {')
search_box_code = search_box_code.replace(
    'Text(\n                                    "搜索分类、账户、金额或备注说明...",\n                                    color = bgConfig.textTertiary\n                                )',
    'Text(\n                                    "搜索分类、账户、金额或备注说明...",\n                                    color = bgConfig.textTertiary,\n                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),\n                                    maxLines = 1\n                                )'
)
# we need to remove the "item {" wrapper since we will place it inside the Column
search_box_code = search_box_code.replace('                // Expandable Search Box (Shown when search icon clicked or active query)\n                item {\n', '                    // Expandable Search Box\n')
# remove the closing brace for item {
search_box_code = search_box_code.rsplit('                    }\n                }\n', 1)[0] + '                    }\n'

# Find quick filter row
filter_row_end_str = '                                    }\n                                }'
filter_idx = content.find(filter_row_end_str, content.find('// Type filter chips'))

if filter_idx != -1:
    insertion_point = filter_idx + len(filter_row_end_str)
    content = content[:insertion_point] + '\n' + search_button_code + content[insertion_point:]
else:
    print("Failed to find filter row")
    exit(1)

# Find end of Quick Filter Buttons Row
# It is the end of the `Row(modifier = Modifier.fillMaxWidth()...)` block.
column_end_str = '                            }\n                        }\n                    }\n                }\n\n                // 6. Items List'
col_idx = content.find(column_end_str)
if col_idx != -1:
    content = content[:col_idx] + search_box_code + content[col_idx:]
else:
    print("Failed to find col end")
    exit(1)

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Search button and box patched!")
