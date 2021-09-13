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

import io.setl.common.NearlyConstant;
import io.setl.util.ConcurrentWeakValueMap;
import io.setl.util.RuntimeInterruptedException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

/**
 * An immutable set of addresses used by a transaction. An address set is associated with a lock. Locks are NOT re-entrant. Holding the lock for an address set
 * prevents any thread (including the owner) from acquiring the lock on any intersecting address set.
 *
 * <p>For example, given addresses A1, A2, A3 and A4, holding the lock on {A1,A2}, prevents acquisition of a lock on {A1,A3} and {A2,A4}, but does not block
 * {A3,A4}. This facilitates the parallel processing of transactions, as transactions can only be processed concurrently if they have wholly distinct address
 * sets.
 * </p>
 *
 * @author Simon Greatrix on 2019-04-24.
 */
public abstract class AddressSet implements Set<String>, Comparable<AddressSet> {

  /** Locks for each address. */
  private static final ConcurrentWeakValueMap<String, SimpleLock> LOCKS = new ConcurrentWeakValueMap<>();



  /** An empty set. */
  private static class A0 extends AddressSet {

    @Override
    public boolean contains(Object o) {
      return false;
    }


    @Override
    protected String get(int index) {
      throw new ArrayIndexOutOfBoundsException(index);
    }


    @Override
    public boolean isEmpty() {
      return true;
    }


    @Override
    public void lock() {
      // Nothing to lock
    }


    @Override
    public int size() {
      return 0;
    }


    @Override
    protected void toStringArray(String[] array) {
      // do nothing
    }


    @Override
    public boolean tryLock() {
      // Nothing to lock
      return true;
    }


    @Override
    public void unlock() {
      // Nothing to do
    }
  }



  /** Specialised set for a single address. */
  @SuppressWarnings("squid:S2160") // Do not need to override "equals"
  private static class A1 extends AddressSet {

    /** The single address in this set. */
    private final String address;

    private final SimpleLock lock;


    A1(String address) {
      this.address = address;
      lock = LOCKS.computeIfAbsent(address, x -> new SimpleLock());
    }


    A1(TreeSet<String> address) {
      this.address = address.iterator().next();
      lock = LOCKS.computeIfAbsent(this.address, x -> new SimpleLock());
    }


    @Override
    public boolean contains(Object o) {
      return address.equals(o);
    }


    @Override
    protected String get(int index) {
      if (index == 0) {
        return address;
      }
      throw new ArrayIndexOutOfBoundsException(index);
    }


    @Override
    public void lock() {
      lock.lock();
    }


    @Override
    public int size() {
      return 1;
    }


    @Override
    protected void toStringArray(String[] array) {
      array[0] = address;
    }


    @Override
    public boolean tryLock() {
      return lock.tryLock();
    }


    @Override
    public void unlock() {
      lock.unlock();
    }
  }



  /** Specialised set for two entries. */
  @SuppressWarnings("squid:S2160") // Do not need to override "equals"
  private static class A2 extends AddressSet {

    private final String address1;

    private final String address2;

    private final SimpleLock lock1;

    private final SimpleLock lock2;


    A2(String address1, String address2) {
      if (address1.compareTo(address2) < 0) {
        this.address1 = address1;
        this.address2 = address2;
      } else {
        this.address1 = address2;
        this.address2 = address1;
      }

      lock1 = LOCKS.computeIfAbsent(address1, x -> new SimpleLock());
      lock2 = LOCKS.computeIfAbsent(address2, x -> new SimpleLock());
    }


    A2(TreeSet<String> address) {
      Iterator<String> iterator = address.iterator();
      this.address1 = iterator.next();
      this.address2 = iterator.next();

      lock1 = LOCKS.computeIfAbsent(address1, x -> new SimpleLock());
      lock2 = LOCKS.computeIfAbsent(address2, x -> new SimpleLock());
    }


    @Override
    public boolean contains(Object o) {
      return address1.equals(o) || address2.equals(o);
    }


