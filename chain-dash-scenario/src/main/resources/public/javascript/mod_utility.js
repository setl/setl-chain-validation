/**********************************************************************************************
 *
 * Utility Functions     Created by nicholas on 16/06/2014.
 *
 **********************************************************************************************/

/**********************************************************************************************
 *
 * http://stackoverflow.com/questions/610406/javascript-equivalent-to-printf-string-format
 *
 * String.format('{0} is dead, but {1} is alive! {0} {2}', 'ASP', 'ASP.NET');
 * gives : ASP is dead, but ASP.NET is alive! ASP {2}
 **********************************************************************************************/

if (!String.format) {
String.format = function(format) {
var args = Array.prototype.slice.call(arguments, 1);
return format.replace(/{(\d+)}/g, function(match, number) {
return typeof args[number] != 'undefined'
  ? args[number]
  : match
  ;
});
};
}

/**********************************************************************************************
 *
 *
 *
 **********************************************************************************************/

function showRecaptcha(element)
  {
  try
    {
    if (Recaptcha != undefined)
      {
      Recaptcha.create("6LenLfwSAAAAAO2MlczKq06rUTLofO-j-7TJFVMR", element, {
        theme: "red",
        callback: Recaptcha.focus_response_field
      });
      }
    }
    catch (e)
    {
    }

  }

/**********************************************************************************************
 *
 * Encode xor and Decode xor functions
 *
 **********************************************************************************************/

function enc_xor(str)
  {
  var encoded = "";
  for (i = 0; i < str.length; i++)
    {
    var a = str.charCodeAt(i);
    var b = a ^ 123;    // bitwise XOR with any number, e.g. 123
    encoded = encoded + String.fromCharCode(b);
    }
  return encoded;
  }

function dec_xor(str)
  {
  var decoded = "";
  for (i = 0; i < str.length; i++)
    {
    var a = str.charCodeAt(i);
    var b = a ^ 123;    // bitwise XOR with any number, e.g. 123
    decoded = decoded + String.fromCharCode(b);
    }
  return decoded;
  }

/**********************************************************************************************
 *
 * https://brainwallet.github.io/
 *
 **********************************************************************************************/

function pad(str, len, ch)
  {
  var padding = '';
  for (var i = 0; i < len - str.length; i++)
    {
    padding += ch;
    }
  return padding + str;
  }

function getEncoded(pt, compressed)
  {
  var x = pt.getX().toBigInteger();
  var y = pt.getY().toBigInteger();
  var enc = integerToBytes(x, 32);
  if (compressed)
    {
    if (y.isEven())
      {
      enc.unshift(0x02);
      }
    else
      {
      enc.unshift(0x03);
      }
    }
  else
    {
    enc.unshift(0x04);
    enc = enc.concat(integerToBytes(y, 32));
    }
  return enc;
  }

function isHex(str) {
return !/[^0123456789abcdef]+/i.test(str);
}

