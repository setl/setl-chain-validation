/**
 * Created by nicholas on 09/05/2014.
 *
 * Note, must be 'include' after the script for GibberishAES and BigInts
 *
 * Modified by Ming on 18/07/2015
 */

// Instantiate default handlers :-

//if (document.SetlSocketCallback == undefined)
//{
//    document.SetlSocketCallback = new SetlCallbackClass();
//}
//
//if (document.SetlSocket == undefined)
//{
//    document.SetlSocket = new SetlSocketClass(false, false, false, false, document.SetlSocketCallback);
//}

/* Socket Class */
function SetlSocketClass(thisProtocol, thisHostname, thisPort, thisRoute, thisCallbackHandler)
{
    /*
     * Luthor Socket Class
     *
     * XX = New SetlSocketClass(
     *               thisProtocol          'ws:' or 'wss:', defaults to 'ws:'
     *               thisHostname          defaults to 'document.location.hostname', i.e. the current host.
     *               thisPort              defaults to 12500 (Arbitrary port chosen by me.)
     *               thisRoute             defaults to 'db' (LuthorWebSocketServer route for DB interaction)
     *               thisCallbackHandler   Instance of SetlCallbackClass.
     *               )
     *
     * Properties :
     *
     * WebSocket     Returns the raw Websocket object.
     *
     * Methods :
     *
     * send(msg)               Pass through to the WebSocket.send() method.
     * sendRequest(Request)    Request is a JS Request object. JSON encodes and encrypts Request before sending
     *                         to the server. Using this method will allow SetlSocketClass to
     *                         Establish a new WebSocket, initialise encryption and then send this request if the
     *                         WebSocket is not open for some reason.
     * openWebSocket([UserCallback])
     *                         Open new WebSocket. Optional parameter is a callback function, called after the encryption
     *                         has been established.
     * closeWebSocket          Closes the web socket.
     *
     * */

    var _this = this;

    thisHostname = thisHostname || document.location.hostname;
    thisPort = thisPort || 12505;
    thisRoute = thisRoute || 'updateSocket';

    thisProtocol = thisProtocol || ((document.location.protocol == 'https:') ? 'wss:' : 'ws:');

    var CallbackHandler = thisCallbackHandler;
    var thisURL = thisProtocol + '//' + thisHostname + ':' + thisPort + '/' + thisRoute;
    var _initialProtocol = thisProtocol;
    var _initialHostname = thisHostname;
    var _initialPort = thisPort;
    var _initialRoute = thisRoute;
    var _haveTriedWs = false;
    var _haveTriedIFrame = false;

    var WebSocketConn = false;
    var InitMessageID = false;

    _this.encryption = {};
    var encryption = _this.encryption;

    var _MessageqQueue = [];
    var _initialising = false;
    var _hasConnected = false;
    var _connectTries = 0;

    if (_initialProtocol == 'ws:') _haveTriedWs = true;

    var InitResponse = function (ID, message, UserData)
    {

        // Init response
        if ((message.Data != undefined) && (message.Data))
        {
            encryption.ServerPublicKey = str2bigInt(message.Data, 10, 80);
            encryption.MyPrivateKey = bigInt2str(
                powMod(
                    encryption.ServerPublicKey,
                    encryption.MySecret,
                    encryption.MyPrime), 10);

        }

        // handleGenericResponse(ID, message, UserData);
    };

    // UserOn... functions, not implimented.

    var userOnOpen = false;
    var userOnClose = false;
    var userOnError = false;
    var userOnMessage = false;

    var defaultOnOpen = function (e)
    {
        if (!document.dataCache.prod) console.log("Connection established!");
    };

    var defaultOnClose = function (e)
    {
        console.log("Connection closed!");
        WebSocketConn = false;
        _initialising = false;
    };

    var defaultOnError = function (e)
    {
        //console.log(e.data);
    };

    var defaultOnMessage = function (e)
    {
        // console.log(e.data);

        var message;
        var decoded;
        var ID = 0;
        var isEncrypted = false;

        if (encryption.ServerPublicKey === false)
        {
            if (e.data.substr(0, 3) == "LZ_")
            {
                message = JSON.parse(JXG.Util.UTF8.decode(JXG.decompress(e.data.substr(3))));
            }
            else
            {
                message = JSON.parse(e.data);
            }
        }
        else
        {
            decoded = GibberishAES.dec(e.data, encryption.MyPrivateKey);
            isEncrypted = true;

            if (decoded.substr(0, 3) == "LZ_")
            {
                message = JSON.parse(JXG.Util.UTF8.decode(JXG.decompress(decoded.substr(3))));
            }
            else
            {
                message = JSON.parse(decoded);
            }
        }

        if (message.RequestID != undefined)
        {
            ID = message.RequestID;
        }

        // Only process callbacks if ID is the InitID or Encryption is in place

        try
        {
            if (!document.dataCache.prod) console.log('On Message : ' + nz(message.Request.MessageBody.RequestName, message.Request.MessageType))
        }
        catch (e)
        {
        }

        if ((true) || (isEncrypted) || (ID === InitMessageID))
        {
            if (CallbackHandler)
            {
                CallbackHandler.handleEvent(ID, message);
            }
        }

    };

    this.WebSocket = function ()
    {
        return WebSocketConn;
    };

    this.send = function (data)
    {
        return WebSocketConn.send(data);
    };

    this.sendRequest = function (Request)
    {
        try
        {
            if (!document.dataCache.prod) console.log("sendRequest() : " + Request['MessageBody']['RequestName']);
        }
        catch (e)
        {
        }

        if (_initialising)
        {
            // console.log("  sendRequest(). Enqueueing message.");
            _MessageqQueue.push(Request);
        }
        else
        {
            if (!WebSocketConn)
            {
                _MessageqQueue.push(Request);

                // console.log("    sendRequest(). openWebSocket().");

                _this.openWebSocket();

            }
            else
            {
                // console.log("  sendRequest(). Preparing Message.");

                Request['Compress'] = 'lz';

                var MessageText = JSON.stringify(Request);

                /*      // We are not compressing the requests, just the responses.

                 if (GetKeyValue(Request, 'Compress', '') == 'lz')
                 {
                 MessageText = LZString.compressToBase64(MessageText);
                 }
                 */

                if (encryption.MyPrivateKey != false)
                {
                    MessageText = GibberishAES.enc(MessageText, encryption.MyPrivateKey);
                }

                // console.log("  sendRequest(). Sending Message.");

                return WebSocketConn.send(MessageText);
            }
        }

    };

    this.openWebSocket = function (UserCallback, UserCallbackData)
    {
        // console.log("openWebSocket()");

        if (WebSocket == undefined)
        {
            return false;
        }

        if (!WebSocketConn)
        {
            if (!_initialising)
            {
                try
                {
                    _initialising = true;
                    try
                    {
                        // If we are having problems connecting, try using non-secure socket, if we are on a secure site.
                        if((!_hasConnected) && (_initialProtocol == 'wss:') && (_connectTries > 1) && ((_connectTries % 2) == 0))
                        {
                            if ((_connectTries % 4) != 0)
                            {
                                thisURL =  'ws://' + _initialHostname + ':80/' + _initialRoute;
                                _haveTriedWs = true;
                            }
                            else
                            {
                                thisURL = _initialProtocol + '//' + _initialHostname + ':' + _initialPort + '/' + _initialRoute;
                            }
                        }

                        _connectTries+=1;
                        WebSocketConn = new WebSocket(thisURL);
                    }
                    catch (e)
                    {
                        // console.log("   Websocket error on instantiation.");

                        // Socket failed to open.
                        _initialising = false;
                        WebSocketConn = false;
                        return false;
                    }

                    if (WebSocketConn.readyState == WebSocketConn.CLOSED)
                    {
                        // console.log("   Websocket failed to open.");

                        // Socket failed to open.
                        _initialising = false;
                        WebSocketConn = false;
                        return false;
                    }

                    _this.encryption.MyPrime = false;
                    _this.encryption.MySecret = false;
                    _this.encryption.MyPrivateKey = false;
                    _this.encryption.MyPublicKey = false;
                    _this.encryption.ServerPublicKey = false;
                    _this.encryption.Generator = 2;

                    encryption = _this.encryption;

                    WebSocketConn.onopen = function (e)
                    {
                        _hasConnected = true;
                        InitMessageID = thisCallbackHandler.getUniqueID();
                        var gBase = parseInt((Math.random() * 100000000000));
                        var Request = {};
                        var MessageText;

                        //encryption.MyPrime = encryption.MyPrime || str2bigInt("155172898181473697471232257763715539915724801966915404479707795314057629378541917580651227423698188993727816152646631438561595825688188889951272158842675419950341258706556549803580104870537681476726513255747040765857479291291572334510643245094715007229621094194349783925984760375594985848253359305585439638443", 10, 80);
                        //encryption.Generator = int2bigInt(gBase, 32, 80);
                        //encryption.MySecret = randBigInt(512, 1);
                        //encryption.MyPublicKey = powMod(encryption.Generator, encryption.MySecret, encryption.MyPrime);


                        thisCallbackHandler.addHandler(
                            InitMessageID,
                            function (pID, pMessage, pData)
                            {
                                InitResponse(pID, pMessage, pData);
                                _initialising = false;

                                if (Object.prototype.toString.call(UserCallback) == "[object Function]")
                                {
                                    UserCallback(pID, pMessage, UserCallbackData);
                                }

                                //console.log("Trigger CallbackHandler OnOpen()");

                                if (CallbackHandler)
                                {
                                    CallbackHandler.handleEvent('OnOpen', {});
                                }

                                //console.log("Queued message count = " + _MessageqQueue.length.toString());

                                while (_MessageqQueue.length > 0)
                                {
                                    //console.log("  Sending Queued message.");
                                    _this.sendRequest(_MessageqQueue.shift());
                                }
                            },
                            {});

                        //Request.MessageType = 'Initialise';
                        //Request.MessageHeader = '';
                        //Request.RequestID = InitMessageID;
                        //Request.MessageBody = {};
                        //
                        //Request.MessageBody.m = bigInt2str(encryption.MyPublicKey, 10);
                        //Request.MessageBody.g = bigInt2str(encryption.Generator, 10);
                        //
                        //MessageText = JSON.stringify(Request);

                        //console.log("Sending Encryption Init message.");

                        //WebSocketConn.send(MessageText);


                        _initialising = false;

                        defaultOnOpen(e);

                        if (CallbackHandler)
                        {
                            CallbackHandler.handleEvent('OnOpen', {});
                        }

                        //console.log("Queued message count = " + _MessageqQueue.length.toString());

                        while (_MessageqQueue.length > 0)
                        {
                            //console.log("  Sending Queued message.");
                            _this.sendRequest(_MessageqQueue.shift());
                        }



                        if (userOnOpen) userOnOpen(e);

                    };

                    WebSocketConn.onclose = function (e)
                    {
                        defaultOnClose(e);

                        if (_hasConnected)
                        {
                            if (CallbackHandler)
                            {
                                CallbackHandler.handleEvent('OnClose', {});
                            }

                            if (userOnClose) userOnClose(e);
                        }
                    };

                    WebSocketConn.onerror = function (e)
                    {
                        if (CallbackHandler)
                        {
                            CallbackHandler.handleEvent('OnError', {});
                        }

                        if (userOnError) userOnError(e);

                        defaultOnError(e);
                    };

                    WebSocketConn.onmessage = function (e)
                    {
                        defaultOnMessage(e);
                        if (userOnMessage) userOnMessage(e);
                    };
                }
                catch (e)
                {
                    showError("Error : " + e.message + ", socket.js, line " + e.lineNumber);
                }
            } // if (!_initialising)
        }
    };

    this.closeWebSocket = function ()
    {
        if (WebSocketConn)
        {
            WebSocketConn.close();
            WebSocketConn = false;
            _initialising = false;
        }
    };

    this.haveTriedWs = function() {return _haveTriedWs;};
    this.hasConnected = function() {return _hasConnected;};
    this.connectTries = function() {
        return _connectTries;
    };
    this.haveTriedIFrame = function(setValue)
    {
        if (setValue === true) _haveTriedIFrame = setValue;
        return _haveTriedIFrame
    };

    var __construct = function ()
    {
        _this.openWebSocket();
    }();

}

