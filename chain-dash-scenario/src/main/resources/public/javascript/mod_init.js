/**********************************************************************************************
 *
 * Set up globals
 *
 **********************************************************************************************/

if (document.dataCache == undefined)
{
    document.dataCache = {};
}

var cache = document.dataCache;

cache.chainID = $('body').attr('data-chain-id');
cache.chainConfigInfo = {};
getChainsConfigInfo(['psGroupID','chainTheme','RestPort','url','showXChain']);

var port2connect = !getUrlParameter('port') ? (location.port?location.port:80): getUrlParameter('port');
var host2connect = !getUrlParameter('host') ? location.hostname: getUrlParameter('host');
// port2connect=13405;
//var port2connect = !getUrlParameter('port') ? 12505: getUrlParameter('port');

var currency2use = !getUrlParameter('currency') ? 'gbp': getUrlParameter('currency');

cache.nodesInfo =   [ // Basic nodes info.
/*
 London 139.162.198.12
 New Jersey 45.33.90.30
 Fremont (CA) 173.255.212.83
 Singapore 139.162.22.254
 Frankfurt  139.162.146.67
 */
    //{nodeName : 'London', nodeURL : '127.0.0.1', nodePortNo : 13505},
    //{nodeName : 'London', nodeURL : '139.162.198.12', nodePortNo : 13305},
    {nodeName : 'London', nodeURL : host2connect, nodePortNo : port2connect},
    {nodeName : 'San Francisco', nodeURL : '173.255.212.83', nodePortNo : port2connect},
    {nodeName : 'NewJersey', nodeURL : '45.33.90.40', nodePortNo : port2connect},
    {nodeName : 'Singapore', nodeURL : '139.162.27.186', nodePortNo : port2connect},
    {nodeName : 'Frankfurt', nodeURL : '139.162.146.67', nodePortNo : port2connect}
];


cache.nodeOrder = {'london':0, 'sanfrancisco':1, 'newjersey':2, 'singapore':3, 'frankfurt': 4}; //use for check index
cache.logAreaAndLevelMap = {'log_area_stateView': parseInt('100',16), 'log_area_scenario': parseInt('200',16),'log_area_validate': parseInt('400',16), 'log_area_webSockets': parseInt('800',16), 'log_area_messaging': parseInt('1000',16), 'log_area_voting': parseInt('2000',16), 'log_area_functions': parseInt('4000',16), 'log_area_critical': parseInt('8000',16), 'log_area_proposal': parseInt('10000',16),
                            'default': parseInt('0',16), 'level1': parseInt('1',16), 'level2': parseInt('2',16), 'level3': parseInt('3',16), 'level4': parseInt('4',16), 'level5': parseInt('5',16), 'levelTop': parseInt('ff',16)};

cache.logLevelMap = ['default', 'level1', 'level2', 'level3', 'level4', 'level5', 'levelTop'];
cache.dividendInfo = {'AUD': 'RBA', 'GBP' : 'BofE', 'EUR': 'ECB', 'JPY': 'BoJ', 'USD' :  'Federal Reserve'};
cache.defaultSet ={
    'aud': {
      currencyNamespace: 'RBA',
      currencyClass: 'AUD',
      stockNamespace: 'Computershare',
      stockClass: 'CPU',
      acCurrencySet: ['RBA|AUD','BofE|GBP','Federal Reserve|USD']
    },
    'gbp': {
        currencyNamespace: 'BofE',
        currencyClass: 'GBP',
        stockNamespace: 'AVIVA',
        stockClass: 'AV.',
        acCurrencySet: ['BofE|GBP', 'ECB|EUR','Federal Reserve|USD']
    }
};
cache.default = cache.defaultSet[currency2use];
cache.prod = false;

/**
 * Initialise all necessary data, and connect to a node with a given NodeID.
 * @param NodeID
 */
