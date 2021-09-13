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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"asset", "namespace", "amount", "address"})
public class Asset {

  private String address;

  private Number amount;

  private String assetName;

  private String namespace;


  /**
   * New instance.
   *
   * @param assetName this asset's name
   * @param namespace the namespace containing this asset
   * @param amount    the amount of this asset
   * @param address   the address which owns this asset
   */
  public Asset(String assetName, String namespace, Number amount, String address) {
    this.assetName = assetName;
    this.namespace = namespace;
    this.amount = amount;
    this.address = address;
  }


  @JsonProperty("address")
  public String getAddress() {
    return address;
  }


  @JsonProperty("amount")
  public Number getAmount() {
    return amount;
  }


  @JsonProperty("asset")
  public String getAsset() {
    return assetName;
  }


  @JsonProperty("namespace")
  public String getNamespace() {
    return namespace;
  }


  public void setAddress(String address) {
    this.address = address;
  }


  public void setAmount(Number amount) {
    this.amount = amount;
  }


  public void setAsset(String asset) {
    this.assetName = asset;
  }


  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
