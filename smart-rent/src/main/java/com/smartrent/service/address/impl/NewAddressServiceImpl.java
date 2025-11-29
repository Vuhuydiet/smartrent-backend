package com.smartrent.service.address.impl;

import com.smartrent.dto.response.NewAddressSearchResponse;
import com.smartrent.dto.response.NewFullAddressResponse;
import com.smartrent.dto.response.NewProvinceResponse;
import com.smartrent.dto.response.NewWardResponse;
import com.smartrent.dto.response.PaginatedResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ProvinceRepository;
import com.smartrent.infra.repository.WardRepository;
import com.smartrent.infra.repository.entity.Province;
import com.smartrent.infra.repository.entity.Ward;
import com.smartrent.mapper.AddressMapper;
import com.smartrent.service.address.NewAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for new administrative structure (34 provinces after 1/7/2025)
 * Uses internal database instead of external API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewAddressServiceImpl implements NewAddressService {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final AddressMapper addressMapper;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<List<NewProvinceResponse>> getNewProvinces(
            String keyword

    ) {
        log.info("Fetching new provinces from database - keyword: {}, page: {}, limit: {}", keyword);

        try {
            Integer validPage = DEFAULT_PAGE;
            Integer validLimit = DEFAULT_LIMIT;

            Pageable pageable = PageRequest.of(validPage - 1, validLimit);
            Page<Province> provincePage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                provincePage = provinceRepository.searchByKeyword(keyword.trim(), pageable);
            } else {
                provincePage = provinceRepository.findAll(pageable);
            }

            List<NewProvinceResponse> provinceResponses = provincePage.getContent().stream()
                    .map(addressMapper::toNewProvinceResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} new provinces from database", provinceResponses.size());

            PaginatedResponse.Metadata metadata = PaginatedResponse.Metadata.builder()
                    .total((int) provincePage.getTotalElements())
                    .page(validPage)
                    .limit(validLimit)
                    .build();

            return PaginatedResponse.<List<NewProvinceResponse>>builder()
                    .success(true)
                    .message("Success")
                    .data(provinceResponses)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching new provinces from database: {}", e.getMessage(), e);
            throw new AppException(DomainCode.UNKNOWN_ERROR,
                    "Failed to fetch new provinces from database");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<List<NewWardResponse>> getWardsByNewProvince(
            String provinceCode,
            String keyword,
            Integer page,
            Integer limit
    ) {
        log.info("Fetching wards for new province {} from database - keyword: {}, page: {}, limit: {}",
                provinceCode, keyword, page, limit);

        if (provinceCode == null || provinceCode.trim().isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR, "Province code is required");
        }

        try {
            // Verify province exists
            Province province = provinceRepository.findByCode(provinceCode)
                    .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND,
                            "Province not found with code: " + provinceCode));

            Integer validPage = page != null && page > 0 ? page : DEFAULT_PAGE;
            Integer validLimit = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;

            Pageable pageable = PageRequest.of(validPage - 1, validLimit);
            Page<Ward> wardPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                wardPage = wardRepository.searchByProvinceAndKeyword(provinceCode, keyword.trim(), pageable);
            } else {
                wardPage = wardRepository.findByProvinceCode(provinceCode, pageable);
            }

            List<NewWardResponse> wardResponses = wardPage.getContent().stream()
                    .map(addressMapper::toNewWardResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} wards for province {} from database",
                    wardResponses.size(), provinceCode);

            PaginatedResponse.Metadata metadata = PaginatedResponse.Metadata.builder()
                    .total((int) wardPage.getTotalElements())
                    .page(validPage)
                    .limit(validLimit)
                    .build();

            return PaginatedResponse.<List<NewWardResponse>>builder()
                    .success(true)
                    .message("Success")
                    .data(wardResponses)
                    .metadata(metadata)
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching wards for province {} from database: {}", provinceCode, e.getMessage(), e);
            throw new AppException(DomainCode.UNKNOWN_ERROR,
                    "Failed to fetch wards from database");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NewFullAddressResponse getNewFullAddress(String provinceCode, String wardCode) {
        log.info("Fetching new full address from database - provinceCode: {}, wardCode: {}",
                provinceCode, wardCode);

        if (provinceCode == null || provinceCode.trim().isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR, "Province code is required");
        }

        try {
            // Fetch province
            Province province = provinceRepository.findByCode(provinceCode)
                    .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND,
                            "Province not found with code: " + provinceCode));

            // Fetch ward if wardCode is provided
            Ward ward = null;
            if (wardCode != null && !wardCode.trim().isEmpty()) {
                ward = wardRepository.findByCode(wardCode)
                        .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND,
                                "Ward not found with code: " + wardCode));
            }

            log.info("Successfully fetched full address for province {} and ward {} from database",
                    provinceCode, wardCode);

            return addressMapper.toNewFullAddressResponse(province, ward);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching full address from database: {}", e.getMessage(), e);
            throw new AppException(DomainCode.UNKNOWN_ERROR,
                    "Failed to fetch full address from database");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<List<NewAddressSearchResponse>> searchNewAddress(
            String keyword,
            Integer page,
            Integer limit
    ) {
        log.info("Searching new addresses from database - keyword: {}, page: {}, limit: {}",
                keyword, page, limit);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR, "Search keyword is required");
        }

        try {
            Integer validPage = page != null && page > 0 ? page : DEFAULT_PAGE;
            Integer validLimit = limit != null && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;

            Pageable pageable = PageRequest.of(validPage - 1, validLimit);

            // Search provinces by keyword
            Page<Province> provincePage = provinceRepository.searchByKeyword(keyword.trim(), pageable);

            // Search wards by keyword (includes province information)
            Page<Ward> wardPage = wardRepository.searchByKeyword(keyword.trim(), pageable);

            // Combine results - provinces first, then wards
            List<NewAddressSearchResponse> searchResults = new ArrayList<>();

            // Add matching provinces
            searchResults.addAll(provincePage.getContent().stream()
                    .map(addressMapper::toNewAddressSearchResponseFromProvince)
                    .collect(Collectors.toList()));

            // Add matching wards
            searchResults.addAll(wardPage.getContent().stream()
                    .map(addressMapper::toNewAddressSearchResponse)
                    .collect(Collectors.toList()));

            // Limit combined results to the requested page size
            if (searchResults.size() > validLimit) {
                searchResults = searchResults.subList(0, validLimit);
            }

            long totalCount = wardPage.getTotalElements() + provincePage.getTotalElements();

            log.info("Successfully found {} addresses matching keyword '{}' from database",
                    searchResults.size(), keyword);

            PaginatedResponse.Metadata metadata = PaginatedResponse.Metadata.builder()
                    .total((int) totalCount)
                    .page(validPage)
                    .limit(validLimit)
                    .build();

            return PaginatedResponse.<List<NewAddressSearchResponse>>builder()
                    .success(true)
                    .message("Success")
                    .data(searchResults)
                    .metadata(metadata)
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching addresses from database: {}", e.getMessage(), e);
            throw new AppException(DomainCode.UNKNOWN_ERROR,
                    "Failed to search addresses from database");
        }
    }
}
