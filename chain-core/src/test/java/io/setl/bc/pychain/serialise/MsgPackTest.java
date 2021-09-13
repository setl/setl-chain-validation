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
package io.setl.bc.pychain.serialise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.setl.common.Hex;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;
import org.junit.Test;
import org.msgpack.jackson.dataformat.MessagePackExtensionType;

/**
 * @author Simon Greatrix on 2019-05-29.
 */
public class MsgPackTest {

  public static class Pojo {

    private BigInteger balance;

    private Pojo child;

    private int count;

    private String text;

    private long value;


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Pojo)) {
        return false;
      }
      Pojo pojo = (Pojo) o;
      return count == pojo.count &&
          value == pojo.value &&
          Objects.equals(balance, pojo.balance) &&
          Objects.equals(text, pojo.text) &&
          Objects.equals(child, pojo.child);
    }


    public BigInteger getBalance() {
      return balance;
    }


    public Pojo getChild() {
      return child;
    }


    public int getCount() {
      return count;
    }


    public String getText() {
      return text;
    }


    public long getValue() {
      return value;
    }


    @Override
    public int hashCode() {
      return Objects.hash(balance, count, text, value, child);
    }


    void init() {
      Random random = new Random();
      text = Long.toString(random.nextLong(), 36);
      count = random.nextInt();
      value = random.nextLong();
      balance = BigInteger.valueOf(random.nextLong()).shiftLeft(32).xor(BigInteger.valueOf(random.nextLong()));
    }


    public void setBalance(BigInteger balance) {
      this.balance = balance;
    }


    public void setChild(Pojo child) {
      this.child = child;
    }


    public void setCount(int count) {
      this.count = count;
    }


    public void setText(String text) {
      this.text = text;
    }


    public void setValue(long value) {
      this.value = value;
    }


    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Pojo{");
      sb.append("balance=").append(balance);
      sb.append(", child=").append(child);
      sb.append(", count=").append(count);
      sb.append(", text='").append(text).append('\'');
      sb.append(", value=").append(value);
      sb.append('}');
      return sb.toString();
    }
  }


  private void bd(Object obj, String hex) throws IOException {
    ObjectWriter writer = MsgPack.writer();
    byte[] bytes = writer.writeValueAsBytes(obj);
    String actual = Hex.encode(bytes);
    assertEquals(hex, actual);

    // canonicalize the input
    if (obj != null) {
      obj = new BigDecimal(obj.toString()).stripTrailingZeros();
    }

    ObjectReader reader = MsgPack.reader(Number.class);
    Object out = reader.readValue(bytes);

    // canonicalize the output for comparison
    if (out != null) {
      out = new BigDecimal(out.toString()).stripTrailingZeros();
    }
    assertEquals(String.valueOf(obj), String.valueOf(out));

    reader = MsgPack.reader(BigDecimal.class);
    out = reader.readValue(bytes);
    assertEquals(String.valueOf(obj), String.valueOf(out));
  }


  private void bi(Object obj, String hex) throws IOException {
    ObjectWriter writer = MsgPack.writer();
    byte[] bytes = writer.writeValueAsBytes(obj);
    String actual = Hex.encode(bytes);
    assertEquals(hex, actual);

    ObjectReader reader = MsgPack.reader(Number.class);
    Object out = reader.readValue(bytes);
    assertEquals(String.valueOf(obj), String.valueOf(out));

    reader = MsgPack.reader(BigInteger.class);
    out = reader.readValue(bytes);
    assertEquals(String.valueOf(obj), String.valueOf(out));
  }


  @Test
  public void bigDecimal() throws IOException {
    // CB is double precision
    bd(0.5, "cb3fe0000000000000");

    // CA is single precision
    bd(0.5f, "ca3f000000");

    // Integer converts to BigDecimal
    bd(5, "05");

    bd(BigDecimal.ZERO, "00");

    // BigDecimal whole number stays as BigDecimal
    bd(BigDecimal.valueOf(5.0), "d5020005");

    // 15 digits is a double
    bd(new BigDecimal("1234567890.12345"), "c70a0205cf00007048860ddf79");

    // 16 digits is a BigDecimal
    bd(new BigDecimal("12345.67890123456"), "c70a020bcf000462d53c8abac0");

    // Ordinary BigDecimal ...
    bd(new BigDecimal("100000000000000000001"), "c70d0200c70901056bc75e2d63100001");
    bd(new BigDecimal("1000000000000000000"), "d502ee01");
    bd(new BigDecimal("100000000000000000000"), "d502ec01");

    // BigDecimal with BigInteger unscaled value
    bd(new BigDecimal("1000000000000.00000001"), "c70d0208c70901056bc75e2d63100001");

    // BigInteger converts to BigDecimal
    bd(new BigInteger("10000000000000000", 16), "c70901010000000000000000");

    // UINT-64
    bd(new BigDecimal(new BigInteger("8765432187654321", 16)).scaleByPowerOfTen(-8), "c70a0208cf8765432187654321");

    bd(null, "c0");
  }


  @Test
  public void bigInteger() throws IOException {
    bi(1, "01");
    bi(2L, "02");
    bi(new BigInteger("3"), "03");

    // D2 = INT-32, with 2's complement
    bi(BigInteger.valueOf(Integer.MIN_VALUE), "d280000000");

    // CE = UINT-32
    bi(BigInteger.valueOf(Integer.MAX_VALUE), "ce7fffffff");

    // D3 = INT-64, with 2's complement
    bi(BigInteger.valueOf(Long.MIN_VALUE), "d38000000000000000");
    bi(new BigInteger("-4000000000000000", 16), "d3c000000000000000");

    // CF = UINT-64
    bi(BigInteger.valueOf(Long.MAX_VALUE), "cf7fffffffffffffff");
    bi(new BigInteger("8765432187654321", 16), "cf8765432187654321");

    // C7, 09, 01 = EXT-8, 9 bytes, type 1
    bi(new BigInteger("10000000000000000", 16), "c70901010000000000000000");

    bi(null, "c0");
  }


  @Test
  public void errorConditions() throws IOException {
    ObjectWriter writer = MsgPack.writer();

    // Non numeric type does not convert
    byte[] bytes = writer.writeValueAsBytes("Hello, World!");

    ObjectReader reader = MsgPack.reader(Number.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }
    reader = MsgPack.reader(BigInteger.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }
    reader = MsgPack.reader(BigDecimal.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }

    // Unknown extension type does not convert
    bytes = writer.writeValueAsBytes(new MessagePackExtensionType((byte) 99, new byte[5]));
    reader = MsgPack.reader(Number.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }
    reader = MsgPack.reader(BigInteger.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }
    reader = MsgPack.reader(BigDecimal.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }

    // BigDecimal does not convert to BigInteger
    bytes = Hex.decode("c70a0208cf8765432187654321");
    reader = MsgPack.reader(BigInteger.class);
    try {
      reader.readValue(bytes);
      fail();
    } catch (JsonParseException e) {
      // correct
    }
  }


  @Test
  public void pojo() throws IOException {
    Pojo parent = new Pojo();
    Pojo child = new Pojo();
    parent.init();
    child.init();
    parent.setChild(child);

    ObjectWriter writer = MsgPack.writer();
    byte[] bytes = writer.writeValueAsBytes(parent);

    ObjectReader reader = MsgPack.reader(Pojo.class);
    Pojo output = (Pojo) reader.readValue(bytes);

    assertEquals(parent,output);
  }
}