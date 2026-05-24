package com.smartrent.utility;

import com.smartrent.config.Constants;
import com.smartrent.service.authentication.domain.OtpData;

import java.time.Year;

public class EmailBuilder {

  static int currentYear() {
    return Year.now().getValue();
  }
  
  static StringBuilder buildHtmlHeader(String title) {
    StringBuilder htmlContent = new StringBuilder();

    htmlContent.append("<!DOCTYPE html>");
    htmlContent.append("<html lang=\"vi\">");
    htmlContent.append("<head>");
    htmlContent.append("<meta charset=\"UTF-8\">");
    htmlContent.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
    htmlContent.append("<title>%s</title>".formatted(title));
    htmlContent.append("<style>");
    htmlContent.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f7fa; color: #1f2937; }");
    htmlContent.append(".container { max-width: 600px; margin: 32px auto; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; }");
    htmlContent.append(".header { background-color: #007BFF; color: #ffffff; padding: 28px 32px; text-align: left; border-bottom: 1px solid #0063CC; }");
    htmlContent.append(".header h1 { margin: 0; font-size: 22px; font-weight: 600; letter-spacing: -0.2px; }");
    htmlContent.append(".content { padding: 32px; }");
    htmlContent.append(".greeting { font-size: 17px; color: #111827; margin-bottom: 16px; font-weight: 600; }");
    htmlContent.append(".message { font-size: 15px; color: #374151; line-height: 1.65; margin-bottom: 24px; }");
    htmlContent.append(".card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 18px; margin: 20px 0; background-color: #ffffff; }");
    htmlContent.append(".card-title { font-size: 13px; font-weight: 600; color: #6b7280; text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 6px; }");
    htmlContent.append(".card-body { font-size: 15px; color: #1f2937; line-height: 1.6; }");
    htmlContent.append(".card-accent { border-left: 3px solid #007BFF; }");
    htmlContent.append(".card-warning { border-color: #fde68a; background-color: #fffbeb; }");
    htmlContent.append(".card-warning .card-title { color: #92400e; }");
    htmlContent.append(".card-warning .card-body { color: #78350f; }");
    htmlContent.append(".card-info { border-color: #cce5ff; background-color: #f0f7ff; }");
    htmlContent.append(".card-info .card-title { color: #004A99; }");
    htmlContent.append(".card-info .card-body { color: #003166; }");
    htmlContent.append(".card-success { border-color: #bbf7d0; background-color: #f0fdf4; }");
    htmlContent.append(".card-success .card-title { color: #166534; }");
    htmlContent.append(".card-success .card-body { color: #14532d; }");
    htmlContent.append(".card-danger { border-color: #fecaca; background-color: #fef2f2; }");
    htmlContent.append(".card-danger .card-title { color: #991b1b; }");
    htmlContent.append(".card-danger .card-body { color: #7f1d1d; }");
    htmlContent.append(".otp-container { text-align: center; margin: 24px 0; }");
    htmlContent.append(".otp-code { display: inline-block; background-color: #f0f7ff; color: #007BFF; border: 1px solid #cce5ff; font-size: 30px; font-weight: 700; padding: 16px 28px; border-radius: 10px; letter-spacing: 10px; font-family: 'Courier New', Consolas, monospace; }");
    htmlContent.append(".footer { background-color: #f9fafb; border-top: 1px solid #e5e7eb; padding: 20px 32px; text-align: center; color: #6b7280; font-size: 13px; line-height: 1.6; }");
    htmlContent.append(".cta-container { text-align: center; margin: 28px 0 8px; }");
    htmlContent.append(".cta-button { display: inline-block; background-color: #007BFF; color: #ffffff !important; font-size: 15px; font-weight: 600; padding: 12px 28px; border-radius: 8px; text-decoration: none; border: 1px solid #0063CC; }");
    htmlContent.append(".cta-fallback { font-size: 12px; color: #6b7280; text-align: center; margin-top: 14px; word-break: break-all; }");
    htmlContent.append(".cta-fallback a { color: #007BFF; text-decoration: none; }");
    htmlContent.append(".divider { height: 1px; background-color: #e5e7eb; margin: 24px 0; border: 0; }");
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
    htmlContent.append("<h1>Chào mừng đến với %s</h1>".formatted(senderName));
    htmlContent.append("</div>");

    // Content section
    htmlContent.append("<div class=\"content\">");
    htmlContent.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(Utils.buildName(firstName, lastName)));

