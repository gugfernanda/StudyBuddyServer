from __future__ import annotations
import argparse, json, re, sys, traceback, unicodedata
from typing import Dict, List, Tuple
import pandas as pd, requests
from bs4 import BeautifulSoup

import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer,
                              encoding="utf-8",
                              errors="replace")
sys.stderr = io.TextIOWrapper(sys.stderr.buffer,
                              encoding="utf-8",
                              errors="replace")


# ---------- helpers ----------

def nfc(x):
    return unicodedata.normalize("NFC", x) if isinstance(x, str) else x

def strip_acc(text: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFKD", text) if not unicodedata.combining(c))

def norm_series_token(t: str) -> str:
    t = strip_acc(t.lower())
    t = re.sub(r"seria", "", t, flags=re.I)
    t = re.sub(r"[\s()]+", "", t)
    return t

# ---------- regex patterns ----------
TIME_PAT = re.compile(r"\d{1,2}\s*[–—-]\s*\d{1,2}")
ROOM_PAT = re.compile(r"\b([A-Z]{1,4}\d{2,3}[A-Z]{0,2})\b", re.I)
KW_PAT   = re.compile(r"curs|laborator|seminar|sport|\\(l\\)|\\(s\\)", re.I)
RO_DAYS  = ["LUNI","MARȚI","MIERCURI","JOI","VINERI","SÂMBĂTĂ","DUMINICĂ"]

# ---------- fetch ----------

def fetch(url: str) -> Dict[str, pd.DataFrame]:
    r = requests.get(url, timeout=20)
    r.raise_for_status()
    r.encoding = "utf-8"
    html = r.text
    soup = BeautifulSoup(html, "lxml")
    names = [li.text.strip() for li in soup.select("#sheet-menu li")]
    if not names:
        m = re.compile(r'name: "(.*?)"')
        s = soup.find("script", text=m)
        names = m.findall(s.text) if s else []
    tables = pd.read_html(html)
    return {names[i] if i < len(names) else f"Sheet{i+1}": t for i, t in enumerate(tables)}

# ---------- timetable helpers ----------

def first_time_row(df: pd.DataFrame) -> int:
    for i in range(len(df)):
        if df.iloc[i].astype(str).str.contains(TIME_PAT).any():
            return i
    return -1

def split_days(hours: List[int | None]) -> List[Tuple[int,int,str]]:
    starts, prev = [0], None
    for i, h in enumerate(hours[1:], 1):
        if h is not None and prev is not None and h < prev:
            starts.append(i)
        if h is not None:
            prev = h
    seg = []
    for k, s in enumerate(starts):
        e = starts[k+1]-1 if k+1 < len(starts) else len(hours)-1
        seg.append((s, e, RO_DAYS[k] if k < len(RO_DAYS) else f"ZI{k+1}"))
    return seg

def day_of(idx: int, seg: List[Tuple[int,int,str]]) -> str:
    for s,e,d in seg:
        if s<=idx<=e:
            return d
    return ""

# ---------- column filter ----------

def col_ok(hdr, grp: str, ser: str | None) -> bool:
    if not any(str(x).strip()==grp for x in hdr):
        return False
    if ser is None:
        return True
    arg = norm_series_token(ser)
    arg_letters = re.sub(r"^\d+", "", arg)
    for x in hdr:
        hx = norm_series_token(str(x))
        hx_letters = re.sub(r"^\d+", "", hx)
        if hx in {arg, arg_letters} or hx_letters in {arg, arg_letters}:
            return True
    return False

# ---------- title / description split ----------

def split_title_desc(raw: str) -> Tuple[str, str]:
    """Return (title, rest) where title is just course name.
    * Remove room code from raw first.
    * Cut at first '(' if present, **keeping parentheses** in rest.
    """
    no_room = ROOM_PAT.sub("", raw).strip()
    m = re.match(r"^(.*?)\s*(\(.*)$", no_room)
    if m:
        title = m.group(1).strip("-–— ").strip()
        rest  = m.group(2).strip()
    else:
        title = no_room
        rest = ""
    return title or no_room, rest

# ---------- iterate events ----------

def iter_events(col: pd.Series, labels: pd.Series, segments, args):
    n = len(col)
    i = 0
    while i < n:
        cell = str(col.iat[i]).strip()
        if not cell or cell.lower() in {"nan","none"}:
            i += 1
            continue

        blk_rows: List[int] = [i]
        texts: List[str] = [cell]

        # include identical rowspan lines
        j = i + 1
        while j < n and str(col.iat[j]).strip() == cell:
            blk_rows.append(j)
            j += 1

        # include following lines until next KW or blank
        while j < n:
            nxt = str(col.iat[j]).strip()
            if not nxt or nxt.lower() in {"nan","none"}:
                break
            if KW_PAT.search(nxt):
                break
            blk_rows.append(j)
            if nxt not in texts:
                texts.append(nxt)
            j += 1

        # prepare title + description
        title_raw = texts[0]
        title, extra_from_title = split_title_desc(title_raw)
        extras = [t for t in texts[1:] if t]

        room = None
        for t in texts:
            m_room = ROOM_PAT.search(t)
            if m_room:
                room = m_room.group(1)
                break

        desc_parts: List[str] = []
        if extra_from_title:
            desc_parts.append(extra_from_title)
        desc_parts.extend(extras)
        if room and all("sala" not in p.lower() for p in desc_parts):
            desc_parts.append(f"sala {room}")
        description = "; ".join(desc_parts)

        # times (no offset)
        m_start = re.match(r"(\d{2})-(\d{2})", labels.iat[blk_rows[0]])
        m_end   = re.match(r"(\d{2})-(\d{2})", labels.iat[blk_rows[-1]])
        if m_start and m_end:
            start_time = f"{int(m_start.group(1)):02d}:00"
            end_time   = f"{int(m_end.group(2)):02d}:00"
        else:
            start_time = end_time = ""

        print(json.dumps({
            "title": title,
            "description": description,
            "dayOfWeek": day_of(blk_rows[0], segments),
            "startDate": args.startDate,
            "endDate": args.endDate,
            "startTime": start_time,
            "endTime": end_time,
        }, ensure_ascii=False))

        i = j

# ---------- main ----------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("url"); ap.add_argument("sheet"); ap.add_argument("group")
    ap.add_argument("--series"); ap.add_argument("--startDate", required=True)
    ap.add_argument("--endDate", required=True)
    args = ap.parse_args()

    data = fetch(args.url)
    if args.sheet not in data:
        sys.exit("sheet not found")

    df = data[args.sheet].reset_index(drop=True).map(nfc)
    trow = first_time_row(df)
    if trow == -1:
        sys.exit("no time row")

    header = df.iloc[:trow]
    body = df.iloc[trow:].reset_index(drop=True)
    body.columns = pd.MultiIndex.from_arrays(header.values)
    body = body.map(nfc)

    time_col = next((i for i in range(body.shape[1]) if body.iloc[:40,i].astype(str).str.contains(TIME_PAT).any()), None)
    if time_col is None:
        sys.exit("no time column")

    raw_times = body.iloc[:, time_col].astype(str).str.replace(r"\s*[–—-]\s*", "-", regex=True).str.strip()
    labels = raw_times.apply(lambda s: f"{int(m.group(1)):02d}-{int(m.group(2)):02d}" if (m:=re.match(r"(\d{1,2})-(\d{1,2})", s)) else "")
    hours = [int(t.split("-")[0]) if t else None for t in labels]
    segments = split_days(hours)

    cols = [i for i,h in enumerate(body.columns) if col_ok(h, args.group, args.series)]
    if not cols:
        avail = [" / ".join(map(str,h)) for h in body.columns]
        sys.exit("Nu am găsit combinația grup/serie. Exista: "+str(avail))

    for col in cols:
        iter_events(body.iloc[:,col], labels, segments, args)

if __name__ == "__main__":
    try:
        main()
    except Exception:
        traceback.print_exc(); sys.exit(1)
