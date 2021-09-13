$(document).ready(function(){

    //Handle data.
    togglePagesHandler();// Handle page toggle, when user chose different menu item.
    //$('[data-page-id = "transactions-view-wrapper"]').trigger('click');
    nodeInitialisation(0, true); // Initialise all necessary data, and connect to default node.
    nodeSwitchingHandler(); // Handle node switching.

    // Dash board
    proposalClickEventHandler(); //handle click event on proposals.

    //Block view page
    userBlockSelectHandler(); //Handle user select block in block view page.
    rawBlockDataEventHandler(); // Handle user click on view raw block data.

    //State view page
    userStateViewSelectHandler(); //Handle user select state view in state view page.
    userStateViewAssetListSelectHandler(); // Handle user select asset list in state view page.
    rawSVDataEventHandler(); // Handle user click on view raw SV data.
    userStateViewIssuerSelectHandler();

    //sort order handlers:
    userStateViewAssetListOrderHandler(); // Change the sort order.
    userStateViewAssetDetailOrderHandler(); // Handle sort order for asset detail

    //wallet page
    sendTransactionHandler();
    dropdownSelectUpdateListener();
    generateQRHandler();
    dairyTxSelectHandler();
    manualCollapseHandler();
    sendCorpBtnLisener();
    corpActionSelectHandler();
    corpDividendCurrencySelectHandler();
    dvpContractAddressSelectHandler();
    dvpContractEventHandler();
    acContractAddressSelectHandler();
    acContractInstrumentSelectHandler();
    acContractQtyInputsHandler();
    acContractEventHandler();

    //transaction page
    transactionToggleReceiveSate(); //toggle if received update.

    //setting page
    paymentSystemSettingHandler(); //Payment system scenario setting handler.

    //log page
    areaLogSettingsStateHandler(); // Handle log area and log level
    diagnosisMonitorHandler(); // Handle diagnosis update
    overDriverSettingHandler(); // Handle overdrive setting.
    diagnosisTxHandler(); // Handle event for TX diagnosis btns
    diagnosisSvDumpHandler(); // Handle event for dump sv
    diagnosisSvDataClickHanler();

    getTwitterForMessage();
    qtyInputHandler(); // Make the quantity input shown as commas separated.

    longPollingCheckLoginState(); // Longpolling for login state.

    //Explorer related.
    explorerSearchBtnEventHandler(); // Handle dialog popup.
    explorerSearchPopupEventsHandler(); //Handle popup window events.

});