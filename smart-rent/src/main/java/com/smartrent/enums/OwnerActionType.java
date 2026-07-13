package com.smartrent.enums;

/**
 * Type of action required from the listing owner.
 *
 * EDIT_RESUBMIT was already in use as a stored value for listing_owner_actions.required_action
 * (e.g. rows created before this enum's write paths were locked down to UPDATE_LISTING /
 * CONTACT_SUPPORT) but was never a declared constant here. Hibernate's EnumType.STRING mapping
 * on ListingOwnerAction.requiredAction throws IllegalArgumentException while hydrating any such
 * row, which crashed every bulk read of owner actions (e.g. GET /v1/listings/my-listings) the
 * moment one of the returned listings had a pending EDIT_RESUBMIT action. Restored as a first-
 * class value instead of just being read; see OwnerActionTypeConverter for the runtime safety
 * net covering any other legacy/unknown value.
 */
public enum OwnerActionType {
    UPDATE_LISTING,
    EDIT_RESUBMIT,
    CONTACT_SUPPORT
}
