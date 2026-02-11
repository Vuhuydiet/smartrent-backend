package com.smartrent.utility;

import java.security.SecureRandom;

public class Utils {

  public static String generateOTP(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("OTP length must be greater than 0");
    }

    SecureRandom random = new SecureRandom();
    StringBuilder otp = new StringBuilder();

    for (int i = 0; i < length; i++) {
      otp.append(random.nextInt(10));
    }

    return otp.toString();
  }

  public static String buildName(String firstName, String lastName) {
    return String.format("%s %s", firstName, lastName);
  }

}
