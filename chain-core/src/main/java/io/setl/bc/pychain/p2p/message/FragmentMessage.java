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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.common.CommonPy.P2PType;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Nicholas Pennington on 2021-04-01
 */
public class FragmentMessage implements Message {
  /*
  * FragmentMessage class is used to persist a piece of a larger Blockchain message and to re-constitute fragments to a whole message.
  *
  *
  * */

  protected final int chainId;      // Chain ID
  protected final int thisCount;    // ranges 1 -> totalCount
  protected final int totalCount;   // Total number of fragments in the overall message
  protected final int valueLength;  // size of the overall message (nut just this fragment)
  protected final int thisOffset;   // offset in the byte array where this data should be written
  protected final String messageID; // Unique message id to link fragments together
  protected final byte[] data;      // Fragment data (or whole message data if re-constituting)
  protected final P2PType type;     // Info Only : type of the original message
  protected final Map metadata;     // Info Only : metadata that may be applied to a fragment
  long timestamp;

  // temp array used when re-constituting messages. counts how often a piece has been applied (should be only once, but there could be retries)
  protected final byte[] index;

  public boolean missingPieces() {
    for (int i = 0; i < index.length; i++) {
      if (index[i] == 0) {
        return true;
      }
    }
    return false;
  }


  public FragmentMessage addPiece(FragmentMessage fm) {
    // Copies into existing data.
    // data byte array was pre-allocated to be the full required length.

    int fragmentIndex = fm.getCount();
    int fragmentDataOffset = fm.getThisOffset();
    int fragmentDataLength = fm.getData().length;

    System.arraycopy(fm.getData(), 0, data, fragmentDataOffset, fragmentDataLength);

    // register in index.
    if ((fragmentIndex > 0) && (fragmentIndex <= index.length) && (index[fragmentIndex - 1] < Byte.MAX_VALUE)) {
      index[fragmentIndex - 1] += 1;
    }

    return this;
  }

  public FragmentMessage(MPWrappedArray message) {
    this.chainId = message.asInt(0);
    this.type = P2PType.get(message.asInt(1));
    this.messageID = message.asString(2);
    this.thisCount = message.asInt(3);
    this.totalCount = message.asInt(4);
    this.valueLength = message.asInt(5);
    this.thisOffset = message.asInt(6);
    this.data = message.asByte(7);
    this.metadata = message.asWrappedMap(8).toMap();

    this.index = new byte[Math.max(0, this.totalCount)];
  }


  public FragmentMessage(int chainId, String messageID, int thisCount, int totalCount, int valueLength, int thisOffset, byte[] data, Map metadata, long timestamp) {
    this.chainId = chainId;
    this.messageID = messageID;
    this.type = P2PType.MESSAGE_FRAGMENT;
    this.thisCount = thisCount;
    this.totalCount = totalCount;
    this.valueLength = valueLength;
    this.thisOffset = thisOffset;
    this.data = data;
    this.metadata = (metadata == null) ? new TreeMap() : metadata;
    this.timestamp = timestamp;

    this.index = new byte[Math.max(0, this.totalCount)];
  }


  @Override
  public Object[] encode() {
    return new Object[]{
        chainId,
        type.getId(),
        messageID,
        thisCount,
        totalCount,
        valueLength,
        thisOffset,
        data,
        new MPWrappedMap<>(metadata)};
  }

  @Override
  public int getChainId() {
    return chainId;
  }

  @Override
  public P2PType getType() {
    return this.type;
  }

  public String getMessageID() {
    return messageID;
  }

  public int getCount() {
    return thisCount;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getValueLength() {
    return valueLength;
  }

  public int getThisOffset() {
    return thisOffset;
  }

  public byte[] getData() {
    return data;
  }

  public Map getMetaData() {
    return this.metadata;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public FragmentMessage setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }


}