function isBase58(str) {
return !/[^123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+/.test(str);
}

function isBase64(str) {
return !/[^ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=]+/.test(str) && (str.length % 4) == 0;
}

function isBin(str) {
return !/[^01 \r\n]+/i.test(str);
}

function isDec(str) {
return !/[^0123456789]+/i.test(str);
}

function getKeysFromPassphrase(passphrase)
  {
  var PUBLIC_KEY_VERSION = 0;
  var PRIVATE_KEY_VERSION = 0x80;
  var RVal = {};

  try
    {
    var secretExponent = Crypto.util.bytesToHex(Crypto.SHA256(passphrase, { asBytes: true }));

    var hash_str = pad(secretExponent, 64, '0');
    var hash = Crypto.util.hexToBytes(hash_str);
    var gen_compressed = true;
    var eckey = new Bitcoin.ECKey(hash);

    if (document.dataCache.secp256k1 == undefined)
      {
      document.dataCache.secp256k1 = getSECCurveByName("secp256k1");
      }

    var curve = document.dataCache.secp256k1;
    var gen_pt = curve.getG().multiply(eckey.priv);

    eckey.pub = getEncoded(gen_pt, gen_compressed);
    eckey.pubKeyHash = Bitcoin.Util.sha256ripe160(eckey.pub);

    var thisPublicKey = Crypto.util.bytesToHex(eckey.pub);

    var hash160 = eckey.getPubKeyHash();
    var addr = new Bitcoin.Address(hash160);
    addr.version = PUBLIC_KEY_VERSION;
    var thisAddress = addr.toString();

    var payload = hash;
    if (gen_compressed)
      payload.push(0x01);
    var sec = new Bitcoin.Address(payload);
    sec.version = PRIVATE_KEY_VERSION;
    var thisPrivateKey = addr.toString();

    RVal = {
      'passphrase' : passphrase,
      'privatekey' : thisPrivateKey,
      'publickey' : thisPublicKey,
      'address' : thisAddress
    };
    }
  catch (e)
    {}

  return RVal;
  }

/**********************************************************************************************
 *
 * Base64 Encode and Decode functions
 *
 **********************************************************************************************/

function base64_decode(data)
  {
  // From: http://phpjs.org/functions
  // +   original by: Tyler Akins (http://rumkin.com)
  // +   improved by: Thunder.m
  // +      input by: Aman Gupta
  // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
  // +   bugfixed by: Onno Marsman
  // +   bugfixed by: Pellentesque Malesuada
  // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
  // +      input by: Brett Zamir (http://brett-zamir.me)
  // +   bugfixed by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
  // *     example 1: base64_decode('S2V2aW4gdmFuIFpvbm5ldmVsZA==');
  // *     returns 1: 'Kevin van Zonneveld'
  // mozilla has this native
  // - but breaks in 2.0.0.12!
  //if (typeof this.window['atob'] === 'function') {
  //    return atob(data);
  //}
  var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
  var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
    ac = 0,
    dec = "",
    tmp_arr = [];

  if (!data)
    {
    return data;
    }

  data += '';

  do { // unpack four hexets into three octets using index points in b64
  h1 = b64.indexOf(data.charAt(i++));
  h2 = b64.indexOf(data.charAt(i++));
  h3 = b64.indexOf(data.charAt(i++));
  h4 = b64.indexOf(data.charAt(i++));

  bits = h1 << 18 | h2 << 12 | h3 << 6 | h4;

  o1 = bits >> 16 & 0xff;
  o2 = bits >> 8 & 0xff;
  o3 = bits & 0xff;

  if (h3 == 64)
    {
    tmp_arr[ac++] = String.fromCharCode(o1);
    }
  else if (h4 == 64)
    {
    tmp_arr[ac++] = String.fromCharCode(o1, o2);
    }
  else
    {
    tmp_arr[ac++] = String.fromCharCode(o1, o2, o3);
    }
  } while (i < data.length);

  dec = tmp_arr.join('');

  return dec;
  }

function base64_encode(data)
  {
  // From: http://phpjs.org/functions
  // +   original by: Tyler Akins (http://rumkin.com)
  // +   improved by: Bayron Guevara
  // +   improved by: Thunder.m
  // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
  // +   bugfixed by: Pellentesque Malesuada
  // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
  // +   improved by: Rafał Kukawski (http://kukawski.pl)
  // *     example 1: base64_encode('Kevin van Zonneveld');
  // *     returns 1: 'S2V2aW4gdmFuIFpvbm5ldmVsZA=='
  // mozilla has this native
  // - but breaks in 2.0.0.12!
  //if (typeof this.window['btoa'] === 'function') {
  //    return btoa(data);
  //}
  var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
  var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
    ac = 0,
    enc = "",
    tmp_arr = [];

  if (!data)
    {
    return data;
    }

  do { // pack three octets into four hexets
  o1 = data.charCodeAt(i++);
  o2 = data.charCodeAt(i++);
  o3 = data.charCodeAt(i++);

  bits = o1 << 16 | o2 << 8 | o3;

  h1 = bits >> 18 & 0x3f;
  h2 = bits >> 12 & 0x3f;
  h3 = bits >> 6 & 0x3f;
  h4 = bits & 0x3f;

  // use hexets to index into b64, and append result to encoded string
  tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
  } while (i < data.length);

  enc = tmp_arr.join('');

  var r = data.length % 3;

  return (r ? enc.slice(0, r - 3) : enc) + '==='.slice(r || 3);

  }

/**********************************************************************************************
 *
 * Hex to Ascii function
 *
 **********************************************************************************************/

function hex2a(hexx)
  {
  var hex = hexx.toString();//force conversion
  var str = '';
  for (var i = 0; i < hex.length; i += 2)
    str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
  return str;
  }

/**********************************************************************************************
 *
 * Validate Email function
 *
 **********************************************************************************************/

function validateEmail(email)
  {
  var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
  return re.test(email);
  }

/**********************************************************************************************
 *
 *  set up window['format']
 *
 **********************************************************************************************/

window['format'] = function (m, v)
  {
  if (!m || isNaN(+v))
    {
    return v; //return as it is.
    }
  //convert any string to number according to formation sign.
  var v = m.charAt(0) == '-' ? -v : +v;
  var isNegative = v < 0 ? v = -v : 0; //process only abs(), and turn on flag.

  // return exponent style numbers as fixed format. NPP Jun 2014, arbitrary!
  if ((v != 0) && (Math.abs(v) < 0.00001))
    {
    return v.toFixed(8);
    }

  //search for separator for grp & decimal, anything not digit, not +/- sign, not #.
  var result = m.match(/[^\d\-\+#]/g);
  var Decimal = (result && result[result.length - 1]) || '.'; //treat the right most symbol as decimal
  var Group = (result && result[1] && result[0]) || ',';  //treat the left most symbol as group separator

  //split the decimal for the format string if any.
  var m = m.split(Decimal);
  //Fix the decimal first, toFixed will auto fill trailing zero.
  v = v.toFixed(m[1] && m[1].length);
  v = +(v) + ''; //convert number to string to trim off *all* trailing decimal zero(es)

  //fill back any trailing zero according to format
  var pos_trail_zero = m[1] && m[1].lastIndexOf('0'); //look for last zero in format
  var part = v.split('.');
  //integer will get !part[1]
  if (!part[1] || part[1] && part[1].length <= pos_trail_zero)
    {
    v = (+v).toFixed(pos_trail_zero + 1);
    }
  var szSep = m[0].split(Group); //look for separator
  m[0] = szSep.join(''); //join back without separator for counting the pos of any leading 0.

  var pos_lead_zero = m[0] && m[0].indexOf('0');
  if (pos_lead_zero > -1)
    {
    while (part[0].length < (m[0].length - pos_lead_zero))
      {
      part[0] = '0' + part[0];
      }
    }
  else if (+part[0] == 0)
    {
    part[0] = '';
    }

  v = v.split('.');
  v[0] = part[0];

  //process the first group separator from decimal (.) only, the rest ignore.
  //get the length of the last slice of split result.
  var pos_separator = ( szSep[1] && szSep[szSep.length - 1].length);
  if (pos_separator)
    {
    var integer = v[0];
    var str = '';
    var offset = integer.length % pos_separator;
    for (var i = 0, l = integer.length; i < l; i++)
      {

      str += integer.charAt(i); //ie6 only support charAt for sz.
      //-pos_separator so that won't trail separator on full length
      if (!((i - offset + 1) % pos_separator) && i < l - pos_separator)
        {
        str += Group;
        }
      }
    v[0] = str;
    }

  v[1] = (m[1] && v[1]) ? Decimal + v[1] : "";
  return (isNegative ? '-' : '') + v[0] + v[1]; //put back any negation and combine integer and fraction.
  };


function nz(pValue, pDefault)
  {
  if ((pValue == undefined) || (pValue == null))
    {
    if ((pDefault == undefined) || (pDefault == null))
      {
      return "";
      }
    else
      {
      return pDefault;
      }
    }

  return pValue;
  }


String.prototype.isNumber = function ()
  {
  return !isNaN(parseFloat(this)) && isFinite(this);
  };

/**********************************************************************************************
 *
 * Test to see if an input is a number
 *
 **********************************************************************************************/

function isNumber(n)
  {
  return !isNaN(parseFloat(n)) && isFinite(n);
  }

// Rounding functions
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/round

// Closure
(function ()
  {

  /**
   * Decimal adjustment of a number.
   *
   * @param  {String}  type  The type of adjustment.
   * @param  {Number}  value  The number.
   * @param  {Integer}  exp    The exponent (the 10 logarithm of the adjustment base).
   * @returns  {Number}      The adjusted value.
   */
  function decimalAdjust(type, value, exp)
    {
    // If the exp is undefined or zero...
    if (typeof exp === 'undefined' || +exp === 0)
      {
      return Math[type](value);
      }
    value = +value;
    exp = +exp;
    // If the value is not a number or the exp is not an integer...
    if (isNaN(value) || !(typeof exp === 'number' && exp % 1 === 0))
      {
      return NaN;
      }
    // Shift
    value = value.toString().split('e');
    value = Math[type](+(value[0] + 'e' + (value[1] ? (+value[1] - exp) : -exp)));
    // Shift back
    value = value.toString().split('e');
    return +(value[0] + 'e' + (value[1] ? (+value[1] + exp) : exp));
    }

  // Decimal round
  if (!Math.round10)
    {
    Math.round10 = function (value, exp)
      {
      return decimalAdjust('round', value, exp);
      };
    }
  // Decimal floor
  if (!Math.floor10)
    {
    Math.floor10 = function (value, exp)
      {
      return decimalAdjust('floor', value, exp);
      };
    }
  // Decimal ceil
  if (!Math.ceil10)
    {
    Math.ceil10 = function (value, exp)
      {
      return decimalAdjust('ceil', value, exp);
      };
    }

  })();


// Commas to Number.

Number.prototype.addCommas = function ()
  {
  var isNegative = (this < 0 ? 1 : 0);
  var intPart = Math.round(Math.abs(this)).toString();
  var decimalPart = (Math.abs(this) - Math.round(Math.abs(this))).toString();
  // Remove the "0." if it exists
  if (decimalPart.length > 2)
    {
    decimalPart = decimalPart.substring(2);
    }
  else
    {
    // Otherwise remove it altogether
    decimalPart = '';
    }
  // Work through the digits three at a time
  var i = intPart.length - 3;
  while (i > 0)
    {
    intPart = intPart.substring(0, i) + ',' + intPart.substring(i);
    i = i - 3;
    }
  return (isNegative ? '-' : '') + intPart + decimalPart;
  };


//
// http://jacwright.com/projects/javascript/date_format/
//
// Simulates PHP's date function
//

Date.prototype.format = function (format)
  {
  var returnStr = '';
  var replace = Date.replaceChars;
  for (var i = 0; i < format.length; i++)
    {
    var curChar = format.charAt(i);
    if (i - 1 >= 0 && format.charAt(i - 1) == "\\")
      {
      returnStr += curChar;
      }
    else if (replace[curChar])
      {
      returnStr += replace[curChar].call(this);
      }
    else if (curChar != "\\")
      {
      returnStr += curChar;
      }
    }
  return returnStr;
  };

Date.replaceChars = {
  shortMonths: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
  longMonths: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
  shortDays: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
  longDays: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],

  // Day
  d: function ()
    {
    return (this.getDate() < 10 ? '0' : '') + this.getDate();
    },
  D: function ()
    {
    return Date.replaceChars.shortDays[this.getDay()];
    },
  j: function ()
    {
    return this.getDate();
    },
  l: function ()
    {
    return Date.replaceChars.longDays[this.getDay()];
    },
  N: function ()
    {
    return this.getDay() + 1;
    },
  S: function ()
    {
    return (this.getDate() % 10 == 1 && this.getDate() != 11 ? 'st' : (this.getDate() % 10 == 2 && this.getDate() != 12 ? 'nd' : (this.getDate() % 10 == 3 && this.getDate() != 13 ? 'rd' : 'th')));
    },
  w: function ()
    {
    return this.getDay();
    },
  z: function ()
    {
    var d = new Date(this.getFullYear(), 0, 1);
    return Math.ceil((this - d) / 86400000);
    }, // Fixed now
  // Week
  W: function ()
    {
    var d = new Date(this.getFullYear(), 0, 1);
    return Math.ceil((((this - d) / 86400000) + d.getDay() + 1) / 7);
    }, // Fixed now
  // Month
  F: function ()
    {
    return Date.replaceChars.longMonths[this.getMonth()];
    },
  m: function ()
    {
    return (this.getMonth() < 9 ? '0' : '') + (this.getMonth() + 1);
    },
  M: function ()
    {
    return Date.replaceChars.shortMonths[this.getMonth()];
    },
  n: function ()
    {
    return this.getMonth() + 1;
    },
  t: function ()
    {
    var d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 0).getDate()
    }, // Fixed now, gets #days of date
  // Year
  L: function ()
    {
    var year = this.getFullYear();
    return (year % 400 == 0 || (year % 100 != 0 && year % 4 == 0));
    }, // Fixed now
  o: function ()
    {
    var d = new Date(this.valueOf());
    d.setDate(d.getDate() - ((this.getDay() + 6) % 7) + 3);
    return d.getFullYear();
    }, //Fixed now
  Y: function ()
    {
    return this.getFullYear();
    },
  y: function ()
    {
    return ('' + this.getFullYear()).substr(2);
    },
  // Time
  a: function ()
    {
    return this.getHours() < 12 ? 'am' : 'pm';
    },
  A: function ()
    {
    return this.getHours() < 12 ? 'AM' : 'PM';
    },
  B: function ()
    {
    return Math.floor((((this.getUTCHours() + 1) % 24) + this.getUTCMinutes() / 60 + this.getUTCSeconds() / 3600) * 1000 / 24);
    }, // Fixed now
  g: function ()
    {
    return this.getHours() % 12 || 12;
    },
  G: function ()
    {
    return this.getHours();
    },
  h: function ()
    {
    return ((this.getHours() % 12 || 12) < 10 ? '0' : '') + (this.getHours() % 12 || 12);
    },
  H: function ()
    {
    return (this.getHours() < 10 ? '0' : '') + this.getHours();
    },
  i: function ()
    {
    return (this.getMinutes() < 10 ? '0' : '') + this.getMinutes();
    },
  s: function ()
    {
    return (this.getSeconds() < 10 ? '0' : '') + this.getSeconds();
    },
  u: function ()
    {
    var m = this.getMilliseconds();
    return (m < 10 ? '00' : (m < 100 ?
      '0' : '')) + m;
    },
  // Timezone
  e: function ()
    {
    return "Not Yet Supported";
    },
  I: function ()
    {
    return "Not Yet Supported";
    },
  O: function ()
    {
    return (-this.getTimezoneOffset() < 0 ? '-' : '+') + (Math.abs(this.getTimezoneOffset() / 60) < 10 ? '0' : '') + (Math.abs(this.getTimezoneOffset() / 60)) + '00';
    },
  P: function ()
    {
    return (-this.getTimezoneOffset() < 0 ? '-' : '+') + (Math.abs(this.getTimezoneOffset() / 60) < 10 ? '0' : '') + (Math.abs(this.getTimezoneOffset() / 60)) + ':00';
    }, // Fixed now
  T: function ()
    {
    var m = this.getMonth();
    this.setMonth(0);
    var result = this.toTimeString().replace(/^.+ \(?([^\)]+)\)?$/, '$1');
    this.setMonth(m);
    return result;
    },
  Z: function ()
    {
    return -this.getTimezoneOffset() * 60;
    },
  // Full Date/Time
  c: function ()
    {
    return this.format("Y-m-d\\TH:i:sP");
    }, // Fixed now
  r: function ()
    {
    return this.toString();
    },
  U: function ()
    {
    return this.getTime() / 1000;
    }
};

