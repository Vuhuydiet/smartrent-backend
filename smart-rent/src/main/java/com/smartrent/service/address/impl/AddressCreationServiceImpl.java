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
        log.info("Creating address with type: {}", request.getAddressType());

        // Validate request
        validateAddressRequest(request);

        // Create address entity
        Address address = Address.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        // Build and set formatted address strings
        if (request.isOldStructure()) {
            String fullAddress = buildOldAddressString(request);
            address.setFullAddress(fullAddress);
            log.info("Built old address: {}", fullAddress);
        } else {
            String fullNewAddress = buildNewAddressString(request);
            address.setFullNewAddress(fullNewAddress);
            log.info("Built new address: {}", fullNewAddress);
        }

        // Save address
        address = addressRepository.save(address);
        log.info("Saved address with ID: {}", address.getAddressId());

        // Create and save metadata
        AddressMetadata metadata = createAddressMetadata(address, request);
        addressMetadataRepository.save(metadata);
        log.info("Saved address metadata with ID: {}", metadata.getMetadataId());

        return address;
    }

    @Override
    public String buildOldAddressString(AddressCreationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add street number
        if (request.getStreetNumber() != null && !request.getStreetNumber().isEmpty()) {
            sb.append(request.getStreetNumber()).append(" ");
        }

        // Add street or project
        if (request.getStreetId() != null) {
            Street street = streetRepository.findById(request.getStreetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Street not found with ID: " + request.getStreetId()));

            if (street.getPrefix() != null && !street.getPrefix().isEmpty()) {
                sb.append(street.getPrefix()).append(" ");
            }
            sb.append(street.getName()).append(", ");
        } else if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            sb.append(project.getName()).append(", ");
        }

        // Add ward
        LegacyWard ward = legacyWardRepository.findById(request.getWardId())
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with ID: " + request.getWardId()));

        if (ward.getType() != null && !ward.getType().isEmpty()) {
            sb.append(ward.getType()).append(" ");
        }
        sb.append(ward.getName()).append(", ");

        // Add district
        District district = legacyDistrictRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District not found with ID: " + request.getDistrictId()));

        if (district.getType() != null && !district.getType().isEmpty()) {
            sb.append(district.getType()).append(" ");
        }
        sb.append(district.getName()).append(", ");

        // Add province
        LegacyProvince province = legacyProvinceRepository.findById(request.getProvinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with ID: " + request.getProvinceId()));
        sb.append(province.getName());

        return sb.toString();
    }

    @Override
    public String buildNewAddressString(AddressCreationRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add street number
        if (request.getStreetNumber() != null && !request.getStreetNumber().isEmpty()) {
            sb.append(request.getStreetNumber()).append(" ");
        }

        // Add street or project
        if (request.getStreetId() != null) {
            Street street = streetRepository.findById(request.getStreetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Street not found with ID: " + request.getStreetId()));

            if (street.getPrefix() != null && !street.getPrefix().isEmpty()) {
                sb.append(street.getPrefix()).append(" ");
            }
            sb.append(street.getName()).append(", ");
        } else if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));
            sb.append(project.getName()).append(", ");
        }

        // Add ward
        Ward ward = wardRepository.findByCode(request.getNewWardCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found with code: " + request.getNewWardCode()));
        sb.append(ward.getName()).append(", ");

        // Add province
        Province province = provinceRepository.findByCode(request.getNewProvinceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found with code: " + request.getNewProvinceCode()));
        sb.append(province.getName());

        return sb.toString();
    }

    @Override
    public AddressMetadata createAddressMetadata(Address address, AddressCreationRequest request) {
        AddressMetadata.AddressMetadataBuilder builder = AddressMetadata.builder()
                .address(address)
                .addressType(request.getAddressType())
                .streetId(request.getStreetId())
                .projectId(request.getProjectId())
                .streetNumber(request.getStreetNumber());

        if (request.isOldStructure()) {
            builder.provinceId(request.getProvinceId())
                    .districtId(request.getDistrictId())
                    .wardId(request.getWardId());
        } else {
            builder.newProvinceCode(request.getNewProvinceCode())
                    .newWardCode(request.getNewWardCode());
        }

        return builder.build();
    }

    @Override
    public void validateAddressRequest(AddressCreationRequest request) {
        if (request.getAddressType() == null) {
            throw new IllegalArgumentException("Address type is required");
        }

        if (!request.hasRequiredFields()) {
            if (request.isOldStructure()) {
                throw new IllegalArgumentException(
                        "For old address structure, provinceId, districtId, and wardId are required");
            } else {
                throw new IllegalArgumentException(
                        "For new address structure, newProvinceCode and newWardCode are required");
            }
        }

        // Validate that at least street or project is provided (optional but recommended)
        // This is just a warning, not an error
        if (request.getStreetId() == null && request.getProjectId() == null) {
            log.warn("Neither street nor project was provided for address creation");
        }

        // Validate coordinates if provided
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new IllegalArgumentException("Both latitude and longitude must be provided together");
            }
        }
    }
}
