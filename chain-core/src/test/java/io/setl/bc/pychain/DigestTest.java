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
package io.setl.bc.pychain;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.state.StateBase;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class DigestTest {

  @Test
  public void create() {
    assertEquals(Digest.TYPE_SHA_256, Digest.create(Digest.TYPE_SHA_256).getType());
    assertEquals(Digest.TYPE_SHA_3_256, Digest.create(Digest.TYPE_SHA_3_256).getType());

    // Well known digests of empty message
    assertEquals(Hash.fromHex("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), Digest.create(Digest.TYPE_SHA_256).digest());
    assertEquals(Hash.fromHex("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"), Digest.create(Digest.TYPE_SHA_3_256).digest());
    assertEquals(Hash.fromHex("c672b8d1ef56ed28ab87c3622c5114069bdd3ad7b8f9737498d0c01ecef0967a"), Digest.create(Digest.TYPE_SHA_512_256).digest());
  }


  @Test(expected = IllegalArgumentException.class)
  public void createBad() {
    Digest.create(1_000_000);
  }


  @Test
  public void getRecommended() {
    assertEquals(Hash.fromHex("c672b8d1ef56ed28ab87c3622c5114069bdd3ad7b8f9737498d0c01ecef0967a"), Digest.create(Digest.getRecommended()).digest());
  }


  @Test
  public void getRecommended1() {
    StateBase mock = Mockito.mock(StateBase.class);
    when(mock.getConfigValue(eq(Digest.DIGEST_SETTING),any())).thenReturn(Digest.TYPE_SHA_3_256);
    assertEquals(Hash.fromHex("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"), Digest.create(Digest.getRecommended(mock)).digest());
  }


  @Test
  public void name() {
    assertEquals("SHA-256", Digest.name(Digest.TYPE_SHA_256));
    assertEquals("SHA-512/256", Digest.name(Digest.TYPE_SHA_512_256));
    assertEquals("SHA3-256", Digest.name(Digest.TYPE_SHA_3_256));
    assertEquals("Unknown (1000000)", Digest.name(1000000));
  }


  @Test
  public void testParse() {
    assertEquals(Integer.valueOf(Digest.TYPE_SHA_512_256), Digest.PARSE_TYPE.apply(null));
    assertEquals(Integer.valueOf(Digest.TYPE_SHA_256), Digest.PARSE_TYPE.apply("sha-256"));
    assertEquals(Integer.valueOf(Digest.TYPE_SHA_3_256), Digest.PARSE_TYPE.apply(Digest.TYPE_SHA_3_256));
  }


  @Test
  public void typeForName() {
    assertEquals(Digest.TYPE_SHA_256, Digest.typeForName("Sha-256"));
    assertEquals(Digest.TYPE_SHA_3_256, Digest.typeForName(Integer.toString(Digest.TYPE_SHA_3_256)));
    assertEquals(Digest.TYPE_SHA_512_256, Digest.typeForName("default"));
    assertEquals(Digest.TYPE_SHA_512_256, Digest.typeForName(null));
  }

}