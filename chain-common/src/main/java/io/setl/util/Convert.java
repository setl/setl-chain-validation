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
package io.setl.util;

import static java.lang.Math.toIntExact;

import java.util.Date;

/**
 * Simple Number : String conversion utility.
 * TODO : Consolidate with other similar utilities ?
 * See also TypeSafeMap.
 */
public class Convert {

  /**
   * @param toConvert : Object to convert to double.
   *
   * @return : double value
   */
  public static double objectToDouble(Object toConvert) {

    if (toConvert instanceof Double) {
      return (Double) toConvert;
    }

    if (toConvert instanceof Number) {
      return ((Number) toConvert).doubleValue();
    } else if (toConvert instanceof Date) {
      // is this correct? seconds since epoch to be consistent with everywhere else? julian day?
      return ((Date) toConvert).getTime();
    } else {
      return Double.parseDouble(toConvert.toString());
    }

  }


  /**
   * @param toConvert : Object to convert to integer.
   *
   * @return : integer value
   */
  public static int objectToInt(Object toConvert) {

    if (toConvert instanceof Integer) {
      return (Integer) toConvert;
    }

    if (toConvert instanceof Number) {
      return ((Number) toConvert).intValue();
    } else if (toConvert instanceof Date) {
      // Surely this is wrong? Perhaps a second-since the epoch, not milliseconds?
      return toIntExact(((Date) toConvert).getTime());
    } else {
      return Integer.decode(toConvert.toString());
    }

  }


  /**
   * @param toConvert : Object to convert to Long.
   *
   * @return : long value
   */
  public static long objectToLong(Object toConvert, long defaultValue) {

    if (toConvert == null) {
      return defaultValue;
    }

    if (toConvert instanceof Number) {
      return ((Number) toConvert).longValue();
    }

    return defaultValue;
  }


  /**
   * @param toConvert : Object to convert to String.
   *
   * @return : String value
   */
  public static String objectToString(Object toConvert, String defaultString) {

    if (toConvert == null) {
      return defaultString;
    }

    return toConvert.toString();

  }


  /**
   * @param toConvert : Object to convert to String.
   *
   * @return : String value
   */
  public static String objectToString(Object toConvert) {

    if (toConvert == null) {
      return "";
    }

    return toConvert.toString();

  }

}
