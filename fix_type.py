import json

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for d in data:
    cat = d['category']
    if cat == "资金流转" or cat == "报销款" or cat == "应收款" or cat == "公积金" or cat == "销售款":
        d['type'] = "EXPENSE"
    elif cat == "兼职外快" or cat == "红包" or cat == "转账":
        d['type'] = "INCOME"

with open('app/src/main/assets/initial_expenses.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

