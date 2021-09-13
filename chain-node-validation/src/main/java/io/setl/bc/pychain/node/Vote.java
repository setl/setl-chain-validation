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
package io.setl.bc.pychain.node;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;

/**
 * Thin wrapper around an message pack style vote.
 * Created by aanten on 28/06/2017.
 */
public class Vote {

  private final MPWrappedArray va;


  public Vote(final MPWrappedArray va) {
    this.va = va;
  }


  public int getHeight() {
    return va.asInt(3);
  }


  public Hash getProposalHash() {
    return Hash.fromHex(va.asString(2));
  }


  public String getPublicKey() {
    return va.asString(6);
  }


  public MPWrappedArray getRaw() {
    return va;
  }


  public String getSignature() {
    return va.asString(4);
  }


  public long getTimestamp() {
    return va.asLong(5);
  }
}
