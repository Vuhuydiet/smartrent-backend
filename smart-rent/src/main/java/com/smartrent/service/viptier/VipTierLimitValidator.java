package com.smartrent.service.viptier;

import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.VipTierDetailRepository;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.infra.repository.entity.VipTierDetail;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Centralised validator that enforces the max_images / max_videos limits
 * configured on each {@link VipTierDetail} tier.
 *
 * Used by listing create and update flows to refuse media attachments that would
 * push a listing past its VIP tier's media quota.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VipTierLimitValidator {

    VipTierDetailRepository vipTierDetailRepository;
    MediaRepository mediaRepository;

    /**
     * Validate the proposed media set for a listing about to be CREATED with
     * the given vipType. There is no existing media to count.
     */
    public void validateForCreate(String vipType, Collection<Long> proposedMediaIds) {
        if (proposedMediaIds == null || proposedMediaIds.isEmpty()) {
            return;
        }

        VipTierDetail tier = loadTier(vipType);

        int proposedImages = countByType(proposedMediaIds, Media.MediaType.IMAGE);
        int proposedVideos = countByType(proposedMediaIds, Media.MediaType.VIDEO);

        enforceLimits(tier, proposedImages, proposedVideos);
    }

    /**
     * Validate the proposed REPLACEMENT media set for an existing listing on update.
     * On update flow, existing media is unlinked first, so we only count the newly
     * attached media; combine with {@link #validateForCreate(String, Collection)}
     * semantics if the update preserves existing media.
     */
    public void validateForUpdate(Long listingId, String vipType, Collection<Long> newMediaIds,
                                  boolean replaceExisting) {
        VipTierDetail tier = loadTier(vipType);

        int existingImages = 0;
        int existingVideos = 0;
        if (!replaceExisting && listingId != null) {
            existingImages = (int) mediaRepository
                    .findByListing_ListingIdAndMediaTypeAndStatusOrderBySortOrderAsc(
                            listingId, Media.MediaType.IMAGE, Media.MediaStatus.ACTIVE)
                    .size();
            existingVideos = (int) mediaRepository
                    .findByListing_ListingIdAndMediaTypeAndStatusOrderBySortOrderAsc(
                            listingId, Media.MediaType.VIDEO, Media.MediaStatus.ACTIVE)
                    .size();
        }

        int proposedImages = countByType(newMediaIds, Media.MediaType.IMAGE);
        int proposedVideos = countByType(newMediaIds, Media.MediaType.VIDEO);

        enforceLimits(tier, existingImages + proposedImages, existingVideos + proposedVideos);
    }

    /**
     * Public helper for read endpoints that need the raw limits (e.g. so the
     * frontend can disable the upload button when the quota is reached).
     */
    public VipTierDetail loadTier(String vipType) {
        if (vipType == null || vipType.isBlank()) {
            throw new AppException(DomainCode.VIP_TIER_NOT_CONFIGURED,
                    String.format(DomainCode.VIP_TIER_NOT_CONFIGURED.getMessage(), "<null>"));
        }
        String code = vipType.toUpperCase();
        return vipTierDetailRepository.findByTierCode(code)
                .orElseThrow(() -> new AppException(DomainCode.VIP_TIER_NOT_CONFIGURED,
                        String.format(DomainCode.VIP_TIER_NOT_CONFIGURED.getMessage(), code)));
    }

    private int countByType(Collection<Long> mediaIds, Media.MediaType type) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return 0;
        }
        Set<Long> ids = Set.copyOf(mediaIds);
        return (int) mediaRepository.findAllById(ids).stream()
                .filter(m -> m.getMediaType() == type)
                .count();
    }

    private void enforceLimits(VipTierDetail tier, int totalImages, int totalVideos) {
        Integer maxImages = tier.getMaxImages();
        Integer maxVideos = tier.getMaxVideos();

        if (maxImages != null && totalImages > maxImages) {
            log.warn("VIP tier {} image limit exceeded: requested={}, max={}",
                    tier.getTierCode(), totalImages, maxImages);
            throw new AppException(DomainCode.VIP_TIER_IMAGE_LIMIT_EXCEEDED,
                    String.format(DomainCode.VIP_TIER_IMAGE_LIMIT_EXCEEDED.getMessage(),
                            tier.getTierCode(), maxImages, totalImages));
        }

        if (maxVideos != null && totalVideos > maxVideos) {
            log.warn("VIP tier {} video limit exceeded: requested={}, max={}",
                    tier.getTierCode(), totalVideos, maxVideos);
            throw new AppException(DomainCode.VIP_TIER_VIDEO_LIMIT_EXCEEDED,
                    String.format(DomainCode.VIP_TIER_VIDEO_LIMIT_EXCEEDED.getMessage(),
                            tier.getTierCode(), maxVideos, totalVideos));
        }
    }
}