Number.prototype.mod = function (n)
  {
  return ((this % n) + n) % n;
  }

Date.prototype.addBusDays = function (dd)
  {
  var wks = Math.floor(dd / 5);
  var dys = dd.mod(5);
  var dy = this.getDay();
  if (dy === 6 && dys > -1)
    {
    if (dys === 0)
      {
      dys -= 2;
      dy += 2;
      }
    dys++;
    dy -= 6;
    }
  if (dy === 0 && dys < 1)
    {
    if (dys === 0)
      {
      dys += 2;
      dy -= 2;
      }
    dys--;
    dy += 6;
    }
  if (dy + dys > 5) dys += 2;
  if (dy + dys < 1) dys -= 2;
  this.setDate(this.getDate() + wks * 7 + dys);
  }

/**********************************************************************************************
 *
 * BusinessDaysBetweenDates function
 *
 **********************************************************************************************/

function BusinessDaysBetweenDates(startDate, endDate)
  {
  // Acknowledge : http://partialclass.blogspot.fr/2011/07/calculating-working-days-between-two.html

  // Validate input
  if (endDate < startDate)
    return 0;

  // Calculate days between dates
  var millisecondsPerDay = 86400 * 1000; // Day in milliseconds
  startDate.setHours(0, 0, 0, 1);  // Start just after midnight
  endDate.setHours(23, 59, 59, 999);  // End just before midnight
  var diff = endDate - startDate;  // Milliseconds between datetime objects
  var days = Math.ceil(diff / millisecondsPerDay);

  // Subtract two weekend days for every week in between
  var weeks = Math.floor(days / 7);
  var days = days - (weeks * 2);

  // Handle special cases
  var startDay = startDate.getDay();
  var endDay = endDate.getDay();

  // Remove weekend not previously removed.
  if (startDay - endDay > 1)
    days = days - 2;

  // Remove start day if span starts on Sunday but ends before Saturday
  if (startDay == 0 && endDay != 6)
    days = days - 1

  // Remove end day if span ends on Saturday but starts after Sunday
  if (endDay == 6 && startDay != 0)
    days = days - 1

  return days;
  }

