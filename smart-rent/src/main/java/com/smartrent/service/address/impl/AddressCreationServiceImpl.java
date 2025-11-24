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
    private final StreetRepository streetRepository;
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
        if (request.isLegacyStructure()) {
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
        } else {
            // Populate new components
            addressBuilder
                    .newProvinceCode(request.getNewProvinceCodeValue())
                    .newWardCode(request.getNewWardCodeValue())
                    .newStreet(request.getStreet());

            // Build formatted address string
            String fullNewAddress = buildNewAddressString(request);
            addressBuilder.fullNewAddress(fullNewAddress);
            log.info("Built new address: {}", fullNewAddress);
        }

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


        // Add street from nested structure or streetId from flat structure
        String street = request.getStreet();
        if (street != null && !street.isEmpty()) {
            // Street provided directly in nested structure
            sb.append(street).append(", ");
        } else if (request.getStreetId() != null) {
            // Street ID provided in flat structure
            Street streetEntity = streetRepository.findById(request.getStreetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Street not found with ID: " + request.getStreetId()));

            if (streetEntity.getPrefix() != null && !streetEntity.getPrefix().isEmpty()) {
                sb.append(streetEntity.getPrefix()).append(" ");
            }
            sb.append(streetEntity.getName()).append(", ");
        } else if (request.getProjectId() != null) {
            // Project provided
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            sb.append(project.getName()).append(", ");
        }

        // Add ward (use helper method for nested/flat compatibility)
        Integer wardId = request.getLegacyWardId();
        if (wardId != null) {
            LegacyWard ward = legacyWardRepository.findById(wardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ward not found with ID: " + wardId));

            if (ward.getType() != null && !ward.getType().isEmpty()) {
                sb.append(ward.getType()).append(" ");
            }
            sb.append(ward.getName()).append(", ");
        }

        // Add district (use helper method for nested/flat compatibility)
        Integer districtId = request.getLegacyDistrictId();
        if (districtId != null) {
            District district = legacyDistrictRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District not found with ID: " + districtId));

            if (district.getType() != null && !district.getType().isEmpty()) {
                sb.append(district.getType()).append(" ");
            }
            sb.append(district.getName()).append(", ");
        }

        // Add province (use helper method for nested/flat compatibility)
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


        // Add street from nested structure or streetId from flat structure
        String street = request.getStreet();
        if (street != null && !street.isEmpty()) {
            // Street provided directly in nested structure
            sb.append(street).append(", ");
        } else if (request.getStreetId() != null) {
            // Street ID provided in flat structure
            Street streetEntity = streetRepository.findById(request.getStreetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Street not found with ID: " + request.getStreetId()));

            if (streetEntity.getPrefix() != null && !streetEntity.getPrefix().isEmpty()) {
                sb.append(streetEntity.getPrefix()).append(" ");
            }
            sb.append(streetEntity.getName()).append(", ");
        } else if (request.getProjectId() != null) {
            // Project provided
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            sb.append(project.getName()).append(", ");
        }

        // Add ward (use helper method for nested/flat compatibility)
        String wardCode = request.getNewWardCodeValue();
        if (wardCode != null && !wardCode.isEmpty()) {
            Ward ward = wardRepository.findByCode(wardCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + wardCode));
            sb.append(ward.getName()).append(", ");
        }

        // Add province (use helper method for nested/flat compatibility)
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
                .streetId(request.getStreetId())
                .projectId(request.getProjectId());

        if (request.isLegacyStructure()) {
            // Use helper methods to get values from nested or flat structure
            builder.provinceId(request.getLegacyProvinceId())
                    .districtId(request.getLegacyDistrictId())
                    .wardId(request.getLegacyWardId());
        } else {
            // Use helper methods to get values from nested or flat structure
            builder.newProvinceCode(request.getNewProvinceCodeValue())
                    .newWardCode(request.getNewWardCodeValue());
        }

        return builder.build();
    }

    @Override
    public void validateAddressRequest(AddressCreationRequest request) {
        // Address type will be auto-detected if not provided, so we don't require it
        // Just validate that we have required fields for the detected type

        if (!request.hasRequiredFields()) {
            // Determine which structure is being used
            boolean hasLegacyData = request.getLegacy() != null ||
                    (request.getProvinceId() != null || request.getDistrictId() != null || request.getWardId() != null);
            boolean hasNewData = request.getNewAddress() != null ||
                    (request.getNewProvinceCode() != null || request.getNewWardCode() != null);

            if (hasLegacyData) {
                throw new IllegalArgumentException(
                        "For legacy address structure, provinceId, districtId, and wardId are required. " +
                        "Nested format: {\"legacy\": {\"provinceId\": 1, \"districtId\": 5, \"wardId\": 20}} " +
                        "or flat format: {\"addressType\": \"OLD\", \"provinceId\": 1, \"districtId\": 5, \"wardId\": 20}");
            } else if (hasNewData) {
                throw new IllegalArgumentException(
                        "For new address structure, provinceCode and wardCode are required. " +
                        "Nested format: {\"new\": {\"provinceCode\": \"01\", \"wardCode\": \"00004\"}} " +
                        "or flat format: {\"addressType\": \"NEW\", \"newProvinceCode\": \"01\", \"newWardCode\": \"00004\"}");
            } else {
                throw new IllegalArgumentException(
                        "Address data is required. Provide either legacy (provinceId/districtId/wardId) or new (provinceCode/wardCode) address structure");
            }
        }

        // Validate that at least street or project is provided (optional but recommended)
        // This is just a warning, not an error
        if (request.getStreetId() == null &&
            request.getProjectId() == null &&
            request.getStreet() == null) {
            log.warn("No street, streetId, or project was provided for address creation");
        }

        // Validate coordinates if provided
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new IllegalArgumentException("Both latitude and longitude must be provided together");
            }
        }
    }
}
