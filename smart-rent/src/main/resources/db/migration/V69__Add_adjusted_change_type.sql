-- Add 'ADJUSTED' to pricing_histories.change_type ENUM.
-- ADJUSTED is used for synthetic interpolated data points inserted between
-- real price-change events to give charts a ~60-day cadence.

ALTER TABLE pricing_histories
    MODIFY COLUMN change_type
        ENUM('INITIAL','INCREASE','DECREASE','UNIT_CHANGE','CORRECTION','ADJUSTED')
        NOT NULL;
