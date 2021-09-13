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
package io.setl.bc.pychain.state.ipfs;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.utils.ByteUtil;
import java.util.Map;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapBackedMerkleStoreString implements MerkleStoreReader<Object> {

  private static final Logger logger = LoggerFactory.getLogger(MapBackedMerkleStoreString.class);

  final Map<String, byte[]> theMap;


  public MapBackedMerkleStoreString(Map<String, byte[]> theMap) {
    this.theMap = theMap;
  }


  @Override
  public Object get(@Nonnull Hash hash) {
    byte[] hashBytes = hash.get();
    if (hashBytes == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Null key");
      }
      return null;
    }
    byte[] bytes = theMap.get(new String(hashBytes, ByteUtil.BINCHARSET));
    if (bytes == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Not found:{}", ByteUtil.bytesToHex(hashBytes));
      }
      return null;
    }

    Object[] o = MsgPackUtil.unpackWrapped(bytes, true).unwrap();
    // Message pack does not support byte[][] - so kludge
    if ((o.length == 0) || !(o[0] instanceof byte[])) {
      return 0;
    }
    byte[][] oo;
    oo = new byte[o.length][];
    for (int ii = 0; ii < oo.length; ii++) {
      oo[ii] = (byte[]) o[ii];
    }
    return oo;
  }

}
