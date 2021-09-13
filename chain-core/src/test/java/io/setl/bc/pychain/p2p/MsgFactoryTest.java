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
package io.setl.bc.pychain.p2p;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.p2p.message.PeerRecord.Record;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.pychain.tx.create.NullTX;
import io.setl.bc.serialise.UUIDEncoder;
import io.setl.common.CommonPy.NodeType;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.Hex;
import io.setl.crypto.KeyGen;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Created by wojciechcichon on 16/06/2017.
 */
public class MsgFactoryTest {

  private static final Object[] BLOCK = new Object[]{1, 2, "3", 4.0, 5L};

  private static final String BLOCK_SIGNATURE = "blockSignature";

  private static final int CHAIN = 20;

  private static final Hash CURRENTBLOCK_HASH = new Hash("blob123".getBytes(UTF_8));

  private static final int HEIGHT = 1;

  private static final MPWrappedMap<String, Object> IP_INFO = new MPWrappedMap<>(new HashMap<>());

  private static final Hash LASTBLOCK_HASH = new Hash("foo".getBytes(UTF_8));

  private static final Hash LASTSTATE_HASH = new Hash("bar".getBytes(UTF_8));

  private static final int LISTEN_PORT = 10;

  private static final String NODENAME = "name";

  private static final String NODE_UUID = "unique_uuid";

  private static final PrivateKey PRIVATE_KEY;

  private static final String PUBLIC_KEY;

  private static final Object[] SIGNATURE = new Object[]{};

  private static final long TIMESTAMP = 140123123L;

  private static final Hash XC_HASH = new Hash("xc hash".getBytes(UTF_8));

  private static final Object[] XC_SIGNATURE = new Object[]{"xc sign"};

  static {
    KeyPair keyPair = KeyGen.generateKeyPair();
    PUBLIC_KEY = Hex.encode(keyPair.getPublic().getEncoded());
    PRIVATE_KEY = keyPair.getPrivate();
  }

  private MsgFactory factory;


  private void assertDeepArrayEquals(String prefix, Object[] a1, Object[] a2) {
    assertEquals(a1.length, a2.length);
    for (int i = 0; i < a1.length; i++) {
      String p = prefix + "." + i;
      Object o1 = a1[i];
      Object o2 = a2[i];
      if (o1 == null) {
        assertNull(p, o2);
        continue;
      }
      assertNotNull(p, o2);
      assertEquals(p, o1.getClass(), o2.getClass());
      if (o1 instanceof Object[]) {
        assertDeepArrayEquals(p, (Object[]) o1, (Object[]) o2);
        continue;
      }
      assertEquals(p, o1, o2);
    }
  }


  @Test
  public void blockRequest() throws Exception {

    Object[] message = factory.blockRequest(CHAIN, HEIGHT, 1);
    Object[] expected = {CHAIN, P2PType.BLOCK_REQUEST.getId(), HEIGHT, 1};
    assertArrayEquals(message, expected);
  }


  @Test
  public void checkOrigin() throws Exception {
    Object[] actual = factory.checkOrigin(CHAIN, NODE_UUID, NodeType.Validation);
    Object[] expected = {CHAIN, P2PType.CHECK_ORIGIN.getId(), NODE_UUID, 0};
    assertArrayEquals(expected, actual);
  }


  @Test
  public void listenPort() throws Exception {
    Object[] actual = factory.listenPort(CHAIN, LISTEN_PORT, NODENAME);
    Object[] expected = {CHAIN, P2PType.LISTEN_PORT.getId(), LISTEN_PORT, NODENAME};
    assertArrayEquals(expected, actual);
  }


  @Test
  public void peerRequest() throws Exception {

    Object[] message = factory.peerRequest(CHAIN);
    Object[] expected = {CHAIN, P2PType.PEER_REQUEST.getId()};
    assertArrayEquals(message, expected);

  }


  @Test
  public void peerRequestResponse() throws Exception {

    Object[] message = factory.peerRequestResponse(CHAIN, Collections.singleton(new Record("dummy", 1234, NodeType.Validation, "wibble")));

    //FIXME currenty message is hardcoded
    assertEquals(CHAIN, message[0]);
    assertEquals(P2PType.PEER_RECORD.getId(), message[1]);
  }


