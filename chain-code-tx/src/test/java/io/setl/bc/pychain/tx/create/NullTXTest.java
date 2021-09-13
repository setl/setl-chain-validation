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
package io.setl.bc.pychain.tx.create;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.common.CommonPy.TxType;

public class NullTXTest {

  @Test
  public void getTxType() {

    NullTX thisClass = new NullTX();

    assertEquals(TxType.DO_NOTHING, thisClass.getTxType());
  }


  @Test
  public void nullUnsigned() {

    int chainID = 20;
    long nonce = 123456;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String poa = "";

    String goodHash = "234a48ac14cafd850d678c4762a4acbbb71644b4f6dbfd71fad0595d0c18fcb1";

    long timestamp = 1502215818L;

    NullTx rVal = NullTX.nullUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    NullTx txCopy = new NullTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    NullTX thisClass = new NullTX(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = NullTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    poa = "not empty";

    rVal = NullTX.nullUnsigned(
        chainID,
        nonce,
        fromPubKey,
        fromAddress,
        poa
    );

    rVal.setTimestamp(timestamp); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

    //

    // MessageFormat.format("At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.", "", new Date(), "");
  }


  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


  @Test
  public void deserializeNullTxFromBaseTransaction() throws IOException {
    String nullTxPayload = "{\"txType\":\"DO_NOTHING\",\"address\":\"string\",\"publicKey\":\"string\"}";
    BaseTransaction tx = OBJECT_MAPPER.readValue(nullTxPayload, BaseTransaction.class);
    assertEquals(TxType.DO_NOTHING, tx.getTxType());
  }


  @Test
  public void deserializeNullTx() throws IOException {
    String nullTxPayload = "{\"address\":\"string\",\"publicKey\":\"string\"}";
    NullTX tx = OBJECT_MAPPER.readValue(nullTxPayload, NullTX.class);
    assertEquals(TxType.DO_NOTHING, tx.getTxType());
    assertEquals("string", tx.getAddress());
    assertEquals("string", tx.getPublicKey());
  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }


}