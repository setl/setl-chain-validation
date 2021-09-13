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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.BlockVerifier;
import io.setl.bc.pychain.p2p.message.ProposalMessage;
import io.setl.crypto.MessageVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validate received proposal Created by aanten on 29/06/2017.
 */
@Component
public class ProposalValidator {

  private static final Logger logger = LoggerFactory.getLogger(ProposalValidator.class);

  //  @Autowired
  private MessageVerifier messageVerifier;

  //  @Autowired
  private StateManager stateManager;


  @Autowired
  public ProposalValidator(StateManager stateManager, MessageVerifier messageVerifier) {
    this.stateManager = stateManager;
    this.messageVerifier = messageVerifier;
  }


  /**
   * Validate the supplied proposal.
   *
   * @param proposalMessage  The proposal
   * @param expectedProposer The expected proposer
   *
   * @return the blockhash if validated otherwise null.
   */
  public Hash validateProposal(ProposalMessage proposalMessage, String expectedProposer, boolean failIfNotExpectedProposer) {

    StateDetail st0 = stateManager.getCurrentStateDetail();

    // Check State height vs Proposal

    if (st0.getHeight() != proposalMessage.getStateHeight()) {
      logger.error("Proposal:Current state height mismatch");
      return null;
    }

    // Check StateHash vs Proposal

    Block block = proposalMessage.getBlock();
    if (!st0.getStateHash().equals(block.getBaseStateHash())) {
      if (logger.isErrorEnabled()) {
        logger.error("Proposal:Current state hash mismatch current {}!=proposal {}", st0.getStateHash(), block.getBaseStateHash());
      }
      return null;
    }

    // Check Proposing Address
    if (!proposalMessage.getPublicKey().equals(expectedProposer)) {
      if (failIfNotExpectedProposer) {
        if (logger.isErrorEnabled()) {
          logger.error("Proposal: expected proposer {} got {}",
              expectedProposer, proposalMessage.getPublicKey());
        }
        return null;
      }

      if (logger.isWarnEnabled()) {
        logger.warn("Proposal: expected proposer {} got {}",
            expectedProposer, proposalMessage.getPublicKey());
      }
    }

    // Calculate Block (proposal) hash

    final Hash blockHash;

    try {
      blockHash = new BlockVerifier().computeHash(block);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Verify Proposer signature.
    boolean verified = proposalMessage.verifySignature();
    if (!verified) {
      logger.error("Proposal:Invalid signature");
      return null;
    }

    return blockHash;
  }
}
