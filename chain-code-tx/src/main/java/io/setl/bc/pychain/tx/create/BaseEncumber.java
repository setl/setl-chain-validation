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
import static io.setl.bc.pychain.state.tx.helper.TxParameters.IS_CUMULATIVE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;
import static io.setl.common.StringUtils.cleanString;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.common.TypeSafeMap;
import io.setl.util.TimeBasedUuid;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.PublicKey;

@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
public abstract class BaseEncumber extends BaseTransaction {

  @JsonClassDescription("A participant in an encumbrance.")
  public static class Participant {

    @JsonProperty(value = "address", required = true)
    @Schema(description = "Address of participant.", required = true)
    @Address
    private String address;

    @JsonProperty("endTime")
    @Schema(description = "The time at which this participant finishes participating in the encumbrance. Represented as seconds since the epoch. "
        + "Use '0' for unlimited.")
    private long endTime;

    @JsonProperty("startTime")
    @Schema(description = "The time at which this participant starts participating in the encumbrance. Represented as seconds since the epoch. "
        + "Use '0' for unlimited.")
    private long startTime;


    @JsonCreator
    public Participant(
        @JsonProperty(value = "address", required = true)
            String address
    ) {
      this.address = address;
    }


    public Participant() {
      // do nothing
    }


    /**
     * Decode the participant from storage in the block chain.
     *
     * @param encoded the encoded form.
     */
    public Participant(Object[] encoded) {
      address = (String) encoded[0];
      startTime = (Long) encoded[1];
      endTime = (Long) encoded[2];
    }


    /**
     * Convert an internal encumbrance detail into a participant.
     *
     * @param detail the detail
     */
    public Participant(EncumbranceDetail detail) {
      address = detail.address;
      startTime = detail.startTime;
      endTime = detail.endTime;
    }


    public EncumbranceDetail asDetail() {
      return new EncumbranceDetail(address, startTime, endTime);
    }


    Object[] encode() {
      return new Object[]{address, startTime, endTime};
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Participant)) {
        return false;
      }
      Participant that = (Participant) o;
      return endTime == that.endTime && startTime == that.startTime && Objects.equals(address, that.address);
    }


    @Address
    public String getAddress() {
      return address;
    }


    public long getEndTime() {
      return endTime;
    }


    public long getStartTime() {
      return startTime;
    }


    @Override
    public int hashCode() {
      return Objects.hash(address, endTime, startTime);
    }


    public void setAddress(String address) {
      this.address = address;
    }


    public void setEndTime(long endTime) {
      this.endTime = endTime;
    }


    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }

  }

  @JsonProperty("address")
  @JsonAlias("fromaddress")
  @Schema(description = "The address which owns the asset. Can be derived from the associated public key.")
  @NotNull
  @Address
  private String address;

  @JsonProperty("administrators")
  @Schema(description = "The administrators of this encumbrance.")
  private List<@Valid Participant> administrators;

  @JsonProperty("amount")
  @Schema(description = "Amount of asset to encumber.", format = "int64")
  @NotNull
  @Min(0)
  private BigInteger amount;

  @JsonProperty("beneficiaries")
  @Schema(description = "The beneficiaries of this encumbrance.")
  private List<@Valid Participant> beneficiaries;

  @JsonProperty("classId")
  @JsonAlias("instrument")
  @Schema(description = "The identifier of the asset's class.")
  @NotNull
  private String classId;

  @JsonProperty("isCumulative")
  @Schema(description = "Is this encumbrance cumulative?")
  private boolean cumulative = true;

  @JsonProperty("metadata")
  @Schema(description = "Any additional data associated with this transaction.")
  private String metadata = "";

  @JsonProperty("namespace")
  @Schema(description = "The name of the namespace which contains the asset class.")
  @NotNull
  private String nameSpace;

  @JsonProperty("protocol")
  @Schema(description = "Protocol used to implement the asset issue.")
  private String protocol = "";

  @JsonProperty("publicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  @PublicKey
  private String publicKey;

  @JsonProperty("reference")
  @Schema(description = "The reference for this encumbrance.")
  private String reference;

  @Schema(description = "The subject's address (reserved for future use).")
  @JsonProperty("subjectAddress")
  @Address
  private String subjectAddress;

  public BaseEncumber() {
    // do nothing
  }


  /**
   * Recreate the specification of the transaction.
   *
   * @param tx the transaction
   */
  public BaseEncumber(EncumberTx tx) {
    super(tx);
    setAmount(toBigInteger(tx.getAmount()));
    setClassId(tx.getClassId());
    setAddress(tx.getFromAddress());
    setPublicKey(tx.getFromPublicKey());
    setMetadata(tx.getMetadata());
    setNameSpace(tx.getNameSpace());
    setProtocol(tx.getProtocol());
    setSubjectAddress(tx.getSubjectaddress());

    MPWrappedMap<String, Object> txDictData = tx.getEncumbrances();
    if (txDictData != null) {
      txDictData.iterate((key, value) -> {
        switch (key) {
          case REFERENCE:
            setReference(String.valueOf(value));
            break;

          case IS_CUMULATIVE:
            setCumulative(TypeSafeMap.asBoolean(value));
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

    if (reference == null) {
      reference = TimeBasedUuid.create().toString();
    }
  }


  public abstract EncumberTx create();


  public String getAddress() {
    return address;
  }


  public List<Participant> getAdministrators() {
    return administrators;
  }


  public BigInteger getAmount() {
    return amount;
  }


  public List<Participant> getBeneficiaries() {
    return beneficiaries;
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


  public String getReference() {
    return reference;
  }


  public String getSubjectAddress() {
    return subjectAddress;
  }


  public boolean isCumulative() {
    return cumulative;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAdministrators(List<Participant> administrators) {
    this.administrators = administrators;
  }


  public void setAmount(BigInteger amount) {
    this.amount = amount;
  }


  public void setBeneficiaries(List<Participant> beneficiaries) {
    this.beneficiaries = beneficiaries;
  }


  public void setClassId(String classId) {
    this.classId = cleanString(classId);
  }


  public void setCumulative(boolean cumulative) {
    this.cumulative = cumulative;
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


  public void setReference(String reference) {
    this.reference = reference;
  }


  public void setSubjectAddress(String subjectAddress) {
    this.subjectAddress = subjectAddress;
  }


}
