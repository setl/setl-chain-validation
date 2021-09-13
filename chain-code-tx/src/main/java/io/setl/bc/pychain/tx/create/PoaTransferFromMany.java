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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaTransferFromManyTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_TRANSFER_ASSET_FROM_MANY, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaTransferFromMany extends BaseTransferFromMany {

  /**
   * poaTransferFromManyUnsigned().
   * <p>Return new, unsigned PoaTransferFromManyTx.</p>
   *
   * @param chainID          : int,    Blockchain ID
   * @param nonce            : int,    TX Nonce (Unique, sequential)
   * @param fromPubKey       : String, 'From' Address Public Key, Relates to From Address and TX Signature.
   * @param fromAddress      : String, Address from which Asset will be taken
   * @param poaAddress       :
   * @param poaReference     :
   * @param nameSpace        : String, Asset namespace
   * @param classId          : String, Asset Class
   * @param subjectAddresses : Obj[],  [[Address, Amount], ...]  Specification of payment addresses.
   * @param amount           : long,   CheckSum. Must equal Sum of amounts specified in subjectAddresses
   * @param protocol         : String, Info Only.
   * @param metadata         : String, Info Only.
   * @param poa              :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaTransferFromManyTx poaTransferFromManyUnsigned(
      int chainID,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      Object[] subjectAddresses,
      Number amount,
      String protocol,
      String metadata,
      String poa
  ) {

    PoaTransferFromManyTx rVal = new PoaTransferFromManyTx(
        chainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        classId,
        subjectAddresses,
        amount,
        "",
        protocol,
        metadata,
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }


  @JsonProperty("poaAddress")
  @Schema(description = "Attorney's address")
  @NotNull
  @Address
  private String poaAddress;

  @JsonProperty("poaReference")
  @Schema(description = "Attorney's reference for this transaction.")
  @NotNull
  private String poaReference;


  public PoaTransferFromMany() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaTransferFromMany(PoaTransferFromManyTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
  }


  @Override
  public PoaTransferFromManyTx create() {
    fixAmount();

    PoaTransferFromManyTx rVal = new PoaTransferFromManyTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getPoaAddress(),
        getPoaReference(),
        getNameSpace(),
        getClassId(),
        getTransfers().stream().map(t -> t.encode()).toArray(),
        getAmount(),
        getSignature(),
        getProtocol(),
        getMetadata(),
        getHeight(),
        getPoa(),
        getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public String getPoaAddress() {
    return poaAddress;
  }


  public String getPoaReference() {
    return poaReference;
  }


  @Override
  public TxType getTxType() {
    return TxType.POA_TRANSFER_ASSET_FROM_MANY;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }
}
