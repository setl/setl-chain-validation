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

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.data.DataNamespace;
import io.setl.bc.json.tx.internal.CreateDataNamespaceTx;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 20/01/2020.
 */
public class CreateDataNamespace extends JsonBaseTransaction {

  private DataNamespace namespace;


  public CreateDataNamespace() {
    // do nothing
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public CreateDataNamespace(
      @JsonProperty(value = "fromAddress", required = true) String fromAddress,
      @JsonProperty(value = "namespace", required = true) DataNamespace namespace
  ) {
    super(fromAddress);
    this.namespace = namespace;
  }


  public CreateDataNamespace(CreateDataNamespaceTx tx) {
    super(tx);
    namespace = tx.getNamespace();
  }


  @Override
  public CreateDataNamespaceTx create() {
    return new CreateDataNamespaceTx(this);
  }


  public DataNamespace getNamespace() {
    return namespace;
  }


  @Nonnull
  @Override
  @JsonIgnore
  @Hidden
  public TxType getTxType() {
    return TxType.JSON_CREATE_DATA_NAMESPACE;
  }


  @JsonProperty(required = true)
  @Schema(description = "The namespace definition", required = true)
  public void setNamespace(DataNamespace namespace) {
    this.namespace = namespace;
  }

}
