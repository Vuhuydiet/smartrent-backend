-- =====================================================
-- Mock Data Script for News & Blog Module
-- Generates 100 news and blog posts for testing
-- =====================================================

-- Ensure we have at least one admin user for author
-- If no admin exists, create a test admin
INSERT IGNORE INTO admins (admin_id, first_name, last_name, email, phone_code, phone_number, password, created_at, updated_at)
VALUES
    ('test-admin-001', 'Admin', 'User', 'admin@smartrent.vn', '+84', '901234567', '$2a$12$5EeWd/K8iKLk1UOw0AkfLOuzwIcA.uSx2nQhqvstoQUckLiXOgiim', NOW(), NOW()),
    ('test-admin-002', 'Content', 'Manager', 'content@smartrent.vn', '+84', '901234568', '$2a$12$5EeWd/K8iKLk1UOw0AkfLOuzwIcA.uSx2nQhqvstoQUckLiXOgiim', NOW(), NOW()),
    ('test-admin-003', 'Marketing', 'Team', 'marketing@smartrent.vn', '+84', '901234569', '$2a$12$5EeWd/K8iKLk1UOw0AkfLOuzwIcA.uSx2nQhqvstoQUckLiXOgiim', NOW(), NOW());

SELECT 'Admins created/verified' as status;

-- =====================================================
-- Generate 100 News & Blog Posts
-- =====================================================

SET @row := 0;
INSERT INTO news (
    title,
    slug,
    summary,
    content,
    category,
    tags,
    thumbnail_url,
    status,
    published_at,
    author_id,
    author_name,
    view_count,
    meta_title,
    meta_description,
    meta_keywords,
    created_at,
    updated_at
)
SELECT
    -- Title based on category and sequence
    CASE
        WHEN category_val = 'NEWS' THEN CONCAT('Breaking News: ', news_title, ' - Update #', seq)
        WHEN category_val = 'BLOG' THEN CONCAT('Blog: ', blog_title, ' - Part ', seq)
    END as title,

    -- Slug (URL-friendly version of title)
    CASE
        WHEN category_val = 'NEWS' THEN CONCAT('breaking-news-', LOWER(REPLACE(news_title, ' ', '-')), '-update-', seq)
        WHEN category_val = 'BLOG' THEN CONCAT('blog-', LOWER(REPLACE(blog_title, ' ', '-')), '-part-', seq)
    END as slug,

    -- Summary
    CASE
        WHEN category_val = 'NEWS' THEN CONCAT('Latest updates on ', news_title, '. Stay informed with our comprehensive coverage of the rental market developments.')
        WHEN category_val = 'BLOG' THEN CONCAT('In this blog post, we explore ', blog_title, ' and share practical insights for renters and landlords.')
    END as summary,
    
    -- Content (HTML)
    CONCAT(
        '<h1>Introduction</h1>',
        '<p>Welcome to this comprehensive article about ',
        CASE
            WHEN category_val = 'NEWS' THEN news_title
            WHEN category_val = 'BLOG' THEN blog_title
        END,
        '. This article provides valuable insights and information for our readers.</p>',
        '<h2>Key Points</h2>',
        '<ul>',
        '<li>Understanding the fundamentals and core concepts</li>',
        '<li>Practical tips and actionable advice</li>',
        '<li>Real-world examples and case studies</li>',
        '<li>Expert recommendations and best practices</li>',
        '<li>Future trends and what to expect</li>',
        '</ul>',
        '<h2>Detailed Analysis</h2>',
        '<p>The rental market in Vietnam has been experiencing significant changes in recent years. ',
        'With the rise of digital platforms like SmartRent, finding the perfect rental property has become easier than ever. ',
        'This article explores the various aspects of ',
        CASE
            WHEN category_val = 'NEWS' THEN news_title
            WHEN category_val = 'BLOG' THEN blog_title
        END,
        ' and provides you with the knowledge you need to make informed decisions.</p>',
        '<h3>Section 1: Background</h3>',
        '<p>Understanding the context is crucial. The Vietnamese rental market has unique characteristics that set it apart from other markets. ',
        'From pricing dynamics to legal requirements, there are many factors to consider when navigating this space.</p>',
        '<h3>Section 2: Current Situation</h3>',
        '<p>As of 2024, the market shows interesting trends. Urban areas like Hanoi and Ho Chi Minh City continue to see high demand, ',
        'while emerging cities are becoming increasingly attractive to renters seeking better value.</p>',
        '<h3>Section 3: Practical Advice</h3>',
        '<p>Here are some practical tips to help you succeed:</p>',
        '<ol>',
        '<li>Research thoroughly before making any decisions</li>',
        '<li>Compare multiple options to find the best fit</li>',
        '<li>Read reviews and testimonials from other users</li>',
        '<li>Understand your rights and responsibilities</li>',
        '<li>Use trusted platforms like SmartRent for safety</li>',
        '</ol>',
        '<h2>Conclusion</h2>',
        '<p>In conclusion, ',
        CASE
            WHEN category_val = 'NEWS' THEN news_title
            WHEN category_val = 'BLOG' THEN blog_title
        END,
        ' is an important topic that deserves attention. We hope this article has provided you with valuable insights and practical information. ',
        'Stay tuned to SmartRent for more updates and helpful content!</p>'
    ) as content,
    
    -- Category
    category_val as category,

    -- Tags (comma-separated)
    CASE
        WHEN category_val = 'NEWS' THEN 'news,rental,real-estate,vietnam,property,housing'
        WHEN category_val = 'BLOG' THEN 'blog,tips,advice,rental-tips,lifestyle,housing'
    END as tags,

    -- Thumbnail URL (using placeholder images)
    CONCAT('https://picsum.photos/seed/', seq, '/800/600') as thumbnail_url,

    -- Status (80% published, 15% draft, 5% archived)
    CASE
        WHEN MOD(seq, 20) = 0 THEN 'ARCHIVED'
        WHEN MOD(seq, 20) IN (1, 2, 3) THEN 'DRAFT'
        ELSE 'PUBLISHED'
    END as status,

    -- Published date (only for published and archived posts)
    CASE
        WHEN MOD(seq, 20) IN (1, 2, 3) THEN NULL
        ELSE DATE_SUB(NOW(), INTERVAL MOD(seq, 90) DAY)
    END as published_at,

    -- Author ID (rotate between 3 admins)
    CASE
        WHEN MOD(seq, 3) = 0 THEN 'test-admin-001'
        WHEN MOD(seq, 3) = 1 THEN 'test-admin-002'
        ELSE 'test-admin-003'
    END as author_id,

    -- Author Name
    CASE
        WHEN MOD(seq, 3) = 0 THEN 'Admin User'
        WHEN MOD(seq, 3) = 1 THEN 'Content Manager'
        ELSE 'Marketing Team'
    END as author_name,

    -- View count (random between 0 and 5000, higher for older posts)
    FLOOR(RAND() * 5000) + (100 - seq) * 20 as view_count,

    -- Meta title
    CONCAT(
        CASE
            WHEN category_val = 'NEWS' THEN CONCAT(news_title, ' - Latest News')
            WHEN category_val = 'BLOG' THEN CONCAT(blog_title, ' - SmartRent Blog')
        END,
        ' | SmartRent Vietnam'
    ) as meta_title,

    -- Meta description
    CASE
        WHEN category_val = 'NEWS' THEN CONCAT('Stay updated with the latest news about ', news_title, '. Read our comprehensive coverage and expert analysis.')
        WHEN category_val = 'BLOG' THEN CONCAT('Discover insights about ', blog_title, '. Practical tips and advice for renters and landlords in Vietnam.')
    END as meta_description,

    -- Meta keywords
    CASE
        WHEN category_val = 'NEWS' THEN 'rental news, real estate news, vietnam property, housing news, smartrent'
        WHEN category_val = 'BLOG' THEN 'rental blog, housing tips, rental advice, vietnam rental, property blog'
    END as meta_keywords,

    -- Created date (spread over last 90 days)
    created_date as created_at,

    -- Updated date (same as created for most, some updated later)
    CASE
        WHEN MOD(seq, 5) = 0 THEN DATE_ADD(created_date, INTERVAL MOD(seq, 30) DAY)
        ELSE created_date
    END as updated_at

