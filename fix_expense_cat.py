import re

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'CategoryItem(name = "漏记款", type = "EXPENSE"' not in content:
    content = content.replace(
        'CategoryItem(name = "人情", type = "EXPENSE", defaultSubcategories = listOf("请客", "送礼", "红包", "孝敬家长", "其他")),',
        'CategoryItem(name = "人情", type = "EXPENSE", defaultSubcategories = listOf("请客", "送礼", "红包", "孝敬家长", "其他")),\n        CategoryItem(name = "漏记款", type = "EXPENSE", defaultSubcategories = listOf("漏记款")),'
    )
    # in case the list was different:
    content = re.sub(
        r'(CategoryItem\(name = "人情".*?\),)',
        r'\1\n        CategoryItem(name = "漏记款", type = "EXPENSE", defaultSubcategories = listOf("漏记款")),',
        content
    )

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)
