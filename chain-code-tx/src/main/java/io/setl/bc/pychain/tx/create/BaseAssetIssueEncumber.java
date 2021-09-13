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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADMINISTRATORS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.BENEFICIARIES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.AssetIssueEncumberTx;
import io.setl.bc.pychain.tx.create.BaseEncumber.Participant;

public abstract class BaseAssetIssueEncumber extends BaseAssetIssue {

  @JsonProperty("administrators")
  @Schema(description = "The administrators of the associated encumbrance.")
  private List<@Valid Participant> administrators;

  @JsonProperty("beneficiaries")
  @Schema(description = "The beneficiaries of the associated encumbrance.")
  private List<@Valid Participant> beneficiaries;

  @JsonProperty("reference")
  @Schema(description = "The reference for the associated encumbrance.")
  private String reference;

  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public BaseAssetIssueEncumber(AssetIssueEncumberTx tx) {
    super(tx);

    MPWrappedMap<String, Object> txDictData = tx.getEncumbrance();
    if (txDictData != null) {
      txDictData.iterate((key, value) -> {
        switch (key) {
          case REFERENCE:
            setReference(String.valueOf(value));
            break;

          case BENEFICIARIES:
            setBeneficiaries(Stream.of((Object[]) value).map(o -> new Participant((Object[]) o)).collect(Collectors.toList()));
            break;

          case ADMINISTRATORS:
            setAdministrators(Stream.of((Object[]) value).map(o -> new Participant((Object[]) o)).collect(Collectors.toList()));
            break;

          default:
            // Checkstyle !!
            break;
        }
      });
    }

  }


  @JsonCreator
  public BaseAssetIssueEncumber(
      @JsonProperty("amount") @NotNull BigInteger amount,
      @JsonProperty("classId") @NotNull String classId,
      @JsonProperty("fromAddress") @NotNull String fromAddress,
      @JsonProperty("namespace") @NotNull String nameSpace,
      @JsonProperty("toAddress") @NotNull String toAddress
  ) {
    super(amount, classId, fromAddress, nameSpace, toAddress);
  }


  public List<Participant> getAdministrators() {
    return administrators;
  }


  public List<Participant> getBeneficiaries() {
    return beneficiaries;
  }


  public String getReference() {
    return reference;
  }


  public void setAdministrators(List<Participant> administrators) {
    this.administrators = administrators;
  }


  public void setBeneficiaries(List<Participant> beneficiaries) {
    this.beneficiaries = beneficiaries;
  }


  public void setReference(String reference) {
    this.reference = reference;
  }


}
