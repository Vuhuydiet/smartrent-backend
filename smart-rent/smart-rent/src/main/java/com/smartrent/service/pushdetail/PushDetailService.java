package com.smartrent.service.pushdetail;

import com.smartrent.dto.response.PushDetailResponse;

import java.util.List;

/**
 * Service interface for managing push pricing details
 */
public interface PushDetailService {

    /**
     * Get all active push details
     *
     * @return List of active push details
     */
    List<PushDetailResponse> getAllActiveDetails();

    /**
     * Get all push details (including inactive)
     *
     * @return List of all push details
     */
    List<PushDetailResponse> getAllDetails();

    /**
     * Get push detail by detail code
     *
     * @param detailCode Detail code (SINGLE_PUSH, PUSH_PACKAGE_3, etc.)
     * @return Push detail
     */
    PushDetailResponse getDetailByCode(String detailCode);

    /**
     * Get push detail by ID
     *
     * @param pushDetailId Push detail ID
     * @return Push detail
     */
    PushDetailResponse getDetailById(Long pushDetailId);
}

