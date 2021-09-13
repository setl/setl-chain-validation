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
package io.setl.bc.pychain.tx.create.contracts;

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;
import static io.setl.common.StringUtils.logSafe;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.setl.bc.pychain.exception.InvalidTransactionException;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.TokensNominateContractData;

/**
 * @author Simon Greatrix on 27/08/2020.
 */
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "contractType")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = DvpContractData.class, name = CONTRACT_NAME_DVP_UK),
        @JsonSubTypes.Type(value = NominateContract.class, name = CONTRACT_NAME_TOKENS_NOMINATE),
        @JsonSubTypes.Type(value = ExchangeContract.class, name = CONTRACT_NAME_EXCHANGE)
    }
)
public interface ContractData {

  static ContractData convert(IContractData internal) throws InvalidTransactionException {
    if (internal == null) {
      return null;
    }
    String type = internal.getContractType();
    switch (type) {
      case CONTRACT_NAME_DVP_UK:
        return new DvpContractData((DvpUkContractData) internal);
      case CONTRACT_NAME_TOKENS_NOMINATE:
        return new NominateContract((TokensNominateContractData) internal);
      case CONTRACT_NAME_EXCHANGE:
        return new ExchangeContract((ExchangeContractData) internal);
      default:
        throw new InvalidTransactionException("Unhandled contract type: " + logSafe(type));
    }
  }

  static void require(Object o, String message) throws InvalidTransactionException {
    if (o == null) {
      throw new InvalidTransactionException(message);
    }
  }

  /**
   * Convert this contract data to its internal representation of the contract data.
   *
   * @return this contract data in internal format
   */
  IContractData asInternalData() throws InvalidTransactionException;

  @JsonTypeId
  @NotNull
  String getContractType();

}
