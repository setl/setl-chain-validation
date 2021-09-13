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
package io.setl.scenario;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.pychain.event.TransactionListenerInternal;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.tx.AssetTransferTx;
import io.setl.bc.pychain.state.tx.Sign;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.Balance;
import io.setl.common.CommonPy.P2PType;
import io.setl.rest.util.WalletPosition;
import io.setl.rest.util.WalletPosition.AddressPosition;
import io.setl.util.LoggedThread;
import io.setl.utils.debug.Ticker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SuppressFBWarnings("PREDICTABLE_RANDOM")
@Component
public class ScenarioProcessor {

  private static final int INT_1 = 4; // Legacy

  private static final String METADATA = "oA==";

  private static final String NS_SEPARATOR = "|";

  private static final int TXPOOLMAXBEFOREOVERLOAD = 200_000;

  private static final Logger logger = LoggerFactory.getLogger(ScenarioProcessor.class);



  static class AddressClass {

    String address;

    String nsCls;


    AddressClass(String address, String nsCls) {

      this.address = address;
      this.nsCls = nsCls;
    }
  }



  private final int chainId;

  private final WalletPosition currentPosition;

  private final AsyncTaskExecutor demoExecutor;

  private final Random random = new Random();

  private final ScenarioStates scenarioStates;

  private final StateManager stateManager;

  private final TransactionListenerInternal transactionListenerInternal;

  private final TransactionPool txPool;

  boolean initRequired = true;

  private String walletFilePath;


  /**
   * ScenarioProcessor.
   *
   * @param demoExecutor                :
   * @param stateManager                :
   * @param eventBus                    :
   * @param scenarioStates              :
   * @param transactionListenerInternal :
   * @param txPool                      :
   * @param currentPosition             :
   * @param walletFilePath              :
   * @param chainId                     :
   *
   * @throws IOException :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public ScenarioProcessor(
      @Qualifier("demoTransactionTaskExecutor") final AsyncTaskExecutor demoExecutor, final StateManager stateManager,
      final EventBus eventBus, final ScenarioStates scenarioStates,
      final TransactionListenerInternal transactionListenerInternal, final TransactionPool txPool, WalletPosition currentPosition,
      @Value("${basedir}${demowallet:${wallet}}") String walletFilePath, @Value("${chainid}") int chainId
  )
      throws IOException {

    this.stateManager = stateManager;
    eventBus.register(this);
    this.scenarioStates = scenarioStates;
    this.transactionListenerInternal = transactionListenerInternal;
    this.txPool = txPool;
    this.walletFilePath = walletFilePath;
    this.chainId = chainId;
    this.demoExecutor = demoExecutor;
    this.currentPosition = currentPosition;

    reloadWalletAndAddresses();
  }


  private Map<String, Object> decodeMetadata(String metadata) throws IOException {

    byte[] data = Base64.getDecoder().decode(metadata);
    Object o = MsgPackUtil.unpackObject(MsgPackUtil.newUnpacker(data), false);
    if (o instanceof MPWrappedMap) {
      return ((MPWrappedMap) o).toMap();
    } else {
      return Collections.emptyMap();
    }
  }


  /**
   * handleScenarioChangeEvent.
   *
   * @param scenarioChangeEvent :
   */
  @Subscribe
  public void handleScenarioChangeEvent(ScenarioChangeEvent scenarioChangeEvent) {

    if (logger.isInfoEnabled()) {
      if (scenarioChangeEvent.scenario == null) {
        logger.info("Overdrive changed");
      } else {
        logger.info("Scenario changed:{}", scenarioChangeEvent.scenario.name());
      }
    }
  }


  private boolean isOverloaded() {

    return txPool.getAvailableTransactionCount() > TXPOOLMAXBEFOREOVERLOAD;
  }


