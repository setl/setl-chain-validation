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
package io.setl.bc.json.validation;

import java.util.List;
import javax.json.JsonValue;

import io.setl.bc.json.data.ValidationFailure;

/**
 * @author Simon Greatrix on 14/02/2020.
 */
public interface Validator {

  /**
   * Initialise the validator with the provided configuration.
   *
   * @param configuration the configuration
   */
  void initialise(JsonValue configuration);

  /**
   * Validate the provided value.
   *
   * @param value the value to validate
   *
   * @return a list of problems found. An empty list indicates that there are no problems.
   */
  List<ValidationFailure> validate(JsonValue value);

}
