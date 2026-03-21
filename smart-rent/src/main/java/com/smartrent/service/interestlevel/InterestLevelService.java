package com.smartrent.service.interestlevel;

import com.smartrent.dto.response.InterestLevelResponse;

public interface InterestLevelService {

    InterestLevelResponse getInterestLevel(Long listingId);
}
