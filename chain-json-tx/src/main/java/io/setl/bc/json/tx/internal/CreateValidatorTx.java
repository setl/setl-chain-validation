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

import io.setl.bc.json.data.DocumentValidator;
import io.setl.bc.json.tx.external.CreateValidator;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
public class CreateValidatorTx extends BaseTx {

  private DocumentValidator validator;


  public CreateValidatorTx(CreateValidator createValidator) {
    super(createValidator);
    validator = createValidator.getValidator();
  }


  public CreateValidatorTx(ObjectArrayReader encoded) {
    super(encoded);
    validator = new DocumentValidator(encoded.getReader());
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    startHash(accumulator);
    accumulator.add(validator.encode());
    return accumulator;
  }


  @Override
  public TxType getTxType() {
    return TxType.JSON_CREATE_VALIDATOR;
  }


  public DocumentValidator getValidator() {
    return validator.copy();
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public void setValidator(DocumentValidator validator) {
    this.validator = validator;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = validator.encode();
    return p;
  }

}
