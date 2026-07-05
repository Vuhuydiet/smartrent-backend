package com.smartrent.service.view;

import com.smartrent.dto.request.ViewTrackRequest;
import com.smartrent.dto.response.ViewTrackResponse;

/**
 * Service for tracking listing detail page views
 */
public interface ViewService {

    /**
     * Track a view on a listing's detail page. Callable anonymously; no
     * authentication required. Deduped per IP address within a short window
     * so page refreshes/reloads don't inflate the count.
     *
     * @param request   View tracking request containing listing ID
     * @param userId    Authenticated user ID if present, otherwise null
     * @param ipAddress IP address of the request
     * @param userAgent User agent string
     * @return View tracking response
     */
    ViewTrackResponse trackView(ViewTrackRequest request, String userId, String ipAddress, String userAgent);
}
