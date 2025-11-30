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
   * Generate both title and description by calling AI service twice with different prompts.
   */
  public ListingDescriptionResponse generateDescription(ListingDescriptionRequest req) {
    // Generate title
    String titlePrompt = buildTitlePrompt(req);
    CompletionRequestModel titleRequest = CompletionRequestModel.builder()
        .prompt(titlePrompt)
        .build();
    CompletionResponseModel titleResponse = aiConnector.generateCompletion(titleRequest);
    String generatedTitle = titleResponse != null ? titleResponse.getText() : null;

    // Generate description
    String descriptionPrompt = buildDescriptionPrompt(req);
    CompletionRequestModel descriptionRequest = CompletionRequestModel.builder()
        .prompt(descriptionPrompt)
        .build();
    CompletionResponseModel descriptionResponse = aiConnector.generateCompletion(descriptionRequest);
    String generatedDescription = descriptionResponse != null ? descriptionResponse.getText() : null;

    return ListingDescriptionResponse.builder()
        .title(generatedTitle)
        .description(generatedDescription)
        .build();
  }

  /**
   * Build prompt for generating listing title.
   */
  private String buildTitlePrompt(ListingDescriptionRequest req) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("Generate a short, catchy listing title in Vietnamese.");
    prompt.append(" Tone: ").append(req.getTone() != null ? req.getTone() : "friendly").append(".");
    prompt.append(" Keep it concise (under 10 words).");

    if (req.getPropertyType() != null) {
      prompt.append(" Property type: ").append(req.getPropertyType()).append(".");
    }
    if (req.getBedrooms() != null) {
      prompt.append(" Bedrooms: ").append(req.getBedrooms()).append(".");
    }
    if (req.getArea() != null) {
      prompt.append(" Area: ").append(req.getArea()).append(" m2.");
    }
    if (req.getFurnishing() != null) {
      prompt.append(" Furnishing: ").append(req.getFurnishing()).append(".");
    }
    if (req.getAddressText() != null) {
      if (req.getAddressText().getNewAddress() != null) {
        prompt.append(" Location: ").append(req.getAddressText().getNewAddress()).append(".");
      }
    }

    return prompt.toString();
  }

  /**
   * Build prompt for generating detailed listing description.
   */
  private String buildDescriptionPrompt(ListingDescriptionRequest req) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("Generate a detailed, attractive listing description in Vietnamese.");
    prompt.append(" Tone: ").append(req.getTone() != null ? req.getTone() : "friendly").append(".");

    if (req.getCategory() != null) {
      prompt.append(" Category: ").append(req.getCategory()).append(".");
    }
    if (req.getPropertyType() != null) {
      prompt.append(" Property type: ").append(req.getPropertyType()).append(".");
    }
    if (req.getPrice() != null) {
      prompt.append(" Price: ").append(req.getPrice());
      if (req.getPriceUnit() != null) prompt.append("/").append(req.getPriceUnit());
      prompt.append(".");
    }
    if (req.getAddressText() != null) {
      if (req.getAddressText().getNewAddress() != null) {
        prompt.append(" Address: ").append(req.getAddressText().getNewAddress()).append(".");
      } else if (req.getAddressText().getLegacy() != null) {
        prompt.append(" Address: ").append(req.getAddressText().getLegacy()).append(".");
      }
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
    if (req.getDirection() != null) {
      prompt.append(" Direction: ").append(req.getDirection()).append(".");
    }
    if (req.getFurnishing() != null) {
      prompt.append(" Furnishing: ").append(req.getFurnishing()).append(".");
    }
    if (req.getAmenities() != null && !req.getAmenities().isEmpty()) {
      prompt.append(" Amenities: ").append(String.join(", ", req.getAmenities())).append(".");
    }
    if (req.getWaterPrice() != null) {
      prompt.append(" Water price: ").append(req.getWaterPrice()).append(".");
    }
    if (req.getElectricityPrice() != null) {
      prompt.append(" Electricity price: ").append(req.getElectricityPrice()).append(".");
    }
    if (req.getInternetPrice() != null) {
      prompt.append(" Internet price: ").append(req.getInternetPrice()).append(".");
    }
    if (req.getServiceFee() != null) {
      prompt.append(" Service fee: ").append(req.getServiceFee()).append(".");
    }

    if (req.getMinWords() != null && req.getMaxWords() != null) {
      prompt.append(" Keep the description between ").append(req.getMinWords())
            .append(" and ").append(req.getMaxWords()).append(" words.");
    } else if (req.getMaxWords() != null) {
      prompt.append(" Keep the description under ").append(req.getMaxWords()).append(" words.");
    } else if (req.getMinWords() != null) {
      prompt.append(" Keep the description at least ").append(req.getMinWords()).append(" words.");
    }

    return prompt.toString();
  }
}
