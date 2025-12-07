package com.smartrent.service.address.impl;

import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.infra.exception.ResourceNotFoundException;
import com.smartrent.infra.repository.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.service.address.AddressCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AddressCreationService
 * Handles creation of addresses for both old and new structures
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressCreationServiceImpl implements AddressCreationService {

    private final AddressRepository addressRepository;
    private final AddressMetadataRepository addressMetadataRepository;
    private final LegacyProvinceRepository legacyProvinceRepository;
    private final LegacyDistrictRepository legacyDistrictRepository;
    private final LegacyWardRepository legacyWardRepository;
    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public Address createAddress(AddressCreationRequest request) {
        log.info("Creating address - Legacy: {}, New: {}, AddressType: {}",
                request.getLegacy() != null,
                request.getNewAddress() != null,
                request.getEffectiveAddressType());

        // Validate request
        validateAddressRequest(request);

        // Determine address type (auto-detect if not set)
        AddressMetadata.AddressType addressType = request.getEffectiveAddressType();

        // Create address entity with component fields populated
        Address.AddressBuilder addressBuilder = Address.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .addressType(addressType)
                .projectId(request.getProjectId());

        // Populate legacy or new address components

        // Populate legacy components
        addressBuilder
                .legacyProvinceId(request.getLegacyProvinceId())
                .legacyDistrictId(request.getLegacyDistrictId())
                .legacyWardId(request.getLegacyWardId())
                .legacyStreet(request.getStreet());

        // Build formatted address string
        String fullAddress = buildOldAddressString(request);
        addressBuilder.fullAddress(fullAddress);
        log.info("Built legacy address: {}", fullAddress);

        // Populate new components
        addressBuilder
                .newProvinceCode(request.getNewProvinceCodeValue())
                .newWardCode(request.getNewWardCodeValue())
                .newStreet(request.getStreet());

        // Build formatted address string
        String fullNewAddress = buildNewAddressString(request);
        addressBuilder.fullNewAddress(fullNewAddress);
        log.info("Built new address: {}", fullNewAddress);


        // Save address
        Address address = addressBuilder.build();
        address = addressRepository.save(address);
        log.info("Saved address with ID: {} - Type: {}, Legacy: [{},{},{}], New: [{},{}]",
                address.getAddressId(),
                address.getAddressType(),
                address.getLegacyProvinceId(),
                address.getLegacyDistrictId(),
                address.getLegacyWardId(),
                address.getNewProvinceCode(),
                address.getNewWardCode());

        // Create and save metadata
        AddressMetadata metadata = createAddressMetadata(address, request);
        addressMetadataRepository.save(metadata);
        log.info("Saved address metadata with ID: {}", metadata.getMetadataId());

        return address;
    }

    @Override
    public String buildOldAddressString(AddressCreationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add street from nested structure
        String street = request.getStreet();
        if (street != null && !street.isEmpty()) {
            sb.append(street).append(", ");
        } else if (request.getProjectId() != null) {
            // Project provided
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            sb.append(project.getName()).append(", ");
        }

        // Add ward
        Integer wardId = request.getLegacyWardId();
        if (wardId != null) {
            LegacyWard ward = legacyWardRepository.findById(wardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ward not found with ID: " + wardId));

            if (ward.getType() != null && !ward.getType().isEmpty()) {
                sb.append(ward.getType()).append(" ");
            }
            sb.append(ward.getName()).append(", ");
        }

        // Add district
        Integer districtId = request.getLegacyDistrictId();
        if (districtId != null) {
            District district = legacyDistrictRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District not found with ID: " + districtId));

            if (district.getType() != null && !district.getType().isEmpty()) {
                sb.append(district.getType()).append(" ");
            }
            sb.append(district.getName()).append(", ");
        }

        // Add province
        Integer provinceId = request.getLegacyProvinceId();
        if (provinceId != null) {
            LegacyProvince province = legacyProvinceRepository.findById(provinceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Province not found with ID: " + provinceId));
            sb.append(province.getName());
        }

        return sb.toString().trim();
    }

    @Override
    public String buildNewAddressString(AddressCreationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add street from nested structure
        String street = request.getStreet();
        if (street != null && !street.isEmpty()) {
            sb.append(street).append(", ");
        } else if (request.getProjectId() != null) {
            // Project provided
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            sb.append(project.getName()).append(", ");
        }

        // Add ward
        String wardCode = request.getNewWardCodeValue();
        if (wardCode != null && !wardCode.isEmpty()) {
            Ward ward = wardRepository.findByCode(wardCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + wardCode));
            sb.append(ward.getName()).append(", ");
        }

        // Add province
        String provinceCode = request.getNewProvinceCodeValue();
        if (provinceCode != null && !provinceCode.isEmpty()) {
            Province province = provinceRepository.findByCode(provinceCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + provinceCode));
            sb.append(province.getName());
        }

        return sb.toString().trim();
    }

    @Override
    public AddressMetadata createAddressMetadata(Address address, AddressCreationRequest request) {
        AddressMetadata.AddressMetadataBuilder builder = AddressMetadata.builder()
                .address(address)
                .addressType(request.getEffectiveAddressType())
                .projectId(request.getProjectId());

        builder.provinceId(request.getLegacyProvinceId())
                .districtId(request.getLegacyDistrictId())
                .wardId(request.getLegacyWardId());

        builder.newProvinceCode(request.getNewProvinceCodeValue())
                .newWardCode(request.getNewWardCodeValue());

        return builder.build();
    }

    @Override
    public void validateAddressRequest(AddressCreationRequest request) {
        // Validate that required fields are present
        if (!request.hasRequiredFields()) {
            boolean hasLegacyData = request.getLegacy() != null;
            boolean hasNewData = request.getNewAddress() != null;

            if (hasLegacyData) {
                throw new IllegalArgumentException(
                        "For legacy address structure, provinceId, districtId, and wardId are required. " +
                        "Format: {\"legacy\": {\"provinceId\": 1, \"districtId\": 5, \"wardId\": 20, \"street\": \"Nguyen Trai\"}}");
            } else if (hasNewData) {
                throw new IllegalArgumentException(
                        "For new address structure, provinceCode and wardCode are required. " +
                        "Format: {\"newAddress\": {\"provinceCode\": \"01\", \"wardCode\": \"00004\", \"street\": \"Nguyen Trai\"}}");
            } else {
                throw new IllegalArgumentException(
                        "Address data is required. Provide either 'legacy' or 'newAddress' object with required fields.");
            }
        }

        // Validate that at least street or project is provided (optional but recommended)
        if (request.getProjectId() == null && request.getStreet() == null) {
            log.warn("No street or project was provided for address creation");
        }

        // Validate coordinates if provided
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new IllegalArgumentException("Both latitude and longitude must be provided together");
            }
        }
    }
}
