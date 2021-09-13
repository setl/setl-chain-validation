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
package io.setl.bc.pychain.tx.create.contracts.dvpcommit;

import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitReceive;
import io.setl.validation.annotations.Address;

/**
 * @author Simon Greatrix on 30/08/2020.
 */
@Schema(description = "Commitment to an inward payment")
public class PayIn {

  @Schema(description = "The address that will receive the payment.")
  private String address;

  @Schema(description = "The index of the inward payment in the party's inward payments list")
  private int index;


  public PayIn() {
    // do nothing
  }


  public PayIn(DvpCommitReceive receive) {
    address = receive.address;
    index = (receive.index != null) ? receive.index.intValue() : 0;
  }


  public DvpCommitReceive asInternal() {
    return new DvpCommitReceive((long) index, address);
  }


  @Address
  public String getAddress() {
    return address;
  }


  public int getIndex() {
    return index;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setIndex(int index) {
    this.index = index;
  }

}
