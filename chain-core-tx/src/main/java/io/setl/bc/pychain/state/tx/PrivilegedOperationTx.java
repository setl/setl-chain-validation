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
package io.setl.bc.pychain.state.tx;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * Process an invocation of the power of a privileged key.
 *
 * @author Simon Greatrix on 07/07/2020.
 */
public class PrivilegedOperationTx extends AbstractTx {

  public static PrivilegedOperationTx decode(MPWrappedArray txData) {
    ObjectArrayReader reader = new ObjectArrayReader(txData.unwrap());
    return new PrivilegedOperationTx(reader);
  }


  private final TreeMap<String, Object> operationInput;

  private final String operationName;


  /**
   * Construct an instance from its encoded form.
   *
   * @param reader reader of the encoded form
   */
  public PrivilegedOperationTx(ObjectArrayReader reader) {
    super(reader);
    operationName = reader.getString();
    operationInput = new TreeMap<>(reader.getMap());
  }


  /**
   * Create a new instance with the standard TX fields.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PrivilegedOperationTx(
      int chainId,
      String hash,
      long nonce,
      boolean isUpdated,
      String address,
      String publicKey,
      String signature,
      String poa,
      long timestamp,
      String operationName,
      Map<String, Object> operationInput
  ) {
    super(
        chainId,
        hash,
        nonce,
        isUpdated,
        address,
        publicKey,
        signature,
        poa,
        timestamp
    );
    this.operationName = operationName;
    this.operationInput = new TreeMap<>(operationInput);
  }


  @Override
  public Set<String> addresses() {
    return Set.of(getFromAddress());
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(operationName);
    accumulator.add(operationInput);
    return accumulator;
  }


  @Override
  public Object[] encodeTx() {
    Object[] data = new Object[requiredEncodingSize()];
    startEncode(data);
    return data;
  }


  public Map<String, Object> getOperationInput() {
    return operationInput;
  }


  public String getOperationName() {
    return operationName;
  }


  @Override
  public TxType getTxType() {
    return TxType.DO_PRIVILEGED_OPERATION;
  }


  @Override
  protected int requiredEncodingSize() {
    // Add fields for operation name and operation input
    return super.requiredEncodingSize() + 2;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = operationName;
    encoded[p++] = operationInput;
    return p;
  }

}