FROM (
    SELECT
        (@row := @row + 1) AS seq,

        -- Category (50 NEWS, 50 BLOG)
        CASE
            WHEN @row <= 50 THEN 'NEWS'
            ELSE 'BLOG'
        END as category_val,

        -- News titles
        ELT(MOD(@row, 10) + 1,
            'Rental Market Surge in Hanoi',
            'New Housing Regulations Announced',
            'Student Housing Demand Increases',
            'Luxury Apartments See Price Drop',
            'Co-living Spaces Gain Popularity',
            'Rental Scams on the Rise',
            'Government Housing Support Program',
            'Foreign Investment in Real Estate',
            'Smart Home Technology Trends',
            'Affordable Housing Initiative'
        ) as news_title,

        -- Blog titles
        ELT(MOD(@row, 10) + 1,
            'Top 10 Rental Tips for Students',
            'How to Negotiate Rent Successfully',
            'Decorating Your Rental on a Budget',
            'Understanding Rental Contracts',
            'Moving Checklist for Renters',
            'Pet-Friendly Rental Guide',
            'Energy Saving Tips for Renters',
            'Dealing with Difficult Landlords',
            'First-Time Renter Mistakes',
            'Rental Insurance Explained'
        ) as blog_title,

        -- Created date (spread over 90 days)
        DATE_SUB(NOW(), INTERVAL MOD(@row, 90) DAY) as created_date

    FROM (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) t1,
    (
        SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) t2
    LIMIT 100
) data;

SELECT 'News and blog posts inserted successfully' as status;

-- =====================================================
-- Display Summary
-- =====================================================
SELECT '=== NEWS & BLOG DATA INSERTION COMPLETE ===' as message;

SELECT
    category,
    status,
    COUNT(*) as count
FROM news
GROUP BY category, status
ORDER BY category, status;

SELECT
    'Total News Posts' as metric,
    COUNT(*) as value
FROM news
UNION ALL
SELECT
    'Published Posts',
    COUNT(*)
FROM news
WHERE status = 'PUBLISHED'
UNION ALL
SELECT
    'Draft Posts',
    COUNT(*)
FROM news
WHERE status = 'DRAFT'
UNION ALL
SELECT
    'Archived Posts',
    COUNT(*)
FROM news
WHERE status = 'ARCHIVED'
UNION ALL
SELECT
    'Total Views',
    SUM(view_count)
FROM news;

SELECT '=== SAMPLE NEWS POSTS ===' as message;

SELECT
    news_id,
    title,
    category,
    status,
    view_count,
    author_name,
    DATE_FORMAT(published_at, '%Y-%m-%d') as published_date
FROM news
ORDER BY news_id
LIMIT 10;

SELECT 'Script execution completed successfully!' as final_message;

