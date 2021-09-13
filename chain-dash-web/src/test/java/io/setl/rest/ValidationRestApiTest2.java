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

import static org.junit.Assert.assertTrue;

import io.setl.common.CommonPy.TxType;
import io.setl.rest.ValidationRestApi.AccountResponse;
import io.setl.rest.ValidationRestApi.AddXChain;
import io.setl.rest.ValidationRestApi.Address;
import io.setl.rest.ValidationRestApi.AssetBalanceRequest;
import io.setl.rest.ValidationRestApi.CommitContract;
import io.setl.rest.ValidationRestApi.Contract;
import io.setl.rest.ValidationRestApi.ContractResponse;
import io.setl.rest.ValidationRestApi.IssueAsset;
import io.setl.rest.ValidationRestApi.IssueAssetResponse;
import io.setl.rest.ValidationRestApi.KeyPair;
import io.setl.rest.ValidationRestApi.Namespace;
import io.setl.rest.ValidationRestApi.NamespaceRegisterResponse;
import io.setl.rest.ValidationRestApi.NonceResponse;
import io.setl.rest.ValidationRestApi.RegisterAddress;
import io.setl.rest.ValidationRestApi.RegisterAsset;
import io.setl.rest.ValidationRestApi.RegisterAssetResponse;
import io.setl.rest.ValidationRestApi.StockSplit;
import io.setl.rest.ValidationRestApi.StockSplitResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ValidationRestApiTest2 {
  
  @Before
  public void setUp() throws Exception {
  
  }
  
  @After
  public void tearDown() throws Exception {
  
  }
  
  @Test
  public void address() {
    
    // No code, just checking that the properties exist.
    
    Address add = new Address();
    
    add.address = "ThisAddress";
    
    assertTrue(add.address.equals("ThisAddress"));
    
    
  }
  
  @Test
  public void commitContract() {
    
    // No code, just checking that the properties exist.
    
    CommitContract obj = new CommitContract();
    
    obj.cancel = new ArrayList<>();
    obj.commitment = new ArrayList<>();
    obj.receive = new ArrayList<>();
    obj.contractaddress = "ThisAddress";
    obj.contractfunction = "ThisFunction";
    obj.issuingaddress = "IssAddress";
    obj.protocol = "ThisProtocol";
    obj.party = Integer.MAX_VALUE;
    
    assertTrue(obj.contractaddress.equals("ThisAddress"));
    
  }
  
  @Test
  public void contract() {
    
    // No code, just checking that the properties exist.
    
    Contract obj = new Contract();
    
    obj.parties = new ArrayList<>();
    
    obj.contractfunction = "ThisFunction";
    obj.issuingaddress = "IssAddress";
    obj.protocol = "ThisProtocol";
    obj.metadata = "ThisMetadata";
    
    assertTrue(obj.metadata.equals("ThisMetadata"));
    
  }
  
  @Test
  public void assetBalanceRequest() {
    
    // No code, just checking that the properties exist.
    
    AssetBalanceRequest xc = new AssetBalanceRequest();
    
    xc.byaddress = true;
    xc.address = "FromAddress";
    xc.namespace = "thisNamespace";
    xc.classid = "thisClassID";
    
    assertTrue(xc.namespace.equals("thisNamespace"));
    assertTrue(xc.classid.equals("thisClassID"));
  }
  
  @Test
  public void addXChain() {
    
    // No code, just checking that the properties exist.
    
    AddXChain xc = new AddXChain();
    
    xc.chainheight = Integer.MAX_VALUE;
    xc.chainid = Integer.MAX_VALUE;
    xc.chainparameters = Integer.MAX_VALUE;
    xc.chainsignodes = new ArrayList<>();
    xc.sigNodes = new ArrayList<>();
    xc.fromAddr = "FromAddress";
    
    assertTrue(xc.chainheight == Integer.MAX_VALUE);
  }
  
  @Test
  public void accountResponse() {
    
    String publicKey = "aPublicKey";
    Number balance = Long.MAX_VALUE;
    String address = "aRandomAddress";
    
    AccountResponse ar = new AccountResponse(publicKey, balance, address);
    
    assertTrue(ar.publickey.equals(publicKey));
    assertTrue(ar.balance.equals(balance));
    assertTrue(ar.address.equals(address));
    
  }
  
  @Test
  public void contractResponse() {
    
    Number amount = 42L;
    int basechain = 99;
    int blockheight = Integer.MAX_VALUE;
    Map<String, Object> contractdata = new HashMap<>();
    long creation = Long.MAX_VALUE;
    String fromaddr = "fromAddress";
    String hash = "thisHash";
    String poa = "ThisPOA";
    String toaddr = "ToAddress";
    int tochain = basechain + 1;
    int txtype = TxType.ISSUE_ASSET.getId();
    String typename = TxType.ISSUE_ASSET.toString();
    boolean update = false;
    
    
    ContractResponse ar = new ContractResponse(
        amount,
        basechain,
        blockheight,
        contractdata,
        creation,
        fromaddr,
        hash,
        poa,
        toaddr,
        tochain,
        txtype,
        typename,
        update
    );
    
    assertTrue(ar.fromaddr.equals(fromaddr));
    
  }
  
  @Test
  public void issueAsset() {
  
    IssueAsset obj = new IssueAsset();
    
  }
  
  @Test
  public void issueAssetResponse() {
  
    Number amount = 42L;
    int basechain = 99;
    int nonce = Integer.MAX_VALUE;
    Map<String, Object> contractdata = new HashMap<>();
    long creation = Long.MAX_VALUE;
    String fromaddr = "fromAddress";
    String hash = "thisHash";
    String poa = "ThisPOA";
    String toaddr = "ToAddress";
    String typename = TxType.ISSUE_ASSET.toString();
    String namespace = "thisNamespace";
    String classID = "thisClassID";
    String protocol = "ThisProtocol";
    String signature = "ThisSignature";
  
    IssueAssetResponse obj = new IssueAssetResponse(
        classID,
        nonce,
        creation,
        hash,
        fromaddr,
        basechain,
        namespace,
        typename,
        amount,
        toaddr,
        signature,
        protocol,
        poa
    );
  
    assertTrue(obj.fromaddr.equals(fromaddr));
  
  }
  
  @Test
  public void keyPair() {
  
    KeyPair obj = new KeyPair();
    
  }
  
  @Test
  public void namespace() {
  
    Namespace obj = new Namespace();
    
  }
  
  @Test
  public void namespaceRegisterResponse() {
    
    int basechain = 99;
    int nonce = Integer.MAX_VALUE;
    Map<String, Object> contractdata = new HashMap<>();
    long creation = Long.MAX_VALUE;
    String fromaddr = "fromAddress";
    String hash = "thisHash";
    String poa = "ThisPOA";
    String typename = TxType.ISSUE_ASSET.toString();
    String namespace = "thisNamespace";
    String signature = "ThisSignature";
  
    NamespaceRegisterResponse obj = new NamespaceRegisterResponse(
        nonce,
        creation,
        hash,
        fromaddr,
        basechain,
        namespace,
        signature,
        typename,
        poa
    );
    
    assertTrue(obj.fromaddr.equals(fromaddr));
    
  }
  
  @Test
  public void nonceResponse() {
  
    int nonce = Integer.MAX_VALUE;
    String fromaddr = "fromAddress";
  
    NonceResponse obj = new NonceResponse(nonce, fromaddr);
  
    assertTrue(obj.address.equals(fromaddr));
  
  }
  
  @Test
  public void registerAddress() {
  
    RegisterAddress obj = new RegisterAddress();
    
  }

  @Test
  public void registerAsset() {
  
    RegisterAsset obj = new RegisterAsset();
    
  }
  
  @Test
  public void registerAssetResponse() {
    
    int basechain = 99;
    int nonce = Integer.MAX_VALUE;
    long creation = Long.MAX_VALUE;
    String fromaddr = "fromAddress";
    String hash = "thisHash";
    String poa = "ThisPOA";
    String typename = TxType.ISSUE_ASSET.toString();
    String namespace = "thisNamespace";
    String classID = "thisClassID";
    String signature = "ThisSignature";
  
    RegisterAssetResponse obj = new RegisterAssetResponse(
        classID,
        nonce,
        creation,
        hash,
        fromaddr,
        basechain,
        namespace,
        signature,
        typename,
        poa
    );
    
    assertTrue(obj.fromaddr.equals(fromaddr));
    
  }
  
  @Test
  public void stocksplit() {
  
    StockSplit obj = new StockSplit();
    
  }
  
  @Test
  public void stockSplitResponse() {
    
    double ratio = 42.42D;
    int basechain = 99;
    int nonce = Integer.MAX_VALUE;
    long creation = Long.MAX_VALUE;
    String fromaddr = "fromAddress";
    String hash = "thisHash";
    String poa = "ThisPOA";
    String typename = TxType.ISSUE_ASSET.toString();
    String namespace = "thisNamespace";
    String classID = "thisClassID";
    String reference = "thisReference";
    String signature = "ThisSignature";
  
    StockSplitResponse obj = new StockSplitResponse(
        classID,
        nonce,
        creation,
        hash,
        reference,
        fromaddr,
        basechain,
        namespace,
        typename,
        signature,
        ratio,
        poa
    );
    
    assertTrue(obj.fromaddr.equals(fromaddr));
    
  }
  
  
}