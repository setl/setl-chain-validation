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
package io.setl.bc.pychain.state.tx.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-04-29.
 */
public class AddressSetTest {

  private class Locker extends Thread {

    final AddressSet set;

    boolean done = false;

    boolean ready = false;

    boolean wait = true;


    Locker(AddressSet set) {
      this.set = set;
    }


    void lock() {
      start();
      synchronized (this) {
        while (!ready) {
          try {
            wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }


    public void run() {
      set.lock();
      try {
        synchronized (this) {
          ready = true;
          notifyAll();

          while (wait) {
            try {
              wait();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }
      } finally {
        set.unlock();
        synchronized (this) {
          done = true;
          notifyAll();
        }
      }
    }


    void unlock() {
      synchronized (this) {
        wait = false;
        notifyAll();

        while (!done) {
          try {
            wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }
  }


  @Test
  public void add() {
    add(AddressSet.of((String) null));
    add(AddressSet.of("aaa"));
    add(AddressSet.of("aaa", "bbb"));
    add(AddressSet.of("aaa", "bbb", "ccc"));
    add(AddressSet.of("aaa", "bbb", "ccc", "ddd"));
  }


  private void add(AddressSet set) {
    try {
      set.add("abc");
      fail("Add did not fail");
    } catch (UnsupportedOperationException e) {
      // correct
    }
  }


  @Test
  public void addAll() {
    addAll(AddressSet.of((String) null));
    addAll(AddressSet.of("aaa"));
    addAll(AddressSet.of("aaa", "bbb"));
    addAll(AddressSet.of("aaa", "bbb", "ccc"));
    addAll(AddressSet.of("aaa", "bbb", "ccc", "ddd"));
  }


  private void addAll(AddressSet set) {
    try {
      set.addAll(Arrays.asList("abc", "def", "ghi"));
      fail("AddAll did not fail");
    } catch (UnsupportedOperationException e) {
      // correct
    }
  }


  @Test
  public void clear() {
    clear(AddressSet.of((String) null));
    clear(AddressSet.of("aaa"));
    clear(AddressSet.of("aaa", "bbb"));
    clear(AddressSet.of("aaa", "bbb", "ccc"));
    clear(AddressSet.of("aaa", "bbb", "ccc", "ddd"));
  }


  private void clear(AddressSet set) {
    try {
      set.clear();
      fail("Clear did not fail");
    } catch (UnsupportedOperationException e) {
      // correct
    }
  }


  @Test
  public void compare() {
    AddressSet[] sets = new AddressSet[]{
        AddressSet.of("a", "b", "c"),
        AddressSet.of("b", "c"),
        AddressSet.of("a", "c"),
        AddressSet.of(),
        AddressSet.of("a", "b", "c"),
        AddressSet.of("c"),
        AddressSet.of("a"),
        AddressSet.of("b")
    };

    Arrays.sort(sets);
    assertEquals("[[], [a], [a,b,c], [a,b,c], [a,c], [b], [b,c], [c]]", Arrays.toString(sets));
  }


  @Test
  public void contains() {
    AddressSet set = AddressSet.of(null, null);
    assertFalse(set.contains("abc"));

    set = AddressSet.of("aaa");
    assertTrue(set.contains("aaa"));
    assertFalse(set.contains("zzz"));
    assertFalse(set.contains(null));

    set = AddressSet.of("aaa", "bbb");
    assertTrue(set.contains("aaa"));
    assertTrue(set.contains("bbb"));
    assertFalse(set.contains("zzz"));
    assertFalse(set.contains(null));

    set = AddressSet.of("aaa", "bbb", "ccc");
    assertTrue(set.contains("aaa"));
    assertTrue(set.contains("bbb"));
    assertFalse(set.contains("zzz"));
    assertFalse(set.contains(null));

    set = AddressSet.of("aaa", "bbb", "ccc", "ddd");
    assertTrue(set.contains("aaa"));
    assertTrue(set.contains("bbb"));
    assertFalse(set.contains("zzz"));
    assertFalse(set.contains(null));

    set = AddressSet.of("aaa", "bbb", "ccc", "ddd", "eee", "fff", "ggg", "hhh", "iii");
    assertTrue(set.contains("aaa"));
    assertTrue(set.contains("bbb"));
    assertFalse(set.contains("zzz"));
    assertFalse(set.contains(null));

    assertTrue(set.containsAll(Arrays.asList("bbb", "eee", "ggg")));
    assertFalse(set.containsAll(Arrays.asList("bbb", "!!!", "ggg")));
  }


  @Test
  public void get() {
    get(AddressSet.of((String) null));
    get(AddressSet.of("0"));
    get(AddressSet.of("0", "1"));
    get(AddressSet.of("1", "2", "0"));
    get(AddressSet.of("0", "1", "2", "3"));
  }


  private void get(AddressSet set) {
    for (int i = 0; i < set.size(); i++) {
      assertEquals(Integer.toString(i), set.get(i));
    }

    try {
      set.get(-1);
      fail("get did not fail");
    } catch (ArrayIndexOutOfBoundsException e) {
      // correct
    }

    try {
      set.get(set.size());
      fail("get did not fail");
    } catch (ArrayIndexOutOfBoundsException e) {
      // correct
    }
  }


  @Test
  public void isEmpty() {
    assertTrue(AddressSet.of(null, null, null).isEmpty());
    assertTrue(AddressSet.of(null, null, null, null, null, null).isEmpty());
    assertFalse(AddressSet.of(null, null, "abc", null, null, null).isEmpty());
  }


  @Test
  public void iterator() {
    AddressSet set = AddressSet.of("b", "c", "a");
    Iterator<String> iterator = set.iterator();
    assertTrue(iterator.hasNext());
    assertEquals("a", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("b", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("c", iterator.next());
    assertFalse(iterator.hasNext());

    try {
      iterator.next();
      fail();
    } catch (NoSuchElementException e) {
      // correct
    }
  }

  @Test
  public void lock() {
    AddressSet set0 = AddressSet.of("a","b");
    AddressSet set1 = AddressSet.of("b","c");
    AddressSet set2 = AddressSet.of("c");
    set0.lock();
    assertFalse(set1.tryLock());
    set0.unlock();

    set0.lock();
    Locker locker = new Locker(set1);
    locker.start();

    // lock should not have been acquired
    assertTrue(set2.tryLock());
    set2.unlock();

    set0.unlock();
    locker.unlock();
  }


  @Test
  public void lock0() {
    AddressSet set0 = AddressSet.of();
    AddressSet set1 = AddressSet.of("a", "b");
    set1.lock();
    assertTrue(set0.tryLock());
    set0.unlock();

    set0.lock();
    set0.unlock();

    set1.unlock();
  }


  @Test
  public void lock1() {
    Locker locker = new Locker(AddressSet.of("a", "b"));
    AddressSet set0 = AddressSet.of("c");
    locker.lock();
    assertFalse(AddressSet.of("a").tryLock());
    assertFalse(AddressSet.of("b").tryLock());
    assertTrue(set0.tryLock());
    set0.unlock();

    set0.lock();
    set0.unlock();

    locker.unlock();
  }


  @Test
  public void lock2() {
    Locker locker = new Locker(AddressSet.of("a", "f"));
    AddressSet set0 = AddressSet.of("c", "d");
    locker.lock();
    assertFalse(AddressSet.of("a", "c").tryLock());
    assertFalse(AddressSet.of("d", "f").tryLock());
    assertTrue(set0.tryLock());
    set0.unlock();

    set0.lock();
    set0.unlock();

    locker.unlock();
  }


  @Test
  public void lock3() {
    Locker locker = new Locker(AddressSet.of("b", "f"));
    AddressSet set0 = AddressSet.of("c", "d", "e");
    locker.lock();
    assertFalse(AddressSet.of("b", "d", "e").tryLock());
    assertFalse(AddressSet.of("a", "b", "e").tryLock());
    assertFalse(AddressSet.of("c", "d", "f").tryLock());
    assertTrue(set0.tryLock());
    set0.unlock();

    set0.lock();
    set0.unlock();

    locker.unlock();
  }


  @Test
  public void lock4() {
    Locker locker = new Locker(AddressSet.of("c", "d"));
    AddressSet set0 = AddressSet.of("a", "b", "e", "f");
    locker.lock();
    assertFalse(AddressSet.of("a", "d", "e", "f").tryLock());
    assertFalse(AddressSet.of("c", "a", "e", "f").tryLock());
    assertFalse(AddressSet.of("c", "d", "b", "f").tryLock());
    assertTrue(set0.tryLock());
    set0.unlock();

    set0.lock();
    set0.unlock();

    locker.unlock();
  }


  @Test
  public void ofSet() {
    assertEquals(AddressSet.of(), ofSet(new String[0]));
    assertEquals(AddressSet.of("a"), ofSet("a"));
    assertEquals(AddressSet.of("b", "c"), ofSet("b", "c"));
    assertEquals(AddressSet.of("x", "b", "c"), ofSet("b", "x", "c"));
    assertEquals(AddressSet.of("b", "y", "z", "c"), ofSet("z", "y", "b", "c"));

    AddressSet addressSet = AddressSet.of("p", "q");
    assertSame(addressSet, AddressSet.of(addressSet));
  }


  private AddressSet ofSet(String... array) {
    HashSet<String> hashSet = new HashSet<>(Arrays.asList(array));
    return AddressSet.of(hashSet);
  }


  @Test
  public void ordering0() {
    AddressSet s0 = AddressSet.of();
    AddressSet s1 = AddressSet.of((String) null);
    AddressSet s2 = AddressSet.of(null, null);
    AddressSet s3 = AddressSet.of(null, null, null);
    AddressSet s4 = AddressSet.of(null, null, null, null);
    AddressSet s5 = AddressSet.of((String[]) null);

    assertEquals(s0, s1);
    assertEquals(s0, s2);
    assertEquals(s0, s3);
    assertEquals(s0, s4);
    assertEquals(s0, s5);

    assertEquals(s0.hashCode(), s1.hashCode());
    assertEquals(s0.hashCode(), s2.hashCode());
    assertEquals(s0.hashCode(), s3.hashCode());
    assertEquals(s0.hashCode(), s4.hashCode());
    assertEquals(s0.hashCode(), s5.hashCode());

    s1 = AddressSet.of("a");
    assertNotEquals(s0, s1);
    assertNotEquals(s0.hashCode(), s1.hashCode());
    assertNotNull(s1.toString());

    s2 = AddressSet.of("b");
    assertNotEquals(s1, s2);
    assertEquals(s1, s1);
    assertFalse(s1.equals(null));
  }


  @Test
  public void ordering1() {
    AddressSet s0 = AddressSet.of(null, null, "a");
    AddressSet s1 = AddressSet.of("a");
    AddressSet s2 = AddressSet.of("a", "a");
    AddressSet s3 = AddressSet.of("a", "a", "a");
    AddressSet s4 = AddressSet.of("a", "a", null);
    AddressSet s5 = AddressSet.of(null, "a", null);

    assertEquals(s0, s1);
    assertEquals(s0, s2);
    assertEquals(s0, s3);
    assertEquals(s0, s4);
    assertEquals(s0, s5);

    assertEquals(s0.hashCode(), s1.hashCode());
    assertEquals(s0.hashCode(), s2.hashCode());
    assertEquals(s0.hashCode(), s3.hashCode());
    assertEquals(s0.hashCode(), s4.hashCode());
    assertEquals(s0.hashCode(), s5.hashCode());
  }


  @Test
  public void ordering2() {
    AddressSet s0 = AddressSet.of("a", "b");
    AddressSet s1 = AddressSet.of("a", "b", null);
    AddressSet s2 = AddressSet.of("a", "a", "b");
    AddressSet s3 = AddressSet.of("b", "b", "a");
    AddressSet s4 = AddressSet.of(null, "b", "a");
    AddressSet s5 = AddressSet.of("b", "a");
    AddressSet s6 = AddressSet.of("b", null, "a");

    assertEquals(s0, s1);
    assertEquals(s0, s2);
    assertEquals(s0, s3);
    assertEquals(s0, s4);
    assertEquals(s0, s5);
    assertEquals(s0, s6);

    assertEquals(s0.hashCode(), s1.hashCode());
    assertEquals(s0.hashCode(), s2.hashCode());
    assertEquals(s0.hashCode(), s3.hashCode());
    assertEquals(s0.hashCode(), s4.hashCode());
    assertEquals(s0.hashCode(), s5.hashCode());
    assertEquals(s0.hashCode(), s6.hashCode());
  }


  @Test
  public void ordering3() {
    AddressSet s0 = AddressSet.of("a", "b", "c");
    AddressSet s1 = AddressSet.of("a", "c", "b");
    AddressSet s2 = AddressSet.of("b", "a", "c");
    AddressSet s3 = AddressSet.of("b", "c", "a");
    AddressSet s4 = AddressSet.of("c", "a", "b");
    AddressSet s5 = AddressSet.of("c", "b", "a");
    AddressSet s6 = AddressSet.of("c", null, "b", "a");

    assertEquals(s0, s1);
    assertEquals(s0, s2);
    assertEquals(s0, s3);
    assertEquals(s0, s4);
    assertEquals(s0, s5);
    assertEquals(s0, s6);

    assertEquals(s0.hashCode(), s1.hashCode());
    assertEquals(s0.hashCode(), s2.hashCode());
    assertEquals(s0.hashCode(), s3.hashCode());
    assertEquals(s0.hashCode(), s4.hashCode());
    assertEquals(s0.hashCode(), s5.hashCode());
    assertEquals(s0.hashCode(), s6.hashCode());
  }


  private void permute(List<String[]> output, int k, String[] input) {
    if (k == 1) {
      output.add(input.clone());
      return;
    }

    permute(output, k - 1, input);
    for (int i = 0; i < k - 1; i++) {
      if ((k & 1) == 0) {
        // k is even
        String t = input[i];
        input[i] = input[k - 1];
        input[k - 1] = t;
      } else {
        // k is odd
        String t = input[0];
        input[0] = input[k - 1];
        input[k - 1] = t;
      }
      permute(output, k - 1, input);
    }
  }


  @Test
  public void remove() {
    remove(AddressSet.of((String) null));
    remove(AddressSet.of("aaa"));
    remove(AddressSet.of("aaa", "bbb"));
    remove(AddressSet.of("aaa", "bbb", "ccc"));
    remove(AddressSet.of("aaa", "bbb", "ccc", "ddd"));
  }


  private void remove(AddressSet set) {
    try {
      set.remove(Arrays.asList("abc", "def", "ghi"));
      fail("Remove did not fail");
    } catch (UnsupportedOperationException e) {
      // correct
    }
  }


  @Test
  public void removeAll() {
    removeAll(AddressSet.of((String) null));
    removeAll(AddressSet.of("aaa"));
    removeAll(AddressSet.of("aaa", "bbb"));
    removeAll(AddressSet.of("aaa", "bbb", "ccc"));
    removeAll(AddressSet.of("aaa", "bbb", "ccc", "ddd"));
  }


  private void removeAll(AddressSet set) {
    try {
      set.removeAll(Arrays.asList("abc", "def", "ghi"));
      fail("RemoveAll did not fail");
    } catch (UnsupportedOperationException e) {
      // correct
    }
  }


  @Test
  public void retainAll() {
    retainAll(AddressSet.of((String) null));
    retainAll(AddressSet.of("aaa"));
    retainAll(AddressSet.of("aaa", "bbb"));
    retainAll(AddressSet.of("aaa", "bbb", "ccc"));
    retainAll(AddressSet.of("aaa", "bbb", "ccc", "ddd"));
  }


  private void retainAll(AddressSet set) {
    try {
      set.retainAll(Arrays.asList("abc", "def", "ghi"));
      fail("retainAll did not fail");
    } catch (UnsupportedOperationException e) {
      // correct
    }
  }


  @Test
  public void toArray() {
    String[] input = new String[]{"ddd", "aaa", "ccc", "bbb"};
    String[] sorted = input.clone();
    Arrays.sort(sorted);

    AddressSet set = AddressSet.of(input);
    assertArrayEquals(sorted, set.toArray());
    assertArrayEquals(sorted, set.toArray(new String[0]));

    String[] test = set.toArray(new String[10]);
    for (int i = 0; i < sorted.length; i++) {
      assertEquals(sorted[i], test[i]);
    }
    assertNull(test[sorted.length]);

    String[][] tests = new String[][]{
        {},
        {"a"},
        {"a", "b"},
        {"a", "b", "c"},
        {"a", "b", "c", "d"},
        {"a", "b", "c", "d", "e", "f", "g", "h", "i"}
    };
    for (String[] t : tests) {
      set = AddressSet.of(t);
      assertArrayEquals(t, set.toArray());
    }

    try {
      AddressSet.of("aaa").toArray(new Integer[4]);
      fail();
    } catch (ClassCastException e) {
      // correct;
    }
  }
}