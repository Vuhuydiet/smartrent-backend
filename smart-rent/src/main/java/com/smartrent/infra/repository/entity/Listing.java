package com.smartrent.infra.repository.entity;

import com.smartrent.enums.ModerationStatus;
import com.smartrent.enums.PostSource;
import com.smartrent.util.TextNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "listings")
@Table(name = "listings",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_category_type", columnList = "category_id, listing_type"),
                @Index(name = "idx_address", columnList = "address_id"),
                @Index(name = "idx_price_type", columnList = "price, listing_type"),
                @Index(name = "idx_status", columnList = "verified, expired, vip_type"),
                @Index(name = "idx_expiry_date", columnList = "expiry_date"),
                @Index(name = "idx_post_date", columnList = "post_date"),
                @Index(name = "idx_listings_user_draft_updated", columnList = "user_id, is_draft, updated_at"),
                @Index(name = "idx_listings_pushed_at", columnList = "pushed_at"),
                @Index(name = "idx_listings_parent", columnList = "parent_listing_id"),
                @Index(name = "idx_listings_public_default_sort",
                        columnList = "moderation_status, is_shadow, is_draft, expired, vip_type_sort_order, updated_at"),
                @Index(name = "idx_listings_public_price_sort",
                        columnList = "moderation_status, is_shadow, is_draft, expired, price"),
                @Index(name = "idx_listings_product_type", columnList = "product_type, listing_type"),
                @Index(name = "idx_listings_user_vip_updated", columnList = "user_id, vip_type, updated_at"),
                // Public homepage VIP-tier carousels (GET /v1/listings/homepage-tier, one tier at a time) — see V91-V93.
                // Single-tier query orders by updated_at DESC only (vip_type pinned ⇒ vip_type_sort_order constant),
                // so a plain trailing updated_at serves it via backward index scan on any MySQL version — no filesort.
                @Index(name = "idx_listings_public_vip_tier", columnList = "vip_type, verified, is_draft, is_shadow, updated_at"),
                // Public default cursor feed (/properties, POST /search/cursor) — see V94.
                // ORDER BY vip_type_sort_order ASC, updated_at DESC, listing_id DESC with the equality prefix
                // contiguous (no `expired` gap) so the keyset seek is an ordered index range scan, no filesort.
                // Directions (updated_at/listing_id DESC) need MySQL 8; the real index is built by V94.
                @Index(name = "idx_listings_public_cursor_default", columnList = "moderation_status, is_draft, is_shadow, vip_type_sort_order, updated_at, listing_id"),
                // Category-filtered cursor feed (?categoryId=…) — leads with category_id. See V94.
                @Index(name = "idx_listings_public_cursor_category", columnList = "category_id, moderation_status, is_draft, is_shadow, vip_type_sort_order, updated_at, listing_id"),
                // Recommendation candidate retrieval (getPersonalizedFeed / getSimilarListings) — see V97.
                // Location keys are denormalized from addresses (fixed reference data) so filter AND sort
                // live on listings ⇒ each candidate query is a single-table index-ordered range scan, no
                // filesort. Equality prefix (location + visibility) + ordered suffix (pushed_at, post_date),
                // mirroring idx_listings_public_default_sort. product_type/listing_type/price/expiry are residual.
                @Index(name = "idx_listings_reco_new_prov", columnList = "new_province_code, is_draft, is_shadow, verified, expired, pushed_at, post_date"),
                @Index(name = "idx_listings_reco_new_ward", columnList = "new_ward_code, is_draft, is_shadow, verified, expired, pushed_at, post_date"),
                @Index(name = "idx_listings_reco_legacy_dist", columnList = "legacy_district_id, is_draft, is_shadow, verified, expired, pushed_at, post_date"),
                @Index(name = "idx_listings_reco_legacy_ward", columnList = "legacy_ward_id, is_draft, is_shadow, verified, expired, pushed_at, post_date"),
                @Index(name = "idx_listings_reco_legacy_prov", columnList = "legacy_province_id, is_draft, is_shadow, verified, expired, pushed_at, post_date"),
                @Index(name = "idx_listings_reco_fresh", columnList = "is_draft, is_shadow, verified, expired, pushed_at, post_date"),
                // Public /map-bounds bounding-box query — see V98/V99. lat/lng are
                // denormalized from addresses so the visibility filter, the bbox and the
                // vip/updated_at sort all live on listings ⇒ a single-table range scan
                // replaces the addresses join. Equality prefix (is_draft, is_shadow,
                // verified, moderation_status, expired) + latitude range + covering
                // suffix (longitude, expiry_date + sort keys). moderation_status/is_shadow
                // added in V99 to match withinMapBounds()'s admin-approval + shadow-ban gate.
                @Index(name = "idx_listings_map_bounds", columnList = "is_draft, is_shadow, verified, moderation_status, expired, latitude, longitude, expiry_date, vip_type_sort_order, updated_at, listing_id"),
                // Admin list, default/unfiltered browse (POST /v1/listings/admin/list with
                // no moderationStatus/category/user filter) — see V100. getAllListingsForAdmin
                // only constrains is_shadow=false in this case; every other sort-supporting
                // index leads with moderation_status/category_id/user_id/vip_type, none of
                // which are bound here, so the planner fell back to a filesort over the
                // whole table. This is a dedicated prefix for that gap.
                @Index(name = "idx_listings_admin_default_sort", columnList = "is_shadow, vip_type_sort_order, updated_at"),
                // Admin "pending review" queue (moderationStatus=PENDING_REVIEW +
                // listingStatus=IN_REVIEW, the FE's default admin-list tab) — see V101/V102.
                // Resolves to: is_shadow=false, is_verify=true, verified=false, expired=false
                // (all pure equality — verified/expired/is_verify are BOOLEAN NOT NULL, so the
                // spec's old OR-isNull branches on them were dead code, removed in
                // ListingSpecification), moderation_status IN ('PENDING_REVIEW', NULL) (genuinely
                // nullable — legacy rows), expiry_date > NOW() OR NULL (residual range).
                // V101 shipped this without the trailing sort columns, which is why MySQL still
                // filesorted every hit; V102 corrects the shape: the 4 equality columns first
                // (single ref lookup, not ref_or_null), moderation_status + expiry_date next so
                // ICP can filter them from index data alone (avoiding a full-row fetch for the
                // ~half of candidates that turn out expired), then the sort columns trailing.
                @Index(name = "idx_listings_admin_review_queue",
                        columnList = "is_shadow, is_verify, verified, expired, moderation_status, expiry_date, vip_type_sort_order, updated_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Listing {

    @Id
    @Column(name = "listing_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long listingId;

    // Core Information
    @Column(name = "title", nullable = false, length = 500)
    String title;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    String description;

    @Column(name = "search_text", length = 512)
    String searchText;

    @Column(name = "title_norm", length = 256)
    String titleNorm;

    @Column(name = "phonetic_title", length = 256)
    String phoneticTitle;

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "post_date")
    LocalDateTime postDate;

    @Column(name = "expiry_date")
    LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    ListingType listingType;


    @Builder.Default
    @Column(name = "verified", nullable = false)
    Boolean verified = false;

    @Builder.Default
    @Column(name = "is_verify", nullable = false)
    Boolean isVerify = true;

    @Builder.Default
    @Column(name = "expired", nullable = false)
    Boolean expired = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "vip_type", nullable = false)
    VipType vipType = VipType.NORMAL;

    @Builder.Default
    @Column(name = "vip_type_sort_order", nullable = false)
    Integer vipTypeSortOrder = 4; // NORMAL=4, SILVER=3, GOLD=2, DIAMOND=1

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "post_source")
    PostSource postSource = PostSource.QUOTA;

    @Column(name = "transaction_id", length = 36)
    String transactionId;

    @Builder.Default
    @Column(name = "is_shadow", nullable = false)
    Boolean isShadow = false;

    @Builder.Default
    @Column(name = "is_draft", nullable = false)
    Boolean isDraft = false;

    @Column(name = "parent_listing_id")
    Long parentListingId;

    @Column(name = "pushed_at")
    LocalDateTime pushedAt;



    // ── Moderation fields (nullable for backward compatibility) ──
    @Convert(converter = com.smartrent.infra.repository.entity.converter.ModerationStatusConverter.class)
    @Column(name = "moderation_status", length = 30)
    ModerationStatus moderationStatus;

    @Column(name = "last_moderated_by", length = 36)
    String lastModeratedBy;

    @Column(name = "last_moderated_at")
    LocalDateTime lastModeratedAt;

    @Column(name = "last_moderation_reason_code", length = 50)
    String lastModerationReasonCode;

    @Column(name = "last_moderation_reason_text", columnDefinition = "TEXT")
    String lastModerationReasonText;

    // Set when a report is resolved as a confirmed severe violation and the
    // listing is removed (moderationStatus=SUSPENDED). Distinguishes that case
    // from an ordinary admin SUSPEND, which the owner CAN resubmit from.
    @Builder.Default
    @Column(name = "permanently_removed", nullable = false)
    Boolean permanentlyRemoved = false;

    @Builder.Default
    @Column(name = "revision_count", nullable = false)
    Integer revisionCount = 0;

    @Column(name = "category_id", nullable = false)
    Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    ProductType productType;

    // Pricing Information
    @Column(name = "price", nullable = false, precision = 15, scale = 0)
    BigDecimal price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", nullable = false)
    PriceUnit priceUnit = PriceUnit.MONTH;

    // Location Information - Reference to Address table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    Address address;

    // Denormalized location keys copied from the referenced Address (fixed
    // reference data). Populated by updateSearchFields() on persist/update and
    // backfilled in V97. These exist ONLY so the recommendation candidate
    // queries can filter AND sort on listings alone (see idx_listings_reco_*),
    // turning a cross-table filter+sort filesort into an index-ordered scan.
    // Read the Address entity for canonical values; never write these directly.
    @Column(name = "new_province_code", length = 10)
    String newProvinceCode;

    @Column(name = "new_ward_code", length = 10)
    String newWardCode;

    @Column(name = "legacy_province_id")
    Integer legacyProvinceId;

    @Column(name = "legacy_district_id")
    Integer legacyDistrictId;

    @Column(name = "legacy_ward_id")
    Integer legacyWardId;

    // Denormalized coordinates copied from the referenced Address, same rationale
    // as the location keys above: /map-bounds filters by a lat/lng bounding box
    // AND sorts by vip_type_sort_order/updated_at. Keeping lat/lng on listings
    // turns that cross-table filter+sort (a ~20s nested-loop join on the
    // small-buffer-pool prod DB) into a single-table index range scan
    // (idx_listings_map_bounds). Populated by updateSearchFields() and backfilled
    // in V98. Read the Address entity for canonical values; never write directly.
    @Column(name = "latitude", precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    BigDecimal longitude;

    // Property Specifications
    @Column(name = "area")
    Float area;

    @Column(name = "bedrooms")
    Integer bedrooms;

    @Column(name = "bathrooms")
    Integer bathrooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing")
    Furnishing furnishing;

    @Column(name = "room_capacity")
    Integer roomCapacity;

    // Utility Costs
    @Column(name = "water_price", length = 50)
    String waterPrice;

    @Column(name = "electricity_price", length = 50)
    String electricityPrice;

    @Column(name = "internet_price", length = 50)
    String internetPrice;

    @Column(name = "service_fee", length = 50)
    String serviceFee;

    // Listing Duration and Payment
    @Builder.Default
    @Column(name = "duration_days")
    Integer durationDays = 30;

    @Builder.Default
    @Column(name = "use_membership_quota", nullable = false)
    Boolean useMembershipQuota = false;

    @Column(name = "payment_provider", length = 50)
    String paymentProvider;

    // Relationships
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    List<Media> media;

    @ManyToMany
    @JoinTable(
            name = "listing_amenities",
            joinColumns = @JoinColumn(name = "listing_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    List<Amenity> amenities;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Favorite> favorites;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<View> views;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<PhoneClickDetail> phoneClickDetails;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    List<PricingHistory> pricingHistories;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Column(name = "updated_by")
    Long updatedBy;

    // Enums
    public enum ListingType {
        RENT, SALE, SHARE
    }

    public enum VipType {
        NORMAL, SILVER, GOLD, DIAMOND
    }

    public enum ProductType {
        ROOM, APARTMENT, HOUSE, OFFICE, STUDIO, STORE
    }

    public enum PriceUnit {
        MONTH, DAY, YEAR
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST,
        NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST
    }

    public enum Furnishing {
        FULLY_FURNISHED, SEMI_FURNISHED, UNFURNISHED
    }

    // Helper methods for VIP types
    public boolean isNormal() {
        return vipType == VipType.NORMAL;
    }

    public boolean isSilver() {
        return vipType == VipType.SILVER;
    }

    public boolean isGold() {
        return vipType == VipType.GOLD;
    }

    public boolean isDiamond() {
        return vipType == VipType.DIAMOND;
    }

    /**
     * Get sort order value for VipType
     * DIAMOND=1 (highest priority), GOLD=2, SILVER=3, NORMAL=4 (lowest priority)
     */
    public static Integer getVipTypeSortOrder(VipType vipType) {
        if (vipType == null) {
            return 4; // Default to NORMAL
        }
        return switch (vipType) {
            case DIAMOND -> 1;
            case GOLD -> 2;
            case SILVER -> 3;
            case NORMAL -> 4;
        };
    }

    // Helper methods for shadow listings
    public boolean isShadowListing() {
        return isShadow != null && isShadow;
    }

    public boolean hasParentListing() {
        return parentListingId != null;
    }

    // Helper methods for verification
    public boolean isAutoVerified() {
        return isVerify != null && isVerify;
    }

    public boolean isManuallyVerified() {
        return verified != null && verified;
    }

    // Helper methods for post source
    public boolean isCreatedWithQuota() {
        return postSource == PostSource.QUOTA;
    }

    public boolean isCreatedWithDirectPayment() {
        return postSource == PostSource.DIRECT_PAYMENT;
    }

    public boolean hasLinkedTransaction() {
        return transactionId != null && !transactionId.isEmpty();
    }

    // Helper methods for expiry
    public boolean isExpiredListing() {
        return expired != null && expired;
    }

    public boolean isActive() {
        return !isExpiredListing() && (isAutoVerified() || isManuallyVerified());
    }

    @PrePersist
    @PreUpdate
    private void updateSearchFields() {
        // Keep the denormalized location keys in sync with the referenced address
        // (see the field comment + idx_listings_reco_*). Address is fixed reference
        // data, so in practice this only ever fires meaningfully on create; the
        // address proxy is already initialized below via getDisplayAddress().
        if (address != null) {
            newProvinceCode = address.getNewProvinceCode();
            newWardCode = address.getNewWardCode();
            legacyProvinceId = address.getLegacyProvinceId();
            legacyDistrictId = address.getLegacyDistrictId();
            legacyWardId = address.getLegacyWardId();
            latitude = address.getLatitude();
            longitude = address.getLongitude();
        }

        String addressText = address != null ? address.getDisplayAddress() : null;
        String descSnippet = description;
        if (descSnippet != null && descSnippet.length() > 200) {
            descSnippet = descSnippet.substring(0, 200);
        }

        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append(title).append(' ');
        }
        if (addressText != null && !addressText.isEmpty()) {
            sb.append(addressText).append(' ');
        }
        if (descSnippet != null && !descSnippet.isEmpty()) {
            sb.append(descSnippet);
        }

        String combined = sb.toString().trim();
        titleNorm = TextNormalizer.compact(title, 256);
        if (titleNorm != null && !titleNorm.isEmpty()) {
            org.apache.commons.codec.language.DoubleMetaphone metaphone = new org.apache.commons.codec.language.DoubleMetaphone();
            metaphone.setMaxCodeLen(256);
            
            // DoubleMetaphone works best per word
            String[] words = titleNorm.split(" ");
            StringBuilder phoneticBuilder = new StringBuilder();
            for (String word : words) {
                String code = metaphone.doubleMetaphone(word);
                if (code != null) {
                    phoneticBuilder.append(code).append(" ");
                }
            }
            phoneticTitle = phoneticBuilder.toString().trim();
            if (phoneticTitle.length() > 256) {
                phoneticTitle = phoneticTitle.substring(0, 256);
            }
        } else {
            phoneticTitle = null;
        }
        searchText = TextNormalizer.compact(combined, 512);
    }

    /**
     * Compute the current listing status for owner view.
     * When moderationStatus is present, it takes precedence for REVISION_REQUIRED/RESUBMITTED states.
     * Falls back to legacy verified/isVerify logic for other states (backward compat).
     * @return ListingStatus enum value
     */
    public com.smartrent.enums.ListingStatus computeListingStatus() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // 1. EXPIRED - Listing has expired
        if (this.expired != null && this.expired) {
            return com.smartrent.enums.ListingStatus.EXPIRED;
        }
        if (this.expiryDate != null && this.expiryDate.isBefore(now)) {
            return com.smartrent.enums.ListingStatus.EXPIRED;
        }

        // 2. PENDING_PAYMENT - Has transaction but not completed
        if (this.transactionId != null && !this.transactionId.isEmpty() &&
            (this.verified == null || !this.verified) &&
            (this.isVerify == null || !this.isVerify)) {
            return com.smartrent.enums.ListingStatus.PENDING_PAYMENT;
        }

        // 2.5 New moderation-aware states (when moderationStatus is populated)
        if (this.moderationStatus != null) {
            switch (this.moderationStatus) {
                case REVISION_REQUIRED:
                    return com.smartrent.enums.ListingStatus.REJECTED; // maps to REJECTED for owner view
                case RESUBMITTED:
                    return com.smartrent.enums.ListingStatus.RESUBMITTED;
                case PENDING_REVIEW:
                    return com.smartrent.enums.ListingStatus.IN_REVIEW;
                case SUSPENDED:
                    return com.smartrent.enums.ListingStatus.REJECTED;
                case REJECTED:
                    return com.smartrent.enums.ListingStatus.REJECTED;
                case APPROVED:
                    if (this.expired != null && this.expired) {
                        return com.smartrent.enums.ListingStatus.EXPIRED;
                    }
                    if (this.expiryDate != null && this.expiryDate.isBefore(now)) {
                        return com.smartrent.enums.ListingStatus.EXPIRED;
                    }
                    if (this.expiryDate != null) {
                        long daysUntilExpiry = java.time.Duration.between(now, this.expiryDate).toDays();
                        if (daysUntilExpiry >= 0 && daysUntilExpiry <= 7) {
                            return com.smartrent.enums.ListingStatus.EXPIRING_SOON;
                        }
                    }
                    return com.smartrent.enums.ListingStatus.DISPLAYING;
            }
        }

        // 3. IN_REVIEW - Pending verification (isVerify=true, verified=false)
        if (this.isVerify != null && this.isVerify &&
            (this.verified == null || !this.verified)) {
            return com.smartrent.enums.ListingStatus.IN_REVIEW;
        }

        // 4. REJECTED - Not verified, not in review, not draft, has postDate
        if ((this.verified == null || !this.verified) &&
            (this.isVerify == null || !this.isVerify) &&
            (this.isDraft == null || !this.isDraft) &&
            this.postDate != null) {
            return com.smartrent.enums.ListingStatus.REJECTED;
        }

        // 5. EXPIRING_SOON - Will expire within 7 days
        if (this.expiryDate != null && this.verified != null && this.verified) {
            long daysUntilExpiry = java.time.Duration.between(now, this.expiryDate).toDays();
            if (daysUntilExpiry >= 0 && daysUntilExpiry <= 7) {
                return com.smartrent.enums.ListingStatus.EXPIRING_SOON;
            }
        }

        // 6. DISPLAYING - Is displaying (verified, not expired, has time remaining > 7 days)
        if (this.verified != null && this.verified &&
            (this.expired == null || !this.expired) &&
            (this.expiryDate == null || this.expiryDate.isAfter(now))) {
            return com.smartrent.enums.ListingStatus.DISPLAYING;
        }

        // 7. VERIFIED - Is verified (fallback for verified listings without expiry info)
        if (this.verified != null && this.verified) {
            return com.smartrent.enums.ListingStatus.VERIFIED;
        }

        // Default to IN_REVIEW if not verified
        return com.smartrent.enums.ListingStatus.IN_REVIEW;
    }
}
