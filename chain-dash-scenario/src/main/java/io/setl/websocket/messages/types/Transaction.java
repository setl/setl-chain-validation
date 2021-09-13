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
package io.setl.websocket.messages.types;

public class Transaction {

  private double amount;

  private String classID;

  private String fromAddress;

  private String hash;

  private int height;

  private String metadata;

  private String namespace;

  private long nonce;

  private String protocol;

  private String toAddress;

  private int type;

  private double unusedValue = 0.0;


  /**
   * New instance.
   *
   * @param type        transaction's type
   * @param fromAddress the submitting address
   * @param toAddress   the destination address
   * @param namespace   the namespace
   * @param classID     the asset ID
   * @param amount      the amount
   * @param hash        this transaction's hash
   * @param height      the chain height
   * @param protocol    the protocol used by the transaction
   * @param nonce       the nonce
   * @param metadata    any associated meta data
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public Transaction(int type, String fromAddress, String toAddress, String namespace, String classID, double amount, String hash,
      int height, String protocol, long nonce, String metadata
  ) {
    this.type = type;
    this.fromAddress = fromAddress;
    this.toAddress = toAddress;
    this.namespace = namespace;
    this.classID = classID;
    this.amount = amount;
    this.hash = hash;
    this.height = height;
    this.protocol = (protocol == null ? "" : protocol);
    this.nonce = nonce;
    this.metadata = (metadata == null ? "" : metadata);
  }


  public double getAmount() {
    return amount;
  }


  public String getClassID() {
    return classID;
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getHash() {
    return hash;
  }


  public int getHeight() {
    return height;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNamespace() {
    return namespace;
  }


  public long getNonce() {
    return nonce;
  }


  public String getProtocol() {
    return protocol;
  }


  public String getToAddress() {
    return toAddress;
  }


  public int getType() {
    return type;
  }


  public double getUnusedValue() {
    return unusedValue;
  }


  public void setAmount(double amount) {
    this.amount = amount;
  }


  public void setClassID(String classID) {
    this.classID = classID;
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }


  public void setHash(String hash) {
    this.hash = hash;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setMetadata(String metadata) {
    this.metadata = (metadata == null ? "" : metadata);
  }


  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }


  public void setNonce(long nonce) {
    this.nonce = nonce;
  }


  public void setProtocol(String protocol) {
    this.protocol = (protocol == null ? "" : protocol);
  }


  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }


  public void setType(int type) {
    this.type = type;
  }


  public void setUnusedValue(double unusedValue) {
    this.unusedValue = unusedValue;
  }
}