    htmlContent.append("<div class=\"message\">");
    htmlContent.append("Để hoàn tất xác minh tài khoản, vui lòng sử dụng mã xác minh bên dưới.");
    htmlContent.append("</div>");

    // OTP section
    htmlContent.append("<div class=\"otp-container\">");
    htmlContent.append("<div class=\"otp-code\">%s</div>".formatted(otpData.getOtpCode()));
    htmlContent.append("</div>");

    // Expiry information
    htmlContent.append("<div class=\"card card-warning\">");
    htmlContent.append("<div class=\"card-title\">Thời gian hiệu lực</div>");
    htmlContent.append("<div class=\"card-body\">Mã xác minh này sẽ hết hạn sau <strong>%d giây</strong>.</div>".formatted(otpDuration));
    htmlContent.append("</div>");

    // Security note
    htmlContent.append("<div class=\"card card-info\">");
    htmlContent.append("<div class=\"card-title\">Lưu ý bảo mật</div>");
    htmlContent.append("<div class=\"card-body\">Không chia sẻ mã này với bất kỳ ai. Đội ngũ của chúng tôi sẽ không bao giờ yêu cầu mã xác minh của bạn.</div>");
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End content

    // Footer
    htmlContent.append("<div class=\"footer\">");
    htmlContent.append("Nếu bạn không yêu cầu xác minh này, vui lòng bỏ qua email này.<br>");
    htmlContent.append("&copy; %d %s. Bản quyền được bảo lưu.".formatted(currentYear(), senderName));
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End container
    htmlContent.append("</body></html>");

    return htmlContent.toString();
  }

  public static String buildExpiringListingHtmlContent(String senderName, String firstName,
                                                      String lastName, int count, long daysToSoonest,
                                                      String manageUrl) {
    StringBuilder htmlContent = buildHtmlHeader(Constants.EMAIL_EXPIRING_LISTING_HEADER);

    htmlContent.append("<div class=\"container\">");

    // Header
    htmlContent.append("<div class=\"header\">");
    htmlContent.append("<h1>Tin đăng sắp hết hạn</h1>");
    htmlContent.append("</div>");

    // Content
    htmlContent.append("<div class=\"content\">");
    htmlContent.append("<div class=\"greeting\">Xin chào %s,</div>"
        .formatted(Utils.buildName(firstName, lastName)));

    htmlContent.append("<div class=\"message\">");
    htmlContent.append("Bạn đang có <strong>%d</strong> tin đăng sắp hết hạn trên %s."
        .formatted(count, senderName));
    htmlContent.append("</div>");

    // Urgency info
    boolean urgent = daysToSoonest <= 0;
    String urgencyClass = urgent ? "card card-danger" : "card card-warning";
    String urgencyTitle = urgent ? "Khẩn" : "Thời hạn";
    String urgencyBody = urgent
        ? "Tin sớm nhất sẽ hết hạn trong vòng <strong>24 giờ tới</strong>."
        : "Tin sớm nhất sẽ hết hạn sau <strong>%d ngày</strong>.".formatted(daysToSoonest);

    htmlContent.append("<div class=\"%s\">".formatted(urgencyClass));
    htmlContent.append("<div class=\"card-title\">%s</div>".formatted(urgencyTitle));
    htmlContent.append("<div class=\"card-body\">%s</div>".formatted(urgencyBody));
    htmlContent.append("</div>");

    // CTA button
    htmlContent.append("<div class=\"cta-container\">");
    htmlContent.append("<a href=\"%s\" class=\"cta-button\">Quản lý tin đăng của tôi</a>"
        .formatted(manageUrl));
    htmlContent.append("<div class=\"cta-fallback\">Hoặc mở liên kết: "
        + "<a href=\"%s\">%s</a></div>".formatted(manageUrl, manageUrl));
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End content

    // Footer
    htmlContent.append("<div class=\"footer\">");
    htmlContent.append("Email này được gửi tự động &mdash; bạn không cần phản hồi.<br>");
    htmlContent.append("&copy; %d %s. Bản quyền được bảo lưu.".formatted(currentYear(), senderName));
    htmlContent.append("</div>");

    htmlContent.append("</div>"); // End container
    htmlContent.append("</body></html>");

    return htmlContent.toString();
  }

}
