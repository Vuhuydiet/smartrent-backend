package com.smartrent.service.discovery;

import com.smartrent.infra.repository.entity.SearchQueryImpression;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists search-telemetry rows in their own short transaction, off the
 * request thread (invoked via {@link TelemetryExecutor}).
 *
 * <p>Separate Spring bean on purpose: the {@code @Transactional} proxy only
 * applies when the method is called through the injected bean, not via
 * self-invocation from {@code SearchSuggestionServiceImpl}.
 *
 * <p>{@link EntityManager#persist} (not {@code repository.save}) is used so an
 * impression carrying an application-assigned id is a single INSERT — no
 * select-before-insert that {@code save()} would do for a non-null id.
 */
@Slf4j
@Component
public class SearchTelemetryWriter {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Inserts one impression. The id MUST already be set by the caller
     * ({@link com.smartrent.util.SnowflakeId}). Never throws — a telemetry
     * write failure must not affect anything user-facing.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveImpression(SearchQueryImpression impression) {
        try {
            entityManager.persist(impression);
        } catch (Exception e) {
            log.warn("search-suggestions: impression persist failed (non-fatal): {}", e.getMessage(), e);
        }
    }
}
