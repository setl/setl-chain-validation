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

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.AddXChainTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.common.CommonPy.XChainParameters;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@JsonClassDescription("Add a connection to a remote chain to the current network.")
@Schema(name = TxExternalNames.ADD_X_CHAIN, description = "Add a connection to a remote chain to the current network.", allOf = BaseTransaction.class)
@JsonDeserialize
public class AddXChain extends BaseTransaction {

  @JsonClassDescription("A Sig Node in the remote chain.")
  public static class SigNode {

    @Schema(description = "Public key of the Sig Node.")
    @JsonProperty("publicKey")
    @NotNull
    @PublicKey
    private String publicKey;

    @JsonProperty("stake")
    @Schema(description = "Voting stake owned by the Sig Node.")
    private BigInteger stake;


    public SigNode() {
      // do nothing
    }


    /**
     * Create SigNode from its encoded form.
     *
     * @param encoded the encoded form
     */
    public SigNode(Object[] encoded) {
      publicKey = (String) encoded[0];
      stake = toBigInteger((Number) encoded[1]);
    }


    Object[] encode() {
      return new Object[]{publicKey, stake};
    }


    public String getPublicKey() {
      return publicKey;
    }


    public BigInteger getStake() {
      return stake;
    }


    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
    }


    public void setStake(BigInteger stake) {
      this.stake = stake;
    }

  }


  /**
   * addXChainUnsigned().
   * <p>Create Unsigned AddXChainTx</p>
   *
   * @param baseChainID      :
   * @param nonce            :
   * @param fromPubKey       :
   * @param fromAddress      :
   * @param newChainId       :
   * @param newBlockHeight   :
   * @param newChainParams   :
   * @param newChainSignodes :
   * @param poa              :
   *
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public static AddXChainTx addXChainUnsigned(
      int baseChainID,
      long nonce,
      String fromPubKey,
      String fromAddress,

      int newChainId,
      int newBlockHeight,
      long newChainParams,
      List<Object[]> newChainSignodes,
      String poa
  ) {

    AddXChainTx rVal = new AddXChainTx(
        baseChainID,
        "",
        nonce,
        false,
        fromPubKey,
        fromAddress,
        newChainId,
        newBlockHeight,
        newChainParams,
        newChainSignodes,
        "",
        -1,
        poa,
        Instant.now().getEpochSecond()
    );

    rVal.setHash(Hash.computeHash(rVal));

    return rVal;
  }

  @JsonProperty("fromAddress")
  @Schema(description = "The issuing address. Must be the controlling address of the namespace. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String fromAddress;

  @JsonProperty("fromPublicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  @PublicKey
  private String fromPublicKey;

  @JsonProperty("xBlockHeight")
  @Schema(description = "The height of the remote chain.")
  private int newBlockHeight;

  @JsonProperty("xChainId")
  @Schema(description = "The ID of the remote chain.")
  private int newChainId;

  @JsonProperty("parameters")
  @Schema(description = "The parameters governing the relationship with the remote chain.")
  private List<String> newChainParams;

  @Schema(description = "The Signing Nodes for the remote chain.")
  @JsonProperty("signingNodes")
  private List<@Valid SigNode> newChainSignodes = Collections.emptyList();

  public AddXChain() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public AddXChain(AddXChainTx tx) {
    super(tx);
    fromAddress = tx.getFromAddress();
    fromPublicKey = tx.getFromPublicKey();
    newBlockHeight = tx.getNewBlockHeight();
    newChainId = tx.getNewChainId();
    newChainParams = Collections.unmodifiableList(new ArrayList<>(XChainParameters.forCode(tx.getNewChainParams())));
    newChainSignodes = Collections.unmodifiableList(tx.getNewChainSignodes().stream().map(oa -> new SigNode(oa)).collect(Collectors.toList()));
  }


  @Override
  public AddXChainTx create() {
    List<Object[]> sigNodeDefs = newChainSignodes.stream().map(SigNode::encode).collect(Collectors.toList());

    AddXChainTx rVal = new AddXChainTx(
        getChainId(),
        getHash(),
        getNonce(),
        isUpdated(),
        getFromPublicKey(),
        getFromAddress(),
        getNewChainId(),
        getNewBlockHeight(),
        XChainParameters.forNames(newChainParams),
        sigNodeDefs,
        getSignature(),
        getHeight(),
        getPoa(),
        getTimestamp()
    );

    if (Strings.isNullOrEmpty(getHash())) {
      rVal.setHash(Hash.computeHash(rVal));
    }

    return rVal;
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getFromPublicKey() {
    return fromPublicKey;
  }


  public int getNewBlockHeight() {
    return newBlockHeight;
  }


  public int getNewChainId() {
    return newChainId;
  }


  public List<String> getNewChainParams() {
    return newChainParams;
  }


  public List<SigNode> getNewChainSignodes() {
    return newChainSignodes;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public String getNonceAddress() {
    return fromAddress;
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getFromPublicKey();
  }


  @Override
  public TxType getTxType() {
    return TxType.ADD_X_CHAIN;
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }


  public void setFromPublicKey(String fromPublicKey) {
    this.fromPublicKey = fromPublicKey;
  }


  public void setNewBlockHeight(int newBlockHeight) {
    this.newBlockHeight = newBlockHeight;
  }


  public void setNewChainId(int newChainId) {
    this.newChainId = newChainId;
  }


  /**
   * Set parameters.
   *
   * @param newChainParams the new parameters
   */
  public void setNewChainParams(List<String> newChainParams) {
    if (newChainParams != null) {
      this.newChainParams = Collections.unmodifiableList(new ArrayList<>(newChainParams));
    } else {
      this.newChainParams = Collections.emptyList();
    }
  }


  /**
   * Set the Sig Nodes.
   *
   * @param newChainSignodes the new nodes
   */
  public void setNewChainSignodes(List<SigNode> newChainSignodes) {
    if (newChainSignodes != null) {
      this.newChainSignodes = Collections.unmodifiableList(new ArrayList<>(newChainSignodes));
    } else {
      this.newChainSignodes = Collections.emptyList();
    }
  }


  @Override
  public void setNoncePublicKey(String key) {
    setFromPublicKey(key);
  }

}
