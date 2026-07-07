package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.CategoryStatsRequest;
import com.smartrent.dto.response.CategoryListingStatsResponse;
import com.smartrent.infra.repository.CategoryListingCount;
import com.smartrent.infra.repository.CategoryRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Category;
import com.smartrent.infra.repository.entity.Listing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * POST /v1/listings/stats/categories used to run one
 * {@code count(ListingSpecification)} per category — N sequential COUNT
 * queries that made the endpoint ~20s on a cold cache. Locks in the fix: it
 * must count every category in a SINGLE grouped query
 * ({@link ListingRepository#countPublicListingsByCategory}) and never fall back
 * to the per-category {@code listingRepository.count(spec)} path.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceImplCategoryStatsTest {

    @Mock
    ListingRepository listingRepository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    ListingServiceImpl service;

    private Category category(long id) {
        return Category.builder()
                .categoryId(id)
                .name("Cat " + id)
                .slug("cat-" + id)
                .icon("icon-" + id)
                .build();
    }

    @Test
    void countsAllCategoriesInOneGroupedQueryAndDefaultsMissingToZero() {
        when(categoryRepository.findAllById(anyCollection()))
                .thenReturn(List.of(category(1), category(2), category(3)));
        // Grouped query only returns rows for categories that have listings;
        // category 3 is absent → the service must default it to 0.
        when(listingRepository.countPublicListingsByCategory(
                anyCollection(), any(), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new CategoryListingCount(1L, 10L),
                        new CategoryListingCount(2L, 4L)));

        CategoryStatsRequest request = CategoryStatsRequest.builder()
                .categoryIds(List.of(1L, 2L, 3L))
                .verifiedOnly(false)
                .build();

        List<CategoryListingStatsResponse> result = service.getCategoryStats(request);

        Map<Long, Long> totalById = result.stream()
                .collect(Collectors.toMap(
                        CategoryListingStatsResponse::getCategoryId,
                        CategoryListingStatsResponse::getTotalListings));

        assertEquals(3, result.size());
        assertEquals(10L, totalById.get(1L));
        assertEquals(4L, totalById.get(2L));
        assertEquals(0L, totalById.get(3L));

        // The fix: a single grouped query, never a per-category specification COUNT.
        verify(listingRepository).countPublicListingsByCategory(
                anyCollection(), any(), any(LocalDateTime.class));
        verify(listingRepository, never()).count(ArgumentMatchers.<Specification<Listing>>any());
    }

    @Test
    void verifiedOnlySkipsZeroCountCategories() {
        when(categoryRepository.findAllById(anyCollection()))
                .thenReturn(List.of(category(1), category(2)));
        when(listingRepository.countPublicListingsByCategory(
                anyCollection(), any(), any(LocalDateTime.class)))
                .thenReturn(List.of(new CategoryListingCount(1L, 7L)));

        CategoryStatsRequest request = CategoryStatsRequest.builder()
                .categoryIds(List.of(1L, 2L))
                .verifiedOnly(true)
                .build();

        List<CategoryListingStatsResponse> result = service.getCategoryStats(request);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCategoryId());
        assertEquals(7L, result.get(0).getTotalListings());
    }
}
