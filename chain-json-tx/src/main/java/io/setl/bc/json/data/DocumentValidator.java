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

import static io.setl.common.StringUtils.notNull;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.json.JsonValue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;

import io.setl.bc.json.tx.external.SpaceId;
import io.setl.bc.json.validation.Validator;
import io.setl.bc.json.validation.ValidatorFactory;
import io.setl.common.ObjectArrayReader;
import io.setl.json.Canonical;
import io.setl.json.primitive.CJString;
import io.setl.util.CopyOnWriteMap;

/**
 * @author Simon Greatrix on 24/01/2020.
 */
public class DocumentValidator extends StandardDatum {

  /** Map of standard document validators by space IDs. */
  private static final CopyOnWriteMap<SpaceId, DocumentValidator> STANDARD_VALIDATORS = new CopyOnWriteMap<>();


  /**
   * Attempt to find a standard validator. Standard validators do not need additional configuration.
   *
   * @param spaceId the ID of the validator
   *
   * @return the document validator, or null
   */
  @Nullable
  public static DocumentValidator findStandard(SpaceId spaceId) {
    if (spaceId == null) {
      return null;
    }
    if (!DataNamespace.INTERNAL_NAMESPACE.equals(spaceId.getNamespace())) {
      return null;
    }

    DocumentValidator documentValidator = STANDARD_VALIDATORS.get(spaceId);
    if (documentValidator != null) {
      return documentValidator;
    }

    if (!spaceId.getId().startsWith("java|")) {
      return null;
    }

    documentValidator = new DocumentValidator(spaceId);
    documentValidator.setAlgorithm("java");
    documentValidator.setConfiguration(CJString.create(spaceId.getId().substring(5)));
    try {
      documentValidator.getValidator();
    } catch (RuntimeException e) {
      // Standard validators should always be available, so this is a major problem
      throw new InternalError("Standard validator cannot be instantiated", e);
    }
    STANDARD_VALIDATORS.put(spaceId, documentValidator);
    return documentValidator;
  }


  @NotNull
  public static SpaceId forClass(Class<?> type) {
    return new SpaceId(DataNamespace.INTERNAL_NAMESPACE, "java|" + type.getName());
  }


  /** The validation algorithm. */
  private String algorithm;

  /** Configuration for the algorithm. */
  private JsonValue configuration;

  /** Lazy loaded validator. */
  private Validator validator;


  public DocumentValidator() {
    // do nothing
  }


  /**
   * Creator for Jackson. Jackson only honours required properties when they are in a creator.
   */
  @JsonCreator
  public DocumentValidator(
      @JsonProperty(value = "id", required = true) SpaceId id
  ) {
    super(id);
  }


  /**
   * New instance as a copy of another instance.
   *
   * @param copy the other instance.
   */
  public DocumentValidator(DocumentValidator copy) {
    super(copy);
    algorithm = copy.algorithm;
    configuration = Canonical.cast(copy.configuration).copy();
    validator = null;
  }


  /**
   * New instance from the encoded form.
   *
   * @param encoded reader of the encoded form
   */
  public DocumentValidator(ObjectArrayReader encoded) {
    super(encoded);
    algorithm = encoded.getString();
    configuration = SmileHelper.readSmileValue(encoded.getBinary());
  }


  @Override
  public DocumentValidator copy() {
    return new DocumentValidator(this);
  }


  @Override
  protected void encode(List<Object> list) {
    super.encode(list);
    list.add(algorithm);
    list.add(SmileHelper.createSmile(configuration));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DocumentValidator)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    DocumentValidator that = (DocumentValidator) o;

    if (!algorithm.equals(that.algorithm)) {
      return false;
    }
    return Objects.equals(configuration, that.configuration);
  }


  public String getAlgorithm() {
    return algorithm;
  }


  public JsonValue getConfiguration() {
    return configuration;
  }


  /**
   * Get the actual validator instance. This may require the validator to be loaded and initialised.
   *
   * @return the validator
   */
  @JsonIgnore
  @Hidden
  public synchronized Validator getValidator() {
    if (validator == null) {
      validator = ValidatorFactory.getValidator(algorithm);
      validator.initialise(configuration);
    }
    return validator;
  }


  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + algorithm.hashCode();
    return 31 * result + (configuration != null ? configuration.hashCode() : 0);
  }


  public void setAlgorithm(String algorithm) {
    this.algorithm = notNull(algorithm);
  }


  public void setConfiguration(JsonValue configuration) {
    this.configuration = configuration;
  }


  public List<ValidationFailure> validate(JsonValue document) {
    return getValidator().validate(document);
  }

}
