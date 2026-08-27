import json

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

income_count = 0
expense_count = 0
for d in data:
    if "余额调整" in d.get('note', ''):
        if d['type'] == 'INCOME':
            income_count += 1
        elif d['type'] == 'EXPENSE':
            expense_count += 1
        else:
            print("OTHER:", d['type'])

print(f"INCOME: {income_count}, EXPENSE: {expense_count}")
