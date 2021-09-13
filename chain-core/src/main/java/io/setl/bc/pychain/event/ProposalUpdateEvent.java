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
package io.setl.bc.pychain.event;

import io.setl.bc.pychain.p2p.message.ProposalMessage;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import java.util.UUID;

/**
 * Nofification that an inplay proposal has been received or updated.
 */
public class ProposalUpdateEvent {

  public enum UpdateType {
    NEW_PROPOSAL,
    VOTE,
    SIGNATURE,
    COMMITTED
  }

  private final double signaturePercentage;

  private final SignatureMessage[] signatures;

  private final UpdateType updateType;

  private final UUID uuid;

  private final double votePercentage;


  /**
   * ProposalUpdateEvent constructor.
   *
   * @param votePercentage      :
   * @param signaturePercentage :
   * @param signatures          :
   * @param updateType          :
   * @param uuid                :
   */
  public ProposalUpdateEvent(double votePercentage, double signaturePercentage,
      SignatureMessage[] signatures, UpdateType updateType, UUID uuid
  ) {
    this.votePercentage = votePercentage;
    this.signaturePercentage = signaturePercentage;
    this.signatures = signatures != null ? signatures.clone() : null;
    this.updateType = updateType;
    this.uuid = uuid;
  }


  public double getSignaturePercentage() {
    return signaturePercentage;
  }


  public SignatureMessage[] getSignatures() {
    return signatures != null ? signatures.clone() : null;
  }


  public UpdateType getUpdateType() {
    return updateType;
  }


  public UUID getUuid() {
    return uuid;
  }


  public double getVotePercentage() {
    return votePercentage;
  }

}
