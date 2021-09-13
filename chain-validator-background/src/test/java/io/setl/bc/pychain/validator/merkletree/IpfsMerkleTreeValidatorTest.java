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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.ipfs.AssetBalanceIpfs;
import io.setl.bc.pychain.state.ipfs.ContractsIpfs;
import io.setl.bc.pychain.state.ipfs.EncumbrancesIpfs;
import io.setl.bc.pychain.state.ipfs.IpfsBasedState;
import io.setl.bc.pychain.state.ipfs.NamespaceIpfs;
import io.setl.bc.pychain.state.ipfs.PowerOfAttorneyIpfs;
import io.setl.bc.pychain.state.ipfs.SignNodeIpfs;
import io.setl.bc.pychain.validator.ValidationException;
import io.setl.bc.pychain.validator.Validator;
import io.setl.bc.store.IpfsStore;
import io.setl.bc.store.RawStore;
import io.setl.common.Sha256Hash;
import io.setl.utils.ByteUtil;

@RunWith(SpringRunner.class)
public class IpfsMerkleTreeValidatorTest {

  @Mock
  DBStore dbStore;

  @Mock
  private AssetBalanceIpfs assetBalanceList;

  @Mock
  private ContractsIpfs contractList;

  @Mock
  private EncumbrancesIpfs encumbranceList;

  private int height = 0;

  @Mock
  private NamespaceIpfs namespaceList;

  @Mock
  private PowerOfAttorneyIpfs powerOfAttorneyList;

  @Mock
  private SignNodeIpfs signNodeList;

  @Mock
  private IpfsBasedState state;

  private StateReader stateReader;

  @Mock
  private Hash topHash;

  private byte[] topHashBytes = ByteUtil.hexToBytes("63e81b531f046dd48db1738e387d469472b1ecc85474a3aadb2b7c214190475a");

  private Validator validator;


  @Test(expected = ValidationException.class)
  public void leafHashesDoNotMatch() throws Exception {
    Object[] ipfsObjects = {
        new byte[][]{
            ByteUtil.hexToBytes("6c399e63cd14d9db73c1debc0837f6bb96c4680ae262aa4ab051e06fdc246c13"),
            ByteUtil.hexToBytes("c2f8f9ba8de86e778588c7dd79ca601f2d3d9f7f49b5e8b726723d88fac7b341")
        },
        new byte[][]{
            ByteUtil.hexToBytes("a1c2bc79ed2bab378c9dc8d7f74700f354d0698473de99dcb4f19f457b5a3c5a"),
            ByteUtil.hexToBytes("e7fd27c51d4974716796dbc3e074e95e51277467a6131084055defc513706d2c")
        },
        new byte[][]{
            ByteUtil.hexToBytes("f791ec2c681e37ffc8cec403522a97cc4cbc7d7b66c94d26841281a01d9a1bda"),
            ByteUtil.hexToBytes("80c0d021dbf28c62bb9d3003f9e38c4b9fdddb1014aa49dcd69b3521a2395fd7")
        },
        new Object[]{"alpha"},
        new Object[]{"zulu"}, // invalid leaf
        new Object[]{"charlie"},
        new Object[]{"delta"}
    };

    doReturn(
        ipfsObjects[0],
        ipfsObjects[1],
        ipfsObjects[3],
        ipfsObjects[4],
        ipfsObjects[2],
        ipfsObjects[5],
        ipfsObjects[6]
    ).when((IpfsStore) stateReader).get(any(Hash.class));

    validator.validate(height);
  }


