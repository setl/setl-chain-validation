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

/**
 * Typical priority levels for the PriorityExecutor. The larger the priority value, the more important the task. Note that if two priorities have the same
 * value, there is no distinction between them.
 *
 * @author Simon Greatrix on 2019-03-18.
 */
public interface Priorities {

  /** Persisting a block to disk. */
  int BLOCK_WRITER = 2500;

  /** The default priority, if none is specified. */
  int DEFAULT = 2000;

  /** Tasks with a priority above this level run at an elevated O/S priority. */
  int ELEVATED_PRIORITY_MARK = 1_000_000_000;

  /** This is the lowest priority task. If we are busy the last thing we want is more connections. */
  int NETWORK_ACCEPT = 0;

  /** Managing the network is a low-ish priority. */
  int NETWORK_MANAGE = 1500;

  /** Reading messages from the network is a low priority. We want to create back-pressure if we are busy. */
  int NETWORK_READ = 1000;

  /** If we have something to say, we want to get the message out the door, provided it doesn't delay processing a proposal. */
  int NETWORK_WRITE = 3000;

  /** Completing a proposal is the most important task. */
  int PROPOSAL = 2_000_000_000;

  /** Updating state. */
  int STATE_UPDATE = 2750;

  /** Tasks with a priority above ELEVATED_PRIORITY_MARK run at this O/S priority. */
  int THREAD_ELEVATED_PRIORITY = (Thread.MAX_PRIORITY + Thread.NORM_PRIORITY) / 2;

  /** Tasks with a negative priority run at this O/S priority. */
  int THREAD_REDUCED_PRIORITY = (Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2;

  /**
   * We are going to need verified transactions for the next proposal. However we may verify as part of the proposal tasks so they can have a lower priority
   * initially.
   */
  int TX_VERIFY = 2001;
}
