/**********************************************************************************************
 *
 * Fetch data and update browser.
 *
 **********************************************************************************************/

/**
 * Load and render initial snapshot.
 */
function loadAndRenderSnapshot(ID, initialSnapshot, UserData){
    restartBlockTimer();

    // Store the useful data we need for later.
    var lastBlock = initialSnapshot['Data']['LastBlock'];
    lastBlock.Hostname = initialSnapshot['Hostname'];
    cache.recentBlocks.push(lastBlock);// Store the block.

    //initialSnapshot['Data']['LogAreas'] initialSnapshot['Data']['LogLevel'] initialSnapshot['Data']['Subscriptions']['status']

    // Render initial snapshot to browser.

    // Header dash
    updateBlockHeight(initialSnapshot['Data']['LastBlock']['Height']);
    updateMovementsLastBlock(initialSnapshot['Data']['LastBlock']['TXCount']);
    //updateDairyMovements(snapShot['txAndBlockSummary']['txPS']);
    updateMovements24h(initialSnapshot['Data']['TX24Hours']);
    updateMovements24hLineChart(initialSnapshot['Data']['TX24Hours']);
    updateLastBlockTimeSeconds(initialSnapshot['Data']['LastBlockTime']);
    updateLastBlockTimeLineChart(initialSnapshot['Data']['LastBlockTime']);

    //// Proposal pie chart tiny
    //updatePieChartCurrentProposalPercentage(snapShot['currentProposal']['signCount'], snapShot['currentProposal']['voteCount']);


    //latest three blocks
    updateLatestThreeBlocks(cache.recentBlocks);

    //Block view page -> block list
    updateBlockViewBlockList();

    // Setting page -> Known node.
    settingPageKnownHostDetailUpdate(initialSnapshot['Data']['Peers']);
    cache.scenarioState = initialSnapshot['Data']['Scenarios'];
    SettingPageUpdateScenarioState();
    settingPageUpdateChainID(initialSnapshot['Data']['ChainID']);

    // Log page
    logPageLogAreaUpdate(initialSnapshot['Data']['LogAreas']);
    logPageLogLevelUpdate(initialSnapshot['Data']['LogLevel']);
    //logPageDiagnosisStateUpdate(initialSnapshot['Data']['Subscriptions']['status']);
    logPageOverdriveStateUpdate(initialSnapshot['Data']['Overdrive']);
    logPageOverdriveMultiplyUpdate(initialSnapshot['Data']['Overdrive']);
}



