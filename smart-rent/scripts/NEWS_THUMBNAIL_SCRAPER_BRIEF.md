# News Thumbnail Scraper — Handoff Brief

## Goal

Replace the current `news.thumbnail_url` values (broken Unsplash placeholders) with **real thumbnail images scraped from batdongsan.com.vn**, uploaded to our Cloudflare R2 bucket, then referenced back in the DB.

## Context

- `news` table contains ~1000 seeded rows (see `scripts/populate_news.sql`).
- Each row has a `category` enum: `NEWS`, `MARKET`, `POLICY`, `BLOG`, `INVESTMENT`, `PROJECT`, `GUIDE`.
- Current `thumbnail_url` points to Unsplash URLs that don't reliably load → need to be replaced with self-hosted images on R2.
- Existing DB migration: `src/main/resources/db/migration/V54__Create_news_table.sql` (PK is `news_id BIGINT`, column `thumbnail_url VARCHAR(500)`).

## Deliverables

1. A scraper script (Node/Python — your choice) that:
   - Crawls batdongsan.com.vn article listings per category.
   - Extracts thumbnail image URLs (hero/cover images from article cards).
   - Downloads images.
   - Uploads each image to R2 under key pattern: `news/thumbnails/{category}/{hash-or-uuid}.{ext}`
   - Emits a mapping file `thumbnail_mapping.json` of `{ category, r2_public_url }` entries — at least 10 per category for variety.
2. An `update_news_thumbnails.sql` that reads the mapping and rotates within each category pool by `news_id % pool_size`.

## batdongsan.com.vn Category Mapping

| SmartRent `category` | batdongsan.com.vn section (suggestion) |
|---|---|
| `NEWS` | https://batdongsan.com.vn/tin-tuc |
| `MARKET` | https://batdongsan.com.vn/phan-tich-nhan-dinh |
| `POLICY` | https://batdongsan.com.vn/chinh-sach-quan-ly |
| `BLOG` / `GUIDE` | https://batdongsan.com.vn/wiki-bat-dong-san or https://thanhnienviet.vn etc. |
| `INVESTMENT` | https://batdongsan.com.vn/tai-chinh-bat-dong-san |
| `PROJECT` | https://batdongsan.com.vn/du-an (project cover images) |

Confirm exact URLs before crawling — site structure changes.

## Scraping Guidelines

- **Respect rate limits**: sleep 1–2s between requests, set a realistic `User-Agent`.
- **Check `robots.txt`**: https://batdongsan.com.vn/robots.txt — only scrape allowed paths.
- **Image filters**: only JPEG/PNG/WebP, min width 400px, skip logos/icons, prefer landscape aspect ratio ~16:9.
- **Dedupe** by perceptual hash (phash) — don't upload the same image twice.
- **Target size per image**: resize to 800×450 WebP, quality 80 before upload to save R2 bandwidth.
- **Attribution**: if required by ToS, store source URL in a separate table or mapping file for reference.

## R2 Upload Details

Existing R2 service: `src/main/java/com/smartrent/infra/storage/R2StorageService.java` (reference only — scraper will use AWS SDK / boto3 / aws-sdk-js directly, not go through the Java backend).

**Credentials** (from `application.yml` → env vars):

```
R2_ENDPOINT           = https://<account>.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID
R2_SECRET_ACCESS_KEY
R2_BUCKET_NAME
R2_PUBLIC_BASE_URL    = https://cdn.smartrent.xxx   # public URL prefix for the bucket
R2_REGION             = auto
```

Ask the backend team for the actual values — do NOT commit them. Use a local `.env`.

**Storage key pattern** (matches existing convention):
```
news/thumbnails/{category_lowercase}/{uuid}.webp
```

**Public URL** to write back to DB:
```
{R2_PUBLIC_BASE_URL}/news/thumbnails/{category_lowercase}/{uuid}.webp
```

## DB Update Strategy

Once R2 upload is done, produce a SQL like:

```sql
UPDATE news
SET thumbnail_url = CASE category
    WHEN 'NEWS' THEN ELT(1 + (news_id % <N>),
        'https://cdn.smartrent.xxx/news/thumbnails/news/<uuid1>.webp',
        'https://cdn.smartrent.xxx/news/thumbnails/news/<uuid2>.webp',
        ...
    )
    WHEN 'MARKET' THEN ELT(1 + (news_id % <M>), ...)
    ...
END
WHERE news_id > 0;
```

MySQL Workbench runs in safe update mode — always include `WHERE news_id > 0` (or disable with `SET SQL_SAFE_UPDATES = 0;`).

Generate this SQL programmatically from `thumbnail_mapping.json` so pool sizes stay in sync with what was actually uploaded.

## Suggested Script Shape (pseudocode)

```python
# scrape.py
for category, listing_url in CATEGORY_MAP.items():
    image_urls = crawl_listing(listing_url, max_pages=5)       # extract <img> hero URLs
    for img_url in dedupe(image_urls)[:20]:                    # keep top 20 per category
        data = download(img_url)
        if not is_valid_image(data): continue
        webp = resize_to_webp(data, 800, 450, quality=80)
        key  = f"news/thumbnails/{category.lower()}/{uuid4()}.webp"
        upload_to_r2(key, webp, "image/webp")
        mapping[category].append(f"{R2_PUBLIC_BASE_URL}/{key}")

write_json("thumbnail_mapping.json", mapping)
write_sql("update_news_thumbnails_real.sql", mapping)
```

## Acceptance Criteria

- [ ] ≥10 unique real thumbnails uploaded to R2 per category (70+ total).
- [ ] All images publicly accessible via `R2_PUBLIC_BASE_URL`, return HTTP 200.
- [ ] Generated SQL updates all ~1000 news rows and runs cleanly in Workbench with safe-update-mode on.
- [ ] No duplicate images within the same category pool.
- [ ] No copyrighted logos/watermarks visible in the final images.

## Out of Scope

- Per-article image matching (we're rotating within category, not 1:1 match to content).
- Backend integration / admin UI for uploads (this is a one-shot data backfill).
- Content scraping (we only need images, not article text).

## Files the Scraper Dev Will Touch / Create

- `scripts/scraper/` (new directory) — scraper code.
- `scripts/scraper/thumbnail_mapping.json` — output mapping.
- `scripts/update_news_thumbnails_real.sql` — generated SQL to run against DB.

## Legal Note

Scraping batdongsan.com.vn for image reuse may conflict with their ToS. Confirm with product/legal before shipping to prod. If blocked, fallback plan: use Pexels/Pixabay API (free, licensed for commercial use) with real-estate search terms and keep the same R2 upload + DB update pipeline.