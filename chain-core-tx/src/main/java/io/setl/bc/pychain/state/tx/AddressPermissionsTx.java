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
package io.setl.bc.pychain.state.tx;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressPermissionsTx extends AbstractTx implements HasToAddress {

  public static final TxType TXT = TxType.UPDATE_ADDRESS_PERMISSIONS;

  private static final Logger logger = LoggerFactory.getLogger(AddressPermissionsTx.class);


  /**
   * Accept an MPWrappedArray object and return it as a AddressPermissionsTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The returned AddressPermissionsTx object
   */
  public static AddressPermissionsTx decodeTX(MPWrappedArray vat) {

    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = vat.asInt(1);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));

    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);

      return null;
    }

    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);

    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);

    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    String toAddress = vat.asString(10);
    long addressPermissions = vat.asLong(11);
    EnumSet<TxType> addressTransactions = null;
    Object[] txIDs = vat.asObjectArray(12);
    if (txIDs != null) {
      addressTransactions = EnumSet.noneOf(TxType.class);
      for (Object o : txIDs) {
        if (o instanceof Number) {
          addressTransactions.add(TxType.get(((Number) o).intValue()));
        }
      }
    }
    String metadata = vat.asString(13);
    String signature = vat.asString(14);
    int height = vat.asInt(15);

    return new AddressPermissionsTx(
        chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        toAddress,
        addressPermissions,
        addressTransactions,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  /**
   * Accept an Object[] and return a AddressPermissionsTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned AddressPermissionsTx object
   */
  public static AddressPermissionsTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  private final String toAddress;

  private long addressPermissions;

  private Set<TxType> addressTransactions;

  private String metadata;


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public AddressPermissionsTx(AddressPermissionsTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.metadata = toCopy.getMetadata();
    this.toAddress = toCopy.getToAddress();
    this.addressPermissions = toCopy.getAddressPermissions();
    this.addressTransactions = toCopy.getAddressTransactions(); // Method clones the array so no need to do it again.
    this.height = toCopy.getHeight();
  }


  /**
   * Constructor.
   *
   * @param chainId             : Txn blockchain chainID
   * @param int1                : Txn int1
   * @param hash                : Txn hash
   * @param nonce               : Txn nonce
   * @param updated             : Txn updated true/false
   * @param fromPubKey          : Txn from Public Key
   * @param fromAddress         : Txn from address
   * @param toAddress           : Txn to address
   * @param addressPermissions  :
   * @param addressTransactions :
   * @param metadata            : Txn metadata
   * @param signature           : Txn signature
   * @param height              : Txn height
   * @param poa                 : Txn Power of Attorney
   * @param timestamp           : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public AddressPermissionsTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String toAddress,
      long addressPermissions,
      Set<TxType> addressTransactions,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {

    //  ensure the super class in invoked
    //
    super(
        chainId,
        hash,
        nonce,
        updated,
        fromAddress,
        fromPubKey,
        signature,
        poa,
        timestamp
    );

    this.metadata = (metadata == null ? "" : metadata);
    this.addressPermissions = addressPermissions;
    this.addressTransactions = addressTransactions != null ? EnumSet.copyOf(addressTransactions) : null;
    this.height = height;
    this.toAddress = toAddress;
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {
    Set<String> rVal = new TreeSet<>();

    rVal.add(this.fromAddress);
    if ((this.toAddress != null) && (this.toAddress.length() > 0)) {
      rVal.add(this.toAddress);
    }

    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.add(chainId);
    hashList.add(TXT.getId());
    hashList.add(nonce);
    hashList.add(fromPubKey);
    hashList.add(fromAddress);
    hashList.add(timestamp);
    hashList.add(powerOfAttorney);
    hashList.add(fromAddress); // in original code it is toAdress field which is set to fromaddress
    hashList.add(addressPermissions);

    if (addressTransactions != null) {
      hashList.add(addressTransactions.stream().mapToInt(TxType::getId).boxed().collect(Collectors.toList()));
    }

    hashList.add(metadata);

    return hashList;
  }


  @Override
  public Object[] encodeTx() {
    Object[] transactions = null;
    if (addressTransactions != null) {
      transactions = addressTransactions.stream().mapToInt(TxType::getId).boxed().toArray();
    }
    return new Object[]{
        this.chainId, this.int1, TXT.getId(),
        this.hash,
        this.nonce,
        this.updated,
        this.fromPubKey,
        this.fromAddress,
        this.timestamp,
        this.powerOfAttorney,
        this.toAddress,
        this.addressPermissions,
        transactions,
        this.metadata,
        this.signature,
        this.height
    };

  }


  public long getAddressPermissions() {
    return addressPermissions;
  }


  /**
   * Return addressTransactions.
   *
   * @return : int[]
   */
  public Set<TxType> getAddressTransactions() {
    if (addressTransactions == null) {
      return null;
    }

    return Collections.unmodifiableSet(addressTransactions);
  }


  @Override
  public String getMetadata() {
    return metadata;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  public String getToAddress() {
    return toAddress;
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public String toString() {
    return String.format("%s from:%s to:%s", super.toString(), fromAddress, toAddress);
  }
}
