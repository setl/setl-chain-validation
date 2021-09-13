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
package io.setl.bc.pychain.validator.merkletree;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.monolithic.AssetBalanceList;
import io.setl.bc.pychain.state.monolithic.ContractsList;
import io.setl.bc.pychain.state.monolithic.EncumbrancesList;
import io.setl.bc.pychain.state.monolithic.NamespaceList;
import io.setl.bc.pychain.state.monolithic.PowerOfAttorney;
import io.setl.bc.pychain.state.monolithic.SignNodeList;
import io.setl.bc.pychain.validator.ValidationException;
import io.setl.bc.pychain.validator.Validator;
import io.setl.common.Sha256Hash;
import io.setl.utils.ByteUtil;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MonolithicMerkleTreeValidatorTest {

  @Mock
  DBStore dbStore;

  @Mock
  AbstractState state;

  @Mock
  StateReader stateReader;

  @Mock
  private AssetBalanceList assetBalanceList;

  @Mock
  private ContractsList contractList;

  @Mock
  private EncumbrancesList encumbranceList;

  private int height = 0;

  @Mock
  private NamespaceList namespaceList;

  @Mock
  private PowerOfAttorney powerOfAttorneyList;

  @Mock
  private Hash signNodeHash;

  @Mock
  private SignNodeList signNodeList;

  private String topHash = "960d1b389c25c0e9aa865761dd9e08426fd51afb1702a2f7153790ddcc56794a";

  private Validator validator;


  @Test(expected = ValidationException.class)
  public void hashesDoNotMatch() throws Exception {
    byte[] invalidTopHash = ByteUtil.hexToBytes("foobar");
    when(signNodeHash.get()).thenReturn(invalidTopHash);
    validator.validate(height);
  }


  @Before
  public void setup() throws Exception {
    when(dbStore.getStateHash(height)).thenReturn(topHash);
    when(stateReader.readState(topHash)).thenReturn(state);

    when(state.getSignNodes()).thenReturn(signNodeList);
    when(state.getNamespaces()).thenReturn(namespaceList);
    when(state.getAssetBalances()).thenReturn(assetBalanceList);
    when(state.getContracts()).thenReturn(contractList);
    when(state.getEncumbrances()).thenReturn(encumbranceList);
    when(state.getPowerOfAttorneys()).thenReturn(powerOfAttorneyList);

    when(namespaceList.iterator()).thenReturn(Collections.emptyIterator());
    when(assetBalanceList.iterator()).thenReturn(Collections.emptyIterator());
    when(contractList.iterator()).thenReturn(Collections.emptyIterator());
    when(encumbranceList.iterator()).thenReturn(Collections.emptyIterator());
    when(powerOfAttorneyList.iterator()).thenReturn(Collections.emptyIterator());

    SignNodeEntry e = new SignNodeEntry("1234", "a1", 1000, 10, 1, 2);
    SignNodeEntry[] leaves = new SignNodeEntry[]{e, e, e, e};
    when(signNodeList.iterator()).thenReturn(Arrays.asList(leaves).iterator());
    when(signNodeList.getHashableEntry(anyLong())).thenReturn(e.encode(-1));
    when(signNodeList.getHash()).thenReturn(signNodeHash);

    validator = new MerkleTreeValidator(dbStore, stateReader, Sha256Hash.newDigest(), HashSerialisation.getInstance());
  }


  @Test
  public void validationSucceeds() throws Exception {
    byte[] validTopHash = ByteUtil.hexToBytes(topHash);
    when(signNodeHash.get()).thenReturn(validTopHash);
    validator.validate(height);
  }

}
