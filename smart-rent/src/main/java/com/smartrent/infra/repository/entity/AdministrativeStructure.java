package com.smartrent.infra.repository.entity;

/**
 * Enum representing the administrative structure version for Vietnamese administrative units.
 * <p>
 * Vietnam underwent a significant administrative reorganization effective July 1, 2025:
 * - OLD: Administrative units from the old structure (before July 1, 2025)
 * - NEW: Administrative units from the new structure (after July 1, 2025)
 * - BOTH: Administrative units valid in both old and new structures
 * </p>
 *
 * <p>Key changes in the new structure:
 * - Districts (Quận/Huyện) were dissolved
 * - Wards (Phường/Xã) now report directly to Provinces (Tỉnh/Thành phố)
 * - Many provinces and wards were merged
 * </p>
 */
public enum AdministrativeStructure {
    /**
     * Old structure (before July 1, 2025)
     * - Province → District → Ward → Street hierarchy
     */
    OLD,

    /**
     * New structure (after July 1, 2025)
     * - Province → Ward → Street hierarchy (no districts)
     */
    NEW,

    /**
     * Valid in both structures
     * - Administrative units that exist in both old and new structures without significant changes
     */
    BOTH
}
