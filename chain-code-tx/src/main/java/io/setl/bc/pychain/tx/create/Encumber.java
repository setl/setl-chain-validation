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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.util.TimeBasedUuid;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nonnull;

@Schema(name = TxExternalNames.ENCUMBER_ASSET, allOf = BaseTransaction.class, type = "object")
@JsonDeserialize
public class Encumber extends BaseEncumber {

  private static final String ADMINISTRATORS = "administrators";

  private static final String BENEFICIARIES = "beneficiaries";

  private static final String REFERENCE = "reference";

  public static final Set<String> DICT_PROPERTIES = Set.of(REFERENCE, "iscumulative", BENEFICIARIES, ADMINISTRATORS);


  /**
   * Convert a map specifying the encumbrance to the correct data types.
   *
   * @param dict the map to clean up.
   */
  public static void cleanMap(Map<String, Object> dict) {
    Object o = dict.get(REFERENCE);
    String s = (o != null) ? String.valueOf(o) : null;
    if (s == null || s.isEmpty()) {
      s = TimeBasedUuid.create().toString();
    }
    dict.put(REFERENCE, s);

    dict.put(BENEFICIARIES, convertToDetail(dict.get(BENEFICIARIES)));
    dict.put(ADMINISTRATORS, convertToDetail(dict.get(ADMINISTRATORS)));
  }


  static Object[] convertToDetail(Object data) {

    if (data == null) {
      return new Object[0];
    }
    if (data instanceof EncumbranceDetail) {
      return new Object[]{((EncumbranceDetail) data).encode()};
    }
    if (data instanceof EncumbranceDetail[]) {
      EncumbranceDetail[] ed = (EncumbranceDetail[]) data;
      Object[] out = new Object[ed.length];
      for (int i = 0; i < out.length; i++) {
        out[i] = ed[i].encode();
      }
      return out;
    }
    if (data instanceof Object[]) {
      Object[] oa = (Object[]) data;
      Object[] tempArray = new Object[oa.length];
      for (int index = 0, l = oa.length; index < l; index++) {
        Object o = oa[index];
        if (o instanceof EncumbranceDetail) {
          tempArray[index] = ((EncumbranceDetail) o).encode();
        } else if (o instanceof Object[]) {
          tempArray[index] = (new EncumbranceDetail((Object[]) o)).encode();
        } else {
          throw new IllegalArgumentException(
              "Encumbrance detail cannot be created from " + ((o == null) ? "null" : o.getClass().getName()));
        }
      }
      return tempArray;
    }

    throw new IllegalArgumentException("Cannot create encumbrance details from " + data.getClass().getName());
  }


  /**
   * encumberUnsigned ; Return new Encumber Tx object.
   *
   * @param chainId        :
   * @param nonce          :
   * @param fromPubKey     :
   * @param fromAddress    :
   * @param nameSpace      :
   * @param classId        :
   * @param subjectAddress :
   * @param amount         :
   * @param txDictData     : Object[].
   * @param protocol       :
   * @param metadata       :
   * @param poa            :
   *
   * @return : Encumber Tx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static EncumberTx encumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      Object[] txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    TreeMap<String, Object> dict = new TreeMap<>();
    for (int i = 0; i < txDictData.length; i += 2) {
      String k = String.valueOf(txDictData[i]);
      if (DICT_PROPERTIES.contains(k)) {
        dict.putIfAbsent(k, txDictData[i + 1]);
      }
    }
    cleanMap(dict);
    return new EncumberTx(
        chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        new MPWrappedMap<>(dict),
        protocol,
        metadata,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );
  }


  /**
   * encumberUnsigned : Return an unsigned Encumber Tx Object.
   *
   * @param chainId        :
   * @param nonce          :
   * @param fromPubKey     :
   * @param fromAddress    :
   * @param nameSpace      :
   * @param classId        :
   * @param subjectAddress :
   * @param amount         :
   * @param txDictData     : MPWrappedMap.
   * @param protocol       :
   * @param metadata       :
   * @param poa            :
   *
   * @return : Encumber Tx Object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static EncumberTx encumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      MPWrappedMap<String, Object> txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    TreeMap<String, Object> dict = new TreeMap<>();
    txDictData.iterate((k, v) -> {
      if (DICT_PROPERTIES.contains(k)) {
        dict.putIfAbsent(k, v);
      }
    });
    cleanMap(dict);

    EncumberTx tx = new EncumberTx(
        chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        new MPWrappedMap<>(dict),
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
   * encumberUnsigned ; Return new Encumber Tx object.
   *
   * @param chainId        :
   * @param nonce          :
   * @param fromPubKey     : Authoring Public Key
   * @param fromAddress    : Authoring Address
   * @param nameSpace      : Namespace of asset to encumber
   * @param classId        : Class ID of asset to encumber
   * @param subjectAddress : For future development. it may become possible to place an encumbrance on a third-party address.
   * @param amount         : Amount of Encumbrance.
   * @param txDictData     : Map.
   *                       {
   *                       reference      : REFERENCE,
   *                       beneficiaries  : [['Address', Start, End], ...]
   *                       administrators : [['Address', Start, End], ...]
   *                       }
   * @param protocol       : Info Only
   * @param metadata       : Info Only
   * @param poa            : POA Data.
   *
   * @return : Encumber Tx object.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static EncumberTx encumberUnsigned(
      int chainId,
      long nonce,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      String subjectAddress,
      Number amount,
      Map<String, Object> txDictData,
      String protocol,
      String metadata,
      String poa

  ) {

    TreeMap<String, Object> dict = new TreeMap<>();
    for (Entry<String, Object> e : txDictData.entrySet()) {
      String k = e.getKey();
      if (DICT_PROPERTIES.contains(k)) {
        dict.putIfAbsent(k, e.getValue());
      }
    }

    cleanMap(dict);

    EncumberTx tx = new EncumberTx(
        chainId,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        new MPWrappedMap<>(dict),
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


  public Encumber() {
    // do nothing
  }


  public Encumber(EncumberTx tx) {
    super(tx);
  }


  @Override
  public EncumberTx create() {
    EncumberTx rVal = new EncumberTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getPublicKey(),
        getAddress(),
        getSubjectAddress(),
        getReference(),
        isCumulative(),
        getNameSpace(),
        getClassId(),
        getAmount(),
        getBeneficiaries().stream().map(Participant::encode).toArray(),
        getAdministrators().stream().map(Participant::encode).toArray(),
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


  @Nonnull
  @Override
  public TxType getTxType() {
    return TxType.ENCUMBER_ASSET;
  }

}
