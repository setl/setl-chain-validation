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
package io.setl.bc.pychain.state.tx.poadataclasses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * The header for a specific PoA. Every unique PoA has a single header and one or more details.
 *
 * @See PoaDetail
 *
 * @author Simon Greatrix on 25/09/2019.
 */
public class PoaHeader {

  @Schema(description = "The time this PoA expires (Seconds since the epoch).")
  public final long expiryDate;

  /** The Hash parameter is unused, but must be preserved to maintain signatures. */
  @JsonIgnore
  @Schema(hidden = true)
  public final String hash; // Unused ?

  @Schema(description = "The unique reference for this PoA.")
  public final String reference;

  @Schema(description = "The time this PoA begins (Seconds since the epoch).")
  public final long startDate;


  @JsonCreator
  public PoaHeader(
      @JsonProperty("reference")
          String reference,
      @JsonProperty("startDate")
          long startDate,
      @JsonProperty("expiryDate")
          long expiryDate
  ) {
    this.reference = reference;
    this.hash = "";
    this.startDate = startDate;
    this.expiryDate = expiryDate;
  }


  public PoaHeader(String reference, String hash, long startDate, long expiryDate) {
    this.reference = reference;
    this.hash = hash;
    this.startDate = startDate;
    this.expiryDate = expiryDate;
  }


  public PoaHeader(Object[] data) {
    if ((data != null) && (data.length == 4)) {
      reference = (String) data[0];
      hash = (String) data[1];
      startDate = (long) data[2];
      expiryDate = (long) data[3];
    } else {
      // Default
      reference = "<Bad Constructor>";
      hash = "";
      startDate = 0L;
      expiryDate = 0L;
    }

  }


  public PoaHeader(PoaHeader toCopy) {
    if (toCopy != null) {
      reference = toCopy.reference;
      hash = toCopy.hash;
      startDate = toCopy.startDate;
      expiryDate = toCopy.expiryDate;
    } else {
      //Default
      reference = "<Bad Constructor>";
      hash = "";
      startDate = 0L;
      expiryDate = 0L;
    }
  }


  /**
   * encode().
   *
   * @return : Object[]
   */
  public Object[] encode() {
    Object[] rVal = new Object[4];

    rVal[0] = reference;
    rVal[1] = hash;
    rVal[2] = startDate;
    rVal[3] = expiryDate;

    return rVal;
  }


  /**
   * equals().
   * <p>Deep equality comparator.</p>
   *
   * @param toCompare :
   *
   * @return :
   */
  @Override
  public boolean equals(Object toCompare) {
    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof PoaHeader)) {
      return false;
    }

    PoaHeader theOther = (PoaHeader) toCompare;

    if (!Objects.equals(reference, theOther.reference)) {
      return false;
    }

    if (!Objects.equals(hash, theOther.hash)) {
      return false;
    }

    if (startDate != theOther.startDate) {
      return false;
    }

    return expiryDate == theOther.expiryDate;
  }


  @Override
  public int hashCode() {
    if (reference != null) {
      return reference.hashCode();
    }

    return 0;
  }

}
