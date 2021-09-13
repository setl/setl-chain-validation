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
package io.setl.bc.json.data.acl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon Greatrix on 21/01/2020.
 */
@Schema(description = "A set of permitted operations")
public class CrudSet implements Iterable<CRUD> {

  /** The 16 singletons for the CRUD lists. */
  private static final CrudSet[] INSTANCES = new CrudSet[16];


  public static CrudSet empty() {
    return INSTANCES[0];
  }


  /**
   * Get the crud list with a specified single entry.
   *
   * @param crud the entry
   *
   * @return the list
   */
  public static CrudSet forCrud(CRUD crud) {
    if (crud == null) {
      return INSTANCES[0];
    }
    return INSTANCES[crud.getBitMask()];
  }


  /**
   * Get the CRUD list for a specific mask.
   *
   * @param mask the mask
   *
   * @return the list
   */
  public static CrudSet forMask(int mask) {
    return INSTANCES[mask];
  }


  /**
   * Get the CRUD list corresponding to the operations. Operations may be any case-insensitive prefix of the standard operation names.
   *
   * @param operations the relevant labels
   *
   * @return the corresponding list
   */
  @JsonCreator
  public static CrudSet forOperations(Iterable<String> operations) {
    int mask = 0;
    CRUD[] values = CRUD.values();
    for (String e : operations) {
      int l = e.length();
      for (CRUD c : values) {
        if (c.getLabel().regionMatches(true, 0, e, 0, l)) {
          mask |= c.getBitMask();
        }
      }
    }
    return forMask(mask);
  }


  static {
    for (int i = 0; i < 16; i++) {
      INSTANCES[i] = new CrudSet(i);
    }
  }

  private final String description;

  private final int mask;

  private final List<String> operations;

  private final CRUD[] values;


  private CrudSet(int mask) {
    this.mask = mask;
    int count = Integer.bitCount(mask);
    values = new CRUD[count];
    String[] ops = new String[count];
    int a = 0;
    if (mask == 0) {
      description = "";
      operations = Collections.emptyList();
    } else {
      StringBuilder buf = new StringBuilder();
      for (CRUD crud : CRUD.values()) {
        if (contains(crud)) {
          String l = crud.getLabel();
          buf.append(l).append(',');
          ops[a] = l;
          values[a++] = crud;
        }
      }
      buf.setLength(buf.length() - 1);
      description = buf.toString();
      operations = List.of(ops);
    }
  }


  /**
   * Get the set of CRUD operations specified by adding the given operation to this set of operations.
   *
   * @param crud the new operation
   *
   * @return the set containing all of this set's operations and the new operation
   */
  public CrudSet add(CRUD crud) {
    if (crud == null) {
      return this;
    }
    return INSTANCES[mask | crud.getBitMask()];
  }


  /**
   * Get the set of CRUD operations which is the union of this set and another set of operations.
   *
   * @param other the other set of operations
   *
   * @return the union of the operations
   */
  public CrudSet combine(CrudSet other) {
    if (other == null) {
      return this;
    }
    return INSTANCES[mask | other.mask];
  }


  public final boolean contains(CRUD crud) {
    return (mask & crud.getBitMask()) != 0;
  }


  /**
   * Perform an action for each operation specified in this set.
   *
   * @param consumer the action to perform
   */
  @Override
  public void forEach(Consumer<? super CRUD> consumer) {
    for (CRUD crud : values) {
      consumer.accept(crud);
    }
  }


  @JsonIgnore
  @Hidden
  public String getDescription() {
    return description;
  }


  @JsonIgnore
  @Hidden
  public int getMask() {
    return mask;
  }


  @Schema(description = "The operations allowed: 'create', 'read', 'update', or 'delete'.")
  @JsonValue
  public List<String> getOperations() {
    return operations;
  }


  @NotNull
  @Override
  public Iterator<CRUD> iterator() {
    return Arrays.asList(values).iterator();
  }


  @Override
  public String toString() {
    return String.format("CrudList(%s)", getDescription());
  }

}
