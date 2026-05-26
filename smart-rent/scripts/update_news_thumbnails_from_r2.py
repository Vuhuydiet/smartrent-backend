"""
Update news.thumbnail_url from R2 URLs grouped by category.

Input  : thumbnail_mapping.json  (produced by the scraper)
           {
             "NEWS":       ["https://cdn.../news/a.webp", "https://cdn.../news/b.webp", ...],
             "MARKET":     [...],
             "POLICY":     [...],
             "BLOG":       [...],
             "INVESTMENT": [...],
             "PROJECT":    [...],
             "GUIDE":      [...]
           }
Effect : UPDATE every row in `news`, rotating thumbnails within its category
         pool via (news_id % pool_size) so each category spreads evenly.

Usage  : pip install pymysql python-dotenv
         cp .env.example .env   # fill DB creds
         python update_news_thumbnails_from_r2.py thumbnail_mapping.json

Safety : deterministic, idempotent, dry-run supported via --dry-run.
"""

import argparse
import json
import os
import sys
from pathlib import Path

import pymysql
from dotenv import load_dotenv

VALID_CATEGORIES = {"NEWS", "MARKET", "POLICY", "BLOG", "INVESTMENT", "PROJECT", "GUIDE"}


def load_mapping(path: Path) -> dict[str, list[str]]:
    with open(path, "r", encoding="utf-8") as f:
        mapping = json.load(f)

    # Validate
    for cat, urls in mapping.items():
        if cat not in VALID_CATEGORIES:
            raise ValueError(f"Unknown category: {cat}. Valid: {VALID_CATEGORIES}")
        if not isinstance(urls, list) or len(urls) == 0:
            raise ValueError(f"Category {cat} has no URLs")
        for u in urls:
            if not u.startswith("http"):
                raise ValueError(f"Invalid URL in {cat}: {u}")

    missing = VALID_CATEGORIES - mapping.keys()
    if missing:
        print(f"[warn] no URLs for categories: {missing} — those rows will keep current thumbnail_url")

    return mapping


def connect():
    load_dotenv()
    return pymysql.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        database=os.getenv("DB_NAME", "smartrent"),
        charset="utf8mb4",
        autocommit=False,
    )


def update_category(cursor, category: str, urls: list[str], dry_run: bool) -> int:
    """
    Update every news row with the given category. Each row gets
    urls[news_id % len(urls)] → deterministic, even distribution.
    """
    cursor.execute(
        "SELECT news_id FROM news WHERE category = %s ORDER BY news_id",
        (category,),
    )
    rows = cursor.fetchall()
    pool_size = len(urls)
    updates = [(urls[nid % pool_size], nid) for (nid,) in rows]

    if dry_run:
        print(f"[dry-run] {category}: would update {len(updates)} rows across {pool_size} images")
        for url, nid in updates[:3]:
            print(f"          news_id={nid} -> {url}")
        return len(updates)

    cursor.executemany(
        "UPDATE news SET thumbnail_url = %s WHERE news_id = %s",
        updates,
    )
    print(f"[done] {category}: updated {len(updates)} rows across {pool_size} images")
    return len(updates)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mapping_file", type=Path, help="thumbnail_mapping.json")
    ap.add_argument("--dry-run", action="store_true", help="show what would change, don't write")
    args = ap.parse_args()

    mapping = load_mapping(args.mapping_file)
    total = 0

    conn = connect()
    try:
        with conn.cursor() as cur:
            for category, urls in mapping.items():
                total += update_category(cur, category, urls, args.dry_run)

        if args.dry_run:
            print(f"\n[dry-run] total rows that would be updated: {total}")
            conn.rollback()
        else:
            conn.commit()
            print(f"\n[committed] total rows updated: {total}")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"[error] {e}", file=sys.stderr)
        sys.exit(1)