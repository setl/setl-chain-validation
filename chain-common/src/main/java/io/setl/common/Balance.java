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
package io.setl.common;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.simple.JSONAware;
import org.json.simple.JSONStreamAware;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.msgpack.SetlExtensions;

/**
 * Balance :  Class to store an integer value.
 * Class will use an internal `Long` or `BigInteger` as required.
 * Value is immutable one created.
 */
public class Balance extends Number implements MsgPackable, Comparable<Number>, JSONAware, JSONStreamAware {

  public static final Balance BALANCE_ZERO;

  public static final BigInteger BIGINT_MAXLONG = BigInteger.valueOf(Long.MAX_VALUE);

  public static final BigInteger BIGINT_MINLONG = BigInteger.valueOf(Long.MIN_VALUE);

  private static final long HALF_LONG = (Long.MAX_VALUE / 2);

  private static final MathContext MC_HALFUP = new MathContext(0, RoundingMode.HALF_UP);


  /**
   * abs() : Return Balance instance representing the absolute (+ve) value of the parameter.
   *
   * @param b1 :
   */
  public static Balance abs(Balance b1) {

    if (b1 == null) {
      return BALANCE_ZERO;
    }

    if (b1.compareTo(BALANCE_ZERO) >= 0) {
      return b1;
    }

    Number value = b1.getValue();

    if (value instanceof BigInteger) {
      return new Balance(((BigInteger) value).abs());
    } else {
      return new Balance(Math.abs(value.longValue()));
    }
  }


  /**
   * max() : Return Balance instance representing larger (more +ve) of the given two parameters.
   *
   * @param b1 :
   * @param b2 :
   */
  public static Balance max(Balance b1, Balance b2) {

    if (b1 == null) {
      return b2;
    }
    if (b2 == null) {
      return b1;
    }

    return (b1.compareTo(b2) >= 0 ? b1 : b2);
  }


  /**
   * min() : Return Balance instance representing smaller (more -ve) of the given two parameters.
   *
   * @param b1 :
   * @param b2 :
   */
  public static Balance min(Balance b1, Balance b2) {

    if (b1 == null) {
      return b2;
    }
    if (b2 == null) {
      return b1;
    }

    return (b1.compareTo(b2) <= 0 ? b1 : b2);
  }


  static {
    BALANCE_ZERO = new Balance(0L);
  }

  protected final BigInteger bigIntValue;

  protected final Long longValue;


  @JsonCreator
  public Balance(Object number) {
    this(number, 10, MC_HALFUP);
  }


  public Balance(MessageUnpacker unpacker) throws IOException {
    MessageFormat format = unpacker.getNextFormat();
    if (format.getValueType().isIntegerType()) {
      longValue = unpacker.unpackLong();
      bigIntValue = null;
    } else {
      // Assume it is a big integer
      BigInteger bi = (BigInteger) SetlExtensions.unpack(unpacker);
      if ((BIGINT_MAXLONG.compareTo(bi) >= 0) && (BIGINT_MINLONG.compareTo(bi) <= 0)) {
        longValue = bi.longValue();
        bigIntValue = null;
      } else {
        bigIntValue = bi;
        longValue = null;
      }
    }
  }


  /**
   * Balance Constructor.
   *
   * @param number      :
   * @param radix       :
   * @param mathContext :
   */
  public Balance(Object number, int radix, MathContext mathContext) {

    if (number == null) {
      longValue = 0L;
      bigIntValue = null;
      return;
    }

    Object thisNumber = number;

    if (number instanceof String) {
      thisNumber = new BigInteger((String) number, radix);
    }

    if (number instanceof BigDecimal) {
      thisNumber = ((BigDecimal) number).round(mathContext).toBigInteger();
    }

    if (thisNumber instanceof BigInteger) {
      if ((BIGINT_MAXLONG.compareTo((BigInteger) thisNumber) >= 0) && (BIGINT_MINLONG.compareTo((BigInteger) thisNumber) <= 0)) {
        longValue = ((BigInteger) thisNumber).longValue();
        bigIntValue = null;
      } else {
        bigIntValue = (BigInteger) thisNumber;
        longValue = null;
      }

    } else if (thisNumber instanceof Balance) {
      longValue = ((Balance) thisNumber).longValue;
      bigIntValue = ((Balance) thisNumber).bigIntValue;

    } else if (thisNumber instanceof Number) {
      longValue = ((Number) thisNumber).longValue();
      bigIntValue = null;

    } else {
      longValue = 0L;
      bigIntValue = null;
    }
  }


