import json

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for d in data:
    if "余额调整" in d['note']:
        d['category'] = "漏记款"
        d['subCategory'] = "漏记款" # Just setting subcategory directly to 漏记款 so it shows up neatly
        # The user requested: 将“余额调整产生的差额”归为漏记款分类，在卡片中展示时仅显示二级分类
        # Let's ensure it shows "漏记款" in the card. Since the card logic we modified is:
        # text = if (expense.subCategory.isNotBlank()) expense.subCategory else expense.category
        # We can set subCategory to "余额调整" if they want, but the prompt says:
        # 将“余额调整产生的差额”归为漏记款分类，在卡片中展示时仅显示二级分类
        # If I set category="漏记款" and subCategory="余额调整", it will display "余额调整".
        # If I set category="漏记款" and subCategory="漏记款", it will display "漏记款".
        # I'll set subCategory to "漏记款" so it literally displays "漏记款".
        d['subCategory'] = "漏记款"

with open('app/src/main/assets/initial_expenses.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Fixed leakage subcategory")
