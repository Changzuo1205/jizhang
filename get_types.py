import json

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for d in data[:20]:
    if "余额调整" in d.get('note', ''):
        print(f"type: {d['type']}, amount: {d['amount']}, date: {d['date_str']}")
