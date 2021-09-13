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
package io.setl.bc.pychain.state.tx.contractdataclasses;

/**
 * Notable events in the life cycle of a contract.
 *
 * @author Simon Greatrix on 30/07/2018.
 */
public enum ContractLifeCycle {
  /** The contract was cancelled. */
  CANCEL,

  /** A party committed to the contract. */
  COMMIT,

  /** The contract completed. */
  COMPLETE,

  /** The contract expired. */
  EXPIRE,

  /** The contract data was cleaned from state. */
  DELETE,

  /** The contract was created. */
  NEW;
}
