package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic filter request for admin list endpoints
 * Accepts key:value pairs for flexible filtering instead of hardcoded search
 * parameters
 * 
 * Example: { "filters": { "firstName": "John", "isBroker": true, "status":
 * "ACTIVE" } }
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Dynamic filter criteria for admin list endpoints - accepts any key:value pairs")
public class AdminFilterRequest {

	@Schema(description = "Page number (1-based indexing)", example = "1", defaultValue = "1")
	@Builder.Default
	Integer page = 1;

	@Schema(description = "Page size (items per page)", example = "20", defaultValue = "20")
	@Builder.Default
	Integer size = 20;

	@Schema(description = """
			Dynamic filters as key:value pairs. Available filter keys depend on the endpoint:

			**FOR ADMIN LIST (/v1/admin):**
			- adminId: String (exact match)
			- firstName: String (contains search)
			- lastName: String (contains search)
			- email: String (contains search)
			- phoneNumber: String (contains search)
			- role: String (CSV format, e.g. "SA,UA,CM" - only admins with these roles)

			**FOR USER LIST (/v1/users):**
			- userId: String (contains search)
			- firstName: String (contains search)
			- lastName: String (contains search)
			- email: String (contains search)
			- phoneNumber: String (contains search)
			- isBroker: Boolean (true/false)

			**FOR LISTING ADMIN (/v1/listings/admin/list):**
			- See ListingFilterRequest documentation (30+ filter fields supported)
			- verified: Boolean
			- vipType: String (NORMAL, SILVER, GOLD, DIAMOND)
			- moderationStatus: String (PENDING_REVIEW, APPROVED, REJECTED, etc.)
			- minPrice: Number
			- maxPrice: Number
			- userId: String (filter by listing owner)

			**FOR NEWS ADMIN (/v1/admin/news):**
			- title: String (contains search)
			- summary: String (contains search)
			- category: String (BLOG, NEWS, GUIDE, etc.)
			- tag: String (comma-separated tags)
			- status: String (DRAFT, PUBLISHED, ARCHIVED)
			""", example = "{ \"firstName\": \"John\", \"isBroker\": true }")
	@Builder.Default
	Map<String, Object> filters = new HashMap<>();

	@Schema(description = "Sort field (optional). Supported values depend on endpoint. Example: 'createdAt', 'firstName'", example = "createdAt")
	String sortBy;

	@Schema(description = "Sort direction: ASC or DESC (default: DESC)", example = "DESC", defaultValue = "DESC")
	@Builder.Default
	String sortDirection = "DESC";

	// Convenience getters for common filters
	public String getStringFilter(String key) {
		Object value = filters.get(key);
		return value != null ? value.toString() : null;
	}

	public Boolean getBooleanFilter(String key) {
		Object value = filters.get(key);
		if (value == null)
			return null;
		if (value instanceof Boolean)
			return (Boolean) value;
		if (value instanceof String)
			return Boolean.parseBoolean((String) value);
		return false;
	}

	public Integer getIntegerFilter(String key) {
		Object value = filters.get(key);
		if (value == null)
			return null;
		if (value instanceof Integer)
			return (Integer) value;
		if (value instanceof Number)
			return ((Number) value).intValue();
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	public Long getLongFilter(String key) {
		Object value = filters.get(key);
		if (value == null)
			return null;
		if (value instanceof Long)
			return (Long) value;
		if (value instanceof Number)
			return ((Number) value).longValue();
		if (value instanceof String) {
			try {
				return Long.parseLong((String) value);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	public Double getDoubleFilter(String key) {
		Object value = filters.get(key);
		if (value == null)
			return null;
		if (value instanceof Double)
			return (Double) value;
		if (value instanceof Number)
			return ((Number) value).doubleValue();
		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	public boolean hasFilter(String key) {
		return filters.containsKey(key) && filters.get(key) != null;
	}
}
