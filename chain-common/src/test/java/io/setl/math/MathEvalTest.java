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
package io.setl.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.common.Balance;
import java.math.BigInteger;
import java.util.Set;
import org.junit.Test;

/**
 * @author Simon Greatrix on 20/07/2018.
 */
public class MathEvalTest {
  
  @Test
  public void checkPrecision() {
    
    MathEval eval = new MathEval();
  
    eval.setConstant("nav", 1700001);
  
    assertEquals("1700011111", eval.evaluate("1700011111 + nav * 0").toPlainString());
    assertEquals("1234567890123456789012345678901234567890",
        eval.evaluate("1234567890123456789012345678901234567890 + nav * 0").toPlainString());

    assertEquals("12345678901234567890123456789012345678901234567890123456789012345678901234567890",
        eval.evaluate("12345678901234567890123456789012345678901234567890123456789012345678901234567890 + nav * 0").toPlainString());

    // Keep lots of precision, scale division to 20 places.
    assertEquals(
        eval.evaluate("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890 / (10 ^ 40)").toPlainString(),
        "123456789012345678901234567890123456789012345678901234567890.12345678901234567890");

    assertEquals("0.33333333333333333333",
        eval.evaluate("1 / 3").toPlainString());

  }
  
  @Test
  public void getVariablesWithin() {
    
    MathEval eval = new MathEval();
    
    Set<String> ss = eval.getVariablesWithin("(VarA * 42) + VarB / VarC");
    assertTrue(ss.contains("VarA"));
    assertTrue(ss.contains("VarB"));
    assertTrue(ss.contains("VarC"));
    assertTrue(ss.size() == 3);
  }
  
  
  @Test
  public void setConstant() {
    
    MathEval eval = new MathEval();
    
    eval.setConstant("ValA", 42);
    
    assertTrue(eval.getConstant("ValA").intValue() == 42);
    
    assertEquals("42", eval.evaluate("1 * ValA").toPlainString());
  
    try {
      eval.setConstant("ValA", null);
      throw new IllegalArgumentException("");
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
  
    try {
      eval.setConstant("ValA1", null);
      assertTrue(eval.getConstant("ValA1").intValue() == 0);
      throw new NullPointerException("");
    } catch (NullPointerException e) {
      assertTrue(true);
    }
  
    // BigInteger
    eval.setConstant("ValA2", BigInteger.valueOf(43L));
    assertTrue(eval.getConstant("ValA2").intValue() == 43);
  
    // Long
    eval.setConstant("ValA3", 44L);
    assertTrue(eval.getConstant("ValA3").intValue() == 44);
  
    // Integer
    eval.setConstant("ValA4", Integer.valueOf(45));
    assertTrue(eval.getConstant("ValA4").intValue() == 45);
  
    // Number
    eval.setConstant("ValA5", new Balance(46));
    assertTrue(eval.getConstant("ValA5").intValue() == 46);
  
    // String
    eval.setConstant("ValA6", "47");
    assertTrue(eval.getConstant("ValA6").intValue() == 47);
  
    assertTrue(eval.getConstant("Undefined").intValue() == 0);
    assertTrue(eval.getConstants() != null);
  
  }
  
  
  @Test
  public void setVariable() {
    
    MathEval eval = new MathEval();
    
    // Seems to have no effect
    eval.setVariableRequired(true);
    assertFalse(eval.getVariableRequired());
    eval.setVariableRequired(false);
    assertTrue(eval.getVariableRequired());
  
    eval.setVariable("ValA", 42);
    
    assertTrue(eval.getVariable("ValA").intValue() == 42);
    
    assertEquals("42.0", eval.evaluate("1 * ValA").toPlainString());
    
    eval.setVariable("ValA", null);
    assertTrue(eval.getVariable("ValA").intValue() == 0);
    
    // BigInteger
    eval.setVariable("ValA", BigInteger.valueOf(43L));
    assertTrue(eval.getVariable("ValA").intValue() == 43);
    
    // Long
    eval.setVariable("ValA", 44L);
    assertTrue(eval.getVariable("ValA").intValue() == 44);
    
    // Integer
    eval.setVariable("ValA", Integer.valueOf(45));
    assertTrue(eval.getVariable("ValA").intValue() == 45);
    
    // Number
    eval.setVariable("ValA", new Balance(46));
    assertTrue(eval.getVariable("ValA").intValue() == 46);
    
    // String
    eval.setVariable("ValA", "47");
    assertTrue(eval.getVariable("ValA").intValue() == 47);
    eval.clear();
    assertTrue(eval.getVariable("ValA").intValue() == 0);
    
    // String
    eval.setVariable("ValA", "47");
    assertTrue(eval.getVariable("ValA").intValue() == 47);
    eval.setVariable("setl.ValA", "47");
    assertTrue(eval.getVariable("setl.ValA").intValue() == 47);
    eval.clear("setl");
    assertTrue(eval.getVariable("setl.ValA").intValue() == 0);
    assertTrue(eval.getVariable("ValA").intValue() == 47);
  
  
    assertTrue(eval.getVariable("Undefined").intValue() == 0);
    assertTrue(eval.getVariables() != null);
  
  }
  
  
  @Test
  public void testCeil() {
    
    MathEval eval = new MathEval();
    assertEquals("13", eval.evaluate("ceil(12.34)").toPlainString());
    assertEquals("12.4", eval.evaluate("ceil(12.38,1)").toPlainString());
    assertEquals("20", eval.evaluate("ceil(12.34,-1)").toPlainString());
  }
  
  @Test
  public void testSetFunctionHandler() {
    
    MathEval eval = new MathEval();
    assertEquals("13", eval.evaluate("ceil(12.34)").toPlainString());
    
    eval.setFunctionHandler("ceil", null, false);
  
    try {
      assertEquals("13", eval.evaluate("ceil(12.34)").toPlainString());
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
  }
  
  @Test
  public void maxmin() {
    
    MathEval eval = new MathEval();
    assertEquals("20", eval.evaluate("max(min(30, 20), 10)").toPlainString());
  }
  
  
  @Test
  public void testCopyConstructor() {
    
    MathEval eval = new MathEval();
    eval.setVariable("ValA", "47");
    eval.setConstant("ValB", 42);

    MathEval eval2 = new MathEval(eval);

    assertTrue(eval.evaluate("ValA + ValB").equals(eval2.evaluate("ValA + ValB")));

  }
  
  
  @Test
  public void testFloor() {
    
    MathEval eval = new MathEval();
    
    assertEquals("12", eval.evaluate("floor(12.34)").toPlainString());
    assertEquals("12.3", eval.evaluate("floor(12.38,1)").toPlainString());
    assertEquals("10", eval.evaluate("floor(12.34,-1)").toPlainString());
  }
  
  
  @Test
  public void testRound() {
    
    MathEval eval = new MathEval();
    assertEquals("12", eval.evaluate("round(12.34)").toPlainString());
    assertEquals("12.4", eval.evaluate("round(12.38,1)").toPlainString());
    assertEquals("10", eval.evaluate("round(12.34,-1)").toPlainString());
  }
  
  
  @Test
  public void testRoundHE() {
    
    MathEval eval = new MathEval();
    assertEquals("12", eval.evaluate("roundHE(12.35)").toPlainString());
    assertEquals("12", eval.evaluate("roundHE(12.25)").toPlainString());
    assertEquals("12", eval.evaluate("roundHE(12.50)").toPlainString());
    assertEquals("14", eval.evaluate("roundHE(13.50)").toPlainString());
    assertEquals("13", eval.evaluate("roundHE(12.52)").toPlainString());
    assertEquals("12.4", eval.evaluate("roundHE(12.38,1)").toPlainString());
    assertEquals("10", eval.evaluate("roundHE(12.34,-1)").toPlainString());
  }
  
  @Test
  public void evaluate() {
  
    MathEval eval = new MathEval();
  
    assertTrue(eval.evaluate("1").intValue() == 1);
    assertTrue(eval.evaluate("abs(-1)").intValue() == 1);
    assertTrue(eval.evaluate("acos(1)").intValue() == 0);
    assertTrue(eval.evaluate("asin(0)").intValue() == 0);
    assertTrue(eval.evaluate("atan(0)").intValue() == 0);
    assertTrue(eval.evaluate("cos(0)").intValue() == 1);
    assertTrue(eval.evaluate("log(e)").intValue() == 1);
    assertTrue(eval.evaluate("log10(10)").intValue() == 1);
    assertTrue(eval.evaluate("cosh(0)").intValue() == 1);
    assertTrue(eval.evaluate("sin(0)").intValue() == 0);
    assertTrue(eval.evaluate("sinh(0)").intValue() == 0);
    assertTrue(eval.evaluate("signum(0)").intValue() == 0);
    assertTrue(eval.evaluate("sqrt(0)").intValue() == 0);
    assertTrue(eval.evaluate("tan(0)").intValue() == 0);
    assertTrue(eval.evaluate("tanh(0)").intValue() == 0);
    assertTrue(eval.evaluate("(1)").intValue() == 1);
    assertTrue(eval.evaluate("(((1+2-1*1/4÷1)^1)%10)").intValue() == 2);
    
    try {
      eval.evaluate("");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    try {
      eval.evaluate("()");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    try {
      eval.evaluate("1/0");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    try {
      eval.evaluate("1 $ 1");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    try {
      eval.evaluate("1 / (2 / 0)");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    try {
      eval.evaluate("(2 / 0) / 1");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    try {
      eval.evaluate("crud(1)");
      throw new ArithmeticException("");
    } catch (ArithmeticException e) {
      assertTrue(true);
    }
  
    
  }
  
  @Test
  public void validateName() {
    
    MathEval eval = new MathEval();
    
    try {
      eval.setVariable(null, 42);
      throw new IllegalArgumentException("Names for constants, variables and functions must not be null");
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
    
    try {
      eval.setVariable("$*!", 42);
      throw new IllegalArgumentException("Names for constants, variables and functions must start with a letter");
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
    
    try {
      eval.setVariable("A{B}", 42);
      throw new IllegalArgumentException("Names for constants, variables and functions may not contain a parenthesis");
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
    
  }
  
}