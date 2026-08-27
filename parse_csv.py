import sys
import csv

def main():
    lines = sys.stdin.read().strip().split('\n')
    reader = csv.reader(lines)
    header = next(reader)
    
    # 0 UUID
    # 1 类型
    # 2 日期
    # 3 金额(元)
    # 4 备注
    # 5 账户
    # 6 对方账户
    # 7 分类
    
    res = []
    for row in reader:
        if len(row) < 24:
            continue
        # 类型
        if row[1] == '支出':
            type_str = 'EXPENSE'
        elif row[1] == '收入':
            type_str = 'INCOME'
        else:
            type_str = 'EXPENSE' # treating transfer as expense or we can skip? No, keep it as EXPENSE for now. Wait, or 'TRANSFER'? The app supports TRANSFER? ExpenseEntity says "EXPENSE" or "INCOME". We'll use EXPENSE for transfers but maybe set category to "转账".
            
        amt = float(row[3])
        note = row[4]
        acc_name = row[5]
        
        cat = row[7] if row[7] else '其他'
        if row[1] == '转账':
            cat = '转账'
            
        # timestamp is 23: 创建时间 (e.g. 1787580171000)
        # wait, let's just parse the date string (row 2) to get timestamp since '创建时间' might be creation time, not actual transaction time.
        # However, 2026-08-24 22:02:51
        date_str = row[2]
        
        # account id matching
        acc_id = 1
        if '支付宝' in acc_name:
            acc_id = 2
        elif '微信' in acc_name:
            acc_id = 1
        elif '招商' in acc_name:
            acc_id = 3
        elif '现金' in acc_name:
            acc_id = 5
        elif '基金' in acc_name:
            acc_id = 3 # map to bank
        elif '信用卡' in acc_name:
            acc_id = 4
        else:
            acc_id = 1 # default
            
        res.append({
            'type': type_str,
            'category': cat,
            'subCategory': '',
            'amount': amt,
            'note': note,
            'date_str': date_str,
            'accountId': acc_id,
            'accountName': acc_name
        })
        
    print(f"Parsed {len(res)} rows")
    
    # Generate Kotlin code
    # We will generate a JSON file to put in res/raw, then read it in DatabaseCallback
    import json
    with open('app/src/main/res/raw/initial_expenses.json', 'w', encoding='utf-8') as f:
        json.dump(res, f, ensure_ascii=False, indent=2)

if __name__ == '__main__':
    main()
