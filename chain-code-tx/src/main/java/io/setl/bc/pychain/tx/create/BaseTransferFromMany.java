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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.TransferFromManyTx;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 04/06/2018.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
public abstract class BaseTransferFromMany extends BaseTransaction {

  public static class Transfer {

    @JsonProperty("address")
    @Schema(description = "The address to transfer from or to.")
    @NotNull
    @Address
    private String address;

    @JsonProperty("amount")
    @Schema(description = "The amount to transfer from or to this address.", format = "int64")
    @Min(0)
    private BigInteger amount;


    public Transfer() {
      // do nothing
    }


    public Transfer(Object[] encoded) {
      address = (String) encoded[0];
      amount = toBigInteger((Number) encoded[1]);
    }


    public Object[] encode() {
      return new Object[]{address, amount};
    }


    public String getAddress() {
      return address;
    }


    public BigInteger getAmount() {
      return amount;
    }


    public void setAddress(String address) {
      this.address = address;
    }


    public void setAmount(BigInteger amount) {
      this.amount = amount;
    }

  }

  @JsonProperty("address")
  @Schema(description = "The controlling address. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("amount")
  @Schema(description = "Amount of asset to transfer.", format = "int64")
  @Min(0)
  private BigInteger amount = null;

  @JsonProperty("classId")
  @JsonAlias("instrument")
  @Schema(description = "The identifier of the asset's class.")
  @NotNull
  private String classId;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("namespace")
  @Schema(description = "The name of the namespace which contains the asset class.")
  @NotNull
  private String nameSpace;

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to implement the transfer.")
  private String protocol = "";

  @JsonAlias("pubkey")
  @JsonProperty("publicKey")
  @Schema(description = "Public key of the controlling address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("transfers")
  @Schema(description = "The individual transfers.")
  @NotNull
  private List<@Valid Transfer> transfers;

  public BaseTransferFromMany() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public BaseTransferFromMany(TransferFromManyTx tx) {
    super(tx);
    setAddress(tx.getFromAddress());
    setAmount(toBigInteger(tx.getAmount()));
    setClassId(tx.getClassId());
    setMetadata(tx.getMetadata());
    setNameSpace(tx.getNameSpace());
    setProtocol(tx.getProtocol());
    setPublicKey(tx.getFromPublicKey());
    setTransfers(Arrays.stream(tx.getSubjectAddresses()).map(Transfer::new).collect(Collectors.toList()));
  }


  protected void fixAmount() {
    if (amount == null) {
      BigInteger total = BigInteger.ZERO;
      for (Transfer t : transfers) {
        total = total.add(t.getAmount());
      }
      amount = total;
    }
  }


  public String getAddress() {
    return address;
  }


  public BigInteger getAmount() {
    fixAmount();
    return amount;
  }


  public String getClassId() {
    return classId;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNameSpace() {
    return nameSpace;
  }


  @Nonnull
  @JsonIgnore
  @Hidden
  @Override
  public String getNonceAddress() {
    return address;
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getPublicKey();
  }


  public String getProtocol() {
    return protocol;
  }


  public String getPublicKey() {
    return publicKey;
  }


  public List<Transfer> getTransfers() {
    return transfers;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setClassId(String classId) {
    this.classId = cleanString(classId);
  }


  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }


  public void setNameSpace(String nameSpace) {
    this.nameSpace = cleanString(nameSpace);
  }


  @Override
  public void setNoncePublicKey(String key) {
    setPublicKey(key);
  }


  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }


  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }


  public void setTransfers(List<Transfer> transfers) {
    this.transfers = transfers;
  }

}
