package com.smartrent.service.discovery;

import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.LegacyProvince;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
