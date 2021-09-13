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
package io.setl.bc.json.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.security.DigestException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;

import org.junit.Test;

import io.setl.bc.json.data.DataDocument;
import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.serialise.Content;

/**
 * @author Simon Greatrix on 23/03/2020.
 */
public class DataDocumentFactoryTest {
  DataDocumentFactory factory = new DataDocumentFactory();

  @Test
  public void getType() {
    assertEquals(DataDocument.class, factory.getType());
  }

  @Test
  public void saveAndLoad() throws DigestException {
    DataDocument dataDocument = new DataDocument();
    dataDocument.setId(new SpaceId("X","test"));
    dataDocument.setAclId(SpaceId.EMPTY);
    dataDocument.setTitle("A test document");
    JsonArray array = Json.createArrayBuilder().add(1).add("text").add(JsonValue.EMPTY_JSON_OBJECT).build();
    dataDocument.setDocument(array);

    Content content = factory.asContent(Digest.TYPE_SHA_512_256,dataDocument);
    DataDocument output = factory.asVerifiedValue(content.getHash(), content.getData());
    assertEquals(dataDocument,output);

    Content content2 = factory.asContent(Digest.TYPE_SHA_512_256,output);
    assertEquals(content.getHash(),content2.getHash());
  }
}