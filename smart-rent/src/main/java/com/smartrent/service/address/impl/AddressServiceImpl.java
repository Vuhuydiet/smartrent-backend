package com.smartrent.service.address.impl;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.DistrictResponse;
import com.smartrent.dto.response.ProvinceResponse;
import com.smartrent.dto.response.StreetResponse;
import com.smartrent.dto.response.WardResponse;
import com.smartrent.infra.repository.AddressRepository;
import com.smartrent.infra.repository.DistrictRepository;
import com.smartrent.infra.repository.ProvinceRepository;
import com.smartrent.infra.repository.StreetRepository;
import com.smartrent.infra.repository.WardRepository;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.District;
import com.smartrent.infra.repository.entity.Province;
import com.smartrent.infra.repository.entity.Street;
import com.smartrent.infra.repository.entity.Ward;
import com.smartrent.mapper.AddressMapper;
import com.smartrent.service.address.AddressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    ProvinceRepository provinceRepository;
    DistrictRepository districtRepository;
    WardRepository wardRepository;
    StreetRepository streetRepository;
    AddressRepository addressRepository;
    AddressMapper addressMapper;

    @Override
    public List<ProvinceResponse> getAllProvinces() {
        log.info("Getting all active provinces");
        return provinceRepository.findByIsActiveTrueOrderByName()
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProvinceResponse> getParentProvinces() {
        log.info("Getting parent provinces for dropdown");
        return provinceRepository.findByParentProvinceIsNullAndIsActiveTrueOrderByName()
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProvinceResponse getProvinceById(Long provinceId) {
        log.info("Getting province by id: {}", provinceId);
        return provinceRepository.findById(provinceId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Province not found with id: " + provinceId));
    }

    @Override
    public List<ProvinceResponse> searchProvinces(String searchTerm) {
        log.info("Searching provinces with term: {}", searchTerm);
        return provinceRepository.findByNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCaseAndIsActiveTrueOrderByName(searchTerm, searchTerm)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictResponse> getDistrictsByProvinceId(Long provinceId) {
        log.info("Getting districts for province: {}", provinceId);
        List<District> districts = districtRepository.findByProvinceProvinceIdAndIsActiveTrueOrderByName(provinceId);

        // Also get districts from merged provinces
        List<District> mergedDistricts = districtRepository.findByProvinceParentProvinceProvinceIdAndIsActiveTrueOrderByName(provinceId);
        districts.addAll(mergedDistricts);

        return districts.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DistrictResponse getDistrictById(Long districtId) {
        log.info("Getting district by id: {}", districtId);
        return districtRepository.findById(districtId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("District not found with id: " + districtId));
    }

    @Override
    public List<DistrictResponse> searchDistricts(String searchTerm, Long provinceId) {
        log.info("Searching districts with term: {} in province: {}", searchTerm, provinceId);
        List<District> districts = districtRepository.findByNameContainingIgnoreCaseAndProvinceProvinceIdAndIsActiveTrueOrderByName(searchTerm, provinceId);

        // Also search in merged provinces
        List<District> mergedDistricts = districtRepository.findByNameContainingIgnoreCaseAndProvinceParentProvinceProvinceIdAndIsActiveTrueOrderByName(searchTerm, provinceId);
        districts.addAll(mergedDistricts);

        return districts.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardResponse> getWardsByDistrictId(Long districtId) {
        log.info("Getting wards for district: {}", districtId);
        return wardRepository.findByDistrictDistrictIdAndIsActiveTrueOrderByName(districtId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WardResponse getWardById(Long wardId) {
        log.info("Getting ward by id: {}", wardId);
        return wardRepository.findById(wardId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Ward not found with id: " + wardId));
    }

    @Override
    public List<WardResponse> searchWards(String searchTerm, Long districtId) {
        log.info("Searching wards with term: {} in district: {}", searchTerm, districtId);
        return wardRepository.findByNameContainingIgnoreCaseAndDistrictDistrictIdAndIsActiveTrueOrderByName(searchTerm, districtId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StreetResponse> getStreetsByWardId(Long wardId) {
        log.info("Getting streets for ward: {}", wardId);
        return streetRepository.findByWardWardIdAndIsActiveTrueOrderByName(wardId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StreetResponse getStreetById(Long streetId) {
        log.info("Getting street by id: {}", streetId);
        return streetRepository.findById(streetId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Street not found with id: " + streetId));
    }

    @Override
    public List<StreetResponse> searchStreets(String searchTerm, Long wardId) {
        log.info("Searching streets with term: {} in ward: {}", searchTerm, wardId);
        return streetRepository.findByNameContainingIgnoreCaseAndWardWardIdAndIsActiveTrueOrderByName(searchTerm, wardId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getAddressesByStreetId(Long streetId) {
        log.info("Getting addresses for street: {}", streetId);
        return addressRepository.findByStreetStreetIdOrderByStreetNumber(streetId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddressById(Long addressId) {
        log.info("Getting address by id: {}", addressId);
        return addressRepository.findById(addressId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));
    }

    @Override
    public List<AddressResponse> searchAddresses(String searchTerm) {
        log.info("Searching addresses with term: {}", searchTerm);
        return addressRepository.findByFullAddressContainingIgnoreCaseOrderByFullAddress(searchTerm)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AddressResponse> getNearbyAddresses(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        log.info("Getting nearby addresses for coordinates: {}, {} within {}km", latitude, longitude, radiusKm);

        // Convert radius to lat/lng delta (rough approximation)
        BigDecimal latDelta = BigDecimal.valueOf(radiusKm / 111.0); // 1 degree lat ≈ 111km
        BigDecimal lngDelta = BigDecimal.valueOf(radiusKm / (111.0 * Math.cos(Math.toRadians(latitude.doubleValue()))));

        return addressRepository.findNearbyAddresses(latitude, longitude, latDelta, lngDelta)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse createAddress(AddressCreationRequest request) {
        log.info("Creating address: {} on street: {}", request.getStreetNumber(), request.getStreetId());

        // Check if address already exists
        Optional<Address> existingAddress = addressRepository.findByStreetNumberAndStreetStreetIdAndWardWardIdAndDistrictDistrictIdAndProvinceProvinceId(
                request.getStreetNumber(), request.getStreetId(), request.getWardId(),
                request.getDistrictId(), request.getProvinceId());

        if (existingAddress.isPresent()) {
            return addressMapper.toResponse(existingAddress.get());
        }

        // Create new address
        Street street = streetRepository.findById(request.getStreetId())
                .orElseThrow(() -> new RuntimeException("Street not found"));
        Ward ward = wardRepository.findById(request.getWardId())
                .orElseThrow(() -> new RuntimeException("Ward not found"));
        District district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new RuntimeException("District not found"));
        Province province = provinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new RuntimeException("Province not found"));

        Address newAddress = Address.builder()
                .streetNumber(request.getStreetNumber())
                .street(street)
                .ward(ward)
                .district(district)
                .province(province)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .fullAddress(buildFullAddress(request.getStreetNumber(), street, ward, district, province))
                .isVerified(false)
                .build();

        Address savedAddress = addressRepository.save(newAddress);
        return addressMapper.toResponse(savedAddress);
    }

    @Override
    public AddressResponse autoCompleteAddress(String fullAddressText) {
        log.info("Auto-completing address: {}", fullAddressText);

        List<Address> addresses = addressRepository.findByFullAddressContainingIgnoreCaseOrderByFullAddress(fullAddressText);
        if (!addresses.isEmpty()) {
            return addressMapper.toResponse(addresses.get(0));
        }

        throw new RuntimeException("Address not found: " + fullAddressText);
    }

    @Override
    public List<AddressResponse> suggestAddresses(String partialAddress) {
        log.info("Suggesting addresses for: {}", partialAddress);
        return addressRepository.findByFullAddressContainingIgnoreCaseOrderByFullAddress(partialAddress)
                .stream()
                .limit(10) // Limit suggestions
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    private String buildFullAddress(String streetNumber, Street street, Ward ward, District district, Province province) {
        StringBuilder sb = new StringBuilder();
        if (streetNumber != null && !streetNumber.trim().isEmpty()) {
            sb.append(streetNumber).append(" ");
        }
        sb.append(street.getName()).append(", ");
        sb.append(ward.getName()).append(", ");
        sb.append(district.getName()).append(", ");
        sb.append(province.getDisplayName());
        return sb.toString();
    }
}