    @Override
    protected String get(int index) {
      switch (index) {
        case 0:
          return address1;
        case 1:
          return address2;
        default:
          throw new ArrayIndexOutOfBoundsException(index);
      }
    }


    @Override
    public void lock() {
      lock1.lock();
      lock2.lock();
    }


    @Override
    public int size() {
      return 2;
    }


    @Override
    protected void toStringArray(String[] array) {
      array[0] = address1;
      array[1] = address2;
    }


    @Override
    public boolean tryLock() {
      if (lock1.tryLock()) {
        if (lock2.tryLock()) {
          return true;
        }
        lock1.unlock();
      }
      return false;
    }


    @Override
    public void unlock() {
      lock2.unlock();
      lock1.unlock();
    }
  }



  /**
   * Specialised set for 3 members.
   */
  @SuppressWarnings("squid:S2160") // Do not need to override "equals"
  private static class A3 extends AddressSet {

    private final String address1;

    private final String address2;

    private final String address3;

    private final SimpleLock lock1;

    private final SimpleLock lock2;

    private final SimpleLock lock3;


    A3(String address1, String address2, String address3) {
      if (address1.compareTo(address2) > 0) {
        String t = address1;
        address1 = address2;
        address2 = t;
      }
      if (address1.compareTo(address3) > 0) {
        String t = address1;
        address1 = address3;
        address3 = t;
      }
      if (address2.compareTo(address3) > 0) {
        String t = address2;
        address2 = address3;
        address3 = t;
      }
      this.address1 = address1;
      this.address2 = address2;
      this.address3 = address3;

      lock1 = LOCKS.computeIfAbsent(address1, x -> new SimpleLock());
      lock2 = LOCKS.computeIfAbsent(address2, x -> new SimpleLock());
      lock3 = LOCKS.computeIfAbsent(address3, x -> new SimpleLock());
    }


    A3(TreeSet<String> addresses) {
      Iterator<String> iterator = addresses.iterator();
      this.address1 = iterator.next();
      this.address2 = iterator.next();
      this.address3 = iterator.next();

      lock1 = LOCKS.computeIfAbsent(address1, x -> new SimpleLock());
      lock2 = LOCKS.computeIfAbsent(address2, x -> new SimpleLock());
      lock3 = LOCKS.computeIfAbsent(address3, x -> new SimpleLock());
    }


    @Override
    public boolean contains(Object o) {
      return address1.equals(o) || address2.equals(o) || address3.equals(o);
    }


    @Override
    protected String get(int index) {
      switch (index) {
        case 0:
          return address1;
        case 1:
          return address2;
        case 2:
          return address3;
        default:
          throw new ArrayIndexOutOfBoundsException(index);
      }
    }


    @Override
    public void lock() {
      lock1.lock();
      lock2.lock();
      lock3.lock();
    }


    @Override
    public int size() {
      return 3;
    }


    @Override
    protected void toStringArray(String[] array) {
      array[0] = address1;
      array[1] = address2;
      array[2] = address3;
    }


    @Override
    public boolean tryLock() {
      if (lock1.tryLock()) {
        if (lock2.tryLock()) {
          if (lock3.tryLock()) {
            return true;
          }
          lock2.unlock();
        }
        lock1.unlock();
      }
      return false;
    }


    @Override
    public void unlock() {
      lock3.unlock();
      lock2.unlock();
      lock1.unlock();
    }
  }



  /**
   * Specialised address set for a large number of entries.
   */
  private static class ALarge extends ASmall {

    ALarge(TreeSet<String> addresses) {
      super(addresses);
    }


    @Override
    public boolean contains(Object o) {
      if (o instanceof String) {
        // Do a binary search
        return Arrays.binarySearch(addresses, o) >= 0;
      }
      return false;
    }
  }



  /**
   * Specialised address set for a modest number of entries.
   */
  @SuppressWarnings("squid:S2160") // Do not need to override "equals"
  private static class ASmall extends AddressSet {

