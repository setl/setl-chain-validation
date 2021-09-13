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

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import javax.annotation.Nullable;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.ipfs.TreePath.Branch;

/**
 * A partial tree contains only some routes from the root node to leaf nodes.
 *
 * @author Simon Greatrix on 06/12/2018.
 */
public class PartialTree<T> {

  /** Internal branch nodes. */
  private final Map<Hash, Branch> branches = new HashMap<>();

  /** Leaf nodes. */
  private final Map<Hash, T> leaves = new HashMap<>();

  /**
   * The tree's root hash.
   */
  private Hash root = null;


  /**
   * Add the specified path to a leaf to this partial tree.
   *
   * @param treePath the path to add
   * @param leaf     the leaf at the end of the path
   */
  public void addPath(TreePath<?> treePath, T leaf) {
    if (root == null) {
      root = treePath.getRoot();
    } else {
      if (!root.equals(treePath.getRoot())) {
        throw new IllegalArgumentException("Incompatible roots");
      }
    }

    for (Branch branch : treePath.getBranches()) {
      branches.put(branch.getId(), branch);
    }

    leaves.put(treePath.getLeafHash(), leaf);
  }


  /**
   * Convert this partial tree to an external representation.
   *
   * @param leaf2Map    convertor for leaf values to an external format
   * @param leaf2Binary convertor for leaf values to a binary format which was used to derive the hash
   *
   * @return this partial tree as a map.
   */
  public Map<String, Object> toMap(Function<T, Map<String, Object>> leaf2Map, Function<T, byte[]> leaf2Binary) {
    if (root == null) {
      return Collections.emptyMap();
    }
    return toMap(root, leaf2Map, leaf2Binary);
  }


  private Map<String, Object> toMap(@Nullable Hash id, Function<T, Map<String, Object>> leaf2Map, Function<T, byte[]> leaf2Binary) {
    if (id == null) {
      return null;
    }
    Map<String, Object> map = new TreeMap<>();
    map.put("hash", id.toB64());

    Branch branch = branches.get(id);
    if (branch != null) {
      // it is a branch
      map.put("left", toMap(branch.getLeft(), leaf2Map, leaf2Binary));
      map.put("right", toMap(branch.getRight(), leaf2Map, leaf2Binary));
      map.put("binary", Base64.getUrlEncoder().withoutPadding().encodeToString(branch.toBinary()));
      return map;
    }

    // Convert leaf
    T leaf = leaves.get(id);
    if (leaf != null) {
      map.put("value", leaf2Map.apply(leaf));
      byte[] binary = leaf2Binary.apply(leaf);
      map.put("binary", Base64.getUrlEncoder().withoutPadding().encodeToString(binary));
    }

    return map;
  }

}
