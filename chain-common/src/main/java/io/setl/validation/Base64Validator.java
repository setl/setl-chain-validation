/* <notice>
 
    SETL Blockchain
    Copyright (C) 2021 SETL Ltd
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License, version 3, as
    published by the Free Software Foundation.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
 
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 
</notice> */
package io.setl.validation;

import static io.setl.validation.annotations.ValidBase64.Flavour;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.setl.validation.annotations.ValidBase64;

/**
 * @author Simon Greatrix on 14/01/2021.
 */
public class Base64Validator implements ConstraintValidator<ValidBase64, String> {

  /**
   * Basic Base-64 uses the MIME alphabet with no extra characters and optional padding.
   *
   * @param value the value to check
   *
   * @return true if valid
   */
  private static boolean checkBasic(String value) {
    int l = trimPadding(value);

    for (int i = 0; i < l; i++) {
      char ch = value.charAt(i);
      if (isNotMimeCharacter(ch)) {
        return false;
      }
    }

    return (l & 3) != 1;
  }


  /**
   * Lenient mime Base-64 uses the MIME alphabet with any extra characters and optional padding.
   *
   * @param value the value to check
   *
   * @return true if valid
   */
  private static boolean checkLenientMime(String value) {
    int padCount = 0;
    int l = value.length();
    char ch;
    do {
      l--;
      ch = value.charAt(l);
      if (ch == '=') {
        padCount++;
        if (padCount == 3) {
          // too many pad characters
          return false;
        }
      }
    }
    while ((l > 0) && (isNotMimeCharacter(ch) || ch == '='));

    int b64Chars = 0;
    while (l >= 0) {
      ch = value.charAt(l);
      l--;

      if (!isNotMimeCharacter(ch)) {
        b64Chars++;
      }
    }

    return (padCount == 0 && (b64Chars & 3) != 1) || (((padCount + b64Chars) & 3) == 0);
  }


  /**
   * Regular MIME Base-64 uses the MIME alphabet with whitespace and optional padding.
   *
   * @param value the value to check
   *
   * @return true if valid
   */
  private static boolean checkMime(String value) {
    int padCount = 0;
    int l = value.length();
    char ch;
    do {
      l--;
      ch = value.charAt(l);
      if (ch == '=') {
        padCount++;
        if (padCount == 3) {
          // too many pad characters
          return false;
        }
      }
    }
    while ((l > 0) && (Character.isWhitespace(ch) || ch == '='));

    int b64Chars = 0;
    while (l >= 0) {
      ch = value.charAt(l);
      l--;

      // skip whitespace
      if (Character.isWhitespace(ch)) {
        continue;
      }

      if (isNotMimeCharacter(ch)) {
        // OK if it is whitespace
        return false;
      }

      b64Chars++;
    }

    return (padCount == 0 && (b64Chars & 3) != 1) || (((padCount + b64Chars) & 3) == 0);
  }


  /**
   * URL Base-64 replaces the '/' and '+' with '-' and '_' to make the data URL and filename friendly.
   *
   * @param value the value to check
   *
   * @return true if valid
   */
  private static boolean checkUrl(String value) {
    int l = trimPadding(value);

    for (int i = 0; i < l; i++) {
      char ch = value.charAt(i);
      if (isNotUrlCharacter(ch)) {
        return false;
      }
    }

    return (l & 3) != 1;
  }


  private static boolean isNotAlphaNumeric(char ch) {
    return (ch < '0') || ('9' < ch && ch < 'A') || ('Z' < ch && ch < 'a') || ('z' < ch);
  }


  private static boolean isNotMimeCharacter(char ch) {
    return isNotAlphaNumeric(ch) && (ch != '+') && (ch != '/');
  }


  private static boolean isNotUrlCharacter(char ch) {
    return isNotAlphaNumeric(ch) && (ch != '-') && (ch != '_');
  }


  private static int trimPadding(String value) {
    int l = value.length();

    // padding is only valid if we have an exact multiple of 4 characters
    if ((l & 3) == 0) {
      // one padding character?
      if (value.charAt(l - 1) == '=') {
        l--;
      }
      // two padding characters?
      if (value.charAt(l - 1) == '=') {
        l--;
      }
    }

    return l;
  }


  Flavour flavour;


  @Override
  public void initialize(ValidBase64 annotation) {
    flavour = annotation.flavour();
  }


  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }

    switch (flavour) {
      case MIME:
        return checkMime(value);
      case LENIENT_MIME:
        return checkLenientMime(value);
      case URL:
        return checkUrl(value);
      default:
        return checkBasic(value);
    }
  }

}