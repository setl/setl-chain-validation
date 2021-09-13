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
package io.setl.bc.pychain.validator.merkletree;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.setl.bc.pychain.validator.ValidationException;
import io.setl.bc.pychain.validator.Validator;
import io.setl.bc.pychain.validator.ValidatorHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Although sleep Thread.sleep() should not be used in tests,
 * we do so in here to allow the various actions to take place
 * and simulate a production scenario.
 */
@RunWith(SpringRunner.class)
public class MerkleTreeValidatorHandlerTest {
  
  @Mock
  private Validator validator;
  
  private ValidatorHandler handler;
  
  @Test
  public void handlerRunsCorrectly() throws Exception {
    doNothing().when(validator).validate(anyInt());
    
    handler = new MerkleTreeValidatorHandler(validator);
    handler.setHeight(1);
    ReflectionTestUtils.setField(handler, "sleepTime", 50);
  
    handler.start();
    Thread.sleep(100);
  
    handler.setHeight(2);
    Thread.sleep(100);

    handler.setHeight(3);
    Thread.sleep(100);
    
    verify(validator, atLeast(1)).validate(anyInt());
    verify(validator, times(3)).validate(anyInt());
  }
  
  @Test
  public void setHandlerPausesAndResumesCorrectly() throws Exception {
    doNothing().when(validator).validate(anyInt());
    
    handler = new MerkleTreeValidatorHandler(validator);
    handler.setHeight(1);
    ReflectionTestUtils.setField(handler, "sleepTime", 50);
    
    handler.start();
    Thread.sleep(100);
    
    verify(validator).validate(1);
    
    handler.pause();
    Thread.sleep(100);
    
    verifyZeroInteractions(validator);
    
    handler.setHeight(2);
    handler.resume();
    Thread.sleep(100);
    
    verify(validator).validate(2);
    verify(validator, times(2)).validate(anyInt());
  }
  
  @Test
  public void validatorThrowsException() throws Exception {
    doThrow(new ValidationException("Something went wrong...")).when(validator).validate(anyInt());
    
    handler = new MerkleTreeValidatorHandler(validator);
    handler.setHeight(1);
    ReflectionTestUtils.setField(handler, "sleepTime", 50);
    
    handler.start();
    Thread.sleep(100);
    
    verify(validator, atMost(1)).validate(anyInt());
  }
  
}
