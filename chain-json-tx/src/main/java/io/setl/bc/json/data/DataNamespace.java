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
package io.setl.bc.json.data;

import static io.setl.common.StringUtils.notNull;

import java.util.List;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 13/01/2020.
 */
public class DataNamespace extends BaseDatum {

  /** This namespace name is reserved for SETL's internal operations. */
  // It has a lock and the SETL icon as unicode characters at the start, to indicate it is locked to SETL and really reduce the chance of accidental collisions.
  public static final String INTERNAL_NAMESPACE = "⚿⋊ SETL Internal Use Only";

  /** Access control list associated with this. */
  private SpaceId defaultAclId = SpaceId.EMPTY;

  /** Default document validator. */
  private SpaceId defaultValidator = SpaceId.EMPTY;

  /** ID of this namespace. */
  private String id = "";

  /** The privileges of the namespace. */
  private NamespacePrivileges privileges = new NamespacePrivileges();


  protected DataNamespace(DataNamespace copy) {
    super(copy);
    id = copy.id;
    defaultAclId = copy.defaultAclId;
    defaultValidator = copy.defaultValidator;
    privileges = new NamespacePrivileges(copy.privileges);
  }


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS") // getReader returns different readers
  public DataNamespace(ObjectArrayReader reader) {
    super(reader);
    id = reader.getString();
    defaultAclId = new SpaceId(reader.getReader());
    defaultValidator = new SpaceId(reader.getReader());
    privileges = new NamespacePrivileges(reader.getReader());
  }


  public DataNamespace() {
    // do nothing
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DataNamespace(
      @JsonProperty(value = "id", required = true) String id,
      @JsonProperty(value = "privileges", required = true) NamespacePrivileges privileges
  ) {
    setId(id);
    setPrivileges(privileges);
  }


  @Override
  public DataNamespace copy() {
    return new DataNamespace(this);
  }


  @Override
  protected void encode(List<Object> list) {
    super.encode(list);
    list.add(id);
    list.add(defaultAclId.encode());
    list.add(defaultValidator.encode());
    list.add(privileges.encode());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataNamespace)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    DataNamespace that = (DataNamespace) o;

    if (!defaultAclId.equals(that.defaultAclId)) {
      return false;
    }
    if (!defaultValidator.equals(that.defaultValidator)) {
      return false;
    }
    if (!id.equals(that.id)) {
      return false;
    }
    return privileges.equals(that.privileges);
  }


  public SpaceId getDefaultAclId() {
    return defaultAclId;
  }


  public SpaceId getDefaultValidator() {
    return defaultValidator;
  }


  public String getId() {
    return id;
  }


  @JsonIgnore
  @Hidden
  @Nonnull
  @Override
  public String getKey() {
    return getId();
  }


  public NamespacePrivileges getPrivileges() {
    return privileges;
  }


  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + defaultAclId.hashCode();
    result = 31 * result + defaultValidator.hashCode();
    result = 31 * result + id.hashCode();
    return 31 * result + privileges.hashCode();
  }


  @Schema(description = "The default Access Control List applied to documents within this namespace")
  public void setDefaultAclId(SpaceId defaultAclId) {
    this.defaultAclId = SpaceId.notNull(defaultAclId);
  }


  @Schema(description = "The default validation mechanism for documents within this namespace")
  public void setDefaultValidator(SpaceId defaultValidator) {
    this.defaultValidator = defaultValidator;
  }


  @Schema(description = "The ID for this namespace", required = true)
  @JsonProperty(required = true)
  public final void setId(String id) {
    this.id = notNull(id);
  }


  @Schema(description = "The privileges assigned to users of this namespace", required = true)
  @JsonProperty(required = true)
  public final void setPrivileges(NamespacePrivileges privileges) {
    this.privileges = privileges != null ? privileges : new NamespacePrivileges();
  }

}
