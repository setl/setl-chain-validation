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
package io.setl.bc.pychain.node.monitor;

import java.util.concurrent.atomic.AtomicInteger;


public class BlueScreenTransactionPoolSize {

  private static final AtomicInteger count = new AtomicInteger(0);

  private Integer index;
  private Long currentTime;
  private Double transactionPoolSize;


  public BlueScreenTransactionPoolSize(Long currentTime, Double transactionPoolSize) {
    this.index = count.incrementAndGet();
    this.currentTime = currentTime;
    this.transactionPoolSize = transactionPoolSize;
  }


  public Integer getIndex() {
    return index;
  }


  public Long getCurrentTime() {
    return currentTime;
  }


  public Double getTransactionPoolSize() {
    return transactionPoolSize;
  }

}