  private int postScenarioTransactions(ScenarioState scenarioState, int txToDo) {

    long chunkSize = 100L;
    List<String> goodAssets = scenarioState.getGoodAssets();
    List<AddressClass> choices = new ArrayList<>();
    currentPosition.getMap().forEach((addr, addressPosition) ->
        goodAssets.forEach(nsCls -> {
          Balance bal = new Balance(addressPosition.nsClsBalance.get(nsCls));
          if (bal.greaterThan(chunkSize)) {
            choices.add(new AddressClass(addr, nsCls));
            if (logger.isTraceEnabled()) {
              logger.trace("Found balance:{}, {},{}={}", scenarioState.getName(), addr, nsCls, bal.getValue());
            }
          }
        })
    );

    if (choices.size() < 2) {
      logger.error("Cannot create transactions, no choices");
      return 0;
    }
    for (int i = 0; i < txToDo; i++) {
      demoExecutor.execute(() -> {

        int choice1 = random.nextInt(choices.size());
        int choice2;
        AddressClass choice1x;
        AddressClass choice2x;
        int maxloop = 100;
        int loop = 0;
        do {
          do {
            choice2 = random.nextInt(choices.size());
            if (loop++ > maxloop) {
              break;
            }
          }
          while (choice1 == choice2);

          choice1x = choices.get(choice1);
          choice2x = choices.get(choice2);
          if (loop++ > maxloop) {
            break;
          }
        }
        while (choice1x.address.equals(choice2x.address));

        AddressPosition ap1;
        AddressPosition ap2;
        AssetTransferTx tx;
        synchronized (currentPosition) {
          ap1 = currentPosition.getPosition(choice1x.address);
          ap2 = currentPosition.getPosition(choice2x.address);
          Number fromBalance = ap1.nsClsBalance.get(choice1x.nsCls);
          Number amount = (new Balance(fromBalance).divideBy(4L)).getValue();
          String[] nameSpaceClass = choice1x.nsCls.split("\\|");
          tx = new AssetTransferTx(chainId, INT_1, null, ap1.getNonce(), true, ap1.entry.getHexPublicKey(),
              choice1x.address, nameSpaceClass[0], nameSpaceClass[1], choice2x.address, amount, null, scenarioState.getName(), METADATA, -1,
              "", 0L
          );
          if (logger.isDebugEnabled()) {
            logger.debug("Done balance:{}/{}->{}/{}", choices.get(choice1).address, choices.get(choice1).nsCls, choices.get(choice2).address,
                choices.get(choice2).nsCls
            );
          }

          ap1.setNonce(ap1.getNonce() + 1);
          ap1.nsClsBalance.replace(choice1x.nsCls, (new Balance(fromBalance)).subtract(amount).getValue());
          ap2.nsClsBalance.put(choice1x.nsCls, (new Balance(ap2.nsClsBalance.getOrDefault(choice1x.nsCls, 0L))).add(amount));
        }

        Sign.signTransaction(tx, ap1.entry.getPrivateKey());

        Object[] tx1 = tx.encodeTx();
        Object[][] txl = new Object[][]{tx1};
        TxPackage msg = new TxPackage(P2PType.TX_PACKAGE_ORIGINAL, tx.getChainId(), txl);

        transactionListenerInternal.transactionReceivedInternal(msg);

      });
    }

    return txToDo;
  }


  private void reloadWalletAndAddresses() throws IOException {
    Set<String> assetOfInterest;

    assetOfInterest = new HashSet<>();
    Merkle<NamespaceEntry> nsl = stateManager.getState().getNamespaces();

    //Compute goodassets - this is simple a list of NS/CLASS where type metadata matches
    nsl.forEach(ns ->
        ns.getClasses().forEach((k, v) -> {
          String metadata = v.getMetadata();
          if (metadata != null && !metadata.isEmpty()) {
            try {
              Map<String, Object> jso = decodeMetadata(metadata);
              String typ = (String) jso.get("Type");
              if (typ != null) {

                for (ScenarioState ss : scenarioStates.getScenarios()) {
                  if (ss.handlesType(typ)) {
                    String assetName = ns.getKey() + NS_SEPARATOR + k;
                    ss.addGoodAsset(assetName);
                    assetOfInterest.add(assetName);
                  }
                }
              }

            } catch (Exception e) {
              logger.error("Exception:", e);
              //Ignore
            }
          }
        }));

    Merkle<AddressEntry> assetBalances = stateManager.getState().getAssetBalances();
    currentPosition.setAssetOfInterest(assetOfInterest);
    currentPosition.load(walletFilePath, assetBalances);
  }


  @Scheduled(fixedRate = 1000)
  private void run() {
    LoggedThread.logged(() -> runImpl()).run();
  }


  private void runImpl() {
    boolean isOverloaded = false;
    if (isOverloaded()) {
      logger.warn("Scenario processor overloaded - skip tx's");
      isOverloaded = true;
    }

    Ticker ticker = new Ticker();
    boolean processed = false;

    for (ScenarioState scenarioState : scenarioStates.getScenarios()) {
      if (!scenarioState.isRunning()) {
        continue;
      }
      //Reload addresses on first run after "all scenario off"
      if (initRequired) {
        try {
          reloadWalletAndAddresses();
          initRequired = false;
        } catch (IOException e) {
          logger.error("Could not start scenario processor");
          return;
        }
      }
      processed = true;
      int txToDo = scenarioState.computeRequiredTransactions(scenarioStates.getEffectiveOverdrive());
      int postedTXCount;
      if (isOverloaded) {
        postedTXCount = txToDo;
      } else {
        postedTXCount = postScenarioTransactions(scenarioState, txToDo);
      }
      scenarioState.addProcessedTransactions(postedTXCount);
    }
    if (processed) {
      if (logger.isInfoEnabled()) {
        logger.info("Ending scenario run in {}", ticker.tick());
      }
    } else {
      initRequired = true;
    }

  }
}
