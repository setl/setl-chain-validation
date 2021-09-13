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
package io.setl.bc.json.tx.internal;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonPatch;

import io.setl.bc.json.data.SmileHelper;
import io.setl.bc.json.tx.external.JsonUpdateTransaction;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.ObjectArrayReader;
import io.setl.json.Canonical;
import io.setl.json.patch.Patch;

/**
 * @author Valerio Trigari, 17/02/2020.
 */
public abstract class BaseUpdateTx extends BaseTx {

  private JsonPatch patch;


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public BaseUpdateTx(ObjectArrayReader reader) {
    super(reader);
    patch = new Patch(SmileHelper.readSmileArray(reader.getBinary()));
  }


  /**
   * New instance from the external representation of the transaction.
   *
   * @param tx the external representation
   */
  public BaseUpdateTx(JsonUpdateTransaction tx) {
    super(tx);
    patch = tx.getPatch();
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator accumulator) {
    super.startHash(accumulator);
    accumulator.add(patch.toJsonArray());
    return accumulator;
  }


  public JsonPatch getPatch() {
    JsonArray jsonArray = Canonical.cast(patch.toJsonArray()).copy().asJsonArray();
    return Json.createPatch(jsonArray);
  }


  @Override
  protected int requiredEncodingSize() {
    return super.requiredEncodingSize() + 1;
  }


  public void setPatch(JsonPatch patch) {
    this.patch = patch;
  }


  @Override
  protected int startEncode(Object[] encoded) {
    int p = super.startEncode(encoded);
    encoded[p++] = SmileHelper.createSmile(patch.toJsonArray());
    return p;
  }

}
