package com.smartrent.utility;

public class MaskingUtil {
  public static String maskFromIndex(String text, int index) {
    if (text == null) {
      return "";
    }

    if (index < 0 || index >= text.length()) {
      return text;
    }

    StringBuilder maskedText = new StringBuilder();

    for (int i = 0; i < index; i++) {
      maskedText.append(text.charAt(i));
    }

    for (int i = index; i < text.length(); i++) {
      maskedText.append('*');
    }

    return maskedText.toString();
  }

  public static String maskEmail(String email) {
    if (email == null) {
      return "";
    }

    int atIndex = email.indexOf('@');

    int maskingIndex = atIndex/2;

    return maskFromIndex(email, maskingIndex);
  }
}
