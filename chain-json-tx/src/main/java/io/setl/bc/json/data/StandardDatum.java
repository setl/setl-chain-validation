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

import java.util.List;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.common.ObjectArrayReader;

/**
 * A standard datum has an ID within a Namespace.
 *
 * @author Simon Greatrix on 23/01/2020.
 */
public abstract class StandardDatum extends BaseDatum {

  private SpaceId id = SpaceId.EMPTY;


  public StandardDatum() {
  }


  protected StandardDatum(StandardDatum copy) {
    super(copy);
    id = copy.id;
  }


  public StandardDatum(ObjectArrayReader encoded) {
    super(encoded);
    id = new SpaceId(encoded.getReader());
  }


  public StandardDatum(SpaceId id) {
    setId(id);
  }


  @Override
  protected void encode(List<Object> list) {
    super.encode(list);
    list.add(id.encode());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StandardDatum)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    StandardDatum that = (StandardDatum) o;

    return id.equals(that.id);
  }


  public SpaceId getId() {
    return id;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public String getKey() {
    return id.getFullId();
  }


  @Override
  public int hashCode() {
    int result = super.hashCode();
    return 31 * result + id.hashCode();
  }


  @Schema(description = "The containing namespace and identifier for this datum.")
  @JsonProperty(required = true)
  public final void setId(SpaceId id) {
    this.id = SpaceId.notNull(id);
  }

}
