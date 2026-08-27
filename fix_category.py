import re

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I want to add CategoryItem(name = "漏记款", type = "EXPENSE", defaultSubcategories = listOf("漏记款")),
# if it doesn't already exist for EXPENSE.
if 'CategoryItem(name = "漏记款", type = "EXPENSE"' not in content:
    content = content.replace(
        '        CategoryItem(name = "人情", type = "EXPENSE", defaultSubcategories = listOf("请客", "送礼", "红包", "孝敬家长")),',
        '        CategoryItem(name = "人情", type = "EXPENSE", defaultSubcategories = listOf("请客", "送礼", "红包", "孝敬家长")),\n        CategoryItem(name = "漏记款", type = "EXPENSE", defaultSubcategories = listOf("漏记款")),'
    )
    
if 'CategoryItem(name = "漏记款", type = "INCOME"' not in content:
    content = content.replace(
        '        CategoryItem(name = "兼职", type = "INCOME", defaultSubcategories = listOf("外包", "自媒体", "奖金")),',
        '        CategoryItem(name = "兼职", type = "INCOME", defaultSubcategories = listOf("外包", "自媒体", "奖金")),\n        CategoryItem(name = "漏记款", type = "INCOME", defaultSubcategories = listOf("漏记款")),'
    )

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed CategoryManager")
