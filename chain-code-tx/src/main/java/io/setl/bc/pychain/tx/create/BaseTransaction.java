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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.EnumMap;
import javax.annotation.Nonnull;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.Views.Input;
import io.setl.bc.pychain.tx.Views.Output;
import io.setl.bc.pychain.tx.Views.Submission;
import io.setl.bc.pychain.tx.deserialization.BaseTransactionDeserializer;
import io.setl.bc.pychain.tx.verifier.TxVerifier;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxExternalNames;
import io.setl.common.CommonPy.TxType;
import io.setl.validation.annotations.Address;
import io.setl.validation.annotations.ValidBase64;
import io.setl.validation.annotations.PublicKey;

/**
 * @author Simon Greatrix on 29/05/2018.
 */
@SuppressWarnings("squid:S2637") // "@NonNull" values should not be set to null. Default constructor is required for json
@Schema(
    type = "object",
    discriminatorProperty = "txType",
    discriminatorMapping = {
        @DiscriminatorMapping(schema = AddressDelete.class, value = TxExternalNames.DELETE_ADDRESS),
        @DiscriminatorMapping(schema = AddressPermissions.class, value = TxExternalNames.UPDATE_ADDRESS_PERMISSIONS),
        @DiscriminatorMapping(schema = AddXChain.class, value = TxExternalNames.ADD_X_CHAIN),
        @DiscriminatorMapping(schema = AssetClassDelete.class, value = TxExternalNames.DELETE_ASSET_CLASS),
        @DiscriminatorMapping(schema = AssetClassRegister.class, value = TxExternalNames.REGISTER_ASSET_CLASS),
        @DiscriminatorMapping(schema = AssetClassUpdate.class, value = TxExternalNames.UPDATE_ASSET_CLASS),
        @DiscriminatorMapping(schema = AssetTransfer.class, value = TxExternalNames.TRANSFER_ASSET),
        @DiscriminatorMapping(schema = AssetIssue.class, value = TxExternalNames.ISSUE_ASSET),
        @DiscriminatorMapping(schema = AssetIssueAndEncumber.class, value = TxExternalNames.ISSUE_AND_ENCUMBER_ASSET),
        @DiscriminatorMapping(schema = AssetTransfer.class, value = TxExternalNames.TRANSFER_ASSET),
        @DiscriminatorMapping(schema = AssetTransferXChain.class, value = TxExternalNames.TRANSFER_ASSET_X_CHAIN),
        @DiscriminatorMapping(schema = Bond.class, value = TxExternalNames.GRANT_VOTING_POWER),
        @DiscriminatorMapping(schema = CommitToContract.class, value = TxExternalNames.COMMIT_TO_CONTRACT),
        @DiscriminatorMapping(schema = DeleteXChain.class, value = TxExternalNames.REMOVE_X_CHAIN),
        @DiscriminatorMapping(schema = Encumber.class, value = TxExternalNames.ENCUMBER_ASSET),
        @DiscriminatorMapping(schema = ExerciseEncumbrance.class, value = TxExternalNames.EXERCISE_ENCUMBRANCE),
        @DiscriminatorMapping(schema = IssuerTransfer.class, value = TxExternalNames.TRANSFER_ASSET_AS_ISSUER),
        @DiscriminatorMapping(schema = LockAsset.class, value = TxExternalNames.LOCK_ASSET),
        @DiscriminatorMapping(schema = LockHolding.class, value = TxExternalNames.LOCK_ASSET_HOLDING),
        @DiscriminatorMapping(schema = Memo.class, value = TxExternalNames.CREATE_MEMO),
        @DiscriminatorMapping(schema = NamespaceDelete.class, value = TxExternalNames.DELETE_NAMESPACE),
        @DiscriminatorMapping(schema = NamespaceRegister.class, value = TxExternalNames.REGISTER_NAMESPACE),
        @DiscriminatorMapping(schema = NamespaceTransfer.class, value = TxExternalNames.TRANSFER_NAMESPACE),
        @DiscriminatorMapping(schema = NewContract.class, value = TxExternalNames.NEW_CONTRACT),
        @DiscriminatorMapping(schema = NullTX.class, value = TxExternalNames.DO_NOTHING),
        @DiscriminatorMapping(schema = PoaAdd.class, value = TxExternalNames.GRANT_POA),
        @DiscriminatorMapping(schema = PoaDelete.class, value = TxExternalNames.REVOKE_POA),
        @DiscriminatorMapping(schema = RegisterAddress.class, value = TxExternalNames.REGISTER_ADDRESS),
        // "Stock Split" - this transaction is deliberately omitted as we do not want any transactions that traverse state to appear in our documentation.
        @DiscriminatorMapping(schema = TransferFromMany.class, value = TxExternalNames.TRANSFER_ASSET_FROM_MANY),
        @DiscriminatorMapping(schema = TransferToMany.class, value = TxExternalNames.TRANSFER_ASSET_TO_MANY),
        @DiscriminatorMapping(schema = UnBond.class, value = TxExternalNames.REVOKE_VOTING_POWER),
        @DiscriminatorMapping(schema = UnEncumber.class, value = TxExternalNames.UNENCUMBER_ASSET),
        @DiscriminatorMapping(schema = UnLockAsset.class, value = TxExternalNames.UNLOCK_ASSET),
        @DiscriminatorMapping(schema = UnlockHolding.class, value = TxExternalNames.UNLOCK_ASSET_HOLDING),

        // PoA Transactions
        @DiscriminatorMapping(schema = PoaAddressDelete.class, value = TxExternalNames.POA_DELETE_ADDRESS),
        @DiscriminatorMapping(schema = PoaAssetClassDelete.class, value = TxExternalNames.POA_DELETE_ASSET_CLASS),
        @DiscriminatorMapping(schema = PoaAssetClassRegister.class, value = TxExternalNames.POA_REGISTER_ASSET_CLASS),
        @DiscriminatorMapping(schema = PoaAssetIssue.class, value = TxExternalNames.POA_ISSUE_ASSET),
        @DiscriminatorMapping(schema = PoaAssetIssueAndEncumber.class, value = TxExternalNames.POA_ISSUE_AND_ENCUMBER_ASSET),
        @DiscriminatorMapping(schema = PoaAssetTransferXChain.class, value = TxExternalNames.POA_TRANSFER_ASSET_X_CHAIN),
        @DiscriminatorMapping(schema = PoaCommitToContract.class, value = TxExternalNames.POA_COMMIT_TO_CONTRACT),
        @DiscriminatorMapping(schema = PoaEncumber.class, value = TxExternalNames.POA_ENCUMBER_ASSET),
        @DiscriminatorMapping(schema = PoaExerciseEncumbrance.class, value = TxExternalNames.POA_EXERCISE_ENCUMBRANCE),
        @DiscriminatorMapping(schema = PoaIssuerTransfer.class, value = TxExternalNames.POA_TRANSFER_ASSET_AS_ISSUER),
        @DiscriminatorMapping(schema = PoaLockAsset.class, value = TxExternalNames.POA_LOCK_ASSET),
        @DiscriminatorMapping(schema = PoaLockHolding.class, value = TxExternalNames.POA_LOCK_ASSET_HOLDING),
        @DiscriminatorMapping(schema = PoaNamespaceDelete.class, value = TxExternalNames.POA_DELETE_NAMESPACE),
        @DiscriminatorMapping(schema = PoaNamespaceRegister.class, value = TxExternalNames.POA_REGISTER_NAMESPACE),
        @DiscriminatorMapping(schema = PoaNamespaceTransfer.class, value = TxExternalNames.POA_TRANSFER_NAMESPACE),
        @DiscriminatorMapping(schema = PoaNewContract.class, value = TxExternalNames.POA_NEW_CONTRACT),
        @DiscriminatorMapping(schema = PoaTransferFromMany.class, value = TxExternalNames.POA_TRANSFER_ASSET_FROM_MANY),
        @DiscriminatorMapping(schema = PoaTransferToMany.class, value = TxExternalNames.POA_TRANSFER_ASSET_TO_MANY),
        @DiscriminatorMapping(schema = PoaUnEncumber.class, value = TxExternalNames.POA_UNENCUMBER_ASSET),
        @DiscriminatorMapping(schema = PoaUnLockAsset.class, value = TxExternalNames.POA_UNLOCK_ASSET),
        @DiscriminatorMapping(schema = PoaUnlockHolding.class, value = TxExternalNames.POA_UNLOCK_ASSET_HOLDING)
    },
    subTypes = {
        AddressDelete.class,
        AddressPermissions.class,
        AddXChain.class,
        AssetClassDelete.class,
        AssetClassRegister.class,
        AssetClassUpdate.class,
        AssetTransfer.class,
        AssetIssue.class,
        AssetIssueAndEncumber.class,
        AssetTransfer.class,
        AssetTransferXChain.class,
        Bond.class,
        CommitToContract.class,
        DeleteXChain.class,
        Encumber.class,
        ExerciseEncumbrance.class,
        IssuerTransfer.class,
        LockAsset.class,
        LockHolding.class,
        Memo.class,
        NamespaceDelete.class,
        NamespaceRegister.class,
        NamespaceTransfer.class,
        NewContract.class,
        NullTX.class,
        PoaAdd.class,
        PoaDelete.class,
        RegisterAddress.class,
        TransferFromMany.class,
        TransferToMany.class,
        UnBond.class,
        UnEncumber.class,
        UnLockAsset.class,
        UnlockHolding.class,
        PoaAddressDelete.class,
        PoaAssetClassDelete.class,
        PoaAssetClassRegister.class,
        PoaAssetIssue.class,
        PoaAssetIssueAndEncumber.class,
        PoaAssetTransferXChain.class,
        PoaCommitToContract.class,
        PoaEncumber.class,
        PoaExerciseEncumbrance.class,
        PoaIssuerTransfer.class,
        PoaLockAsset.class,
        PoaLockHolding.class,
        PoaNamespaceDelete.class,
        PoaNamespaceRegister.class,
        PoaNamespaceTransfer.class,
        PoaNewContract.class,
        PoaTransferFromMany.class,
        PoaTransferToMany.class,
        PoaUnEncumber.class,
        PoaUnLockAsset.class,
        PoaUnlockHolding.class,
    }
)
@JsonDeserialize(using = BaseTransactionDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeIdResolver(BaseTransactionTypeResolver.class)
public abstract class BaseTransaction {

  protected static final String UNSET_VALUE = "UNSET-REQUIRED-VALUE";



  /**
   * We cannot statically initialise the representation classes within BaseTransaction without creating a circular dependency. The sub-classes of
   * BaseTransaction cannot initialise until BaseTransaction itself, is fully initialised and hence it cannot access its own sub-classes during static
   * initialisation. One has to either defer initialisation, or move it to a second class like this.
   */
  static class RepresentationHolder {

    static final EnumMap<TxType, Constructor<? extends BaseTransaction>> noArgConstructors = new EnumMap<>(TxType.class);

    static final EnumMap<TxType, Constructor<? extends BaseTransaction>> txConstructors = new EnumMap<>(TxType.class);

    static {
      for (Class<? extends BaseTransaction> cl : BaseTransactionTypeResolver.ALL_SUB_TYPES) {
        try {
          Method createMethod = cl.getMethod("create");
          Class<?> txiType = createMethod.getReturnType();
          Constructor<? extends BaseTransaction> con = cl.getConstructor(txiType);
          TxType txType = cl.getConstructor().newInstance().getTxType();
          txConstructors.put(txType, con);
          noArgConstructors.put(txType, cl.getConstructor());
        } catch (NoSuchMethodException e) {
          throw new InternalError("Missing required constructor for type " + cl, e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
          throw new InternalError("Class " + cl + " could not be constructed", e);
        }
      }
    }

    private RepresentationHolder() {
      // do nothing
    }

  }


  /**
   * Get the representation for a transaction type.
   *
   * @param txType the transaction type
   *
   * @return a new representation instance
   */
  public static BaseTransaction getRepresentation(TxType txType) {
    try {
      return RepresentationHolder.noArgConstructors.get(txType).newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new InternalError("Failed to create instance", e);
    }
  }


  /**
   * Get the representation of a given transaction.
   *
   * @param tx the transaction
   *
   * @return a new representation instance
   */
  public static BaseTransaction getRepresentation(Txi tx) {
    try {
      return RepresentationHolder.txConstructors.get(tx.getTxType()).newInstance(tx);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new InternalError("Failed to create instance", e);
    }
  }


  /**
   * Convert a number to a BigInteger.
   *
   * @param number the number
   *
   * @return the BigInteger
   */
  @NotNull
  protected static BigInteger toBigInteger(Number number) {
    if (number instanceof BigInteger) {
      return (BigInteger) number;
    }
    if (number instanceof Balance) {
      return ((Balance) number).bigintValue();
    }
    if (number instanceof Long || number instanceof Integer) {
      return BigInteger.valueOf(number.longValue());
    }
    return new BigDecimal(number.toString()).setScale(0, RoundingMode.HALF_UP).toBigInteger();
  }


  public static JSONObject toJSON(Txi txi) {
    return getRepresentation(txi).toJSON((Class<?>) null);
  }


  @JsonProperty("chainId")
  @Schema(description = "ID of the block chain.")
  @JsonView(Output.class)
  private int chainId;

  @JsonProperty("hash")
  @Schema(description = "Hash of the transaction.")
  @JsonView(Output.class)
  private String hash;

  @JsonProperty("height")
  @Schema(description = "Location in the block chain.")
  @JsonView(Output.class)
  private int height;

  @JsonProperty("noCheck")
  @Schema(description = "Whether the transaction should be validated against last good state prior to being submitted to the chain.")
  @JsonView(Input.class)
  @Deprecated(since = "5.0.1")
  private boolean noCheck;

  @JsonProperty("nonce")
  @Schema(description = "The 'from' address's nonce for this transaction. Normally derived from the wallet.")
  @Min(-1)
  @JsonView({Output.class, Submission.class})
  private long nonce = -1;

  @JsonProperty("poa")
  @Schema(description = "Any Power of Attorney data associated with this transaction.")
  private String poa = "";

  @JsonProperty("signature")
  @Schema(description = "The signature from the 'from' address. Normally derived from the wallet.")
  @JsonView(Output.class)
  private String signature;

  @JsonProperty("timestamp")
  @Schema(description = "Timestamp for this transaction, with second precision. Set when submitted to consensus.")
  @NotNull
  @JsonView(Output.class)
  private long timestamp = -1;

  @Schema(description = "Whether state has been updated with this transaction or not.")
  @JsonView(Output.class)
  private boolean updated;


  protected BaseTransaction() {
    // do nothing
  }


  protected BaseTransaction(AbstractTx txi) {
    chainId = txi.getChainId();
    hash = txi.getHash();
    height = txi.getHeight();
    nonce = txi.getNonce();
    poa = txi.getPowerOfAttorney();
    signature = txi.getSignature();
    timestamp = txi.getTimestamp();
    updated = txi.isGood();
  }


  /**
   * Create the transaction object given this specification.
   *
   * @return the transaction object
   */
  public abstract Txi create();


  @JsonView(Output.class)
  public int getChainId() {
    return chainId;
  }


  @JsonView(Output.class)
  public String getHash() {
    return hash;
  }


  @JsonView(Output.class)
  public int getHeight() {
    return height;
  }


  @JsonView(Output.class)
  public long getNonce() {
    return nonce;
  }


  /**
   * The address associated with the nonce. Normally this will be either the "address" or the "from address" property depending on the transaction type.
   *
   * @return the nonce address
   */
  @Nonnull
  @Address
  @JsonIgnore
  @Hidden
  public abstract String getNonceAddress();

  /**
   * Get the public key associated with the nonce. Normally this will be either the "public key" or the "from public key" property depending on the transaction
   * type.
   *
   * @return the public key
   */
  @JsonIgnore
  @Hidden
  @PublicKey
  public abstract String getNoncePublicKey();


  public String getPoa() {
    return poa;
  }


  @ValidBase64
  public String getSignature() {
    return signature;
  }


  public long getTimestamp() {
    return timestamp;
  }


  @NotNull
  @JsonIgnore
  @Hidden
  public abstract TxType getTxType();


  @NotNull
  @JsonProperty("txType")
  @Schema(name = "txType", required = true)
  public String getTxTypeName() {
    return getTxType().getExternalName();
  }


  public boolean isNoCheck() {
    return noCheck;
  }


  /**
   * Test if the nonce address was derived from the nonce public key.
   *
   * @return False if the nonce public key is set but does not correspond to the nonce address. True otherwise.
   */
  @JsonIgnore
  @Hidden
  @AssertTrue(message = "io.setl.bc.pychain.tx.create.BaseTransaction.isNonceIdentityValid")
  public boolean isNonceIdentityValid() {
    String nonceAddress = getNonceAddress();
    String noncePublicKey = getNoncePublicKey();
    return noncePublicKey == null || AddressUtil.verify(nonceAddress, noncePublicKey, AddressUtil.getAddressType(nonceAddress));
  }


  public boolean isUpdated() {
    return updated;
  }


  @JsonIgnore
  @Hidden
  @AssertTrue(message = "{io.setl.bc.pychain.tx.create.BaseTransaction.isSignatureValid}")
  public boolean isValidSignature(@Autowired TxVerifier txVerifier) {
    if (signature == null) {
      // This is OK
      return true;
    }
    return txVerifier.verifySignature(create());
  }


  public void setChainId(int chainId) {
    this.chainId = chainId;
  }


  public void setNoCheck(boolean noCheck) {
    this.noCheck = noCheck;
  }


  public void setNonce(long nonce) {
    this.nonce = nonce;
  }


  /**
   * Set public key associated with the nonce. Normally this will be either the "public key" or the "from public key" property depending on the transaction
   * type.
   *
   * @param key the public key
   */
  public abstract void setNoncePublicKey(String key) throws InvalidTransactionException;


  public void setPoa(String poa) {
    this.poa = poa;
  }


  public void setSignature(String signature) {
    this.signature = signature;
  }


  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  /**
   * Create a JSON representation of this using the provided Jackson view marker class.
   *
   * @param view the marker class (can be null)
   *
   * @return the JSON representation.
   */
  public JSONObject toJSON(Class<?> view) {
    ObjectMapper mapper = new ObjectMapper();
    if (view != null) {
      mapper.setConfig(mapper.getSerializationConfig().withView(view));
    }
    return mapper.convertValue(this, JSONObject.class);
  }


  public JSONObject toJSON() {
    return toJSON((Class<?>) null);
  }

}
