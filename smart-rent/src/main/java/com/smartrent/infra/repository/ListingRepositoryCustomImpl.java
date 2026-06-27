package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.listing.cursor.ListingCursorSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Spring Data picks this up by the {@code <RepositoryName>Impl} naming convention
 * (same package as {@link ListingRepository}).
 */
public class ListingRepositoryCustomImpl implements ListingRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Listing> findByCursor(Specification<Listing> spec,
                                      List<ListingCursorSupport.CursorKey> keys,
                                      int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Listing> cq = cb.createQuery(Listing.class);
        Root<Listing> root = cq.from(Listing.class);

        // Reuse the exact same filter logic (and its address fetch / distinct) as
        // the offset search path — only the paging mechanism differs.
        Predicate p = spec.toPredicate(root, cq, cb);
        if (p != null) {
            cq.where(p);
        }
        cq.orderBy(ListingCursorSupport.orders(cb, root, keys));

        return em.createQuery(cq).setMaxResults(limit).getResultList();
    }
}
