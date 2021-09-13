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

import static io.setl.common.StringUtils.cleanString;

import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.StockSplitTx;
import java.time.Instant;

public class StockSplit {

  /**
   * Return a new, unsigned StockSplitTx.
   *
   * @param chainID            :
   * @param nonce              :
   * @param fromPubKey         :
   * @param fromAddress        :
   * @param nameSpace          :
   * @param classId            :
   * @param referenceStateHash :
   * @param ratio              :
   * @param metadata           :
   * @param poa                :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static StockSplitTx stockSplitUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String referenceStateHash,
      double ratio,
      String metadata,
      String poa
  ) {

    StockSplitTx rVal = new StockSplitTx(
        chainID,
        4,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        cleanString(nameSpace),
        cleanString(classId),
        referenceStateHash,
        ratio,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;

  }


  private StockSplit() {

  }


}
