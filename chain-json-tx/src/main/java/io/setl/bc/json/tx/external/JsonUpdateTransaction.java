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

import javax.json.JsonPatch;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.internal.BaseUpdateTx;

/**
 * Intermediate class for all the transactions that take a JSON patch to do an update.
 */
public abstract class JsonUpdateTransaction extends JsonBaseTransaction {

  private JsonPatch patch;


  public JsonUpdateTransaction(String fromAddress, JsonPatch patch) {
    super(fromAddress);
    this.patch = patch;
  }


  /**
   * New instance from the internal form of the transaction.
   *
   * @param tx the internal for
   */
  public JsonUpdateTransaction(BaseUpdateTx tx) {
    super(tx);
    patch = tx.getPatch();
  }


  public JsonUpdateTransaction() {
    // do nothing
  }


  public JsonPatch getPatch() {
    return patch;
  }


  @JsonProperty(value = "patch", required = true)
  @Schema(description = "The patch to apply", ref = "#/components/schemas/json.patch")
  public void setPatch(JsonPatch patch) {
    this.patch = patch;
  }

}
