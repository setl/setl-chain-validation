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
package io.setl.validation.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

import io.setl.common.AddressType;
import io.setl.validation.AddressValidator;
import io.setl.validation.Base64Validator;

/**
 * Validate that a field is valid base 64.
 *
 * <p>Note the Java Base64 MIME decoder is incredibly lenient as invalid characters are simply ignored. The validator only allows white-space.</p>
 *
 * @author Simon Greatrix on 14/01/2021.
 */
@Documented
@Constraint(validatedBy = Base64Validator.class)
@Target({METHOD, FIELD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBase64 {

  enum Flavour {
    /** Basic MIME-compatible Base64 with no additional characters. */
    BASIC,

    /** MIME alphabet with white-space. */
    MIME,

    /** MIME alphabet with bad characters ignored. */
    LENIENT_MIME,

    /** URL and Filename safe Base 64. */
    URL
  }

  Class<?>[] groups() default {};

  String message() default "{io.setl.validation.annotations.Base64.message}";

  Class<? extends Payload>[] payload() default {};

  /** If specified, the base 64 must be one of these types. */
  Flavour flavour() default Flavour.BASIC;

}
