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
package io.setl.bc.json.tx.internal;

import io.setl.bc.json.tx.external.JsonBaseTransaction;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.helper.AddressSet;
import io.setl.common.ObjectArrayReader;
import java.util.Set;

/**
 * @author Simon Greatrix on 23/01/2020.
 */
public abstract class BaseTx extends AbstractTx {

  protected BaseTx(JsonBaseTransaction base) {
    super(
        base.getChainId(),
        base.getHash(),
        base.getNonce(),
        base.isUpdated(),
        base.getFromAddress(),
        base.getFromPublicKey(),
        base.getSignature(),
        base.getPoa(),
        base.getTimestamp()
    );
  }


  protected BaseTx(ObjectArrayReader reader) {
    super(reader);
  }


  @Override
  public Set<String> addresses() {
    return AddressSet.of(getFromAddress());
  }


  @Override
  public Object[] encodeTx() {
    Object[] data = new Object[requiredEncodingSize()];
    startEncode(data);
    return data;
  }

}
