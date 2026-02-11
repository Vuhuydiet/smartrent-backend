package com.smartrent.utility;

import com.smartrent.config.Constants;
import com.smartrent.service.authentication.domain.OtpData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmailBuilder {
  
  static StringBuilder buildHtmlHeader(String title) {
    StringBuilder htmlContent = new StringBuilder();
    
    htmlContent.append("<!DOCTYPE html>");
    htmlContent.append("<html lang=\"en\">");
    htmlContent.append("<head>");
    htmlContent.append("<meta charset=\"UTF-8\">");
    htmlContent.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
    htmlContent.append("<title>%s</title>".formatted(title));
    htmlContent.append("<style>");
    htmlContent.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
    htmlContent.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden; }");
    htmlContent.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 20px; text-align: center; }");
    htmlContent.append(".header h1 { margin: 0; font-size: 28px; font-weight: 300; }");
    htmlContent.append(".content { padding: 40px 30px; }");
    htmlContent.append(".greeting { font-size: 18px; color: #333; margin-bottom: 20px; }");
    htmlContent.append(".message { font-size: 16px; color: #555; line-height: 1.6; margin-bottom: 30px; }");
    htmlContent.append(".otp-container { text-align: center; margin: 30px 0; }");
    htmlContent.append(".otp-code { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; font-size: 32px; font-weight: bold; padding: 15px 30px; border-radius: 8px; letter-spacing: 8px; margin: 10px 0; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3); }");
    htmlContent.append(".expiry-info { background-color: #fff3cd; border: 1px solid #ffeaa7; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; text-align: center; }");
    htmlContent.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 14px; }");
    htmlContent.append(".security-note { background-color: #d1ecf1; border: 1px solid #bee5eb; color: #0c5460; padding: 15px; border-radius: 5px; margin: 20px 0; }");
    htmlContent.append("</style>");
    htmlContent.append("</head>");
    htmlContent.append("<body>");
  
    return htmlContent;
  }

  public static String buildVerifyHtmlContent(String senderName, String firstName, String lastName, OtpData otpData, int otpDuration) {
    // HTML header with styles
    StringBuilder htmlContent = buildHtmlHeader(Constants.EMAIL_VERIFICATION_HEADER);

    // Main container
    htmlContent.append("<div class=\"container\">");

    // Header section
    htmlContent.append("<div class=\"header\">");
    htmlContent.append("<h1>Welcome to %s</h1>".formatted(senderName));
    htmlContent.append("</div>");

    // Content section
    htmlContent.append("<div class=\"content\">");
    htmlContent.append("<div class=\"greeting\">Hello %s! 👋</div>".formatted(Utils.buildName(firstName, lastName)));

    htmlContent.append("<div class=\"message\">");
    htmlContent.append("To complete your account verification, please use the verification code below:");
    htmlContent.append("</div>");

    // OTP section
    htmlContent.append("<div class=\"otp-container\">");
    htmlContent.append("<div class=\"otp-code\">%s</div>".formatted(otpData.getOtpCode()));
    htmlContent.append("</div>");

    // Expiry information
    htmlContent.append("<div class=\"expiry-info\">");
    htmlContent.append("⏰ <strong>Important:</strong> This verification code will expire in %d seconds.".formatted(otpDuration));
    htmlContent.append("</div>");

    // Security note
    htmlContent.append("<div class=\"security-note\">");
    htmlContent.append("🔒 <strong>Security Note:</strong> Never share this code with anyone. Our team will never ask for your verification code.");
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End content

    // Footer
    htmlContent.append("<div class=\"footer\">");
    htmlContent.append("If you didn't request this verification, please ignore this email.<br>");
    htmlContent.append("© 2024 %s. All rights reserved.".formatted(senderName));
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End container
    htmlContent.append("</body></html>");

    return htmlContent.toString();
  }

  /**
   * Build HTML content for listing disabled notification email
   *
   * @param senderName    Name of the sender (e.g., "SmartRent")
   * @param ownerName     Name of the listing owner
   * @param listingTitle  Title of the disabled listing
   * @param listingId     ID of the disabled listing
   * @param reasons       List of report reason descriptions
   * @param adminNotes    Admin notes on the resolution
   * @param disabledAt    Timestamp when the listing was disabled
   * @return HTML content string
   */
  public static String buildListingDisabledHtmlContent(
          String senderName,
          String ownerName,
          String listingTitle,
          Long listingId,
          List<String> reasons,
          String adminNotes,
          LocalDateTime disabledAt) {

    StringBuilder htmlContent = buildHtmlHeader(Constants.LISTING_DISABLED_EMAIL_HEADER);

    // Main container
    htmlContent.append("<div class=\"container\">");

    // Header section
    htmlContent.append("<div class=\"header\">");
    htmlContent.append("<h1>%s - Thông Báo</h1>".formatted(senderName));
    htmlContent.append("</div>");

    // Content section
    htmlContent.append("<div class=\"content\">");
    htmlContent.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(ownerName));

    htmlContent.append("<div class=\"message\">");
    htmlContent.append("Chúng tôi xin thông báo rằng tin đăng của bạn đã bị vô hiệu hóa sau khi xem xét báo cáo vi phạm.");
    htmlContent.append("</div>");

    // Listing details
    htmlContent.append("<div class=\"expiry-info\" style=\"background-color: #f8d7da; border-color: #f5c6cb; color: #721c24;\">");
    htmlContent.append("<strong>📋 Chi tiết tin đăng:</strong><br>");
    htmlContent.append("• Tiêu đề: <strong>%s</strong><br>".formatted(listingTitle));
    htmlContent.append("• Mã tin: <strong>#%d</strong><br>".formatted(listingId));

    String formattedDate = disabledAt != null
            ? disabledAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            : "N/A";
    htmlContent.append("• Thời gian: <strong>%s</strong>".formatted(formattedDate));
    htmlContent.append("</div>");

    // Reasons
    if (reasons != null && !reasons.isEmpty()) {
      htmlContent.append("<div class=\"security-note\" style=\"background-color: #fff3cd; border-color: #ffeaa7; color: #856404;\">");
      htmlContent.append("<strong>📌 Lý do:</strong><br>");
      for (String reason : reasons) {
        htmlContent.append("• %s<br>".formatted(reason));
      }
      htmlContent.append("</div>");
    }

    // Admin notes
    if (adminNotes != null && !adminNotes.isEmpty()) {
      htmlContent.append("<div class=\"security-note\">");
      htmlContent.append("<strong>📝 Ghi chú từ quản trị viên:</strong><br>");
      htmlContent.append("%s".formatted(adminNotes));
      htmlContent.append("</div>");
    }

    // Appeal info
    htmlContent.append("<div class=\"message\">");
    htmlContent.append("Nếu bạn cho rằng đây là một sai sót, vui lòng liên hệ với chúng tôi qua email để được hỗ trợ.");
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End content

    // Footer
    htmlContent.append("<div class=\"footer\">");
    htmlContent.append("Đây là email tự động, vui lòng không trả lời trực tiếp.<br>");
    htmlContent.append("© 2024 %s. All rights reserved.".formatted(senderName));
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End container
    htmlContent.append("</body></html>");

    return htmlContent.toString();
  }

}
