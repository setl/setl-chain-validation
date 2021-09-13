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
package io.setl.bc.pychain.tx.deserialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

import io.setl.bc.pychain.tx.create.BaseTransaction;
import io.setl.common.CommonPy.TxType;

public class BaseTransactionDeserializer extends JsonDeserializer<BaseTransaction> {

  @Override
  public BaseTransaction deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode treeNode = p.readValueAsTree();

    if (treeNode.has("txType")) {
      BaseTransaction tx = BaseTransaction.getRepresentation(TxType.get(treeNode.get("txType").asText()));
      return p.getCodec().treeToValue(treeNode, tx.getClass());
    }

    throw InvalidTypeIdException.from(p, "BaseTransaction must specify a valid 'txType' property", ctxt.constructType(BaseTransaction.class), null);
  }

}
