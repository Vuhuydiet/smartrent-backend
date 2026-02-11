package com.smartrent.service.listing;

import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "application.search.backfill", name = "enabled", havingValue = "true")
public class ListingSearchBackfillRunner implements ApplicationRunner {

    private final ListingRepository listingRepository;

    @Value("${application.search.backfill.batch-size:500}")
    private int batchSize;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting listing search backfill with batch size {}", batchSize);
        int page = 0;
        Page<Listing> batch;

        do {
            batch = listingRepository.findAll(PageRequest.of(page, batchSize));
            List<Listing> toUpdate = new ArrayList<>();

            for (Listing listing : batch.getContent()) {
                if (listing.getSearchText() == null || listing.getTitleNorm() == null) {
                    String addressText = listing.getAddress() != null
                            ? listing.getAddress().getDisplayAddress()
                            : null;
                    String descSnippet = listing.getDescription();
                    if (descSnippet != null && descSnippet.length() > 200) {
                        descSnippet = descSnippet.substring(0, 200);
                    }

                    StringBuilder sb = new StringBuilder();
                    if (listing.getTitle() != null && !listing.getTitle().isEmpty()) {
                        sb.append(listing.getTitle()).append(' ');
                    }
                    if (addressText != null && !addressText.isEmpty()) {
                        sb.append(addressText).append(' ');
                    }
                    if (descSnippet != null && !descSnippet.isEmpty()) {
                        sb.append(descSnippet);
                    }

                    String combined = sb.toString().trim();
                    listing.setTitleNorm(TextNormalizer.compact(listing.getTitle(), 256));
                    listing.setSearchText(TextNormalizer.compact(combined, 512));
                    toUpdate.add(listing);
                }
            }

            if (!toUpdate.isEmpty()) {
                listingRepository.saveAll(toUpdate);
                log.info("Backfilled {} listings on page {}", toUpdate.size(), page);
            }

            page++;
        } while (batch.hasNext());

        log.info("Listing search backfill completed");
    }
}
