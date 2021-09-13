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
package io.setl.bc.pychain.node;

import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.eventbus.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.BlockWriter;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.PrivateKeySource;
import io.setl.bc.pychain.StateReader;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.file.FileBlockLoader;
import io.setl.bc.pychain.file.FileBlockWriter;
import io.setl.bc.pychain.file.WalletLoader;
import io.setl.bc.pychain.node.txpool.LargeTransactionPool;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.bc.pychain.tx.verifier.DefaultTxVerifier;
import io.setl.bc.pychain.tx.verifier.TxVerifier;
import io.setl.bc.pychain.validator.Validator;
import io.setl.bc.pychain.validator.ValidatorHandler;
import io.setl.bc.pychain.validator.merkletree.MerkleTreeValidator;
import io.setl.bc.pychain.validator.merkletree.MerkleTreeValidatorHandler;
import io.setl.bc.pychain.wallet.Wallet;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.Sha256Hash;
import io.setl.crypto.MessageVerifier;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.util.LoggedThread;
import io.setl.util.PerformanceDrivenPriorityExecutor;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;
import io.setl.util.TimeBasedUuid;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@ComponentScan(basePackages = {
    "io.setl.bc.pychain",
    "io.setl.rest",
    "io.setl.utils.spring"
})
@EnableScheduling
@PropertySource("classpath:vnode-setting.properties")
class SpringConfiguration {

  @Value("${chainid}")
  private int chainId;

  @Value("${executor.workQueue.max}")
  private int maxJobs;

  @Value("${executor.threadPool.max}")
  private int maxThreads;

  @Value("${executor.workQueue.min}")
  private int minJobs;

  @Value("${executor.threadPool.min}")
  private int minThreads;

  @Value("${nostart:false}")
  private boolean noStart;

  @Value("${proposaldisabled:false}")
  private boolean proposalDisabled;

  @Value("${txexecutorpoolsize:8}")
  private int txExecutorPoolSize;

  @Value("${txexecutorqueuesize:1000}")
  private int txExecutorQueueSize;

  @Value("${wallet}")
  private String walletFile;


  @Autowired
  public SpringConfiguration(Defaults defaults) {
    defaults.validateFolders();
  }


  @Bean
  protected BlockReader blockReader() {
    return new FileBlockLoader();
  }


  @Bean
  protected BlockWriter blockWriter() {
    return new FileBlockWriter();
  }


  /**
   * Configure default thread pool.
   */
  @Bean
  protected AsyncTaskExecutor defaultTaskExecutor(PriorityExecutor priorityExecutor) {
    return new TaskExecutorAdapter(priorityExecutor);
  }


  @Bean
  protected EventBus eventBus() {
    return new EventBus();
  }


  @Bean
  @DependsOn("vaultAccessor")
  FileWatcher fileWatcher() {
    return new FileWatcher(
        Defaults.get().getAbsolutePath(walletFile),
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE
    );
  }


  /**
   * Configure class which verifies p2p signature messages.
   */
  @Bean
  protected DefaultSignatureMessageVerifier getSignatureVerifier() {
    return new DefaultSignatureMessageVerifier();
  }


  @Bean
  @DependsOn("vaultAccessor")
  KeyRepository keyRepository(StateManager stateManager, FileWatcher fileWatcher) throws IOException {
    KeyRepository kr = new KeyRepository(stateManager);
    kr.setSignNodePrivateKeySource(privateKeySource());
    fileWatcher.watch((a, b) -> {
      try {
        kr.setSignNodePrivateKeySource(privateKeySource());
      } catch (IOException e) {
        FileWatcher.logger.error("Unable to read keys from new file ::: ", e);
      }

    });

    return kr;
  }


  @Bean
  public ValidatorHandler merkleTreeValidatorHandler(DBStore dbStore, StateReader stateReader) {
    Validator validator = new MerkleTreeValidator(dbStore, stateReader, messageDigest(), serialiser());
    return new MerkleTreeValidatorHandler(validator);
  }


