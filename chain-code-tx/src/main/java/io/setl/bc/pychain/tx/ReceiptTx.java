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
package io.setl.bc.pychain.tx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.Txi;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

/**
 * Acknowledge receipt of a transaction.
 *
 * @author Simon Greatrix on 03/10/2019.
 */
@JsonInclude(Include.NON_NULL)
public class ReceiptTx {

  @Schema(description = "The address which issued the transaction.")
  private String address;

  @Schema(description = "The result of checking the transaction.")
  private SuccessType checkResult;

  @Schema(description = "The status message from checking the transaction.")
  private String checkStatus;

  @Schema(description = "Only for contracts. The unique identifier for the contract that is to affected by the transaction.")
  private String contractAddress;

  @Schema(description = "The transaction's hash.")
  private String hash;

  @Schema(description = "The nonce assigned to the transaction.")
  private long nonce;

  @Schema(description = "The transaction's timestamp (in seconds since the epoch).")
  private long timestamp;

  @Schema(description = "The transaction's type.")
  private TxType txType;


  public ReceiptTx() {
    // do nothing
  }


  /**
   * Set the properties of the receipt from the properties common to all transactions.
   *
   * @param txi the transaction
   */
  public ReceiptTx(Txi txi) {
    address = txi.getNonceAddress();
    hash = txi.getHash();
    nonce = txi.getNonce();
    timestamp = txi.getTimestamp();
    txType = txi.getTxType();
  }


  public String getAddress() {
    return address;
  }


  public SuccessType getCheckResult() {
    return checkResult;
  }


  public String getCheckStatus() {
    return checkStatus;
  }


  public String getContractAddress() {
    return contractAddress;
  }


  public String getHash() {
    return hash;
  }


  public long getNonce() {
    return nonce;
  }


  public long getTimestamp() {
    return timestamp;
  }


  public TxType getTxType() {
    return txType;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setCheckResult(SuccessType checkResult) {
    this.checkResult = checkResult;
  }


  public void setCheckStatus(String checkStatus) {
    this.checkStatus = checkStatus;
  }


  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }


  public void setHash(String hash) {
    this.hash = hash;
  }


  public void setNonce(long nonce) {
    this.nonce = nonce;
  }


  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  public void setTxType(TxType txType) {
    this.txType = txType;
  }


  @Override
  public String toString() {
    return String.format(
        "ReceiptTx(address='%s', checkResult=%s, checkStatus='%s', contractAddress='%s', hash='%s', nonce=%s, timestamp=%s, txType=%s)",
        this.address, this.checkResult, this.checkStatus, this.contractAddress, this.hash, this.nonce, this.timestamp, this.txType
    );
  }

}
