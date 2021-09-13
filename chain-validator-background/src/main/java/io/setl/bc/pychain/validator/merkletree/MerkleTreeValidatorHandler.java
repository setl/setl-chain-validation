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

import io.setl.bc.pychain.validator.ValidationException;
import io.setl.bc.pychain.validator.Validator;
import io.setl.bc.pychain.validator.ValidatorHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/**
 * Handles the running of the Merkle Tree Validator.
 *
 * @author Valerio Trigari 23/11/2018
 */
public class MerkleTreeValidatorHandler implements ValidatorHandler {
  
  @Value("${validator.sleep:5000}")
  private int sleepTime;
  
  private static final Logger logger = LoggerFactory.getLogger(MerkleTreeValidatorHandler.class);
  
  private final Object lock = new Object();
  
  private Validator validator;

  private AtomicBoolean active = new AtomicBoolean(true);
  private AtomicBoolean running = new AtomicBoolean(true);
  private AtomicBoolean isValidated = new AtomicBoolean(true);
  private AtomicInteger height = new AtomicInteger(0);
  private AtomicLong resultTime = new AtomicLong();
  
  private int previousHeight = 0;
  
  public MerkleTreeValidatorHandler(Validator validator) {
    this.validator = validator;
  }
  
  @Override
  public void start() {
    // Run validate method in separate thread, with lowest priority
    logger.info("Validator started");
    
    Thread thread = new Thread(() -> {
      while (running.get()) {
        if (active.get()) {
          try {
            while ((previousHeight == height.get()) && running.get()) {
              synchronized (lock) {
                logger.info("Height did not change, Validator sleeping for up to {} seconds", sleepTime / 1000);
                lock.wait(sleepTime);
              }
            }
            
            validator.validate(height.get());
            resultTime.set(System.currentTimeMillis());
          } catch (ValidationException | InterruptedException e) {
            logger.error("Validation failed", e);
            isValidated.set(false);
            stop();
            resultTime.set(System.currentTimeMillis());
            Thread.currentThread().interrupt();
          }
          
          previousHeight = height.get();
        }
      }
    });
    
    thread.setPriority(1);
    thread.start();
  }
  
  @Override
  @EventListener(ContextClosedEvent.class)
  public void stop() {
    logger.info("Validator stopped");
    running.set(false);
  }
  
  @Override
  public void pause() {
    logger.info("Validator paused");
    active.set(false);
  }
  
  @Override
  public void resume() {
    active.set(true);
    
    synchronized (lock) {
      logger.info("Validator resumed");
      lock.notifyAll();
    }
  }
  
  @Override
  public void setHeight(int height) {
    this.height.set(height);
  }

  @Override
  public AtomicBoolean getIsValidated() {
    return isValidated;
  }

  @Override
  public AtomicLong getResultTime() {
    return resultTime;
  }
  
}
