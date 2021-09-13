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
package io.setl.bc.pychain.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.state.tx.helper.TxParameters;
import java.io.IOException;
import java.util.List;
import org.json.simple.JSONObject;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class EncumbranceDetail implements MsgPackable {

  public final String address;

  public final long endTime;

  public final long startTime;


  /**
   * EncumbranceDetail() Constructor.
   *
   * @param address   : String, Associated Address : Either the beneficiary or the administrator address.
   * @param startTime : Time at which this benefit initiates. UTC Unix epoch, seconds.
   * @param endTime   : Time at which this authorisation lapses, 0 for eternal.
   */
  public EncumbranceDetail(String address, Long startTime, Long endTime) {
    this.address = (address != null ? address : "");
    this.startTime = (startTime != null ? startTime : 0L);
    this.endTime = (endTime != null ? (endTime == 0L ? endTime : Math.max(endTime, this.startTime)) : 0L);
  }


  /**
   * EncumbranceDetail() Constructor.
   *
   * @param address   : String, Associated Address : Either the beneficiary or the administrator address.
   * @param startTime : Time at which this benefit initiates. UTC Unix epoch, seconds.
   * @param endTime   : Time at which this authorisation lapses, 0 for eternal.
   */
  @JsonCreator
  public EncumbranceDetail(
      @JsonProperty("address") String address,
      @JsonProperty("startTime") long startTime,
      @JsonProperty("endTime") long endTime
  ) {
    this.address = (address != null ? address : "");
    this.startTime = startTime;
    this.endTime = endTime == 0L ? endTime : Math.max(endTime, this.startTime);
  }


  /**
   * EncumbranceDetail().
   * <p>EncumbranceDetail Copy Constructor.</p>
   *
   * @param toCopy : EncumbranceDetail to copy.
   */
  public EncumbranceDetail(Object[] toCopy) {
    if ((toCopy == null) || (toCopy.length < 3)) {
      this.address = "";
      this.startTime = 1L;
      this.endTime = 1L;

    } else {

      this.address = (toCopy[0] != null ? toCopy[0].toString() : "");
      this.startTime = (toCopy[1] != null ? ((Number) toCopy[1]).longValue() : 0L);

      Long tempEnd = (toCopy[2] != null ? ((Number) toCopy[2]).longValue() : 0L);
      this.endTime = (tempEnd.equals(0L) ? tempEnd : Math.max(tempEnd, this.startTime));
    }
  }


  /**
   * New instance.
   *
   * @param unpacker the source of the data
   */
  public EncumbranceDetail(MessageUnpacker unpacker) throws IOException {
    address = unpacker.unpackString();
    startTime = unpacker.unpackLong();
    endTime = unpacker.unpackLong();
  }


  /**
   * EncumbranceDetail().
   * <p>EncumbranceDetail Copy Constructor.</p>
   *
   * @param toCopy : EncumbranceDetail to copy.
   */
  public EncumbranceDetail(List<?> toCopy) {
    this(toCopy.toArray());
  }


  /**
   * EncumbranceDetail().
   * <p>EncumbranceDetail Deep-Copy Constructor.</p>
   *
   * @param toCopy : EncumbranceDetail to copy.
   */
  public EncumbranceDetail(EncumbranceDetail toCopy) {

    this.address = toCopy.address;
    this.startTime = toCopy.startTime;
    this.endTime = toCopy.endTime;
  }


  /**
   * encode().
   * <p>Return Encumbrance Detail as Object[] for Hashing or Persistence purposes.</p>
   *
   * @return : Object[]
   */
  public Object[] encode() {

    return new Object[]{this.address, this.startTime, this.endTime};

  }


  /**
   * encodeJson.
   *
   * @return :
   */
  public JSONObject encodeJson() {

    JSONObject rVal = new JSONObject(true);

    rVal.put(TxParameters.ADDRESS, this.address);
    rVal.put(TxParameters.START_TIME, this.startTime);
    rVal.put(TxParameters.END_TIME, this.endTime);

    return rVal;
  }


  @Override
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof EncumbranceDetail)) {
      return false;
    }

    EncumbranceDetail theOther = (EncumbranceDetail) toCompare;

    if (!this.address.equals(theOther.address)) {
      return false;
    }

    if (this.startTime != theOther.startTime) {
      return false;
    }

    return this.endTime == theOther.endTime;
  }


  @Override
  public int hashCode() {

    return this.address.hashCode();
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    p.packString(address);
    p.packLong(startTime);
    p.packLong(endTime);
  }
}
