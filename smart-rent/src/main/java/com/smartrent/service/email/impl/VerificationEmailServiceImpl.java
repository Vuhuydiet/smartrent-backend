package com.smartrent.service.email.impl;

import static com.smartrent.utility.Utils.buildName;

import com.smartrent.config.Constants;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.repository.entity.VerifyCode;
import com.smartrent.service.email.EmailService;
import com.smartrent.service.email.VerificationEmailService;
import com.smartrent.utility.EmailBuilder;
import com.smartrent.utility.Utils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service(Constants.VERIFICATION_EMAIL_SERVICE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class VerificationEmailServiceImpl implements VerificationEmailService {

  @NonFinal
  @Value("${application.email.sender.email}")
  String senderEmail;

  @NonFinal
  @Value("${application.email.sender.name}")
  String senderName;

  @NonFinal
  @Value("${application.email.subject}")
  String subject;

  @NonFinal
  @Value("${application.otp.length}")
  int otpLength;

  @NonFinal
  @Value("${application.otp.duration}")
  int otpDuration;

  EmailService emailService;

  UserRepository userRepository;


  @Override
  public VerifyCode sendCode(String id) {

    User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    VerifyCode verifyCode = buildVerifyCode(user);
    String htmlContent = EmailBuilder.buildVerifyHtmlContent(senderName, user.getFirstName(), user.getLastName(), verifyCode, otpDuration);
    EmailRequest emailRequest = buildEmailRequest(user, htmlContent);

    // send email
    emailService.sendEmail(emailRequest);

    return verifyCode;
  }



  private EmailRequest buildEmailRequest(User user, String htmlContent) {

    return  EmailRequest.builder()
        .sender(EmailInfo.builder().email(senderEmail).name(senderName).build())
        .to(List.of(EmailInfo.builder().name(buildName(user.getFirstName(), user.getLastName())).email(user.getEmail()).build()))
        .subject(subject)
        .htmlContent(htmlContent)
        .build();
  }
  
  private VerifyCode buildVerifyCode(User user) {
    return VerifyCode.builder()
        .verifyCode(Utils.generateOTP(otpLength))
        .user(user)
        .expirationTime(LocalDateTime.now().plusSeconds(otpDuration))
        .build();
  }
  
}
