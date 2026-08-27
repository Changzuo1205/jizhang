#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
seed_converter.py — 交接包真实样本 → App 种子 JSON 转换管线（Phase 4）

输入（从 project-handover.zip 解包）:
    03-数据样本/bills-三表合并.csv   （20 列权威明细：类型/一级/二级/账户/对方账户/状态/UUID）
    03-数据样本/json/bills.json      （交叉校验源：uuid/is_deleted 权威）

输出:
    assets/seed_transactions.json    结构见 SEED_SCHEMA
    seed_audit_report.txt            转换对账审计（计数/总额分桶对照）

用法:
    python tools/seed_converter.py <三表合并csv路径> <bills.json路径> [输出目录]

语义规则（与计划一致）:
    1. 元 → Int 分（round(x*100)，全程 Decimal 避免 float 尾差）
    2. 日期+时间 按 Asia/Shanghai(+08:00) 固定偏移转 Unix 毫秒
       （历史数据即东八区产生，避免夏令时歧义）
    3. 类型映射:  支出→EXPENSE  收入→INCOME
                   转账→ counter_account 非空 = 真 TRANSFER（双端记录）
                          counter_account 为空 = 挖财资产调整，方向不可考，
                          一律按【资金入账】处理 → INCOME + 二级分类"漏记款"
                          并打 audit 标记 TODO_CONFIRM_DIRECTION=true，
                          共 N 笔需用户人工复核（占比极小，误判影响可通过
                          余额校准功能二次修正）
    4. 状态:   状态列 == "已删除" → is_deleted=true 保留导入
    5. 分类:   优先以 "二级分类"；未命中内置树则回退 "一级分类"；
               再未命中 → 其他；分类ID(挖财数字) 仅作审计参考不入库
    6. 账户:   名称直配种子八账户；空账户 → "现金"；泛称"银行卡"→"招商银行储蓄卡"
    7. 账本:   列值恒为日常账本 → bookUuid 占位符 "__DEFAULT_BOOK__"
               （运行时由 Seeder 替换成真实默认账本 id 对应 uuid）
"""
import csv
import json
import sys
from datetime import datetime, timezone, timedelta
from decimal import Decimal, ROUND_HALF_UP

TZ8 = timezone(timedelta(hours=8))
CST_EPOCH = datetime(1970, 1, 1, tzinfo=TZ8)

def yuan_to_cents(text):
    d = Decimal(str(text).strip().replace("¥", "").replace(",", "") or "0")
    return int((d * 100).quantize(Decimal("1"), rounding=ROUND_HALF_UP))

def to_millis(date_str, time_str):
    dt = datetime.strptime(f"{date_str} {time_str}", "%Y-%m-%d %H:%M:%S").replace(tzinfo=TZ8)
    return round((dt - CST_EPOCH).total_seconds() * 1000)

def map_account(raw_name):
    name = (raw_name or "").strip()
    if not name:
        return "现金", True          # 空账户归现金并打标
    if name in ("银行卡",):
        return "招商银行储蓄卡", False
    return name, False

def normalize_type(row):
    t = (row.get("类型") or "").strip()
    if t == "支出":
        return "EXPENSE", None
    if t == "收入":
        return "INCOME", None
    # 转账：按对方账户是否存在分流
    if (row.get("对方账户") or "").strip():
        return "TRANSFER", None
    return "INCOME", "TODO_CONFIRM_DIRECTION"

def main(csv_path, json_path, out_dir="."):
    stats = {"total": 0, "expense": 0, "income": 0, "transfer_real": 0,
             "adjust_as_income": 0, "deleted": 0, "empty_account": 0}

    # ---- 权威删除态集合（bills.json 的 uuid -> is_deleted）----
    deleted_uuids = set()
    with open(json_path, encoding="utf-8") as f:
        for b in json.load(f)["bills"]:
            if b.get("is_deleted"):
                deleted_uuids.add((b.get("uuid") or "").lower())

    rows_out = []
    audit_lines = ["== 转换审计 =="]
    with open(csv_path, encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader, start=2):          # 第1行为表头
            stats["total"] += 1
            rid = (row.get("UUID") or "").strip().lower()

            tx_type, warn = normalize_type(row)
            cents = yuan_to_cents(row.get("金额(元)", "0"))
            ts = to_millis(row["日期"], row.get("时间", "00:00:00"))
            account_name, flagged = map_account(row.get("账户"))

            cat1 = (row.get("一级分类") or "").strip() or "其他"
            cat2 = (row.get("二级分类") or "").strip()
            status_raw = (row.get("状态") or "").strip()
            is_del = (status_raw == "已删除") or (rid in deleted_uuids if rid else False)

            entry = {
                "uuid": rid or f"converted-{i}",
                "type": tx_type,
                "amount_cents": cents,
                "account": account_name,
                "counterAccount": (row.get("对方账户") or "").strip() or None,
                "category": cat1 if not cat2 else "",
                "subCategory": cat2 or "",
                "note": (row.get("备注") or "").strip(),
                "occurredAt": ts,
                "isDeleted": bool(is_del),
                "book": "__DEFAULT_BOOK__",
                "source": "wacai-import",
            }
            # 资产调整笔强制归漏记款口径
            if warn:
                tx_type = "INCOME"
                entry["type"] = "INCOME"
                entry["category"], entry["subCategory"] = "", "漏记款"
                stats["adjust_as_income"] += 1
                audit_lines.append(f"L{i} 转账(无对方)按入账处理: {row.get('日期')} {entry['amount_cents']/100:.2f}元 → {account_name}")

            if tx_type == "EXPENSE":
                stats["expense"] += 1
            elif tx_type == "INCOME":
                stats["income"] += 1
            else:
                stats["transfer_real"] += 1
            if entry["isDeleted"]:
                stats["deleted"] += 1
            if flagged:
                stats["empty_account"] += 1

            rows_out.append(entry)

    # 元字段展开：给主键 id（种子顺序稳定）
    for idx, e in enumerate(rows_out, start=1):
        e["seedId"] = idx

    out_json = f"{out_dir}/seed_transactions.json".replace("\\", "/")
    with open(out_json, "w", encoding="utf-8") as f:
        json.dump({"meta": {"generator": __file__, "count": len(rows_out)},
                   "transactions": rows_out}, f, ensure_ascii=False, indent=1)

    # 总额对账（有效行，排除已删除）
    alive = [e for e in rows_out if not e["isDeleted"]]
    exp_sum = sum(e["amount_cents"] for e in alive if e["type"] == "EXPENSE")
    inc_sum = sum(e["amount_cents"] for e in alive if e["type"] == "INCOME")
    trf_sum = sum(e["amount_cents"] for e in alive if e["type"] == "TRANSFER")
    audit_lines += [
        f"总解析行: {stats['total']}",
        f"支出: {stats['expense']}  收入: {stats['income']}  真转账: {stats['transfer_real']}",
        f"资产调整按入账: {stats['adjust_as_income']}  (需人工复核方向)",
        f"已删除保留: {stats['deleted']}  空账户→现金: {stats['empty_account']}",
        f"有效支出总额: {exp_sum/100:.2f} 元",
        f"有效转入额(漏记款口径): {inc_sum/100:.2f} 元",
        f"真转账总额: {trf_sum/100:.2f} 元",
    ]
    with open(f"{out_dir}/seed_audit_report.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(audit_lines))

    print("\n".join(audit_lines))
    print(f"\n输出: {out_json}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else ".")
