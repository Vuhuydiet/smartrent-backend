-- Reset search_text and title_norm to NULL so backfill runner can regenerate
-- with fixed Vietnamese đ/Đ normalization
-- After this migration, run backfill: application.search.backfill.enabled=true

UPDATE listings SET search_text = NULL, title_norm = NULL WHERE search_text IS NOT NULL OR title_norm IS NOT NULL;