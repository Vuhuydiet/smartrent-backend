package com.smartrent.service.admin.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.request.AdminFilterRequest;
import com.smartrent.dto.request.AdminUpdateRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.infra.exception.AdminNotFoundException;
import com.smartrent.infra.exception.EmailExistingException;
import com.smartrent.infra.exception.InvalidRoleException;
import com.smartrent.infra.exception.PhoneExistingException;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.RoleRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Role;
import com.smartrent.mapper.AdminMapper;
import com.smartrent.service.admin.AdminService;
import com.smartrent.utility.MaskingUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminServiceImpl implements AdminService {

  AdminRepository adminRepository;

  RoleRepository roleRepository;

  PasswordEncoder passwordEncoder;

  AdminMapper adminMapper;

  @Override
  public AdminCreationResponse createAdmin(AdminCreationRequest request) {

    if (adminRepository.existsByEmail(request.getEmail())) {
      throw new EmailExistingException();
    }

    if (adminRepository.existsByPhoneCodeAndPhoneNumber(request.getPhoneCode(),
        request.getPhoneNumber())) {
      throw new PhoneExistingException();
    }

    Admin admin = adminMapper.mapFromAdminCreationRequestToAminEntity(request);

    admin.setPassword(passwordEncoder.encode(request.getPassword()));

    List<Role> roles = roleRepository.getRolesByRoleIdIn(request.getRoles());

    if (roles.size() < request.getRoles().size()) {
      throw new InvalidRoleException();
    }

    admin.setRoles(roles);

    adminRepository.saveAndFlush(admin);

    return adminMapper.mapFromAdminEntityToAdminCreationResponse(admin);
  }

  @Override
  @Transactional(readOnly = true)
  public GetAdminResponse getAdminById(String id) {
    Admin admin = adminRepository.findById(id).orElseThrow(UserNotFoundException::new);

    log.info(
        "getUserById: id={}, firstName={}, lastName={}, phoneNumber={}, email={}",
        MaskingUtil.maskFromIndex(admin.getAdminId(), Constants.USER_ID_MASKING_INDEX),
        admin.getFirstName(),
        admin.getLastName(), MaskingUtil.maskFromIndex(admin.getPhoneCode() + admin.getPhoneNumber(),
            Constants.PHONE_MASKING_INDEX),
        MaskingUtil.maskEmail(admin.getEmail()));

    return adminMapper.mapFromAdminEntityToGetAdminResponse(admin);
  }

  private static final Set<String> ADMIN_SORTABLE_FIELDS = Set.of("firstName", "lastName", "createdAt");

  private Sort buildSort(AdminFilterRequest filter, Set<String> allowedFields, String defaultField) {
    String field = filter.getSortBy();
    if (field == null || !allowedFields.contains(field)) {
      field = defaultField;
    }
    Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;
    return Sort.by(direction, field);
  }

  /**
   * Parses a single date ("2026-02-09") or a "from..to" range. Either side of a
   * range may be omitted for an open-ended bound. Returns {@code null} when the
   * input is blank.
   */
  private DateRange parseDateOrRange(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.trim();
    if (s.isEmpty()) {
      return null;
    }
    int idx = s.indexOf("..");
    if (idx < 0) {
      LocalDate date = LocalDate.parse(s);
      return new DateRange(date.atStartOfDay(), date.atTime(LocalTime.MAX));
    }
    String fromStr = s.substring(0, idx).trim();
    String toStr = s.substring(idx + 2).trim();
    LocalDateTime from = fromStr.isEmpty() ? null : LocalDate.parse(fromStr).atStartOfDay();
    LocalDateTime to = toStr.isEmpty() ? null : LocalDate.parse(toStr).atTime(LocalTime.MAX);
    if (from == null && to == null) {
      return null;
    }
    return new DateRange(from, to);
  }

  private record DateRange(LocalDateTime from, LocalDateTime to) {}

  @Override
  @Transactional(readOnly = true)
  public PageResponse<GetAdminResponse> getAllAdmins(AdminFilterRequest filter) {
    log.info("Fetching all admins - page: {}, size: {}, filters: {}",
        filter.getPage(), filter.getSize(), filter.getFilters());

    // Validate and set defaults
    int page = Math.max(filter.getPage() != null ? filter.getPage() : 1, 1);
    int size = Math.max(filter.getSize() != null ? filter.getSize() : 20, 1);

    Sort sort = buildSort(filter, ADMIN_SORTABLE_FIELDS, "createdAt");
    Pageable pageable = PageRequest.of(page - 1, size, sort);

    // Build dynamic specification from filter request
    Specification<Admin> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

    // Search filters - apply contains search for multiple fields
    if (filter.hasFilter("adminId") || filter.hasFilter("firstName") ||
        filter.hasFilter("lastName") || filter.hasFilter("email") || filter.hasFilter("phoneNumber")) {

      spec = spec.and((root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();

        // adminId filter
        if (filter.hasFilter("adminId")) {
          String value = "%" + filter.getStringFilter("adminId").toLowerCase() + "%";
          predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("adminId")), value));
        }

        // firstName filter
        if (filter.hasFilter("firstName")) {
          String value = "%" + filter.getStringFilter("firstName").toLowerCase() + "%";
          predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), value));
        }

        // lastName filter
        if (filter.hasFilter("lastName")) {
          String value = "%" + filter.getStringFilter("lastName").toLowerCase() + "%";
          predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), value));
        }

        // email filter
        if (filter.hasFilter("email")) {
          String value = "%" + filter.getStringFilter("email").toLowerCase() + "%";
          predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), value));
        }

        // phoneNumber filter
        if (filter.hasFilter("phoneNumber")) {
          String value = "%" + filter.getStringFilter("phoneNumber").toLowerCase() + "%";
          predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), value));
        }

        // Combine with OR if multiple filters exist
        if (predicates.isEmpty()) {
          return criteriaBuilder.conjunction();
        } else if (predicates.size() == 1) {
          return predicates.get(0);
        } else {
          return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        }
      });
    }

    // Role filter - filter admins that have any of the specified roles
    if (filter.hasFilter("role")) {
      String rolesCsv = filter.getStringFilter("role");
      List<String> normalizedRoleIds = normalizeRoleIds(rolesCsv);

      if (!normalizedRoleIds.isEmpty()) {
        for (String roleId : normalizedRoleIds) {
          spec = spec.and((root, query, criteriaBuilder) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var subRoot = subquery.from(Admin.class);
            var subJoin = subRoot.join("roles");

            subquery.select(criteriaBuilder.literal(1L))
                .where(
                    criteriaBuilder.equal(subRoot.get("adminId"), root.get("adminId")),
                    criteriaBuilder.equal(criteriaBuilder.upper(subJoin.get("roleId")), roleId));

            return criteriaBuilder.exists(subquery);
          });
        }
      }
    }

    // createdAt filter - single date ("2026-02-09") or range ("2026-02-09..2026-03-10")
    if (filter.hasFilter("createdAt")) {
      DateRange range = parseDateOrRange(filter.getStringFilter("createdAt"));
      if (range != null) {
        DateRange finalRange = range;
        spec = spec.and((root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (finalRange.from() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), finalRange.from()));
          }
          if (finalRange.to() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), finalRange.to()));
          }
          return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
      }
    }

    Page<Admin> adminPage = adminRepository.findAll(spec, pageable);

    List<GetAdminResponse> adminResponses = adminPage.getContent().stream()
        .map(adminMapper::mapFromAdminEntityToGetAdminResponse)
        .collect(Collectors.toList());

    log.info("Successfully retrieved {} admins", adminResponses.size());

    return PageResponse.<GetAdminResponse>builder()
        .page(page)
        .size(adminPage.getSize())
        .totalPages(adminPage.getTotalPages())
        .totalElements(adminPage.getTotalElements())
        .data(adminResponses)
        .build();
  }

  private List<String> normalizeRoleIds(String rolesCsv) {
    if (StringUtils.isBlank(rolesCsv)) {
      return List.of();
    }
    return Arrays.stream(rolesCsv.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .map(role -> role.toUpperCase())
        .distinct()
        .toList();
  }

  @Override
  @Transactional
  public GetAdminResponse updateAdmin(String adminId, AdminUpdateRequest request) {
    log.info("Updating admin with ID: {}", adminId);

    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> {
          log.error("Admin not found with ID: {}", adminId);
          return new AdminNotFoundException();
        });

    // Update email if provided and different
    if (request.getEmail() != null && !request.getEmail().equals(admin.getEmail())) {
      if (adminRepository.existsByEmail(request.getEmail())) {
        throw new EmailExistingException();
      }
      admin.setEmail(request.getEmail());
    }

    // Update phone if provided and different
    if (request.getPhoneCode() != null && request.getPhoneNumber() != null) {
      if (!request.getPhoneCode().equals(admin.getPhoneCode())
          || !request.getPhoneNumber().equals(admin.getPhoneNumber())) {
        if (adminRepository.existsByPhoneCodeAndPhoneNumber(
            request.getPhoneCode(), request.getPhoneNumber())) {
          throw new PhoneExistingException();
        }
        admin.setPhoneCode(request.getPhoneCode());
        admin.setPhoneNumber(request.getPhoneNumber());
      }
    }

    // Update first name if provided
    if (request.getFirstName() != null) {
      admin.setFirstName(request.getFirstName());
    }

    // Update last name if provided
    if (request.getLastName() != null) {
      admin.setLastName(request.getLastName());
    }

    // Update roles if provided
    if (request.getRoles() != null && !request.getRoles().isEmpty()) {
      List<Role> roles = roleRepository.getRolesByRoleIdIn(request.getRoles());
      if (roles.size() < request.getRoles().size()) {
        throw new InvalidRoleException();
      }
      admin.setRoles(roles);
    }

    admin = adminRepository.saveAndFlush(admin);
    log.info("Successfully updated admin: {}", adminId);

    return adminMapper.mapFromAdminEntityToGetAdminResponse(admin);
  }

  @Override
  @Transactional
  public void deleteAdmin(String adminId) {
    log.info("Deleting admin with ID: {}", adminId);

    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> {
          log.error("Admin not found with ID: {}", adminId);
          return new AdminNotFoundException();
        });

    adminRepository.delete(admin);
    log.info("Successfully deleted admin: {}", adminId);
  }

}
