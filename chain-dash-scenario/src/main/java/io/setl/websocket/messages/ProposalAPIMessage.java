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
package io.setl.websocket.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.setl.bc.pychain.p2p.message.SignatureMessage.SignatureDetail;
import io.setl.bc.pychain.p2p.message.SignatureMessage.XCSignatureDetail;
import io.setl.websocket.messages.serializers.SignatureSerializer;
import io.setl.websocket.messages.serializers.TimestampSerializer;
import io.setl.websocket.messages.types.Location;
import java.util.List;

public class ProposalAPIMessage implements SubscriptionMessage {
  private double votePercentage;
  private String baseHash;
  private List<XCSignatureDetail> signaturesXC;
  private double timestamp;
  private String hostname;
  private int height;
  private List<SignatureDetail> signatures;
  private String proposedBlockHash;
  private Location location;
  private int txCount;
  private String error;
  private double signPercentage;


  /**
   * New instance.
   *
   * @param votePercentage    message parameter
   * @param baseHash          message parameter
   * @param signaturesXC      message parameter
   * @param timestamp         message parameter
   * @param hostname          message parameter
   * @param height            message parameter
   * @param signatures        message parameter
   * @param proposedBlockHash message parameter
   * @param location          message parameter
   * @param txCount           message parameter
   * @param error             message parameter
   * @param signPercentage    message parameter
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public ProposalAPIMessage(double votePercentage, String baseHash, List<XCSignatureDetail> signaturesXC, double timestamp, String hostname,
      int height, List<SignatureDetail> signatures, String proposedBlockHash, Location location, int txCount, String error, double signPercentage
  ) {
    this.votePercentage = votePercentage;
    this.baseHash = baseHash;
    this.signaturesXC = signaturesXC;
    this.timestamp = timestamp;
    this.hostname = hostname;
    this.height = height;
    this.signatures = signatures;
    this.proposedBlockHash = proposedBlockHash;
    this.location = location;
    this.txCount = txCount;
    this.error = error;
    this.signPercentage = signPercentage;
  }

  @JsonProperty("VotePercentage")
  public double getVotePercentage() {
    return votePercentage;
  }

  public void setVotePercentage(double votePercentage) {
    this.votePercentage = votePercentage;
  }

  @JsonProperty("BaseHash")
  public String getBaseHash() {
    return baseHash;
  }

  public void setBaseHash(String baseHash) {
    this.baseHash = baseHash;
  }

  @JsonSerialize(using = SignatureSerializer.class)
  @JsonProperty("SignaturesXC")
  public List<XCSignatureDetail> getSignaturesXC() {
    return signaturesXC;
  }

  public void setSignaturesXC(List<XCSignatureDetail> signaturesXC) {
    this.signaturesXC = signaturesXC;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  @JsonProperty("Timestamp")
  public double getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(double timestamp) {
    this.timestamp = timestamp;
  }

  @JsonProperty("Hostname")
  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @JsonProperty("Height")
  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  @JsonSerialize(using = SignatureSerializer.class)
  @JsonProperty("Signatures")
  public List<SignatureDetail> getSignatures() {
    return signatures;
  }

  public void setSignatures(List<SignatureDetail> signatures) {
    this.signatures = signatures;
  }

  @JsonProperty("ProposedBlockHash")
  public String getProposedBlockHash() {
    return proposedBlockHash;
  }

  public void setProposedBlockHash(String proposedBlockHash) {
    this.proposedBlockHash = proposedBlockHash;
  }

  @JsonProperty("Location")
  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  @JsonProperty("TXCount")
  public int getTxCount() {
    return txCount;
  }

  public void setTxCount(int txCount) {
    this.txCount = txCount;
  }

  @JsonProperty("Error")
  public String getError() {
    return error;
  }


  public void setError(String error) {
    this.error = error;
  }

  @JsonProperty("SignPercentage")
  public double getSignPercentage() {
    return signPercentage;
  }

  public void setSignPercentage(double signPercentage) {
    this.signPercentage = signPercentage;
  }

}