    protected String[] addresses;

    protected SimpleLock[] locks;


    ASmall(TreeSet<String> addressSet) {
      addresses = addressSet.toArray(new String[0]);
      int l = addresses.length;
      locks = new SimpleLock[l];
      for (int i = 0; i < l; i++) {
        locks[i] = LOCKS.computeIfAbsent(addresses[i], x -> new SimpleLock());
      }
    }


    @Override
    public boolean contains(Object o) {
      // Simple is fast when the number of entries to check is small
      for (String a : addresses) {
        if (a.equals(o)) {
          return true;
        }
      }
      return false;
    }


    @Override
    protected String get(int index) {
      return addresses[index];
    }


    @Override
    public void lock() {
      int l = locks.length;
      for (int i = 0; i < l; i++) {
        locks[i].lock();
      }
    }


    @Override
    public int size() {
      return addresses.length;
    }


    @Override
    protected void toStringArray(String[] array) {
      System.arraycopy(addresses, 0, array, 0, addresses.length);
    }


    @Override
    public boolean tryLock() {
      int l = locks.length;
      int i = 0;
      while (locks[i].tryLock()) {
        i++;
        if (i == l) {
          return true;
        }
      }
      while (i > 0) {
        i--;
        locks[i].unlock();
      }
      return false;
    }


    @Override
    public void unlock() {
      int l = locks.length;
      for (int i = l - 1; i >= 0; i--) {
        locks[i].unlock();
      }
    }
  }



  private static class SimpleLock {

    private boolean isLocked = false;


    synchronized void lock() {
      while (isLocked) {
        try {
          wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeInterruptedException(e);
        }
      }
      isLocked = true;
    }


    synchronized boolean tryLock() {
      if (isLocked) {
        return false;
      }
      isLocked = true;
      return true;
    }


    synchronized void unlock() {
      isLocked = false;
      notifyAll();
    }
  }


  /**
   * Construct an address set from a single address.
   *
   * @param address the address
   *
   * @return the set
   */
  public static AddressSet of(String address) {
    return address != null ? new A1(NearlyConstant.fixed(address)) : new A0();
  }


  /**
   * Construct an address set from a pair of address. If the addresses are equal to each other, the
   *
   * @param address1 an address
   * @param address2 an address
   *
   * @return the set
   */
  public static AddressSet of(String address1, String address2) {
    if (address1 == null) {
      return of(address2);
    }
    if (address2 == null) {
      return of(address1);
    }
    return address1.equals(address2)
        ? new A1(NearlyConstant.fixed(address1))
        : new A2(NearlyConstant.fixed(address1), NearlyConstant.fixed(address2));
  }


  /**
   * Create an address set from the provided addresses.
   *
   * @param addresses the addresses
   *
   * @return the set
   */
  public static AddressSet of(String... addresses) {
    TreeSet<String> treeSet = new TreeSet<>();
    if (addresses != null) {
      for (String s : addresses) {
        if (s != null) {
          treeSet.add(NearlyConstant.fixed(s));
        }
      }
    }
    int s = treeSet.size();
    switch (s) {
      case 0:
        return new A0();
      case 1:
        return new A1(treeSet);
      case 2:
        return new A2(treeSet);
      case 3:
        return new A3(treeSet);
      default:
        if (s < 8) {
          return new ASmall(treeSet);
        }
        return new ALarge(treeSet);
    }
  }


  /**
   * Create an address set from the provided addresses.
   *
   * @param addresses the addresses
   *
   * @return the set
   */
  public static AddressSet of(Set<String> addresses) {
    if (addresses instanceof AddressSet) {
      return (AddressSet) addresses;
    }

    int s = addresses.size();
    Iterator<String> iterator = addresses.iterator();
    switch (s) {
      case 0:
        return new A0();
      case 1:
        return of(iterator.next());
      case 2:
        return of(iterator.next(), iterator.next());
      case 3:
        return of(iterator.next(), iterator.next(), iterator.next());
      default:
        return of(addresses.toArray(new String[0]));
    }
  }


