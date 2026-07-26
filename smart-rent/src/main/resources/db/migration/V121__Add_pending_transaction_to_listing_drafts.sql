-- V121: remember which payment a draft is currently waiting on.
--
-- Was authored as V118 on a branch cut before V118__Add_price_comparables_index landed
-- on main, and merged after it. Two files claiming version 118 is a hard Flyway
-- resolution error ("Found more than one migration with version 118") — not something
-- validate-on-migrate: false relaxes — so the application refused to start from the
-- moment both were on main, and every deploy since has kept serving the previous image.
-- Renumbered above the already-applied 118 rather than renumbering that one, which is
-- recorded in flyway_schema_history on every environment that booted before this broke.
--
-- publishDraft deliberately keeps the draft alive while a payment is pending, so an
-- abandoned payment doesn't lose the user's work. But nothing stopped the same draft
-- from being published again in the meantime, and that combination takes the user's
-- money for nothing:
--
--   1. publish with payment  -> draft kept, request cached, payment pending
--   2. publish again with quota -> listing created, media stamped with its listing_id,
--                                  draft deleted
--   3. the original payment lands -> the cached request is materialised, but
--      linkMediaToListing now rejects media that belongs to the listing from step 2.
--      The callback's error is swallowed by the IPN handler, so the provider is told
--      "success", the transaction stays COMPLETED, and no listing is ever created.
--
-- Holding the pending transaction id on the draft lets publishDraft refuse step 2
-- while step 1 is still outstanding.

ALTER TABLE listing_drafts
    ADD COLUMN pending_transaction_id VARCHAR(100) NULL AFTER benefit_ids;
