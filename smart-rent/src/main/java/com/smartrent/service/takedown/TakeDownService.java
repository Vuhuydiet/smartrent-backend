package com.smartrent.service.takedown;

import com.smartrent.dto.request.TakeDownListingRequest;
import com.smartrent.dto.response.TakeDownResponse;

public interface TakeDownService {

    TakeDownResponse takeDownListing(String userId, TakeDownListingRequest request);
}