function SetlCallbackClass()
{
    /*
     Class to handle defined callbacks.

     One or more callback functions of the form function(ID, EventData, UserData) can be associated with an ID.
     When the handleEvent(ID, EventData) method is called, all callbacks associated with the given ID are called.
     If the ID is >= 1000, then the callbacks are deleted - i.e. one shot only.
     If no callback is associated with the given ID, then the default callbacks are called, these being the callbacks associated with ID = 0.
     events on IDs between 1 and 999 act as normal except that the callbacks are re-usable (not deleted).

     The class will return an incrementing ID via the 'getUniqueID()' property to help you maintain unique IDs for your events.

     addHandler(ID, Callback, UserData) //  UserData passed through to the callback function.
     removeHandler(ID, Callback)        //
     removeAllHandlers(ID)              //
     handleEvent(ID, EventData)         //

     */

    var _this = this;
    var CallbackCache = {};
    var UniqueID;
    var MaxReservedID = 999;

    var __construct = function ()
    {
        UniqueID = MaxReservedID + 1; // Initial value.
    }();

    this.getUniqueID = function ()
    {
        return UniqueID++;
    };

    this.isNumber = function (n)
    {
        return !isNaN(parseFloat(n)) && isFinite(n);
    };

    this.addHandler = function (ID, Callback, UserData)
    {
        try
        {
            if (!(this.isNumber(ID)))
            {
                ID = ID.toUpperCase();
            }

            CallbackCache[ID] = CallbackCache[ID] || [];
            if (Callback) CallbackCache[ID].push({ID: ID, Callback: Callback, UserData: UserData});
            return ID;
        }
        catch (e)
        {
        }
        return (-1);
    };

    this.removeHandler = function (ID, Callback)
    {
        var rVal = 0;

        try
        {
            if (!(this.isNumber(ID)))
            {
                ID = ID.toUpperCase();
            }

            if (CallbackCache[ID])
            {
                var thisCallbackObject;

                for (var index = 0; index < CallbackCache[ID].length; ++index)
                {
                    try
                    {
                        if (index in CallbackCache[ID])
                        {
                            thisCallbackObject = CallbackCache[ID][index];

                            if (thisCallbackObject.Callback == Callback)
                            {
                                delete CallbackCache[ID][index];
                                rVal++;
                            }
                        }
                    }
                    catch (e)
                    {
                        rVal = (-1);
                    }
                }
            }
        }
        catch (e)
        {
            rVal = (-1);
        }

        return rVal;
    };

    this.removeAllHandlers = function (ID)
    {
        var rVal = 0;

        try
        {
            if (!(this.isNumber(ID)))
            {
                ID = ID.toUpperCase();
            }

            if (CallbackCache[ID])
            {
                delete CallbackCache[ID];
                rVal++;
            }
        }
        catch (e)
        {
            rVal = (-1);
        }

        return rVal;
    };

    this.handleEvent = function (ID, EventData)
    {
        /*
         Call functions associated with this ID, Calls functions associated with ID = 0 if no other callback set.
         Callbacks are deleted when handled, except for ID = 0;
         */

        if (!(this.isNumber(ID)))
        {
            ID = ID.toUpperCase();
        }

        var rVal = 0;
        var thisID = ID;

        if ((!CallbackCache[ID]) && (this.isNumber(ID)))
        {
            thisID = 0;
        }

        if (CallbackCache[ID])
        {
            var thisCallbackObject;

            for (var index = 0; index < CallbackCache[ID].length; ++index)
            {
                try
                {
                    if (index in CallbackCache[ID])
                    {
                        thisCallbackObject = CallbackCache[ID][index];

                        if ((thisCallbackObject) && (thisCallbackObject.Callback))
                        {
                            thisCallbackObject.Callback(thisCallbackObject.ID, EventData, thisCallbackObject.UserData);
                            rVal++;
                        }
                    }
                }
                catch (e)
                {
                }
            }

            try
            {
                if ((thisID) && (isNumber(thisID)) && (thisID > MaxReservedID)) // Don't delete if is a number and <= MaxReservedID
                {
                    CallbackCache[ID] = [];
                    delete CallbackCache[ID];
                }
            }
            catch (e)
            {
            }
        }

        return rVal;
    };

}

