package com.smartrent.infra.repository;

/**
 * Row projection for
 * {@link ListingRepository#countPublicListingsByCategory}: a category id paired
 * with the number of publicly-visible listings it contains, produced by the
 * grouped homepage "by category" count query.
 */
public record CategoryListingCount(Long categoryId, Long total) {
}
