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
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.PoaEncumberTx;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotNull;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(name = TxExternalNames.POA_ENCUMBER_ASSET, allOf = BaseTransaction.class)
@JsonDeserialize
public class PoaEncumber extends BaseEncumber {

  /**
   * encumberUnsigned : Return an unsigned PoaEncumber Tx Object.
   *
   * @param chainId        :
   * @param nonce          :
   * @param fromPubKey     :
   * @param fromAddress    :
   * @param poaAddress     :
   * @param poaReference   :
   * @param nameSpace      :
   * @param classId        :
   * @param subjectAddress :
   * @param amount         :
   * @param txDictData     : MPWrappedMap.
   * @param protocol       :
   * @param metadata       :
   * @param poa            :
   *
   * @return : PoaEncumber Tx Object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaEncumberTx encumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      MPWrappedMap<String, Object> txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    PoaEncumberTx tx = new PoaEncumberTx(chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        PoaEncumberTx.txDictDataCopy(txDictData),
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    tx.setHash(Hash.computeHash(tx));
    return tx;
  }


  /**
   * encumberUnsigned ; Return new PoaEncumber Tx object.
   *
   * @param chainId        :
   * @param nonce          :
   * @param fromPubKey     : Authoring Public Key
   * @param fromAddress    : Authoring Address
   * @param poaAddress     :
   * @param poaReference   :
   * @param nameSpace      : Namespace of asset to encumber
   * @param classId        : Class ID of asset to encumber
   * @param subjectAddress : For future development. it may become possible to place an encumbrance on a third-party address.
   * @param amount         : Amount of Encumbrance.
   * @param txDictData     : Map.
   *                       {
   *                       reference      : "reference",
   *                       beneficiaries  : [['Address', Start, End], ...]
   *                       administrators : [['Address', Start, End], ...]
   *                       }
   * @param protocol       : Info Only
   * @param metadata       : Info Only
   * @param poa            : POA Data.
   *
   * @return : PoaEncumber Tx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaEncumberTx encumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      Map<String, Object> txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    PoaEncumberTx tx = new PoaEncumberTx(chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        PoaEncumberTx.txDictDataCopy(new MPWrappedMap<>(txDictData)),
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );
    tx.setHash(Hash.computeHash(tx));
    return tx;
  }


  /**
   * encumberUnsigned ; Return new PoaEncumber Tx object.
   *
   * @param chainId        :
   * @param nonce          :
   * @param fromPubKey     :
   * @param fromAddress    :
   * @param poaAddress     :
   * @param poaReference   :
   * @param nameSpace      :
   * @param classId        :
   * @param subjectAddress :
   * @param amount         :
   * @param txDictData     : Object[].
   * @param protocol       :
   * @param metadata       :
   * @param poa            :
   *
   * @return : PoaEncumber Tx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static PoaEncumberTx encumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      Object[] txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    PoaEncumberTx tx = new PoaEncumberTx(chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        PoaEncumberTx.txDictDataCopy(new MPWrappedMap<>(txDictData)),
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );
    tx.setHash(Hash.computeHash(tx));
    return tx;
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


  public PoaEncumber() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public PoaEncumber(PoaEncumberTx tx) {
    super(tx);
    setPoaAddress(tx.getPoaAddress());
    setPoaReference(tx.getPoaReference());
  }


  @Override
  public PoaEncumberTx create() {
    PoaEncumberTx rVal = new PoaEncumberTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getPoaAddress(),
        getPoaReference(),
        getSubjectAddress(),
        getReference(),
        isCumulative(),
        getNameSpace(),
        getClassId(),
        getAmount(),
        getBeneficiaries().stream().map(o -> o.encode()).toArray(),
        getAdministrators().stream().map(o -> o.encode()).toArray(),
        getProtocol(),
        getMetadata(),
        getSignature(),
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
    return TxType.POA_ENCUMBER_ASSET;
  }


  public void setPoaAddress(String poaAddress) {
    this.poaAddress = poaAddress;
  }


  public void setPoaReference(String poaReference) {
    this.poaReference = poaReference;
  }
}
