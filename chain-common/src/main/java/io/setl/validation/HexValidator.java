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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.setl.validation.annotations.ValidHex;

/**
 * @author Simon Greatrix on 14/01/2021.
 */
public class HexValidator implements ConstraintValidator<ValidHex, String> {

  @Override
  public void initialize(ValidHex annotation) {
    // nothing to do
  }


  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    int l = value.length();
    if ((l & 1) == 1) {
      // Odd number of characters cannot be correct
      return false;
    }
    for (int i = l - 1; i >= 0; i--) {
      char ch = value.charAt(i);
      if ((ch < '0') || ('9' < ch && ch < 'A') || ('F' < ch && ch < 'a') || ('f' < ch)) {
        // Bad hex character
        return false;
      }
    }
    // it is valid
    return true;
  }

}