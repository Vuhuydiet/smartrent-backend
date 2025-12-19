package com.smartrent.controller;

import com.smartrent.dto.request.ChatRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ChatResponse;
import com.smartrent.service.ai.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai/chat")
@Tag(name = "AI", description = "AI-powered chat for property listing search")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  @Operation(
      summary = "Trò chuyện với trợ lý AI bất động sản",
      description = "Gửi tin nhắn hội thoại đến trợ lý AI để tìm kiếm và khám phá bất động sản cho thuê. " +
                    "AI hiểu ngôn ngữ tự nhiên (tiếng Việt) và tự động tìm kiếm bất động sản dựa trên yêu cầu của bạn. " +
                    "AI sẽ phân tích kết quả và chọn ra tối đa 5 bất động sản phù hợp nhất để giới thiệu.",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ChatRequest.class),
              examples = {
                  @ExampleObject(
                      name = "Tìm kiếm đơn giản",
                      value = "{ \"messages\": [ { \"role\": \"user\", \"content\": \"Tôi cần tìm căn hộ 2 phòng ngủ ở Hà Nội, giá khoảng 10 triệu/tháng\" } ] }"
                  ),
                  @ExampleObject(
                      name = "Hội thoại nhiều lượt",
                      value = "{ \"messages\": [ { \"role\": \"user\", \"content\": \"Tìm giúp tôi căn hộ ở Quận 1\" }, { \"role\": \"assistant\", \"content\": \"Tôi sẽ giúp bạn tìm căn hộ ở Quận 1. Bạn có thể cho tôi biết thêm về ngân sách và số phòng ngủ mong muốn không?\" }, { \"role\": \"user\", \"content\": \"Cần 1 phòng ngủ, giá từ 6-8 triệu\" } ] }"
                  )
              }
          )
      ),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "200",
              description = "Phản hồi thành công với danh sách bất động sản được AI lựa chọn",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ChatResponse.class),
                  examples = @ExampleObject(
                      name = "Thành công - AI trả về 5 BĐS phù hợp nhất",
                      value = """
                          {
                            "code": "999999",
                            "message": "Operation completed successfully",
                            "data": {
                              "message": {
                                "role": "assistant",
                                "content": "Tôi đã tìm thấy 15 căn hộ 2 phòng ngủ tại Hà Nội và đã chọn ra 5 căn phù hợp nhất với yêu cầu của bạn:\n\n1. **Listing ID: 123** - Căn hộ 2PN view đẹp Q. Đống Đa - 10 triệu/tháng, 78m². Căn này có giá tốt và view đẹp.\n\n2. **Mã tin: 456** - Chung cư cao cấp 2PN Cầu Giấy - 12 triệu/tháng, 85m². Đầy đủ nội thất, gần trường học.\n\n3. **Listing ID: 789** - Căn hộ hiện đại 2PN Ba Đình - 11 triệu/tháng, 80m². Vị trí trung tâm, gần công viên.\n\nTôi đã chọn những căn này vì chúng phù hợp với ngân sách 10 triệu của bạn, có đầy đủ 2 phòng ngủ và ở vị trí thuận tiện."
                              },
                              "metadata": {
                                "model": "gemini-2.0-flash",
                                "tools_used": ["search_listings"],
                                "totalCount": 15,
                                "selectedFromTotal": 5
                              },
                              "listings": {
                                "listings": [
                                  {
                                    "listingId": 123,
                                    "title": "Căn hộ 2PN view đẹp Q. Đống Đa",
                                    "price": 10000000,
                                    "area": 78.5,
                                    "bedrooms": 2,
                                    "bathrooms": 2,
                                    "address": {
                                      "street": "Đường Láng",
                                      "ward": "Láng Thượng",
                                      "district": "Đống Đa",
                                      "province": "Hà Nội"
                                    }
                                  },
                                  {
                                    "listingId": 456,
                                    "title": "Chung cư cao cấp 2PN Cầu Giấy",
                                    "price": 12000000,
                                    "area": 85.0,
                                    "bedrooms": 2,
                                    "bathrooms": 2,
                                    "address": {
                                      "street": "Trần Thái Tông",
                                      "ward": "Dịch Vọng",
                                      "district": "Cầu Giấy",
                                      "province": "Hà Nội"
                                    }
                                  }
                                ],
                                "totalCount": 15,
                                "currentPage": 1,
                                "pageSize": 20
                              }
                            }
                          }
                          """
                  )
              )
          ),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "400",
              description = "Invalid request - messages cannot be empty or last message must be from user"
          ),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "500",
              description = "Internal server error or AI service unavailable"
          )
      }
  )
  public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
    ChatResponse response = chatService.processChat(request);
    return ApiResponse.<ChatResponse>builder().data(response).build();
  }
}