/**********************************************************************************************
 *
 * Get Key Values
 *
 **********************************************************************************************/

function GetKeyValue(params, keyName, defaultValue)
  {
  var thisKey;
  var thisKeyName = nz(keyName, '').toString().toUpperCase();

  var rVal = defaultValue;

  try
    {
    if (keyName in params) return nz(params[keyName], defaultValue);

    for (thisKey in params)
      {
      if (thisKey.toUpperCase() == thisKeyName)
        {
        return nz(params[thisKey], defaultValue);
        }
      }
    }
  catch (e)
    {
    rVal = nz(defaultValue, defaultValue);
    }

  return rVal;
  }

/**********************************************************************************************
 *
 * Get Convert a number of seconds to a reasonable duration description.
 *
 **********************************************************************************************/

function SecondsToDescription(activitySeconds)
  {
  var rVal = '';
  var weekSet = false;
  var daysSet = false;

  try
    {
    if (isNumber(activitySeconds))
      {
      activitySeconds = Math.abs(parseInt(activitySeconds));

      if (activitySeconds >= 1209600)
        {
        var weekCount = Math.floor(activitySeconds / 604800);
        rVal = format('#,##0.', weekCount) + ' ' +  (weekCount > 1 ? getTranslation('txt_weeks', 'weeks') : getTranslation('txt_week', 'week'));
        activitySeconds = activitySeconds % 604800;
        weekSet = true;
        }
      else if (activitySeconds >= 86400)
        {
        var dayCount = Math.floor(activitySeconds / 86400);
        rVal = format('#,##0.', dayCount) + ' ' +  (dayCount > 1 ? getTranslation('txt_days', 'days') : getTranslation('txt_day', 'day'));
        activitySeconds = activitySeconds % 86400;
        daysSet = true;
        }

      if ((activitySeconds >= 3600) && (!weekSet))
        {
        var hourCount = Math.floor(activitySeconds / 3600);
        rVal = rVal + (daysSet ? ', ' : '') + format('#,##0.', hourCount) + ' ' + (hourCount > 1 ? getTranslation('txt_hours', 'hours') : getTranslation('txt_hour', 'hour'));
        }
      else
        {
        if ((!daysSet) && (!weekSet))
          {
          var minuteCount = Math.floor(activitySeconds / 60);

          if (minuteCount == 0)
            {
            rVal = format('#,##0.', Math.max(activitySeconds, 1)) + ' ' + (activitySeconds <= 1 ? getTranslation('txt_second', 'second') : getTranslation('txt_seconds', 'seconds'));
            }
          else
            {
            rVal = format('#,##0.', minuteCount) + ' ' + (minuteCount > 1 ? getTranslation('txt_minutes', 'minutes') : getTranslation('txt_minute', 'minute'));
            }
          }
        }
      }
    }
  catch (e)
    {
    }

  return rVal;
  }

