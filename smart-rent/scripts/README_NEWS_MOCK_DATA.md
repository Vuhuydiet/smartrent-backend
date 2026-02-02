# News & Blog Mock Data Script

## Overview

This script generates **100 mock news and blog posts** for testing the News & Blog feature in SmartRent.

## What It Creates

### Data Distribution

- **50 NEWS posts** - Breaking news and updates about the rental market
- **50 BLOG posts** - Tips, advice, and lifestyle content

### Status Distribution

- **80% Published** (80 posts) - Visible to public users
- **15% Draft** (15 posts) - Work in progress, admin-only
- **5% Archived** (5 posts) - Archived content

### Features

- ✅ Realistic titles and content for each category
- ✅ SEO-optimized meta tags (title, description, keywords)
- ✅ URL-friendly slugs
- ✅ Varied view counts (0-5000+)
- ✅ Published dates spread over last 90 days
- ✅ 3 different admin authors
- ✅ Category-specific tags
- ✅ Rich HTML content with headings, lists, and paragraphs
- ✅ Placeholder thumbnail images from picsum.photos

## Prerequisites

1. **Database migration V54 must be run first** to create the `news` table
2. **At least one admin user** should exist (script creates 3 test admins if needed)

## How to Run

### Option 1: Using MySQL Command Line

```bash
# Navigate to the scripts directory
cd smart-rent/scripts

# Run the script
mysql -u your_username -p your_database_name < insert_news_mock_data.sql
```

### Option 2: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your database
3. Open the file `insert_news_mock_data.sql`
4. Click "Execute" (⚡ icon) or press `Ctrl+Shift+Enter`

### Option 3: Using DBeaver or Other GUI Tools

1. Open your SQL client
2. Connect to the SmartRent database
3. Open and execute `insert_news_mock_data.sql`

### Option 4: Using Docker (if database is in container)

```bash
# Copy script to container
docker cp smart-rent/scripts/insert_news_mock_data.sql mysql-container:/tmp/

# Execute script
docker exec -i mysql-container mysql -u root -p smartrent_db < /tmp/insert_news_mock_data.sql
```

## Expected Output

After running the script, you should see:

```
Admins created/verified
News and blog posts inserted successfully
=== NEWS & BLOG DATA INSERTION COMPLETE ===

Category breakdown:
+----------+-----------+-------+
| category | status    | count |
+----------+-----------+-------+
| BLOG     | ARCHIVED  |     2 |
| BLOG     | DRAFT     |     8 |
| BLOG     | PUBLISHED |    40 |
| NEWS     | ARCHIVED  |     3 |
| NEWS     | DRAFT     |     7 |
| NEWS     | PUBLISHED |    40 |
+----------+-----------+-------+

Summary:
- Total News Posts: 100
- Published Posts: 80
- Draft Posts: 15
- Archived Posts: 5
- Total Views: ~150,000+
```

## Verification

After running the script, verify the data:

```sql
-- Check total count
SELECT COUNT(*) FROM news;

-- Check by category
SELECT category, COUNT(*) FROM news GROUP BY category;

-- Check by status
SELECT status, COUNT(*) FROM news GROUP BY status;

-- View sample posts
SELECT news_id, title, category, status, view_count 
FROM news 
ORDER BY published_at DESC 
LIMIT 10;

-- Check slugs are unique
SELECT slug, COUNT(*) 
FROM news 
GROUP BY slug 
HAVING COUNT(*) > 1;
```

## Testing the API

Once the data is inserted, you can test the APIs:

### Public Endpoints

```bash
# Get all published news
curl http://localhost:8080/v1/news?page=1&size=20

# Filter by category
curl http://localhost:8080/v1/news?category=BLOG&page=1

# Search by keyword
curl http://localhost:8080/v1/news?keyword=rental&page=1

# Get news detail by slug
curl http://localhost:8080/v1/news/complete-guide-finding-your-perfect-rental-1-tips
```

### Admin Endpoints (requires authentication)

```bash
# Get all news (including drafts and archived)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/v1/admin/news?page=1&size=20

# Get news by ID
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/v1/admin/news/1
```

## Cleanup

To remove all mock data:

```sql
-- Delete all news posts
DELETE FROM news;

-- Reset auto-increment
ALTER TABLE news AUTO_INCREMENT = 1;

-- Optionally, remove test admins
DELETE FROM admins WHERE admin_id LIKE 'test-admin-%';
```

## Customization

You can modify the script to:

- Change the number of posts (modify `LIMIT 100`)
- Adjust status distribution (modify the CASE statement for status)
- Add more variety to titles (add more options to ELT functions)
- Change date ranges (modify `INTERVAL MOD(seq, 90) DAY`)
- Customize content templates

## Troubleshooting

### Error: Table 'news' doesn't exist

**Solution**: Run the migration first:
```bash
# Make sure V54__Create_news_table.sql has been executed
```

### Error: Duplicate entry for key 'slug'

**Solution**: The script generates unique slugs. If you run it multiple times, delete existing data first:
```sql
DELETE FROM news;
```

### Error: Foreign key constraint fails (author_id)

**Solution**: The script creates test admins automatically. If you still get this error, manually create an admin or modify the author_id values in the script.

## Notes

- **Thumbnail URLs** use picsum.photos placeholder images. Replace with real images in production.
- **View counts** are randomly generated. They don't reflect actual user views.
- **Published dates** are backdated to simulate historical content.
- **Content** is generic Lorem Ipsum-style text. Replace with real content for production.

## Support

For issues or questions, contact the backend development team.

---

**Last Updated**: 2024-01-15  
**Script Version**: 1.0  
**Compatible with**: SmartRent Backend v1.0+
