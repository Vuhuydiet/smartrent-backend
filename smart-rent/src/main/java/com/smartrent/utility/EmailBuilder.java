package com.smartrent.utility;

import com.smartrent.config.Constants;
import com.smartrent.infra.repository.entity.VerifyCode;

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

  public static String buildVerifyHtmlContent(String senderName, String firstName, String lastName, VerifyCode verifyCode, int otpDuration) {
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
    htmlContent.append("<div class=\"otp-code\">%s</div>".formatted(verifyCode.getVerifyCode()));
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
  
}