/**********************************************************************************************
 *
 * Set the values on a button group
 *
 * Please note, if trapping events on Radio groups, use the change event, not the click event.
 **********************************************************************************************/

function ButtonsetValue(group, val)
  {
  var thisGroup;

  if (group.jquery)
    {
    thisGroup = group;
    }
  else
    {
    thisGroup = $(group);
    }


  if (typeof(val) != 'undefined')
    {
    // Bootstrap version...

    if (thisGroup.length)
      {
      thisGroup
        .attr('data-updating', '1')
        .find('input[value="' + val + '"]') //.attr("checked",true)
        .closest('.btn').not('.active')
        .trigger('click');

      thisGroup
        .attr('data-updating', '0');

      /*    // JQuery UI version.
       thisGroup
       .find('input')
       .removeProp('checked')
       .filter('[value="' + val + '"]')
       .prop('checked', true)
       .end()
       .end();*/
      }
    }
  else
    {
    var RVal;
    var CheckedControl = thisGroup.find(':checked');

    if (CheckedControl.length > 0)
      {
      return CheckedControl.val();
      }

    CheckedControl = thisGroup.find('label.active input');
    return CheckedControl.val();
    }
  }

/**********************************************************************************************
 *
 **********************************************************************************************/

function modalWarning(thisTitle, thisHTML)
  {
  try
    {
    var genericConfirm = $('#modalWarning');

    genericConfirm.find('.modal-title').html(thisTitle);
    genericConfirm.find('.modalInfo').html(thisHTML);

    genericConfirm.modal({
      keyboard: true,
      show: true
    });
    }
  catch (e)
    {
    }
  }


