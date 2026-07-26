package com.smartrent.service.contactreveal.impl;

import com.smartrent.dto.request.RevealContactRequest;
import com.smartrent.dto.response.RevealContactResponse;
import com.smartrent.enums.ContactRevealChannel;
import com.smartrent.infra.repository.ContactRevealLogRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.ContactRevealLog;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.contactreveal.ContactRevealService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ContactRevealServiceImpl implements ContactRevealService {

    ContactRevealLogRepository contactRevealLogRepository;
    UserRepository userRepository;
    ListingRepository listingRepository;

    @Override
    @Transactional
    public RevealContactResponse revealContact(String sellerUserId,
                                               RevealContactRequest request,
                                               String viewerUserId,
                                               String ipAddress,
                                               String userAgent) {
        User seller = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + sellerUserId));

        User viewer = userRepository.findById(viewerUserId)
                .orElseThrow(() -> new RuntimeException("Viewer not found with ID: " + viewerUserId));

        // Listing is optional context — a reveal on the seller profile page has none.
        Listing listing = null;
        if (request.getListingId() != null) {
            listing = listingRepository.findById(request.getListingId()).orElse(null);
        }

        ContactRevealLog logEntry = ContactRevealLog.builder()
                .viewer(viewer)
                .seller(seller)
                .listing(listing)
                .channel(request.getChannel())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        contactRevealLogRepository.save(logEntry);
        log.info("Contact reveal logged: viewer={} seller={} channel={} listing={}",
                viewerUserId, sellerUserId, request.getChannel(), request.getListingId());

        return buildContactResponse(seller, request.getChannel());
    }

    private RevealContactResponse buildContactResponse(User seller, ContactRevealChannel channel) {
        String contactPhone = seller.getContactPhoneNumber();
        if (contactPhone == null || contactPhone.isBlank()) {
            String code = seller.getPhoneCode() == null ? "" : seller.getPhoneCode();
            String number = seller.getPhoneNumber() == null ? "" : seller.getPhoneNumber();
            contactPhone = (code + number).trim();
        }
        boolean hasPhone = contactPhone != null && !contactPhone.isBlank();
        String digits = hasPhone ? contactPhone.replaceAll("\\D", "") : "";
        String zaloLink = digits.isEmpty() ? null : "https://zalo.me/" + digits;

        String contactName = ((seller.getLastName() == null ? "" : seller.getLastName()) + " "
                + (seller.getFirstName() == null ? "" : seller.getFirstName())).trim();

        return RevealContactResponse.builder()
                .contactName(contactName.isEmpty() ? null : contactName)
                .phoneNumber(hasPhone ? contactPhone : null)
                .email(seller.getEmail())
                .zaloLink(zaloLink)
                .channel(channel)
                .build();
    }
}
