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
package io.setl.bc.json.data;

import java.util.List;
import java.util.Objects;
import javax.json.JsonStructure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.common.ObjectArrayReader;
import io.setl.json.Canonical;

/**
 * @author Simon Greatrix on 13/01/2020.
 */
public class DataDocument extends StandardDatum {

  /** The ID of the document's ACL. */
  private SpaceId aclId = SpaceId.EMPTY;

  /** The document itself. */
  private JsonStructure document;

  /**
   * An optional document owner. A document's owner address is matched against the special ACL 'owner' address, which allows standard ACLs to be re-used by
   * multiple document owners.
   */
  private String owner;

  /**
   * A cryptographic salt included in this record to ensure to identical documents have different hashes and to prevent reconstruction of a document from a
   * known hash.
   */
  private String salt;

  /** The ID of the validator for the document. */
  private SpaceId validatorId = SpaceId.EMPTY;


  public DataDocument() {
    // do nothing
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DataDocument(
      @JsonProperty(value = "id", required = true) SpaceId id,
      @JsonProperty(value = "document", required = true) JsonStructure document
  ) {
    super(id);
    setDocument(document);
  }


  /**
   * New instance as a copy of another instance.
   *
   * @param document the other instance
   */
  public DataDocument(DataDocument document) {
    super(document);
    this.document = (JsonStructure) Canonical.cast(document.document).copy();
    this.aclId = document.aclId;
    this.validatorId = document.validatorId;
    this.salt = document.getSalt();
    this.owner = document.owner;
  }


  /**
   * New instance from the encoded form.
   *
   * @param reader reader of the encoded form
   */
  public DataDocument(ObjectArrayReader reader) {
    super(reader);
    this.document = SmileHelper.readSmile(reader.getBinary());
    this.aclId = new SpaceId(reader.getReader());
    this.validatorId = new SpaceId(reader.getReader());
    this.salt = reader.getString();
    this.owner = reader.getString();
  }


  @Override
  public DataDocument copy() {
    return new DataDocument(this);
  }


  @Override
  protected void encode(List<Object> list) {
    super.encode(list);
    list.add(SmileHelper.createSmile(document));
    list.add(aclId.encode());
    list.add(validatorId.encode());
    list.add(getSalt());
    list.add(owner);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataDocument)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    DataDocument that = (DataDocument) o;
    return getSalt().equals(that.getSalt()) && Objects.equals(owner, that.owner) && document.equals(that.document) && aclId.equals(that.aclId);
  }


  @Schema(description = "The ACL ID as a String array.", ref = "#/components/schemas/json.array")
  public SpaceId getAclId() {
    return aclId;
  }


  @Schema(
      description = "The JSON data for this document.",
      ref = "#/components/schemas/json.structure"
  )
  public JsonStructure getDocument() {
    return document;
  }


  @Schema(description = "An optional document owner address")
  @JsonInclude(Include.NON_NULL)
  @JsonProperty("owner")
  public String getOwner() {
    return owner;
  }


  /**
   * Get the cryptographic salt associated with this data document.
   *
   * @return the salt
   */
  @Schema(description = "Cryptographic salt")
  public String getSalt() {
    if (this.salt == null) {
      this.salt = JsonSalt.create();
    }
    return this.salt;
  }


  @Schema(description = "The Validator ID as a String array.", ref = "#/components/schemas/json.array")
  public SpaceId getValidatorId() {
    return validatorId;
  }


  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(owner);
    result = 31 * result + Objects.hashCode(salt);
    result = 31 * result + document.hashCode();
    return 31 * result + aclId.hashCode();
  }


  public void setAclId(SpaceId aclId) {
    this.aclId = SpaceId.notNull(aclId);
  }


  @Schema(description = "The JSON document", required = true, ref = "#/components/schemas/json.structure")
  @JsonProperty(required = true)
  public final void setDocument(JsonStructure document) {
    Objects.requireNonNull(document, "JSON document cannot be null");
    this.document = document;
  }


  public void setOwner(String owner) {
    this.owner = owner;
  }


  @Schema(description = "Cryptographic salt")
  @JsonProperty
  public void setSalt(String salt) {
    this.salt = (salt == null || salt.isEmpty()) ? JsonSalt.create() : salt;
  }


  public void setValidatorId(SpaceId validatorId) {
    this.validatorId = SpaceId.notNull(validatorId);
  }

}
