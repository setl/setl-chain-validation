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
// Created by Lawrence PC Dol.  Released into the public domain.
// http://tech.dolhub.com
//
// Contributions by Carlos Gómez of Asturias, Spain, in the area of unary operators
// and right-to-left evaluations proved invaluable to implementing these features.
// Thanks Carlos!
//
// Source is licensed for any use, provided this copyright notice is retained.
// No warranty for any purpose whatsoever is implied or expressed.  The author
// is not liable for any losses of any kind, direct or indirect, which result
// from the use of this software.

package io.setl.math;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Math Evaluator.  Provides the ability to evaluate a String math expression, with support for pureFunctions, variables and
 * standard math constants.
 *
 * <p>Supported Operators:
 * Operator  Precedence  Unary Binding  Description
 * --------- ----------- -------------- ------------------------------------------------
 * '='       99 / 99     RIGHT_SIDE     Simple assignment (internal, used for the final operation)
 * '^'       80 / 81     NO_SIDE        Power
 * '±'       60 / 60     RIGHT_SIDE     Unary negation (internal, substituted for '-')
 * '*'       40 / 40     NO_SIDE        Multiple (conventional computer notation)
 * '×'       40 / 40     NO_SIDE        Multiple (because it's a Unicode world out there)
 * '·'       40 / 40     NO_SIDE        Multiple (because it's a Unicode world out there)
 * '('       40 / 40     NO_SIDE        Multiply (implicit due to brackets, e.g "(a)(b)")
 * '/'       40 / 40     NO_SIDE        Divide (conventional computer notation)
 * '÷'       40 / 40     NO_SIDE        Divide (because it's a Unicode world out there)
 * '%'       40 / 40     NO_SIDE        Remainder
 * '+'       20 / 20     NO_SIDE        Add/unary-positive
 * '-'       20 / 20     NO_SIDE        Subtract/unary-negative
 *
 *
 * <p>Predefined Constants:
 * Name                 Description
 * -------------------- ----------------------------------------------------------------
 * E                    The BigDecimal value that is closer than any other to e, the base of the natural logarithms (2.718281828459045).
 * Euler                Euler's Constant (0.577215664901533).
 * LN2                  Log of 2 base e (0.693147180559945).
 * LN10                 Log of 10 base e (2.302585092994046).
 * LOG2E                Log of e base 2 (1.442695040888963).
 * LOG10E               Log of e base 10 (0.434294481903252).
 * PHI                  The golden ratio (1.618033988749895).
 * PI                   The BigDecimal value that is closer than any other to pi, the ratio of the circumference of a circle to its diameter (3.141592653589793)
 *
 *
 * <p>Supported Functions (see java.Math for detail and parameters):
 * abs
 * acos
 * asin
 * atan
 * ceil
 * cos
 * cosh
 * exp
 * floor
 * log
 * log10
 * max
 * min
 * random
 * round
 * roundHE (maps to Math.rint)
 * signum
 * sin
 * sinh
 * sqrt
 * tan
 * tanh
 * toDegrees
 * toRadians
 * ulp
 *
 * <p>Threading Design : [x] Single Threaded  [ ] Threadsafe  [ ] Immutable  [ ] Isolated
 *
 * @author Lawrence Dol
 * @since Build 2008.0426.1016
 */

public class MathEval extends Object {

  /** Implementation for the default function (java.lang.Math). */
  public static final FunctionHandler DFT_FUNCTION_HANDLER = DefaultImpl.INSTANCE;

// *************************************************************************************************
// INSTANCE PROPERTIES
// *************************************************************************************************

  /** Implementation for the default operators. */
  public static final OperatorHandler DFT_OPERATOR_HANDLER = DefaultImpl.INSTANCE;

  /** Operator/operand on on the left. */
  public static final int LEFT_SIDE = 'L';

  /** Operator/operand side is immaterial. */
  public static final int NO_SIDE = 'B';

  /** Operator/operand on on the right. */
  public static final int RIGHT_SIDE = 'R';

  private static final Operator OPERAND = new Operator('\0', 0, 0, NO_SIDE, false, null);  // special "non-operator" representing an operand character.

  private static BigDecimal BD_2PI = BigDecimal.valueOf(Math.PI * 2.0);

  private static BigDecimal BD_E = BigDecimal.valueOf(Math.E * 2.0);

  private static MathContext MC_CEILING = new MathContext(0, RoundingMode.CEILING);

  private static MathContext MC_FLOOR = new MathContext(0, RoundingMode.FLOOR);

  private static MathContext MC_HALF_DOWN = new MathContext(0, RoundingMode.HALF_DOWN);

  private static MathContext MC_HALF_UP = new MathContext(0, RoundingMode.HALF_UP);

  private static int DIVIDE_ROUNDING = BigDecimal.ROUND_HALF_UP;

  private static int DIVIDE_SCALE = 20;


  public static interface FunctionHandler {

    public BigDecimal evaluateFunction(String fncnam, ArgParser fncargs);
  }



  public static interface OperatorHandler {

    public BigDecimal evaluateOperator(BigDecimal lft, char opr, BigDecimal rgt);
  }



  /**
   * An implementation of the default supported operations and functions.
   */
  @SuppressFBWarnings("PREDICTABLE_RANDOM")
  static class DefaultImpl
      extends Object
      implements OperatorHandler, FunctionHandler {

    static final DefaultImpl INSTANCE = new DefaultImpl();

    private static final Operator OPR_ADD = new Operator('+', 20, DefaultImpl.INSTANCE); // add/unary-positive

    private static final Operator OPR_BKT = new Operator('(', 40, DefaultImpl.INSTANCE); // multiply (implicit , e.g "(a)(b)")

    private static final Operator OPR_DIV1 = new Operator('/', 40, DefaultImpl.INSTANCE); // divide (classical computing)

    private static final Operator OPR_DIV2 = new Operator('÷', 40, DefaultImpl.INSTANCE); // divide (it's a Unicode world out there)

    private static final Operator OPR_EQU = new Operator('=', 99, 99, RIGHT_SIDE, true, DefaultImpl.INSTANCE); // simple assignment

    private static final Operator OPR_MLT1 = new Operator('*', 40, DefaultImpl.INSTANCE); // multiply (classical)

    private static final Operator OPR_MLT2 = new Operator('×', 40, DefaultImpl.INSTANCE); // multiply (it's a Unicode world out there)

    private static final Operator OPR_MLT3 = new Operator('·', 40, DefaultImpl.INSTANCE); // multiply (it's a Unicode world out there)

    private static final Operator OPR_MOD = new Operator('%', 40, DefaultImpl.INSTANCE); // remainder

    private static final Operator OPR_NEG = new Operator('±', 60, 60, RIGHT_SIDE, true, DefaultImpl.INSTANCE); // unary negation

    private static final Operator OPR_PWR = new Operator('^', 80, 81, NO_SIDE, false, DefaultImpl.INSTANCE); // power

    private static final Operator OPR_SUB = new Operator('-', 20, DefaultImpl.INSTANCE); // subtract/unary-negative


    // To add/remove functions change evaluateOperator() and registration
    static void registerFunctions(MathEval tgt) {
      tgt.setFunctionHandler("abs", INSTANCE);
      tgt.setFunctionHandler("acos", INSTANCE);
      tgt.setFunctionHandler("asin", INSTANCE);
      tgt.setFunctionHandler("atan", INSTANCE);
      tgt.setFunctionHandler("ceil", INSTANCE);
      tgt.setFunctionHandler("cos", INSTANCE);
      tgt.setFunctionHandler("cosh", INSTANCE);
      tgt.setFunctionHandler("exp", INSTANCE);
      tgt.setFunctionHandler("expm1", INSTANCE);
      tgt.setFunctionHandler("floor", INSTANCE);
      //t.setFunctionHandler("getExponent"   ,INSTANCE); // needs Java 6
      tgt.setFunctionHandler("log", INSTANCE);
      tgt.setFunctionHandler("log10", INSTANCE);
      tgt.setFunctionHandler("max", INSTANCE);
      tgt.setFunctionHandler("min", INSTANCE);
      //t.setFunctionHandler("nextUp"        ,INSTANCE); // needs Java 6
      tgt.setFunctionHandler("random", INSTANCE, true); // impure
      tgt.setFunctionHandler("round", INSTANCE);
      tgt.setFunctionHandler("roundHE", INSTANCE); // round half-even
      tgt.setFunctionHandler("signum", INSTANCE);
      tgt.setFunctionHandler("sin", INSTANCE);
      tgt.setFunctionHandler("sinh", INSTANCE);
      tgt.setFunctionHandler("sqrt", INSTANCE);
      tgt.setFunctionHandler("tan", INSTANCE);
      tgt.setFunctionHandler("tanh", INSTANCE);
      tgt.setFunctionHandler("toDegrees", INSTANCE);
      tgt.setFunctionHandler("toRadians", INSTANCE);
      tgt.setFunctionHandler("ulp", INSTANCE);
    }


    // To add/remove operators change evaluateOperator() and registration
    static void registerOperators(MathEval tgt) {

      tgt.setOperator(OPR_EQU);
      tgt.setOperator(OPR_PWR);
      tgt.setOperator(OPR_NEG);
      tgt.setOperator(OPR_MLT1);
      tgt.setOperator(OPR_MLT2);
      tgt.setOperator(OPR_MLT3);
      tgt.setOperator(OPR_BKT);
      tgt.setOperator(OPR_DIV1);
      tgt.setOperator(OPR_DIV2);
      tgt.setOperator(OPR_MOD);
      tgt.setOperator(OPR_ADD);
      tgt.setOperator(OPR_SUB);
    }


    private DefaultImpl() {
    }


    // To add/remove functions change evaluateOperator() and registration
    public BigDecimal evaluateFunction(String fncnam, ArgParser fncargs) {

      switch (Character.toLowerCase(fncnam.charAt(0))) {
        case 'a': {
          if (fncnam.equalsIgnoreCase("abs")) {
            return fncargs.next().abs();
          }
          if (fncnam.equalsIgnoreCase("acos")) {
            return BigDecimal.valueOf(Math.acos(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("asin")) {
            return BigDecimal.valueOf(Math.asin(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("atan")) {
            return BigDecimal.valueOf(Math.atan(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
        }
        break;
        case 'c': {
          if (fncnam.equalsIgnoreCase("ceil")) {
            BigDecimal arg1 = fncargs.next();
            BigDecimal arg2 = BigDecimal.ZERO;
            if (fncargs.hasNext()) {
              arg2 = fncargs.next();
            }
            return arg1.setScale(arg2.intValue(), RoundingMode.CEILING);
          }
          if (fncnam.equalsIgnoreCase("cos")) {
            return BigDecimal.valueOf(Math.cos(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("cosh")) {
            return BigDecimal.valueOf(Math.cosh(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
        }
        break;
        case 'e': {
          if (fncnam.equalsIgnoreCase("exp")) {
            // TODO BIGDOUBLE Need proper BigDouble implimentation
            return BigDecimal.valueOf(Math.exp(fncargs.next().doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("expm1")) {
            // TODO BIGDOUBLE Need proper BigDouble implimentation
            return BigDecimal.valueOf(Math.expm1(fncargs.next().doubleValue()));
          }
        }
        break;
        case 'f': {
          if (fncnam.equalsIgnoreCase("floor")) {
            BigDecimal arg1 = fncargs.next();
            BigDecimal arg2 = BigDecimal.ZERO;
            if (fncargs.hasNext()) {
              arg2 = fncargs.next();
            }
            return arg1.setScale(arg2.intValue(), RoundingMode.FLOOR);
          }
        }
        break;
        case 'g': {
//              if(fncnam.equalsIgnoreCase("getExponent"   )) { return Math.getExponent(fncargs.next());                } needs Java 6
        }
        break;
        case 'l': {
          if (fncnam.equalsIgnoreCase("log")) {
            // TODO BIGDOUBLE Need proper BigDouble implimentation
            return BigDecimal.valueOf(Math.log(fncargs.next().doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("log10")) {
            // TODO BIGDOUBLE Need proper BigDouble implimentation
            return BigDecimal.valueOf(Math.log10(fncargs.next().doubleValue()));
          }
        }
        break;
        case 'm': {
          BigDecimal v1 = fncargs.next();
          BigDecimal v2 = fncargs.next();
          if (fncnam.equalsIgnoreCase("max")) {
            return (v1.compareTo(v2) >= 0 ? v1 : v2);
          }
          if (fncnam.equalsIgnoreCase("min")) {
            return (v1.compareTo(v2) < 0 ? v1 : v2);
          }
        }
        break;
        case 'n': {
//              if(fncnam.equalsIgnoreCase("nextUp"        )) { return Math.nextUp     (fncargs.next());                } needs Java 6
        }
        break;
        case 'r': {
          if (fncnam.equalsIgnoreCase("random")) {
            return BigDecimal.valueOf(Math.random());
          } // impure
          if (fncnam.equalsIgnoreCase("round")) {
            BigDecimal arg1 = fncargs.next();
            BigDecimal arg2 = BigDecimal.ZERO;
            if (fncargs.hasNext()) {
              arg2 = fncargs.next();
            }
            return arg1.setScale(arg2.intValue(), RoundingMode.HALF_UP);
          }
          if (fncnam.equalsIgnoreCase("roundHE")) {
            BigDecimal arg1 = fncargs.next();
            BigDecimal arg2 = BigDecimal.ZERO;
            if (fncargs.hasNext()) {
              arg2 = fncargs.next();
            }
            return arg1.setScale(arg2.intValue(), RoundingMode.HALF_EVEN);
          } // round half-even
        }
        break;
        case 's': {
          if (fncnam.equalsIgnoreCase("signum")) {
            return new BigDecimal(fncargs.next().compareTo(BigDecimal.ZERO));
          }
          if (fncnam.equalsIgnoreCase("sin")) {
            return BigDecimal.valueOf(Math.sin(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("sinh")) {
            return BigDecimal.valueOf(Math.sinh(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("sqrt")) {
            // TODO BIGDOUBLE Need proper BigDouble implimentation
            return BigDecimal.valueOf(Math.sqrt(fncargs.next().doubleValue()));
          }
        }
        break;
        case 't': {
          if (fncnam.equalsIgnoreCase("tan")) {
            return BigDecimal.valueOf(Math.tan(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("tanh")) {
            return BigDecimal.valueOf(Math.tanh(fncargs.next().remainder(BD_2PI).doubleValue()));
          }
          if (fncnam.equalsIgnoreCase("toDegrees")) {
            return fncargs.next().multiply(BigDecimal.valueOf(180.0 / Math.PI));
          }
          if (fncnam.equalsIgnoreCase("toRadians")) {
            return fncargs.next().divide(BigDecimal.valueOf(180.0 / Math.PI));
          }
        }
        break;
        case 'u': {
          if (fncnam.equalsIgnoreCase("ulp")) {
            // TODO BIGDOUBLE Need proper BigDouble implimentation
            return BigDecimal.valueOf(Math.ulp(fncargs.next().doubleValue()));
          }
        }
        break;
        default:
          // no default
          break;
      }
      throw new UnsupportedOperationException("MathEval internal function setup is incorrect - internal function \"" + fncnam + "\" not handled");
    }


    // To add/remove operators change evaluateOperator() and registration
    public BigDecimal evaluateOperator(BigDecimal lft, char opr, BigDecimal rgt) {

      switch (opr) {
        case '=':
          return rgt;                                                    // simple assignment, used as the final operation, must be maximum precedence
        case '^':
          return lft.pow(rgt.intValue(), MC_HALF_UP);
        // return Math.pow(lft, rgt);                                                    // power
        case '±':
          return rgt.negate();
        // return -rgt;                                                                 // unary negation
        case '*':
          return (lft.multiply(rgt, MC_HALF_UP));
        // return lft * rgt;                                                              // multiply (classical)
        case '×':
          return (lft.multiply(rgt, MC_HALF_UP));
        // return lft * rgt;                                                              // multiply (because it's a Unicode world out there)
        case '·':
          return (lft.multiply(rgt, MC_HALF_UP));
        // return lft * rgt;                                                              // multiply (because it's a Unicode world out there)
        case '(':
          return (lft.multiply(rgt, MC_HALF_UP));
        // return lft * rgt;                                                              // multiply (implicit due to brackets, e.g "(a)(b)")
        case '/':
          return (lft.divide(rgt, DIVIDE_SCALE, DIVIDE_ROUNDING));
        // return lft / rgt;                                                              // divide (classical computing)
        case '÷':
          return (lft.divide(rgt, DIVIDE_SCALE, DIVIDE_ROUNDING));
        // return lft / rgt;                                                              // divide (because it's a Unicode world out there)
        case '%':
          return (lft.remainder(rgt, MC_HALF_UP));
        // return lft % rgt;                                                              // remainder
        case '+':
          return (lft.add(rgt, MC_HALF_UP));
        // return lft + rgt;                                                              // add/unary-positive
        case '-':
          return (lft.subtract(rgt, MC_HALF_UP));
        // return lft - rgt;                                                              // subtract/unary-negative
        default:
          throw new UnsupportedOperationException("MathEval internal operator setup is incorrect - internal operator \"" + opr + "\" not handled");
      }
    }
  }



  /**
   * Operator Structure.
   *
   * <p>This class is immutable and threadsafe, but note that whether it can be used in multiple MathEval instances (as
   * opposed to for multiple operators in one instance) depends on the threadsafety of the handler it contains.
   */
  public static final class Operator
      extends Object {

    final OperatorHandler handler;

    final boolean internal;                                                   // internal pseudo operator

    final int precedenceL;                                                // precedence when on the left

    final int precedenceR;                                                // precedence when on the right

    final char symbol;                                                     // parser symbol for this operator

    final int unary;                                                      // unary operator binding: left, right, or neither


    /**
     * Create a binary operator with the same precedence on the left and right.
     */
    public Operator(char sym, int prc, OperatorHandler hnd) {
      this(sym, prc, prc, NO_SIDE, false, hnd);
    }


    /**
     * Create an operator which may have different left and right precedence and/or may be unary.
     * Using different precedence for one side allows affinity binding such that consecutive operators are evaluated left to right.
     * Marking an operator as unary binds the precedence for the specified side such that it always has maximum precedence when considered from the opposite
     * side.
     */
    public Operator(char sym, int prclft, int prcrgt, int unibnd, OperatorHandler hnd) {

      this(sym, prclft, prcrgt, unibnd, false, hnd);

      if (prclft < 0 || prclft > 99) {
        throw new IllegalArgumentException("Operator precendence must be 0 - 99");
      }
      if (prcrgt < 0 || prcrgt > 99) {
        throw new IllegalArgumentException("Operator precendence must be 0 - 99");
      }
      if (handler == null) {
        throw new IllegalArgumentException("Operator handler is required");
      }
    }


    Operator(char sym, int prclft, int prcrgt, int unibnd, boolean intern, OperatorHandler hnd) {

      symbol = sym;
      precedenceL = prclft;
      precedenceR = prcrgt;
      unary = unibnd;
      internal = intern;
      handler = hnd;
    }


    public String toString() {

      return ("MathOperator['" + symbol + "']");
    }
  }

// *************************************************************************************************
// INSTANCE CREATE/DELETE
// *************************************************************************************************



  /**
   * An abstract parser for function arguments.
   */
  public final class ArgParser {

    final int exEnd;

    int index;


    ArgParser(int excstr, int excend) {

      exEnd = excend;

      index = (excstr + 1);

      index = skipWhitespace(expression, index, exEnd - 1);
    }


    int getIndex() {

      return index;
    }


    /**
     * Test whether there is another argument to parse.
     */
    public boolean hasNext() {

      return (expression.charAt(index) != ')');
    }


    /**
     * Parse the next argument, returning the supplied default if there are no more arguments.
     */
    public BigDecimal next(BigDecimal dft) {

      if (!hasNext()) {
        return dft;
      }
      return this.nextSymbol();
    }


    /**
     * Parse the next argument, throwing an exception if there are no more arguments.
     *
     * @throws ArithmeticException If there are no more arguments.
     */
    public BigDecimal next() {

      if (!hasNext()) {
        throw exception(index, "Function has too few arguments");
      }
      return this.nextSymbol();
    }


    private BigDecimal nextSymbol() {

      if (expression.charAt(index) == ',') {
        index++;
      }
      BigDecimal ret = evaluateSub(index, exEnd);
      index = offset;
      return ret;
    }
  }



  private final SortedMap<String, BigDecimal> constants;                       // external constants

// *************************************************************************************************
// INSTANCE METHODS - ACCESSORS
// *************************************************************************************************

  private final SortedMap<String, FunctionHandler> impureFunctions;         // external pureFunctions

  private final SortedMap<String, FunctionHandler> pureFunctions;           // external pureFunctions

  private final SortedMap<String, BigDecimal> variables;                       // external variables

  private String expression;                      // expression being evaluated

  private boolean isConstant;                      // last expression evaluated is constant

  private int offset;                          // used when returning from a higher precedence sub-expression evaluation

  private Operator[] operators;                       // operators in effect for this parser

  private boolean relaxed;                         // allow variables to be undefined

  private String separators;                      // cache of the operators, used for separators for getVariablesWithin()


  /**
   * Create a math evaluator.
   */
  public MathEval() {
    super();

    operators = new Operator[256];
    DefaultImpl.registerOperators(this);

    constants = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    setConstant("E", Math.E);
    setConstant("Euler", 0.577215664901533D);
    setConstant("LN2", 0.693147180559945D);
    setConstant("LN10", 2.302585092994046D);
    setConstant("LOG2E", 1.442695040888963D);
    setConstant("LOG10E", 0.434294481903252D);
    setConstant("PHI", 1.618033988749895D);
    setConstant("PI", Math.PI);

    pureFunctions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    impureFunctions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    DefaultImpl.registerFunctions(this);

    relaxed = false;
    separators = null;

    offset = 0;
    isConstant = false;
  }


  /**
   * Create a math evaluator with the same constants, variables, function handlers and relaxation setting as the supplied evaluator.
   */
  public MathEval(MathEval oth) {
    super();

    operators = oth.operators;

    constants = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    constants.putAll(oth.constants);

    variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    variables.putAll(oth.variables);

    pureFunctions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    impureFunctions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    pureFunctions.putAll(oth.pureFunctions);
    impureFunctions.putAll(oth.impureFunctions);

    relaxed = oth.relaxed;
    separators = oth.separators;

    offset = 0;
    isConstant = false;
  }


  /**
   * Clear all variables (constants are not affected).
   */
  public MathEval clear() {

    variables.clear();
    return this;
  }


  /**
   * Clear all variables prefixed by the supplied string followed by a dot, such that they match "Prefix.xxx".
   */
  public MathEval clear(String pfx) {

    variables.subMap((pfx + "."), (pfx + "." + Character.MAX_VALUE)).clear();
    return this;
  }


  @SuppressWarnings({"squid:S1143", "squid:S1163"}) // Throw error in finally.
  private BigDecimal doFunction(int beg, int end) {

    int argbeg;

    for (argbeg = beg; argbeg <= end && expression.charAt(argbeg) != '('; argbeg++) {
      ;
    }

    String fncnam = expression.substring(beg, argbeg).trim();
    ArgParser fncargs = new ArgParser(argbeg, end);
    FunctionHandler fnchdl = null;

    try {
      if ((fnchdl = pureFunctions.get(fncnam)) != null) {
        return fnchdl.evaluateFunction(fncnam, fncargs);
      } else if ((fnchdl = impureFunctions.get(fncnam)) != null) {
        isConstant = false;                                                                       // impure functions cannot be guaranteed to be constant
        return fnchdl.evaluateFunction(fncnam, fncargs);
      }
      fncargs = null;                                                                               // suppress check for too many fncargs
    } catch (ArithmeticException thr) {
      fncargs = null;
      throw thr;
    } catch (NoSuchMethodError thr) {
      fncargs = null;
      throw exception(beg, "Function not supported in this JVM: \"" + fncnam + "\"");
    } catch (UnsupportedOperationException thr) {
      fncargs = null;
      throw exception(beg, thr.getMessage());
    } catch (Throwable thr) {
      fncargs = null;
      throw exception(beg, "Unexpected exception parsing function arguments", thr);
    } finally {
      if (fncargs != null) {
        if (fncargs.hasNext()) {
          throw exception(fncargs.getIndex(), "Function has too many arguments");
        }
        offset = fncargs.getIndex();
      }
    }
    throw exception(beg, "Function \"" + fncnam + "\" not recognized");
  }


  private BigDecimal doNamedVal(int beg, int end) {

    while (beg < end && Character.isWhitespace(expression.charAt(end))) {
      end--;
    }                     // since a letter triggers a named value, this can never reduce to beg==end

    String nam = expression.substring(beg, (end + 1));
    BigDecimal val;

    if ((val = constants.get(nam)) != null) {
      return val;
    } else if ((val = variables.get(nam)) != null) {
      isConstant = false;
      return val;
    } else if (relaxed) {
      isConstant = false;
      return BigDecimal.ZERO;
    }

    throw exception(beg, "Unrecognized constant or variable \"" + nam + "\"");
  }

// *************************************************************************************************
// INSTANCE METHODS - PUBLIC API
// *************************************************************************************************


  private BigDecimal doOperation(int beg, BigDecimal lft, Operator opr, BigDecimal rgt) {

    if (opr.unary != RIGHT_SIDE && (lft == null)) {
      throw exception(beg, "Mathematical NaN detected in right-operand");
    }
    if (opr.unary != LEFT_SIDE && (rgt == null)) {
      throw exception(beg, "Mathematical NaN detected in left-operand");
    }

    try {
      return opr.handler.evaluateOperator(lft, opr.symbol, rgt);
    } catch (ArithmeticException thr) {
      throw exception(beg, "Mathematical expression \"" + expression + "\" failed to evaluate", thr);
    } catch (UnsupportedOperationException thr) {
      int tmp = beg;
      while (tmp > 0 && getOperator(expression.charAt(tmp)) == null) {
        tmp--;
      }                        // set up for offset of the offending operator
      throw exception(tmp,
          "Operator \"" + opr.symbol + "\" not handled by math engine (Programmer error: The list of operators is inconsistent within the engine)",thr);
    }
  }


  /**
   * Evaluate this expression.
   */
  public BigDecimal evaluate(String exp) {

    expression = exp;
    isConstant = true;
    offset = 0;
    return evaluateSub(0, (exp.length() - 1));
  }


  /**
   * Evaluate a complete (sub-)expression.
   *
   * @param beg Inclusive begin offset for subexpression.
   * @param end Inclusive end offset for subexpression.
   */
  private BigDecimal evaluateSub(int beg, int end) {
    return evaluateSub(beg, end, BigDecimal.ZERO, OPERAND, getOperator('='));
  }

// *************************************************************************************************
// INSTANCE METHODS - PRIVATE IMPLEMENTATION
// *************************************************************************************************


  /**
   * Evaluate the next operand of an expression.
   *
   * @param beg Inclusive begin offset for subexpression.
   * @param end Inclusive end offset for subexpression.
   * @param pnd Pending operator (operator previous to this subexpression).
   * @param lft Left-value with which to initialize this subexpression.
   * @param cur Current operator (the operator for this subexpression).
   */
  private BigDecimal evaluateSub(int beg, int end, BigDecimal lft, Operator pnd, Operator cur) {

    Operator nxt = OPERAND;                                                // next operator
    int ofs;                                                        // current expression offset

    for (ofs = beg; (ofs = skipWhitespace(expression, ofs, end)) <= end; ofs++) {
      BigDecimal rgt = null;                                             // next operand (right-value) to process

      for (beg = ofs; ofs <= end; ofs++) {
        char chr = expression.charAt(ofs);
        if ((nxt = getOperator(chr)) != OPERAND) {
          if (nxt.internal) {
            nxt = OPERAND;
          } else {
            break;
          }
        } else if (chr == ')' || chr == ',') {       // end of subexpression or function argument.
          break;
        }
      }

      char ch0 = expression.charAt(beg);
      boolean alp = Character.isLetter(ch0);

      if (cur.unary != LEFT_SIDE) {
        if (ch0 == '+') {
          continue;
        }                                              // unary '+': no-op; i.e. +(-1) == -1
        if (ch0 == '-') {
          nxt = getOperator('±');
        }                                              // unary '-': right-binding, high precedence operation (different from subtract)
      }

      if (beg == ofs && (cur.unary == LEFT_SIDE || nxt.unary == RIGHT_SIDE)) {
        rgt = null;                         // left-binding unary operator; right value will not be used and should be blank
      } else if (ch0 == '(') {
        rgt = evaluateSub(beg + 1, end);
        ofs = skipWhitespace(expression, offset + 1, end);                     // skip past ')' and any following whitespace
        nxt = (ofs <= end ? getOperator(expression.charAt(ofs)) : OPERAND);    // modify next operator
      } else if (alp && nxt.symbol == '(') {
        rgt = doFunction(beg, end);
        ofs = skipWhitespace(expression, offset + 1, end);                     // skip past ')' and any following whitespace
        nxt = (ofs <= end ? getOperator(expression.charAt(ofs)) : OPERAND);    // modify next operator
      } else if (alp) {
        rgt = doNamedVal(beg, (ofs - 1));
      } else {
        try {
          if (stringOfsEq(expression, beg, "0x")) {
            rgt = new BigDecimal(new BigInteger(expression.substring(beg + 2, ofs).trim(), 16));
          } else {
            rgt = new BigDecimal(expression.substring(beg, ofs).trim());
          }
        } catch (NumberFormatException thr) {
          throw exception(beg, "Invalid numeric value \"" + expression.substring(beg, ofs).trim() + "\"",thr);
        }
      }

      if (opPrecedence(cur, LEFT_SIDE) < opPrecedence(nxt,
          RIGHT_SIDE)) {               // correct even for last (non-operator) character, since non-operators have the artificial "precedence" zero
        rgt = evaluateSub((ofs + 1), end, rgt, cur, nxt);                                // from after operator to end of current subexpression
        ofs = offset;                                                                  // modify offset to after subexpression
        nxt = (ofs <= end ? getOperator(expression.charAt(ofs)) : OPERAND);            // modify next operator
      }

      lft = doOperation(beg, lft, cur, rgt);

      cur = nxt;
      if (opPrecedence(pnd, LEFT_SIDE) >= opPrecedence(cur, RIGHT_SIDE)) {
        break;
      }
      if (cur.symbol == '(') {
        ofs--;
      }                                                              // operator omitted for implicit multiplication of subexpression
    }
    if (ofs > end && cur != OPERAND) {
      if (cur.unary == LEFT_SIDE) {
        lft = doOperation(beg, lft, cur, null);
      } else {
        throw exception(ofs, "Expression ends with a blank operand after operator '" + nxt.symbol + "'");
      }
    }
    offset = ofs;
    return lft;
  }


  private ArithmeticException exception(int ofs, String txt) {

    return new ArithmeticException(txt + " at offset " + ofs + " in expression \"" + expression + "\"");
  }


  private ArithmeticException exception(int ofs, String txt, Throwable thr) {

    return new ArithmeticException(
        txt + " at offset " + ofs + " in expression \"" + expression + "\"" + " (Cause: " + (thr.getMessage() != null ? thr.getMessage() : thr.toString())
            + ")");
  }


  /**
   * Set a named constant (constant names are not case-sensitive).  Constants are like variables but are not cleared by clear().
   * Variables of the same name have precedence over constants.
   */
  public BigDecimal getConstant(String nam) {
    BigDecimal val = constants.get(nam);

    return (val == null ? BigDecimal.ZERO : val);
  }


  /** Gets an unmodifiable iterable of the constants in this evaluator. */
  public Iterable<Map.Entry<String, BigDecimal>> getConstants() {
    return Collections.unmodifiableMap(constants).entrySet();
  }


  private Operator getOperator(char chr) {

    if (chr < operators.length) {
      Operator opr = operators[chr];
      if (opr != null) {
        return opr;
      }
    }
    return OPERAND;
  }


  /**
   * Set a named variable (variables names are not case-sensitive).
   */
  public BigDecimal getVariable(String nam) {

    BigDecimal val = variables.get(nam);

    return (val == null ? BigDecimal.ZERO : val);
  }


  /**
   * Get whether a variable which is used in an expression is required to be explicitly set. If not explicitly set, the value 0.0 is assumed.
   */
  public boolean getVariableRequired() {

    return relaxed;
  }


  /**
   * Gets an unmodifiable iterable of the variables in this evaluator.
   */
  public Iterable<Map.Entry<String, BigDecimal>> getVariables() {

    return Collections.unmodifiableMap(variables).entrySet();
  }


  /** Return a set of the variables in the supplied expression. Note: Substitutions which are in the constant table are not included. */
  public Set<String> getVariablesWithin(String exp) {

    Set<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    String add = null;

    if (separators == null) {
      StringBuilder sep = new StringBuilder(10);
      for (char chr = 0; chr < operators.length; chr++) {
        if (operators[chr] != null && !operators[chr].internal) {
          sep.append(chr);
        }
      }
      sep.append("()");
      separators = sep.toString();
    }

    for (StringTokenizer tkz = new StringTokenizer(exp, separators, true); tkz.hasMoreTokens(); ) {
      String tkn = tkz.nextToken().trim();

      if (tkn.length() != 0 && Character.isLetter(tkn.charAt(0))) {
        add = tkn;
      } else if (tkn.length() == 1 && tkn.charAt(0) == '(') {
        add = null;
      } else if (add != null && !constants.containsKey(add)) {
        all.add(add);
      }
    }
    if (add != null && !constants.containsKey(add)) {
      all.add(add);
    }
    return all;
  }


  private int opPrecedence(Operator opr, int sid) {

    if (opr == null) {
      return Integer.MIN_VALUE;
    } else if (opr.unary == NO_SIDE || opr.unary != sid) {
      return (sid == LEFT_SIDE ? opr.precedenceL : opr.precedenceR);
    } else {
      return Integer.MAX_VALUE;
    } // operator is unary and associates with the operand on this side
  }

// *************************************************************************************************
// INSTANCE INNER CLASSES - FUNCTION ARGUMENT PARSER
// *************************************************************************************************


  /**
   * Return whether the previous expression evaluated was constant (i.e. contained no variables).
   * This is useful when optimizing to store the result instead of repeatedly evaluating a constant expression like "2+2".
   */
  public boolean previousExpressionConstant() {
    return isConstant;
  }

// *************************************************************************************************
// STATIC NESTED CLASSES - OPERATOR
// *************************************************************************************************


  /**
   * Set a named constant (constants names are not case-sensitive).  Constants are like variables but are not cleared by clear(). Variables of the same name
   * have precedence over constants.
   */
  public MathEval setConstant(String nam, Object val) {

    if (constants.get(nam) != null) {
      throw new IllegalArgumentException("Constants may not be redefined");
    }

    BigDecimal toSet;

    if (val instanceof BigDecimal) {
      toSet = (BigDecimal) val;
    } else if (val instanceof BigInteger) {
      toSet = new BigDecimal((BigInteger) val);
    } else if (val instanceof Long) {
      toSet = BigDecimal.valueOf((Long) val);
    } else if (val instanceof Integer) {
      toSet = BigDecimal.valueOf(((Integer) val).longValue());
    } else if (val instanceof Number) {
      toSet = BigDecimal.valueOf(((Number) val).doubleValue());
    } else {
      toSet = new BigDecimal(val.toString());
    }

    validateName(nam);
    constants.put(nam, toSet);
    return this;
  }

// *************************************************************************************************
// STATIC NESTED CLASSES - OPERATION EVALUATOR INTERFACE
// *************************************************************************************************


  /**
   * Set a pure function handler for the specific named function, replacing any existing handler for
   * the given name; if the handler is null the function handler is removed.
   * Pure functions have results which depend purely on their arguments; given constant arguments they
   * will have a constant result.  Impure functions are rare.
   */
  public MathEval setFunctionHandler(String nam, FunctionHandler hdl) {
    return setFunctionHandler(nam, hdl, false);
  }

// *************************************************************************************************
// STATIC NESTED CLASSES - FUNCTION EVALUATOR INTERFACE
// *************************************************************************************************


  /**
   * Set a function handler for the specific named function optionally tagging the function as impure,
   * replacing any existing handler for the given name; if the handler is null the function handler is removed
   * Pure functions have results which depend purely on their arguments; given constant arguments they
   * will have a constant result.  Impure functions are rare.
   */
  public MathEval setFunctionHandler(String nam, FunctionHandler hdl, boolean impure) {

    validateName(nam);
    if (hdl == null) {
      pureFunctions.remove(nam);
      impureFunctions.remove(nam);
    } else if (impure) {
      pureFunctions.remove(nam);
      impureFunctions.put(nam, hdl);
    } else {
      pureFunctions.put(nam, hdl);
      impureFunctions.remove(nam);
    }
    return this;
  }

// *************************************************************************************************
// STATIC NESTED CLASSES - DEFAULT OPERATOR/FUNCTION IMPLEMENTATION
// *************************************************************************************************


  /**
   * Set a custom operator, replacing any existing operator with the same symbol. Operators cannot be removed, only replaced.
   */
  public MathEval setOperator(Operator opr) {

    if (opr.symbol >= operators.length) {                                                              // extend the array if necessary
      Operator[] noa = new Operator[opr.symbol + (opr.symbol % 255) + 1];                                 // use allocation pages of 256
      System.arraycopy(operators, 0, noa, 0, operators.length);
      operators = noa;
    }
    operators[opr.symbol] = opr;
    return this;
  }

// *************************************************************************************************
// STATIC PROPERTIES
// *************************************************************************************************


  /**
   * Set a named variable (variables names are not case-sensitive).
   */
  public MathEval setVariable(String nam, double val) {

    return setVariable(nam, BigDecimal.valueOf(val));
  }


  /**
   * Set a named variable (variables names are not case-sensitive). If the value is null, the variable is removed.
   */
  public MathEval setVariable(String nam, Object val) {

    validateName(nam);

    if (val == null) {
      variables.remove(nam);
    } else {
      BigDecimal toSet;

      if (val instanceof BigDecimal) {
        toSet = (BigDecimal) val;
      } else if (val instanceof BigInteger) {
        toSet = new BigDecimal((BigInteger) val);
      } else if (val instanceof Long) {
        toSet = BigDecimal.valueOf((Long) val);
      } else if (val instanceof Integer) {
        toSet = BigDecimal.valueOf(((Integer) val).longValue());
      } else if (val instanceof Number) {
        toSet = BigDecimal.valueOf(((Number) val).doubleValue());
      } else {
        toSet = new BigDecimal(val.toString());
      }

      variables.put(nam, toSet);
    }

    return this;
  }


  /**
   * Set whether a variable which is used in an expression is required to be explicitly set. If not explicitly set, the value 0.0 is assumed.
   */
  public MathEval setVariableRequired(boolean val) {

    relaxed = (!val);
    return this;
  }


  private int skipWhitespace(String exp, int ofs, int end) {

    while (ofs <= end && Character.isWhitespace(exp.charAt(ofs))) {
      ofs++;
    }
    return ofs;
  }


  private boolean stringOfsEq(String str, int ofs, String val) {

    return str.regionMatches(true, ofs, val, 0, val.length());
  }


  private void validateName(String nam) {

    if ((nam == null) || nam.isEmpty()) {
      throw new IllegalArgumentException("Names for constants, variables and functions must not be null");
    }

    if (!Character.isLetter(nam.charAt(0))) {
      throw new IllegalArgumentException("Names for constants, variables and functions must start with a letter");
    }

    if (nam.indexOf('(') != -1 || nam.indexOf(')') != -1) {
      throw new IllegalArgumentException("Names for constants, variables and functions may not contain a parenthesis");
    }
  }

// *************************************************************************************************
// STATIC METHODS - UTILITY
// *************************************************************************************************

} // END CLASS