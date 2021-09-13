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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;

import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 13/01/2020.
 */
@JsonPropertyOrder(alphabetic = true)
public abstract class BaseDatum implements MEntry {

  /** Time of last modification or creation. */
  protected long lastModified = 0;

  /** Metadata associated with this. */
  protected String metadata = "";

  /** The human-friendly title for this. */
  protected String title = "";

  /** The chain height at which this was updated or created. */
  protected long updateHeight = -1;


  protected BaseDatum() {
    // do nothing
  }


  protected BaseDatum(BaseDatum copy) {
    this.lastModified = copy.lastModified;
    this.metadata = copy.metadata;
    this.title = copy.title;
    this.updateHeight = copy.updateHeight;
  }


  /**
   * New instance from the encoded form.
   *
   * @param encoded reader of the encoded form
   */
  public BaseDatum(ObjectArrayReader encoded) {
    this.lastModified = encoded.getLong();
    this.metadata = encoded.getString();
    this.title = encoded.getString();
    this.updateHeight = encoded.getLong();
  }


  protected void encode(List<Object> list) {
    list.clear();
    list.add(lastModified);
    list.add(metadata);
    list.add(title);
    list.add(updateHeight);
  }


  /**
   * Encode this for persistent storage.
   *
   * @return the encoded form of this
   */
  public Object[] encode() {
    ArrayList<Object> list = new ArrayList<>();
    encode(list);
    return list.toArray();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseDatum)) {
      return false;
    }

    BaseDatum baseDatum = (BaseDatum) o;

    if (updateHeight != baseDatum.updateHeight) {
      return false;
    }
    if (lastModified != baseDatum.lastModified) {
      return false;
    }
    if (!metadata.equals(baseDatum.metadata)) {
      return false;
    }
    return title.equals(baseDatum.title);
  }


  @Schema(description = "The block chain height at which this datum was last updated.", accessMode = AccessMode.READ_ONLY)
  @JsonProperty("updateHeight")
  @Override
  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  @Schema(description = "The time (in seconds since the epoch) at which this datum was last updated.", accessMode = AccessMode.READ_ONLY)
  public long getLastModified() {
    return lastModified;
  }


  @Schema(description = "Metadata associated with this datum.")
  public String getMetadata() {
    return metadata;
  }


  @Schema(description = "The title of this datum.")
  public String getTitle() {
    return title;
  }


  @Override
  public int hashCode() {
    int result = (int) (updateHeight ^ (updateHeight >>> 32));
    result = 31 * result + (int) (lastModified ^ (lastModified >>> 32));
    result = 31 * result + metadata.hashCode();
    return 31 * result + title.hashCode();
  }


  @Override
  public void setBlockUpdateHeight(long blockHeight) {
    updateHeight = blockHeight;
  }


  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }


  public void setMetadata(String metadata) {
    this.metadata = notNull(metadata);
  }


  public void setTitle(String title) {
    this.title = notNull(title);
  }

}
