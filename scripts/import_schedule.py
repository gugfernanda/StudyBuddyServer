#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# asigură stdout UTF-8 chiar pe Windows
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

import argparse, json, re, traceback, unicodedata, requests, pandas as pd
from bs4 import BeautifulSoup

# ════════════════════════════════════════════════════════════════════════════
def extract_google_sheets_data(url: str) -> dict[str, pd.DataFrame]:
    resp = requests.get(url)
    resp.raise_for_status()
    resp.encoding = "utf-8"
    html = resp.text

    soup = BeautifulSoup(html, "lxml")
    names = [li.text.strip() for li in soup.select("#sheet-menu li")]
    if not names:
        m = re.compile(r'name: "(.*?)"')
        scr = soup.find("script", text=m)
        if scr:
            names = m.findall(scr.text)

    tables = pd.read_html(html)
    return {names[i] if i < len(names) else f"Sheet{i+1}": df
            for i, df in enumerate(tables)}

# ════════════════════════════════════════════════════════════════════════════
def main() -> None:
    p = argparse.ArgumentParser(description="Import schedule from Google Sheets")
    p.add_argument("url")
    p.add_argument("sheet_name")
    p.add_argument("group")
    p.add_argument("--series")
    p.add_argument("--startDate", required=True)
    p.add_argument("--endDate",   required=True)
    args = p.parse_args()

    data = extract_google_sheets_data(args.url)
    if args.sheet_name not in data:
        sys.exit(f"Error: sheet '{args.sheet_name}' not found")

    df_raw = data[args.sheet_name].reset_index(drop=False)

    header = df_raw.iloc[0:2]
    body   = df_raw.iloc[2:].reset_index(drop=True)
    body.columns = pd.MultiIndex.from_arrays(header.values)
    body = body.applymap(lambda x: unicodedata.normalize("NFC", x)
    if isinstance(x, str) else x)

    # detectăm coloana cu ore
    time_pat = re.compile(r"\d{1,2}\s*[–—-]\s*\d{1,2}")
    time_col = next((i for i in range(body.shape[1])
                     if body.iloc[:30, i].astype(str).str.contains(time_pat).any()),
                    None)
    if time_col is None:
        sys.exit("No time column found")

    raw_times = (body.iloc[:, time_col].astype(str)
                 .str.replace(r"\s*[–—-]\s*", "-", regex=True)
                 .str.strip())

    def pad(slot: str) -> str:
        m = re.match(r"(\d{1,2})-(\d{1,2})", slot)
        return f"{int(m.group(1)):02d}-{int(m.group(2)):02d}" if m else ""

    time_labels = raw_times.apply(pad)

    # începuturile de zi pe baza reset-ului de oră
    hours = [int(t.split("-")[0]) if t else None for t in time_labels]
    day_starts = [0]
    prev = None
    for i, h in enumerate(hours[1:], 1):
        if h is not None and prev is not None and h < prev:
            day_starts.append(i)
        prev = h if h is not None else prev

    ro_days = ["LUNI", "MARȚI", "MIERCURI", "JOI", "VINERI",
               "SÂMBĂTĂ", "DUMINICĂ"]
    segments = []
    for k, s in enumerate(day_starts):
        e = day_starts[k+1] - 1 if k+1 < len(day_starts) else len(body) - 1
        segments.append((s, e, ro_days[k] if k < len(ro_days) else f"ZI{k+1}"))

    def day_of_row(r: int) -> str:
        for s, e, d in segments:
            if s <= r <= e:
                return d
        return ""

    # filtrăm coloanele pentru grupă / serie
    col_idxs = [i for i, (ser, grp) in enumerate(body.columns)
                if str(grp) == args.group and (args.series is None or str(ser) == args.series)]
    if not col_idxs:
        sys.exit("No matching column for group/series")

    for col in col_idxs:
        col_s = body.iloc[:, col]
        i, n = 0, len(col_s)
        while i < n:
            if pd.isna(col_s.iat[i]) or not str(col_s.iat[i]).strip():
                i += 1
                continue

            run, texts = [i], [str(col_s.iat[i]).strip()]
            j = i + 1
            while j < n and not pd.isna(col_s.iat[j]) and str(col_s.iat[j]).strip():
                run.append(j)
                texts.append(str(col_s.iat[j]).strip())
                j += 1

            uniq = []
            for t in texts:
                if t not in uniq:
                    uniq.append(t)
            title = uniq[0]
            description = ", ".join(uniq[1:]) if len(uniq) > 1 else ""

            m1 = re.match(r"(\d{2})-(\d{2})", time_labels.iat[run[0]])
            m2 = re.match(r"(\d{2})-(\d{2})", time_labels.iat[run[-1]])
            start = f"{m1.group(1)}:00" if m1 else ""
            end   = f"{m2.group(2)}:00" if m2 else ""

            payload = {
                "title":       title,
                "description": description,
                "dayOfWeek":         day_of_row(run[0]),
                "startDate":   args.startDate,
                "endDate":     args.endDate,
                "startTime":   start,
                "endTime":     end
            }

            # eliminăm newline-urile brute din texte
            for k in ("title", "description"):
                if isinstance(payload[k], str):
                    payload[k] = payload[k].replace("\n", " ").replace("\r", " ")

            print(json.dumps(payload, ensure_ascii=False))
            i = j

# ════════════════════════════════════════════════════════════════════════════
if __name__ == "__main__":
    try:
        main()
    except Exception:
        traceback.print_exc()
        sys.exit(1)
