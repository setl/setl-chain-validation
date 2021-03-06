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
package io.setl.bc.json.tx.external;

import static io.setl.common.StringUtils.logSafe;

import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import io.setl.common.ObjectArrayReader;
import io.setl.common.StringUtils;

/**
 * A unique ID and the namespace containing it. This is used in the external representations.
 *
 * @author Simon Greatrix on 23/01/2020.
 */
@JsonFormat(shape = Shape.ARRAY)
@JsonPropertyOrder({"namespace", "id"})
// Issue with Swagger-Core v2.0.9 - If this is defined as an ArraySchema then it is inlined everywhere it is used which is inefficient. If it is defined
// externally, then this class still gets processed and over-writes the external definition. By allowing the unwanted copy to be generated the external
// reference is correctly used in all the places the API uses SpaceId. We hope a future release of SwaggerCore will correctly reference ArraySchema
//
// @ArraySchema(
//     arraySchema = @Schema(description = "The namespace ID and object ID"),
//     schema = @Schema(implementation = String.class),
//     minItems = 2, maxItems = 2)
@Schema(name = "SpaceId-unwanted-copy", ref = "SpaceId",
    description = "An unwanted schema that is erroneously generated by Swagger Core. Do not use.")
public class SpaceId implements Comparable<SpaceId> {

  /** An empty space ID to use as an initial value. */
  public static final SpaceId EMPTY = new SpaceId();

  /** Text used to separate escape the namespace separator and itself. Naturally we use the C0 control code 27: "Escape". */
  public static final char NAMESPACE_ESCAPE = (char) 27;

  /** Text used to separate namespace from ID internally. The C0 control code 30 is the "Unit separator" for separating two pieces of textual data. */
  public static final char NAMESPACE_SEPARATOR = (char) 30;


  /**
   * Derive the actual identifier used in state from the namespace's ID and the item's ID.
   *
   * @param namespace the namespace's ID
   * @param id        the entry's ID
   *
   * @return the combined identifier used in state
   */
  public static String deriveFullId(String namespace, String id) {
    // probably don't need to escape as we are using the C0 codes
    int escapeNeeded = 0;
    int l = namespace.length();
    for (int i = 0; i < l; i++) {
      char ch = namespace.charAt(i);
      if (ch == NAMESPACE_ESCAPE || ch == NAMESPACE_SEPARATOR) {
        escapeNeeded++;
      }
    }

    if (escapeNeeded > 0) {
      // We need some escapes, so create an escaped copy of the namespace name
      char[] buf = new char[l + escapeNeeded];
      int j = 0;
      for (int i = 0; i < l; i++) {
        char ch = namespace.charAt(i);
        if (ch == NAMESPACE_ESCAPE || ch == NAMESPACE_SEPARATOR) {
          buf[j++] = NAMESPACE_ESCAPE;
        }
        buf[j++] = ch;
      }

      // concatenate the escaped name, the separator and the id
      return String.valueOf(buf) + NAMESPACE_SEPARATOR + id;
    }

    // No escapes needed - just concatenate the name, the separator and the id
    return namespace + NAMESPACE_SEPARATOR + id;
  }


  /**
   * Get a space ID or an empty space ID, never null.
   *
   * @param id the ID
   *
   * @return the ID or empty, never null
   */
  public static SpaceId notNull(SpaceId id) {
    return (id != null) ? id : EMPTY;
  }


  private final String fullId;

  private final String id;

  private final String namespace;


  private SpaceId() {
    id = "";
    namespace = "";
    fullId = deriveFullId(this.namespace, this.id);
  }


  /**
   * New instance.
   *
   * @param namespace the namespace ID
   * @param id        the entry's ID
   */
  @JsonCreator
  public SpaceId(
      @JsonProperty("namespace") String namespace,
      @JsonProperty("id") String id
  ) {
    this.namespace = StringUtils.notNull(namespace);
    this.id = StringUtils.notNull(id);
    fullId = deriveFullId(this.namespace, this.id);
  }


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public SpaceId(ObjectArrayReader reader) {
    namespace = reader.getString();
    id = reader.getString();
    fullId = deriveFullId(this.namespace, this.id);
  }


  /**
   * Recover a Space Id from its fullId representation.
   *
   * @param fullId the full ID representation
   */
  public SpaceId(String fullId) {
    this.fullId = fullId;
    int ps = fullId.indexOf(NAMESPACE_SEPARATOR);
    if (ps == -1) {
      throw new IllegalArgumentException(logSafe(fullId) + " is not a valid space ID full ID");
    }
    int pe = fullId.indexOf(NAMESPACE_ESCAPE);
    if (pe == -1) {
      // super-easy case
      namespace = fullId.substring(0, ps);
      id = fullId.substring(ps + 1);
      return;
    }

    // need to undo escaping
    int l = fullId.length();
    boolean lastWasEscape = false;
    StringBuilder builder = new StringBuilder(l);
    for (int i = 0; i < l; i++) {
      char ch = fullId.charAt(i);
      if (lastWasEscape) {
        if (ch != NAMESPACE_ESCAPE && ch != NAMESPACE_SEPARATOR) {
          throw new IllegalArgumentException(logSafe(fullId) + " is not a valid space ID full ID");
        }
        builder.append(ch);
        lastWasEscape = false;
      } else if (ch == NAMESPACE_ESCAPE) {
        lastWasEscape = true;
      } else if (ch == NAMESPACE_SEPARATOR) {
        namespace = builder.toString();
        id = fullId.substring(i + 1);
        return;
      } else {
        builder.append(ch);
      }
    }
    throw new IllegalArgumentException(logSafe(fullId) + " is not a valid space ID full ID");
  }


  public JsonObject asObject() {
    return Json.createObjectBuilder().add("namespace", namespace).add("id", id).build();
  }


  @Override
  public int compareTo(@NotNull SpaceId o) {
    int c = namespace.compareTo(o.namespace);
    if (c != 0) {
      return c;
    }
    return id.compareTo(o.id);
  }


  public Object[] encode() {
    return new Object[]{namespace, id};
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SpaceId)) {
      return false;
    }

    SpaceId spaceId = (SpaceId) o;

    if (!namespace.equals(spaceId.namespace)) {
      return false;
    }
    return id.equals(spaceId.id);
  }


  @JsonIgnore
  @Hidden
  @Nonnull
  public String getFullId() {
    return fullId;
  }


  @Schema(description = "The item's identifier within the namespace")
  public String getId() {
    return id;
  }


  @Schema(description = "The identifier of the namespace which contains the item.")
  public String getNamespace() {
    return namespace;
  }


  @Override
  public int hashCode() {
    int result = namespace.hashCode();
    return 31 * result + id.hashCode();
  }


  @JsonIgnore
  @Hidden
  public boolean isEmpty() {
    return id.isEmpty() && namespace.isEmpty();
  }


  public String toString() {
    return String.format("[%s|%s]", namespace, id);
  }

}
