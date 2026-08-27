import re

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'name = "漏记款",\n            type = "EXPENSE"' not in content:
    addition = """        CategoryItem(
            name = "漏记款",
            type = "EXPENSE",
            defaultSubcategories = listOf("漏记款")
        ),
"""
    content = content.replace('        CategoryItem(\n            name = "资金流转",', addition + '        CategoryItem(\n            name = "资金流转",')

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)
