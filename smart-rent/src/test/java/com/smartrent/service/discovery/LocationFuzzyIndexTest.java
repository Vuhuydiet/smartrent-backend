package com.smartrent.service.discovery;

import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.LegacyProvince;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Spring-free core of {@link LocationFuzzyIndex}: a misspelt
 * place ("tn binh") must still resolve to the right district AND carry the
 * parent province's legacy id, while the precision floor keeps unrelated
 * names out.
 */
class LocationFuzzyIndexTest {

    private static LegacyProvince province(int id, String code, String name, String shortName) {
        return LegacyProvince.builder()
                .id(id).code(code).name(name).shortName(shortName).key("").build();
    }

    private static District district(
            int id, String code, String provinceCode,
            String name, String shortName, String provinceName) {
        return District.builder()
                .id(id).code(code).provinceCode(provinceCode)
                .name(name).shortName(shortName)
                .provinceName(provinceName).provinceShortName(provinceName)
                .type("Quận").build();
    }

    private LocationFuzzyIndex.Index sampleIndex() {
        List<LegacyProvince> provinces = List.of(
                province(79, "79", "Thành phố Hồ Chí Minh", "Hồ Chí Minh"),
                province(1, "01", "Thành phố Hà Nội", "Hà Nội"));
        List<District> districts = List.of(
                district(766, "766", "79", "Quận Tân Bình", "Tân Bình", "Thành phố Hồ Chí Minh"),
                district(778, "778", "79", "Quận 7", "7", "Thành phố Hồ Chí Minh"),
                district(7, "007", "01", "Quận Hoàn Kiếm", "Hoàn Kiếm", "Thành phố Hà Nội"));
        return LocationFuzzyIndex.buildIndex(provinces, districts);
    }

    @Test
    @DisplayName("'tn binh' resolves Tân Bình district + its parent province id")
    void typoResolvesDistrictAndParentProvince() {
        List<LocationFuzzyIndex.Match> hits = LocationFuzzyIndex.searchIndex(
                sampleIndex(), "tn binh", LocationFuzzyIndex.DEFAULT_MIN_SCORE, 5);

        LocationFuzzyIndex.Match top = hits.get(0);
        assertEquals(LocationFuzzyIndex.Kind.DISTRICT, top.kind());
        assertEquals("Quận Tân Bình", top.display());
        assertEquals(766, top.legacyDistrictId().intValue());
        assertEquals(79, top.legacyProvinceId().intValue());
        assertEquals("79", top.provinceCode());
    }

    @Test
    @DisplayName("exact province name still matches with score 1.0")
    void exactProvinceMatches() {
        List<LocationFuzzyIndex.Match> hits = LocationFuzzyIndex.searchIndex(
                sampleIndex(), "ha noi", LocationFuzzyIndex.DEFAULT_MIN_SCORE, 5);

        assertTrue(hits.stream().anyMatch(m ->
                m.kind() == LocationFuzzyIndex.Kind.PROVINCE
                        && m.legacyProvinceId() == 1
                        && "Thành phố Hà Nội".equals(m.display())));
    }

    @Test
    @DisplayName("precision floor: an over-strict threshold rejects the typo")
    void highThresholdRejectsNoise() {
        assertTrue(LocationFuzzyIndex.searchIndex(
                sampleIndex(), "tn binh", 0.999, 5).isEmpty());
    }

    @Test
    @DisplayName("fragments shorter than the minimum are ignored")
    void tooShortQueryIgnored() {
        assertTrue(LocationFuzzyIndex.searchIndex(
                sampleIndex(), "tn", LocationFuzzyIndex.DEFAULT_MIN_SCORE, 5).isEmpty());
    }

    @Test
    @DisplayName("limit caps the number of returned matches")
    void limitIsRespected() {
        List<LocationFuzzyIndex.Match> hits = LocationFuzzyIndex.searchIndex(
                sampleIndex(), "ha noi", 0.0, 1);
        assertTrue(hits.size() <= 1);
    }

