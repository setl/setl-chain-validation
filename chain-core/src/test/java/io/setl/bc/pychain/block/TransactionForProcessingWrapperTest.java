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
package io.setl.bc.pychain.block;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.pychain.state.tx.Txi;

public class TransactionForProcessingWrapperTest {
  
  @Test
  public void generalTest() throws Exception {
  
    final String  hash        = "HASH";
    final String  fromPubKey  = "fromPubKey";
    final String  fromAddress = "fromAddress";
    final String  signature   = "signature";
    final String  poa         = "poa";
  
  
    NullTx tx = new NullTx( 1,
        2,              //  int1
        hash,
        3,
        true,
        fromPubKey,
        fromAddress,
        signature,
        4,              //  NB. set by class to -1
        poa,
        5
    );
  
    TransactionForProcessing thisTfpw = new TransactionForProcessing(tx);
    
    assertTrue(thisTfpw.getWrapped().equals(tx));
    assertTrue(thisTfpw.getFromAddress().equals(tx.getFromAddress()));
    assertTrue(thisTfpw.getHash().equals(tx.getHash()));
    assertTrue(thisTfpw.getNonce() == tx.getNonce());
    assertTrue(!thisTfpw.isReplay());
    assertTrue(!thisTfpw.isFuture());
    
    thisTfpw.rejectNonceInFuture();
    thisTfpw.rejectNonceInPast();
    assertTrue(thisTfpw.isReplay());
    assertTrue(thisTfpw.isFuture());
  
    TransactionForProcessing[] thisTfpArray =  TransactionForProcessing.wrap(new Txi[] {tx});
    
    assertTrue(thisTfpArray.length == 1);
    assertTrue(thisTfpArray[0].getWrapped().equals(tx));
    assertTrue(!thisTfpArray[0].isReplay());
    assertTrue(!thisTfpArray[0].isFuture());
  
  }
  
}