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
package io.setl.bc.json.tx.external;

import static io.setl.common.StringUtils.notNull;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.tx.create.BaseTransaction;

/**
 * Adds the "fromAddress" and "fromPublicKey" fields as standard
 *
 * @author Simon Greatrix on 21/01/2020.
 */
@JsonDeserialize
public abstract class JsonBaseTransaction extends BaseTransaction {

  @JsonProperty("fromAddress")
  @Schema(description = "The source address. Can be derived from the associated public key.")
  @NotNull
  private String fromAddress = "";

  @JsonProperty("fromPublicKey")
  @Schema(description = "Public key of the source address. Normally derived from the wallet.")
  private String fromPublicKey = "";

  protected JsonBaseTransaction() {
    // do nothing
  }


  protected JsonBaseTransaction(String fromAddress) {
    this.fromAddress = notNull(fromAddress);
  }


  protected JsonBaseTransaction(AbstractTx tx) {
    super(tx);
    fromAddress = tx.getFromAddress();
    fromPublicKey = tx.getFromPublicKey();
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getFromPublicKey() {
    return fromPublicKey;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public String getNonceAddress() {
    return getFromAddress();
  }


  @JsonIgnore
  @Hidden
  @Override
  public String getNoncePublicKey() {
    return getFromPublicKey();
  }


  public void setFromAddress(String fromAddress) {
    this.fromAddress = notNull(fromAddress);
  }


  public void setFromPublicKey(String fromPublicKey) {
    this.fromPublicKey = notNull(fromPublicKey);
  }


  @Override
  public void setNoncePublicKey(String key) {
    setFromPublicKey(key);
  }

}
