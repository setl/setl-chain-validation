/**********************************************************************************************
 *
 * Paint updates to the DOM - Dash
 *
 **********************************************************************************************/

function updateBlockHeight(blockHeight){
    try {
        $('#blockheight').html(commaSeparateNumber(blockHeight));
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateTimeTakenLastBlock(timeTaken){
    try {
        timeTaken = Math.round(timeTaken * 100) / 100;
        //$('#lastBlockSeconds').attr('data-value', (Math.round(timeTaken*100)/100).toFixed(1));
        $('#sinceLastBlockSeconds').html(timeTaken.toFixed(1));
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

}

function updateMovementsLastBlock(lastBlockMovement){
    try {
        var startValue = cache.recentBlocks.length > 1 ? cache.recentBlocks[1]['TXCount'] : 0;
        $({someValue: startValue}).animate({someValue: lastBlockMovement}, {
            duration: 1000,
            easing: 'swing', // can be anything
            step: function () { // called on every step
                // Update the element's text with rounded-up value:
                $('#movementsLastBlock').html(commaSeparateNumber(Math.round(this.someValue)));
            },
            complete: function () {
                $('#movementsLastBlock').html(commaSeparateNumber(lastBlockMovement));
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateMovements24h(movements24H){
    try {
        $('#movements24h').html(commaSeparateNumber(movements24H));
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateMovements24hLineChart(movements24H){
    try {
        if (cache.movements24hArr.length > 10)
            cache.movements24hArr.shift();
        cache.movements24hArr.push(movements24H);

        $("#stats-line-movements24h").sparkline(cache.movements24hArr, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateDairyMovements(movementsPD){
    try {
        // Use average instead of current movements, this is trying to smooth out the Line chart.
        var newMovementsPD = Math.floor(cache.dairyMovementsArr.length > 0 ? (movementsPD + cache.dairyMovementsArr[cache.dairyMovementsArr.length - 1]) / 2 : movementsPD);
        $('#movementsPS').html(commaSeparateNumber(newMovementsPD));
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateMovementsDairyLineChart(movementsPD){
    try {
        // Use average instead of current movements, this is trying to smooth out the Line chart.
        var newMovementsPD = Math.floor(cache.dairyMovementsArr.length > 0 ? (movementsPD + cache.dairyMovementsArr[cache.dairyMovementsArr.length - 1]) / 2 : movementsPD);

        if (cache.dairyMovementsArr.length > 10)
            cache.dairyMovementsArr.shift();
        cache.dairyMovementsArr.push(movementsPD);

        if (cache.dairyMovementsArrNew.length > 10)
            cache.dairyMovementsArrNew.shift();
        cache.dairyMovementsArrNew.push(newMovementsPD);

        $("#stats-line-movementPS").sparkline(cache.dairyMovementsArrNew, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateLastBlockTimeSeconds(lastBlockTime){
    $('#lastBlockTimeSeconds').html(Math.round(lastBlockTime*100)/100);
}

function updateCurrentProposalTime(proposalTime){
    $('#proposalTime').html('<span>'+unixTimeToUTC(proposalTime)+'</span>');
}

function updateXchainMovementIn(xchainIn){
    try {
        var startValue = cache.recentBlocks.length > 1 && cache.recentBlocks[1]['TXIn'] !== undefined ? cache.recentBlocks[1]['TXIn'] : 0;
        $({someValue: startValue}).animate({someValue: xchainIn}, {
            duration: 1000,
            easing: 'swing', // can be anything
            step: function () { // called on every step
                // Update the element's text with rounded-up value:
                $('#xchainIn').html(commaSeparateNumber(Math.round(this.someValue)));
            },
            complete: function () {
                $('#xchainIn').html(commaSeparateNumber(xchainIn));
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateXchainMovementInLineChart(xchainIn){
    try {
        if (cache.xchainMovementInArr.length > 10)
            cache.xchainMovementInArr.shift();
        cache.xchainMovementInArr.push(xchainIn);
        $('#stats-line-xchainIn').sparkline(cache.xchainMovementInArr, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateXchainMovementOut(xchainOut){
    try {
        var startValue = cache.recentBlocks.length > 1 ? cache.recentBlocks[1]['TXOut'] : 0;
        $({someValue: startValue}).animate({someValue: xchainOut}, {
            duration: 1000,
            easing: 'swing', // can be anything
            step: function () { // called on every step
                // Update the element's text with rounded-up value:
                $('#xchainOut').html(commaSeparateNumber(Math.round(this.someValue)));
            },
            complete: function () {
                $('#xchainOut').html(commaSeparateNumber(xchainOut));
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateXchainMovementOutLineChart(xchainOut){
    try {
        if (cache.xchainMovementOutArr.length > 10)
            cache.xchainMovementOutArr.shift();
        cache.xchainMovementOutArr.push(xchainOut);
        $('#stats-line-xchainOut').sparkline(cache.xchainMovementOutArr, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateCurrentProposalMovements(proposalMovement){
    $('#proposalMovements').html('<span>'+commaSeparateNumber(proposalMovement)+'</span>');
}

function updateCurrentProposalNode(proposalNode){
    //$('#proposalNode').html(('<span class="animated fadeInDown">'+cache.nodesInfo[proposalID]['nodeName'])+'</span>');
    $('#proposalNode').html(('<span>'+proposalNode)+'</span>');
}

function updateCurrentProposalSigned(signedPercentage){
    $('#proposalSigned').html('<span>'+ Math.round(signedPercentage*10000) / 100 + '%'+'</span>');
}
function updateCurrentProposalVoted(votePercentage){
    $('#proposalVoted').html('<span>'+ Math.round(votePercentage*10000) / 100 + '%'+'</span>');
}

function updateCurrentProposalBlock(blockNumber){
    $('#proposalBlock').html('<span>'+commaSeparateNumber(blockNumber)+'</span>');
}

function updatePieChartCurrentProposalPercentage(sign,count){
    $('#proposal-pie-chart-wrapper .pie-chart-tiny:nth-child(1)').data('easyPieChart').update(Math.max(sign*100,count*100));
}

function initialiseDashBarChart(){
    //Bar Chart
    try {
        if ($("#dash_bar-chart")[0]) {
            if(cache.chainID == '16' || cache.chainID == '100') {
                cache.barChartData = [{
                    data: cache.scenarioBenchMarkArr,
                    label: 'BenchMark',
                    bars: {
                        show: true,
                        barWidth: 0.3,
                        order: 1,
                        fill: 1,
                        lineWidth: 0,
                        fillColor: 'rgba(66,139,202,0.8)'
                    }
                }];
            }

            cache.barChartData.push({
                data: cache.scenarioActualArr,
                label: 'Actual',
                bars: {
                    show: true,
                    barWidth: 0.3,
                    order: 2,
                    fill: 1,
                    lineWidth: 0,
                    fillColor: cache.chainID == 0 ? 'rgba(66,139,202,0.8)' : 'rgba(255,204,0,0.6)'

                }
            });


            //Display graph
            cache.dashBarsChart = $.plot($("#dash_bar-chart"), cache.barChartData, {

                grid: {
                    borderWidth: 1,
                    borderColor: 'rgba(255,255,255,0.25)',
                    show: true,
                    hoverable: true,
                    clickable: true
                },

                yaxis: {
                    tickColor: 'rgba(255,255,255,0.15)',

                    font: {
                        lineHeight: 13,
                        style: "normal",
                        color: "rgba(255,255,255,0.8)"
                    },
                    shadowSize: 0,
                    tickDecimals: 3,
                    ticks: [100000, 1000000, 10000000, 100000000, 1000000000],
                    transform: function (v) {
                        return (Math.log(v + 0.0001)) / (Math.log(10));
                        /*+0.0001 move away from zero*/
                    },
                    tickFormatter: function (val, axis) {
                        return commaSeparateNumber(val);
                    },
                    min: 20000, max: 100000000
                },

                xaxis: {
                    tickColor: 'rgba(255,255,255,0)',
                    tickDecimals: 0,
                    font: {
                        lineHeight: 13,
                        style: "normal",
                        color: "rgba(255,255,255,0.8)"
                    },
                    shadowSize: 0,
                    labelWidth: cache.chainID == 0 ? 2 : null,

                    ticks: [[1, '<h6>'+cache.paymentSystemsLabel[0]+'</h6>'],
                            [2, '<h6>'+cache.paymentSystemsLabel[1]+'</h6>'],
                            [3, '<h6>'+cache.paymentSystemsLabel[2]+'</h6>'],
                            [4, '<h6>'+cache.paymentSystemsLabel[3]+'</h6>'],
                            [5, '<h6>'+cache.paymentSystemsLabel[4]+'</h6>'],
                            [6, '<h6>'+cache.paymentSystemsLabel[5]+'</h6>']]
                },

                legend: true,
                tooltip: true,
                tooltipOpts: {
                    content: "<b>%x</b> = <span>%y</span>",
                    defaultTheme: false
                }

            });

            $("#dash_bar-chart").bind("plothover", function (event, pos, item) {
                if (item) {
                    //var x = item.datapoint[0].toFixed(2),
                    var y = commaSeparateNumber(item.datapoint[1].toFixed(0));
                    $("#dash_barchart-tooltip").html(item.series.label + " = " + y).css({
                        top: item.pageY + 5,
                        left: item.pageX + 5
                    }).fadeIn(200);
                }
                else {
                    $("#dash_barchart-tooltip").hide();
                }
            });


            $("<div id='dash_barchart-tooltip' class='chart-tooltip'></div>").appendTo("body");

        }
    }
    catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 *  Handle click event for pie chart name hyperlink.
 */
function proposalClickEventHandler(){
    $('#proposal-pie-chart-wrapper').on('click', '.pie-chart-tiny a', function(e){
        try {
            e.preventDefault();

            var proposalID = parseInt($(this).closest('.pie-chart-tiny').index());


            BootstrapDialog.alert({
                title: 'Proposal Detail',
                message: '<div class="row"><div class="col-lg-6">Proposal Time:</div><div class="col-lg-6">' + unixTimeToUTC(cache.fiveRecentProposal[proposalID]['Timestamp']) + '</div></div>' +
                '<div class="row"><div class="col-lg-6">Number of Movements: </div><div class="col-lg-6">' + cache.fiveRecentProposal[proposalID]['TXCount'] + '</div></div>' +
                '<div class="row"><div class="col-lg-6">Proposed By: </div><div class="col-lg-6">' + cache.fiveRecentProposal[proposalID]['Hostname'] + '</div></div>' +
                '<div class="row"><div class="col-lg-6">Signed By:</div><div class="col-lg-6"> ' + Math.round(cache.fiveRecentProposal[proposalID]['SignPercentage'] * 10000) / 100 + '%</div></div>' +
                '<div class="row"><div class="col-lg-6">Voted By:</div><div class="col-lg-6"> ' + Math.round(cache.fiveRecentProposal[proposalID]['VotePercentage'] * 10000) / 100 + '%</div></div>' +
                '<div class="row"><div class="col-lg-6">Block Number:</div><div class="col-lg-6"> ' + cache.fiveRecentProposal[proposalID]['Height'] + '</div></div>'
            });
        } catch(err){
            if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
        }
    });
}

/**
 * Render the latest three blocks to browser.
 */
function updateLatestThreeBlocks(latestThreeBlocks){
    try {
        for (var i = 0; i <= 2 && i <= (latestThreeBlocks.length - 1); i++) {
            $('#dash-last-three-block-wrapper [data-block-id =' + i + ']').html(
                '<td>' + latestThreeBlocks[i]['Height'] + '</td>' +
                '<td>' + unixTimeToUTC(latestThreeBlocks[i]['Timestamp']) + '</td>' +
                '<td>' + latestThreeBlocks[i]['TXCount'] + '</td>' +
                    //todo add these columns.
                '<td>' + latestThreeBlocks[i]['Hostname'] + '</td>' +
                '<td>% ' + (isNaN(latestThreeBlocks[i]['SignPercentage']) ? '' : Math.round(latestThreeBlocks[i]['SignPercentage'] * 10000) / 100) + '</td>' +
                '<td>% ' + (isNaN(latestThreeBlocks[i]['VotePercentage']) ? '' : Math.round(latestThreeBlocks[i]['VotePercentage'] * 10000) / 100) + '</td>'
            );
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateLastBlockTimeLineChart(lastBlockTime){
    try {
        if (cache.lastBlockTimeBlockTimeArr.length > 10)
            cache.lastBlockTimeBlockTimeArr.shift();
        cache.lastBlockTimeBlockTimeArr.push(Math.round(lastBlockTime * 100) / 100);

        $("#stats-line-lastBlockTimeSeconds").sparkline(cache.lastBlockTimeBlockTimeArr, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateRecentBlockMovementLineChart(currentBlockMovement){
    try {
        if (cache.blockMovementArr.length > 10)
            cache.blockMovementArr.shift();
        cache.blockMovementArr.push(currentBlockMovement);
        $("#stats-line-movementsLastBlock").sparkline(cache.blockMovementArr, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateTimeTakenLastBlockLineChart(currentTimeTaken){
    try {
        if (cache.blockTimeTakenArr.length > 10)
            cache.blockTimeTakenArr.shift();
        cache.blockTimeTakenArr.push(Math.round(currentTimeTaken * 100) / 100);

        $('#stats-line-sinceLastBlockSeconds').sparkline(cache.blockTimeTakenArr, {
            type: 'line',
            width: '100%',
            height: '65',
            lineColor: 'rgba(255,255,255,0.4)',
            fillColor: 'rgba(0,0,0,0.2)',
            lineWidth: 1.25
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateLatestTransactionsTable(){
    try {
        $('#dash-last-12-transactions-wrapper tbody').empty();

        for (var i = 0; i <= cache.latestTransactions[0]['Transaction'].length - 1; i++) {
            $('#dash-last-12-transactions-wrapper tbody').append(
                '<tr data-transaction-id="' + i + '">' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][7].substr(0, 6) + '...' + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][3] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][4] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][1] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][2] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][9] + '</td>' +
                '<td>' + 'externalRef' + '</td>' +
                    //'<td class="animated fadeIn">' + cache.last12Transactions[i]['contingent'] + '</td>' +
                '<td>' + commaSeparateNumber(cache.latestTransactions[0]['Transaction'][i][5]) + '</td>' +
                '<td>' + unixTimeToUTC(cache.latestTransactions[0]['Timestamp']) + '</td>' +
                '</tr>'
            );
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateProposalPieChart(newPiePercentage){
    try {
        //delete the oldest one.
        if ($('#proposal-pie-chart-wrapper .pie-chart-tiny').length == 5)
            $('#proposal-pie-chart-wrapper .pie-chart-tiny')[4].remove();

        ////update the data-proposal-id attribute.
        //if ($('#proposal-pie-chart-wrapper .pie-chart-tiny').length > 0) {
        //    for (var i = $('#proposal-pie-chart-wrapper .pie-chart-tiny').length - 1; i >= 0; i--) {
        //        $('[data-proposal-id=' + i + ']').attr('data-proposal-id', i + 1);
        //    }
        //}
        //
        //create new one.
        $('#proposal-pie-chart-wrapper > div').prepend(
            '<div class="pie-chart-tiny animated flash" data-percent="' + newPiePercentage + '">' +
            '<span class="percent"></span>' +
            '<span class="pie-title"><a href="" data-proposal-id="0">Proposal ' + (cache.fiveRecentProposal[0]['ProposedBlockHash']).substr(0, 4) + '...</a></span>' +
            '</div>'
        );

        $($('#proposal-pie-chart-wrapper .pie-chart-tiny')[0]).easyPieChart({
            //easing: 'easeOutBounce',
            barColor: 'rgba(255,255,255,0.75)',
            trackColor: 'rgba(0,0,0,0.3)',
            scaleColor: 'rgba(255,255,255,0.3)',
            lineCap: 'square',
            lineWidth: 4,
            size: 100,
            animate: 500,
            onStep: function (from, to, percent) {
                $(this.el).find('.percent').text(Math.round(percent));
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
 }


/**********************************************************************************************
 *
 * Paint updates to the DOM - Block View
 *
 **********************************************************************************************/
function updateBlockViewBlockList()
{
    try {
        if (cache.recentBlocks.length > 1) {
            // Remove the last if necessary.
            if ($('#blocks-view-wrapper #block-list-wrapper .list-group .list-group-item').length == 20)
                $('#blocks-view-wrapper #block-list-wrapper .list-group .list-group-item:nth-child(20)').remove();

            if ($('#blocks-view-wrapper #block-list-wrapper .list-group .list-group-item.active').length == 0)
                cache.blockViewSelected = false;

            // Update the last Current Block text.
            $('#blocks-view-wrapper #block-list-wrapper .list-group .list-group-item:nth-child(1)').html(pad(cache.recentBlocks[1]['Height'], 8) + '<span class="pull-right">' + unixTimeToUTC(cache.recentBlocks[1]['Timestamp']) + '</span>' + '<div class="list-options">' + '<button class="btn btn-sm block-detial">View</button>' + '</div>');
            if (cache.blockViewSelected === false) { //If user not chosen a block.
                $('#blocks-view-wrapper #block-list-wrapper .list-group .list-group-item').removeClass('active');
                // Prepend new current block with active state.
                $('#blocks-view-wrapper #block-list-wrapper .list-group').prepend(
                    '<a href="#" class="list-group-item active" >Current Block' + '<span class="pull-right">' + unixTimeToUTC(cache.recentBlocks[0]['Timestamp']) + '</span>' +
                    '<div class="list-options">' +
                    '<button class="btn btn-sm block-detial">View</button>' +
                    '</div>' +
                    '</a>'
                );

                //Block view page -> block detial.
                updateBlockViewBlockDetial(0);

                return true;
            }

            // Prepend new current block with inactive state.
            $('#blocks-view-wrapper #block-list-wrapper .list-group').prepend(
                '<a href="#" class="list-group-item" >Current Block ' + '<span class="pull-right">' + unixTimeToUTC(cache.recentBlocks[0]['Timestamp']) + '</span>' +
                '<div class="list-options">' +
                '<button class="btn btn-sm block-detial">View</button>' +
                '</div>' +
                '</a>'
            );
        }
        else {
            // Prepend new current block with active state.
            $('#blocks-view-wrapper #block-list-wrapper .list-group').prepend(
                '<a href="#" class="list-group-item active" >Current Block' + '<span class="pull-right">' + unixTimeToUTC(cache.recentBlocks[0]['Timestamp']) + '</span>' +
                '<div class="list-options">' +
                '<button class="btn btn-sm block-detial">View</button>' +
                '</div>' +
                '</a>'
            );

            //Block view page -> block detial.
            updateBlockViewBlockDetial(0);
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateBlockViewBlockDetial(index){
    try {
        //basci info.
        $('#block-basic-info-wrapper #block-info-height span').html(pad(cache.recentBlocks[index]['Height'], 10));
        $('#block-basic-info-wrapper #block-info-hash span').html(cache.recentBlocks[index]['Hash'].substr(0, 15) + '...');
        $('#block-basic-info-wrapper #block-info-hash').attr('title', cache.recentBlocks[index]['Hash']);
        $('#block-basic-info-wrapper #block-info-node span').html(cache.recentBlocks[index]['Hostname']);
        $('#block-basic-info-wrapper #block-info-movement span').html(cache.recentBlocks[index]['TXCount']);
        $('#block-basic-info-wrapper #block-info-time span').html(unixTimeToUTC(cache.recentBlocks[index]['Timestamp']));
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

    //transactions
    $('#block-view-transactions-wrapper tbody').empty();
    try {
        for (var i = 0; i < cache.recentBlocks[index]['Transactions'].length; i++) {
            $('#block-view-transactions-wrapper tbody').append(
                '<tr>' +
                '<td>' + cache.recentBlocks[index]['Transactions'][i][7].substr(0, 10) + '...' + '</td>' +
                '<td>' + cache.recentBlocks[index]['Transactions'][i][3] + '</td>' +
                '<td>' + cache.recentBlocks[index]['Transactions'][i][4] + '</td>' +
                '<td>' + cache.recentBlocks[index]['Transactions'][i][1] + '</td>' +
                '<td>' + cache.recentBlocks[index]['Transactions'][i][2] + '</td>' +
                '<td>' + cache.recentBlocks[index]['Transactions'][i][9] + '</td>' +
                '<td>' + 'externalRef' + '</td>' +
                    //'<td class="animated fadeIn">' + cache.last12Transactions[i]['contingent'] + '</td>' +
                '<td>' + commaSeparateNumber(cache.recentBlocks[index]['Transactions'][i][5]) + '</td>' +
                '<td>' + unixTimeToUTC(cache.recentBlocks[index]['Timestamp']) + '</td>' +
                '</tr>'
            );
        }
    }
    catch(e){

    }
}

/**********************************************************************************************
 *
 * Paint updates to the DOM - State View
 *
 **********************************************************************************************/

function updateStateViewBalanceList(){
    try {
        // Remove the last if necessary.
        if ($('#state-view-wrapper #state-list-wrapper .list-group .list-group-item').length == 20)
            $('#state-view-wrapper #state-list-wrapper .list-group .list-group-item:nth-child(20)').remove();

        if (cache.recentStateViews.length > 1) {
            if ($('#state-view-wrapper #state-list-wrapper .list-group .list-group-item.active').length == 0)
                cache.stateViewSelected = false;

            // Update the last Current Block text.
            try {
                $('#state-view-wrapper #state-list-wrapper .list-group .list-group-item:nth-child(1)').html(pad(cache.recentStateViews[1]['Height'], 8) + '<span class="pull-right">' + unixTimeToUTC(cache.recentStateViews[1]['Timestamp']) + '</span>' + '<div class="list-options">' + '<button class="btn btn-sm bv-detial">View</button>' + '</div>' + '</a>');
            }
            catch (e) {
            }

            if (cache.stateViewSelected === false) { //If user not chosen a state view.


                $('#state-view-wrapper #state-list-wrapper .list-group .list-group-item').removeClass('active');
                // Prepend new current block with active state.
                $('#state-view-wrapper #state-list-wrapper .list-group').prepend(
                    '<a href="#" class="list-group-item active">Current State View ' + '<span class="pull-right">' + unixTimeToUTC(cache.recentStateViews[0]['Timestamp']) + '</span>' +
                    '<div class="list-options">' +
                    '<button class="btn btn-sm bv-detial">View</button>' +
                    '</div>' +
                    '</a>'
                );

                //State view page -> state asset list.
                updateStateViewAssetList(0);

            } else {
                // Prepend new current block with inactive state.
                $('#state-view-wrapper #state-list-wrapper .list-group').prepend(
                    '<a href="#" class="list-group-item">Current State View ' + '<span class="pull-right">' + unixTimeToUTC(cache.recentStateViews[0]['Timestamp']) + '</span>' +
                    '<div class="list-options">' +
                    '<button class="btn btn-sm bv-detial">View</button>' +
                    '</div>' +
                    '</a>'
                );
            }
        }
        else {
            // Prepend new current block with active state.
            $('#state-view-wrapper #state-list-wrapper .list-group').prepend(
                '<a href="#" class="list-group-item active">Current State View  ' + '<span class="pull-right">' + unixTimeToUTC(cache.recentStateViews[0]['Timestamp']) + '</span>' +
                '<div class="list-options">' +
                '<button class="btn btn-sm bv-detial">View</button>' +
                '</div>' +
                '</a>'
            );

            //State view page -> balance asset list.
            updateStateViewAssetList(0);
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateStateViewAssetList(index){
    try {
        // Store the first asset name.
        var activeAssetName = $('#state-view-wrapper #asset-list-wrapper .list-group .list-group-item.active').attr('data-asset-name');

        //Empty the div.
        $('#state-view-wrapper #asset-list-wrapper .list-group').empty();

        // sort the asset.
        var keys = getKeyOfObject(cache.recentStateViews[index]['assetList']);
        quicksort(keys, 0, keys.length - 1, "", cache.assetSortOrder);

        // Add asset to the div.
        $.each(keys, function (i, assetName) {
            if ((activeAssetName == undefined) || (cache.recentStateViews[index]['assetList'][activeAssetName] == undefined)) {
                activeAssetName = assetName;
            }
            $('#state-view-wrapper #asset-list-wrapper .list-group').append(
                '<a class="list-group-item" data-asset-name="' + assetName + '">' + assetName + '</a>'
            );
        });

        $('#state-view-wrapper #asset-list-wrapper .list-group [data-asset-name="' + activeAssetName + '"]').addClass('active');

        //show the asset detail.
        updateStateViewAssetDetail(index, activeAssetName);

        // Record the previous current asset name.
        cache.previousChosenAssetNameIndex = activeAssetName;
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateStateViewAssetDetail(stateviewIndex, assetNameIndex)
{
    try {
        var assetDetail = cache.recentStateViews[stateviewIndex]['assetList'][assetNameIndex];
        var lastAssetDetail = cache.recentStateViews[stateviewIndex + 1]['assetList'][assetNameIndex];

        var sortNameArr = [cache.assetDetailSortName];
        var sortOrderArr = [cache.assetDetailSortOrder, cache.assetDetailSortOrder];
        if (cache.assetDetailSortName == 'address') {
            sortNameArr.push('namespace');
        }
        else if (cache.assetDetailSortName == 'namespace') {
            sortNameArr.push('address');
        }

        quicksort(assetDetail, 0, assetDetail.length - 1, sortNameArr, sortOrderArr);
        quicksort(lastAssetDetail, 0, lastAssetDetail.length - 1, sortNameArr, sortOrderArr);

        //empty the body.
        $('#state-view-assets-info-wrapper tbody').empty();

        // Randomise Certified.
        var cert = ['Certex', 'Bankex', 'Tradex'];
        var cert = ['Certex', 'Bankex', 'Tradex'];

        $.each(assetDetail, function (i, assetBalance) {
            var rand = parseInt(assetBalance['address'].substr(assetBalance['address'].length - 1).charCodeAt(0)) % 3;

            $('#state-view-assets-info-wrapper tbody').append(
                '<tr data-address="' + assetBalance['address'] + '" data-index="' + i + '">' +
                '<td>' + assetBalance['address'] + '</td>' +
                '<td>' + assetBalance['namespace'] + '</td>' +
                '<td>' + assetBalance['asset'] + '</td>' +
                '<td>' + cert[rand] + '</td>' +
                '<td>' + commaSeparateNumber(assetBalance['amount']) + '</td>' +
                '</tr>'
            );

            // If balance change.
            if (stateviewIndex == 0) {
                if (lastAssetDetail[i]['amount'] != assetBalance['amount']) {

                    $('#state-view-assets-info-wrapper tbody tr[data-index="' + i + '"]').addClass('tablerow_hover');

                    setTimeout(function () {
                        $('#state-view-assets-info-wrapper tbody tr[data-index="' + i + '"]').removeClass('tablerow_hover');
                        setTimeout(function () {
                            $('#state-view-assets-info-wrapper tbody tr[data-index="' + i + '"]').addClass('tablerow_hover');
                            setTimeout(function () {
                                $('#state-view-assets-info-wrapper tbody tr[data-index="' + i + '"]').removeClass('tablerow_hover');
                            }, 500);
                        }, 500);
                    }, 500);
                }
            }
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**********************************************************************************************
 *
 * Paint updates to the DOM - Setting page
 *
 **********************************************************************************************/
function settingPageKnownHostDetailUpdate(knownNodes){
    try {
        $('#setting-page-know-hosts-wrapper tbody').empty();

        $.each(knownNodes, function (i, nodeDetail) {
            $('#setting-page-know-hosts-wrapper tbody').append(
                '<tr>' +
                '<td>' + nodeDetail[0] + '</td>' +
                '<td>' + nodeDetail[1] + '</td>' +
                '</tr>'
            );
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function settingPageUpdateChainID(chainID){
    $('#setting-chainID').html(chainID);
}

function SettingPageUpdateScenarioState(){
    try {
        for (var i = 0; i <= cache.scenarioState.length - 1; i++) {
            // Get the scenario number
            var scenarioNo = parseInt(cache.scenarioState[i][0]) - 1;

            if (cache.scenarioState[i][1]) {
                $('#paymentSystemSettingWrapper [data-id="' + cache.paymentSystemsArr[scenarioNo] + '"] div').bootstrapSwitch('setState', true);
            }
            else {
                $('#paymentSystemSettingWrapper [data-id="' + cache.paymentSystemsArr[scenarioNo] + '"] div').bootstrapSwitch('setState', false);
            }
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateScenarioBarchart(protocolCount){
    try {
        if(cache.newBlockTimeTaken !== 0) {
            var scenarioNo;
            var index = cache.chainID == 0 ? 0 : 1;
            $.each(protocolCount, function (protocolName, Count) {
                scenarioNo = $.inArray(protocolName, cache.paymentSystemsArr);
                if (scenarioNo !== -1)
                    cache.barChartData[index]['data'][scenarioNo] = [scenarioNo + 1, (Count * 86400 / cache.newBlockTimeTaken)];
            });
        }
    }catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }

    redrawBarChart();
}

function updateXChainDetail(xcdetails){
    try {
        $('#setting-page-xchain-detail-wrapper tbody').empty();

        $.each(xcdetails, function (i, xcdetail) {
            $('#setting-page-xchain-detail-wrapper tbody').append(
                '<tr>' +
                '<td>' + xcdetail[0] + '</td>' +
                '<td>' + xcdetail[1] + '</td>' +
                '<td>' + xcdetail[2] + '</td>' +
                '<td>' + (xcdetail[3] >>> 0).toString(2) + '</td>' +
                '<td>' + xcdetail[4] + '</td>' +
                '</tr>'
            );
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**********************************************************************************************
 *
 * Paint updates to the DOM - Transaction page
 *
 **********************************************************************************************/
function transactionPageUpdateTransactionsTable(){
    try {
        if ($('#transaction-page-latest-transactions-wrapper tbody tr').length >= 50) {
            // Remove the last 5 transactions, and add new 5 transactions.
            for (var i = 0; i < 5; i++) {
                $('#transaction-page-latest-transactions-wrapper tbody tr:last-child').remove();
            }
        }

        for (var i = 0; i < 5 && i < cache.latestTransactions[0]['Transaction'].length; i++) {

            $('#transaction-page-latest-transactions-wrapper tbody').prepend(
                '<tr>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][7].substr(0, 10) + '...' + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][3] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][4] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][1] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][2] + '</td>' +
                '<td>' + cache.latestTransactions[0]['Transaction'][i][9] + '</td>' +
                '<td>' + 'externalRef' + '</td>' +
                    //'<td class="animated fadeIn">' + cache.last12Transactions[i]['contingent'] + '</td>' +
                '<td>' + commaSeparateNumber(cache.latestTransactions[0]['Transaction'][i][5]) + '</td>' +
                '<td>' + unixTimeToUTC(cache.latestTransactions[0]['Timestamp']) + '</td>' +
                '</tr>'
            );
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}


/**********************************************************************************************
 *
 * Paint updates to the DOM - Log page
 *
 **********************************************************************************************/
//update state for log page - log area, from data received form server
function logPageLogAreaUpdate(bitMap){
    try {
        // use the bit map to find all the state of log area.
        $.each($('#log-tab-pane .make-switch'), function (i, element) {
            var bit = cache.logAreaAndLevelMap[$(element).attr('data-id')];
            //perform or bit-wise operation.
            var state = bit & bitMap;

            if (state != 0)
                $('#log-tab-pane [data-id="' + $(element).attr('data-id') + '"] div').bootstrapSwitch('setState', true);
            else
                $('#log-tab-pane [data-id="' + $(element).attr('data-id') + '"] div').bootstrapSwitch('setState', false);
        });
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

//update log level in log page, from data received from server.
function logPageLogLevelUpdate(logLevel){
    $('#log-level-selector').val(cache.logLevelMap[logLevel]);
    $('#log-level-selector').selectpicker('refresh');
}

//update diagnosis state.
function logPageDiagnosisStateUpdate(state){
    try {
        if (state != 0)
            $('#receiveStatusUpdateSwitch div').bootstrapSwitch('setState', true);
        else
            $('#receiveStatusUpdateSwitch div').bootstrapSwitch('setState', false);
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

//update overdrive state.
function logPageOverdriveStateUpdate(state){
    try {
        if (state != 0)
            $('#overdriveSwitch div').bootstrapSwitch('setState', true);
        else
            $('#overdriveSwitch div').bootstrapSwitch('setState', false);
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

//update overdrive multiply.
function logPageOverdriveMultiplyUpdate(multiply){
    if(multiply != 0)
        $('#overdriveSpiner').val(multiply);
}

/**********************************************************************************************
 *
 * Paint updates to the DOM - Wallet Page
 *
 **********************************************************************************************/

function updateDropDown(div){
    updateIssuerDropDown(div);
}

function updateIssuerDropDown(div){
    try {
        //Empty the Issuers list
        $(div + ' select.issuer-select').empty();

        $(div + ' select.issuer-select').append(
            '<option value=""></option>'
        );

        //paint the issuers to DOM
        var sortedIssuer = sortedObjectKey(cache.indexedStateView);
        $.each(sortedIssuer, function (index, issuer) {
            $(div + ' select.issuer-select').append(
                '<option value="' + issuer + '">' + issuer + '</option>'
            );
        });

        $(div + ' select.issuer-select').selectpicker('refresh');
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateSecurityDropDown(div,selectedIssuer){
    try {
        //Empty the security list
        $(div + ' select.security-select').empty();

        $(div + ' select.security-select').append(
            '<option value=""></option>'
        );

        //paint the security to DOM
        if (selectedIssuer != '') {
            var sortedSecurity = sortedObjectKey(cache.indexedStateView[selectedIssuer]);
            $.each(sortedSecurity, function (index, security) {
                $(div + ' select.security-select').append(
                    '<option value="' + security + '">' + security + '</option>'
                );
            });
        }

        $(div + ' select.security-select').selectpicker('refresh');
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

function updateAddressDropDown(div,selectedIssuer, selectedSecurity){
    try {
        //Empty the ToAddress list
        $(div).empty();

        $(div).append(
            '<option value=""></option>'
        );

        if (selectedIssuer != '' && selectedSecurity != '') {
            //paint the issuers to DOM
            $.each(cache.indexedStateView[selectedIssuer][selectedSecurity], function (toAddress) {
                $(div).append(
                    '<option value="' + toAddress + '">' + toAddress + '</option>'
                );
            });
        }

        $(div).selectpicker('refresh');
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * update receiving address balance - receiving tab.
 * @param tabPaneID
 * @param div
 * @param selectedIssuer
 * @param selectedSecurity
 * @param selectedAddress
 * @param isUpdate
 */
function updateAddressBalance(tabPaneID,div, selectedIssuer,selectedSecurity, selectedAddress, isUpdate){
    try {
        if (isUpdate) {
            var selectedIssuer = $((tabPaneID ) + ' select.issuer-select').val();
            var selectedSecurity = $((tabPaneID ) + ' select.security-select').val();
            var selectedAddress = (tabPaneID == '#receivingTab') ? $((tabPaneID ) + ' select.toAddress-select').val() : $((tabPaneID ) + ' select.fromAddress-select').val();
            var oldBalance = $((tabPaneID ) + ' .balance').html();
        }

        if (selectedAddress != '' && selectedIssuer != '' && selectedSecurity != '') {
            // Check if this address is empty, if not render the balance, otherwise render balance: 0
            if (cache.indexedStateView[selectedIssuer][selectedSecurity][selectedAddress] != undefined)
                $(div).html(
                    '<tr>' +
                    '<td>' + selectedAddress + '</td>' +
                    '<td>' + selectedIssuer + '</td>' +
                    '<td>' + selectedSecurity + '</td>' +
                    '<td class="balance">' + commaSeparateNumber(cache.indexedStateView[selectedIssuer][selectedSecurity][selectedAddress]) + '</td>' +
                    '</tr>'
                );
            else {
                $(div).html(
                    '<tr>' +
                    '<td>' + selectedAddress + '</td>' +
                    '<td>' + selectedIssuer + '</td>' +
                    '<td>' + selectedSecurity + '</td>' +
                    '<td class="balance">0</td>' +
                    '</tr>'
                );
            }

            //flash the table row, if the balance changed.
            if (isUpdate && oldBalance != cache.indexedStateView[selectedIssuer][selectedSecurity][selectedAddress] && cache.indexedStateView[selectedIssuer][selectedSecurity][selectedAddress] != undefined) {

                $(div + ' tr').addClass('tablerow_hover');

                setTimeout(function () {
                    $(div + ' tr').removeClass('tablerow_hover');
                    setTimeout(function () {
                        $(div + ' tr').addClass('tablerow_hover');
                        setTimeout(function () {
                            $(div + ' tr').removeClass('tablerow_hover');
                        }, 500);
                    }, 500);
                }, 500);
            }

        }
        else {
            $(div).empty();
        }
    } catch(err){
        if(cache.prod == false) console.log("Error : " + err.message + " line " + err.lineNumber);
    }
}

/**
 * Update corp address balance - corp action tab.
 */
function CorpUpdateAddressBalance(tabPaneID,div, selectedIssuer,selectedSecurity, isUpdate){
   try {
       // Get active corp action.
       var corpAction = $('.accordion-toggle.active').attr('for');
       // Get Selected currency.
       var selectedCurrency = $('#dividend-currency').val();

       if(corpAction == 'dividend')
           $('.dividend-show').show();
       else
           $('.dividend-show').hide();

       if (isUpdate) {
           var selectedIssuer = $((tabPaneID ) + ' select.issuer-select').val();
           var selectedSecurity = $((tabPaneID ) + ' select.security-select').val();

           if (selectedIssuer != '' && selectedSecurity != '' && cache.indexedStateView[selectedIssuer][selectedSecurity] != undefined) {
               $.each(cache.indexedStateView[selectedIssuer][selectedSecurity], function (address) {

                   //check if address rendered before.
                   var addrDom = $((tabPaneID ) + ' tr[data-address="' + address + '"]');

                   var oldBalance = (addrDom.length == 0) ? undefined : $((tabPaneID ) + ' tr[data-address="' + address + '"] .balance').html();
                   var oldCurrencyBl = (addrDom.length == 0) ? undefined : $((tabPaneID ) + ' tr[data-address="' + address + '"] .currency-balance').html();

                   oldBalance = commasSeparatedToInt(oldBalance);
                   if (oldCurrencyBl !== undefined) oldCurrencyBl = commasSeparatedToInt(oldCurrencyBl);

                   addrDom.remove();

                   if(corpAction == 'dividend'){
                       // Set currency balance to 0, when it is not defined.
                       if(cache.indexedStateView[cache.dividendInfo[selectedCurrency]] == undefined) {
                           cache.indexedStateView[cache.dividendInfo[selectedCurrency]] = {};
                           cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency] = {};
                       }

                       if(cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency][address] == undefined)
                           cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency][address] = 0;

                       $(div).append(
                           '<tr data-address="' + address + '">' +
                           '<td>' + address + '</td>' +
                           '<td>' + selectedIssuer + '</td>' +
                           '<td>' + selectedSecurity + '</td>' +
                           '<td class="balance">' + commaSeparateNumber(cache.indexedStateView[selectedIssuer][selectedSecurity][address]) + '</td>' +
                           '<td>' + selectedCurrency + '</td>' +
                           '<td class="currency-balance">' + commaSeparateNumber(cache.indexedStateView[[cache.dividendInfo[selectedCurrency]]][selectedCurrency][address]) + '</td>' +
                           '</tr>'
                       );
                   }
                   else{
                       $(div).append(
                           '<tr data-address="' + address + '">' +
                           '<td>' + address + '</td>' +
                           '<td>' + selectedIssuer + '</td>' +
                           '<td>' + selectedSecurity + '</td>' +
                           '<td class="balance">' + commaSeparateNumber(cache.indexedStateView[selectedIssuer][selectedSecurity][address]) + '</td>' +
                           '</tr>'
                       );
                   }



                   //flash the table row, if the balance changed.
                   if (isUpdate &&
                       (
                        (oldBalance != cache.indexedStateView[selectedIssuer][selectedSecurity][address] && oldBalance != undefined)
                       ||
                        (oldCurrencyBl != cache.indexedStateView[[cache.dividendInfo[selectedCurrency]]][selectedCurrency][address] && oldCurrencyBl!= undefined)
                       )) {

                       $(div + ' tr[data-address="'+address+'"]').addClass('tablerow_hover');

                       setTimeout(function () {
                           $(div + ' tr[data-address="'+address+'"]').removeClass('tablerow_hover');
                           setTimeout(function () {
                               $(div + ' tr[data-address="'+address+'"]').addClass('tablerow_hover');
                               setTimeout(function () {
                                   $(div + ' tr[data-address="'+address+'"]').removeClass('tablerow_hover');
                               }, 500);
                           }, 500);
                       }, 500);
                   }
               });

           }
           else {
               $(div).empty();
           }
       }
       else{
           $(div).empty();
           if (selectedIssuer != '' && selectedSecurity != '' && cache.indexedStateView[selectedIssuer][selectedSecurity] != undefined) {
               $.each(cache.indexedStateView[selectedIssuer][selectedSecurity], function (address) {

                   if(corpAction == 'dividend'){
                       // Set currency balance to 0, when it is not defined.
                       if(cache.indexedStateView[cache.dividendInfo[selectedCurrency]] == undefined) {
                           cache.indexedStateView[cache.dividendInfo[selectedCurrency]] = {};
                           cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency] = {};
                       }

                       if(cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency][address] == undefined)
                           cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency][address] = 0;

                       $(div).append(
                           '<tr data-address="' + address + '">' +
                           '<td>' + address + '</td>' +
                           '<td>' + selectedIssuer + '</td>' +
                           '<td>' + selectedSecurity + '</td>' +
                           '<td class="balance">' + commaSeparateNumber(cache.indexedStateView[selectedIssuer][selectedSecurity][address]) + '</td>' +
                           '<td>' + selectedCurrency + '</td>' +
                           '<td class="currency-balance">' + commaSeparateNumber(cache.indexedStateView[cache.dividendInfo[selectedCurrency]][selectedCurrency][address]) + '</td>' +
                           '</tr>'
                       );
                   }
                   else{
                       $(div).append(
                           '<tr data-address="' + address + '">' +
                           '<td>' + address + '</td>' +
                           '<td>' + selectedIssuer + '</td>' +
                           '<td>' + selectedSecurity + '</td>' +
                           '<td class="balance">' + commaSeparateNumber(cache.indexedStateView[selectedIssuer][selectedSecurity][address]) + '</td>' +
                           '</tr>'
                       );
                   }
               });

           }
           else {
               $(div).empty();
           }
       }
   }
    catch(e){
        if (cache.prod == false){
            console.log(e.lineNumber + ': ' + e.message);
        }
    }
}