/**********************************************************************************************
 *
 * Helper functions.
 *
 **********************************************************************************************/
/**
 * Generate random value.
 * @param min
 * @param max
 * @returns random value.
 */
function getRandomArbitrary(min, max) {
    return Math.random() * (max - min) + min;
}

/**
 *
 * @param val
 * @returns {*}
 */
function commaSeparateNumber(val){
    //while (/(\d+)(\d{3})/.test(val.toString())){
    //    val = val.toString().replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
    //}
    //return val;
    // ref: http://stackoverflow.com/questions/2901102/how-to-print-a-number-with-commas-as-thousands-separators-in-javascript
    val = val.toString().replace(/,/g,'');
    return (val).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

/**
 * Convert unix timestamp to UTC string.
 * @param unixTime
 * @returns {string}
 */
function unixTimeToUTC(unixTime){
    var date = new Date(parseInt(unixTime*1000));
    var utcString = '';
    return (date.getUTCHours() > 9 ? date.getUTCHours() : '0'+date.getUTCHours()) + ':'+ (date.getUTCMinutes() > 9 ? date.getUTCMinutes() : '0'+date.getUTCMinutes()) + ':' + (date.getUTCSeconds() > 9 ? date.getUTCSeconds() : '0'+date.getUTCSeconds())+' UTC';
}


/**
 * Node switching handler.
 */
function nodeSwitchingHandler(){
    $('.jvectormap-marker').on('click tap touchend',function(){
        try {
            cache.currentConnectedNode.disConnectToNode();
            nodeInitialisation($(this).attr('data-index'), false); // Initialise all necessary data, and connect to default node.
            getInitialSnapshotFromThroughSocket(); // Get the initial snapshot, and handle it.

            initialiseDashBarChart();
            setChainSetting();
            // Load dumped tx list through websock.
            sendCommandToDumpedTxList(2);
            sendCommandToDumpedSvList(2);
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Toggle pages
 */
function togglePagesHandler(){
    $('#sidebar .side-menu a').on('click tap touchend', function(e){
        try {
            var targetID = ($(this).attr('data-page-id')); // Get page wrapper ID.
            $('.page-wrapper').removeClass('active'); // Hide all the page wrappers
            $('.side-menu li').removeClass('active');

            // Check if this is logout attempt.
            if (targetID != undefined) {
                $('#' + targetID).addClass('active');// Show the clicked page wrapper.
                $(this).closest('li').addClass('active');
            }
            else {
                $.ajax({url: "/php/dummyAuthen.php", data: {'logout': 1}}).done(function () {
                    window.location.replace("login.php");
                });
            }

            //reset the select states.
            if (targetID != 'blocks-view-wrapper') cache.blockViewSelected = false;
            if (targetID != 'state-view-wrapper') cache.stateViewSelected = false;

            //Make sure the payment setting bar-chart show. As bar-chart will not draw when the div was "display = none".
            redrawBarChart();
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Listen for user selecting a block, in block view page.
 */
function userBlockSelectHandler(){
    $('#blocks-view-wrapper #block-list-wrapper').on('click tap touchend', '.list-group-item', function(e){
        try {
            e.preventDefault();

            //get the selected index.
            var index = $(e.target).index();

            // Record that user has selected a block.
            if ($('.list-group-item:contains("Current Block")')[0] === $(e.target).closest('.list-group-item').get(0)) { //if "current" block is selected
                cache.blockViewSelected = false;
                $(e.target).blur();
            }
            else {
                cache.blockViewSelected = true;
            }

            //remove all other active class
            $('#blocks-view-wrapper #block-list-wrapper .list-group .list-group-item').removeClass('active');
            $(e.target).addClass('active');

            //Update the block detail view, in block view page.
            updateBlockViewBlockDetial(index);

            //if this is a mobile device, collapse the box after user select.
            if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
                $('#block-list-content-wrapper').collapse("hide");
                $('#blocks-view-wrapper #block-list-wrapper h2').toggleClass('collapsed');
            }
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 *  Handle view raw block data event.
 */
function rawBlockDataEventHandler(){
    $('#blocks-view-wrapper #block-list-wrapper').on('click tap touchend', '.btn.block-detial', function(e){
        try {
            e.preventDefault();

            //get the selected index.
            var blockIndex = $(e.target).closest('.list-group-item').index();

            var blockHtml = $.parseHTML(JSONTree.create(cache.recentBlocks[blockIndex])); // parseHTML: make the string clean for render. otherwise it will render some unwanted <br />. Do not know why.

            var dialog = new BootstrapDialog({
                type: 'type-info',
                cssClass: 'rawBlockDialog',
                size: BootstrapDialog.SIZE_WIDE,
                title: 'Raw Block Detail',
                message: blockHtml,
                closable: true
            });

            //// Get the size of the screen, in order to set the modal max height.
            var maxHeight = $(window).height() - 60;
            dialog.realize();
            dialog.getModalContent().css('max-height', maxHeight + 'px');
            dialog.getModalBody().css('max-height', maxHeight - 78 + 'px');
            dialog.open();
            //setTimeout(function(){$('.jstValue').children().children().children().children().children().children('.jstFold').trigger('click');},500);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}

/**
 * Listen for user selecting a state view, in state view page.
 */
function userStateViewSelectHandler(){
    $('#state-view-wrapper #state-list-wrapper').on('click tap touchend', '.list-group-item', function(e){
        e.preventDefault();

        try{
            //get the selected index.
            var index = $(this).index();

            // Record that user has selected a block.
            if( $('.list-group-item:contains("Current State View")')[0] === $(e.target).closest('.list-group-item').get(0)){ //if "current" block is selected
                cache.stateViewSelected = false;
                $(this).blur();
            }
            else
                cache.stateViewSelected = true;

            //remove all other active class
            $('#state-view-wrapper #state-list-wrapper .list-group .list-group-item').removeClass('active');
            $(this).addClass('active');

            //Update the asset list, in state view page.
            updateStateViewAssetList(index);

            //if this is a mobile device, collapse the box after user select.
            if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
                $('#stateview-list-content-wrapper').collapse("hide");
                $('#state-view-wrapper #state-list-wrapper h2').toggleClass('collapsed');
            }
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}


/**
 *  Handle view raw SV data event.
 */
function rawSVDataEventHandler(){
    $('#state-view-wrapper #state-list-wrapper').on('click tap touchend', '.bv-detial.btn', function(e){
        try {
            e.preventDefault();

            //get the selected index.
            var bvIndex = $(e.target).closest('.list-group-item').index();

            var bvHtml = $.parseHTML(JSONTree.create(cache.recentStateViews[bvIndex])); // parseHTML: make the string clean for render. otherwise it will render some unwanted <br />. Do not know why.

            var dialog = new BootstrapDialog({
                type: 'type-info',
                cssClass: 'rawBlockDialog',
                size: BootstrapDialog.SIZE_WIDE,
                title: 'Raw SV Detail',
                message: bvHtml,
                closable: true,
                autodestroy: true
            });

            //// Get the size of the screen, in order to set the modal max height.
            var maxHeight = $(window).height() - 60;
            dialog.realize();
            dialog.getModalContent().css('max-height', maxHeight + 'px');
            dialog.getModalBody().css('max-height', maxHeight - 78 + 'px');

            dialog.open();

            //setTimeout(function(){$('.jstValue').children().children().children().children().children().children('.jstFold').trigger('click');},500);
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Listen for user selecting an asset, in state view page.
 */
function userStateViewAssetListSelectHandler(){
    $('#state-view-wrapper #asset-list-wrapper').on('click tap touchend', '.list-group-item', function(e){
        try {
            e.preventDefault();

            //get the selected asset name index.
            var assetNameIndex = $(e.target).attr('data-asset-name');

            // Record that user has selected a block.
            //cache.stateViewSelected = true;

            //remove all other active class
            $('#state-view-wrapper #asset-list-wrapper .list-group .list-group-item').removeClass('active');
            $('#state-view-wrapper #asset-list-wrapper .list-group .list-group-item').blur();
            $(e.target).addClass('active');

            // Get the state view index.
            var stateviewIndex = $('#state-view-wrapper #state-list-wrapper .list-group-item.active').index();

            // Update the asset detail view, in the state view.
            updateStateViewAssetDetail(stateviewIndex, assetNameIndex);

            //if this is a mobile device, collapse the box after user select.
            if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
                $('#asset-list-content-wrapper').collapse("hide");
                $('#asset-list-wrapper #asset-list-content-wrapper h2').toggleClass('collapsed');
            }
        }catch(err) {
            if (cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * State view page.
 * Select the corresponding option in corpactionTab, when a table row is selected in the asset table.
 */
function userStateViewIssuerSelectHandler(){
    $('#state-view-wrapper #state-view-assets-info-wrapper').on('click tap touchend', 'tr', function(e) {
        try {
            e.preventDefault();

            //get the selected issuer.
            var issuer = this.children[1].innerHTML;
            var security = $('#state-view-wrapper #asset-list-wrapper .list-group-item.active').attr('data-asset-name');

            // Select the issue and security.
            $('#corpactionTab select.issuer-select').selectpicker('val', issuer);
            $('#corpactionTab select.security-select').selectpicker('val', security);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Change the sort order for asset list.
 */
function userStateViewAssetListOrderHandler(){
    $('#asset-list-wrapper').on('click tap touchend', '.sort-icon', function(e){
        try {
            //toggle sort icon.
            if ($('#asset-list-wrapper .sort-icon .fa').hasClass('fa-sort-amount-asc')) {
                $('#asset-list-wrapper .sort-icon .fa').removeClass('fa-sort-amount-asc');
                $('#asset-list-wrapper .sort-icon .fa').addClass('fa-sort-amount-desc');
            } else {
                $('#asset-list-wrapper .sort-icon .fa').removeClass('fa-sort-amount-desc');
                $('#asset-list-wrapper .sort-icon .fa').addClass('fa-sort-amount-asc');
            }

            //toggle sort method.
            cache.assetSortOrder = (cache.assetSortOrder == 1 ? -1 : 1);

            // Get the state view index.
            var stateviewIndex = $('#state-view-wrapper #state-list-wrapper .list-group-item.active').index();


            // Update the asset detail view, in the state view.
            updateStateViewAssetList(stateviewIndex);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    })
}

/**
 * Handle sort order for asset detail
 */
function userStateViewAssetDetailOrderHandler(){
    $('#state-view-assets-info-wrapper th').on('click tap touchend', function(e){
        try {
            var sortName = $(this).attr('data-attr');

            if (sortName != 'asset' && sortName != 'certifier') {
                //if the attribute is the same, toggle the sort order.
                if (sortName == cache.assetDetailSortName) {
                    //toggle sort icon.
                    if ($('#state-view-assets-info-wrapper .sort-icon .fa').hasClass('fa-sort-amount-asc')) {
                        $('#state-view-assets-info-wrapper .sort-icon .fa').removeClass('fa-sort-amount-asc');
                        $('#state-view-assets-info-wrapper .sort-icon .fa').addClass('fa-sort-amount-desc');
                    } else {
                        $('#state-view-assets-info-wrapper .sort-icon .fa').removeClass('fa-sort-amount-desc');
                        $('#state-view-assets-info-wrapper .sort-icon .fa').addClass('fa-sort-amount-asc');
                    }

                    //toggle sort method.
                    cache.assetDetailSortOrder = (cache.assetDetailSortOrder == 1 ? -1 : 1);
                }
                //otherwise, change the sorting attribute, and reset the sort order.
                else {
                    //remove all the sort icon in the table header.
                    $('#state-view-assets-info-wrapper .sort-icon').empty();
                    //Add the new sort icon to the selected table header.
                    $('[data-attr="' + sortName + '"] span').html('<i class="fa fa-sort-amount-asc"></i>');
                    cache.assetDetailSortName = $(this).attr('data-attr');
                    cache.assetDetailSortOrder = 1;
                }

                // Redraw the asset detail.
                //get current active stateview index.
                var stateviewIndex = $('#state-view-wrapper #state-list-wrapper .list-group-item.active').index();

                //get current active asset name index.
                var assetNameIndex = $('#state-view-wrapper #asset-list-wrapper .list-group-item.active').attr('data-asset-name');

                cache.assetDetailResort = true;

                updateStateViewAssetDetail(stateviewIndex, assetNameIndex);

            }
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}

/**
 * Handle toggle receive transaction state.
 */
function transactionToggleReceiveSate(){
    $('#receiveTransactionSwitch label').on('click tap touchend',function(e){
        try {
            setTimeout(function () {
                if ($('#receiveTransactionSwitch .switch-animate').hasClass('switch-on'))
                    cache.receiveTransactionUpdateState = true;
                else
                    cache.receiveTransactionUpdateState = false;
            }, 0);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}


/**
 * Handle send transaction todo to be delete
 */
function sendTransactionHandler(){

    $('#send-transaction').on('click tap touchend', function(e){
        try {
            setTimeout(function () {
                if ($('.wallet-page-transaction-wrapper .formError').length == 0) {
                    $('.wallet-page-transaction-wrapper').addClass('zoomOutUp');

                    var issuer = $('#sendingTab .issuer-select').val();
                    var security = $('#sendingTab .security-select').val();
                    var toAddress = $('#sendingTab .toAddress-select').val();
                    var fromAddress = $('#sendingTab .fromAddress-select').val();
                    var amount = $('#sendingTab #amount').val();
                    var pin = $('#sendingTab [name=pin]').val();

                    if (issuer == '' || security == '' || toAddress == '' || amount == '' || fromAddress == '')
                        return;

                    window.location.replace('/php/transferTxForm.php?issue=' + issuer + '&&security=' + security + '&&toAddress=' + toAddress + '&&fromAddress=' + fromAddress + '&&amount=' + amount + '&&pin=' + pin + '&&node=' + cache.currentConnectedNode.nodeURL + ":" + (cache.currentConnectedNode.port - 1) + "&&autosend=1");

                }
            }, 0);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    })
}

/**
 * Generate QR handler
 */
function generateQRHandler(){
    $('#generateQR').on('click tap touchend', function(e){
        try {
            e.preventDefault()
            var issuer = encodeURIComponent($('#receivingTab .issuer-select').val());
            var security = encodeURIComponent($('#receivingTab .security-select').val());
            var toAddress = encodeURIComponent($('#receivingTab .toAddress-select').val());
            var amount = encodeURIComponent(commasSeparatedToInt($('#receivingTab #amount').val()));

            if (issuer == '' || security == '' || toAddress == '' || amount == '')
                return;

            var qrText = 'http://' + cache.chainConfigInfo[cache.chainID]['url'] + '/php/transferTxForm.php?issue=' + issuer + '&&security=' + security + '&&toAddress=' + toAddress + '&&amount=' + amount + '&&node=' + cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort'];


            $('#qrcode').empty().qrcode({"width": 180, "height": 180, "text": qrText, "correctLevel": 2});
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

}

/**
 * Manual handle accordion toggle, due to a bug: "supper admin theme" cause accordion toggle not collapse others.
 */
function manualCollapseHandler(){
    $('#corp-action-wrapper .accordion-toggle').on('click tap touchend', function(e){
        try {
            e.preventDefault();
            $('.panel-collapse').collapse('hide');
            $(this).closest('.panel-collapse').collapse('show');
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * trigger balance table change.
 */
function corpActionSelectHandler(){
    $('#corp-action-wrapper').on('shown.bs.collapse', '.collapse', function(e){
        CorpUpdateAddressBalance('#corpactionTab','#corp-ad-bal-wrapper tbody', '','', true);
    })
}

/**
 * Contract Page
 */
/**
 * Render the address
 * @param id
 */
function contractUpadteAddrDropDown(div){
    try {
        $(div + ' select.address-select').empty();

        $(div + ' select.address-select').append(
            '<option value=""></option>'
        );

        $.each(cache.ownedAddrList, function (key,addr) {
            $(div + ' select.address-select').append(
                '<option value="' + addr + '">' + addr + '</option>'
            );
        });


        $(div + ' select.address-select').selectpicker('refresh');
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract Page -> dvp
 * Render default address
 * @param div
 */
function dvpContractDefaultAddress(div){
    try {
        // Update balance
        //var randomIndex = div === '#dvp-parta-bl-wrapper' ? 2 : 3;
        // make the second address has the default stock:
        if (div === '#dvp-parta-bl-wrapper') {
            var address = $(div + ' select.address-select option:nth-child(2)').attr('value'); // "Random" address
            //var address = Object.keys(cache.indexedStateView[cache.default.currencyNamespace][cache.default.currencyClass])[3];
        }
        else{
            var address = $(div + ' select.address-select option:nth-child(3)').attr('value'); // "Random" address
            //var address = Object.keys(cache.indexedStateView[cache.default.stockNamespace][cache.default.stockClass])[0];
            //if(address == $('#dvp-parta-bl-wrapper select.address-select').val()) address = Object.keys(cache.indexedStateView[cache.default.stockNamespace][cache.default.stockClass])[1];
        }

        $(div + ' select.address-select').selectpicker('val', address);
        contractUpdateBalance(div, address, "dvp");
        // Update contract
        var instrumentSelectDiv = div === '#dvp-parta-bl-wrapper' ? '#dvp-contract-wrapper-a' : '#dvp-contract-wrapper-b';
        contractRenderInstrumentSelect(instrumentSelectDiv, address);

        // Set the quantity input field a default value:
        var qty = div === '#dvp-parta-bl-wrapper' ? 1000 : 100;
        $(instrumentSelectDiv + ' .qty1').val(qty);

        // Trigger a change event so the part 3 qty update.
        if (instrumentSelectDiv === '#dvp-contract-wrapper-a')
            $(instrumentSelectDiv + ' .qty1').change();

    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

}

/**
 * Contract page -> sac / mac
 * @param parentdiv: div identifier
 * @param div : div identifier.
 * @param n : index of the list to select.
 */
function acContractDefaultAddress(parentdiv, div, n){
    try{
        var address = $(parentdiv + ' .address-select' + div + ' option:nth-child(' + n + ')').attr('value'); // "Random" address
        $(parentdiv + ' .address-select' + div ).selectpicker('val', address);

        // Set the corresponding address.
        var partyID = $(parentdiv + ' .address-select' + div ).attr('data-party');
        $(partyID + ' input').val(address);
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page -> sac/mac
 * Set reasonable qty for the contract.
 * @param div
 */
function acContractDefaultQtyForContract(div){
    try {
        var instruList = [];
        var partiesArr = ['.partyA', '.partyB', '.partyC', '.partyD'];
        var randomPartiesArr = [['.partyA', '.partyB', '.partyC', '.partyD'],['.partyD','.partyB', '.partyC','.partyA'], ['.partyC','.partyA', '.partyB', '.partyD']];
        randomPartiesArr =  shuffle(randomPartiesArr);
        var instrumentWrapper = [];

        if (div === '#sac-new-contract-wrapper') {
            instruList[0] = $(div + ' .partyA .instru').val();
            instrumentWrapper[0] = '';

        }else if(div === '#mac-new-contract-wrapper'){
            var instru1 = $(div + " .partyA .instrument-wrapper[data-instru-id='1'] .instru").val();
            var instru2 = $(div + " .partyA .instrument-wrapper[data-instru-id='2'] .instru").val();
            var instru3 = $(div + " .partyA .instrument-wrapper[data-instru-id='3'] .instru").val();

            instruList = [instru1, instru2, instru3];
            if(instru1 === '' || instru2 === '' || instru3 === '')
                return;
            instrumentWrapper = [".instrument-wrapper[data-instru-id='1']",".instrument-wrapper[data-instru-id='2']",".instrument-wrapper[data-instru-id='3']"];
        }else{
            instruList = [];
        }

        for (var i = 0; i < instruList.length; i++) {
            var totalPay = 0;
            var payingParties = [];
            var receiveParties = [];

            for (var j = 0; j < randomPartiesArr[i].length; j++) {
                var bal = $(div + ' ' +  randomPartiesArr[i][j] + ' ' + instrumentWrapper[i] + ' .bl-wrap').attr('data-val');
                // work out 10% of it.
                bal = parseInt(bal) * 0.1;

                if (bal > 0) {
                    bal = Math.round(bal);
                    payingParties.push([randomPartiesArr[i][j], bal]);
                    totalPay += bal;
                }
                else
                    receiveParties.push([randomPartiesArr[i][j], 0]);
            }

            // Assign the total paying out to receiving parties.
            if (receiveParties.length === 1) {
                receiveParties[0][1] = totalPay;
            } else if (receiveParties.length === 2) {
                receiveParties[0][1] = Math.round(totalPay * 0.2);
                receiveParties[1][1] = totalPay - receiveParties[0][1];
            } else if (receiveParties.length === 3) {
                receiveParties[0][1] = Math.round(totalPay * 0.2);
                receiveParties[1][1] = Math.round(totalPay * 0.3);
                receiveParties[2][1] = totalPay - receiveParties[0][1] - receiveParties[1][1];
            } else if(receiveParties.length === 4){
                //If all parties are Receive, should not happen, let's see.
                if(cache.prod == false) console.log("Unknown Error, function: acContractDefaultQtyForContract() : All parties are receiving, Instrument is " + instruList[i]);

            } else if(receiveParties.length === 0){
                //If all parties are paying, set partyB and C Receiving:
                totalPay = totalPay - payingParties[2][1] - payingParties[3][1];

                receiveParties[0] = [payingParties[2][0], 0];
                receiveParties[1] = [payingParties[3][0], 0];

                // Delete them from payingParties array.
                payingParties.pop();
                payingParties.pop();

                receiveParties[0][1] = Math.round(totalPay * 0.2);
                receiveParties[1][1] = totalPay - receiveParties[0][1];
            } else{
                if(cache.prod == false) console.log("Unknown Error, function: acContractDefaultQtyForContract()");
            }

            // Assign the qtyes to the inputs.
            for(var k = 0; k < receiveParties.length; k++){
                $(div + ' ' + receiveParties[k][0]  + ' ' + instrumentWrapper[i] + ' input[data-type = "receive"]').val(commaSeparateNumber(receiveParties[k][1]));
                $(div + ' ' + receiveParties[k][0]  + ' ' + instrumentWrapper[i] + ' input[data-type = "receive"]').removeAttr('disabled');
                $(div + ' ' + receiveParties[k][0]  + ' ' + instrumentWrapper[i] + ' input[data-type = "pay"]').attr('disabled','disabled');
                $(div + ' ' + receiveParties[k][0]  + ' ' + instrumentWrapper[i] + ' input[data-type = "pay"]').val('');
            }
            for(var k = 0; k < payingParties.length; k++){
                $(div + ' ' + payingParties[k][0]  + ' ' + instrumentWrapper[i] +  ' input[data-type = "pay"]').val(commaSeparateNumber(payingParties[k][1]));
                $(div + ' ' + payingParties[k][0]  + ' ' + instrumentWrapper[i] +  ' input[data-type = "pay"]').removeAttr('disabled');
                $(div + ' ' + payingParties[k][0]  + ' ' + instrumentWrapper[i] +  ' input[data-type = "receive"]').attr('disabled','disabled');
                $(div + ' ' + payingParties[k][0]  + ' ' + instrumentWrapper[i] +  ' input[data-type = "receive"]').val('');
            }
        }


    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page -> sac / mac
 * @param rootdiv: div identifier.
 * @param parentdiv: div identifier
 * @param instrumentWrapper: div identifier
 * @param div: div identifier.
 * @param n: index of the instrument to select
 */
function acContractRenderInstrumentList(rootdiv, parentdiv, instrumentWrapper, div, n){
    try {
        // Previous selected instrument
        var psInstru = $(rootdiv + ' ' + instrumentWrapper +  ' .instrument-select').val();

        var renderingInstruments = [];
        var addr1 = $(rootdiv + ' .address-select.partyA').val();
        var addr2 = $(rootdiv + ' .address-select.partyB').val();
        var addr3 = $(rootdiv + ' .address-select.partyC').val();
        var addr4 = $(rootdiv + ' .address-select.partyD').val();
        var addrs = [addr1, addr2, addr3, addr4];

        // Set party Addresses.
        $(parentdiv + ' .partyA').attr('data-addr',addr1);
        $(parentdiv + ' .partyB').attr('data-addr',addr2);
        $(parentdiv + ' .partyC').attr('data-addr',addr3);
        $(parentdiv + ' .partyD').attr('data-addr',addr4);


        if(addr1 === '' || addr2 === '' || addr3 === '' || addr4 === '') {
            renderSelectList(parentdiv, instrumentWrapper, div, []);
            $(parentdiv + ' ' + instrumentWrapper + ' input').val('');
            $(parentdiv + ' ' + instrumentWrapper + ' .instru-bal').html('');

            return 0;
        }

        for (var i = 0; i < addrs.length; i++) {
            for (var key in cache.recentStateViews[0]['Assetbalances'][addrs[i]]) {
                renderingInstruments.push(key);
            }
        }

        // make the list unique
        renderingInstruments = renderingInstruments.filter(function (itm, i, arr) {
            return i == arr.indexOf(itm);
        });

        renderingInstruments.sort();

        // remove currency:
        var currencyArr = ['GBP','EUR', 'USD', 'JPY', 'AUD'];
        renderingInstruments = renderingInstruments.filter(function (item, i, arr) {
            var classid = item.split(/[|,:]/)[1];
            return $.inArray(classid, currencyArr) !== -1;
        });

        renderSelectList(parentdiv, instrumentWrapper, div, renderingInstruments);

        // Set it back to the previous selected value. if possible
        if ( psInstru !== '' && $.inArray(psInstru, renderingInstruments)) {
            //empty input fields:
            $(parentdiv + ' ' + instrumentWrapper + ' input.qty').val('');

            $(rootdiv + ' ' + instrumentWrapper + ' .instrument-select').selectpicker('val', psInstru);
        } else{
            if(instrumentWrapper == '' || instrumentWrapper == ".instrument-wrapper[data-instru-id='1']"){
                var randomValue = cache.default.acCurrencySet[0];
            }else if (instrumentWrapper == ".instrument-wrapper[data-instru-id='2']"){
                //var randomValue = 'ECB|EUR';
                var randomValue = cache.default.acCurrencySet[1];
            }else if (instrumentWrapper == ".instrument-wrapper[data-instru-id='3']"){
                var randomValue = cache.default.acCurrencySet[2];
            }
            try {
                $(rootdiv + ' ' + instrumentWrapper + ' .instrument-select').selectpicker('val', randomValue);
            } catch(e){
                var randomValue = $(rootdiv + ' ' + instrumentWrapper + ' .instrument-select option:nth-child('+n+')').attr('value');
                $(rootdiv + ' ' + instrumentWrapper + ' .instrument-select').selectpicker('val', randomValue);
            }
        }

    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Render a list to
 * a select input.
 * @param: parentdiv
 * @param: instrumentWrapper
 * @param: selectElement.
 * @param: list.
 */
function renderSelectList(parentdiv, instrumentWrapper, selectElement, list){
    $(parentdiv + ' ' + instrumentWrapper + ' select' + selectElement).empty();

    $(parentdiv + ' ' + instrumentWrapper + ' select' + selectElement).append(
        '<option value=""></option>'
    );

    for(var i = 0; i < list.length; i++){
        $(parentdiv + ' ' + instrumentWrapper + ' select' + selectElement).append(
            '<option value="'+list[i]+'">' +
                list[i] +
            '</option>'
        );
    }
    $(parentdiv + ' ' + instrumentWrapper + ' ' + selectElement).selectpicker('refresh');
}

/**
 * Contract page
 * Update balance of an address in balance table
 * @param div
 * @param address
 * @param contractType
 * @param party
 */
function contractUpdateBalance(div, address, contractType, party){
    try {
        var addressWrapper = {dvp:  'select.address-select', sac: 'input', mac: 'input'};

        if (contractType === 'dvp' && div === '#dvp-partc-bl-wrapper' && $.isEmptyObject(cache.contractData["dvp"])){
            return;
        }

        if (address === undefined || address === '') {
            address = $(div + ' ' + addressWrapper[contractType]).val();
        }

        //sort it.
        var instrumentsToRender;
        if(contractType == 'dvp') {
            if ($.isEmptyObject(cache.contractData[contractType])) {
                instrumentsToRender = [];
                for (var key in cache.recentStateViews[0]['Assetbalances'][address]) {
                    instrumentsToRender.push(key);
                }

                instrumentsToRender = instrumentsToRender.sort();
            } else {
                if (div !== '#dvp-partc-bl-wrapper') {
                    cache.instrumensInContract[contractType] = cache.instrumensInContract[contractType].sort();
                    instrumentsToRender = cache.instrumensInContract[contractType];
                }
                else {
                    instrumentsToRender = [];
                    instrumentsToRender.push($('#dvp-contract-wrapper-c select.instrument-select').val());
                }
            }

        }else{
            instrumentsToRender = [];
            if(!$.isEmptyObject(cache.contractData[contractType])) {
                cache.instrumensInContract[contractType][party] = cache.instrumensInContract[contractType][party].sort();
                instrumentsToRender = cache.instrumensInContract[contractType][party];
            }
        }

        // Duplicate only exist when we rendering contract instruments.
        if (!$.isEmptyObject(cache.contractData[contractType])) {
            instrumentsToRender = instrumentsToRender.filter(function (itm, i, a) {
                return i == a.indexOf(itm);
            });
        }

        $(div + ' tbody').empty();
        $(div + ' tbody').attr('data-addr', address);

        $.each(instrumentsToRender, function (index, value) {

            var amount = cache.recentStateViews[0]['Assetbalances'][address][value] !== undefined ? cache.recentStateViews[0]['Assetbalances'][address][value] : 0;

            $(div + ' tbody').append(
                '<tr data-instru="' + value + '">' +
                '<td>' + value + '</td>' +
                '<td>' + commaSeparateNumber(amount) + '</td>' +
                '</tr>'
            );

            if (amount !== cache.recentStateViews[1]['Assetbalances'][address][value] && cache.recentStateViews[0]['Assetbalances'][address][value] !== undefined) {
                //flash it
                $(div + ' tbody[data-addr="' + address + '"] tr[data-instru="' + value + '"]').addClass('tablerow_hover');

                setTimeout(function () {
                    $(div + ' tbody[data-addr="' + address + '"] tr[data-instru="' + value + '"]').removeClass('tablerow_hover');
                    setTimeout(function () {
                        $(div + ' tbody[data-addr="' + address + '"] tr[data-instru="' + value + '"]').addClass('tablerow_hover');
                        setTimeout(function () {
                            $(div + ' tbody[data-addr="' + address + '"] tr[data-instru="' + value + '"]').removeClass('tablerow_hover');
                        }, 500);
                    }, 500);
                }, 500);
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page -> sac/mac
 * Update the balance in new contract div.
 */
function acContractNewContractBalanceUpdate(div){
    /**
     * addrInstru
     * {address: [
     *              "party",
     *              [instrument...]
 *              ]
     *
     * ...}
     *
     */
    try {
        var addrInstru = {};
        var addr1 = $(div + ' .partyA').attr('data-addr');
        var addr2 = $(div + ' .partyB').attr('data-addr');
        var addr3 = $(div + ' .partyC').attr('data-addr');
        var addr4 = $(div + ' .partyD').attr('data-addr');

        if(addr1 === '' || addr2 === '' || addr3 === '' || addr4 === '') {
            return;
        }

        addrInstru[addr1] = [];
        addrInstru[addr1][0] = 'partyA';
        addrInstru[addr1][1] = [];
        addrInstru[addr2] = [];
        addrInstru[addr2][0] = 'partyB';
        addrInstru[addr2][1] = [];
        addrInstru[addr3] = [];
        addrInstru[addr3][0] = 'partyC';
        addrInstru[addr3][1] = [];
        addrInstru[addr4] = [];
        addrInstru[addr4][0] = 'partyD';
        addrInstru[addr4][1] = [];



        for (var index = 1; index < 5; index++) {
            var instruA = $(div + ' .partyA' + ' .instrument-wrapper[data-instru-id = "' + index + '"] .instru').val();
            var instruB = $(div + ' .partyB' + ' .instrument-wrapper[data-instru-id = "' + index + '"] .instru').val();
            var instruC = $(div + ' .partyC' + ' .instrument-wrapper[data-instru-id = "' + index + '"] .instru').val();
            var instruD = $(div + ' .partyD' + ' .instrument-wrapper[data-instru-id = "' + index + '"] .instru').val();

            if (instruA === '' || instruB === '' || instruC === '' || instruD === '' || instruA === undefined || instruB === undefined || instruC === undefined || instruD === undefined) {
                $(div + ' .instrument-wrapper[data-instru-id = "' + index + '"] bal-wrapper').html('0');
                $(div + ' .instrument-wrapper[data-instru-id = "' + index + '"] bal-wrapper').attr('data-val',0);
                continue;
            }


            addrInstru[addr1][1].push(instruA);
            addrInstru[addr2][1].push(instruB);
            addrInstru[addr3][1].push(instruC);
            addrInstru[addr4][1].push(instruD);
        }

        for (var addr in addrInstru) {
            for (var i = 0; i < addrInstru[addr][1].length; i++) {
                var element = div + ' .' + addrInstru[addr][0] + ' .instrument-wrapper[data-instru-id = "' + (i+1) + '"]' + ' .bl-wrap';
                updateBalance(addr, addrInstru[addr][1][i], element);
            }
        }

    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Update balance
 * @param addr
 * @param instru
 * @param element
 */
function updateBalance(addr,instru, element){
    if (cache.recentStateViews.length > 1)
        var oldBal  = cache.recentStateViews[1]['Assetbalances'][addr][instru] === undefined ? 0 : cache.recentStateViews[1]['Assetbalances'][addr][instru];
    else
        var oldBal = 0;
    var newBal  = cache.recentStateViews[0]['Assetbalances'][addr][instru] === undefined ? 0 : cache.recentStateViews[0]['Assetbalances'][addr][instru];

    if (oldBal !== newBal){

        //flash it
        $(element).addClass('tablerow_hover');

        setTimeout(function () {
            $(element).removeClass('tablerow_hover');
            setTimeout(function () {
                $(element).addClass('tablerow_hover');
                setTimeout(function () {
                    $(element).removeClass('tablerow_hover');
                }, 500);
            }, 500);
        }, 500);
    }
    $(element).html(commaSeparateNumber(newBal));
    $(element).attr('data-val',newBal);
}

/**
 * Contract page
 * Render instrument select input box for the address.
 * @param address
 */
function contractRenderInstrumentSelect(div, address){
    try {
        $(div + ' select.instrument-select').empty();

        $(div + ' select.instrument-select').append(
            '<option value=""></option>'
        );

        $(div + ' .addr').html(address);

        //sort it.
        var instrumentsInAddr = [];
        var currencyArr = ['GBP','EUR', 'USD','JPY','AUD'];
        for (var key in cache.recentStateViews[0]['Assetbalances'][address]){
            // make part 1 only currency, party 2 only stock. demo purpose
            //var classid = key.split(/[|,:]/)[1];
            //if(($.inArray(classid, currencyArr) === -1 && div === '#dvp-contract-wrapper-a')
            //    || ($.inArray(classid, currencyArr) !== -1 && div === '#dvp-contract-wrapper-b')){
            //    continue;
            //}else{
                instrumentsInAddr.push(key);
            //}
        }

        instrumentsInAddr = instrumentsInAddr.sort();

        $.each(instrumentsInAddr, function (key, value) {

            $(div + ' select.instrument-select').append(
                '<option value="' + value + '">' + value + '</option>'
            );
        });

        $(div + ' select.instrument-select').selectpicker('refresh');

        if(div === '#dvp-contract-wrapper-a'){
            var instru = cache.default.currencyNamespace + '|' + cache.default.currencyClass;
        }else{
            var instru = cache.default.stockNamespace + '|' + cache.default.stockClass;//$(div + ' select.instrument-select option:nth-child(2)').attr('value'); // "Random" address
        }

        $(div + ' select.instrument-select.instrument1').selectpicker('val', instru);

    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 *  Handle search btn event.
 *  when user click on the search btn.
 *  search for address holding, tx.
 */
function explorerSearchBtnEventHandler(){
    $('#explorerSearchBtn').on('click tap touchend', function(e){
        try {
            e.preventDefault();

            var dialog = new BootstrapDialog({
                type: 'type-info',
                cssClass: 'explorerSearchDialog',
                size: BootstrapDialog.SIZE_WIDE,
                title: 'SETL SEARCH',
                message: $('<div></div>').load('../searchPopup.html'),
                closable: true,
                autodestroy: true
            });

            // Get the size of the screen, in order to set the modal max height.
            var maxHeight = $(window).height() - 60;
            dialog.realize();
            dialog.getModalContent().css('max-height', maxHeight + 'px');
            dialog.getModalBody().css('max-height', maxHeight - 78 + 'px');

            // Set the popup modal theme same with the theme currently using.
            var themeId = $('body').attr('id');
            dialog.getModalContent().attr("id", themeId);

            // Some styling:
            dialog.getModalBody().css('padding', 0);
            dialog.getModalHeader().hide();

            dialog.open();
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}

/**
 * Handle events within the popup dialog.
 */
function explorerSearchPopupEventsHandler(){
    explorerUserFinishTypingSearchHandler(); // Handle using typing the search txt.
    explorerResultEntryClickEventHandler(); // Handle user click on a result entry.
    addressHoldingOrderHandler(); // Handle sorting change.
}

/**
 * Detect when user finish typing their search text, and render result accordingly.
 */
function explorerUserFinishTypingSearchHandler(){
    try {
        // Detect finish typing event variables.
        var searchtxtTypingTimer; //flag to track if user finish typing search text.
        var doneTypingInterval = 500; //Interval for finish typing.
        var searchtxtEnterTimer = null; //flag to track if 'ENTER' was hit when typing search text.
        var nextEnterInterval = 500; //Interval for detect next 'ENTER'.

        // Call doneTyping function when finish waiting for "doneTypeingInterval", after key up.
        $('body').on('keyup', '#SearchPopupWrapper .main-search', function (item) {
            clearTimeout(searchtxtTypingTimer); //clear time refresh the key up event.

            // Check if the key pressed was 'ENTER'.
            var keycode = (item.keyCode ? item.keyCode : item.which);
            if (keycode == '13') {
                //if this is first 'ENTER' within a second, check the search text. Otherwise ignore it.
                if (searchtxtEnterTimer === null) {
                    searchtxtEnterTimer = setTimeout(function () {
                        searchtxtEnterTimer = null;
                    }, nextEnterInterval); //set timer to allow detection for next 'ENTER'.
                    explorerPopupShowResult();
                }
                return 1;
            }

            searchtxtTypingTimer = setTimeout(explorerPopupShowResult, doneTypingInterval);

        });

        // Clear the Interval function when key done.
        $('body').on('keydown', '#SearchPopupWrapper .main-search', function (item) {
            clearTimeout(searchtxtTypingTimer);
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Show the search results.
 * @returns {boolean}
 */
function explorerPopupShowResult()
{
    try {
        if (cache.recentStateViews.length == 0)
            return true;
        var searchTxt = $('#SearchPopupWrapper .main-search').val(); // The text we are searching for.
        cache.resultObject = [{}, [], {}, [],{},[]];

        // Empty the previous result.
        $('#SearchPopupWrapper .searchResultPreview').empty();
        $('#SearchPopupWrapper .searchResultList').empty();

        // Search for result. Search data cache for tx, and address holding, store the matched result.
        var numberOfMatched = 0;
        var regex = new RegExp('^' + searchTxt, "g");

        var namespace, asset, bal, namespaceAssetInfo;

        for (var key in cache.recentStateViews[0]['Assetbalances']) {
            if (numberOfMatched >= 15)
                break;

            var addrHoldings = cache.recentStateViews[0]['Assetbalances'][key];
            regex.lastIndex = 0;
            if (regex.test(key)) {
                //Store it.
                var matchedEntryArr = [];
                for (var namespaceAsset in addrHoldings) {
                    var matchedEntry = {};
                    namespaceAssetInfo = namespaceAsset.split(/[|,:]/);
                    namespace = namespaceAssetInfo[0];
                    asset = namespaceAssetInfo[1];
                    bal = addrHoldings[namespaceAsset];

                    matchedEntry['address'] = key;
                    matchedEntry['namespace'] = namespace;
                    matchedEntry['asset'] = asset;
                    matchedEntry['bal'] = bal;

                    matchedEntryArr.push(matchedEntry);

                }

                cache.resultObject[0][key] = matchedEntryArr;
                cache.resultObject[1].push(key);
                numberOfMatched++;
            }
        }

        for (var i = 0; i < cache.latestTransactions.length; i++) {
            var txInBlock = cache.latestTransactions[i]['Transaction'];
            for (var j = 0; j < txInBlock.length; j++) {
                if (numberOfMatched >= 30)
                    break;

                var txID = txInBlock[j][7];
                regex.lastIndex = 0;
                if (regex.test(txID)) {
                    //Store it.
                    cache.resultObject[2][txID] = txInBlock[j];
                    cache.resultObject[3].push(txID);

                    // Store the time: cache.resultObject[1][txID][10]
                    cache.resultObject[2][txID][10] = cache.latestTransactions[i]['Timestamp'];
                    numberOfMatched++;
                }
            }
        }

        for (var i = 0; i < cache.historicContracts[0].length; i++){
            var contractAddr = cache.historicContracts[0][i];
            regex.lastIndex = 0;
            if (regex.test(contractAddr)) {
                if (numberOfMatched >= 45)
                    break;
                //Store it.
                cache.resultObject[4][contractAddr] = cache.historicContracts[1][contractAddr];
                cache.resultObject[5].push(contractAddr);

                numberOfMatched++;
            }
        }

        if (numberOfMatched !== 0) {
            // Loop through the result, show the first one by default. And put each entry to the corresponding div.

            // Set the max height of searchResultList wrapper.
            var maxHeight = $(window).height() - 188;
            $('#SearchPopupWrapper .searchResultList').css('max-height', maxHeight + 'px');
            $('#SearchPopupWrapper .searchResultList').css('overflow', "scroll");

            // Sort the result:
            cache.resultObject[1] = cache.resultObject[1].sort();
            cache.resultObject[3] = cache.resultObject[3].sort();

            $('#SearchPopupWrapper .searchResultList').append('<div class="listview list-container"></div>');

            // Render Contract matched. cache.resultObject[5] hold the info.
            if (cache.resultObject[5].length > 0) {
                $('#SearchPopupWrapper .searchResultList .list-container').append(
                    '<div class="media">' +
                    '<div class="media-body"> <i class="fa fa-copyright"></i>&nbsp;&nbsp;Contracts:</div>' +
                    '</div>'
                );
                for (var i = 0; i < cache.resultObject[5].length; i++) {

                    $('#SearchPopupWrapper .searchResultList .list-container').append(
                        '<div class="media">' +
                        '<div class="media-body" data-entryid = "' + cache.resultObject[5][i] + '" data-type="contract"> ' +
                        '<i class="fa fa-copyright"></i>&nbsp;' + cache.resultObject[5][i] +
                        '</div>' +
                        '</div>'
                    );
                }
            }

            // render address hodling. cache.resultObject[1] hold the info.
            if (cache.resultObject[1].length > 0) {
                $('#SearchPopupWrapper .searchResultList .list-container').append(
                    '<div class="media">' +
                    '<div class="media-body"> <i class="fa fa-table"></i>&nbsp;&nbsp;Addresses:</div>' +
                    '</div>'
                );
                for (var i = 0; i < cache.resultObject[1].length; i++) {

                    $('#SearchPopupWrapper .searchResultList .list-container').append(
                        '<div class="media">' +
                        '<div class="media-body" data-entryid = "' + cache.resultObject[1][i] + '" data-type="addr"> ' +
                        '<i class="fa fa-table"></i>&nbsp;' + cache.resultObject[1][i] +
                        '</div>' +
                        '</div>'
                    );
                }
            }

            // Render TX matched. cache.resultObject[3] hold the info.
            if (cache.resultObject[3].length > 0) {
                $('#SearchPopupWrapper .searchResultList .list-container').append(
                    '<div class="media">' +
                    '<div class="media-body"> <i class="fa fa-text-width"></i>&nbsp;&nbsp;TXs:</div>' +
                    '</div>'
                );
                for (var i = 0; i < cache.resultObject[3].length; i++) {

                    $('#SearchPopupWrapper .searchResultList .list-container').append(
                        '<div class="media">' +
                        '<div class="media-body" data-entryid = "' + cache.resultObject[3][i] + '" data-type="tx"> ' +
                        '<i class="fa fa-text-width"></i>&nbsp;' + cache.resultObject[3][i] +
                        '</div>' +
                        '</div>'
                    );
                }
            }

            // Preview the first result.
            var type, id;
            type = $('#SearchPopupWrapper .searchResultList .list-container .media:nth-child(2) .media-body').attr('data-type');
            id = $('#SearchPopupWrapper .searchResultList .list-container  .media:nth-child(2) .media-body').attr('data-entryid');
            explorerRenderSearchPreview(type, id)
        }
        else if (numberOfMatched === 0) {

            $('#SearchPopupWrapper .searchResultList').append(
                '<div class="listview list-container">' +
                '<div class="media">' +
                '<div class="media-body"> ' +
                'NO RESULT IS FOUND' +
                '</div>' +
                '</div>' +
                '</div>'
            );
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function explorerResultEntryClickEventHandler()
{
    $('body').on('click tap touchend','#SearchPopupWrapper .searchResultList .list-container .media-body', function(e) {
        try {
            var id = ($(e.currentTarget).attr('data-entryid'));
            var type = ($(e.currentTarget).attr('data-type')); //can be address type or tx type

            if (id !== undefined && type !== undefined)
                explorerRenderSearchPreview(type, id);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Handle sort order for address holding
 */
function addressHoldingOrderHandler(){

    $('body').on('click tap touchend','#SearchPopupWrapper .searchResultPreview .table-responsive th', function(e){
        try {
            var sortName = $(this).attr('data-attr');

            //if the attribute is the same, toggle the sort order.
            if (sortName == cache.explorerHoldingSortName) {
                //toggle sort icon.
                if ($('#SearchPopupWrapper .searchResultPreview .table-responsive .sort-icon .fa').hasClass('fa-sort-amount-asc')) {
                    $('#SearchPopupWrapper .searchResultPreview .table-responsive .sort-icon .fa').removeClass('fa-sort-amount-asc');
                    $('#SearchPopupWrapper .searchResultPreview .table-responsive .sort-icon .fa').addClass('fa-sort-amount-desc');
                } else {
                    $('#SearchPopupWrapper .searchResultPreview .table-responsive .sort-icon .fa').removeClass('fa-sort-amount-desc');
                    $('#SearchPopupWrapper .searchResultPreview .table-responsive .sort-icon .fa').addClass('fa-sort-amount-asc');
                }

                //toggle sort method.
                cache.explorerHoldingSortOrder = (cache.explorerHoldingSortOrder == 1 ? -1 : 1);
            }
            //otherwise, change the sorting attribute, and reset the sort order.
            else {
                //remove all the sort icon in the table header.
                $('#SearchPopupWrapper .searchResultPreview .table-responsive .sort-icon').empty();
                //Add the new sort icon to the selected table header.
                $('[data-attr="' + sortName + '"] span').html('<i class="fa fa-sort-amount-asc"></i>');
                cache.explorerHoldingSortName = $(this).attr('data-attr');
                cache.explorerHoldingSortOrder = 1;
            }

            // Redraw the addr holding table.
            //get current result id.
            var id = $('#SearchPopupWrapper .searchResultPreview .table-responsive table').attr('data-id');

            //redraw it.
            explorerRenderSearchPreview("addr", id, true);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}

function explorerRenderSearchPreview(type, id, isRedraw)
{
    try {
        if (isRedraw === undefined) isRedraw = false;

        if (type === 'addr') { //if is address type

            if (!isRedraw) {
                // table header
                $('#SearchPopupWrapper .searchResultPreview').html(
                    '<div class="row p-10" id ="explorer-bl-wrapper">' +
                    '<div class="col-md-12">' +
                    '<div class="panel panel-default tile">' +
                    '<div class="panel-heading"><h3 class="panel-title">' + id + '</h3></div>' +
                    '<div id="tableHover8">' +
                    '<div class="table-responsive overflow" style="overflow: hidden;" tabindex="5003">' +
                    '<table class="table table-bordered table-hover" data-id="' + id + '">' +
                    '<thead>' +
                    '<tr>' +
                    '<th data-attr="namespace">Issuer <span class="sort-icon pull-right"><i class="fa fa-sort-amount-asc"></i></span></th>' +
                    '<th data-attr="asset">Security <span class="sort-icon pull-right"></i></span></th>' +
                    '<th data-attr="bal">Balance <span class="sort-icon pull-right"></span></th>' +
                    '</tr>' +
                    '</thead>' +
                    '<tbody>' +
                    '</tbody>' +
                    '</table>' +
                    '</div>' +
                    '</div>' +
                    '</div>' +
                    '</div>' +
                    '</div>' +
                    '</div>');


                // Set the max height of searchResultPreview wrapper.
                var maxHeight = $(window).height() - 288;
                $('#SearchPopupWrapper .searchResultPreview .table-responsive').css('max-height', maxHeight + 'px');
                $('#SearchPopupWrapper .searchResultPreview .table-responsive').css('overflow', "scroll");
            } else {
                $('#SearchPopupWrapper .searchResultPreview tbody').empty();
            }

            var addrHolding = cache.resultObject[0][id];

            //sort it:
            quicksort(addrHolding, 0, addrHolding.length - 1, cache.explorerHoldingSortName, cache.explorerHoldingSortOrder);

            for (var i = 0; i < addrHolding.length; i++) {
                // Unique id for the row.
                var entryindex = addrHolding[i]['namespace'] + addrHolding[i]['asset'];
                $('#SearchPopupWrapper .searchResultPreview tbody').append(
                    '<tr data-entryindex="' + entryindex + '">' +
                    '<td data-attr="namespace">' + addrHolding[i]['namespace'] + '</td>' +
                    '<td data-attr="asset">' + addrHolding[i]['asset'] + '</td>' +
                    '<td data-attr="balance">' + commaSeparateNumber(addrHolding[i]['bal']) + '</td>' +
                    '</tr>'
                );
            }

        }
        else if (type === 'tx') {
            var issuer = cache.resultObject[2][id][3];
            var instrument = cache.resultObject[2][id][4];
            var fromKey = cache.resultObject[2][id][1];
            var toKey = cache.resultObject[2][id][2];
            var protocol = cache.resultObject[2][id][9];
            var unit = cache.resultObject[2][id][5];
            var time = cache.resultObject[2][id][10];

            $('#SearchPopupWrapper .searchResultPreview').html(
                '<div class="p-10">' +
                '<div class="tile">' +
                '<h2 class="tile-title">Tx: ' + id + '</h2>' +
                '<div class="listview narrow">' +

                '<div class="media">' +
                '<div class="media-body">' +
                    '<div class="col-md-6">' +'<h6>ISSUER</h6>' + '</div>'+
                    '<div class="col-md-6"><h6 class="text-wrapper">' + issuer + '</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>INSTRUMENT</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">' + instrument + '</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>KEY FROM</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">' + fromKey + '</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>KEY TO</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">' + toKey + '</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>PROTOCOL</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">' + protocol + '</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>externalRef</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">externalRef</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>UNIT</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">' + commaSeparateNumber(unit) + '</h6></div>' +
                '</div>' +
                '</div>' +

                '<div class="media">' +
                '<div class="media-body">' +
                '<div class="col-md-6">' +'<h6>TIME</h6>' + '</div>'+
                '<div class="col-md-6"><h6 class="text-wrapper">' + unixTimeToUTC(time) + '</h6></div>' +
                '</div>' +
                '</div>' +

                '</div>' +
                '</div>' +
                '<div>'
            );

            // Set the max height of searchResultPreview wrapper.
            var maxHeight = $(window).height() - 288;
            $('#SearchPopupWrapper .searchResultPreview .tile').css('max-height', maxHeight + 'px');
            $('#SearchPopupWrapper .searchResultPreview .tile').css('overflow', "scroll");

        }
        else if(type === 'contract'){
            var contractAddr = cache.resultObject[4][id]["__address"];
            var status = cache.resultObject[4][id]["status"];
            var partiesLabelArr = [" ","PARTY A", "PARTY B", "PARTY C", "PARTY D"];
            var partiesIDLabelArr = [" ","explorer-partA", "explorer-partB", "explorer-partC", "explorer-partD"];
            var contractType = cache.resultObject[4][id]['protocol'];

            var rawHtml = $.parseHTML(JSONTree.create(cache.resultObject[4][id]));

                var renderingHtml =
                '<div class="p-10">' +
                '<div class="tile">' +
                '<div id="explorer-existing-contract-wrapper"> ' +
                    '<div class="block-area tile"> ' +
                        '<div class="panel panel-default tile"> ' +
                            '<div class="panel-heading wbb"><h6 class="panel-title">Contract:&nbsp;&nbsp;<span class="contract-addr">'+ contractAddr +'</span></h6></div> ';


                for(var i = 1; i < cache.resultObject[4][id]["parties"].length; i++) {
                    var partyLabel = (i === 3 && contractType === 'dvp') ? "PARTY C ( TAX )" : partiesLabelArr[i];
                    renderingHtml +=
                        '<div id="'+ partiesIDLabelArr[i] +'" class="p-b-5"> ' +
                            '<div class="listview narrow wbb"> ' +
                                '<div class="media"> ' +
                                    '<div class="media-body"> ' +
                                        '<h6>'+partyLabel+'</h6> ' +
                                    '</div> ' +
                                '</div>' +
                            '</div>' +
                            '<div class="table-responsive overflow wbb" style="overflow: hidden;" tabindex="5003">' +
                            '<table class="table table-bordered table-hover">' +
                                '<thead>' +
                                    '<tr>' +
                                        '<th>Instrument</th>' +
                                        '<th>Quantity</th>';

                    if(i === 3 && contractType === 'dvp') {
                        renderingHtml +=
                                        '<th></th>';

                    }
                    else{
                        renderingHtml +=
                                        '<th>Committed</th>';
                    }
                    renderingHtml +=
                                    '</tr>' +
                                '</thead>' +
                                '<tbody>';

                    for(var j = 0; j < cache.resultObject[4][id]["parties"][i][2].length; j++){
                        var instru = cache.resultObject[4][id]["parties"][i][2][j][1] + '|' + cache.resultObject[4][id]["parties"][i][2][j][2];
                        var qty = cache.resultObject[4][id]["parties"][i][2][j][3];
                        renderingHtml +=
                                    '<tr>' +
                                        '<td>' + instru + ' ( Paying )</td>' +
                                        '<td>' + commaSeparateNumber(qty) + '</td>';

                        if(i === 3 && contractType === 'dvp') {
                            renderingHtml +=
                                        '<td></td>';
                        }
                        else{
                            var partyCommitStatus = cache.resultObject[4][id]["parties"][i][1] != "" ? "checked" : "";
                            renderingHtml +=
                                        '<td>' +
                                            '<input type="checkbox" disabled ' + partyCommitStatus + '>' +
                                        '</td>';
                        }
                        renderingHtml +=
                                    '</tr>';
                    }

                    for(var j = 0; j < cache.resultObject[4][id]["parties"][i][3].length; j++){
                        var instru = cache.resultObject[4][id]["parties"][i][3][j][1] + '|' + cache.resultObject[4][id]["parties"][i][3][j][2];
                        var qty = cache.resultObject[4][id]["parties"][i][3][j][3];
                        renderingHtml +=
                                    '<tr>' +
                                        '<td>' + instru + ' ( Receiving )</td>' +
                                        '<td>' + commaSeparateNumber(qty) + '</td>';

                        if(i === 3 && contractType === 'dvp') {
                            renderingHtml += '<td></td>';
                        }else{
                            var partyCommitStatus = cache.resultObject[4][id]["parties"][i][1] != "" ? "checked" : "";
                            renderingHtml +=
                                        '<td>' +
                                            '<input type="checkbox" disabled ' + partyCommitStatus + '>' +
                                        '</td>';
                        }
                        renderingHtml +=
                                    '</tr>';
                    }

                    renderingHtml +=
                                '</tbody>' +
                            '</table>' +
                        '</div>'+
                        '</div>';
                }

            renderingHtml +=
                        '<div class="panel-footer">' +
                            '<h3 class="panel-title">STATUS: <span id="explorer-contract-status" class="p-5">'+status+'</span></h3>' +
                        '</div>' +
                        '</div>' +
                    '</div>' +

                    '<div class="block-area tile" id="explorer-contract-raw-view-wrapper">' +
                    '</div>' +
                '</div>' +
                '</div>' +
                '</div>';

            $('#SearchPopupWrapper .searchResultPreview').html(renderingHtml);

            $('#explorer-contract-raw-view-wrapper').html(rawHtml);

            //Checkbox + Radio skin
            $('input:checkbox:not([data-toggle="buttons"] input, .make-switch input), input:radio:not([data-toggle="buttons"] input)').iCheck({
                checkboxClass: 'icheckbox_minimal',
                radioClass: 'iradio_minimal',
                increaseArea: '20%' // optional
            });

            // Expand parties level.
            $('#explorer-contract-raw-view-wrapper .jstProperty.parties .jstExpand')[0].click();

            // Set the max height of searchResultPreview wrapper.
            var maxHeight = $(window).height() - 288;
            $('#SearchPopupWrapper .searchResultPreview .tile').css('max-height', maxHeight + 'px');
            $('#SearchPopupWrapper .searchResultPreview .tile').css('overflow', "scroll");
        }
        else {
            $('#SearchPopupWrapper .searchResultPreview').html(
                '<div class="media">' +
                '<div class="media-body"> ' +
                'Unable to preview unknown type.' +
                '</div>' +
                '</div>'
            );
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Check if the address holding balance has change, if yes, flash it.
 */
function checkAddressHoldingUpdate()
{
    try {
        // If the address holding table was rendered.
        if ($('#explorer-bl-wrapper').length) {
            var id = $('#SearchPopupWrapper .searchResultPreview .table-responsive table').attr('data-id');
            var newAddrHolding = cache.recentStateViews[0]['Assetbalances'][id];
            var oldAddrHolding = cache.resultObject[0][id];

            $.each(oldAddrHolding, function (i) {
                var key = oldAddrHolding[i]["namespace"] + '|' + oldAddrHolding[i]["asset"];
                var entryindex = oldAddrHolding[i]["namespace"] + oldAddrHolding[i]["asset"];
                // Get the new balance
                var newBal = newAddrHolding[key];
                var oldBal = oldAddrHolding[i]["bal"];

                if (newBal != oldBal) {
                    // Update the data.
                    cache.resultObject[0][id][i]["bal"] = newBal;

                    //flash it
                    $('#SearchPopupWrapper .searchResultPreview tbody tr[data-entryindex = "' + entryindex + '"]').addClass('tablerow_hover');

                    setTimeout(function () {
                        $('#SearchPopupWrapper .searchResultPreview tbody tr[data-entryindex = "' + entryindex + '"]').removeClass('tablerow_hover');
                        setTimeout(function () {
                            $('#SearchPopupWrapper .searchResultPreview tbody tr[data-entryindex = "' + entryindex + '"]').addClass('tablerow_hover');
                            setTimeout(function () {
                                $('#SearchPopupWrapper .searchResultPreview tbody tr[data-entryindex = "' + entryindex + '"]').removeClass('tablerow_hover');
                            }, 500);
                        }, 500);
                    }, 500);

                    //Update the rendered table.
                    $('#SearchPopupWrapper .searchResultPreview tbody tr[data-entryindex = "' + entryindex + '"] td[data-attr = "balance"]').html(commaSeparateNumber(newBal));

                }
            });

        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Payment system scenario setting handler.
 */
function paymentSystemSettingHandler(){
    $('#paymentSystemSettingWrapper .make-switch label').on('click tap touchend', function(e){
        try {
            setTimeout(function () {
                //Get the state and scenario.
                var scenarioIDName = $(e.target).closest('.make-switch').attr('data-id');
                var scenarioID = $.inArray(scenarioIDName, cache.paymentSystemsArr) + 1;
                var scenarioState = $('[data-id="' + scenarioIDName + '"]' + ' .switch-animate').hasClass('switch-on') ? 1 : 0;

                //send corresponding command to socket server.
                try {
                    sendCommandToChangeScenario(scenarioID, parseInt(scenarioState));

                }
                catch (e) {
                    if (!cache.prop) console.log(e.message);
                }
            }, 0)
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}

/**
 * Send Transaction.
 */
function sendCorpBtnLisener(){
    $('#sendCorpBtn').on('click tap touchend', function(e){
        try {
            e.preventDefault();

            var namespace = $('#corpactionTab .select.issuer-select').val();
            var classid = $('#corpactionTab .select.security-select').val();
            var ratio = $('#split-ratio').val();
            var currency = $('#dividend-currency').val();
            var amount = $('#dividend-amount').val();
            var action = $('.accordion-toggle.active').attr('for');

            if (namespace == '' || classid == '' || (action == 'split' && ratio == '') || (action == 'dividend' && (amount == '' || currency == ''))) {
                BootstrapDialog.alert({
                    title: 'Required fields needed',
                    message: 'Required fields missing, please try again!'
                });
                return;
            }

            if (action == 'split') {
                BootstrapDialog.show({
                    title: 'Commiting Corp Action...',
                    message: '<h5>Do you want to make the following Corp Action:</h5><br />' +
                    'Action: Stock Split<br />' +
                    'Issuer:&nbsp;' + namespace + '<br />' +
                    'Security: &nbsp;' + classid + '<br />' +
                    'Ratio: &nbsp;( 1 : ' + ratio + ' )',

                    buttons: [{
                        label: 'Yes',
                        action: function (dialogItself) {
                            dialogItself.close();

                            $.ajax({
                                url: 'php/corp_action.php',
                                context: document.body,
                                method: "POST",
                                data: {
                                    namespace: namespace,
                                    classid: classid,
                                    ratio: ratio,
                                    action: 'split',
                                    url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
                                },
                                dataType: 'json'
                            }).done(function (response) {
                                //console.log(response)
                            })
                        }
                    }, {
                        label: 'No',
                        action: function (dialogItself) {
                            dialogItself.close();
                            return;
                        }
                    }]
                });
            } else {
                BootstrapDialog.show({
                    title: 'Commiting Corp Action...',
                    message: '<h5>Do you want to make the following Corp Action:</h5><br />' +
                    'Action: Dividend<br />' +
                    'Issuer:&nbsp;' + namespace + '<br />' +
                    'Security: &nbsp;' + classid + '<br />' +
                    'Dividend Issuer &nbsp;' + cache.dividendInfo[currency] + '<br />' +
                    'Currency: &nbsp;' + currency + '<br />' +
                    'Amount: &nbsp;' + commaSeparateNumber(amount),

                    buttons: [{
                        label: 'Yes',
                        action: function (dialogItself) {
                            dialogItself.close();

                            $.ajax({
                                url: 'php/corp_action.php',
                                context: document.body,
                                method: "POST",
                                data: {
                                    namespace: namespace,
                                    classid: classid,
                                    dividend_issuer: cache.dividendInfo[currency],
                                    currency: currency,
                                    amount: amount,
                                    action: 'dividend',
                                    url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
                                },
                                dataType: 'json'
                            }).done(function (response) {
                            })
                        }
                    }, {
                        label: 'No',
                        action: function (dialogItself) {
                            dialogItself.close();
                            return;
                        }
                    }]
                });
            }
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}


/**
 * Remove an element from a array.
 * @param arr
 * @param valOfElementToRemove
 */
function removeSpecificElementFromArr(arr, valOfElementToRemove){
    arr = jQuery.grep(arr, function(value) {
        return value[0] != valOfElementToRemove;
    });
    return arr;
}

/**
 * Pad string with desired length.
 * @param str
 * @param max
 * @returns {*}
 */
function pad (str, max) {
    str = str.toString();
    return str.length < max ? pad("0" + str, max) : str;
}

/**
 * Get asset list from assetBalances, and store the assetBalances with asset as object property.
 * @param assetBalances
 * @returns {Array}
 */
function getCurrentStateViewAssetList(assetBalances){
    try {
        var newAssetBalances = {};

        $.each(assetBalances, function (address, assetBalanceInAddress) {
            $.each(assetBalanceInAddress, function (namespaceAndAsset, balance) {
                // Ignore SYS|STAKE
                if(namespaceAndAsset != "SYS|STAKE") {
                    var asset = namespaceAndAsset.split(/[|,:]/)[1];
                    var namespace = namespaceAndAsset.split(/[|,:]/)[0];

                    var balanceRow = {};

                    balanceRow['asset'] = asset;
                    balanceRow['namespace'] = namespace;
                    balanceRow['amount'] = balance;
                    balanceRow['address'] = address;

                    if (newAssetBalances[asset] === undefined)
                        newAssetBalances[asset] = [];

                    newAssetBalances[asset].push(balanceRow);
                }
            });
        });

        return newAssetBalances;
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Get the key of a object, and return them as an array.
 * @param obj
 * @returns {Array}
 */
function getKeyOfObject(obj){
    var a = [];
    $.each(obj, function(k){ a.push(k) });
    return a;
}

function quicksort(sortArray, left, right, sortname, multiplier)
{
    /*
     --Code from Nick.

     Generic QuickSort algorithm, sourced from the internet somewhere.
     Designed to sort a section of an array, delimited by 'left' and 'right'.

     sortArray  : Array to Sort
     left       : Start Index
     right      : End Index
     sortname   : Property name on which to sort.
     multiplier : 1 - Sort Ascending, -1 - sort Descending.
     */

    if (!$.isArray(sortname)) sortname = [sortname];
    if (!$.isArray(multiplier)) multiplier = [multiplier];

    if (left < right)
    {

        var pivot = sortArray[(left + right) >> 1];
        var left_new = left, right_new = right;

        do {
            while (compare(sortArray[left_new], pivot, sortname, multiplier, 0) < 0)
            {
                left_new++;
            }

            while (compare(pivot, sortArray[right_new], sortname, multiplier, 0) < 0)
            {
                right_new--;
            }

            if (left_new <= right_new)
                swap(sortArray, left_new++, right_new--);
        } while (left_new <= right_new);

        quicksort(sortArray, left, right_new, sortname, multiplier);
        quicksort(sortArray, left_new, right, sortname, multiplier);
    }

    function swap(sortArray, i, j)
    {
        var t = sortArray[i];
        sortArray[i] = sortArray[j];
        sortArray[j] = t;
    }

    function compare(a, b, sortname, multiplier, depth)
    {
        /*
         a          : Object 1
         b          : Object 2
         sortname   : Array of Property name on which to sort.
         multiplier : Array to match [sortname] 1 - Sort Ascending, -1 - sort Descending.
         */
        var fieldName = sortname[depth];
        var thisMultiplier = multiplier[depth];

        if(fieldName == ""){
            if (nz(a, '') < nz(b, '')) return (-1 * thisMultiplier);
            if (nz(a, '') > nz(b, '')) return (thisMultiplier);
        }else{
            if (nz(a[fieldName], '') < nz(b[fieldName], '')) return (-1 * thisMultiplier);
            if (nz(a[fieldName], '') > nz(b[fieldName], '')) return (thisMultiplier);
        }


        if ((depth + 1) < sortname.length)
        {
            return compare(a, b, sortname, multiplier, (depth + 1));
        }
        else
        {
            return 0;
        }
    }
}

/**
 * redraw bar chart
 * @param scenarioID
 * @param value
 */
function redrawBarChart(){
    //fix bar chart resize bug
    //var width = $('#dash_bar-chart canvas').width();
    //$('#dash_bar-chart').css('width',width);
    try {
        cache.dashBarsChart.setData(cache.barChartData);
        cache.dashBarsChart.setupGrid();
        cache.dashBarsChart.draw();
    }
    catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

    //fix bar chart resize bug
    //$('#dash_bar-chart').css('width', 'auto');
}

/**
 * Restart the block timer, for block time taken.
 */
function restartBlockTimer(){
    try {
        cache.newBlockTimeTaken = 0;
        clearInterval(cache.newBlockTimeTakenTimer);
        cache.newBlockTimeTakenTimer = setInterval(function () {
            updateTimeTakenLastBlock(cache.newBlockTimeTaken);
            cache.newBlockTimeTaken += 0.1;
        }, 100);
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function flashAMakerInMap(markerID){
    try {
        var mapObject = $('#world-map').vectorMap('get', 'mapObject');

        mapObject.markers[markerID].element.setStyle('fill', '#428bca');
        setTimeout(function () {
            mapObject.markers[markerID].element.setStyle('fill', '#e80000');
        }, 1000);
    }catch(err){
        //if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function renderDataAccordionView(Data){
    try {
        $('#diagnosis-data-wrapper .row').empty();

        $.each(Data, function (key, value) {
            $('#diagnosis-data-wrapper .row').append(
                '<div class="col-md-6 block">' +
                '<p class="p-5">' + key + '</p>' +
                '<div class="table-responsive overflow" id="diagnosis_' + key.toLowerCase() + '" style="overflow: hidden;" tabindex="5003">' +
                '<table class="table table-bordered table-hover tile">' +
                '<thead>' +
                '<tr>' +
                '<th data-attr="key">Key</td>' +
                '<th data-attr="value">Value</td>' +
                '</tr>' +
                '</thead>' +
                '<tbody>');
            $.each(value, function (key_inner, value_inner) {
                $('#diagnosis-tab-pane #diagnosis_' + key.toLowerCase() + ' tbody').append(
                    '<tr>' +
                    '<td>' + key_inner + '</td>' +
                    '<td>' + value_inner + '</td>' +
                    '</tr>'
                );
            });

            $('#diagnosis-data-wrapper .row').append(
                '</tbody>' +
                '</table>' +
                '</div>' +
                '</div>');
        });
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}


function areaLogSettingsStateHandler(){
    $('#log-tab-pane .make-switch label').on('click tap touchend', function(e){
        try {
            setTimeout(function () {
                //Get the state and scenario.
                var areaIDName = $(e.target).closest('.make-switch').attr('data-id');
                var areaID = cache.logAreaAndLevelMap[areaIDName];
                var areaState = $('[data-id="' + areaIDName + '"]' + ' .switch-animate').hasClass('switch-on') ? 1 : 0;

                //send corresponding command to socket server.
                try {
                    sendCommandToChangeLogArea(areaID, areaState);
                }
                catch (e) {
                    if (!cache.prop) console.log(e.lineNumber + ': ' + e.message);
                }
            }, 0)
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('#log-level-selector').on('change', function(){
        sendCommandToChangeLogLevel(cache.logAreaAndLevelMap[$('#log-level-selector').val()]);
    })
}

function diagnosisMonitorHandler(){
    $('#receiveStatusUpdateSwitch label').on('click tap touchend', function(e){
        try {
            setTimeout(function () {
                var updateState = $('#receiveStatusUpdateSwitch .switch-animate').hasClass('switch-on') ? 1 : 0;

                try {
                    sendCommandToToggleStatusUpdate(updateState);
                }
                catch (e) {
                }
            }, 0);
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Handle event for TX diagnosis btns
 */
function diagnosisTxHandler(){
    $('#txdiagnosis-tab-pane button').on('click', function(e){
        try {
            var filename = $('#dumpedTxListWrapper').val() != undefined ? $('#dumpedTxListWrapper').val()[0] : '';
            var isLoadAll = $('#isloadAllTXs').is(':checked');
            var isPopulate = $('#isPopulate').is(':checked');

            sendCommandToDiagnosisTX(0, filename, isLoadAll, isPopulate);
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('#dumpedTxListWrapper ').on('click tap touchend', 'button', function(){
        try {
            var action = $(this).attr('data-action');
            var filename = $(this).closest('.media').attr('data-id');
            var isLoadAll = $('#isloadAllTXs').is(':checked');
            var isPopulate = $('#isPopulate').is(':checked');

            if($(this).attr('data-action') == 'load')
                sendCommandToDiagnosisTX(1, filename, isLoadAll, isPopulate);
            else
                sendCommandToDumpedTxList(3,filename);
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    })
}

function overDriverSettingHandler(){
    try {
        $('#overdriveSpiner').spinedit('setMaximum', 80.0);
        $('#overdriveSpiner').spinedit('setMinimum', 1.0);
        $('#overdriveSpiner').spinedit('setStep', 0.1);
        $('#overdriveSpiner').spinedit('setNumberOfDecimals', 1);

        var readOverDriverTimer;
        $('#overdriveSpiner').on('change', function () {
            clearTimeout(readOverDriverTimer);
            readOverDriverTimer = setTimeout(function () {
                var multiply = $('#overdriveSpiner').val();
                var state = $('#overdriveSwitch .switch-animate').hasClass('switch-on') ? 1 : 0;

                sendCommandToChangeOverDriveSetting(state, multiply);
            }, 500);

        });

        $('#overdriveSwitch label').on('click tap touchend', function (e) {
            setTimeout(function () {
                var state = $('#overdriveSwitch .switch-animate').hasClass('switch-on') ? 1 : 0;
                var multiply = $('#overdriveSpiner').val() != 0 ? $('#overdriveSpiner').val() : 1;

                sendCommandToChangeOverDriveSetting(state, multiply);
            }, 0);
        })
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Diagnosis -> Sv Dump
 */
function diagnosisSvDumpHandler(){
    $('#stateviewdiagnosis-tab-pane button.dump-sv').on('click', function(e){
        try {
            var dataNameArr = {};
            $.each($('#stateviewdiagnosis-tab-pane input'), function(i,item){
                if($(item).is(':checked')){
                    dataNameArr[$(item).attr('data-name')] = "";
                }
            });

            sendCommandToDumpSv(0, dataNameArr);
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('#dumpedSvListWrapper ').on('click tap touchend', 'button', function(){
        try {
            var action = $(this).attr('data-action');
            console.log(action);
            var filename = $(this).closest('.media').attr('data-id');

            if($(this).attr('data-action') == 'delete')
                sendCommandToDumpedSvList(3,filename,'');
            else {
                var dataNameArr = [];
                $.each($('#stateviewdiagnosis-tab-pane input'), function(i,item){
                    if($(item).is(':checked')){
                        dataNameArr.push($(item).attr('data-name'));
                    }
                });
                sendCommandToDumpedSvList(1, filename, dataNameArr);
            }
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    })
}

//Twitter Parsers
function twitterParseDate(str) {
    var v=str.split(' ');
    var newDate = new Date(Date.parse(v[1]+" "+v[2]+", "+v[5]+" "+v[3]+" UTC"));
    return newDate.getUTCDate() + '/' + (newDate.getUTCMonth()+1) + '/' + newDate.getUTCFullYear() + ' ' + unixTimeToUTC(newDate);
}


// Get twitter for messages
function getTwitterForMessage(){
    try {
        $.ajax({
            url: '/getTwitterFeed.php',
            context: document.body,
            dataType: 'json'
        }).done(function (json) {
            for (var i = 0; i < json.length; i++) {
                var tweet = linkify_entities(json[i]);
                $('#twitter-wrapper').append(
                    '<div class="media">' +
                    '<div class="pull-left">' +
                    '<img width="40" src="' + json[i]['user']['profile_image_url'] + '" alt="">' +
                    '</div>' +
                    '<div class="media-body">' +
                    '<small class="text-muted">' + json[i]['user']['name'] + ' - On ' + twitterParseDate(json[i]['created_at']) + '</small><br>' +
                    '<span class="t-overflow">' + tweet + '<a class="m-l-10 text-muted" href="https://twitter.com/bankofengland" target="_blank">Read More</a></span>' +
                    '</div>' +
                    '</div>');

            }
        })
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

//for check login state.
function longPollingCheckLoginState(){
    try {
        setInterval(function () {
            $.ajax({url: "/php/clef_isLogin.php"}).done(function (isLogin) {

                if (isLogin === '0') {
                    //log user out.
                    $.ajax({url: "/php/dummyAuthen.php", data: {'logout': 1}}).done(function () {
                        window.location.replace("login.php");
                    });
                }
            })

        }, 60000);
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}


/**
 * Get indexed state view.
 * @param balances
 */
function getStateViewForWallet(balances){
    try {
        cache.indexedStateView = {};
        $.each(balances, function (asset, balance) {
            for (var i = 0; i < balance.length; i++) {
                if (cache.indexedStateView[balance[i]['namespace']] == undefined)
                    cache.indexedStateView[balance[i]['namespace']] = {};
                if (cache.indexedStateView[balance[i]['namespace']][balance[i]['asset']] == undefined)
                    cache.indexedStateView[balance[i]['namespace']][balance[i]['asset']] = {};
                if (cache.indexedStateView[balance[i]['namespace']][balance[i]['asset']][balance[i]['address']] == undefined)
                    cache.indexedStateView[balance[i]['namespace']][balance[i]['asset']][balance[i]['address']] = balance[i]['amount'];
            }
        });
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Update indexed state view.
 * @param balances
 */
function updateSVForWallet(balances){
    try {
        $.each(balances, function (asset, balance) {
            for (var i = 0; i < balance.length; i++) {
                cache.indexedStateView[balance[i]['namespace']][balance[i]['asset']][balance[i]['address']] = balance[i]['amount'];
            }
        });
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Handle when dropdown was selected -- Wallet page
 */
function dropdownSelectUpdateListener(){
    $('#wallet-page-transaction-wrapper select').on('change', function(e){
        try {
            if ($(this).hasClass('issuer-select')) {
                //Get the recently select security.
                var selectedIssuer = $(this).val();
                var tabPaneID = '#' + $(this).closest('.tab-pane').attr('id');

                updateSecurityDropDown(tabPaneID, selectedIssuer);
                //select a default security (first one on the list):
                var defaultSecurity = $(tabPaneID + ' select.security-select option')[1].getAttribute('value');
                $(tabPaneID + ' select.security-select').selectpicker('val', defaultSecurity);

                if (tabPaneID == '#receivingTab') {
                    updateAddressDropDown(tabPaneID + ' select.toAddress-select', selectedIssuer, defaultSecurity);

                    //select a default address (first one on the list):
                    var defaultAddress = $(tabPaneID + ' select.toAddress-select option')[1].getAttribute('value');
                    $(tabPaneID + ' select.toAddress-select').selectpicker('val', defaultAddress);
                }
                //if(tabPaneID  == '#sendingTab') {
                //    updateAddressDropDown((tabPaneID + ' select.fromAddress-select' ), selectedIssuer, '');
                //}
            }

            if ($(this).hasClass('security-select')) {
                //Get the recently select security.
                var selectedSecurity = $(this).val();
                var tabPaneID = '#' + $(this).closest('.tab-pane').attr('id');
                var selectedIssuer = $((tabPaneID ) + ' select.issuer-select').val();

                updateAddressDropDown(tabPaneID + ' select.toAddress-select', selectedIssuer, selectedSecurity);

                //if(tabPaneID  == '#sendingTab')
                //    updateAddressDropDown((tabPaneID+' select.fromAddress-select' ),selectedIssuer, selectedSecurity);
                if (tabPaneID == '#corpactionTab') {
                    CorpUpdateAddressBalance('#corpactionTab', '#corp-ad-bal-wrapper tbody', selectedIssuer, selectedSecurity, false);
                }
                else {
                    //select a default address (first one on the list):
                    var defaultAddress = $(tabPaneID + ' select.toAddress-select option')[1].getAttribute('value');
                    $(tabPaneID + ' select.toAddress-select').selectpicker('val', defaultAddress);
                }
            }

            if ($(this).hasClass('toAddress-select') || $(this).hasClass('fromAddress-select')) {
                var tabPaneID = '#' + $(this).closest('.tab-pane').attr('id');

                var selectedAddress = $(this).val();
                var selectedIssuer = $((tabPaneID ) + ' select.issuer-select').val();
                var selectedSecurity = $((tabPaneID ) + ' select.security-select').val();

                if (tabPaneID == '#receivingTab')
                    updateAddressBalance(tabPaneID, '#receiving-ad-bal-wrapper tbody', selectedIssuer, selectedSecurity, selectedAddress, false);
                //else {
                //    if($(this).hasClass('fromAddress-select'))
                //        updateAddressBalance(tabPaneID, '#sending-ad-bal-wrapper tbody', selectedIssuer, selectedSecurity, selectedAddress, false);
                //}
            }
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }

    });
}

/**
 * Generate default tx once got asset balance detail.
 */
function generateDefaultTx(){
    try {
        fillUpDairyTx("coffee");
    }catch(e){
        if(cache.prod == false) console.log(e.lineNumber+ ': ' .message);
    }
}

/**
 * Generate default corp action option-: Issuer, security.
 */
function generateDefaultCorpOptions(){
    try {
        if($('#corpactionTab select.issuer-select').selectpicker('val') != cache.default.stockNamespace)
            $('#corpactionTab select.issuer-select').selectpicker('val', cache.default.stockNamespace);
        if($('#corpactionTab select.security-select').selectpicker('val') != cache.default.stockClass)
            $('#corpactionTab select.security-select').selectpicker('val', cache.default.stockClass);

        // Set default currency.
        $('#dividend-currency').selectpicker('val',cache.default.currencyClass);

    }catch(e){
        if(cache.prod == false) console.log(e.lineNumber+ ': ' + e.message);
    }
}

function corpDividendCurrencySelectHandler(){
    $('#dividend-currency').on('change', function(e){
        CorpUpdateAddressBalance('#corpactionTab','#corp-ad-bal-wrapper tbody', '','', true);
    });
}

/**
 * Handle selection on dairy TX.
 */
function dairyTxSelectHandler(){
    $('#diary-tx-wrapper .diary-tx-btn').on('click tap touchend', function(e){
        e.preventDefault();
        var item = $(this).attr('data-choice');
        fillUpDairyTx(item);
    });
}

/**
 * fill the tx form for dairy tx.
 * @param: item. - type of dairy tx.
 */
function fillUpDairyTx(item){
    /**
     * Set default issuer and security.
     * create new toAddress
     * append the new address to the list
     * select the newly created address
     * setup the price base of the item was selected.
     * Generate QR.
     */
    try {
        var keyPair, newAddr, price;

        if ($('#receivingTab select.issuer-select').selectpicker('val') != cache.default.currencyNamespace)
            $('#receivingTab select.issuer-select').selectpicker('val', cache.default.currencyNamespace);
        if ($('#receivingTab select.security-select').selectpicker('val') != cache.default.currencyClass)
            $('#receivingTab select.security-select').selectpicker('val', cache.default.currencyClass);

        keyPair = bitcoin.ECPair.makeRandom();
        newAddr = keyPair.getAddress();

        $('#receivingTab select.toAddress-select').append('<option value="' + newAddr + '">' + newAddr + '</option>');
        $('#receivingTab select.toAddress-select').selectpicker('refresh');

        $('#receivingTab select.toAddress-select').selectpicker('val', newAddr);

        switch (item) {
            case 'coffee':
                price = 3;
                break;
            case 'brunch':
                price = 6;
                break;
            case 'newspaper':
                price = 1;
                break;
            default:
                break;
        }
        $('#receivingTab #amount').val(price);

        $('#generateQR').trigger('click');
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page -> dvp
 * Handle user address selection
 */
function dvpContractAddressSelectHandler(){
    $('#dvp-smartContractTab select.address-select').on('change', function(e){
        try {
            var div = $(e.target).hasClass('partyA') ? '#dvp-parta-bl-wrapper' : '#dvp-partb-bl-wrapper';
            contractUpdateBalance(div,"","dvp");

            var address = $(e.target).val();
            var instrumentSelectDiv = div === '#dvp-parta-bl-wrapper' ? '#dvp-contract-wrapper-a' : '#dvp-contract-wrapper-b';
            contractRenderInstrumentSelect(instrumentSelectDiv, address);
            dvpPickParty3Address();
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Contract page -> single asset contract
 * Handle user address selection
 */
function acContractAddressSelectHandler(){
    try{
        $('.ac-wrapper .address-select-wrapper .address-select').on('change', function (e) {
            if (cache.acContractInstruemntDefaultRender === false) {
                var contractMainWrapper = '#' + $(this).closest('.ac-wrapper').attr('id');

                var addr1 = $(contractMainWrapper + ' .address-select.partyA').val();
                var addr2 = $(contractMainWrapper + ' .address-select.partyB').val();
                var addr3 = $(contractMainWrapper + ' .address-select.partyC').val();
                var addr4 = $(contractMainWrapper + ' .address-select.partyD').val();
                var thisVal = $(this).selectpicker('val');
                var partyID = $(this).attr('data-party');

                if (addr1 === addr2 || addr1 === addr3 || addr1 === addr4 || addr2 === addr3 || addr2 === addr4 || addr3 === addr4 && thisVal != ''){
                    $(this).selectpicker("val", "");
                    // Select the corresponding address in the balance table.
                    $(partyID + ' input').val(thisVal);
                    BootstrapDialog.alert({
                        title: 'Party Address invalid',
                        message: 'Duplicate party address!'
                    });
                    return;
                }

                // Render instrument.
                var type =  $(this).attr('data-type');

                if(type === 'sac') {
                    acContractRenderInstrumentList(contractMainWrapper, '#sac-new-contract-wrapper', '', '.instrument-select', 2);

                    // Select the corresponding address in the balance table.
                    $('#single-clearingTab ' + partyID + ' input').val(thisVal);

                    acContractNewContractBalanceUpdate('#sac-new-contract-wrapper');
                }
                else{
                    // Prevent instrument select handler detect the change and update multiple time, cause update balance and generate default amounts for contract, unwanted.
                    cache.waitingForAllInstrumentsReady = true;
                    acContractRenderInstrumentList(contractMainWrapper,'#mac-new-contract-wrapper', ".instrument-wrapper[data-instru-id='1']", '.instrument-select',2);
                    acContractRenderInstrumentList(contractMainWrapper,'#mac-new-contract-wrapper', ".instrument-wrapper[data-instru-id='2']", '.instrument-select', 3);
                    cache.waitingForAllInstrumentsReady = false;
                    acContractRenderInstrumentList(contractMainWrapper,'#mac-new-contract-wrapper', ".instrument-wrapper[data-instru-id='3']", '.instrument-select', 5);

                    // Select the corresponding address in the balance table.
                    $('#multi-clearingTab ' + partyID + ' input').val(thisVal);

                    acContractNewContractBalanceUpdate('#mac-new-contract-wrapper');
                }



            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page -> single asset contract/ mac
 * Handle user instrument selection
 */
function acContractInstrumentSelectHandler(){
    try{
        $('.ac-wrapper .instrument-select').on('change', function (e) {
            // Selected instrument.
            var selectedInstru = $(this).val();
            var instru_id = $(this).closest(".instrument-wrapper").attr('data-instru-id');

            var newContractWrapper = '#' + $(this).closest(".ac-new-contract-wrapper").attr("id");

            // Set the rest of the instruments
            $(newContractWrapper + ' .partyB .instrument-wrapper[data-instru-id = "' + instru_id + '"] .instru').val(selectedInstru);
            $(newContractWrapper + ' .partyC .instrument-wrapper[data-instru-id = "' + instru_id + '"] .instru').val(selectedInstru);
            $(newContractWrapper + ' .partyD .instrument-wrapper[data-instru-id = "' + instru_id + '"] .instru').val(selectedInstru);

            if (selectedInstru !== "" && selectedInstru !== undefined && selectedInstru !== null && cache.waitingForAllInstrumentsReady === false) {
                // Smart contract page -> sac -> balance update.
                acContractNewContractBalanceUpdate(newContractWrapper);

                // Smart contract page -> sac -> set default quantity:
                acContractDefaultQtyForContract(newContractWrapper);
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page -> sac/mac
 * Make sure either receive or pay can input. unless both of them empty.
 */
function acContractQtyInputsHandler(){
    $('.ac-wrapper input').on('keyup', function(e){
        var type = $(this).attr('data-type'), party = $(this).attr('data-party'), counter_type = type === 'pay' ? 'receive' : 'pay';
        var parentDiv = $(this).closest('.ac-wrapper').attr('id');

        if ($(this).val() !== ''){
            $('#' + parentDiv + ' ' + party + ' input[data-type="'+ counter_type +'"]' ).attr('disabled','disabled');
        }else{
            $('#' + parentDiv + ' ' + party + ' input[data-type="'+ counter_type +'"]' ).removeAttr('disabled');
        }
    });
}

/**
 * Contract page
 * Handle events in the contract page.
 */
function dvpContractEventHandler() {
    // Listen to create contract btn
    $('#dvp-new-contract-wrapper button[type="submit"]').on('click tap touchend', function (e) {
        e.preventDefault();

        try {
            // Send create contract request to server, Fill exiting contract.
            var party1Instrument1 = $('#dvp-contract-wrapper-a .select.instrument-select.instrument1').val();
            var party1Amount1 = commasSeparatedToInt($('#dvp-contract-wrapper-a .qty1').val());
            var party1Instrument2 = $('#dvp-contract-wrapper-a .select.instrument-select.instrument2').val() === undefined ? '' : $('#dvp-contract-wrapper-a .select.instrument-select.instrument2').val();
            var party1Amount2 = commasSeparatedToInt($('#dvp-contract-wrapper-a .qty2').val()) === undefined ? '' : commasSeparatedToInt($('#dvp-contract-wrapper-a .qty2').val());
            var party1Instrument3 = $('#dvp-contract-wrapper-a .select.instrument-select.instrument3').val() === undefined ? '' : $('#dvp-contract-wrapper-a .select.instrument-select.instrument3').val();
            var party1Amount3 = commasSeparatedToInt($('#dvp-contract-wrapper-a .qty3').val()) === undefined ? '' : commasSeparatedToInt($('#dvp-contract-wrapper-a .qty3').val());

            var party2Instrument1 = $('#dvp-contract-wrapper-b .select.instrument-select.instrument1').val();
            var party2Amount1 = commasSeparatedToInt($('#dvp-contract-wrapper-b .qty1').val());
            var party2Instrument2 = $('#dvp-contract-wrapper-b .select.instrument-select.instrument2').val() === undefined ? '' : $('#dvp-contract-wrapper-b .select.instrument-select.instrument2').val();
            var party2Amount2 = commasSeparatedToInt($('#dvp-contract-wrapper-b .qty2').val()) === undefined ? '' : commasSeparatedToInt($('#dvp-contract-wrapper-b .qty2').val());
            var party2Instrument3 = $('#dvp-contract-wrapper-b .select.instrument-select.instrument3').val() === undefined ? '' : $('#dvp-contract-wrapper-b .select.instrument-select.instrument3').val();
            var party2Amount3 = commasSeparatedToInt($('#dvp-contract-wrapper-b .qty3').val()) === undefined ? '' : commasSeparatedToInt($('#dvp-contract-wrapper-b .qty3').val());

            var party3Instrument1 = $('#dvp-contract-wrapper-c .select.instrument-select.instrument1').val();
            var party3Amount1 = commasSeparatedToInt($('#dvp-contract-wrapper-c .qty1').val());

            if (party1Instrument1 === '' || party1Amount1 === '' || party2Instrument1 === '' || party2Amount1 === ''
                || party1Instrument2 !== '' && party1Amount2 === ''
                || party1Instrument3 !== '' && party1Amount3 === ''
                || party2Instrument2 !== '' && party2Amount2 === ''
                || party2Instrument3 !== '' && party2Amount3 === ''
                || party1Amount2 !== '' && party1Instrument2 === ''
                || party1Amount3 !== '' && party1Instrument3 === ''
                || party2Amount2 !== '' && party2Instrument2 === ''
                || party2Amount3 !== '' && party2Instrument3 === ''
            ) {
                BootstrapDialog.alert({
                    title: 'Required fields needed',
                    message: 'Required fields missing, please try again!'
                });
                return;
            }

            var party1Addr = $('#dvp-contract-wrapper-a .addr').html();
            var party2Addr = $('#dvp-contract-wrapper-b .addr').html();
            var party3Addr = $('#dvp-contract-wrapper-c .addr').html();

            if (cache.recentStateViews[0]['Assetbalances'][party1Addr][party1Instrument1] < party1Amount1 ||
                cache.recentStateViews[0]['Assetbalances'][party2Addr][party2Instrument1] < party2Amount1 ||
                cache.recentStateViews[0]['Assetbalances'][party1Addr][party1Instrument2] < party1Amount2 && party1Amount2 != undefined ||
                cache.recentStateViews[0]['Assetbalances'][party1Addr][party1Instrument3] < party1Amount3 && party1Amount3 != undefined ||
                cache.recentStateViews[0]['Assetbalances'][party2Addr][party2Instrument2] < party2Amount2 && party2Amount2 != undefined ||
                cache.recentStateViews[0]['Assetbalances'][party2Addr][party2Instrument3] < party2Amount3 && party2Amount3 != undefined
            ) {
                BootstrapDialog.alert({
                    title: 'Insufficient balance',
                    message: 'Insufficient balance for a instrument!'
                });
                return;
            }

            if (party1Amount1 < 10){
                BootstrapDialog.alert({
                    title: 'Invalid quantity',
                    message: 'Minimum quantity for currency is 10!'
                });
                return;
            }
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }


        try {
            // Send ajax request to create the contract.
            $.ajax({
                url: 'php/dvp_create_contract.php',
                context: document.body,
                method: "POST",
                data: JSON.stringify({
                      parties: [
                        3,
                        [
                          "1",
                          "",
                          [
                            [
                              party1Addr,
                              party1Instrument1.split("|")[0],
                              party1Instrument1.split("|")[1],
                              party1Amount1,
                              "",
                              "",
                              false,
                              ""
                            ]
                          ],
                          [
                            [
                              party2Addr,
                              party2Instrument1.split("|")[0],
                              party2Instrument1.split("|")[1],
                              party2Amount1
                            ]
                          ],
                          "",
                          "",
                          false
                        ],
                        [
                          "2",
                          "",
                          [
                            [
                              party2Addr,
                              party2Instrument1.split("|")[0],
                              party2Instrument1.split("|")[1],
                              party2Amount1,
                              "",
                              "",
                              false,
                              ""
                            ]
                          ],
                          [
                            [
                              party1Addr,
                              party1Instrument1.split("|")[0],
                              party1Instrument1.split("|")[1],
                              party1Amount1 - party3Amount1
                            ]
                          ],
                          "",
                          "",
                          false
                        ],
                        [
                          "3",
                          party3Addr,
                          [],
                          [
                            [
                              party3Addr,
                              party3Instrument1.split("|")[0],
                              party3Instrument1.split("|")[1],
                              party3Amount1
                            ]
                          ],
                          "",
                          "",
                          false
                        ]
                      ],
                    protocol: 'dvp',
                    contractfunction: 'dvp_uk',
                    url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
                }),
                contentType:"application/json; charset=utf-8",
                dataType: 'json'
            }).done(function (response) {
                if (response['result'] == false){

                  BootstrapDialog.alert({
                        title: 'Contract creation fail',
                        message: 'Fail to create contract!'
                    });
                  return;
                }

                var res = response['result'];

                if ($.type(res[1]) === 'string' || $.type(res[1]['Status']) === 'string') {
                  BootstrapDialog.alert({
                        title: 'Contract creation fail',
                        message: 'Fail to create contract!'
                    });
                    return;
                }
                else {
                    res = res[1];
                    $('#dvp-existing-contract-wrapper').removeClass('hidden');
                    $('#dvp-new-contract-wrapper').addClass('hidden');

                    $('#dvp-existing-contract-wrapper').attr('data-id', res['toaddr']);
                    $('#dvp-existing-contract-wrapper .contract-addr').html(res['toaddr']);
                    $('#dvp-contract-status').html('PENDING');

                    var addrA = $('#dvp-contract-wrapper-a .addr').html();
                    var addrB = $('#dvp-contract-wrapper-b .addr').html();
                    $('#dvp-partA').attr('data-id', addrA);
                    $('#dvp-partB').attr('data-id', addrB);

                    $('#dvp-partA tbody').empty();
                    $.each(res['contractdata']['parties'][1][2], function (key, value) {
                        $('#dvp-partA tbody').append(
                            '<tr>' +
                            '<td>' + value[1] + '|' + value[2] + ' ( Paying )</td>' +
                            '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                            '<td>' +
                            '<input type="checkbox" disabled>' +
                            '</td>' +
                            '</tr>'
                        );
                        cache.instrumensInContract['dvp'].push(value[1] + '|' + value[2]);
                    });

                    $.each(res['contractdata']['parties'][1][3], function (key, value) {
                        $('#dvp-partA tbody').append(
                            '<tr>' +
                            '<td>' + value[1] + '|' + value[2] + ' ( Receiving )</td>' +
                            '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                            '<td>' +
                            '<input type="checkbox" disabled>' +
                            '</td>' +
                            '</tr>'
                        );
                    });

                    $('#dvp-partB tbody').empty();
                    $.each(res['contractdata']['parties'][2][2], function (key, value) {
                        $('#dvp-partB tbody').append(
                            '<tr>' +
                            '<td>' + value[1] + '|' + value[2] + ' ( Paying )</td>' +
                            '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                            '<td>                    ' +
                            '<input type="checkbox" disabled>' +
                            '</td>' +
                            '</tr>'
                        );
                        cache.instrumensInContract['dvp'].push(value[1] + '|' + value[2]);
                    });

                    $.each(res['contractdata']['parties'][2][3], function (key, value) {
                        $('#dvp-partB tbody').append(
                            '<tr>' +
                            '<td>' + value[1] + '|' + value[2] + ' ( Receiving )</td>' +
                            '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                            '<td>                    ' +
                            '<input type="checkbox" disabled>' +
                            '</td>' +
                            '</tr>'
                        );
                        cache.instrumensInContract['dvp'].push(value[1] + '|' + value[2]);
                    });

                    $('#dvp-partC tbody').empty();
                    $.each(res['contractdata']['parties'][3][3], function (key, value) {
                        $('#dvp-partC tbody').append(
                            '<tr>' +
                            '<td>' + value[1] + '|' + value[2] + ' ( Receiving )</td>' +
                            '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                            '<td></td>' +
                            '</tr>'
                        );
                    });

                    //Checkbox + Radio skin
                    $('input:checkbox:not([data-toggle="buttons"] input, .make-switch input), input:radio:not([data-toggle="buttons"] input)').iCheck({
                        checkboxClass: 'icheckbox_minimal',
                        radioClass: 'iradio_minimal',
                        increaseArea: '20%' // optional
                    });

                    // Save the current contract. reset the corresponding data.
                    cache.contractData["dvp"] = res;
                    cache.contractData["dvp"]['completed'] = false;
                    cache.contractData["dvp"]['committed'] = [];

                    // Make Party C balance table visisble:
                    $('#dvp-partc-bl-wrapper').removeClass('hidden');

                    // Set the address selectors as read only.
                    $('#dvp-parta-bl-wrapper select.address-select').attr('disabled', 'disabled');
                    $('#dvp-partb-bl-wrapper select.address-select').attr('disabled', 'disabled');
                    $('#dvp-contract-tab select.address-select').selectpicker('refresh');

                    // Render the contract detail to the browser:
                    var contractHtml = $.parseHTML(JSONTree.create(cache.contractData["dvp"]));
                    $('#dvp-contract-raw-view-wrapper').html(contractHtml);
                    $('#dvp-contract-raw-view-wrapper .jstProperty.parties .jstExpand')[0].click()


                }
            }).fail(function (response) {
                if (cache.prod == false)
                    console.log(response);
            });
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('#dvp-smartContractTab button.new-contract').on('click tap touchend', function (e) {
        try {
            $('#dvp-existing-contract-wrapper').addClass('hidden');
            $('#dvp-new-contract-wrapper').removeClass('hidden');

            $('#dvp-parta-bl-wrapper select.address-select').removeAttr('disabled');
            $('#dvp-partb-bl-wrapper select.address-select').removeAttr('disabled');
            $('#dvp-contract-tab select.address-select').selectpicker('refresh');

            $('#dvp-partA .commited.all button').removeAttr('disabled');
            $('#dvp-partB .commited.all button').removeAttr('disabled');

            $('#dvp-existing-contract-wrapper').attr('data-id', '');
            $('#dvp-existing-contract-wrapper .contract-addr').html('');

            // Clear the contract data in the cache.
            cache.contractData["dvp"] = {};
            cache.commitAwaitBlockResponse['dvp'] = [];
            cache.instrumensInContract['dvp'] = [];
            cache.rawContractRendered['dvp'] = false;

            // Hide the party C balance table.
            $('#dvp-partc-bl-wrapper').addClass('hidden');

            // Smart contract page -> address balances.
            dvpContractDefaultAddress('#dvp-parta-bl-wrapper');
            dvpContractDefaultAddress('#dvp-partb-bl-wrapper');

        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('#dvp-existing-contract-wrapper .commited.all button').on('click tap touchend', function (e) {
        e.preventDefault();

        try {
            var party = parseInt($(e.target).attr('data-id'));

            var partyAddr = party == 1 ? $('#dvp-partA').attr('data-id') : $('#dvp-partB').attr('data-id');
            commitToContract(party, partyAddr, 'dvp');
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    /**
     * Update the part 3 instrument as part 1 update.
     */
    $('#dvp-contract-wrapper-a select.instrument-select').on('change', function(e){
        try {
            var instrument = $('#dvp-contract-wrapper-a select.instrument-select').val();

            $('#dvp-contract-wrapper-c select.instrument-select.instrument1').html('<option value="' + instrument + '">' + instrument + '</option>');
            $('#dvp-contract-wrapper-c select.instrument-select.instrument1').selectpicker('refresh');

        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    /**
     * Update the part 3 qty as part 1 update.
     */
    $('#dvp-contract-wrapper-a .qty1').on('change keyup', function(e){
        try{
            var part1Val = $('#dvp-contract-wrapper-a .qty1').val();
            if(part1Val !== '') {
                var qty = Math.round(commasSeparatedToInt(part1Val) * 0.01); // 10% of the qty.
                $('#dvp-contract-wrapper-c .qty1').val(qty);
            }else{
                $('#dvp-contract-wrapper-c .qty1').val('');
            }
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Contract page -> dvp.
 * Pick an address that is not party 1 and 2.
 */
function dvpPickParty3Address(){
    var addr3Index = Math.round(getRandomArbitrary(20, 23));
    var addr1 = $('#dvp-contract-wrapper-a .addr').html();
    var addr2 = $('#dvp-contract-wrapper-b .addr').html();
    var addr3 = $('#dvp-parta-bl-wrapper select.address-select option:nth-child(' + addr3Index + ')').attr('value');
    if(addr3 === addr1 || addr3 === addr2){
        dvpPickParty3Address();
    }else{
        $('#dvp-contract-wrapper-c .addr').html(addr3);
        $('#dvp-partc-bl-wrapper select.address-select').html('<option value="' + addr3 + '">' + addr3 + '</option>');
        $('#dvp-partc-bl-wrapper select.address-select').selectpicker('refresh');
    }
}

/**
 * Contract page
 * Commit to a contract.
 * @param party: identifier of the party within a contract.
 * @param addr: address of the party.
 * @param contractType: type of the contract
 */
function commitToContract(party, addr, contractType){
    try {
        // Send commit contract request to server, Fill exiting contract.
        var issuingaddress = addr;
        var contractaddress = cache.contractData[contractType]['toaddr'];
        var party = party;
        var commitment = cache.contractData[contractType]['contractdata']['parties'][party][2];
        $.each(commitment, function (key) {
            commitment[key][0] = key;
        });
        var tmp_receive = cache.contractData[contractType]['contractdata']['parties'][party][3];
        var receive = [];
        $.each(tmp_receive, function (key) {
            receive[key] = [key, addr];
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

    try {
        $.ajax({
            url: 'php/commit_contract.php',
            context: document.body,
            method: "POST",
            data: JSON.stringify({
                issuingaddress: issuingaddress,
                contractaddress: contractaddress,
                commitment: commitment,
                party: party,
                receive: receive,
                protocol: contractType,
                url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
            }),
            contentType:"application/json; charset=utf-8",
            dataType: 'json'
        }).done(function (response) {
            var res = response['result'];
            if ($.type(res[1]['status']) === 'Failed to create TX.') {
                showContractFailPopUp();
            }
            else {
                res = res[1];
                var party = parseInt(res['contractdata']['party'][0]);

                var protocol = response['protocol'];

                var partyArr = ['', '-partA','-partB','-partC','-partD'];

                cache.commitAwaitBlockResponse[contractType].push('#'+ contractType +partyArr[party]);
                $('#'+ contractType + partyArr[party] + ' .commited.all button').attr('disabled', 'disabled');
                // Record that we have received confirmation on commitment.
                cache.contractData[contractType]['committed'].push(party);
            }
        }).fail(function (response) {
            if (cache.prod == false)
                console.log(response);
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

}

function showContractFailPopUp(){
    BootstrapDialog.alert({
        title: 'Committing Contract fail',
        message: 'Fail to commit contract!'
    });
    return;
}

/**
 * contract page
 * Request contract detail
 * @param cache
 */
function requestContractDetail(addr){
    try {
        $.ajax({
            url: 'php/contract_detail.php',
            context: document.body,
            method: "POST",
            data: JSON.stringify({
                addr: addr,
                url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
            }),
            contentType:"application/json; charset=utf-8",
            dataType: 'json'
        }).done(function (response) {
            var result = response['result'];

            if (result !== undefined) {
                var contractType = result['protocol'];

                if (result['__completed'] === -1) {
                    // Set the contract in the cache as completed, and update the status in the HTML.
                    cache.contractData[contractType]['completed'] = true;

                    // Flash it.
                    $('#'+contractType+'-contract-status').html('COMPLETED');
                    $('#'+contractType+'-contract-status').addClass('tablerow_hover');
                    setTimeout(function () {
                        $('#'+contractType+'-contract-status').removeClass('tablerow_hover');
                        setTimeout(function () {
                            $('#'+contractType+'-contract-status').addClass('tablerow_hover');
                            setTimeout(function () {
                                $('#'+contractType+'-contract-status').removeClass('tablerow_hover');
                            }, 500);
                        }, 500);
                    }, 500);

                    // Render the contract detail to the browser:
                    var contractHtml = $.parseHTML(JSONTree.create(result));

                    $('#'+contractType+'-contract-raw-view-wrapper').html(contractHtml);

                    $('#'+contractType+'-contract-raw-view-wrapper .jstProperty.parties .jstExpand')[0].click();

                    // Save / Update contract in the cache.
                    result["status"] = "COMPLETED";
                    storeHistoricContract(result);
                }

                if (!cache.rawContractRendered[contractType]) {
                    // Render the contract detail to the browser:
                    var contractHtml = $.parseHTML(JSONTree.create(result));

                    $('#'+contractType+'-contract-raw-view-wrapper').html(contractHtml);
                    cache.rawContractRendered[contractType] = true;

                    $('#'+contractType+'-contract-raw-view-wrapper .jstProperty.parties .jstExpand')[0].click();

                    // Save / Update contract in the cache.
                    result["status"] = "PENDING";
                    storeHistoricContract(result);
                }
            }

        }).fail(function (response) {
            console.log(response)
        })
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page
 * check if contract expired.
 * @param addr
 */
function checkIfExpired(addr){
    try {
        $.ajax({
            url: 'php/contract_detail.php',
            context: document.body,
            method: "POST",
            data: {
                addr: addr,
                url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
            },
            dataType: 'json'
        }).done(function (response) {
            var result = response['result'];
            var addr = response['addr'];
            var contractType = cache.historicContracts[2][addr];

            if (result === undefined) {

                if(contractType === 'dvp'){
                    var partyArr = ['-partA', '-partB'];
                }
                else{
                    var partyArr = ['-partA', '-partB', '-partC', '-partD'];
                }

                // Set the contract in the cache as completed, and update the status in the HTML.
                cache.contractData[contractType]['completed'] = true;

                $.each(partyArr, function(index,party){
                    $('#'+ contractType + party + ' .commited.all button').attr('disabled', 'disabled');
                });

                // Flash it.
                $('#'+ contractType +'-contract-status').html('EXPIRED');
                $('#'+ contractType +'-contract-status').addClass('tablerow_hover');
                setTimeout(function () {
                    $('#'+ contractType +'-contract-status').removeClass('tablerow_hover');
                    setTimeout(function () {
                        $('#dvp-contract-status').addClass('tablerow_hover');
                        setTimeout(function () {
                            $('#'+ contractType +'-contract-status').removeClass('tablerow_hover');
                        }, 500);
                    }, 500);
                }, 500);

                // Mark contract status as expired.
                cache.historicContracts[1][cache.contractData[contractType]["toaddr"]]["status"] = "EXPIRED";
            }

        }).fail(function (response) {
            console.log(response)
        })
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract page.
 * Update the tick boxes in the contract commitment.
 * @param contractType
 */
function renderUpdateCommitment(contractType){
    try {
        for (var i = 0; i < cache.commitAwaitBlockResponse[contractType].length; i++) {
            $(cache.commitAwaitBlockResponse[contractType][i] + ' input:checkbox').iCheck('check');
        }
        cache.commitAwaitBlockResponse[contractType] = [];

        requestContractDetail(cache.contractData[contractType]['toaddr']);
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Contract Page / Search Explorer
 * Store contract in the cache, upon creation and update.
 * @param contractObj
 */
function storeHistoricContract(contractObj){
    if(cache.historicContracts[1][contractObj["__address"]] === undefined){
        cache.historicContracts[0].unshift(contractObj["__address"]);
    }
    cache.historicContracts[1][contractObj["__address"]] = contractObj;
    cache.historicContracts[2][contractObj["__address"]] = contractObj["protocol"];

    if (cache.historicContracts[0].length > 20){
        cache.historicContracts[0].pop();
        cache.historicContracts[1].pop();
    }
}

/**
 * Contract page -> sac/mac
 * Handle the events for sac/mac
 */
function acContractEventHandler(){
    // sac/mac create Conctract event
    $('.ac-wrapper button[type="submit"].new-contract').on('click tap touchend', function (e) {
        try {
            e.preventDefault();

            var parties = [4];
            var parentDiv = $(this).attr('data-parent-wrapper');
            var isValidContract = true;
            var totalReceive = 0;
            var totalPay = 0;
            var contractType = $(this).attr('data-contract-type');

            // Get all the neccessary inputs
            $.each($(parentDiv + ' .party-wrapper'), function (n, partyWrapper) {
                if (isValidContract === false)
                    return false;
                var partyIdentifier = n + 1;
                var partySignAddress = $(partyWrapper).attr('data-addr');
                var payList = [];
                var receiveList = [];
                var publicKey = "";
                var signature = "";
                var mustSign = false;
                var party;
                $.each($(partyWrapper).children(), function (index, instruWrapper) {
                    var addr = partySignAddress;
                    var addr2 = (contractType === 'mac') ? addr : '';
                    var namespace;
                    var classid;
                    var amount = 0;
                    var pub = '';
                    var sig = '';
                    var isPay;
                    var isReceive;
                    var instrument = $(instruWrapper).find('.instru').val();

                    if (instrument == '' || instrument == null) {
                        isValidContract = false;
                        return false;
                    }

                    var instrumentArr = instrument.split(/[:|]/);
                    namespace = instrumentArr[0];
                    classid = instrumentArr[1];
                    isPay = !$(instruWrapper).find('[data-type = "pay"]').prop('disabled');
                    isReceive = !$(instruWrapper).find('[data-type = "receive"]').prop('disabled');

                    if (isPay) {
                        amount = commasSeparatedToInt($(instruWrapper).find('[data-type = "pay"]').val());
                        if(!isInteger(amount)){
                            isValidContract = false;
                        }
                        payList.push([addr, namespace, classid, amount, pub, sig, false, '', '']);
                        receiveList.push([addr2,'','',0]);
                        totalPay += parseInt(amount);
                    }

                    if (isReceive) {
                        amount = commasSeparatedToInt($(instruWrapper).find('[data-type = "receive"]').val());
                        if(!isInteger(amount)){
                            isValidContract = false;
                        }
                        receiveList.push([addr, namespace, classid, amount]);
                        payList.push([addr2, '', '', 0, '', '', false, '', '']);
                        totalReceive += parseInt(amount);
                    }

                    if (amount === 0)
                        isValidContract = false;
                });

                party = [partyIdentifier, partySignAddress, payList, receiveList, publicKey, signature, mustSign];
                parties.push(party);
            });

            if(totalPay !== totalReceive)
                isValidContract = false;


            if (!isValidContract){
                promptInvalidContractInput();
                return;
            }

            // Send request to create ac contract.
            $.ajax({
                url: 'php/ac_create_contract.php',
                context: document.body,
                method: "POST",
                data: JSON.stringify({
                    parties: parties,
                    protocol: contractType,
                    contractfunction: 'dvp_uk',
                    url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
                }),
                contentType:"application/json; charset=utf-8",
                dataType: 'json'
            }).done(function(response){
                var res = response['result'];
                if (response['result'] == false){
                    BootstrapDialog.alert({
                        title: 'Contract creation fail',
                        message: 'Fail to create contract!'
                    });
                    return;
                }

                if ($.type(res[1]) === 'string' || $.type(res[1]['Status']) === 'string') {
                    BootstrapDialog.alert({
                        title: 'Contract creation fail',
                        message: 'Fail to create contract!'
                    });
                    return;
                }
                else {
                    res = res[1];
                    var typeOfContract = res['contractdata']['protocol'];
                    if (typeOfContract === 'sac'){
                        var newContractWrapper = '#sac-new-contract-main-wrapper';
                        var existingContractWrapper = '#sac-existing-contract-main-wrapper';
                        var statusWrapper = '#sac-contract-status';
                    }else if(typeOfContract === 'mac'){
                        var newContractWrapper = '#mac-new-contract-main-wrapper';
                        var existingContractWrapper = '#mac-existing-contract-main-wrapper';
                        var statusWrapper = '#mac-contract-status';
                    }

                    $(existingContractWrapper).removeClass('hidden');
                    $(newContractWrapper).addClass('hidden');

                    $(existingContractWrapper).attr('data-id', res['toaddr']);
                    $(existingContractWrapper + ' .contract-addr').html(res['toaddr']);
                    $(statusWrapper).html('PENDING');

                    var addrA = $('#'+ typeOfContract+ '-parta-bl-wrapper .partyA').val();
                    var addrB = $('#'+ typeOfContract+ '-partb-bl-wrapper .partyB').val();
                    var addrC = $('#'+ typeOfContract+ '-partc-bl-wrapper .partyC').val();
                    var addrD = $('#'+ typeOfContract+ '-partd-bl-wrapper .partyD').val();


                    $('#'+ typeOfContract+ '-partA').attr('data-id', addrA);
                    $('#'+ typeOfContract+ '-partB').attr('data-id', addrB);
                    $('#'+ typeOfContract+ '-partC').attr('data-id', addrC);
                    $('#'+ typeOfContract+ '-partD').attr('data-id', addrD);

                    var partyWrapperArray = ['#'+ typeOfContract+ '-partA', '#'+ typeOfContract+ '-partB', '#'+ typeOfContract+ '-partC', '#'+ typeOfContract+ '-partD'];
                    var partyAddresses = [addrA, addrB, addrC, addrD];
                    var partyArray = ['partyA','partyB','partyC','partyD'];

                    $.each(partyWrapperArray, function(index, wrapper){
                        $(wrapper).attr('data-id', partyAddresses[index]);

                        $(wrapper + ' tbody').empty();

                        $.each(res['contractdata']['parties'][index + 1][2], function (key, value) {
                            if (value[1] !== "" && value[2] !== "" && value[3] !== 0) {
                              $(wrapper + ' tbody').append(
                                  '<tr>' +
                                  '<td>' + value[1] + '|' + value[2] + ' ( Paying )</td>' +
                                  '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                                  '<td>' +
                                  '<input type="checkbox" disabled>' +
                                  '</td>' +
                                  '</tr>'
                              );
                              cache.instrumensInContract[typeOfContract][partyArray[index]].push(value[1] + '|' + value[2]);
                            }
                        });

                        $.each(res['contractdata']['parties'][index + 1][3], function (key, value) {
                            if (value[1] !== "" && value[2] !== "" && value[3] !== 0) {
                              $(wrapper + ' tbody').append(
                                  '<tr>' +
                                  '<td>' + value[1] + '|' + value[2] + ' ( Receiving )</td>' +
                                  '<td>' + commaSeparateNumber(value[3]) + '</td>' +
                                  '<td>' +
                                  '<input type="checkbox" disabled>' +
                                  '</td>' +
                                  '</tr>'
                              );
                              cache.instrumensInContract[typeOfContract][partyArray[index]].push(value[1] + '|' + value[2]);
                            }
                        });
                    });

                    //Checkbox + Radio skin
                    $('input:checkbox:not([data-toggle="buttons"] input, .make-switch input), input:radio:not([data-toggle="buttons"] input)').iCheck({
                        checkboxClass: 'icheckbox_minimal',
                        radioClass: 'iradio_minimal',
                        increaseArea: '20%' // optional
                    });

                    // Save the current contract. reset the corresponding data.
                    cache.contractData[typeOfContract] = res;
                    cache.contractData[typeOfContract]['completed'] = false;
                    cache.contractData[typeOfContract]['committed'] = [];

                    // Render the contract detail to the browser:
                    var contractHtml = $.parseHTML(JSONTree.create(cache.contractData[typeOfContract]));
                    $('#'+ typeOfContract+ '-contract-raw-view-wrapper').html(contractHtml);

                }

            }).fail(function(err){
                    if(cache.prod == false) console.log(err);
            });
        }catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('.ac-wrapper .commited.all button').on('click tap touchend', function (e) {
        e.preventDefault();

        try {
            var party = parseInt($(e.target).attr('data-id'));
            var typeOfContract = $(this).attr('data-contract-type');
            var partyArr = ["","-partA", "-partB","-partC","-partD"];
            var partyAddr = $('#' + typeOfContract + partyArr[party] ).attr('data-id');
            commitToContract(party, partyAddr, typeOfContract);
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });

    $('.ac-wrapper button.new-contract').on('click tap touchend', function (e) {
        try {

            var typeOfContract = $(this).attr('data-contract-type');
            if (typeOfContract === 'sac'){
                var newContractMainWrapper = '#sac-new-contract-main-wrapper';
                var newcontractWrapper = '#sac-new-contract-wrapper';
                var existingContractWrapper = '#sac-existing-contract-main-wrapper';
                var statusWrapper = '#sac-contract-status';
            }else if(typeOfContract === 'mac'){
                var newContractMainWrapper = '#mac-new-contract-main-wrapper';
                var newcontractWrapper = '#mac-new-contract-wrapper';
                var existingContractWrapper = '#mac-existing-contract-main-wrapper';
                var statusWrapper = '#mac-contract-status';
            }

            $(newContractMainWrapper).removeClass('hidden');
            $(existingContractWrapper).addClass('hidden');

            $('#' + typeOfContract+ '-partA .commited.all button').removeAttr('disabled');
            $('#' + typeOfContract+ '-partB .commited.all button').removeAttr('disabled');
            $('#' + typeOfContract+ '-partC .commited.all button').removeAttr('disabled');
            $('#' + typeOfContract+ '-partD .commited.all button').removeAttr('disabled');

            $('#' + typeOfContract+ '-existing-contract-wrapper').attr('data-id', '');
            $('#' + typeOfContract+ '-existing-contract-wrapper .contract-addr').html('');

            // Clear the contract data in the cache.
            cache.contractData[typeOfContract] = {};
            cache.commitAwaitBlockResponse[typeOfContract] = [];
            cache.instrumensInContract[typeOfContract] = {partyA:[],partyB:[],partyC:[],partyD:[]};
            cache.rawContractRendered[typeOfContract] = false;

            // Smart contract page -> address balances.
            var newContractMainWrapper = '#' + $(this).closest('.new-contract-wrapper').attr('id');
            acContractNewContractBalanceUpdate(newContractMainWrapper);

            acContractDefaultQtyForContract(newcontractWrapper);

        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

function promptInvalidContractInput(){
    BootstrapDialog.alert({
        title: 'Invalid Contract',
        message: 'Invalid data for contract!'
    });
    return;
}

/**
 * Handle event, when user input into a quantity field.
 * In essense, make the input number commas separated.
 */
function qtyInputHandler(){
    $('input.qty').on('change keyup', function(e){
        $(this).val(commaSeparateNumber($(this).val()));
    });
}

/**
 * Handle event, when Sv name is click in the diagnosis page. in the DATA View.
 */
function diagnosisSvDataClickHanler(){
    $('#dumpedSvDataView').on('click touch', '.svDataEntryWrapper', function(e){
        e.preventDefault();

        var svProperty = $(this).attr('data-name');
        // Render the sv data detail to the browser:
        var html = $.parseHTML(JSONTree.create(cache.diagnosisSV[svProperty]));

        // Create a modal to show it.
        var dialog = new BootstrapDialog({
            type: 'type-info',
            cssClass: '',
            size: BootstrapDialog.SIZE_WIDE,
            title: 'Raw SV Detail',
            message: html,
            closable: true
        });

        //// Get the size of the screen, in order to set the modal max height.
        var maxHeight = $(window).height() - 60;
        dialog.realize();
        dialog.getModalContent().css('max-height', maxHeight + 'px');
        dialog.getModalBody().css('max-height', maxHeight - 78 + 'px');
        dialog.open();
    });
}

/**
 * check if integer
 * @param string
 * @returns {boolean|*}
 */
function isInteger(string){
    return (Math.floor(string) == string && $.isNumeric(string))
}

/**
 * Get Url parameter.
 * @param sParam
 * @returns {boolean}
 * source: http://stackoverflow.com/questions/19491336/get-url-parameter-jquery
 */
function getUrlParameter(sParam) {
    try {
        var sPageURL = decodeURIComponent(window.location.search.substring(1)),
            sURLVariables = sPageURL.split('&'),
            sParameterName,
            i;

        for (i = 0; i < sURLVariables.length; i++) {
            sParameterName = sURLVariables[i].split('=');

            if (sParameterName[0] === sParam) {
                return sParameterName[1] === undefined ? false : sParameterName[1];
            }
        }
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Get the keys of an object and sort it.
 * @param obj
 * @returns key of the key that sorted
 */
function sortedObjectKey(obj){
    try {
        var keyArr = [];

        if (obj instanceof Object) {
            $.each(obj, function (key) {
                keyArr.push(key);
            })
        }
        keyArr.sort(compareFnAlph);
        return keyArr;
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Comparation function that make alphabetical letter higher than number.
 */
function compareFnAlph(a, b){
    try {
        var hasNumber = /\d/;

        if (hasNumber.test(a)) {
            if (!hasNumber.test(b))
                return 1;
        }
        else {
            if (hasNumber.test(b))
                return -1;
        }

        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        // a must be equal to b
        return 0;
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 *  Get the configuration setting from the DB.
 *  @para: attrs: the setting we want to get from the DB, or array of settings.
 *  @return: object of all the setting info.
 */
function getChainsConfigInfo(attrs)
{
    $.ajax({
        //url: '/php/getChainConfigs.php',
      url: 'chain-configs.json',
        context: document.body,
        data: { attrs: attrs},
        dataType: 'json',
        async: false,
        headers: { 'x-my-custom-header': 'some value' }
    }).done(function(response){
        cache.chainConfigInfo = response;
    }).fail(function(e){
        if(cache.prod == false) console.log(e.message);
        return null;
    })
}

/**
 * Configure the necessary setting of the site.
 */
function setChainSetting(){
    $('#paymentSystemSettingWrapper .ps1 h6').html(cache.paymentSystemsLabel[0]);
    $('#paymentSystemSettingWrapper .ps2 h6').html(cache.paymentSystemsLabel[1]);
    $('#paymentSystemSettingWrapper .ps3 h6').html(cache.paymentSystemsLabel[2]);
    $('#paymentSystemSettingWrapper .ps4 h6').html(cache.paymentSystemsLabel[3]);
    $('#paymentSystemSettingWrapper .ps5 h6').html(cache.paymentSystemsLabel[4]);
    $('#paymentSystemSettingWrapper .ps6 h6').html(cache.paymentSystemsLabel[5]);
}

/**
 * Convert commas separated string to integer
 * @param str
 * @returns {Number}
 */
function commasSeparatedToInt(str){
    if (str === undefined)
        return undefined;
    return parseInt(str.replace(/,/g,''))
}

/**
 * shuffle algorithm
 * ref: http://stackoverflow.com/questions/2450954/how-to-randomize-shuffle-a-javascript-array
 * @param array
 * @returns {*}
 */
function shuffle(array) {
    var currentIndex = array.length, temporaryValue, randomIndex;

    // While there remain elements to shuffle...
    while (0 !== currentIndex) {

        // Pick a remaining element...
        randomIndex = Math.floor(Math.random() * currentIndex);
        currentIndex -= 1;

        // And swap it with the current element.
        temporaryValue = array[currentIndex];
        array[currentIndex] = array[randomIndex];
        array[randomIndex] = temporaryValue;
    }

    return array;
}

/**
 * Contract page -> sac/mac
 * Handle the events for sac/mac
 */
function getOwnedAddr() {
    // Send request to create ac contract.
    $.ajax({
        url: 'php/getOwnedAddr.php',
        context: document.body,
        method: "POST",
        data: {
            url: cache.currentConnectedNode.nodeURL + ":" + cache.chainConfigInfo[cache.chainID]['RestPort']
        },
        dataType: 'json'

    }).done(function (response) {
        var res = response['result'];
        for(var i = 0; i < res.length; i++)
        {
            cache.ownedAddrList.push(res[i]['address']);
        }

    }).fail(function (err) {
        if (cache.prod == false) console.log(err);
    });
}