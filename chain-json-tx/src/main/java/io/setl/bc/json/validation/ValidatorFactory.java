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

import static io.setl.common.StringUtils.logSafe;

import java.util.function.Supplier;

import io.setl.util.CopyOnWriteMap;

/**
 * @author Simon Greatrix on 14/02/2020.
 */
public class ValidatorFactory {

  /** Constant used to identify validation using a JSON schema. */
  public static final String JSON_SCHEMA = "json-schema";

  /**
   * Validator suppliers.
   */
  private static final CopyOnWriteMap<String, Supplier<Validator>> SUPPLIERS = new CopyOnWriteMap<>();


  /**
   * Add a named supplier of validators.
   *
   * @param name     the name
   * @param supplier the supplier
   */
  public static void addSupplier(String name, Supplier<Validator> supplier) {
    SUPPLIERS.put(name, supplier);
  }


  /**
   * Get an instance of a named validator type. The instance will have to be initialised before use.
   *
   * @param name the validator type
   *
   * @return the uninitialised validator
   */
  public static Validator getValidator(String name) {
    Supplier<Validator> supplier = SUPPLIERS.get(name);
    if (supplier != null) {
      return supplier.get();
    }
    throw new IllegalArgumentException("Unknown validation \"" + logSafe(name) + "\"");
  }



  static {
    // Auto-register JSON schema validator
    SUPPLIERS.put(JSON_SCHEMA, () -> new JsonSchemaValidator());
  }

}
