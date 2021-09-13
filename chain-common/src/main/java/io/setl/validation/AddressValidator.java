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

import java.util.Arrays;
import java.util.EnumSet;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.validation.annotations.Address;

/**
 * @author Simon Greatrix on 07/09/2020.
 */
public class AddressValidator implements ConstraintValidator<Address, String> {

  private EnumSet<AddressType> validTypes;


  @Override
  public void initialize(Address annotation) {
    AddressType[] types = annotation.type();
    if (types.length > 0) {
      validTypes = EnumSet.noneOf(AddressType.class);
      validTypes.addAll(Arrays.asList(annotation.type()));
      if (validTypes.contains(AddressType.NORMAL)) {
        validTypes.add(AddressType.PRIVILEGED);
      }
    } else {
      validTypes = EnumSet.of(AddressType.NORMAL, AddressType.PRIVILEGED);
    }
  }


  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (!AddressUtil.verifyAddress(value)) {
      return false;
    }
    AddressType type = AddressUtil.getAddressType(value);
    return validTypes.contains(type);
  }

}