// WHY IS THIS PIECE OF CODE HERE?

// From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
if (!Object.keys)
  {
  Object.keys = (function ()
    {
    'use strict';
    var hasOwnProperty = Object.prototype.hasOwnProperty,
      hasDontEnumBug = !({toString: null}).propertyIsEnumerable('toString'),
      dontEnums = [
        'toString',
        'toLocaleString',
        'valueOf',
        'hasOwnProperty',
        'isPrototypeOf',
        'propertyIsEnumerable',
        'constructor'
      ],
      dontEnumsLength = dontEnums.length;

    return function (obj)
      {
      if (typeof obj !== 'object' && (typeof obj !== 'function' || obj === null))
        {
        throw new TypeError('Object.keys called on non-object');
        }

      var result = [], prop, i;

      for (prop in obj)
        {
        if (hasOwnProperty.call(obj, prop))
          {
          result.push(prop);
          }
        }

      if (hasDontEnumBug)
        {
        for (i = 0; i < dontEnumsLength; i++)
          {
          if (hasOwnProperty.call(obj, dontEnums[i]))
            {
            result.push(dontEnums[i]);
            }
          }
        }
      return result;
      };
    }());
  }

function addMinutes(date, minutes)
  {
  return new Date(date.getTime() + minutes * 60000);
  }
