-- ============================================================
-- Update news.thumbnail_url with REAL Unsplash real-estate photos
-- Each row gets a themed real photo based on category + news_id
-- Photos rotate within each category pool → variety + realism
-- Safe to re-run (idempotent, deterministic per row)
-- ============================================================

UPDATE news
SET thumbnail_url = CONCAT(
    'https://images.unsplash.com/photo-',
    CASE category
        WHEN 'NEWS' THEN ELT(1 + (news_id % 8),
            '1560518883-ce09059eeffa',   -- city skyline
            '1486325212027-8081e485255e', -- modern apartment building
            '1545324418-cc1a3fa12c98',   -- housing complex aerial
            '1512917774080-9991f1c4c750', -- property exterior
            '1515263487990-61b07816b324', -- villa exterior
            '1568605114967-8130f3a36994', -- modern home
            '1564013799919-ab600027ffc6', -- house front
            '1480714378408-67cf0d13bc1b'  -- downtown cityscape
        )
        WHEN 'MARKET' THEN ELT(1 + (news_id % 6),
            '1611348586804-61bf6c080437', -- financial chart
            '1560179406-1dd927abb465',    -- business analysis
            '1466611653911-0628d5f2b9a3', -- market overview
            '1551836022-4196f3d39c3e',    -- analytics desk
            '1590283603385-17ffb3a7f29f', -- trading screens
            '1554224155-6726b3ff858f'     -- data report
        )
        WHEN 'POLICY' THEN ELT(1 + (news_id % 5),
            '1589939705384-5185137a7f0f', -- government building
            '1454165804606-c3d57bc86b40', -- signing contract
            '1450101499163-c8848c66ca85', -- law books
            '1505664194779-8beaceb93744', -- official document
            '1589391886645-d51941baf7fb'  -- courthouse
        )
        WHEN 'BLOG' THEN ELT(1 + (news_id % 8),
            '1583608205776-bfd35f0d9f83', -- cozy living room
            '1522708323590-d24dbb6b0267', -- modern bedroom
            '1600585154340-be6161a56a0c', -- bright apartment interior
            '1507209596998-b0b83e843df7', -- lifestyle / coffee table
            '1493809842364-78817add7ffb', -- luxury interior
            '1505843513577-22bb7d21e455', -- modern kitchen
            '1484154218962-a197022b5858', -- interior with view
            '1556020685-ae41abfc9365'     -- dining room
        )
        WHEN 'INVESTMENT' THEN ELT(1 + (news_id % 6),
            '1611348586804-61bf6c080437', -- finance chart
            '1560179406-1dd927abb465',    -- calculator / money
            '1486406146926-c627a92ad1ab', -- skyscraper investment
            '1466611653911-0628d5f2b9a3', -- growth chart
            '1579621970795-87facc2f976d', -- coins & calculator
            '1554224154-22dec7ec8818'     -- investment portfolio
        )
        WHEN 'PROJECT' THEN ELT(1 + (news_id % 7),
            '1486406146926-c627a92ad1ab', -- construction / tower
            '1512917774080-9991f1c4c750', -- completed property
            '1570129477492-45c003edd2be', -- modern tower
            '1545324418-cc1a3fa12c98',    -- aerial project view
            '1541123437800-1bb1317badc2', -- construction site
            '1590725140246-20acdee442be', -- residential project
            '1582407947304-fd86f028f716'  -- architecture render
        )
        ELSE -- GUIDE
            ELT(1 + (news_id % 5),
            '1454165804606-c3d57bc86b40', -- reading contract
            '1507209596998-b0b83e843df7', -- notepad / checklist
            '1450101499163-c8848c66ca85', -- paperwork
            '1554224155-8d04cb21cd6c',    -- planning / notes
            '1517245386807-bb43f82c33c4'  -- legal documents
        )
    END,
    '?w=800&h=450&fit=crop&auto=format&q=80'
)
WHERE news_id > 0;

-- Verify a sample per category
SELECT category, COUNT(*) AS cnt, MIN(thumbnail_url) AS sample_url
FROM news
GROUP BY category
ORDER BY category;