function nodeInitialisation(NodeID, isDefaultConnection){
    cache.recentBlocks = []; // Store all recent blocks.
    /**
     * Data Structure summary.
     * [Height]... [assetList]
     *                 [asset]
     *                     [address][amount][asset].
     * @type {Array}
     */
    cache.recentStateViews = []; //Store all recent state views.
    cache.indexedStateView = {}; //three dimensional array.
    cache.fiveRecentProposal = []; // Store last five proposal
    cache.latestTransactions = []; // Store latest transactions.
    cache.xchainMovementInArr = []; // Store latest xchainIn count.
    cache.xchainMovementOutArr = [];// Store latest xchainOut count.
    cache.blockMovementArr = [];  // Store the historic block movement.
    cache.movements24hArr = []; //Store the movements of 24h rates.
    cache.blockTimeTakenArr = []; // Store the historic block time taken.
    cache.dairyMovementsArrNew = []; // Store the historic movements per day (smooth out version. Average with previous value).
    cache.dairyMovementsArr = [];  // Store the historic movements per day.
    cache.dairyMovementsArrNew = []; // Store the historic movements per day (smooth out version. Average with previous value).
    cache.lastBlockTimeBlockTimeArr = []; // Store the historic block time average.
    cache.currentConnectedNode = {}; // Current connected node.
    cache.blockViewSelected = false; // Record the state, whether user had selected a block.
    cache.stateViewSelected = false; // Record the state, where user had make a selection on the state view page.
    cache.previousChosenAssetNameIndex= '';
    cache.assetSortOrder = 1; // Default asset list sort order. 1: ascending, -1 descending.
    cache.assetDetailSortName = 'address'; // // Default asset detail sort by attribute.
    cache.assetDetailSortOrder = 1; // Default asset detail sort order. 1: ascending, -1 descending.
    cache.assetDetailResort = false;
    cache.receiveTransactionUpdateState = true; // Flag to record if we want to received update.
    cache.paymentSystemsLabel = [cache.chainConfigInfo[cache.chainID]['psGroupID']['ps1'],cache.chainConfigInfo[cache.chainID]['psGroupID']['ps2'],
        cache.chainConfigInfo[cache.chainID]['psGroupID']['ps3'], cache.chainConfigInfo[cache.chainID]['psGroupID']['ps4'],
        cache.chainConfigInfo[cache.chainID]['psGroupID']['ps5'], cache.chainConfigInfo[cache.chainID]['psGroupID']['ps6']];
    cache.paymentSystemsArr = ['CHAPS','CREST', 'CLS', 'FPS', 'BACS', 'LINK'];
    cache.scenarioBenchMarkArr = [[1,140000], [2,190000], [3,790000], [4,4350000], [5,23090000], [6,5700000]];
    cache.scenarioActualArr = [[1,0], [2,0], [3,0], [4,0], [5,0],[6,0]];
    cache.scenarioState = []; //Setting page - scenario states.
    cache.barChartData = []; //barchar data.
    cache.newBlockTimeTaken = 1; //record when the last block came in, to calculate time taken for the last block.
    clearInterval(cache.newBlockTimeTakenTimer);
    cache.gotInitialBV = 0;
    cache.resultObject; // Store the recent stateview for search explorer.
    cache.explorerHoldingSortName = 'namespace'; // // Default explorer address holding sort by attribute.
    cache.explorerHoldingSortOrder = 1; // Default explorer address holding sort order. 1: ascending, -1 descending.
    cache.contractData = {dvp:{}, sac: {}, mac:{}}; // Store existing contract.
    cache.commitAwaitBlockResponse = {dvp:[], sac: [], mac: []}; // Store the parties that commit the contract in the page, and waiting for response.
    cache.instrumensInContract = {dvp:[], sac:{partyA:[],partyB:[],partyC:[],partyD:[]}, mac :{partyA:[],partyB:[],partyC:[],partyD:[]}}; // Store the instruments in a contract, for render later.
    cache.rawContractRendered = {dvp: false, sac:false, mac: false}; // Flag to render raw contract. After contract_detail was request, should not render again unless, contract is updated.
    cache.historicContracts = [[],{},{}];  // Store last 20 contracts in the cache, for search explorer. [0,1]: 0: chronological indexed contract address (Acting as a lookup). 1: actual contract objects.
    cache.acContractInstruemntDefaultRender = true; // Flag to avoid re-render instrument list on default rendering.
    cache.waitingForAllInstrumentsReady = false;// Prevent instrument select handler detect the change and update multiple time, cause update balance and generate default amounts for contract, unwanted.

    cache.diagnosisSV = {}; // Store the diagnosis SV data for retrieving.

    cache.ownedAddrList = []; // Store the addresses owned by the connected node. Avoid creating contract with address that not belong to the node.



    // Connect to default node.
    cache.currentConnectedNode = new Node(NodeID);
    cache.currentConnectedNode.connectToNode(isDefaultConnection);

    initialiseDashBarChart();
    setChainSetting();
    getOwnedAddr();

    // Load dumped tx list through websock.
    sendCommandToDumpedTxList(2);
    sendCommandToDumpedSvList(2);
}

/**********************************************************************************************
 *
 * Node Classes.
 *
 **********************************************************************************************/
function Node (nodesID) {
    var _this = this;

    //var _nodeID = nodesID;
    var _nodeName = cache.nodesInfo[nodesID]['nodeName'];
    var _nodeURL = cache.nodesInfo[nodesID]['nodeURL'];
    var _nodePortNo = cache.nodesInfo[nodesID]['nodePortNo'];

    this.nodeURL = _nodeURL;
    this.port = _nodePortNo;

    this.connectToNode = function (isDefaultConnection)
    {
        if(isDefaultConnection)
        {

            if ("WebSocket" in window) {
                if (document.SetlSocketCallback == undefined)
                {
                    document.SetlSocketCallback = new SetlCallbackClass(); // Instantiate call back class.

                    document.SetlSocketCallback.addHandler('OnOpen', Setl_SubscribeForUpdate); // Subscribe to update.

                    // If socket closes, re-open it.

                    document.SetlSocketCallback.addHandler('OnClose',
                        function (ID, message, UserData)
                        {
                            if (document.SetlSocket)
                                document.SetlSocket.openWebSocket();
                        }
                    );

                    document.SetlSocketCallback.addHandler('Update', Setl_ProcessUpdateMessage);

                }

                if (document.SetlSocket == undefined)
                {
                    // Instantiate WebSocket object.
                    document.SetlSocket = new SetlSocketClass("ws:", _nodeURL, _nodePortNo, "", document.SetlSocketCallback);

                    $('#current-connected-node-title').html(_nodeName); // Render to Dom.
                }
            }

        }
        else
        {
            try
            {
                // Instantiate WebSocket object.
                document.SetlSocket = new SetlSocketClass("ws:", _nodeURL, _nodePortNo, "", document.SetlSocketCallback);


                // Display notification message.
                BootstrapDialog.alert({
                    title: 'Notification',
                    message: 'Connected to Node ' + _nodeName + ': ' + _nodeURL + ' (' + _nodePortNo + ')'
                });

                $('#current-connected-node-title').html(_nodeName); // Render to Dom.


            }catch(e)
            {
                if (!document.dataCache.prod) console.log("Connection failed!");
            }
        }
    };

    this.disConnectToNode = function(){
        document.SetlSocket.closeWebSocket();
    };
}