/**********************************************************************************************
 *
 * Setl Messaging handlers
 *
 **********************************************************************************************/
function Setl_SubscribeForUpdate(ID, message, UserData)
{
    // ID will be 'ONOPEN'
    // Message will be {}
    // UserData will not be set.

    try
    {
        if ((document.SetlSocket != undefined) && (document.dataCache != undefined))
        {
            Setl_MessagingSubscribe(ID, message, UserData);
            getInitialSnapshotFromThroughSocket(); // Get the initial snapshot, and handle it.

        }
    }
    catch (e)
    {
        if(cache.prod == false) console.log("Error : " + e.message + "socket.js, line " + e.lineNumber);
    }
}

function Setl_MessagingSubscribe(ID, message, UserData)
{
    // Any Message with 'Token' in the body will serve to register the Token (User ID) with the WebSocket Server.
    // This message will also register for all Update messages.

    if (document.SetlSocket)
    {
        var Request = {};
        var messageID = document.SetlSocketCallback.getUniqueID();

        Request.MessageType = 'Subscribe';
        Request.MessageHeader = '';
        Request.MessageBody = {RequestName: '', Topic: ['block','balanceview','proposal','transaction','serverstatus']};
        Request.RequestID = messageID;

        //MessageText = GibberishAES.enc(JSON.stringify(Request), document.SetlSocket.encryption.MyPrivateKey);

        document.SetlSocket.sendRequest(Request);

    }
}