function addSeconds(date, seconds)
  {
  return new Date(date.getTime() + seconds * 1000);
  }


function refreshToken(menuName)
  {
  try
    {
    if ((document.SetlSocket) && (!document.dataCache.extendTimeout))
      { // Websocket
      var messageID = document.SetlSocketCallback.getUniqueID();

      document.SetlSocketCallback.addHandler(messageID,
        function (ID, message, UserData)
        {
        try
          {
          var SecondsRemaining = (document.dataCache['SessionExpiryTime'] == false ? 0 : (document.dataCache['SessionExpiryTime'].getTime() - (new Date).getTime()) / 1000);

          if (isNumber(SecondsRemaining) && (SecondsRemaining < 700))
            {
            Hearbeat_Function();
            }
          else
            {
            // Hide generic modal IF it is showing the timeout modal.
            var genericModal = $('#modalGeneric');
            if ((genericModal.is(':visible')) && (genericModal.data('data-modalaction') == 'extendsession'))
              {
              genericModal.modal('hide');
              }

            if ((document.SessionExpiryInterval != undefined) && (document.SessionExpiryInterval !== false))
              {
              clearInterval(document.SessionExpiryInterval);
              document.SessionExpiryInterval = false;
              }
            }
          }
        catch (e)
          {
          }
        }, {});

      var Request =
      {
        MessageType: 'DataRequest',
        MessageHeader: '',
        RequestID: messageID,
        MessageBody: {RequestName: 'extendsession', 'Token': document.dataCache.Token, 'Audit': menuName.toString}
      };

      document.SetlSocket.sendRequest(Request);
      }
    }
  catch (e)
    {
    showError("Error : " + e.message + ", mod_utility.js, line " + e.lineNumber);
    }
  }

function getSearchParameters()
  {
  var prmstr = window.location.search.substr(1);
  return prmstr != null && prmstr != "" ? transformToAssocArray(prmstr) : {};
  }

function transformToAssocArray(prmstr)
  {
  var params = {};
  var prmarr = prmstr.split("&");
  for (var i = 0; i < prmarr.length; i++)
    {
    var tmparr = prmarr[i].split("=");
    params[tmparr[0]] = tmparr[1];
    }
  return params;
  }

/**********************************************************************************************
 *
 * Function to score the strength of a password
 *
 **********************************************************************************************/

function scorePassword(pass)
  {
  var score = 0;
  if (!pass)
    return score;

  // award every unique letter until 5 repetitions
  var letters = new Object();
  for (var i = 0; i < pass.length; i++)
    {
    letters[pass[i]] = (letters[pass[i]] || 0) + 1;
    score += 5.0 / letters[pass[i]];
    }

  // bonus points for mixing it up
  var variations = {
    digits: /\d/.test(pass),
    lower: /[a-z]/.test(pass),
    upper: /[A-Z]/.test(pass),
    nonWords: /\W/.test(pass)
  }

  variationCount = 0;
  for (var check in variations)
    {
    variationCount += (variations[check] == true) ? 1 : 0;
    }
  score += (variationCount - 1) * 10;

  return parseInt(score);
  }

