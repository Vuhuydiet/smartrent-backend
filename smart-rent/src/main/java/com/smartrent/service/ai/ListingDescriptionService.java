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

    // Role and context
    prompt.append("Bạn là một chuyên gia bất động sản chuyên tạo tiêu đề hấp dẫn cho tin đăng cho thuê/bán nhà.\n\n");

    // Task description
    prompt.append("NHIỆM VỤ: Tạo một tiêu đề ngắn gọn, hấp dẫn và thu hút sự chú ý cho tin đăng bất động sản.\n\n");

    // Tone requirement
    String tone = req.getTone() != null ? req.getTone() : "friendly";
    prompt.append("PHONG CÁCH: ").append(tone).append("\n\n");

    // Length requirements
    prompt.append("ĐỘ DÀI: ");
    if (req.getTitleMinWords() != null && req.getTitleMaxWords() != null) {
      prompt.append("Từ ").append(req.getTitleMinWords())
            .append(" đến ").append(req.getTitleMaxWords()).append(" từ.");
    } else if (req.getTitleMaxWords() != null) {
      prompt.append("Tối đa ").append(req.getTitleMaxWords()).append(" từ.");
    } else if (req.getTitleMinWords() != null) {
      prompt.append("Ít nhất ").append(req.getTitleMinWords()).append(" từ.");
    } else {
      prompt.append("Ngắn gọn (dưới 10 từ).");
    }
    prompt.append("\n\n");

    // Property information
    prompt.append("THÔNG TIN BẤT ĐỘNG SẢN:\n");
    if (req.getPropertyType() != null) {
      prompt.append("- Loại hình: ").append(req.getPropertyType()).append("\n");
    }
    if (req.getBedrooms() != null) {
      prompt.append("- Số phòng ngủ: ").append(req.getBedrooms()).append("\n");
    }
    if (req.getArea() != null) {
      prompt.append("- Diện tích: ").append(req.getArea()).append(" m²\n");
    }
    if (req.getFurnishing() != null) {
      prompt.append("- Tình trạng nội thất: ").append(req.getFurnishing()).append("\n");
    }
    if (req.getAddressText() != null && req.getAddressText().getNewAddress() != null) {
      prompt.append("- Vị trí: ").append(req.getAddressText().getNewAddress()).append("\n");
    }

    // Guidelines
    prompt.append("\nHƯỚNG DẪN:\n");
    prompt.append("- Làm nổi bật điểm mạnh của bất động sản\n");
    prompt.append("- Sử dụng từ ngữ tích cực và hấp dẫn\n");
    prompt.append("- Ưu tiên đề cập vị trí nếu là điểm mạnh\n");
    prompt.append("- Tránh dùng từ ngữ cường điệu quá mức\n\n");

    // Output format
    prompt.append("ĐỊNH DẠNG ĐẦU RA: Chỉ trả về tiêu đề, không kèm giải thích hay chú thích.");

    return prompt.toString();
  }

  /**
   * Build prompt for generating detailed listing description.
   */
  private String buildDescriptionPrompt(ListingDescriptionRequest req) {
    StringBuilder prompt = new StringBuilder();

    // Role and context
    prompt.append("Bạn là một chuyên gia bất động sản có nhiều năm kinh nghiệm trong việc tạo mô tả hấp dẫn cho tin đăng cho thuê/bán nhà.\n\n");

    // Task description
    prompt.append("NHIỆM VỤ: Viết một mô tả chi tiết, hấp dẫn và chuyên nghiệp cho tin đăng bất động sản để thu hút khách hàng tiềm năng.\n\n");

    // Tone requirement
    String tone = req.getTone() != null ? req.getTone() : "friendly";
    prompt.append("PHONG CÁCH: ").append(tone).append(", chuyên nghiệp và thuyết phục\n\n");

    // Length requirements
    if (req.getDescriptionMinWords() != null || req.getDescriptionMaxWords() != null) {
      prompt.append("ĐỘ DÀI: ");
      if (req.getDescriptionMinWords() != null && req.getDescriptionMaxWords() != null) {
        prompt.append("Từ ").append(req.getDescriptionMinWords())
              .append(" đến ").append(req.getDescriptionMaxWords()).append(" từ.");
      } else if (req.getDescriptionMaxWords() != null) {
        prompt.append("Tối đa ").append(req.getDescriptionMaxWords()).append(" từ.");
      } else if (req.getDescriptionMinWords() != null) {
        prompt.append("Ít nhất ").append(req.getDescriptionMinWords()).append(" từ.");
      }
      prompt.append("\n\n");
    }

    // Property basic information
    prompt.append("THÔNG TIN CƠ BẢN:\n");
    if (req.getCategory() != null) {
      prompt.append("- Danh mục: ").append(req.getCategory()).append("\n");
    }
    if (req.getPropertyType() != null) {
      prompt.append("- Loại hình: ").append(req.getPropertyType()).append("\n");
    }
    if (req.getPrice() != null) {
      prompt.append("- Giá: ").append(req.getPrice());
      if (req.getPriceUnit() != null) prompt.append("/").append(req.getPriceUnit());
      prompt.append("\n");
    }
    if (req.getAddressText() != null) {
      String address = req.getAddressText().getNewAddress() != null
          ? req.getAddressText().getNewAddress()
          : req.getAddressText().getLegacy();
      if (address != null) {
        prompt.append("- Địa chỉ: ").append(address).append("\n");
      }
    }

    // Property details
    prompt.append("\nĐẶC ĐIỂM BẤT ĐỘNG SẢN:\n");
    if (req.getArea() != null) {
      prompt.append("- Diện tích: ").append(req.getArea()).append(" m²\n");
    }
    if (req.getBedrooms() != null) {
      prompt.append("- Số phòng ngủ: ").append(req.getBedrooms()).append("\n");
    }
    if (req.getBathrooms() != null) {
      prompt.append("- Số phòng tắm: ").append(req.getBathrooms()).append("\n");
    }
    if (req.getDirection() != null) {
      prompt.append("- Hướng: ").append(req.getDirection()).append("\n");
    }
    if (req.getFurnishing() != null) {
      prompt.append("- Tình trạng nội thất: ").append(req.getFurnishing()).append("\n");
    }

    // Amenities
    if (req.getAmenities() != null && !req.getAmenities().isEmpty()) {
      prompt.append("\nTIỆN ÍCH:\n");
      for (String amenity : req.getAmenities()) {
        prompt.append("- ").append(amenity).append("\n");
      }
    }

    // Utility costs
    boolean hasUtilityCosts = req.getWaterPrice() != null || req.getElectricityPrice() != null
        || req.getInternetPrice() != null || req.getServiceFee() != null;
    if (hasUtilityCosts) {
      prompt.append("\nCHI PHÍ DỊCH VỤ:\n");
      if (req.getWaterPrice() != null) {
        prompt.append("- Tiền nước: ").append(req.getWaterPrice()).append("\n");
      }
      if (req.getElectricityPrice() != null) {
        prompt.append("- Tiền điện: ").append(req.getElectricityPrice()).append("\n");
      }
      if (req.getInternetPrice() != null) {
        prompt.append("- Tiền internet: ").append(req.getInternetPrice()).append("\n");
      }
      if (req.getServiceFee() != null) {
        prompt.append("- Phí dịch vụ: ").append(req.getServiceFee()).append("\n");
      }
    }

    // Guidelines
    prompt.append("\nHƯỚNG DẪN VIẾT MÔ TẢ:\n");
    prompt.append("1. Bắt đầu với câu mở đầu hấp dẫn để thu hút người đọc\n");
    prompt.append("2. Mô tả vị trí và các tiện ích xung quanh (giao thông, trường học, chợ, công viên...)\n");
    prompt.append("3. Nêu rõ các đặc điểm nổi bật của bất động sản (diện tích, số phòng, hướng, nội thất...)\n");
    prompt.append("4. Nhấn mạnh các tiện nghi và tiện ích đi kèm\n");
    prompt.append("5. Đề cập chi phí dịch vụ một cách rõ ràng và minh bạch\n");
    prompt.append("6. Kết thúc bằng lời kêu gọi hành động (call-to-action) khuyến khích liên hệ\n");
    prompt.append("7. Sử dụng ngôn ngữ tích cực, mô tả sinh động nhưng trung thực\n");
    prompt.append("8. Tổ chức thông tin thành các đoạn văn ngắn gọn, dễ đọc\n\n");

    // Output format
    prompt.append("ĐỊNH DẠNG ĐẦU RA: Chỉ trả về mô tả hoàn chỉnh, không kèm giải thích, tiêu đề hay chú thích.");

    return prompt.toString();
  }
}
