-- V72: Create search suggestion telemetry tables
-- search_query_impressions: records every call to the suggestions endpoint
-- search_suggestion_clicks: records every user click on a returned suggestion
-- No FK between the two tables to keep insert throughput high (async-safe)

CREATE TABLE search_query_impressions (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    query_norm       VARCHAR(256)  NOT NULL,
    province_id      VARCHAR(20)   NULL,
    category_id      BIGINT        NULL,
    suggestion_count INT           NOT NULL DEFAULT 0,
    client_ip        VARCHAR(45)   NULL,
    session_id       VARCHAR(64)   NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_sqi_query_norm  (query_norm),
    INDEX idx_sqi_created_at  (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Records each call to /v1/listings/search-suggestions for analytics';

CREATE TABLE search_suggestion_clicks (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    impression_id    BIGINT        NULL     COMMENT 'Soft-ref to search_query_impressions.id',
    suggestion_type  VARCHAR(20)   NOT NULL COMMENT 'TITLE | LOCATION | POPULAR_QUERY',
    suggestion_text  VARCHAR(512)  NOT NULL,
    listing_id       BIGINT        NULL,
    rank_position    INT           NOT NULL DEFAULT 0 COMMENT '0-indexed position in result list',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ssc_impression_id (impression_id),
    INDEX idx_ssc_type_text     (suggestion_type, suggestion_text(64)),
    INDEX idx_ssc_created_at    (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = 'Records user clicks on search suggestions for popularity ranking';
