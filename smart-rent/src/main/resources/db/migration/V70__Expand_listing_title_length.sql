-- Expand title column from VARCHAR(200) to VARCHAR(500) to allow longer listing titles
ALTER TABLE listings
    MODIFY COLUMN title VARCHAR(500) NOT NULL;

ALTER TABLE listing_drafts
    MODIFY COLUMN title VARCHAR(500);
