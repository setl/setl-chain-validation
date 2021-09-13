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

/**
 * Standard transaction parameters.
 *
 * @author Simon Greatrix on 25/01/2018.
 */
public interface TxParameters {
  
  String ADDRESS = "address";
  
  String ADDRESSES = "addresses";
  
  String ADD_ENCUMBRANCES = "addencumbrances";
  
  String ADMINISTRATORS = "administrators";
  
  String ADMINS = "admins";
  
  String AMOUNT = "amount";
  
  String ASSETS_IN = "assetsin";
  
  String ASSETS_OUT = "assetsout";
  
  String ASSET_CLASS = "assetclass";
  
  String ASSET_ID = "assetid";
  
  String ASSET_IN = "assetin";
  
  String AUTHORISATIONS = "authorisations";
  
  String AUTHORISATION_ID = "authorisationid";
  
  String AUTHORISE = "authorise";
  
  String AUTH_ID = "authid";
  
  String AUTO_SIGN = "autosign";
  
  String BASE_CHAIN = "basechain";
  
  String BENEFICIARIES = "beneficiaries";
  
  String BLOCK_SIZE = "blocksize";
  
  String BLOCK_SIZE_IN = "blocksizein";
  
  String BLOCK_SIZE_OUT = "blocksizeout";
  
  String CALCULATED_INDEX = "calculatedindex";
  
  String CALCULATION_ONLY = "calculationonly";
  
  String CANCEL = "cancel";
  
  String CLASS = "class";
  
  String CLASSID = "classid";
  
  String COMMITMENT = "commitment";
  
  String CONTRACT_ADDRESS = "contractaddress";
  
  String CONTRACT_FUNCTION = "contractfunction";
  
  String CONTRACT_SPECIFIC = "contractspecific";
  
  String CREATION = "creation";
  
  String DELAY_ON_COMPLETE = "deletedelayoncomplete";
  
  String ENCUMBRANCE = "encumbrance";
  
  String ENCUMBRANCES = "encumbrances";
  
  String ENCUMBRANCE_NAME = "encumbrancename";
  
  String END_TIME = "endtime";
  
  String EVENTS = "events";
  
  String EXPIRY = "expiry";
  
  String FROM_ADDR = "fromaddr";
  
  String HASH = "hash";
  
  String HEIGHT = "height";
  
  String INDEX = "index";
  
  String INPUT_TOKEN_CLASS = "inputtokenclass";
  
  String ISSUANCE = "issuance";
  
  String ISSUING_ADDRESS = "issuingaddress";
  
  String IS_CUMULATIVE = "iscumulative";
  
  String MAX_BLOCKS = "maxblocks";
  
  String METADATA = "metadata";
  
  String MIN_BLOCKS = "minblocks";
  
  String MUST_SIGN = "mustSign";
  
  String NAMESPACE = "namespace";
  
  String NEW_BLOCK_HEIGHT = "newblockheight";
  
  String NEW_CHAIN_ID = "newchainid";
  
  String NONCE = "nonce";
  
  String OUTPUT_TOKEN_CLASS = "outputtokenclass";
  
  String PARAMETERS = "parameters";
  
  String PARAMETER_NAME = "parametername";
  
  String PARTIES = "parties";
  
  String PARTY = "party";
  
  String PARTY_IDENTIFIER = "partyidentifier";
  
  String PAY_LIST = "paylist";
  
  String PERMISSIONS = "permissions";
  
  String POA = "poa";
  
  String POA_PUBLIC_KEY = "poapublickey";
  
  String POA_REFERENCE = "poareference";

  String POA_ADDRESS = "poaaddress";

  String PROTOCOL = "protocol";
  
  String PUBLIC_KEY = "publickey";
  
  String RATIO = "ratio";
  
  String RECEIVE = "receive";
  
  String RECEIVE_LIST = "receivelist";
  
  String REFERENCE = "reference";
  
  String REFUSED = "refused";
  
  String RETURN_ADDR = "returnaddr";
  
  String SIGNATURE = "signature";
  
  String SIG_ADDRESS = "sigaddress";
  
  String SIG_NODES = "signodes";
  
  String STAKE = "stake";
  
  String START_DATE = "startdate";
  
  String START_TIME = "starttime";
  
  String SUBJECT_ADDR = "subjectaddr";
  
  String TO_ADDR = "toaddr";
  
  String TO_CHAIN_ID = "tochainid";
  
  String TO_PUBLIC_KEY = "topublickey";
  
  String TRANSACTIONS = "transactions";
  
  String TX_TYPE = "txtype";
  
  String UPDATED = "updated";
  
  String USE_CREATOR_ENCUMBRANCE = "usecreatorencumbrance";
  
  String VALUE = "value";
  
  String X_CHAIN_ID = "xchainid";
  
  String __ADDRESS = "__address";
  
  String __CANCELTIME = "__canceltime";
  
  String __COMPLETED = "__completed";
  
  String __FUNCTION = "__function";
  
  String __STATUS = "__status";
  
  String __TIMEEVENT = "__timeevent";
  
}
