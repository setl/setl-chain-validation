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
package io.setl.bc.pychain.state.test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.setl.bc.pychain.Hash;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Identifier of a cross chain transaction.
 *
 * @author Simon Greatrix on 09/01/2020.
 */
@JsonFormat(shape = Shape.ARRAY)
@JsonPropertyOrder({"chainId", "hash"})
public class XCTxId implements Comparable<XCTxId> {

  private int chainId;

  private Hash hash;


  public XCTxId() {
    // do nothing
  }


  public XCTxId(int chainId, Hash hash) {
    this.hash = hash;
    this.chainId = chainId;
  }


  @Override
  public int compareTo(@NotNull XCTxId o) {
    int c = Integer.compare(chainId, o.chainId);
    if (c != 0) {
      return c;
    }
    return hash.compareTo(o.hash);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof XCTxId)) {
      return false;
    }
    XCTxId xcTxId = (XCTxId) o;
    return chainId == xcTxId.chainId && Objects.equals(hash, xcTxId.hash);
  }


  public int getChainId() {
    return chainId;
  }


  public Hash getHash() {
    return hash;
  }


  @Override
  public int hashCode() {
    return Objects.hash(chainId, hash);
  }


  public void setChainId(int chainId) {
    this.chainId = chainId;
  }


  public void setHash(Hash hash) {
    this.hash = hash;
  }


  @Override
  public String toString() {
    return String.format(
        "XCTxId(chainId=%s, hash=%s)",
        this.chainId, this.hash
    );
  }
}
