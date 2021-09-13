if (document.dataCache == undefined)
{
    document.dataCache = {};
}

var cache = document.dataCache;

cache.prod = false;

$(document).ready(function(){
    fillFromAddressOrCheckBalance();
    sendTransctionBtnLisener();
    cancelTransctionBtnLisener();
});

/**
 * Cancel Transaction.
 */
function cancelTransctionBtnLisener(){
    $('#cancel-transaction').on('click tap', function(e){
        e.preventDefault();
        window.location.replace(cache.chainConfigInfo[cache.chainID]['url']);
    });
}

/**
 * Get from address list from server.
 */
function fillFromAddressOrCheckBalance(namespace,classid,byaddress, address,url){
    try {
        if (classid == undefined) classid = $('[name=security]').val();
        if (namespace == undefined) namespace = $('[name=issue]').val();
        if (byaddress == undefined) byaddress = true;
        if (address == undefined) address = '';
        if (url == undefined) url = $('[name=nodeIP]').val();

        $.ajax({
            url: 'getFromAddr.php',
            context: document.body,
            method: 'POST',
            data: {namespace: namespace, classid: classid, byaddress: byaddress, address: address, url: url},
            dataType: 'json',
            async: false

        }).done(function (response) {
            if (address == '') { // Fill from address list.
                if (!response['result']) {
                    $('#error').html("<h3><span class='label label-default m-t-20' width='200px'>Can't Fetch from addresses list.</span></h3>");
                } else {
                    try {
                        var result = $.parseJSON(response['result']);

                        //console.log(result);
                        $.each(result, function (address) {
                            $('#send-tx-form-wrapper select.fromAddress').append(
                                '<option value="' + address + '">' + address + '</option>'
                            );
                        });
                        $('#send-tx-form-wrapper .fromAddress').selectpicker('refresh');

                        /**
                         * Pick any of the first 20 Address (keep the number, avoiding overflow).
                         *
                         * Find the value of the index "rand".
                         *
                         * Select the address.
                         */
                        var randIndex = Math.floor((Math.random() * 20 + 1));
                        var randValue = $('#send-tx-form-wrapper .select.fromAddress option')[randIndex].value;
                        $('#send-tx-form-wrapper .select.fromAddress').selectpicker('val', randValue);

                    }
                    catch (e) {
                        if (cache.prod == false) console.log(e.message);
                        $('#error').html("<h3><span class='label label-default m-t-20' width='200px'>Can't Fetch from addresses list.</span></h3>");
                    }
                }

            } else {
                if (!response['result']) {
                    $('#read-balance-error').removeClass('hidden');
                }
                else {
                    paintFromBalanceOrReturnBalance($.parseJSON(response['result']));
                }

            }
        })
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

}

/**
 * Send Transaction.
 */
function sendTransctionBtnLisener(){
    $('#send-transaction').on('click tap', function(e){
        try {
            e.preventDefault();

            var classid = $('[name=security]').val();
            var namespace = $('[name=issue]').val();
            var amount = $('[name=amount]').val();
            var toaddr = $('[name=toAddress]').val();
            var fromaddr = $('[name=fromAddress]').val();
            var nodeIP = $('[name=nodeIP]').val();
            var chainID = $('body').attr('data-chain-id');

            if (fromaddr == undefined || fromaddr == '') {
                BootstrapDialog.alert({
                    title: 'From address needed',
                    message: 'From address field empty. Please select a from address.'
                });
                return;
            }

            BootstrapDialog.show({
                title: 'Sending Transaction...',
                message: '<h5>Do you want to make the following transaction:</h5><br />' +
                'Address (From): ' + fromaddr + '<br />' +
                'Address (To): ' + toaddr + '<br />' +
                'Issuer: ' + classid + '<br />' +
                'Instrument: ' + namespace + '<br />' +
                'Amount: ' + amount,

                buttons: [{
                    label: 'Yes',
                    action: function (dialogItself) {
                        dialogItself.close();

                        $.ajax({
                            url: 'sendTX.php',
                            context: document.body,
                            method: "POST",
                            data: {
                                namespace: namespace,
                                classid: classid,
                                fromaddr: fromaddr,
                                toaddr: toaddr,
                                amount: amount,
                                nodeIP: nodeIP,
                                tochain: chainID
                            },
                            dataType: 'json'
                        }).done(function (response) {
                            if (response['result']) {
                                try {
                                    $('#send-tx-detail').removeClass('hidden');
                                    $('#send-tx-form-wrapper').addClass('hidden');

                                    paintTxDetail($.parseJSON(response['result']));
                                    fillFromAddressOrCheckBalance(namespace, classid, true, fromaddr, nodeIP);
                                } catch (e) {
                                    if (cache.prod == false) console.log(e.message);
                                    $('#send-tx-error').removeClass('hidden');
                                    $('#send-tx-form-wrapper').addClass('hidden');
                                }
                            }
                            else {
                                $('#send-tx-error').removeClass('hidden');
                                $('#send-tx-form-wrapper').addClass('hidden');
                            }

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
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * paint TX detail to DOM.
 */
function paintTxDetail(tx){
    $('#send-tx-detail .tx-id').html(tx['hash'].substr(0,15)+"...");
    $('#send-tx-detail .fromAddr').html(tx['fromaddr'].substr(0,15)+"...");
    $('#send-tx-detail .toAddr').html(tx['toaddr'].substr(0,15)+"...");
    $('#send-tx-detail .tx-amount').html(tx['amount']);
    //$('#send-tx-detail .tx-fee').html(tx['fee']);
    $('#send-tx-detail .issuer').html(tx['namespace']);
    $('#send-tx-detail .security').html(tx['classid']);
    //$('#send-tx-detail .baseChain-id').html(tx['basechain']);
    //$('#send-tx-detail .toChain-id').html(tx['tochain']);
}

/**
 * paint balance to DOM.
 */
function paintFromBalanceOrReturnBalance(balanceDetail){
    try {
        $.each(balanceDetail, function (addr, item) {
            $.each(item, function (namespaceAndClass, balance) {
                var namespace = namespaceAndClass.split('|')[0];
                var classid = namespaceAndClass.split('|')[1];

                //paint to DOM
                $('#tx-fromAddr-balance tbody').html(
                    '<tr>' +
                    '<td>' + addr + '</td>' +
                    '<td>' + namespace + '</td>' +
                    '<td>' + classid + '</td>' +
                    '<td>' + balance + '</td>' +
                    '</tr>'
                );

            })
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}