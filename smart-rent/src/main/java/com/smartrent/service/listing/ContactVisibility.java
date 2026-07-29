package com.smartrent.service.listing;

import com.smartrent.dto.response.ListingCardListResponse;
import com.smartrent.dto.response.ListingCardResponse;
import com.smartrent.dto.response.ListingCursorResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.UserCreationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Withholds a seller's real contact details (phone / email / zalo) from
 * listing responses served to <b>anonymous</b> callers, so contact info is not
 * scrapeable without logging in. Names, avatars and broker status are kept.
 *
 * <p>Gated by {@code application.contact.strip-anonymous-contact} (default
 * {@code false}). It is shipped OFF because enabling it requires two coupled
 * changes to be in place first:
 * <ul>
 *   <li>the web frontend must render its "log in to view contact" affordance
 *       from an availability signal rather than the value's presence (today it
 *       keys off the value, so a stripped payload would show "no contact"); and</li>
 *   <li>the AI service must forward the user's token on its listing calls so
 *       authenticated chat users still receive contact (guests are stripped,
 *       which the gated chat card already handles).</li>
 * </ul>
 * Flip the flag to {@code true} once those are deployed and verified.
 */
@Component
@Slf4j
public class ContactVisibility {

    private final boolean stripEnabled;

    public ContactVisibility(
            @Value("${application.contact.strip-anonymous-contact:false}") boolean stripEnabled) {
        this.stripEnabled = stripEnabled;
    }

    /** True when the current request has no authenticated (non-anonymous) user. */
    private boolean isAnonymous() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName());
    }

    private boolean shouldStrip() {
        return stripEnabled && isAnonymous();
    }

    public ListingResponse apply(ListingResponse response) {
        if (response != null && shouldStrip()) {
            scrub(response);
        }
        return response;
    }

    public List<ListingResponse> applyDetails(List<ListingResponse> responses) {
        if (responses != null && shouldStrip()) {
            responses.forEach(this::scrub);
        }
        return responses;
    }

    public ListingCardListResponse apply(ListingCardListResponse response) {
        if (response != null && shouldStrip()) {
            scrubCards(response.getListings());
        }
        return response;
    }

    public List<ListingCardResponse> applyCards(List<ListingCardResponse> cards) {
        if (cards != null && shouldStrip()) {
            scrubCards(cards);
        }
        return cards;
    }

    public ListingCursorResponse apply(ListingCursorResponse response) {
        if (response != null && shouldStrip()) {
            scrubCards(response.getItems());
        }
        return response;
    }

    private void scrub(ListingResponse response) {
        response.setOwnerContactPhoneNumber(null);
        response.setOwnerZaloLink(null);
        response.setContactPhone(null);

        UserCreationResponse user = response.getUser();
        if (user != null) {
            user.setPhoneCode(null);
            user.setPhoneNumber(null);
            user.setEmail(null);
            user.setContactPhoneNumber(null);
        }
    }

    private void scrubCards(List<ListingCardResponse> cards) {
        if (cards == null) {
            return;
        }
        cards.forEach(this::scrubCard);
    }

    private void scrubCard(ListingCardResponse card) {
        if (card == null || card.getUser() == null) {
            return;
        }
        // UserCard is builder-only (no setters), so rebuild it without the
        // contact-bearing fields.
        ListingCardResponse.UserCard user = card.getUser();
        card.setUser(
                ListingCardResponse.UserCard.builder()
                        .userId(user.getUserId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(null)
                        .contactPhoneNumber(null)
                        .contactPhoneVerified(user.getContactPhoneVerified())
                        .avatarUrl(user.getAvatarUrl())
                        .isBroker(user.getIsBroker())
                        .brokerVerificationStatus(user.getBrokerVerificationStatus())
                        .build());
    }
}
