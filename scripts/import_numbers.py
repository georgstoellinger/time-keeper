#!/usr/bin/env python3
"""Import time entries from an Apple Numbers workbook into Time Keeper.

Requires: pip install numbers-parser requests

The workbook uses monthly sheets. Each relevant sheet has a table with
"Date", "From", "To", "Type" and optionally "Details" columns.

Handling of spreadsheet quirks:
  * Rows with an empty Date cell belong to the most recent Date above them.
  * Only the time-of-day of From/To is used; their date part is unreliable.
  * Details cells are treated as day-level notes (deduplicated, joined).
  * Summary / pivot tables (no Date+From+To header) are ignored.

Usage:
  python scripts/import_numbers.py --file Timekeeping.numbers \
      --sheets "September 2026" "October 2026" "November 2026" --dry-run
"""

from __future__ import annotations

import argparse
import datetime as dt
import sys
from collections import defaultdict

try:
    from numbers_parser import Document
except ImportError:
    sys.exit("Missing dependency. Install with: pip install numbers-parser")

try:
    import requests
except ImportError:
    sys.exit("Missing dependency. Install with: pip install requests")


PALETTE = ["#4f46e5", "#16a34a", "#f59e0b", "#dc2626", "#0891b2", "#7c3aed"]


def as_date(value):
    if isinstance(value, dt.datetime):
        return value.date()
    if isinstance(value, dt.date):
        return value
    return None


def hhmm(value):
    if not isinstance(value, dt.datetime):
        return None
    return value.strftime("%H:%M")


def minutes_between(start: str, end: str) -> int:
    sh, sm = map(int, start.split(":"))
    eh, em = map(int, end.split(":"))
    return (eh * 60 + em) - (sh * 60 + sm)


def find_entry_table(sheet):
    for table in sheet.tables:
        rows = table.rows(values_only=True)
        if not rows:
            continue
        header = [str(c).strip() if c is not None else "" for c in rows[0]]
        if "Date" in header and "From" in header and "To" in header:
            return header, rows
    return None, None


def parse_sheet(sheet, warnings):
    header, rows = find_entry_table(sheet)
    if rows is None:
        return [], {}, {}

    idx = {name: header.index(name) for name in ("Date", "From", "To")}
    type_idx = header.index("Type") if "Type" in header else None
    details_idx = header.index("Details") if "Details" in header else None
    total_idx = next(
        (i for i, h in enumerate(header) if h.lower().startswith("total per day")),
        None,
    )

    entries = []
    day_details = defaultdict(list)
    declared_totals = {}
    current_date = None

    for row in rows[1:]:
        row_date = as_date(row[idx["Date"]])
        if row_date is not None:
            current_date = row_date
            if total_idx is not None:
                declared = row[total_idx]
                if isinstance(declared, (int, float)):
                    declared_totals[row_date.isoformat()] = float(declared)

        if details_idx is not None:
            raw = row[details_idx]
            if raw is not None and str(raw).strip():
                day_details[current_date].append(str(raw).strip())

        start = hhmm(row[idx["From"]])
        end = hhmm(row[idx["To"]])
        if start is None or end is None:
            continue
        if current_date is None:
            warnings.append(f"[{sheet.name}] entry without a date skipped: {start}-{end}")
            continue

        minutes = minutes_between(start, end)
        if minutes <= 0:
            warnings.append(
                f"[{sheet.name}] {current_date} invalid time {start}-{end} skipped"
            )
            continue

        type_name = None
        if type_idx is not None:
            raw_type = row[type_idx]
            if raw_type is not None and str(raw_type).strip():
                type_name = str(raw_type).strip()
        if not type_name:
            type_name = "Other"
            warnings.append(
                f"[{sheet.name}] {current_date} {start}-{end} missing Type, using Other"
            )

        entries.append(
            {
                "date": current_date.isoformat(),
                "start": start,
                "end": end,
                "minutes": minutes,
                "type": type_name,
            }
        )

    notes = {}
    for day, values in day_details.items():
        if day is None:
            continue
        seen = []
        for value in values:
            if value not in seen:
                seen.append(value)
        notes[day.isoformat()] = ", ".join(seen)

    return entries, notes, declared_totals


