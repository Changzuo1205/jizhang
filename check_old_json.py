import json

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for i in range(5):
    print(data[i])

print(f"Total records: {len(data)}")

# Count unique notes
notes = {}
for d in data:
    note = d['note']
    cat = d['category']
    if note not in notes:
        notes[note] = 0
    notes[note] += 1

# Print top notes
for k, v in sorted(notes.items(), key=lambda x: x[1], reverse=True)[:10]:
    print(f"Note: '{k}' -> {v} times")