  /**
   * Create an address set from the provided addresses.
   *
   * @param address1 first address
   * @param address2 second address
   * @param address3 third address
   *
   * @return the set
   */
  public static AddressSet of(String address1, String address2, String address3) {
    if (address1 == null || address1.equals(address2) || address1.equals(address3)) {
      return of(address2, address3);
    }
    if (address2 == null || address2.equals(address3)) {
      return of(address1, address3);
    }
    if (address3 == null) {
      return of(address1, address2);
    }
    return new A3(
        NearlyConstant.fixed(address1),
        NearlyConstant.fixed(address2),
        NearlyConstant.fixed(address3)
    );
  }


  @Override
  public boolean add(String s) {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean addAll(Collection<? extends String> c) {
    throw new UnsupportedOperationException();
  }


  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }


  @Override
  @SuppressWarnings("squid:S4973") // Using '==' as quick check for object equality is fine in this case
  public int compareTo(AddressSet other) {
    int s = Math.min(size(), other.size());
    for (int i = 0; i < s; i++) {
      String myString = get(i);
      String otherString = other.get(i);

      // We use the NearlyConstant cache to make object identity likely
      int c = (myString == otherString) ? 0 : myString.compareTo(otherString);
      if (c != 0) {
        return c;
      }
    }

    // smaller set sorts first
    return Integer.compare(size(), other.size());
  }


  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }


  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AddressSet)) {
      return false;
    }
    AddressSet other = (AddressSet) o;
    int s = size();
    if (s != other.size()) {
      return false;
    }
    for (int i = 0; i < s; i++) {
      if (!get(i).equals(other.get(i))) {
        return false;
      }
    }
    return true;
  }


  protected abstract String get(int index);


  @Override
  public int hashCode() {
    int s = size();
    int h = 131071; // a Mersenne prime
    for (int i = 0; i < s; i++) {
      h *= 31;
      h += get(i).hashCode();
    }
    return h;
  }


  @Override
  public boolean isEmpty() {
    // Every transaction involves at least one address.
    return false;
  }


  @Override
  public Iterator<String> iterator() {
    final int s = size();
    return new Iterator<String>() {
      int i = 0;


      @Override
      public boolean hasNext() {
        return i < s;
      }


      @Override
      public String next() {
        if (i >= s) {
          throw new NoSuchElementException();
        }
        return get(i++);
      }
    };
  }


  /** Release the lock associated with this address set. */
  public abstract void lock();


  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String[] toArray() {
    String[] array = new String[size()];
    toStringArray(array);
    return array;
  }


  @Override
  public <T> T[] toArray(T[] array) {
    if (!(array instanceof String[])) {
      throw new ClassCastException("Must be String[]");
    }

    // Handle the provided array according to the Set contract.
    int s = size();
    String[] sa = (String[]) array;
    if (sa.length < s) {
      sa = new String[s];
    }
    if (sa.length > s) {
      sa[s] = null;
    }
    toStringArray(sa);

    @SuppressWarnings("unchecked")
    T[] ta = (T[]) sa;
    return ta;
  }


  @Override
  public String toString() {
    int s = size();
    StringBuilder builder = new StringBuilder().append('[');
    for (int i = 0; i < s; i++) {
      builder.append(get(i)).append(',');
    }
    if (s > 0) {
      builder.setLength(builder.length() - 1);
    }
    builder.append(']');
    return builder.toString();
  }


  /**
   * Implement the writing of set members into a pre-created array as part of the implementation of toArray() and toArray(T[]).
   *
   * @param array the array to populate.
   */
  protected abstract void toStringArray(String[] array);


  /**
   * Try to lock this address set.
   *
   * @return true if the address set was locked.
   */
  public abstract boolean tryLock();


  /**
   * Release the lock on this address set.
   */
  public abstract void unlock();
}
