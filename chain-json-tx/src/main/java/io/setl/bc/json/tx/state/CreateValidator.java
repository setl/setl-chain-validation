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
package io.setl.bc.json.tx.state;

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.data.DocumentValidator;
import io.setl.bc.json.data.NamespacePrivilege;
import io.setl.bc.json.tx.internal.CreateValidatorTx;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.bc.pychain.tx.updatestate.TxProcessor;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.rules.AddressRules;
import io.setl.bc.pychain.tx.updatestate.rules.MerkleRules;

/**
 * @author Simon Greatrix on 24/01/2020.
 */
public class CreateValidator extends TxProcessor<CreateValidatorTx> {

  private MutableMerkle<DataNamespace> namespaceMerkle;
  private DocumentValidator validator;
  private MutableMerkle<DocumentValidator> validatorMerkle;


  @Override
  protected Class<CreateValidatorTx> fulfills() {
    return CreateValidatorTx.class;
  }


  @Override
  protected void initialise(StateSnapshot snapshot, CreateValidatorTx txi) {
    namespaceMerkle = snapshot.getMerkle(DataNamespace.class);
    validator = txi.getValidator();
    validatorMerkle = snapshot.getMerkle(DocumentValidator.class);
  }


  @Override
  protected ReturnTuple tryApply(CreateValidatorTx txi, StateSnapshot snapshot, long updateTime) {

    // Create the validator
    validator.setLastModified(updateTime);
    validatorMerkle.add(validator);
    return null;
  }


  @Override
  protected ReturnTuple tryChecks(CreateValidatorTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // verify all the addresses are OK
    AddressRules.checkAddress(txi.getFromAddress());

    // Check the validator has not already been created.
    MerkleRules.checkNotExists("Validator", validatorMerkle, validator);

    // Check that the namespace it will be stored in has been created
    Rules.checkNamespacePrivilege(namespaceMerkle, validator.getId(), NamespacePrivilege.CREATE_VALIDATOR, txi.getFromAddress());

    // Verify the validator can be instantiated.
    try {
      validator.getValidator();
    } catch (RuntimeException e) {
      throw new TxFailedException("Unable to instantiate validator", e);
    }

    // all checks done
    return null;
  }

}
