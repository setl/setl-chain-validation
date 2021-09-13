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
import io.setl.bc.serialise.UUIDEncoder;
import io.setl.common.CommonPy.P2PType;
import java.util.UUID;

/**
 * @author Simon Greatrix on 2019-03-27.
 */
public class TxPackageResponse extends TxPackage {

  private final UUID uuid;


  public TxPackageResponse(MPWrappedArray message) {
    super(P2PType.TX_PACKAGE_RESPONSE, message.asInt(0), message.asObjectArray(3));
    uuid = UUIDEncoder.decode(message.asByte(2));
  }


  public TxPackageResponse(int chainId, UUID proposalId, Object[][] encodedTx) {
    super(P2PType.TX_PACKAGE_RESPONSE, chainId, encodedTx);
    this.uuid = proposalId;
  }


  @Override
  public Object[] encode() {
    return new Object[]{chainId, type.getId(), UUIDEncoder.encode(uuid), encodedTxs.toArray()};
  }


  @Override
  public P2PType getType() {
    return P2PType.TX_PACKAGE_RESPONSE;
  }


  public UUID getUuid() {
    return uuid;
  }
}
