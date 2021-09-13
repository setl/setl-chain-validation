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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.setl.bc.pychain.Hash;

/**
 * A path from a tree root to a specific leaf node.
 *
 * @author Simon Greatrix on 06/12/2018.
 */
public class TreePath<T> {

  /**
   * A branch is an internal node that has two child nodes.
   */
  static class Branch {

    /** This node's ID. */
    private final Hash id;

    /** Hash of left child. */
    @Nonnull
    private final Hash left;

    /** Hash of right child. */
    @Nullable
    private final Hash right;


    /**
     * A branch node in the merkle tree.
     *
     * @param id    The hash that identifies this node
     * @param left  The identifier of the 'left' node in the tree. Never null.
     * @param right The identifier of the 'right' node in the tree. May be null.
     */
    public Branch(Hash id, @Nonnull Hash left, @Nullable Hash right) {
      this.id = id;
      this.left = left;
      this.right = right;
    }


    public Hash getId() {
      return id;
    }


    @Nonnull
    public Hash getLeft() {
      return left;
    }


    @Nullable
    public Hash getRight() {
      return right;
    }


    public byte[] toBinary() {
      // 'left' should always be a non-null hash, but we check it anyway
      byte[] l = left.get();
      if (l == null) {
        l = new byte[0];
      }

      // 'right' may be null. It should never be a null has, but we check it anyway
      byte[] r = null;
      if (right != null) {
        r = right.get();
      }
      if (r == null) {
        r = new byte[0];
      }

      byte[] o = new byte[l.length + r.length];
      System.arraycopy(l, 0, o, 0, l.length);
      System.arraycopy(r, 0, o, l.length, r.length);
      return o;
    }

  }



  private final List<Branch> branches = new ArrayList<>();

  private Hash leafHash;

  private T leafValue;

  private Hash root;


  public void addBranch(Hash id, Hash left, Hash right) {
    branches.add(new Branch(id, left, right));
  }


  public List<Branch> getBranches() {
    return Collections.unmodifiableList(branches);
  }


  public Hash getLeafHash() {
    return leafHash;
  }


  public T getLeafValue() {
    return leafValue;
  }


  public Hash getRoot() {
    return root;
  }


  public void setLeaf(Hash id, T leaf) {
    this.leafHash = id;
    this.leafValue = leaf;
  }


  public void setRoot(Hash root) {
    this.root = root;
  }

}