function getInitialSnapshotFromThroughSocket(){
    //reset the necessary data
    cache.lastBlockMovement_last = 0;
    cache.recentBlocks = [];

    // Create get initial snapshot message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, loadAndRenderSnapshot, {});

    Request.MessageType = 'Request';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.RequestName = 'state';

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

/**
 * Send command to server socket.
 * @param scenario
 * @param state
 */
function sendCommandToChangeScenario(scenario, state){

    // Create message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, paymentSystemSettingResponseHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Scenario = scenario; //1-5
    Request.MessageBody.State = state; //1 or 0
    Request.MessageBody.Command = 'scenario';

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

function paymentSystemSettingResponseHandler(ID, response, UserData){
    if(response['Request'] !== undefined){
        // Display notification message.
        //var state = response['Request'][3]['State'] == 1 ? 'On' : 'Off';
        //var scenario = cache.paymentSystemsLabel[response['Request'][3]['Scenario']-1];
        //
        //BootstrapDialog.alert({
        //    title: 'Notification',
        //    message: 'Scenario state ' + scenario + ' successfully turn ' + state
        //});
    }
}



function logAreaResponseHandler(ID, response, UserData){
}

/**
 * Handle the response from websocket.
 * For tx diagnosis command.
 * @param ID
 * @param response
 * @param UserData
 */
function diagnoseTXResponseHandler(ID, response, UserData){
    var txs = response['Data'];
    $('#txdiagnosis-tab-pane .txcount').html(txs.length);

    // Render the Header
    //$('#txdiagnosis-tab-pane table thead tr').empty();
    //for(var i = 1; i <= txs['length']; i++){
    //    $('#txdiagnosis-tab-pane table thead tr').append('<th>' + i + '</td>');
    //}

    $('#txdiagnosis-tab-pane table tbody').empty();
    var nToRender = $('#isloadAllTXs').is(':checked') ? txs.length : 20;

    for(var i = 0; i < nToRender && i < txs.length; i++)
    {
        $('#txdiagnosis-tab-pane table tbody').append(
            '<tr>'+'</tr>'
        );
        for(var j = 0; j < txs[i]['length']; j++){
            var val = typeof txs[i][j] === 'string' && txs[i][j].length > 10 ? txs[i][j].substr(0, 10) : txs[i][j];
            $('#txdiagnosis-tab-pane table tbody tr:last-child').append(
                '<td>' + val + '</td>'
            );
        }
    }

    if(response['Request'][3]['Action'] == 0)
        // Load dumped tx list through websock.
        sendCommandToDumpedTxList(2);
}

/**
 * Handle the response from websocket.
 * For sv dump command.
 * @param ID
 * @param response
 * @param UserData
 */
function diagnoseSvResponseHandler(ID, response, UserData){
    sendCommandToDumpedSvList(2);
}

/**
 * Handle response from sendCommandToDumpedTxList
 * @param ID
 * @param response
 * @param UserData
 */
function getDumpedTxListHandler(ID, response, UserData){
    //render the lsit:
    $('#dumpedTxListWrapper').empty();
    var list = response['Data'].reverse();
    $.each(list, function(i, item){
        $('#dumpedTxListWrapper').append(
            '<div class="media" data-id="'+item+'">'+
            '<div class="media-body">'+
                item+
                '<div class="list-options">'+
                    '<button class="btn btn-sm" data-action="load">Load</button>'+
                    '<button class="btn btn-sm" data-action="delete">Delete</button>'+
                '</div>'+
            '</div>'+
            '</div>'
        );
    });
}

/**
 * Handle response from sendCommandToDumpedSvList
 * @param ID
 * @param response
 * @param UserData
 */
function getDumpedSvListHandler(ID, response, UserData){
    //render the lsit:
    $('#dumpedSvListWrapper').empty();
    var list = response['Data'].reverse();
    $.each(list, function(i, item){
        $('#dumpedSvListWrapper').append(
            '<div class="media" data-id="'+item+'">'+
            '<div class="media-body">'+
            item+
            '<div class="list-options">'+
            '<button class="btn btn-sm" data-action="load">Load</button>'+
            '<button class="btn btn-sm" data-action="delete">Delete</button>'+
            '</div>'+
            '</div>'+
            '</div>'
        );
    });
    if(response['sv'] !== undefined){
        var i = 1;
        $('#dumpedSvDataView').empty();
        var html = '';
        for (var key in response['sv']) {
            if (i % 4 == 1 )
                html += '<div class="row">';
            html += '<div class="col-md-3"><a href="#" class= "svDataEntryWrapper" data-name="'+key+'">'+key+'</a></div>';
            if (i % 4 == 0 || i == Object.size(response['sv']))
                html += '</div>';
            i ++;
        }
        $('#dumpedSvDataView').html(html);
        cache.diagnosisSV = response['sv'];
    }
}

/**
 * Change the log area setting.
 */
function sendCommandToChangeLogArea(logArea, state){
    // Create message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, logAreaResponseHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'log'; //1-5
    Request.MessageBody.State = state; //1 or 0
    Request.MessageBody.LogArea = logArea;

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

/**
 * Change the log area setting.
 */
function sendCommandToChangeLogLevel(logLevel){
    // Create message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, logAreaResponseHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'log'; //1-5
    Request.MessageBody.LogLevel = logLevel;

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

function sendCommandToToggleStatusUpdate(state){
    // Create message.
    var Request = {};

    var messageID = document.SetlSocketCallback.getUniqueID();

    Request.MessageType = state == 1 ? 'Subscribe' : 'UnSubscribe';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.RequestName = ''; //1-5
    Request.MessageBody.Topic = ['status'];

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

function sendCommandToChangeOverDriveSetting(state, multiply){
    // Create message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, logAreaResponseHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'overdrive'; //1-5
    Request.MessageBody.State = state;
    Request.MessageBody.Overdrive = multiply;


    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

/**
 * Send command to diagnose tx through websocket.
 * @param state
 */
function sendCommandToDiagnosisTX(action,filename,isLoadAll, isPopulate){
    // Create message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, diagnoseTXResponseHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'txdiagnosis';
    Request.MessageBody.Action = action;  // 0 is dump, 1 is load
    Request.MessageBody.Filename = filename;
    Request.MessageBody.IsLoadAll = isLoadAll;
    Request.MessageBody.IsPopulate = isPopulate;

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}



/**
 * Get dumped tx list through websocket.
 */
function sendCommandToDumpedTxList(action,filename){
    // Create message.
    var Request = {};

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, getDumpedTxListHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'txdiagnosis';
    Request.MessageBody.Filename = filename;
    Request.MessageBody.Action = action;  // 0 is dump, 1 is load

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

/**
 * Get dumped sv list through websocket.
 */
function sendCommandToDumpedSvList(action, filename, dataNameArr){
    // Create message.
    var Request = {};

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, getDumpedSvListHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'dumpsvdata';
    Request.MessageBody.dataNameArr = dataNameArr;
    Request.MessageBody.Filename = filename;
    Request.MessageBody.Action = action;  // 0 is dump, 1 is load

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

/**
 * Send command to dump sv through websocket.
 * @param state
 */
function sendCommandToDumpSv(action, dataNameArr){
    // Create message.
    var Request = {};
    var MessageText;

    var messageID = document.SetlSocketCallback.getUniqueID();
    document.SetlSocketCallback.addHandler(messageID, diagnoseSvResponseHandler, {});

    Request.MessageType = 'Command';
    Request.MessageHeader = '';
    Request.MessageBody = {};
    Request.RequestID = messageID;

    Request.MessageBody.Command = 'dumpsvdata';
    Request.MessageBody.Action = action;
    Request.MessageBody.dataNameArr = dataNameArr;  // 0 is dump, 1 is load

    //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

    document.SetlSocket.sendRequest(Request);
}

/**
 * Callback function to handle incoming update message.
 * @param ID
 * @param message
 * @param UserData
 * @constructor
 */
function Setl_ProcessUpdateMessage(ID, message, UserData)
{
    var MessageType;
    var Data;

    try
    {
        MessageType = (('MessageType' in message) ? message['MessageType'] : '');
        Data = (('Data' in message) ? message['Data'] : ((MessageType in message) ? message[MessageType] : []));

        switch (MessageType.toLowerCase())
        {
            case 'block':
                try {
                    // Store the useful data we need for later.

                    if (Data['Height'] != cache.recentBlocks[0]['Height'] || cache.recentBlocks.length == 0) {
                        cache.recentBlocks.unshift(Data);// Store the block.

                        updateBlockHeight(Data['Height']);
                        updateMovementsLastBlock(Data['TXCount']);

                        // Calculate movement PS.
                        var averageBlockTime = cache.recentBlocks[0]['Timestamp'] - cache.recentBlocks[1]['Timestamp'];
                        var movementsPS = cache.recentBlocks[0]['TXCount'] / averageBlockTime;
                        var movementsPD = Math.floor(movementsPS * 86400);
                        updateDairyMovements(movementsPD);
                        updateMovementsDairyLineChart(movementsPD);


                        updateMovements24h(Data['TX24Hours']);
                        updateMovements24hLineChart(Data['TX24Hours']);
                        updateLastBlockTimeSeconds(Data['LastBlockTime']);
                        updateLastBlockTimeLineChart(Data['LastBlockTime']);

                        updateLatestThreeBlocks(cache.recentBlocks);

                        updateRecentBlockMovementLineChart(cache.recentBlocks[0]['TXCount']);

                        if (cache.chainConfigInfo[cache.chainID]['showXChain'] == 1) {
                            // TXIn TXOut
                            Data['TXIn'] = Data['TXIn'] == undefined ? 0 : Data['TXIn']; // testing purpose, to be delete.
                            Data['TXOut'] = Data['TXOut'] == undefined ? 0 : Data['TXOut']; // testing purpose, to be delete.

                            updateXchainMovementIn(Data['TXIn']);
                            updateXchainMovementInLineChart(Data['TXIn']);
                            updateXchainMovementOut(Data['TXOut']);
                            updateXchainMovementOutLineChart(Data['TXOut']);
                        }

                        if (cache.recentBlocks.length != 0) {
                            updateTimeTakenLastBlockLineChart(cache.newBlockTimeTaken);
                            // Update bar chart.
                            updateScenarioBarchart(Data['ProtocolCount']);
                        }

                        restartBlockTimer();

                        //Block view page -> block list
                        updateBlockViewBlockList();

                        // Contract Page
                        // Check if the contract has been successfully completed.

                        $.each(cache.contractData, function(type, contract){
                            if (!$.isEmptyObject(contract)){
                                if (!contract['completed']){
                                    requestContractDetail(contract['toaddr']);
                                }
                            }
                        });

                        if(Data['ContractEvents'] !== undefined && Data['ContractEvents'].length > 0) {
                            $.each(Data['ContractEvents'], function(index, event){
                                var contractType = cache.historicContracts[2][event[0]];
                                if (contractType === undefined) return;
                                if (!$.isEmptyObject(cache.contractData[contractType])) {
                                    if (Data['ContractEvents'][0][0] === cache.contractData[contractType]['toaddr']) {
                                        // Allow us to render the raw data again.
                                        cache.rawContractRendered[contractType] = false;

                                        renderUpdateCommitment(contractType);
                                    }
                                }
                            });
                        }

                        if(Data['TimeEvents'] !== undefined && Data['TimeEvents'].length > 0) {
                            $.each(Data['TimeEvents'], function(index, addr){
                                var contractType = cache.historicContracts[2][addr];
                                if (contractType === undefined) return true;
                                if (!$.isEmptyObject(cache.contractData[contractType]) && !cache.contractData[contractType]['completed'] && addr == cache.contractData[contractType]['toaddr']) {
                                    checkIfExpired(addr);
                                }
                            });
                        }
                    }

                    if (cache.recentBlocks.length > 20) cache.recentBlocks.pop(); //remove the old block.
                }catch(e){
                    if (!cache.prop) console.log(e);
                }
                break;


            case 'proposal':
            case 'proposal_update':
                try {
                    if (cache.fiveRecentProposal.length > 0) {
                        if (Data['Height'] != cache.fiveRecentProposal[0]['Height']) {
                            cache.fiveRecentProposal.unshift(Data); // store the proposal.
                            updatePieChartCurrentProposalPercentage(1, 1);
                            updateProposalPieChart(Math.max(Data['VotePercentage'], Data['SignPercentage']));
                        } else {
                            cache.fiveRecentProposal[0] = Data;
                            updatePieChartCurrentProposalPercentage(Data['SignPercentage'], Data['VotePercentage']);
                        }

                        //update block vote and sign in recent block table.
                        if (Data['Height'] == cache.recentBlocks[0]['Height']) {
                            try {
                                $('#dash-last-three-block-wrapper [data-block-id =' + 0 + '] td:nth-child(5)').html('% ' + Math.round(Data['SignPercentage'] * 10000) / 100);
                                $('#dash-last-three-block-wrapper [data-block-id =' + 0 + '] td:nth-child(6)').html('% ' + Math.round(Data['VotePercentage'] * 10000) / 100);

                                cache.recentBlocks[0]['SignPercentage'] = Data['SignPercentage'];
                                cache.recentBlocks[0]['VotePercentage'] = Data['VotePercentage'];
                            }
                            catch (e) {
                                if (!cache.prop) console.log(e);
                            }
                        }
                    } else {
                        cache.fiveRecentProposal.push(Data);
                        updateProposalPieChart(Math.max(Data['VotePercentage'], Data['SignPercentage']));
                    }

                    if (cache.fiveRecentProposal.length > 5) cache.fiveRecentProposal.pop(); //remove the old proposal.

                    //Flash map marker indicate proposal update or new proposal.
                    try {
                        flashAMakerInMap(cache.nodeOrder[Data['Hostname'].toLowerCase()]);
                    }
                    catch (e) {
                    }

                    // current proposal
                    updateCurrentProposalTime(Data['Timestamp']);
                    updateCurrentProposalMovements(Data['TXCount']);
                    updateCurrentProposalNode(Data['Hostname']);
                    updateCurrentProposalSigned(Data['SignPercentage']);
                    updateCurrentProposalVoted(Data['VotePercentage']);
                    updateCurrentProposalBlock(Data['Height']);
                } catch(e){
                    if (!cache.prop) console.log(e);
                }
                break;

            case 'status':

                renderDataAccordionView(Data); // Handle treeview selection.
                break;

            case 'serverstatus':
                //scenario state
                cache.scenarioState = Data['Scenarios'];
                SettingPageUpdateScenarioState();

                // Log page
                logPageLogAreaUpdate(Data['LogAreas']);
                logPageLogLevelUpdate(Data['LogLevel']);
                logPageDiagnosisStateUpdate(Data['Subscriptions']['status']);
                logPageOverdriveStateUpdate(Data['Overdrive']);
                logPageOverdriveMultiplyUpdate(Data['Overdrive']);

                break;

            case 'transaction':
                //latest transactions
                cache.latestTransactions.unshift({Transaction: Data['Transactions'], Timestamp : Data['Timestamp']});

                // We only store 100 recent update.
                if(cache.latestTransactions.length > 100)
                    cache.latestTransactions.pop();

                // Dash page.
                if($('#dash-wrapper').hasClass('active'))
                    updateLatestTransactionsTable();

                // Transaction page.
                if(cache.receiveTransactionUpdateState)
                    transactionPageUpdateTransactionsTable();


                break;

            case 'balanceview':
                try {
                    //get the Assetlist and new assetBalance Array (index with asset) for the current state view.
                    var assetlistedBalance = getCurrentStateViewAssetList(Data['Assetbalances']);

                        if (cache.recentStateViews.length==0 || Data['Height'] != cache.recentStateViews[0]['Height']) {
                            Data['assetList'] = assetlistedBalance;
                            Data['Timestamp'] = Math.floor((new Date).getTime() / 1000);
                            cache.recentStateViews.unshift(Data);// Store the block.

                            //state view page -> state view list.
                            updateStateViewBalanceList();

                            //Wallet page -> update dropdowns.
                            if (!cache.gotInitialBV) {
                                cache.gotInitialBV = 1;

                                try {
                                    getStateViewForWallet(assetlistedBalance);
                                }
                                catch (e) {
                                    if (!cache.prod == false) console.log(e.message);
                                }

                                updateDropDown('#receivingTab');
                                updateDropDown('#corpactionTab');
                                //updateDropDown('#sendingTab');
                                //updateDropDown();


                                $('#diary-tx-wrapper').removeClass('hidden');//Show diary tx div.

                                generateDefaultTx();
                                generateDefaultCorpOptions();

                                // Smart contract page -> dvp -> address balances.
                                contractUpadteAddrDropDown('#dvp-smartContractTab');

                                dvpContractDefaultAddress('#dvp-parta-bl-wrapper');
                                dvpContractDefaultAddress('#dvp-partb-bl-wrapper');

                                // Smart contract page -> dvp
                                // Pick a random address for party 3.
                                dvpPickParty3Address();

                                // Smart contract page -> sac -> address balances.
                                contractUpadteAddrDropDown('#sac-address-select-wrapper');

                                // Smart contract page -> sac
                                // Pick some default address
                                acContractDefaultAddress('#sac-address-select-wrapper', '.partyA', 10);
                                acContractDefaultAddress('#sac-address-select-wrapper', '.partyB', 12);
                                acContractDefaultAddress('#sac-address-select-wrapper', '.partyC', 13);
                                acContractDefaultAddress('#sac-address-select-wrapper', '.partyD', 15);

                                // Smart contract -> sac
                                // render the instrument select list.
                                acContractRenderInstrumentList('#single-clearingTab','#sac-new-contract-wrapper', '', '.instrument-select', 2);

                                // Smart contract page -> sac -> balance update.
                                acContractNewContractBalanceUpdate('#sac-new-contract-wrapper');

                                // Smart contract page -> mac -> address balances.
                                contractUpadteAddrDropDown('#mac-address-select-wrapper');

                                // Smart contract page -> mac
                                // Pick some default address
                                acContractDefaultAddress('#mac-address-select-wrapper', '.partyA', 10);
                                acContractDefaultAddress('#mac-address-select-wrapper', '.partyB', 12);
                                acContractDefaultAddress('#mac-address-select-wrapper', '.partyC', 13);
                                acContractDefaultAddress('#mac-address-select-wrapper', '.partyD', 15);

                                // Smart contract -> mac
                                // render the instrument select list.
                                cache.waitingForAllInstrumentsReady = true;
                                acContractRenderInstrumentList('#multi-clearingTab','#mac-new-contract-wrapper', ".instrument-wrapper[data-instru-id='1']", '.instrument-select', 2);
                                acContractRenderInstrumentList('#multi-clearingTab','#mac-new-contract-wrapper', ".instrument-wrapper[data-instru-id='2']", '.instrument-select', 3);
                                cache.waitingForAllInstrumentsReady = false;
                                acContractRenderInstrumentList('#multi-clearingTab','#mac-new-contract-wrapper', ".instrument-wrapper[data-instru-id='3']", '.instrument-select', 5);

                                // Smart contract page -> mac -> balance update.
                                acContractNewContractBalanceUpdate('#mac-new-contract-wrapper');

                                cache.acContractInstruemntDefaultRender = false;
                            } else {
                                updateSVForWallet(assetlistedBalance);
                                updateAddressBalance('#receivingTab', '#receiving-ad-bal-wrapper tbody', '', '', '', true);
                                //updateAddressBalance('#sendingTab', '#sending-ad-bal-wrapper tbody', '','', '', true);
                                CorpUpdateAddressBalance('#corpactionTab', '#corp-ad-bal-wrapper tbody', '', '', true);

                                // Smart contract page -> dvp -> address balances.
                                contractUpdateBalance('#dvp-parta-bl-wrapper', "", "dvp");
                                contractUpdateBalance('#dvp-partb-bl-wrapper', "", "dvp");
                                contractUpdateBalance('#dvp-partc-bl-wrapper', "", "dvp");

                                // Smart contract page -> sac -> balance update.
                                acContractNewContractBalanceUpdate('#sac-new-contract-wrapper');

                                // Smart contract page-> sac -> balance table update.
                                contractUpdateBalance('#sac-parta-bl-wrapper', "", "sac", "partyA");
                                contractUpdateBalance('#sac-partb-bl-wrapper', "", "sac", "partyB");
                                contractUpdateBalance('#sac-partc-bl-wrapper', "", "sac", "partyC");
                                contractUpdateBalance('#sac-partd-bl-wrapper', "", "sac", "partyD");

                                // Smart contract page -> mac -> balance update.
                                acContractNewContractBalanceUpdate('#mac-new-contract-wrapper');

                                // Smart contract page-> sac -> balance table update.
                                contractUpdateBalance('#mac-parta-bl-wrapper', "", "mac", "partyA");
                                contractUpdateBalance('#mac-partb-bl-wrapper', "", "mac", "partyB");
                                contractUpdateBalance('#mac-partc-bl-wrapper', "", "mac", "partyC");
                                contractUpdateBalance('#mac-partd-bl-wrapper', "", "mac", "partyD");

                            }

                        }
                        if (cache.recentStateViews.length > 20) cache.recentStateViews.pop(); //remove the old state view.


                    // Render XChain detail: xcdetails
                    updateXChainDetail(Data['xcdetails']);

                    // Update explore address holding balance.
                    checkAddressHoldingUpdate();
                }catch(e){
                    if (!cache.prop) console.log(e);
                }
                break;


            default:
                break;
        }
    }
    catch(e){
        if(!cache.prod) console.log(e.lineNumber + ' : '+ e.message);
    }
}

Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};