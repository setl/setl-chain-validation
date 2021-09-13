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
package io.setl.bc.json.validation;

import static io.setl.common.StringUtils.logSafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.validation.ConstraintViolation;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.setl.bc.json.data.ValidationFailure;
import io.setl.json.jackson.Convert;

/**
 * @author Simon Greatrix on 23/09/2020.
 */
public class ObjectValidator implements Validator {

  private final javax.validation.Validator javaValidator;

  private final ObjectMapper objectMapper;

  private Class<?> target;


  public ObjectValidator(ObjectMapper objectMapper, javax.validation.Validator javaValidator) {
    this.objectMapper = objectMapper;
    this.javaValidator = javaValidator;
  }


  @Override
  public void initialise(JsonValue configuration) {
    String className = ((JsonString) configuration).getString();
    try {
      target = Class.forName(className);
    } catch (ClassNotFoundException classNotFoundException) {
      throw new IllegalArgumentException("Class " + logSafe(className) + " is not available");
    }
  }


  @Override
  public List<ValidationFailure> validate(JsonValue value) {
    return validateInternal(value);
  }


  private <T> List<ValidationFailure> validateInternal(JsonValue value) {
    @SuppressWarnings("unchecked")
    Class<? extends T> type = (Class<? extends T>) target;

    // first try to convert to a POJO
    T object;
    try {
      object = objectMapper.treeToValue(Convert.toJackson(value), type);
    } catch (JsonProcessingException jsonProcessingException) {
      ValidationFailure failure = new ValidationFailure();
      failure.setMessage(jsonProcessingException.getOriginalMessage());
      JsonLocation location = jsonProcessingException.getLocation();
      if (location != null) {
        failure.setColumn(location.getColumnNr());
        failure.setLine(location.getLineNr());
        long offset = location.getCharOffset();
        if (offset == -1) {
          offset = location.getByteOffset();
        }
        failure.setStreamOffset(offset);
      }
      return List.of(failure);
    }

    Set<ConstraintViolation<T>> violationSet = javaValidator.validate(object);
    if (violationSet.isEmpty()) {
      // all good
      return Collections.emptyList();
    }

    ArrayList<ValidationFailure> failures = new ArrayList<>(violationSet.size());
    for (ConstraintViolation<T> violation : violationSet) {
      ValidationFailure failure = new ValidationFailure();
      failure.setMessage(logSafe(violation.getMessage()) + " AT " + logSafe(violation.getPropertyPath().toString()));
      failures.add(failure);
    }
    return failures;
  }

}
