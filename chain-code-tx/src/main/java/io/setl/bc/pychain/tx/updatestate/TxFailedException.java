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
package io.setl.bc.pychain.tx.updatestate;

import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;

/**
 * @author Simon Greatrix on 20/01/2020.
 */
public class TxFailedException extends Exception {

  private Object datum;


  public TxFailedException(String status) {
    super(status);
    datum = null;
  }


  public TxFailedException(String status, Object datum) {
    super(status);
    this.datum = datum;
  }


  public TxFailedException(String status, Throwable cause) {
    super(status, cause);
  }


  public ReturnTuple getFailure(boolean checkOnly) {
    return new ReturnTuple(checkOnly ? SuccessType.WARNING : SuccessType.FAIL, getMessage(), datum);
  }
}
