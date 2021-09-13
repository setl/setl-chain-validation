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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 21/01/2020.
 */
public class JsonTxFromList implements TxFromList.Decoder {

  private static final EnumSet<TxType> HANDLES = EnumSet.of(
      TxType.JSON_CREATE_DATA_NAMESPACE,
      TxType.JSON_DELETE_DATA_NAMESPACE,
      TxType.JSON_UPDATE_DATA_NAMESPACE,
      TxType.JSON_CREATE_DATA_DOCUMENT,
      TxType.JSON_DELETE_DATA_DOCUMENT,
      TxType.JSON_UPDATE_DATA_DOCUMENT,
      TxType.JSON_UPDATE_DESCRIPTION,
      TxType.JSON_CREATE_VALIDATOR,
      TxType.JSON_DELETE_VALIDATOR,
      TxType.JSON_CREATE_ALGORITHM,
      TxType.JSON_CREATE_ACL,
      TxType.JSON_DELETE_ACL,
      TxType.JSON_UPDATE_ACL,
      TxType.JSON_CREATE_ACL_ROLE,
      TxType.JSON_DELETE_ACL_ROLE,
      TxType.JSON_UPDATE_ACL_ROLE
  );


  @Override
  public AbstractTx decode(TxType txType, MPWrappedArray txData) {
    ObjectArrayReader reader = new ObjectArrayReader(txData.unwrap());
    switch (txType) {
      case JSON_CREATE_ACL:
        return new CreateAclTx(reader);
      case JSON_DELETE_ACL:
        return new DeleteAclTx(reader);
      case JSON_UPDATE_ACL:
        return new UpdateAclTx(reader);
      case JSON_CREATE_ACL_ROLE:
        return new CreateAclRoleTx(reader);
      case JSON_DELETE_ACL_ROLE:
        return new DeleteAclRoleTx(reader);
      case JSON_UPDATE_ACL_ROLE:
        return new UpdateAclRoleTx(reader);
      case JSON_CREATE_DATA_NAMESPACE:
        return new CreateDataNamespaceTx(reader);
      case JSON_UPDATE_DATA_NAMESPACE:
        return new UpdateDataNamespaceTx(reader);
      case JSON_DELETE_DATA_NAMESPACE:
        return new DeleteDataNamespaceTx(reader);
      case JSON_CREATE_DATA_DOCUMENT:
        return new CreateDataDocumentTx(reader);
      case JSON_DELETE_DATA_DOCUMENT:
        return new DeleteDataDocumentTx(reader);
      case JSON_UPDATE_DATA_DOCUMENT:
        return new UpdateDataDocumentTx(reader);
      case JSON_UPDATE_DESCRIPTION:
        return new UpdateDataDocumentDescriptionTx(reader);
      case JSON_CREATE_VALIDATOR:
        return new CreateValidatorTx(reader);
      case JSON_DELETE_VALIDATOR:
        return new DeleteValidatorTx(reader);
      case JSON_CREATE_ALGORITHM:
        // We will not be implementing create algorithm at this time
        throw new NotImplementedException();
      default:
        throw new IllegalArgumentException("Handler asked to handle: " + txType);
    }
  }


  @Override
  public Set<TxType> handles() {
    return Collections.unmodifiableSet(HANDLES);
  }

}
