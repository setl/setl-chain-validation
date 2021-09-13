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
package io.setl.bc.pychain.tx.create.contracts.dvp;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEncumbrance;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
@Schema(
    description = "An encumbrance that parties will use to fulfill their payments"
)
public class Encumbrance {

  @Schema(
      description = "If true, use the contract author's administration authority to exercise the encumbrance. If sufficient encumbered assets are available "
          + "to fulfill the contract, then the party with the encumbrance does not have to sign the contract. As per the normal rules for encumbrances, if "
          + "the administrator is also a beneficiary, they may send the asset anywhere. Otherwise, they can only send it to a beneficiary."
  )
  private boolean exercise = false;

  @Schema(
      description = "The name of the encumbrance to use. If left blank, the contract's ID will be used."
  )
  private String reference = "";


  public Encumbrance() {
    // do nothing
  }


  public Encumbrance(DvpEncumbrance encumbrance) {
    reference = encumbrance.encumbranceName;
    exercise = encumbrance.useCreatorEncumbrance;
  }


  public DvpEncumbrance asInternal() {
    return new DvpEncumbrance(exercise, reference);
  }


  @NotNull
  public String getReference() {
    return reference;
  }


  public boolean isExercise() {
    return exercise;
  }


  public void setExercise(boolean exercise) {
    this.exercise = exercise;
  }


  public void setReference(String reference) {
    this.reference = reference != null ? reference : "";
  }

}
