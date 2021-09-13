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
package io.setl.rest;

import static io.setl.bc.pychain.state.tx.helper.TxParameters.BASE_CHAIN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CLASSID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CREATION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.FROM_ADDR;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.HASH;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.NONCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.POA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.SIGNATURE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.TX_TYPE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.UPDATED;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.AddressToNonceMapper;
import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.event.TransactionListenerInternal;
import io.setl.bc.pychain.file.WalletLoader;
import io.setl.bc.pychain.node.StateInitializedEvent;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.monolithic.ContractsList;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.AddXChainTx;
import io.setl.bc.pychain.state.tx.AssetClassRegisterTx;
import io.setl.bc.pychain.state.tx.AssetIssueTx;
import io.setl.bc.pychain.state.tx.AssetTransferXChainTx;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.EncumberTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.bc.pychain.state.tx.NamespaceRegisterTx;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.Sign;
import io.setl.bc.pychain.state.tx.StockSplitTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.tx.contractsign.DvpCommit;
import io.setl.bc.pychain.tx.contractsign.DvpContract;
import io.setl.bc.pychain.tx.create.Encumber;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.pychain.wallet.Wallet;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.Hex;
import io.setl.common.StringUtils;
import io.setl.crypto.KeyGen;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.crypto.SHA256;
import io.setl.rest.util.WalletPosition;
import io.setl.utils.ByteUtil;
import io.setl.utils.TimeUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ValidationRestApi implements ApplicationListener<StateInitializedEvent> {

  static final String ADDRESS = "address";

  static final String CLASS_ID = "classid";

  static final String CONTRACT_ADDRESS = "contractaddress";

  static final String CONTRACT_FUNCTION = "contractfunction";

  static final String FUNCTION = "__function";

  static final String ISSUING_ADDRESS = "issuingaddress";

  static final String NAMESPACE = "namespace";

  static final String PROTOCOL = "protocol";

  static final String RESULT = "result";

  static final String STOCK_SPLIT = "stocksplit";

  private static final int INT_1 = 4; // Legacy

  private static final String SYS_STAKE_ID = "SYS|STAKE";

  private static final Logger logger = LoggerFactory.getLogger(ValidationRestApi.class);



  static class AccountResponse {

    public final String address;

    public final Number balance;

    public final String publickey;


    public AccountResponse(String publickey, Number balance, String address) {

      this.publickey = publickey;
      this.balance = balance;
      this.address = address;
    }
  }



  @SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
  static class AddXChain {

    public int chainheight;

    public int chainid;

    public int chainparameters;

    public List<Object[]> chainsignodes;

    public String fromAddr;

    public List<Object> sigNodes;
  }



  static class Address {

    public String address;
  }



  static class AssetBalanceRequest {

    public String address;

    public boolean byaddress;

    public String classid;

    public String namespace;
  }



  @SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
  static class CommitContract {

    public List<Object> cancel;

    public List<Object> commitment;

    public String contractaddress;

    public String contractfunction;

    public String issuingaddress;

    public int party;

    public String protocol;

    public List<Object> receive;
  }



  @SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
  static class Contract {

    public String contractfunction;

    public String issuingaddress;

    public String metadata;

    public List<Object> parties;

    public String protocol;
  }



  static class ContractResponse {

    public final Number amount;

    public final int basechain;

    public final int blockheight;

    public final Map<String, Object> contractdata;

    public final long creation;

    public final String fromaddr;

    public final String hash;

    public final String poa;

    public final String toaddr;

    public final int tochain;

    public final int txtype;

    public final String typeName;

    public final boolean update;


    @SuppressWarnings("squid:S00107") // Params > 7
    public ContractResponse(
        Number amount, int basechain, int blockheight, Map<String, Object> contractdata, long creation, String fromaddr, String hash,
        String poa, String toaddr, int tochain, int txtype, String typename, boolean update
    ) {

      this.amount = amount;
      this.basechain = basechain;
      this.blockheight = blockheight;
      this.contractdata = contractdata;
      this.creation = creation;
      this.fromaddr = fromaddr;
      this.hash = hash;
      this.poa = poa;
      this.toaddr = toaddr;
      this.tochain = tochain;
      this.txtype = txtype;
      this.typeName = typename;
      this.update = update;
    }
  }



  static class IssueAsset {

    public Number amount;

    public String classid;

    public String fromaddr;

    public String metadata;

    public String namespace;

    public String protocol;

    public String toaddr;
  }



  static class IssueAssetResponse {

    public final Number amount;

    public final int basechain;

    public final String classid;

    public final long creation;

    public final String fromaddr;

    public final String hash;

    public final String namespace;

    public final long nonce;

    public final String poa;

    public final String protocol;

    public final String signature;

    public final String toaddr;

    public final String typeName;


    @SuppressWarnings("squid:S00107") // Params > 7
    public IssueAssetResponse(
        String classid, long nonce, long creation, String hash, String fromaddr, int basechain, String namespace, String typename,
        Number amount, String toaddr, String signature, String protocol, String poa
    ) {

      this.classid = classid;
      this.nonce = nonce;
      this.creation = creation;
      this.hash = hash;
      this.fromaddr = fromaddr;
      this.basechain = basechain;
      this.namespace = namespace;
      this.typeName = typename;
      this.amount = amount;
      this.toaddr = toaddr;
      this.signature = signature;
      this.protocol = (protocol == null ? "" : protocol);
      this.poa = poa;
    }
  }



  static class KeyPair {

    String privkey;

    String pubkey;
  }



  @SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
  static class Namespace {

    public Number amount;

    public String fromaddr;

    public Object metadata;

    public String namespace;
  }



  static class NamespaceRegisterResponse {

    public final int basechain;

    public final long creation;

    public final String fromaddr;

    public final String hash;

    public final String namespace;

    public final long nonce;

    public final String poa;

    public final String signature;

    public final String typeName;


    @SuppressWarnings("squid:S00107") // Params > 7
    public NamespaceRegisterResponse(
        long nonce, long creation, String hash, String fromaddr, int basechain, String namespace, String signature,
        String typename, String poa
    ) {

      this.nonce = nonce;
      this.creation = creation;
      this.hash = hash;
      this.fromaddr = fromaddr;
      this.basechain = basechain;
      this.namespace = namespace;
      this.signature = signature;
      this.typeName = typename;
      this.poa = poa;
    }
  }



  static class NonceResponse {

    public final String address;

    public final long nonce;


    public NonceResponse(long nonce, String address) {

      this.nonce = nonce;
      this.address = address;
    }
  }



  @SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
  static class RegisterAddress {

    public String address;

    public String metadata;
  }



  @SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
  static class RegisterAsset {

    public String address;

    public String classid;

    public String fromaddr;

    public Object metadata;

    public String namespace;
  }



  static class RegisterAssetResponse {

    public final int basechain;

    public final String classid;

    public final long creation;

    public final String fromaddr;

    public final String hash;

    public final String namespace;

    public final long nonce;

    public final String poa;

    public final String signature;

    public final String typeName;


    @SuppressWarnings("squid:S00107") // Params > 7
    public RegisterAssetResponse(
        String classid, long nonce, long creation, String hash, String fromaddr, int basechain, String namespace,
        String signature, String typename, String poa
    ) {

      this.classid = classid;
      this.nonce = nonce;
      this.creation = creation;
      this.hash = hash;
      this.fromaddr = fromaddr;
      this.basechain = basechain;
      this.namespace = namespace;
      this.signature = signature;
      this.typeName = typename;
      this.poa = poa;
    }
  }



  static class StockSplit {

    public String classid;

    public String contractname;

    public String fromaddress;

    public String namespace;

    public double ratio;
  }



  static class StockSplitResponse {

    public final int basechain;

    public final String classid;

    public final long creation;

    public final String fromaddr;

    public final String hash;

    public final String namespace;

    public final long nonce;

    public final String poa;

    public final double ratio;

    public final String reference;

    public final String signature;

    public final String typeName;


    @SuppressWarnings("squid:S00107") // Params > 7
    public StockSplitResponse(
        String classid, long nonce, long creation, String hash, String reference, String fromaddr,
        int basechain, String namespace, String typename, String signature, double ratio, String poa
    ) {

      this.classid = classid;
      this.nonce = nonce;
      this.creation = creation;
      this.hash = hash;
      this.reference = reference;
      this.fromaddr = fromaddr;
      this.basechain = basechain;
      this.namespace = namespace;
      this.typeName = typename;
      this.signature = signature;
      this.ratio = ratio;
      this.poa = poa;
    }
  }



  private final AddressToKeysMapper addressToKeysMapper;

  private final AddressToNonceMapper addressToNonceMapper;

  private final int chainId;

  private final StateManager stateManager;

  private final TransactionListenerInternal transactionListenerInternal;

  private final String walletFilePath;

  private final WalletLoader walletLoader;

  private final WalletPosition walletPosition;


  /**
   * ValidationRestApi Constructor.
   *
   * @param stateManager                :
   * @param walletLoader                :
   * @param transactionListenerInternal :
   * @param walletFilePath              :
   * @param chainId                     :
   */
  public ValidationRestApi(
      StateManager stateManager, WalletLoader walletLoader, TransactionListenerInternal transactionListenerInternal,
      @Value("${basedir}${demowallet:${wallet}}") String walletFilePath, @Value("${chainid}") int chainId,
      WalletPosition walletPosition
  ) throws IOException {
    logger.info("RestApi ctor");
    this.stateManager = stateManager;
    this.walletLoader = walletLoader;
    this.transactionListenerInternal = transactionListenerInternal;
    this.walletFilePath = walletFilePath;
    this.chainId = chainId;

    this.walletPosition = walletPosition;
    this.addressToNonceMapper = walletPosition;
    this.addressToKeysMapper = walletPosition;
  }


  // Single/Multi Asset Clearing
  @PostMapping(path = "php/ac_create_contract.php")
  @ResponseBody
  public Map<String, Object> acContract(@RequestBody() Map<String, Object> acContractRequest) {

    return buildContractResponse(acContractRequest);
  }


  @GetMapping(path = "accounts")
  @ResponseBody
  public List<AccountResponse> accountsGet(@RequestParam() Map<String, Object> myMap) {

    return getAccounts((String) myMap.getOrDefault(ADDRESS, null));
  }


  @PostMapping(path = "accounts")
  @ResponseBody
  public List<AccountResponse> accountsPost(@RequestBody() Map<String, Object> myMap) {

    return getAccounts((String) myMap.getOrDefault(ADDRESS, null));
  }


  /**
   * Create and submit an "Add Cross Chain" transaction.
   *
   * @param addXChain the transaction definition
   *
   * @return "OK"
   */
  @PostMapping(path = "add_xchain")
  @ResponseBody
  public String addXChain(@RequestBody() AddXChain addXChain) {


    /* @param chainId          : Chain to submit Tx to.
     * @param hash             :
     * @param nonce            : Tx Nonce.
     * @param updated          : Updated Status.
     * @param fromPubKey       : Authoring Public Key
     * @param fromAddress      : Authoring Address
     * @param newChainId       : Chain to associate with.
     * @param newBlockHeight   : Block Height on the other chain.
     * @param newChainParams   : Parameters to apply to his association.
     * @param newChainSignodes : Signing Information for the other chain, at the given height.
     * @param signature        : Signature for this transaction.
     * @param height           : Not Used.
     * @param poa              : POA, if applicable.
     * @param timestamp        : TX Timestamp. */

    String poa = "";
    String signatures = "";
    String address = addXChain.fromAddr;
    if (address == null || address.isEmpty()) {
      address = addressToKeysMapper.getAddresses().iterator().next();

    }

    List<Object[]> signingnodes = addXChain.chainsignodes;
    int height = 0;
    long nonce = getNonce(address);
    KeyPair kp = getPublicPrivateKeyForAddress(address);
    AddXChainTx transaction = new AddXChainTx(chainId, "", nonce, true, kp.pubkey, address, addXChain.chainid, addXChain.chainheight, addXChain.chainparameters,
        signingnodes, signatures, height, poa, 0L
    );

    sendTx(transaction, kp.privkey);

    //TODO check is it safe
    addressToNonceMapper.increaseNonce(address);

    return "OK";
  }


  /**
   * Query state for the asset balances.
   *
   * @param assetBalanceRequest the request
   *
   * @return a map of asset name to balance
   */
  @SuppressFBWarnings("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
  @PostMapping(path = "asset_balances")
  @ResponseBody
  public Map<String, Object> assetBalances(@RequestBody() AssetBalanceRequest assetBalanceRequest) throws IOException {

    final boolean filterByNamespace = (assetBalanceRequest.namespace != null) && !assetBalanceRequest.namespace.isEmpty();
    final boolean filterByClassId = (assetBalanceRequest.classid != null) && !assetBalanceRequest.classid.isEmpty();
    final boolean hasAddress = (assetBalanceRequest.address != null) && !assetBalanceRequest.address.isEmpty();

    boolean groupByAddress = assetBalanceRequest.byaddress;
    String namespace = assetBalanceRequest.namespace;
    String classId = assetBalanceRequest.classid;
    String address = assetBalanceRequest.address;

    Map<String, Object> assetBalancesMap = new HashMap<>();

    if (!groupByAddress && hasAddress) {
      throw new IOException("Cannot specify address if by byaddress is set to false.");
    }

    if (!filterByNamespace && filterByClassId) {
      throw new IOException("Cannot filter by classid without filtering by namespace.");
    }

    if (groupByAddress) {
      if (hasAddress) {
        AddressEntry addressEntry = stateManager.getState().getAssetBalances().find(address);

        Map<String, Object> currentAssetBalance = new HashMap<>();

        if ((addressEntry != null) && (addressEntry.getClassBalance() != null)) {
          addressEntry.getClassBalance().forEach((assetId, balance) ->
              getAssetBalance(filterByNamespace, filterByClassId, namespace, classId, assetId, currentAssetBalance, balance)
          );
        }

        assetBalancesMap.put(address, currentAssetBalance);
      } else {
        stateManager.getState().getAssetBalances().forEach(addressEntry -> {
          Map<String, Object> currentAssetBalance = new HashMap<>();

          if (addressEntry != null) {
            if (addressEntry.getClassBalance() != null) {
              addressEntry.getClassBalance().forEach((assetId, balance) ->
                  getAssetBalance(filterByNamespace, filterByClassId, namespace, classId, assetId, currentAssetBalance, balance)
              );
            }
            assetBalancesMap.put(addressEntry.getKey(), currentAssetBalance);
          }
        });
      }
    } else {
      stateManager.getState().getAssetBalances().forEach(addressEntry -> {
            if ((addressEntry != null) && (addressEntry.getClassBalance() != null)) {
              addressEntry.getClassBalance().forEach((assetId, balance) ->
                  getAssetBalance(filterByNamespace, filterByClassId, namespace, classId, assetId, assetBalancesMap, balance)
              );
            }
          }
      );

    }

    return assetBalancesMap;
  }


  private Map<String, Number> balance() {

    Map<String, Number> balanceResponse = new HashMap<>();

    Balance[] balance = new Balance[]{BALANCE_ZERO};

    stateManager.getState().getAssetBalances().forEach(addressEntry -> {
      if (addressEntry.getClassBalance() != null && addressEntry.getClassBalance().containsKey(SYS_STAKE_ID)) {
        balance[0] = balance[0].add(Balance.max(BALANCE_ZERO, addressEntry.getClassBalance().get(SYS_STAKE_ID)));
      }
    });
    balanceResponse.put("balance", balance[0].getValue());

    Balance[] sigBalance = new Balance[]{BALANCE_ZERO};
    try {
      Wallet wallet = walletLoader.loadWalletFromFile(walletFilePath);
      wallet.forEachAddress(signNode -> {
        SignNodeEntry signNodeEntry = stateManager.getState().getSignNodes().find(signNode.getHexPublicKey());
        if (signNodeEntry != null) {
          sigBalance[0] = sigBalance[0].add(signNodeEntry.getBalance());
        }
      });
    } catch (IOException ioe) {
      logger.error("Unable to load wallet file: {}", walletFilePath, ioe);
    }
    balanceResponse.put("sigbalance", sigBalance[0].getValue());

    return balanceResponse;
  }


  @GetMapping(path = "balance")
  public Map<String, Number> balanceGet() {
    return balance();
  }


  @PostMapping(path = "balance")
  public Map<String, Number> balancePost() {
    return balance();
  }


  private Map<String, Object> buildContractResponse(Map<String, Object> contractRequest) {

    Map<String, Object> responseMap = new HashMap<>();
    List<Object> response = execContract(contractRequest);
    if (!response.isEmpty()) {
      responseMap.put(RESULT, response);
    }

    return responseMap;
  }


  private void calculateBalance(Map<String, Object> assetBalancesMap, String assetId, Balance balance) {

    if ((balance != null) && (balance.greaterThanZero())) {
      if (assetBalancesMap.containsKey(assetId)) {
        Balance currentBalance = new Balance(assetBalancesMap.get(assetId));
        assetBalancesMap.put(assetId, currentBalance.add(balance).getValue());
      } else {
        assetBalancesMap.put(assetId, balance.getValue());
      }
    }
  }


  /**
   * Issue a "Commit To Contract" transaction.
   *
   * @param commitRequest the transaction
   */
  @SuppressWarnings("unchecked")
  @PostMapping(path = "php/commit_contract.php")
  @ResponseBody
  public Map<String, Object> commitContract(@RequestBody() Map<String, Object> commitRequest) {

    String issuingAddress = (String) commitRequest.get(ISSUING_ADDRESS);
    String contractAddress = (String) commitRequest.get(CONTRACT_ADDRESS);
    int party = (int) commitRequest.get("party");
    List<Object> commitment = (List<Object>) commitRequest.get("commitment");
    List<Object> receive = (List<Object>) commitRequest.get("receive");
    KeyPair issuingKp = getPublicPrivateKeyForAddress(issuingAddress);

    Map<String, Object> commitDataMap = new HashMap<>();
    commitDataMap.put(CONTRACT_FUNCTION, CONTRACT_NAME_DVP_UK_COMMIT);
    commitDataMap.put(ISSUING_ADDRESS, issuingAddress);
    commitDataMap.put(CONTRACT_ADDRESS, contractAddress);
    commitDataMap.put("party", setParty(party, contractAddress, issuingKp));
    commitDataMap.put("commitment", setCommitmentArray(commitment, contractAddress, issuingKp));
    commitDataMap.put("receive", toObjectArray(receive));

    Map<String, Object> responseMap = new HashMap<>();
    List<Object> response = execContract(commitDataMap);
    if (!response.isEmpty()) {
      responseMap.put(RESULT, response);
      responseMap.put(PROTOCOL, commitRequest.get(PROTOCOL));
    }

    return responseMap;
  }


  /**
   * Create and send a contract transaction. The contract type is determined by the input.
   *
   * @param myMap the contract specification
   *
   * @return An array of the form [ 1, {response} ]
   */
  @PostMapping(path = "/contract")
  @ResponseBody
  public List<Object> contract(@RequestBody() Map<String, Object> myMap) {

    return (execContract(myMap));

  }


  /**
   * Request the details of a contract.
   *
   * @param contractDetailRequest the request
   *
   * @return the details.
   */
  @PostMapping(path = "php/contract_detail.php")
  @ResponseBody
  public Map<String, Object> contractDetail(@RequestBody() Map<String, Object> contractDetailRequest) {

    String contractAddress = (String) contractDetailRequest.get("addr");

    Map<String, Object> responseMap = new HashMap<>();
    ((ContractsList) stateManager.getState().getContracts()).forEachContract(address -> {
      if (address.getContractAddress().equals(contractAddress)) {
        Map<String, Object> contractData = address.getContractData().encodeToMap();
        responseMap.put(RESULT, contractData);
        responseMap.put("addr", contractAddress);
      }
    });

    return responseMap;
  }


  /**
   * Request the details of a contract.
   *
   * @param requestDetails the request
   *
   * @return the details.
   */
  @GetMapping(path = "/contract_details")
  @ResponseBody
  public Map<String, Object> contractDetailsGet(@RequestParam() Map<String, Object> requestDetails) {

    return getContractDetails(requestDetails);
  }


  /**
   * Request the details of a contract.
   *
   * @param requestDetails the request
   *
   * @return the details.
   */
  @PostMapping(path = "/contract_details")
  @ResponseBody
  public Map<String, Object> contractDetailsPost(@RequestBody() Map<String, Object> requestDetails) {

    return getContractDetails(requestDetails);
  }


  /**
   * Perform a stock split.
   *
   * @param request details of the split
   *
   * @return summary of the split
   */
  @PostMapping(
      path = "php/corp_action.php",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  public Map<String, Object> corporateAction(@RequestParam Map<String, Object> request) {

    String action = (String) request.get("action");
    Map<String, Object> responseMap = new HashMap<>();

    if (!"split".equals(action)) {
      return Collections.emptyMap();
    }

    StockSplit stockSplit = new StockSplit();
    stockSplit.contractname = STOCK_SPLIT;
    stockSplit.classid = (String) request.get(CLASS_ID);
    stockSplit.namespace = (String) request.get(NAMESPACE);
    stockSplit.ratio = Double.parseDouble((String) request.get("ratio"));
    List<Object> response = execStockSplit(stockSplit);
    responseMap.put(RESULT, response);

    return responseMap;
  }


  // Delivery vs. Payment UK
  @PostMapping(path = "php/dvp_create_contract.php")
  @ResponseBody
  public Map<String, Object> dvpUkContract(@RequestBody() Map<String, Object> dvpContractRequest) {

    return buildContractResponse(dvpContractRequest);
  }


  private String encodeMetadata(Object metadata) {

    try {
      byte[] bb = MsgPackUtil.pack((metadata == null ? "" : metadata));
      return new String(Base64.getEncoder().encode(bb), ByteUtil.BINCHARSET);
    } catch (Exception e) {
      return "oA==";
    }

  }


  Map<String, Object> encumber(@RequestBody() Map<String, Object> requestDetails) {

    String reference = (String) requestDetails.getOrDefault("reference", "");
    String fromaddr = (String) requestDetails.getOrDefault(ADDRESS, requestDetails.getOrDefault("fromaddr", ""));
    String subjectaddress = (String) requestDetails.getOrDefault("subjectaddress", "");
    String namespace = (String) requestDetails.getOrDefault(NAMESPACE, "");
    String classid = (String) requestDetails.getOrDefault(CLASS_ID, "");
    String protocol = (String) requestDetails.getOrDefault(PROTOCOL, "");
    String metadata = (String) requestDetails.getOrDefault("metadata", "");
    String poa = (String) requestDetails.getOrDefault("poa", "");
    Number amount = ((Number) requestDetails.getOrDefault("amount", 0L));
    List beneficiaries = (List) requestDetails.getOrDefault("beneficiaries", new ArrayList<>());
    List administrators = (List) requestDetails.getOrDefault("administrators", new ArrayList<>());

    KeyPair kp = getPublicPrivateKeyForAddress(fromaddr);
    long nonce = getNonce(fromaddr);

    HashMap<String, Object> propertiesDict = new HashMap<>();
    propertiesDict.put("reference", listToObject(reference));
    propertiesDict.put("beneficiaries", listToObject(beneficiaries));
    propertiesDict.put("administrators", listToObject(administrators));

    EncumberTx tx = Encumber.encumberUnsigned(chainId, nonce, kp.pubkey, fromaddr, namespace, classid, subjectaddress, amount,
        propertiesDict, protocol, metadata, poa
    );

    sendTx(tx, addressToKeysMapper.getPrivateKey(fromaddr));

    addressToNonceMapper.increaseNonce(fromaddr);

    HashMap<String, Object> thisMap = new HashMap<>();
    thisMap.put(HASH, tx.getHash());
    thisMap.put(UPDATED, tx.isGood());
    thisMap.put(TX_TYPE, tx.getTxType().getId());
    thisMap.put(BASE_CHAIN, tx.getChainId());
    thisMap.put(FROM_ADDR, tx.getFromAddress());
    thisMap.put(CREATION, tx.getTimestamp());
    thisMap.put(POA, tx.getPowerOfAttorney());
    thisMap.put(SIGNATURE, tx.getSignature());
    thisMap.put(NONCE, tx.getNonce());
    thisMap.put(NAMESPACE, tx.getNameSpace());
    thisMap.put(CLASSID, tx.getClassId());
    thisMap.put(METADATA, tx.getMetadata());

    return thisMap;
  }


  @GetMapping(path = "/encumber")
  @ResponseBody
  public Map<String, Object> encumberGet(@RequestParam() Map<String, Object> requestDetails) {

    return encumber(requestDetails);
  }


  @PostMapping(path = "/encumber")
  @ResponseBody
  public Map<String, Object> encumberPost(@RequestBody() Map<String, Object> requestDetails) {

    return encumber(requestDetails);
  }


  private List<Object> execContract(Map<String, Object> contractRequest) {

    String contractFunction = "missing";

    if (contractRequest.containsKey(FUNCTION)) {
      contractFunction = (String) contractRequest.getOrDefault(FUNCTION, "");
      contractRequest.put(CONTRACT_FUNCTION, contractFunction.toLowerCase());
    } else if (contractRequest.containsKey(CONTRACT_FUNCTION)) {
      contractFunction = (String) contractRequest.getOrDefault(CONTRACT_FUNCTION, "");
      contractRequest.put(FUNCTION, contractFunction.toLowerCase());
    } else if (contractRequest.containsKey("contractname")) {
      contractFunction = (String) contractRequest.getOrDefault("contractname", "");
      contractRequest.put(FUNCTION, contractFunction.toLowerCase());
    }

    String issuingAddress = (String) contractRequest.get(ISSUING_ADDRESS);

    if (issuingAddress == null || issuingAddress.isEmpty()) {
      issuingAddress = getRandomAddress();
    }

    IContractData txContractData;

    KeyPair issuingKp = getPublicPrivateKeyForAddress(issuingAddress);
    long nonce = getNonce(issuingAddress);
    long timestamp = getTimestamp();

    Object txResponse = null;
    AbstractTx sentTx = null;

    switch (contractFunction) {

      case STOCK_SPLIT:

        StockSplit thisStocksplit = new StockSplit();
        thisStocksplit.contractname = (String) contractRequest.getOrDefault(FUNCTION, "");
        thisStocksplit.fromaddress = (String) contractRequest.getOrDefault("fromaddress", "");
        thisStocksplit.namespace = (String) contractRequest.getOrDefault(NAMESPACE, "");
        thisStocksplit.classid = (String) contractRequest.getOrDefault(CLASS_ID, "");
        thisStocksplit.ratio = ((Number) contractRequest.getOrDefault("ratio", 0.0)).doubleValue();

        return execStockSplit(thisStocksplit);

      case CONTRACT_NAME_DVP_UK:
        int now = TimeUtil.unixTime();

        if (!contractRequest.containsKey("startdate")) {
          contractRequest.put("startdate", now);
        }
        if (!contractRequest.containsKey("expiry")) {
          contractRequest.put("expiry", now + 86400); // 86400 as per prior python.
        }

        txContractData = new DvpUkContractData(contractRequest);

        NewContractTx txDvp = new NewContractTx(chainId, "", nonce, false, issuingKp.pubkey, issuingAddress, null, "", -1, "", timestamp);

        // Auto Sign ?
        if (((DvpUkContractData) txContractData).getAutosign()) {
          signDvpUkContractData((DvpUkContractData) txContractData, txDvp.getContractAddress(), txDvp.isPOA(), txDvp.getAuthoringAddress());
        }

        txDvp.setContractDictionary(txContractData.encode());

        sentTx = txDvp;

        txResponse = new ContractResponse(0, txDvp.getChainId(), txDvp.getHeight(), txContractData.encodeToMap(), txDvp.getTimestamp(), txDvp.getFromAddress(),
            txDvp.getHash(), txDvp.getPowerOfAttorney(), txDvp.getContractAddress(), 0, txDvp.getTxType().getId(), txDvp.getTxType().toString(), false
        );

        break;

      case CONTRACT_NAME_DVP_UK_COMMIT:

        String contractAddress = (String) contractRequest.get(CONTRACT_ADDRESS);

        txContractData = new DvpUKCommitData(contractRequest);

        signDvpUkCommitmentData((DvpUKCommitData) txContractData, contractAddress, issuingAddress, false);

        CommitToContractTx txDvpCommit = new CommitToContractTx(chainId, "", nonce, false, issuingKp.pubkey, issuingAddress, contractAddress,
            null, "", -1, "", timestamp
        );

        txDvpCommit.setCommitmentDictionary(txContractData.encode());

        sentTx = txDvpCommit;

        // TODO : Cope with ContractAddressArray

        txResponse = new ContractResponse(0, txDvpCommit.getChainId(), txDvpCommit.getHeight(), txContractData.encodeToMap(), txDvpCommit.getTimestamp(),
            txDvpCommit.getFromAddress(),
            txDvpCommit.getHash(), txDvpCommit.getPowerOfAttorney(), txDvpCommit.getContractAddress().get(0), 0, txDvpCommit.getTxType().getId(),
            txDvpCommit.getTxType().toString(), false
        );

        break;

      default:

        break;
    }

    if (sentTx != null) {
      sendTx(sentTx, addressToKeysMapper.getPrivateKey(issuingAddress));
      addressToNonceMapper.increaseNonce(issuingAddress);
    }

    List<Object> response = new ArrayList<>();
    response.add(1);
    response.add(txResponse);

    return response;
  }


  private List<Object> execStockSplit(StockSplit stockSplitRequest) {

    String contractName = stockSplitRequest.contractname;
    String fromAddress = stockSplitRequest.fromaddress;
    String namespace = stockSplitRequest.namespace;
    String classId = stockSplitRequest.classid;
    double ratio = stockSplitRequest.ratio;

    if (contractName == null || contractName.isEmpty() || !STOCK_SPLIT.equals(contractName)) {
      logger.error("Contract name for stock split is invalid: {}", StringUtils.logSafe(contractName));
      return Collections.emptyList();
    }

    if (fromAddress == null || fromAddress.isEmpty()) {
      fromAddress = getRandomAddress();
    }

    long nonce = getNonce(fromAddress);
    long timestamp = getTimestamp();
    KeyPair kp = getPublicPrivateKeyForAddress(fromAddress);

    StockSplitTx tx = new StockSplitTx(chainId, INT_1, null, nonce, false, kp.pubkey, fromAddress, namespace, classId, null,
        ratio, "", null, 0, null, timestamp
    );

    sendTx(tx, addressToKeysMapper.getPrivateKey(fromAddress));
    addressToNonceMapper.increaseNonce(fromAddress);

    StockSplitResponse txResponse = new StockSplitResponse(tx.getClassId(), tx.getNonce(), tx.getTimestamp(), tx.getHash(), tx.getReferenceStateHash(),
        tx.getFromAddress(), tx.getChainId(), tx.getNameSpace(), tx.getTxType().toString(), tx.getSignature(), tx.getRatio(), tx.getPowerOfAttorney()
    );

    List<Object> response = new ArrayList<>();
    response.add(1);
    response.add(txResponse);

    return response;
  }


  private List<AccountResponse> getAccounts(String address) {

    List<AccountResponse> accountResponses = new ArrayList<>();
    try {
      Map<String, Object[]> walletDataMap = new HashMap<>();
      Merkle<AddressEntry> assetBalances = stateManager.getState().getAssetBalances();

      walletLoader.loadWalletFromFile(walletFilePath).forEachAddress(entry -> {
        String addr = AddressUtil.publicKeyToAddress(entry.getHexPublicKey(), AddressType.NORMAL);
        AddressEntry addressEntry = assetBalances.find(addr);
        Number balanceAmount = (addressEntry != null) ? addressEntry.getAssetBalance(SYS_STAKE_ID).getValue() : 0L;
        walletDataMap.put(addr, new Object[]{addressToKeysMapper.getPublicKey(addr), balanceAmount});
      });

      if (address == null || address.length() == 0) {
        walletDataMap.forEach((currentAddress, data) -> accountResponses.add(new AccountResponse((String) data[0], (Number) data[1], currentAddress)));
      } else {
        Object[] data = walletDataMap.get(address);
        if (data == null) {
          accountResponses.add(new AccountResponse("", 0L, address));
        } else {
          accountResponses.add(new AccountResponse((String) data[0], (Number) data[1], address));
        }
      }
    } catch (IOException e) {
      logger.error("accounts:", e);
      throw new RuntimeException(e);
    }

    return accountResponses;
  }


  private void getAssetBalance(
      boolean filterByNamespace, boolean filterByClassId, String namespace, String classId, String assetId,
      Map<String, Object> assetBalancesMap, Balance balance
  ) {

    String[] namespaceAndClassId = assetId.split("\\|");

    if (filterByNamespace) {
      if (filterByClassId) {
        if (namespace.equals(namespaceAndClassId[0]) && classId.equals(namespaceAndClassId[1])) {
          calculateBalance(assetBalancesMap, assetId, balance);
        }
      } else {
        if (namespace.equals(namespaceAndClassId[0])) {
          calculateBalance(assetBalancesMap, assetId, balance);
        }
      }
    } else {
      calculateBalance(assetBalancesMap, assetId, balance);
    }
  }


  private void getClasses(NamespaceEntry entry, List<Object[]> classes) {

    entry.getAllAssetNames().forEach(namespaceClass -> {
      String m = entry.getClassMetadata(namespaceClass);
      try {
        Object meta = Collections.emptyMap();
        if (m != null) {
          byte[] raw = Base64.getDecoder().decode(m.getBytes(ByteUtil.BINCHARSET));
          Object obj = MsgPackUtil.unpackObject(MsgPackUtil.newUnpacker(raw));
          meta = MPWrappedUnwrap.unwrap(obj);
        }
        classes.add(new Object[]{entry.getAddress(), entry.getKey(), namespaceClass, meta});
      } catch (IOException e) {
        logger.error("Failed to access message packed B64 metadata:{}", StringUtils.logSafe(m), e);
      }
    });
  }


  Map<String, Object> getContractDetails(Map<String, Object> requestDetails) {

    String contractAddress = "";
    Map<String, Object> responseMap;

    if (requestDetails.containsKey(ADDRESS)) {
      contractAddress = (String) requestDetails.getOrDefault(ADDRESS, "");
    }

    ContractEntry thisContract = stateManager.getState().getContracts().find(contractAddress);

    if (thisContract != null) {
      responseMap = thisContract.getContractData().encodeToMap();
    } else {
      responseMap = new HashMap<>();
    }

    return responseMap;
  }


  private long getNonce(String address) {

    return addressToNonceMapper.getNonce(address);
  }


  private List<NonceResponse> getNonces(String address) {

    List<NonceResponse> nonceResponses = new ArrayList<>();
    if ((address == null) || (address.length() == 0)) {
      addressToNonceMapper.getAddresses().forEach(addr -> nonceResponses.add(new NonceResponse(addressToNonceMapper.getNonce(addr), addr)));
    } else {
      nonceResponses.add(new NonceResponse(addressToNonceMapper.getNonce(address), address));
    }

    return nonceResponses;

  }


  /**
   * Get the addresses owned by this validation node and their STAKE balance.
   *
   * @param request the request
   *
   * @return the address and balance details.
   */
  @PostMapping(
      path = "php/getOwnedAddr.php",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  public Map<String, Object> getOwnedAddress(@RequestParam Map<String, Object> request) {

    Address address = new Address();
    address.address = (String) request.get(ADDRESS);
    List<AccountResponse> response = getAccounts(address.address);
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(RESULT, response);

    return responseMap;
  }


  KeyPair getPublicPrivateKeyForAddress(String address) {

    WalletAddress akp = addressToKeysMapper.getKeyPair(address);
    KeyPair kp = new KeyPair();
    kp.pubkey = akp.getHexPublicKey();

    kp.privkey = Hex.encode(akp.getPrivateKey().getEncoded());

    return kp;
  }


  private String getRandomAddress() {

    List<String> walletAddresses = new ArrayList<>();
    try {
      walletLoader.loadWalletFromFile(walletFilePath).forEachAddress(entry -> walletAddresses.add(
          AddressUtil.publicKeyToAddress(entry.getHexPublicKey(), AddressType.NORMAL)
      ));
    } catch (IOException ioe) {
      logger.error("Failed to access wallet file:{}", walletFilePath, ioe);
    }

    return walletAddresses.get(0);
  }


  public long getTimestamp() {

    return TimeUtil.unixTime();
  }


  /**
   * Perform an asset issue transaction.
   *
   * @param issueAsset details of the transaction
   *
   * @return results of the transaction
   */
  @PostMapping(path = "issue_asset")
  @ResponseBody
  public IssueAssetResponse issueAssetClass(@RequestBody() IssueAsset issueAsset) {

    logger.info("amount:{}", issueAsset.amount);
    Number amount = issueAsset.amount;
    String toaddr = issueAsset.toaddr;
    String nameSpace = issueAsset.namespace;
    String classId = issueAsset.classid;
    String protocol = issueAsset.protocol;
    String metadata = issueAsset.metadata;
    String fromAddress = issueAsset.fromaddr;

    long timestamp = getTimestamp();

    NamespaceEntry entry = stateManager.getState().getNamespaces().find(nameSpace);
    if (entry != null) {
      fromAddress = entry.getAddress();
    }

    KeyPair kp = getPublicPrivateKeyForAddress(fromAddress);
    long nonce = getNonce(fromAddress);

    AssetIssueTx tx = new AssetIssueTx(chainId, null, nonce, true, kp.pubkey, fromAddress, nameSpace,
        classId, toaddr, amount, null, -1, null, timestamp, protocol, metadata
    );

    sendTx(tx, addressToKeysMapper.getPrivateKey(fromAddress));
    addressToNonceMapper.increaseNonce(fromAddress);

    return new IssueAssetResponse(tx.getClassId(), tx.getNonce(), tx.getTimestamp(), tx.getHash(), tx.getFromAddress(), tx.getChainId(), tx.getNameSpace(),
        tx.getTxType().toString(), tx.getAmount(), tx.getToAddress(), tx.getSignature(), tx.getProtocol(), tx.getPowerOfAttorney()
    );
  }


  /**
   * Get the asset classes. Optionally, only those asset classes owned by a specific address.
   *
   * @param myMap the request
   *
   * @return the class details
   */
  @PostMapping(path = "list_classes")
  @ResponseBody
  public List<Object[]> listClasses(@RequestBody() Map<String, Object> myMap) {

    List<Object[]> classes = new ArrayList<>();
    Merkle<NamespaceEntry> namespaceList = stateManager.getState().getNamespaces();
    if (myMap.get(ADDRESS) == null || myMap.get(ADDRESS).toString().isEmpty()) {
      namespaceList.forEach(entry -> getClasses(entry, classes));
    } else {
      String address = myMap.get(ADDRESS).toString();

      namespaceList.forEach(entry -> {
        if (address.equals(entry.getAddress())) {
          getClasses(entry, classes);
        }
      });
    }

    return classes;
  }


  /**
   * Get the asset classes. Optionally, only those asset classes owned by a specific address.
   *
   * @param myMap the request
   *
   * @return the class details
   */
  @GetMapping(path = "list_classes")
  @ResponseBody
  public List<Object[]> listClassesGet(@RequestBody() Map<String, Object> myMap) {

    return listClasses(myMap);

  }


  /**
   * Get the asset namespaces. Optionally, only those asset namespaces owned by a specific address.
   *
   * @param myMap the request
   *
   * @return the namespace details
   */
  @PostMapping(path = "list_namespaces")
  @ResponseBody
  public Set<String> listNamespaces(@RequestBody() Map<String, Object> myMap) {

    final String address = ((myMap.get(ADDRESS) == null || myMap.get(ADDRESS).toString().isEmpty()) ? null : myMap.get(ADDRESS).toString());
    final Set<String> addressNamespaces = new HashSet<>();

    Merkle<NamespaceEntry> namespaceTree = stateManager.getState().getNamespaces();
    namespaceTree.forEach(c -> {
      if ((address == null) || (c.getAddress().equalsIgnoreCase(address))) {

        addressNamespaces.add(c.getKey());
      }
    });

    return addressNamespaces;
  }


  /**
   * Get the asset namespaces. Optionally, only those asset namespaces owned by a specific address.
   *
   * @param myMap the request
   *
   * @return the namespace details
   */
  @GetMapping(path = "list_namespaces")
  @ResponseBody
  public Set<String> listNamespacesHet(@RequestBody() Map<String, Object> myMap) {

    return listNamespaces(myMap);

  }


  private Object listToObject(Object thisList) {

    if (!(thisList instanceof List)) {
      return thisList;
    }

    List myList = (List) thisList;

    int size = myList.size();
    Object[] rVal = new Object[size];

    for (int i = 0; i < size; i++) {
      rVal[i] = listToObject(myList.get(i));
    }
    return rVal;
  }


  private String newAddress() {
    final WalletAddress walletAddress;

    //TODO - This should be ED25519 curve, new WalletAddress fails for ED25519Raw
    java.security.KeyPair pubpriv = KeyGen.generateKeyPair();

    try {
      Wallet wallet = walletLoader.loadWalletFromFile(walletFilePath);
      walletAddress = new WalletAddress(wallet.getId(), pubpriv, AddressType.NORMAL);
      wallet.addAddressEntry(walletAddress);
      walletLoader.saveWalletToFile(walletFilePath, wallet);

      addressToKeysMapper.addAddress(walletAddress.getAddress(), pubpriv);

    } catch (IOException | GeneralSecurityException ioe) {

      throw new RuntimeException(ioe);
    }

    return walletAddress.getAddress();
  }


  @GetMapping(path = "new_address")
  @ResponseBody
  public synchronized String newAddressGet() {
    return newAddress();
  }


  @PostMapping(path = "new_address")
  @ResponseBody
  public synchronized String newAddressPost() {
    return newAddress();
  }


  /**
   * Get the current nonce for a specified address.
   *
   * @param address the address
   *
   * @return the nonce
   */
  @GetMapping(path = "nonces")
  @ResponseBody
  public List<NonceResponse> noncesGet(@RequestParam() Address address) {

    return getNonces((address == null ? null : address.address));
  }


  /**
   * Get the current nonce for a specified address.
   *
   * @param address the address
   *
   * @return the nonce
   */
  @PostMapping(path = "nonces")
  @ResponseBody
  public List<NonceResponse> noncesPost(@RequestBody() Address address) {

    return getNonces((address == null ? null : address.address));

  }


  @Override
  public void onApplicationEvent(StateInitializedEvent event) {
    Merkle<AddressEntry> assetBalances = stateManager.getState().getAssetBalances();
    try {
      walletPosition.load(walletFilePath, assetBalances);
    } catch (IOException e) {
      logger.error("Failed to initialise validation REST API", e);
    }
  }


  /**
   * Register an address in the block chain. (Not yet implemented)
   *
   * @param registerAddress the specification of the new address
   *
   * @return "OK" (Not yet implemented)
   */
  // TODO: Not complete yet
  @PostMapping(path = "register_address")
  @ResponseBody
  public String registerAddress(@RequestBody() RegisterAddress registerAddress) {

    return "OK";
  }


  /**
   * Create and issue a "Register Asset Class" transaction.
   *
   * @param registerAsset the transaction specification
   *
   * @return the transaction response
   */
  @PostMapping(path = "register_asset")
  @ResponseBody
  public RegisterAssetResponse registerAssetClass(@RequestBody() RegisterAsset registerAsset) {

    logger.info("meta:{}", registerAsset.metadata);
    String fromaddr = registerAsset.fromaddr;
    String nameSpace = registerAsset.namespace;
    String classId = registerAsset.classid;
    String metadata = encodeMetadata(registerAsset.metadata);

    if (fromaddr == null || fromaddr.isEmpty()) {
      fromaddr = getRandomAddress();
    }

    long nonce = getNonce(fromaddr);
    long timestamp = getTimestamp();
    KeyPair kp = getPublicPrivateKeyForAddress(fromaddr);

    AssetClassRegisterTx tx = new AssetClassRegisterTx(chainId, INT_1, null, nonce, true, kp.pubkey, fromaddr, nameSpace, classId, metadata, null, 0, null,
        timestamp
    );

    sendTx(tx, addressToKeysMapper.getPrivateKey(fromaddr));
    addressToNonceMapper.increaseNonce(fromaddr);

    return new RegisterAssetResponse(tx.getClassId(), tx.getNonce(), tx.getTimestamp(), tx.getHash(), tx.getFromAddress(), tx.getChainId(), tx.getNameSpace(),
        tx.getSignature(), tx.getTxType().toString(), tx.getPowerOfAttorney()
    );
  }


  /**
   * Create and issue a "Register Namespace" transaction.
   *
   * @param namespace the namespace specification
   *
   * @return the transaction response
   */
  @PostMapping(path = "register_namespace")
  @ResponseBody
  public NamespaceRegisterResponse registerNamespace(@RequestBody() Namespace namespace) throws IOException {

    if (namespace.namespace == null || namespace.namespace.isEmpty()) {
      throw new IOException("Namespace invalid");
    }

    String nameSpace = namespace.namespace;
    String fromaddr = namespace.fromaddr;
    String metadata = (namespace.metadata != null) ? namespace.metadata.toString() : "oA==";

    if (fromaddr == null || fromaddr.isEmpty()) {
      fromaddr = getRandomAddress();
    }

    long nonce = getNonce(fromaddr);
    long timestamp = getTimestamp();
    KeyPair kp = getPublicPrivateKeyForAddress(fromaddr);

    NamespaceRegisterTx tx = new NamespaceRegisterTx(chainId, null, nonce, true, kp.pubkey, fromaddr, nameSpace, metadata, null, 0, null, timestamp);

    sendTx(tx, addressToKeysMapper.getPrivateKey(fromaddr));
    addressToNonceMapper.increaseNonce(fromaddr);

    return new NamespaceRegisterResponse(tx.getNonce(), tx.getTimestamp(), tx.getHash(), tx.getFromAddress(), tx.getChainId(), tx.getNameSpace(),
        tx.getSignature(), tx.getTxType().toString(), tx.getPowerOfAttorney()
    );
  }


  private void sendTx(AbstractTx tx, byte[] privateKeyBytes) {

    logger.info("Connect done : sendTx(byte[])");

    tx.setHash(Hash.computeHash(tx));
    Sign.signTransaction(tx, privateKeyBytes);

    sendTx(tx);
  }


  private void sendTx(AbstractTx tx, String privateKey) {

    logger.info("Connect done : sendTx(String)");

    tx.setHash(Hash.computeHash(tx));
    byte[] privateKeyBytes = ByteUtil.hexToBytes(privateKey);
    Sign.signTransaction(tx, privateKeyBytes);

    sendTx(tx);
  }


  private void sendTx(AbstractTx tx) {

    Object[] encodedTx = tx.encodeTx();
    Object[][] txArray = new Object[][]{encodedTx};
    transactionListenerInternal.transactionReceivedInternal(new TxPackage(P2PType.TX_PACKAGE_ORIGINAL, tx.getChainId(), txArray));

    logger.info("Send complete");
  }


  private Object[] setCommitmentArray(List<Object> commitments, String contractAddress, KeyPair partyKeyPair) {

    List<Object> commitmentArray = new ArrayList<>();

    commitments.forEach(commitment -> {
      Object[] commitmentData = new Object[3];

      commitmentData[0] = ((ArrayList) commitment).get(0);
      commitmentData[1] = partyKeyPair.pubkey;

      JSONHashAccumulator accumulator = new JSONHashAccumulator();
      accumulator.add(contractAddress);
      accumulator.add((String) ((ArrayList) commitment).get(1));
      accumulator.add((String) ((ArrayList) commitment).get(2));
      accumulator.add((int) ((ArrayList) commitment).get(3));
      String hash = Hex.encode(SHA256.sha256(accumulator.getBytes()));
      commitmentData[2] = MessageVerifierFactory.get().createSignatureB64(hash, partyKeyPair.privkey);

      commitmentArray.add(commitmentData);
    });

    return commitmentArray.toArray();
  }


  private Object[] setParty(int party, String contractAddress, KeyPair issuingKp) {

    return new Object[]{Integer.toString(party), issuingKp.pubkey, MessageVerifierFactory.get().createSignatureB64(contractAddress, issuingKp.privkey)};
  }


  /**
   * Set the access control headers for the response.
   *
   * @param response the response to configure
   */
  @ModelAttribute
  @SuppressFBWarnings("PERMISSIVE_CORS")
  public void setVaryResponseHeader(HttpServletResponse response) {

    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
    response.setHeader("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, X-Requested-With, X-CSRF-Token");
    response.setHeader("Access-Control-Expose-Headers", "Content-Length");
  }


  void signDvpUkCommitmentData(
      DvpUKCommitData contractData, String contractAddress, String issuingAddress,
      boolean isPoA
  ) {

    WalletAddress akp = addressToKeysMapper.getKeyPair(issuingAddress);
    if (akp == null) {
      return;
    }

    DvpCommit.sign(addressToKeysMapper, contractData, contractAddress, akp, isPoA);
  }


  void signDvpUkContractData(
      DvpUkContractData contractData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress
  ) {

    DvpContract.sign(addressToKeysMapper, contractData, contractAddress, isPoA, authoringAddress);
  }


  /**
   * Get the list of validation nodes from a specific state height.
   *
   * @param body request, possibly specifying "height"
   *
   * @return list of signing node specifications.
   */
  @PostMapping(path = "signode_list")
  @ResponseBody
  public List<Object[]> signodeListPost(@RequestBody() Map<String, Integer> body) {

    try {

      State loadedState;
      if (body.containsKey("height")) {
        loadedState = stateManager.getState(body.get("height"));
      } else {
        loadedState = stateManager.getState();
      }
      List<Object[]> listSigningNodes = new ArrayList<>();
      Merkle<SignNodeEntry> signodes = loadedState.getSignNodes();
      for (SignNodeEntry signingnode : signodes) {
        listSigningNodes.add(new Object[]{
            signingnode.getHexPublicKey(),
            signingnode.getBalance().getValue()
        });
      }
      return listSigningNodes;
    } catch (DBStoreException | NoStateFoundException e) {
      logger.error("Failed to retrieve and access saved state", e);
      throw new RuntimeException("Invalid request", e);
    }


  }


  @PostMapping(path = STOCK_SPLIT)
  @ResponseBody
  public List<Object> stockSplit(@RequestBody() StockSplit stockSplitRequest) {

    return execStockSplit(stockSplitRequest);
  }


  private Object[] toObjectArray(List<Object> data) {

    Object[] dataArray = data.toArray();

    for (int i = 0; i < dataArray.length; i++) {
      if (dataArray[i] instanceof ArrayList) {
        dataArray[i] = ((ArrayList) dataArray[i]).toArray();
      }
    }

    return dataArray;
  }


  /**
   * Create and issue a Cross Chain Asset Transfer transaction.
   *
   * @param payload the transaction definition.
   *
   * @return empty map
   */
  @PostMapping(path = "transfer_xchain_asset")
  @ResponseBody
  public Map<String, Object> transferXchainAsset(@RequestBody() Map<String, Object> payload) {

    int tochain = (payload.containsKey("tochain") ? Integer.parseInt(payload.get("tochain").toString()) : chainId);
    String classid = payload.getOrDefault(CLASS_ID, "").toString();
    String namespace = payload.getOrDefault(NAMESPACE, "").toString();
    Number amount = (new Balance(payload.getOrDefault("amount", 0L))).getValue();

    String toaddr = payload.getOrDefault("toaddr", "").toString();
    String fromaddr = payload.getOrDefault("fromaddr", "").toString();
    long nonce = getNonce(fromaddr);
    KeyPair kp = getPublicPrivateKeyForAddress(fromaddr);

    AssetTransferXChainTx tx = new AssetTransferXChainTx(
        chainId,
        "",
        nonce,
        true,
        kp.pubkey,
        fromaddr,
        namespace,
        classid,
        tochain,
        toaddr,
        amount,
        "",
        "",
        "",
        -1,
        "", Instant.now().getEpochSecond()
    );

    sendTx(tx, addressToKeysMapper.getPrivateKey(fromaddr));

    addressToNonceMapper.increaseNonce(fromaddr);
    return Collections.emptyMap();
  }
}