    @Test
    @DisplayName("resolveDistrict: exact normalized name within a province → ids")
    void resolveDistrictExactWithinProvince() {
        Optional<LocationFuzzyIndex.Match> m = LocationFuzzyIndex.resolveDistrictIn(
                sampleIndex(), "79", "tan binh");
        assertTrue(m.isPresent());
        assertEquals(766, m.get().legacyDistrictId().intValue());
        assertEquals(79, m.get().legacyProvinceId().intValue());

        // Numbered district resolves via its "quan N" normalized name too.
        assertTrue(LocationFuzzyIndex
                .resolveDistrictIn(sampleIndex(), "79", "quan 7").isPresent());
    }

    @Test
    @DisplayName("resolveDistrict: wrong province / unknown name → empty")
    void resolveDistrictScopedAndStrict() {
        assertFalse(LocationFuzzyIndex
                .resolveDistrictIn(sampleIndex(), "01", "tan binh").isPresent());
        assertFalse(LocationFuzzyIndex
                .resolveDistrictIn(sampleIndex(), "79", "khong co dau").isPresent());
    }

    // ── resolveLocationPhrase: the "<district> <province>" splitter ──────────

    @Test
    @DisplayName("'quan 7 ho chi minh' → Quận 7 district + HCM province ids")
    void numberedDistrictThenProvince() {
        Optional<LocationFuzzyIndex.Match> m = LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "quan 7 ho chi minh");
        assertTrue(m.isPresent());
        assertEquals(LocationFuzzyIndex.Kind.DISTRICT, m.get().kind());
        assertEquals(778, m.get().legacyDistrictId().intValue());
        assertEquals(79, m.get().legacyProvinceId().intValue());
        assertEquals("79", m.get().provinceCode());
    }

    @Test
    @DisplayName("'tan binh ho chi minh' → Tân Bình district + HCM province ids")
    void namedDistrictThenProvince() {
        Optional<LocationFuzzyIndex.Match> m = LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "tan binh ho chi minh");
        assertTrue(m.isPresent());
        assertEquals(LocationFuzzyIndex.Kind.DISTRICT, m.get().kind());
        assertEquals(766, m.get().legacyDistrictId().intValue());
        assertEquals(79, m.get().legacyProvinceId().intValue());
    }

    @Test
    @DisplayName("province-first order: 'ha noi hoan kiem' → Hoàn Kiếm + Hà Nội")
    void provinceThenDistrict() {
        Optional<LocationFuzzyIndex.Match> m = LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "ha noi hoan kiem");
        assertTrue(m.isPresent());
        assertEquals(LocationFuzzyIndex.Kind.DISTRICT, m.get().kind());
        assertEquals(7, m.get().legacyDistrictId().intValue());
        assertEquals(1, m.get().legacyProvinceId().intValue());
    }

    @Test
    @DisplayName("order-independent: 'ho chi minh quan 7' (province FIRST + numbered) → Quận 7")
    void provinceFirstThenNumberedDistrict() {
        Optional<LocationFuzzyIndex.Match> m = LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "ho chi minh quan 7");
        assertTrue(m.isPresent());
        assertEquals(LocationFuzzyIndex.Kind.DISTRICT, m.get().kind());
        assertEquals(778, m.get().legacyDistrictId().intValue());
        assertEquals(79, m.get().legacyProvinceId().intValue());
        assertEquals("79", m.get().provinceCode());
    }

    @Test
    @DisplayName("single-segment phrase is left to the DB tiers → empty")
    void singleSegmentIsEmpty() {
        assertFalse(LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "ho chi minh").isPresent());
        assertFalse(LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "tan binh").isPresent());
    }

    @Test
    @DisplayName("district not in the resolved province → province-only match")
    void districtNotInProvinceFallsBackToProvince() {
        // "Hoàn Kiếm" belongs to Hà Nội, not HCM → no district in 79;
        // the resolver still returns the province it DID resolve.
        Optional<LocationFuzzyIndex.Match> m = LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "hoan kiem ho chi minh");
        assertTrue(m.isPresent());
        assertEquals(LocationFuzzyIndex.Kind.PROVINCE, m.get().kind());
        assertEquals(79, m.get().legacyProvinceId().intValue());
    }

    @Test
    @DisplayName("no province anywhere in the phrase → empty (no false district)")
    void noProvinceMeansEmpty() {
        assertFalse(LocationFuzzyIndex
                .resolveLocationPhraseIn(sampleIndex(), "tan binh quan 7").isPresent());
    }
}