  private MessageDigest messageDigest() {
    return Sha256Hash.newDigest();
  }


  /**
   * Configure sign/verify providers.
   */
  @Bean
  public MessageVerifier messageVerifier() {
    return MessageVerifierFactory.get();
  }


  /**
   * Configure blockchain static configuration provider.
   */
  @Bean
  public NodeConfiguration nodeConfiguration() {
    return new NodeConfiguration() {
      String uuid = TimeBasedUuid.create().toString();


      @Override
      public int getChainId() {
        return chainId;
      }


      @Override
      public boolean getProposalDisabled() {
        return proposalDisabled;
      }


      @Override
      public String getUniqueNodeIdentifier() {
        return uuid;
      }
    };
  }


  @Bean
  protected AsyncTaskExecutor prePoolTransactionTaskExecutor(PriorityExecutor priorityExecutor) {
    return new TaskExecutorAdapter(r -> priorityExecutor.submit(Priorities.TX_VERIFY, r));
  }


  @Bean
  public PriorityExecutor priorityExecutor() {
    return new PerformanceDrivenPriorityExecutor(minThreads, maxThreads, minJobs, maxJobs);
  }


  /**
   * Configure privateKeySource for signing purposes.
   */
  protected PrivateKeySource privateKeySource() throws IOException {

    if (!noStart) {
      // Use pychain wallet files to provide a privateKeySource for signing purposes.
      Wallet wallet = new WalletLoader().loadWalletFromFile(Defaults.get().getAbsolutePath(walletFile));

      //Future support for multiple wallet files
      Wallet[] wals = new Wallet[]{wallet};

      return publicKey -> {
        for (Wallet w : wals) {
          WalletAddress sn = w.getMatch(publicKey);
          if (sn != null) {
            return sn.getPrivateKey();
          }
        }
        return null;

      };
    }
    //If not starting
    return publicKey -> null;
  }


  private SerialiseToByte serialiser() {
    return HashSerialisation.getInstance();
  }


  @Bean
  @ConditionalOnProperty(value = "sigcheckmultithread", havingValue = "true")
  protected AsyncTaskExecutor sigCheckExecutor(PriorityExecutor priorityExecutor) {
    return new TaskExecutorAdapter(r -> priorityExecutor.submit(Priorities.PROPOSAL, r));
  }


  @Bean
  public TaskScheduler taskScheduler() {
    ScheduledExecutorService tp = LoggedThread.logged(Executors.newScheduledThreadPool(10));
    return new ConcurrentTaskScheduler(tp);
  }


  @Bean
  public TransactionPool transactionPool(
      StateManager stateManager, PriorityExecutor priorityExecutor, SecureRandom secureRandom, MeterRegistry meterRegistry,
      @Value("${txPool.maxLevel:0}") int maxLevel,
      @Value("${txPool.minLevel:2000000000}") int minLevel
  ) {
    LargeTransactionPool pool = new LargeTransactionPool(stateManager, priorityExecutor, secureRandom, meterRegistry);
    pool.setMaxLevel(maxLevel);
    pool.setMinLevel(minLevel);
    return pool;
  }


  @Bean
  protected TransactionProcessor transactionProcessor() {
    return DefaultProcessor.getInstance();
  }


  @Bean
  public TxVerifier txVerifier() {
    return new DefaultTxVerifier();
  }


  @Bean
  @DependsOn("vaultAccessor")
  public WalletLoader walletLoader() {
    return new WalletLoader();
  }


  @Bean
  public TransactionPool transactionPool(StateManager stateManager, PriorityExecutor priorityExecutor, SecureRandom secureRandom, MeterRegistry meterRegistry) {
    return new LargeTransactionPool(stateManager, priorityExecutor, secureRandom, meterRegistry);
  }

}
