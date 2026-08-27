import re

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove 漏记款 from 居家 defaultSubcategories
content = content.replace('"婚庆摄影", "漏记款", "生活其他"', '"婚庆摄影", "生活其他"')
content = content.replace('CategoryItem(name = "漏记款", type = "INCOME", defaultSubcategories = listOf("其他"))', 'CategoryItem(name = "漏记款", type = "INCOME", defaultSubcategories = listOf("漏记款"))')

with open('app/src/main/java/com/example/data/local/CategoryManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Cleaned cat")
