package com.smartrent.utility;

/**
 * Builds HTML email content for moderation-related notifications.
 * All methods follow the same styling as {@link EmailBuilder}.
 */
public class ModerationEmailBuilder {

    private ModerationEmailBuilder() {}

    public static String buildApprovedEmail(String listingTitle, String ownerFirstName) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Tin đăng đã được duyệt");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Tin đăng đã được duyệt ✅</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Xin chào %s! 👋</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Tin tốt! Tin đăng <strong>%s</strong> của bạn đã được duyệt và hiện đã hiển thị công khai.".formatted(safe(listingTitle)));
        html.append("</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. Bản quyền được bảo lưu.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildRejectedEmail(String listingTitle, String ownerFirstName, String reason) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Tin đăng bị từ chối");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Tin đăng bị từ chối ❌</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Rất tiếc, tin đăng <strong>%s</strong> của bạn đã bị từ chối.".formatted(safe(listingTitle)));
        html.append("</div>");
        if (reason != null && !reason.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Lý do:</strong> %s".formatted(safe(reason)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Vui lòng cập nhật tin đăng và gửi lại để được xem xét.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. Bản quyền được bảo lưu.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildRevisionRequestedEmail(String listingTitle, String ownerFirstName, String reason) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Yêu cầu chỉnh sửa");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Yêu cầu chỉnh sửa 📝</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Tin đăng <strong>%s</strong> của bạn cần được cập nhật trước khi có thể được duyệt.".formatted(safe(listingTitle)));
        html.append("</div>");
        if (reason != null && !reason.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Nội dung cần cập nhật:</strong> %s".formatted(safe(reason)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Vui lòng chỉnh sửa tin đăng và nhấn \"Gửi lại để xem xét\" khi bạn hoàn tất.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. Bản quyền được bảo lưu.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildReportActionRequiredEmail(String listingTitle, String ownerFirstName, String adminNotes) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Yêu cầu hành động");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Yêu cầu hành động ⚠️</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Một báo cáo về tin đăng <strong>%s</strong> của bạn đã được đội ngũ quản trị xem xét và yêu cầu bạn thực hiện hành động.".formatted(safe(listingTitle)));
        html.append("</div>");
        if (adminNotes != null && !adminNotes.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Ghi chú từ quản trị viên:</strong> %s".formatted(safe(adminNotes)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Vui lòng cập nhật tin đăng và gửi lại để được xem xét.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. Bản quyền được bảo lưu.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildReportResolvedForReporterEmail(String listingTitle, String reporterName,
                                                               String resolution, String adminNotes) {
        boolean resolved = "RESOLVED".equalsIgnoreCase(resolution);
        String headerText = resolved ? "Báo cáo đã được xử lý ✅" : "Báo cáo đã được xem xét ℹ️";
        String outcomeText = resolved
                ? "chúng tôi đã thực hiện các biện pháp phù hợp đối với tin đăng này."
                : "sau khi xem xét kỹ lưỡng, hiện tại không cần thực hiện thêm hành động nào.";

        StringBuilder html = EmailBuilder.buildHtmlHeader(headerText);
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>%s</h1></div>".formatted(headerText));
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(safe(reporterName)));
        html.append("<div class=\"message\">");
        html.append("Cảm ơn bạn đã báo cáo tin đăng <strong>%s</strong>. ".formatted(safe(listingTitle)));
        html.append("Chúng tôi đã xem xét báo cáo của bạn và %s".formatted(outcomeText));
        html.append("</div>");
        if (adminNotes != null && !adminNotes.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Ghi chú từ quản trị viên:</strong> %s".formatted(safe(adminNotes)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Chúng tôi đánh giá cao sự giúp đỡ của bạn trong việc giữ SmartRent trở thành nền tảng an toàn và chính xác.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. Bản quyền được bảo lưu.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildReportResolvedForOwnerEmail(String listingTitle, String ownerFirstName,
                                                            String resolution, String adminNotes) {
        boolean resolved = "RESOLVED".equalsIgnoreCase(resolution);
        String headerText = resolved ? "Báo cáo về tin đăng của bạn — Đã xử lý ⚠️" : "Báo cáo về tin đăng của bạn — Không phát hiện vấn đề ✅";
        String outcomeText = resolved
                ? "Đội ngũ của chúng tôi đã xem xét báo cáo và thực hiện các biện pháp phù hợp. Vui lòng kiểm tra tin đăng và cập nhật nếu cần thiết."
                : "Đội ngũ của chúng tôi đã xem xét báo cáo và không phát hiện vấn đề nào. Tin đăng của bạn vẫn đang hoạt động và bạn không cần thực hiện thêm hành động nào.";

        StringBuilder html = EmailBuilder.buildHtmlHeader(headerText);
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>%s</h1></div>".formatted(headerText));
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Xin chào %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Một báo cáo đã được gửi liên quan đến tin đăng <strong>%s</strong> của bạn. ".formatted(safe(listingTitle)));
        html.append(outcomeText);
        html.append("</div>");
        if (adminNotes != null && !adminNotes.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Ghi chú từ quản trị viên:</strong> %s".formatted(safe(adminNotes)));
            html.append("</div>");
        }
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. Bản quyền được bảo lưu.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildApprovedAfterResubmitEmail(String listingTitle, String ownerFirstName) {
        return buildApprovedEmail(listingTitle, ownerFirstName); // Same content
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