  @Test(expected = ValidationException.class)
  public void nodeHashesDoNotMatch() throws Exception {
    Object[] ipfsObjects = {
        new byte[][]{
            ByteUtil.hexToBytes("6c399e63cd14d9db73c1debc0837f6bb96c4680ae262aa4ab051e06fdc246c13"),
            ByteUtil.hexToBytes("c2f8f9ba8de86e778588c7dd79ca601f2d3d9f7f49b5e8b726723d88fac7b341")
        },
        new byte[][]{
            ByteUtil.hexToBytes("a1c2bc79ed2bab378c9dc8d7f74700f354d0698473de99dcb4f19f457b5a3c5a"),
            ByteUtil.hexToBytes("3b3b53c2a6bdd088d8b0fa6b73274972db439f6ae25393f680a77d6112cded94") // invalid hash
        },
        new byte[][]{
            ByteUtil.hexToBytes("f791ec2c681e37ffc8cec403522a97cc4cbc7d7b66c94d26841281a01d9a1bda"),
            ByteUtil.hexToBytes("80c0d021dbf28c62bb9d3003f9e38c4b9fdddb1014aa49dcd69b3521a2395fd7")
        },
        new Object[]{"alpha"},
        new Object[]{"bravo"},
        new Object[]{"charlie"},
        new Object[]{"delta"}
    };

    doReturn(
        ipfsObjects[0],
        ipfsObjects[1],
        ipfsObjects[3],
        ipfsObjects[4],
        ipfsObjects[2],
        ipfsObjects[5],
        ipfsObjects[6]
    ).when((IpfsStore) stateReader).get(any(Hash.class));

    validator.validate(height);
  }


  @Before
  public void setup() throws Exception {
    String stateHash = "ABCD1234";

    RawStore db = mock(RawStore.class);
    IpfsStore bosStateHandler = new IpfsStore(db);
    stateReader = spy(bosStateHandler);

    when(dbStore.getStateHash(height)).thenReturn(stateHash);
    doReturn(state).when(stateReader).readState(stateHash);

    when(state.getSignNodes()).thenReturn(signNodeList);
    when(state.getNamespaces()).thenReturn(namespaceList);
    when(state.getAssetBalances()).thenReturn(assetBalanceList);
    when(state.getContracts()).thenReturn(contractList);
    when(state.getEncumbrances()).thenReturn(encumbranceList);
    when(state.getPowerOfAttorneys()).thenReturn(powerOfAttorneyList);

    when(signNodeList.getHash()).thenReturn(topHash);
    when(namespaceList.getHash()).thenReturn(topHash);
    when(assetBalanceList.getHash()).thenReturn(topHash);
    when(contractList.getHash()).thenReturn(topHash);
    when(encumbranceList.getHash()).thenReturn(topHash);
    when(powerOfAttorneyList.getHash()).thenReturn(topHash);

    when(topHash.get()).thenReturn(topHashBytes, null, null, null, null, null);

    validator = new MerkleTreeValidator(dbStore, stateReader, Sha256Hash.newDigest(), HashSerialisation.getInstance());
  }


  @Test
  public void validationSucceeds() throws Exception {
    Object[] ipfsObjects = {
        new byte[][]{
            ByteUtil.hexToBytes("6c399e63cd14d9db73c1debc0837f6bb96c4680ae262aa4ab051e06fdc246c13"),
            ByteUtil.hexToBytes("c2f8f9ba8de86e778588c7dd79ca601f2d3d9f7f49b5e8b726723d88fac7b341")
        },
        new byte[][]{
            ByteUtil.hexToBytes("a1c2bc79ed2bab378c9dc8d7f74700f354d0698473de99dcb4f19f457b5a3c5a"),
            ByteUtil.hexToBytes("e7fd27c51d4974716796dbc3e074e95e51277467a6131084055defc513706d2c")
        },
        new byte[][]{
            ByteUtil.hexToBytes("f791ec2c681e37ffc8cec403522a97cc4cbc7d7b66c94d26841281a01d9a1bda"),
            ByteUtil.hexToBytes("80c0d021dbf28c62bb9d3003f9e38c4b9fdddb1014aa49dcd69b3521a2395fd7")
        },
        new Object[]{"alpha"},
        new Object[]{"bravo"},
        new Object[]{"charlie"},
        new Object[]{"delta"}
    };

    doReturn(
        ipfsObjects[0],
        ipfsObjects[1],
        ipfsObjects[3],
        ipfsObjects[4],
        ipfsObjects[2],
        ipfsObjects[5],
        ipfsObjects[6]
    ).when((IpfsStore) stateReader).get(any(Hash.class));

    validator.validate(height);
  }

}