  /**
   * add() : Return Balance instance representing the sum of the current instance and the given parameter.
   *
   * @param toAdd :
   */
  public Balance add(Number toAdd) {

    if (toAdd == null) {
      return this;
    }

    if (toAdd instanceof Balance) {
      return this.add(((Balance) toAdd).getValue());
    }

    if (bigIntValue != null) {
      if (toAdd instanceof BigInteger) {
        return new Balance(bigIntValue.add((BigInteger) toAdd));
      } else {
        return new Balance(bigIntValue.add(BigInteger.valueOf(toAdd.longValue())));
      }
    } else {
      if (toAdd instanceof BigInteger) {
        return new Balance(((BigInteger) toAdd).add(BigInteger.valueOf(longValue)));
      } else {
        if ((Math.abs(longValue) >= HALF_LONG) || (Math.abs(toAdd.longValue()) >= HALF_LONG)) {
          try {
            // A bit of a fudge, but should be OK.
            return new Balance(Math.addExact(longValue, toAdd.longValue()));
          } catch (ArithmeticException e) {
            return new Balance(BigInteger.valueOf(longValue).add(BigInteger.valueOf(toAdd.longValue())));
          }
        } else {
          return new Balance(longValue + toAdd.longValue());
        }
      }
    }
  }


  public BigInteger bigintValue() {

    return (bigIntValue != null ? bigIntValue : ((longValue == 0L) ? BigInteger.ZERO : BigInteger.valueOf(longValue)));
  }


  @Override
  public int compareTo(Number toCompare) {

    if (toCompare == null) {
      return this.compareTo(BALANCE_ZERO);
    }

    if (toCompare instanceof Balance) {
      return this.compareTo(((Balance) toCompare).getValue());
    }

    if (longValue != null) {
      if (toCompare instanceof BigInteger) {
        return BigInteger.valueOf(longValue).compareTo((BigInteger) toCompare);

      } else {
        if (longValue < toCompare.longValue()) {
          return -1;
        } else if (longValue == toCompare.longValue()) {
          return 0;
        }

        return 1;
      }
    } else {
      // BigInt
      if (toCompare instanceof BigInteger) {
        return bigIntValue.compareTo((BigInteger) toCompare);
      } else {
        return bigIntValue.compareTo(BigInteger.valueOf(toCompare.longValue()));
      }
    }

  }


  /**
   * Integer division of Balance.
   * <p>The Balance is, of course, a whole number, the divisor must be (will be treated as) a whole number !</p>
   *
   * @param toDivideBy :
   *
   * @return : new Balance(result)
   */
  public Balance divideBy(Number toDivideBy) {

    if (toDivideBy == null) {
      return BALANCE_ZERO;
    }

    if (toDivideBy instanceof Balance) {
      return this.divideBy(((Balance) toDivideBy).getValue());
    }

    if (bigIntValue != null) {
      if (toDivideBy instanceof BigInteger) {
        return new Balance(bigIntValue.divide((BigInteger) toDivideBy));
      } else {
        return new Balance(bigIntValue.divide(BigInteger.valueOf(toDivideBy.longValue())));
      }
    } else {
      if (toDivideBy instanceof BigInteger) {
        return new Balance(BigInteger.valueOf(longValue).divide((BigInteger) toDivideBy));
      } else {
        return new Balance(longValue / toDivideBy.longValue());
      }
    }
  }


  @Override
  public double doubleValue() {

    return getValue().doubleValue();
  }


  /**
   * Return true if the 'value' of this Balance equals the given value.
   *
   * @param toCompare :
   *
   * @return :
   */
  public boolean equalTo(Object toCompare) {

    if (toCompare == null || !(toCompare instanceof Number)) {
      return false;
    }

    Number thisCompare = (Number) toCompare;

    if (toCompare instanceof Balance) {
      thisCompare = ((Balance) toCompare).getValue();
    }

    if (thisCompare instanceof BigInteger) {
      return thisCompare.equals(this.bigintValue());
    }

    if (bigIntValue == null) {
      return (thisCompare.longValue() == longValue);
    }
    return bigIntValue.equals(BigInteger.valueOf(thisCompare.longValue()));
  }


  /**
   * Return true if this Balance is numerically equal to Zero.
   *
   * @return true if this is equal to zero
   */
  public boolean equalZero() {
    if (this.longValue != null) {
      return this.longValue.longValue() == 0;
    }
    return this.bigIntValue.signum() == 0;
  }


  @SuppressFBWarnings("EQ_UNUSUAL") // This may be unusual, but it is also correct
  @Override
  public boolean equals(Object toCompare) {
    return this.equalTo(toCompare);
  }


  @Override
  public float floatValue() {

    return getValue().floatValue();
  }


  @JsonValue
  public Number getValue() {

    return (longValue == null ? bigIntValue : longValue);
  }


  /**
   * greaterThan() : Return true if Instance is greater than the given parameter.
   *
   * @param toCompare :
   */
  public boolean greaterThan(Number toCompare) {

    return (this.compareTo(toCompare) > 0);

  }


  /**
   * greaterThanEqualTo() : Return true if Instance is greater than or equal to the given parameter.
   *
   * @param toCompare :
   */
  public boolean greaterThanEqualTo(Number toCompare) {

    return (this.compareTo(toCompare) >= 0);

  }


  /**
   * greaterThanEqualZero() : Return true if Instance is greater than or equal to zero.
   */
  public boolean greaterThanEqualZero() {

    if (this.longValue != null) {
      return (this.compareTo(0L) >= 0);
    } else {
      return (this.compareTo(BALANCE_ZERO) >= 0);
    }
  }


