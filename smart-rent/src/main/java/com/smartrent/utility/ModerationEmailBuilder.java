package com.smartrent.utility;

/**
 * Builds HTML email content for moderation-related notifications.
 * All methods follow the same styling as {@link EmailBuilder}.
 */
public class ModerationEmailBuilder {

    private ModerationEmailBuilder() {}

    public static String buildApprovedEmail(String listingTitle, String ownerFirstName) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Listing Approved");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Listing Approved ✅</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Hello %s! 👋</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Great news! Your listing <strong>%s</strong> has been approved and is now visible to the public.".formatted(safe(listingTitle)));
        html.append("</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. All rights reserved.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildRejectedEmail(String listingTitle, String ownerFirstName, String reason) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Listing Rejected");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Listing Rejected ❌</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Hello %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Unfortunately, your listing <strong>%s</strong> has been rejected.".formatted(safe(listingTitle)));
        html.append("</div>");
        if (reason != null && !reason.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Reason:</strong> %s".formatted(safe(reason)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Please update the listing and resubmit it for review.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. All rights reserved.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildRevisionRequestedEmail(String listingTitle, String ownerFirstName, String reason) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Revision Required");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Revision Required 📝</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Hello %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("Your listing <strong>%s</strong> needs some updates before it can be approved.".formatted(safe(listingTitle)));
        html.append("</div>");
        if (reason != null && !reason.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>What to update:</strong> %s".formatted(safe(reason)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Please edit the listing and click \"Resubmit for Review\" when you're done.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. All rights reserved.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildReportActionRequiredEmail(String listingTitle, String ownerFirstName, String adminNotes) {
        StringBuilder html = EmailBuilder.buildHtmlHeader("Action Required");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>Action Required ⚠️</h1></div>");
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Hello %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("A report on your listing <strong>%s</strong> has been reviewed by our admin team and requires your action.".formatted(safe(listingTitle)));
        html.append("</div>");
        if (adminNotes != null && !adminNotes.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Admin notes:</strong> %s".formatted(safe(adminNotes)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">Please update the listing and resubmit it for review.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. All rights reserved.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildReportResolvedForReporterEmail(String listingTitle, String reporterName,
                                                               String resolution, String adminNotes) {
        boolean resolved = "RESOLVED".equalsIgnoreCase(resolution);
        String headerText = resolved ? "Report Resolved ✅" : "Report Reviewed ℹ️";
        String outcomeText = resolved
                ? "we have taken appropriate action regarding this listing."
                : "after careful review, no further action is required at this time.";

        StringBuilder html = EmailBuilder.buildHtmlHeader(headerText);
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>%s</h1></div>".formatted(headerText));
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Hello %s,</div>".formatted(safe(reporterName)));
        html.append("<div class=\"message\">");
        html.append("Thank you for reporting the listing <strong>%s</strong>. ".formatted(safe(listingTitle)));
        html.append("We have reviewed your report and %s".formatted(outcomeText));
        html.append("</div>");
        if (adminNotes != null && !adminNotes.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Admin notes:</strong> %s".formatted(safe(adminNotes)));
            html.append("</div>");
        }
        html.append("<div class=\"message\">We appreciate your help in keeping SmartRent a safe and accurate platform.</div>");
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. All rights reserved.</div>");
        html.append("</div></body></html>");
        return html.toString();
    }

    public static String buildReportResolvedForOwnerEmail(String listingTitle, String ownerFirstName,
                                                            String resolution, String adminNotes) {
        boolean resolved = "RESOLVED".equalsIgnoreCase(resolution);
        String headerText = resolved ? "Report on Your Listing — Action Taken ⚠️" : "Report on Your Listing — No Issues Found ✅";
        String outcomeText = resolved
                ? "Our team has reviewed the report and taken appropriate action. Please review your listing and make any necessary updates."
                : "Our team has reviewed the report and found no issues. Your listing remains active and no action is required on your part.";

        StringBuilder html = EmailBuilder.buildHtmlHeader(headerText);
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\"><h1>%s</h1></div>".formatted(headerText));
        html.append("<div class=\"content\">");
        html.append("<div class=\"greeting\">Hello %s,</div>".formatted(safe(ownerFirstName)));
        html.append("<div class=\"message\">");
        html.append("A report was submitted regarding your listing <strong>%s</strong>. ".formatted(safe(listingTitle)));
        html.append(outcomeText);
        html.append("</div>");
        if (adminNotes != null && !adminNotes.isBlank()) {
            html.append("<div class=\"expiry-info\">");
            html.append("<strong>Admin notes:</strong> %s".formatted(safe(adminNotes)));
            html.append("</div>");
        }
        html.append("</div>");
        html.append("<div class=\"footer\">© 2024 SmartRent. All rights reserved.</div>");
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