  @Test
  public void preparingProposal() throws Exception {
    Object[] actual = factory.preparingProposal(CHAIN, TIMESTAMP, PUBLIC_KEY, HEIGHT, CURRENTBLOCK_HASH, PRIVATE_KEY);
    actual[actual.length - 1] = null; // remove signature
    Object[] expected = {CHAIN, P2PType.PREPARING_PROPOSAL.getId(), TIMESTAMP, HEIGHT, CURRENTBLOCK_HASH.get(), PUBLIC_KEY, null};
    assertArrayEquals(expected, actual);
  }


  @Test
  public void proposal() throws Exception {
    Block block = Mockito.mock(Block.class);
    Object[] actual = factory.proposal(CHAIN, HEIGHT, LASTSTATE_HASH, TIMESTAMP, block, NODENAME, PUBLIC_KEY, PRIVATE_KEY);
    actual[8] = null; // remove signature
    Object[] expected = {
        CHAIN, P2PType.PROPOSAL.getId(), HEIGHT, LASTSTATE_HASH.get(), TIMESTAMP,
        null, NODENAME, PUBLIC_KEY, null
    };
    assertArrayEquals(expected, actual);
  }


  @Before
  public void setUp() throws Exception {

    factory = new MsgFactory();
  }


  @Test
  public void signature() throws Exception {
    UUID proposalId = UUID.randomUUID();
    Object[] actual = factory.signature(CHAIN, HEIGHT, proposalId, CURRENTBLOCK_HASH, Collections.emptyList(), "XC", XC_HASH, PUBLIC_KEY, PRIVATE_KEY);
    actual[actual.length - 1] = null; // remove signature
    actual[actual.length - 2] = null; // remove xc signature
    Object[] expected =
        {
            CHAIN, P2PType.SIGNATURE.getId(), HEIGHT, UUIDEncoder.encode(proposalId), CURRENTBLOCK_HASH.get(), new Object[0], "XC", XC_HASH.get(), PUBLIC_KEY,
            null, null
        };
    assertArrayEquals(expected, actual);
  }


  @Test
  public void stateRequest() throws Exception {

    Object[] actual = factory.stateRequest(CHAIN);
    Object[] expected = {CHAIN, P2PType.STATE_REQUEST.getId()};

    assertArrayEquals(expected, actual);
  }


  @Test
  public void stateResponse() throws Exception {

    Object[] message = factory.stateResponse(CHAIN, HEIGHT, LASTSTATE_HASH, LASTBLOCK_HASH, 33L);
    Object[] expected = new Object[]{
        CHAIN, P2PType.STATE_RESPONSE.getId(), HEIGHT, new Object[0], LASTSTATE_HASH.get(),
        LASTBLOCK_HASH.get(), 33L
    };
    assertArrayEquals(expected, message);

  }


  @Test
  public void txPackageForwardFromOriginal() throws Exception {
    NullTx tx = NullTX.nullUnsigned(1, 2, "fred", "address", "poa");
    Object[] data = factory.txPackageOriginal(CHAIN, new Object[][]{tx.encodeTx()});
    TxPackage txPackage = (TxPackage) factory.create(new MPWrappedArrayImpl(data));
    Object[] expected = new Object[]{CHAIN, P2PType.TX_PACKAGE_FORWARD.getId(), new Object[]{new MPWrappedArrayImpl(tx.encodeTx())}};
    Object[] actual = factory.txPackageForwardFromOriginal(new MPWrappedArrayImpl(data));

    assertNotNull(actual);
    assertEquals(expected.length, actual.length);
    assertDeepArrayEquals("", expected, actual);
  }


  @Test
  public void vote() throws Exception {
    UUID proposalId = UUID.randomUUID();
    Object[] actual = factory.vote(CHAIN, proposalId, CURRENTBLOCK_HASH, HEIGHT, TIMESTAMP, PUBLIC_KEY, PRIVATE_KEY);
    actual[actual.length - 1] = null; // remove signature
    Object[] expected =
        {CHAIN, P2PType.VOTE.getId(), UUIDEncoder.encode(proposalId), TIMESTAMP, CURRENTBLOCK_HASH.get(), HEIGHT, TIMESTAMP, PUBLIC_KEY, null};
    assertArrayEquals(expected, actual);
  }

}