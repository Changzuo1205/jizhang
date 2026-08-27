import json

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for d in data:
    cat = d['category']
    subcat = d['subCategory']
    
    if cat == "报销款":
        d['category'] = "资金流转"
        d['subCategory'] = "报销款"
    elif cat == "应收款":
        d['category'] = "资金流转"
        d['subCategory'] = "应收款"
    elif cat == "公积金":
        d['category'] = "资金流转"
        d['subCategory'] = "公积金"
    elif cat == "销售款":
        d['category'] = "资金流转"
        d['subCategory'] = "销售款"
        
    # Also in note
    if d['note'] == "综述" or d['note'] == "课本" or d['note'] == "多又佳" or d['note'] == "pbl报告" or d['note'] == "讲义" or d['note'] == "报告册":
        d['category'] = "资金流转"
        d['subCategory'] = "报销款"

with open('app/src/main/assets/initial_expenses.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Fixed capital flow categories")
