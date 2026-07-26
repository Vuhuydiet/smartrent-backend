package com.smartrent.service.contactreveal;

import com.smartrent.dto.request.RevealContactRequest;
import com.smartrent.dto.response.RevealContactResponse;

public interface ContactRevealService {

    /**
     * Log a contact reveal and return the seller's contact details.
     *
     * @param sellerUserId whose contact is being revealed
     * @param request      channel + optional listing context
     * @param viewerUserId the authenticated user performing the reveal
     * @param ipAddress    request IP (audit)
     * @param userAgent    request User-Agent (audit)
     */
    RevealContactResponse revealContact(String sellerUserId,
                                        RevealContactRequest request,
                                        String viewerUserId,
                                        String ipAddress,
                                        String userAgent);
}
