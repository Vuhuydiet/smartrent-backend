package com.smartrent.service.ai;

import com.smartrent.dto.request.ListingDescriptionRequest;
import com.smartrent.infra.connector.SmartRentAiConnector;
import com.smartrent.dto.response.ListingDescriptionResponse;
import com.smartrent.infra.connector.model.CompletionRequestModel;
import com.smartrent.infra.connector.model.CompletionResponseModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ListingDescriptionService {

  private final SmartRentAiConnector aiConnector;

  /**
   * Build a simple prompt from listing fields and call the AI completion endpoint.
   */
  public ListingDescriptionResponse generateDescription(ListingDescriptionRequest req) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("Generate a short, attractive listing description in Vietnamese.");
    prompt.append(" Tone: ").append(req.getTone() != null ? req.getTone() : "friendly").append(".");

    if (req.getTitle() != null) {
      prompt.append(" Title: ").append(req.getTitle()).append(".");
    }
    if (req.getAddressText() != null) {
      prompt.append(" Address: ").append(req.getAddressText()).append(".");
    }
    if (req.getArea() != null) {
      prompt.append(" Area: ").append(req.getArea()).append(" m2.");
    }
    if (req.getBedrooms() != null) {
      prompt.append(" Bedrooms: ").append(req.getBedrooms()).append(".");
    }
    if (req.getBathrooms() != null) {
      prompt.append(" Bathrooms: ").append(req.getBathrooms()).append(".");
    }
    if (req.getFurnishing() != null) {
      prompt.append(" Furnishing: ").append(req.getFurnishing()).append(".");
    }
    if (req.getPropertyType() != null) {
      prompt.append(" Property type: ").append(req.getPropertyType()).append(".");
    }
    if (req.getPrice() != null) {
      prompt.append(" Price: ").append(req.getPrice());
      if (req.getPriceUnit() != null) prompt.append("/").append(req.getPriceUnit());
      prompt.append(".");
    }
    if (req.getAmenityIds() != null && !req.getAmenityIds().isEmpty()) {
      prompt.append(" Amenities (ids): ").append(req.getAmenityIds()).append(".");
    }

    if (req.getMaxWords() != null) {
      prompt.append(" Keep the description under ").append(req.getMaxWords()).append(" words.");
    }

    // Build completion request
    CompletionRequestModel completionReq = CompletionRequestModel.builder()
        .prompt(prompt.toString())
        .build();

    // call external AI completion endpoint via Feign
    CompletionResponseModel completionResp = aiConnector.generateCompletion(completionReq);

  // Build and return the API response DTO (do not expose connector DTOs)
  ListingDescriptionResponse resp = ListingDescriptionResponse.builder()
    .generatedDescription(completionResp != null ? completionResp.getText() : null)
    .build();

  return resp;
  }
}
