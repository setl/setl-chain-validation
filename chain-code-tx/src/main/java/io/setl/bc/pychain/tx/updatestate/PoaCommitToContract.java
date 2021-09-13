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
package io.setl.bc.pychain.tx.updatestate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommitToContract.
 */
public class PoaCommitToContract extends CommitToContract {
  
  private static final Logger logger = LoggerFactory.getLogger(PoaCommitToContract.class);
  
  /* This class only uses methods from the base class. It exists for semantics only. */
  
  /*
  io.setl.bc.pychain.tx.updatestate.contracts.DVPCommitTest > updatestate FAILED
    java.lang.AssertionError at DVPCommitTest.java:378

io.setl.bc.pychain.tx.updatestate.contracts.DVPStateTests > stateContract2 FAILED
    java.lang.IndexOutOfBoundsException at DVPStateTests.java:303

io.setl.bc.pychain.tx.updatestate.contracts.DVPStateTests > stateParams FAILED
    java.lang.IndexOutOfBoundsException at DVPStateTests.java:303

   */
}