class Api:
    def __init__(self, base: str):
        self.base = base.rstrip("/")
        self.http = requests.Session()

    def _check(self, response):
        if not response.ok:
            raise RuntimeError(
                f"{response.request.method} {response.url} -> "
                f"{response.status_code}: {response.text}"
            )
        return response

    def categories(self):
        return self._check(self.http.get(f"{self.base}/api/categories")).json()

    def create_category(self, name, color):
        return self._check(
            self.http.post(f"{self.base}/api/categories", json={"name": name, "color": color})
        ).json()

    def day(self, date):
        return self._check(self.http.get(f"{self.base}/api/days/{date}")).json()

    def add_entry(self, date, payload):
        return self._check(
            self.http.post(f"{self.base}/api/days/{date}/entries", json=payload)
        ).json()

    def set_notes(self, date, notes):
        return self._check(
            self.http.put(f"{self.base}/api/days/{date}", json={"notes": notes})
        ).json()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", required=True, help="Path to the .numbers file")
    parser.add_argument("--api-base", default="http://localhost:8080", help="Backend base URL")
    parser.add_argument("--sheets", nargs="+", required=True, help="Sheet names to import")
    parser.add_argument("--dry-run", action="store_true", help="Parse and validate only")
    args = parser.parse_args()

    doc = Document(args.file)
    by_name = {s.name: s for s in doc.sheets}

    warnings = []
    all_entries = []
    all_notes = {}
    all_declared = {}

    for name in args.sheets:
        if name not in by_name:
            sys.exit(f"Sheet not found: {name}")
        entries, notes, declared = parse_sheet(by_name[name], warnings)
        all_entries.extend(entries)
        all_notes.update(notes)
        all_declared.update(declared)
        total = sum(e["minutes"] for e in entries)
        print(f"{name:16s} entries={len(entries):4d}  total={total / 60:7.2f} h")

    total_minutes = sum(e["minutes"] for e in all_entries)
    print(f"{'TOTAL':16s} entries={len(all_entries):4d}  total={total_minutes / 60:7.2f} h")

    computed = defaultdict(int)
    for e in all_entries:
        computed[e["date"]] += e["minutes"]
    mismatches = []
    for date, declared in sorted(all_declared.items()):
        diff = abs(computed.get(date, 0) / 60 - declared)
        if diff > 0.06:
            mismatches.append((date, computed.get(date, 0) / 60, declared, diff))

    if mismatches:
        print(f"\nDay-total mismatches ({len(mismatches)}):")
        for date, got, declared, diff in mismatches:
            print(f"  {date}: imported={got:.2f} sheet={declared:.2f} (diff {diff:.2f})")
    else:
        print("\nAll declared per-day totals match the imported entries.")

    if warnings:
        print(f"\nWarnings ({len(warnings)}):")
        for warning in warnings:
            print(f"  {warning}")

    if args.dry_run:
        print("\nDry run: no data written.")
        return

    api = Api(args.api_base)
    category_ids = {c["name"].lower(): c["id"] for c in api.categories()}
    created = 0

    def category_id(name):
        nonlocal created
        key = name.lower()
        if key not in category_ids:
            color = PALETTE[created % len(PALETTE)]
            category = api.create_category(name, color)
            category_ids[key] = category["id"]
            created += 1
            print(f"Created category '{category['name']}'")
        return category_ids[key]

    imported = skipped = 0
    for entry in all_entries:
        cid = category_id(entry["type"])
        existing = api.day(entry["date"])["entries"]
        signature = {
            (e["startTime"][:5], e["endTime"][:5], e["categoryId"]) for e in existing
        }
        if (entry["start"], entry["end"], cid) in signature:
            skipped += 1
            continue
        api.add_entry(
            entry["date"],
            {
                "startTime": entry["start"],
                "endTime": entry["end"],
                "categoryId": cid,
                "notes": None,
            },
        )
        imported += 1

    notes_written = 0
    for date, notes in all_notes.items():
        if not notes:
            continue
        if api.day(date).get("notes") != notes:
            api.set_notes(date, notes)
            notes_written += 1

    print(
        f"\nImported {imported} entries, skipped {skipped} duplicates, "
        f"set notes on {notes_written} days, created {created} categories."
    )


if __name__ == "__main__":
    main()
