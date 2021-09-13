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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.Problem;
import org.leadpony.justify.api.ProblemHandler;
import org.leadpony.justify.api.ValidationConfig;

import io.setl.bc.json.data.ValidationFailure;

/**
 * @author Simon Greatrix on 14/02/2020.
 */
@SuppressFBWarnings("IMC_IMMATURE_CLASS_NO_TOSTRING")
class JsonSchemaValidator implements Validator {

  private static final JsonValidationService service = JsonValidationService.newInstance();



  @SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
  static class MyValidator implements ProblemHandler {

    private final JsonReaderFactory factory;

    private List<Problem> problems = null;


    MyValidator(JsonSchema mySchema) {
      ValidationConfig config = service.createValidationConfig().withSchema(mySchema).withDefaultValues(true).withProblemHandler(this);
      factory = service.createReaderFactory(config.getAsMap());
    }


    @Override
    public synchronized void handleProblems(List<Problem> newProblems) {
      problems.addAll(newProblems);
    }


    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // False positive from try-with-resources
    synchronized List<Problem> validate(JsonValue value) {
      problems = new ArrayList<>();
      try (JsonReader reader = factory.createReader(new StringReader(value.toString()))) {
        reader.readValue();
      }
      List<Problem> list = problems;
      problems = null;
      return list;
    }

  }



  private final BlockingQueue<MyValidator> myValidators = new ArrayBlockingQueue<>(10);

  private JsonSchema mySchema;


  @Override
  public void initialise(JsonValue configurationValue) {
    JsonObject configuration = configurationValue.asJsonObject();
    JsonValue schemaValue = configuration.get("schema");
    mySchema = service.readSchema(new StringReader(schemaValue.toString()));
  }


  private void releaseValidator(MyValidator validator) {
    myValidators.add(validator);
  }


  private MyValidator reserveValidator() {
    MyValidator v = myValidators.poll();
    if (v != null) {
      return v;
    }
    return new MyValidator(mySchema);
  }


  @Override
  public List<ValidationFailure> validate(JsonValue value) {
    MyValidator validator = reserveValidator();
    List<Problem> problemList;
    try {
      problemList = validator.validate(value);
    } finally {
      releaseValidator(validator);
    }

    ArrayList<ValidationFailure> problems = new ArrayList<>(problemList.size());
    for (Problem p : problemList) {
      problems.add(new ValidationFailure(p.getMessage(), p.getLocation()));
    }
    return problems;
  }

}