function checkPassStrength(pass)
  {
  var score = scorePassword(pass);
  if (score > 80)
    return "<span class='pstrong'>Strong</span>";
  if (score > 60)
    return "<span class='pmoderate'>Moderate</span>";
  if (score >= 30)
    return "<span class='pweak'>Weak</span>";

  return "<span class='pweak'>Very Weak</span>";
  }

/**********************************************************************************************
 *
 * Functions to get the browser and the version
 *
 **********************************************************************************************/

function get_browser(){
    var N=navigator.appName, ua=navigator.userAgent, tem;
    var M=ua.match(/(opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i);
    if(M && (tem= ua.match(/version\/([\.\d]+)/i))!= null) M[2]= tem[1];
    M=M? [M[1], M[2]]: [N, navigator.appVersion, '-?'];
    return M[0];
    }
    
function get_browser_version(){
    var N=navigator.appName, ua=navigator.userAgent, tem;
    var M=ua.match(/(opera|chrome|safari|firefox|msie)\/?\s*(\.?\d+(\.\d+)*)/i);
    if(M && (tem= ua.match(/version\/([\.\d]+)/i))!= null) M[2]= tem[1];
    M=M? [M[1], M[2]]: [N, navigator.appVersion, '-?'];
    return M[1];
    }

/**********************************************************************************************
 *
 * Function
 *
 **********************************************************************************************/

window['isMobile']  = {
  Android: function() {
  return navigator.userAgent.match(/Android/i) ? true : false;
  },
  BlackBerry: function() {
  return navigator.userAgent.match(/BlackBerry/i) ? true : false;
  },
  iOS: function() {
  return navigator.userAgent.match(/iPhone|iPad|iPod/i) ? true : false;
  },
  Windows: function() {
  return navigator.userAgent.match(/IEMobile/i) ? true : false;
  },
  any: function() {
  return (isMobile.Android() || isMobile.BlackBerry() || isMobile.iOS() || isMobile.Windows());
  }
};

/**********************************************************************************************
 *
 * Get the base url
 *
 **********************************************************************************************/

function getBaseURL() {
    var url = location.href;  // entire url including querystring - also: window.location.href;
    var baseURL = url.substring(0, url.indexOf('/', 23));


    if (baseURL.indexOf('http://localhost/luthor') != -1) {
        // Base Url for localhost
        var pathname = location.pathname;  // window.location.pathname;
        var index1 = url.indexOf(pathname);
        var index2 = url.indexOf("/", index1 + 1);
        var baseLocalUrl = url.substr(0, index2);

        return baseLocalUrl + "/";
    }
    else {
        // Root Url for domain name
        return baseURL + "/";
    }

}

/**********************************************************************************************
 *
 * Return slug for use in advertisement link.
 *
 **********************************************************************************************/

function getSlug(isBuy)
  {
  try
    {
    var BUYSLUGS = ['buy-bitcoins-online-', 'purchase-bitcoins-online-', 'get-bitcoins-online-', 'own-bitcoins-online-', 'deal-in-bitcoins-online-', 'exchange-bitcoins-online-', 'market-bitcoins-online-'];
    var SELLSLUGS = ['sell-bitcoins-online-', 'sale-bitcoins-online-', 'selling-bitcoins-online-', 'advertise-bitcoins-online-', 'deal-in-bitcoins-online-', 'exchange-bitcoins-online-'];

    var slugs = (isBuy ? BUYSLUGS : SELLSLUGS);
    return slugs[math.floor(math.random() * slugs.length)];
    }
  catch (e)
    {
    }
  return 'bitcoins';
  }

function seoUrl(thisUrl)
  {
  return thisUrl.replace(/\s/g , "-");
  }

/**********************************************************************************************
 *
 * Remove js or css from dom
 *
 **********************************************************************************************/
function removejscssfile(filename, filetype){
    var targetelement=(filetype=="js")? "script" : (filetype=="css")? "link" : "none" //determine element type to create nodelist from
    var targetattr=(filetype=="js")? "src" : (filetype=="css")? "href" : "none" //determine corresponding attribute to test for
    var allsuspects=document.getElementsByTagName(targetelement)
    for (var i=allsuspects.length; i>=0; i--){ //search backwards within nodelist for matching elements to remove
        if (allsuspects[i] && allsuspects[i].getAttribute(targetattr)!=null && allsuspects[i].getAttribute(targetattr).indexOf(filename)!=-1)
            allsuspects[i].parentNode.removeChild(allsuspects[i]) //remove element by calling parentNode.removeChild()
    }
}