  /**
   * greaterThanZero() : Return true if Instance is greater than zero.
   */
  public boolean greaterThanZero() {

    if (this.longValue != null) {
      return (this.compareTo(0L) > 0);
    } else {
      return (this.compareTo(BALANCE_ZERO) > 0);
    }
  }


  @Override
  public int hashCode() {

    return this.getValue().hashCode();

  }


  @Override
  public int intValue() {

    return getValue().intValue();
  }


  /**
   * Return true is this Balance is numerically less than the given Balance.
   *
   * @param toCompare :
   *
   * @return :
   */
  public boolean lessThan(Number toCompare) {

    return (this.compareTo(toCompare) < 0);

  }


  /**
   * Return true is this Balance is numerically less or equal to the given Balance.
   *
   * @param toCompare :
   *
   * @return :
   */
  public boolean lessThanEqualTo(Number toCompare) {

    return (this.compareTo(toCompare) <= 0);

  }


  /**
   * Return true is this Balance is numerically less than or equal to Zero.
   *
   * @return :
   */
  public boolean lessThanEqualZero() {

    if (this.longValue != null) {
      return (this.compareTo(0L) <= 0);
    } else {
      return (this.compareTo(BALANCE_ZERO) <= 0);
    }
  }


  /**
   * Return true is this Balance is numerically less than Zero.
   *
   * @return :
   */
  public boolean lessThanZero() {

    if (this.longValue != null) {
      return (this.compareTo(0L) < 0);
    } else {
      return (this.compareTo(BALANCE_ZERO) < 0);
    }
  }


  @Override
  public long longValue() {

    return getValue().longValue();
  }


  /**
   * Return the mathematical modulus of this Balance and the given Number.
   *
   * @param toModulus :
   *
   * @return :
   */
  public Balance modulus(Number toModulus) {

    if ((toModulus == null) || (BALANCE_ZERO.equalTo(toModulus))) {
      return BALANCE_ZERO;
    }

    if (toModulus instanceof Balance) {
      return this.modulus(((Balance) toModulus).getValue());
    }

    if (bigIntValue != null) {
      if (toModulus instanceof BigInteger) {
        return new Balance(bigIntValue.mod(((BigInteger) toModulus).abs()));
      } else {
        return new Balance(bigIntValue.mod(BigInteger.valueOf(Math.abs(toModulus.longValue()))));
      }
    } else {
      if (toModulus instanceof BigInteger) {
        return new Balance(BigInteger.valueOf(longValue).mod((BigInteger) ((BigInteger) toModulus).abs()));
      } else {
        try {
          return new Balance(longValue % Math.abs(toModulus.longValue()));
        } catch (ArithmeticException e) {
          return new Balance(BigInteger.valueOf(longValue).mod(BigInteger.valueOf(toModulus.longValue()).abs()));
        }
      }
    }
  }


  /**
   * Return the mathematical product of this Balance and the given Balance.
   *
   * @param toMultiply :
   *
   * @return :
   */
  public Balance multiplyBy(Number toMultiply) {

    if (toMultiply == null) {
      return BALANCE_ZERO;
    }

    if (toMultiply instanceof Balance) {
      return this.multiplyBy(((Balance) toMultiply).getValue());
    }

    if (bigIntValue != null) {
      if (toMultiply instanceof BigInteger) {
        return new Balance(bigIntValue.multiply((BigInteger) toMultiply));
      } else {
        return new Balance(bigIntValue.multiply(BigInteger.valueOf(toMultiply.longValue())));
      }
    } else {
      if (toMultiply instanceof BigInteger) {
        return new Balance(((BigInteger) toMultiply).multiply(BigInteger.valueOf(longValue)));
      } else {
        try {
          return new Balance(Math.multiplyExact(longValue, toMultiply.longValue()));
        } catch (ArithmeticException e) {
          return new Balance(BigInteger.valueOf(longValue).multiply(BigInteger.valueOf(toMultiply.longValue())));
        }
      }
    }
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    Number n = getValue();
    if (n instanceof BigInteger) {
      SetlExtensions.pack(p, (BigInteger) n);
    } else {
      p.packLong(n.longValue());
    }
  }


  /**
   * Return the result of subtracting the given Balance from this Balance.
   *
   * @param toSubtract :
   *
   * @return :
   */
  public Balance subtract(Number toSubtract) {

    if (toSubtract == null) {
      return this;
    }

    if (toSubtract instanceof Balance) {
      return this.subtract(((Balance) toSubtract).getValue());
    } else if (toSubtract instanceof BigInteger) {
      return this.add(((BigInteger) toSubtract).negate());
    } else {
      return this.add(-toSubtract.longValue());
    }
  }


  @Override
  public String toJSONString() {
    // Simple-JSON Interface method.

    return this.getValue().toString();

  }


  @Override
  public String toString() {

    return this.getValue().toString();

  }


  @Override
  public void writeJSONString(Writer out) throws IOException {
    // Simple-JSON Interface streaming method.

    out.write(this.getValue().toString());

  }

}
