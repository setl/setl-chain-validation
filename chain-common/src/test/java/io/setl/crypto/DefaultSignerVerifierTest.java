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
package io.setl.crypto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.common.Hex;
import io.setl.crypto.KeyGen.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.Test;

/**
 * @author Simon Greatrix on 04/12/2017.
 */
@SuppressWarnings("checkstyle:linelengthcheck")
public class DefaultSignerVerifierTest {

  private static String[] b64KeyPairs = new String[]{
      // secp128r1
      "MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAES8r2qcs+yp7ZnW8DQunbPP1PViJkeswNa9AHF87NCPM=",
      "MC4CAQAwEAYHKoZIzj0CAQYFK4EEABwEFzAVAgEBBBB8C2AavJTGau8vhk6vMGBX",

      // NIST P-224
      "ME4wEAYHKoZIzj0CAQYFK4EEACEDOgAEv9gFIy8aJum+cvlWf8I4N7EzL4F1PsAmdP+BtxCAjK615MdFe2dH20yyvrEebEvYHgYhXkgjFUE=",
      "MDoCAQAwEAYHKoZIzj0CAQYFK4EEACEEIzAhAgEBBBwYJjW9sLt8JQBP06CvfSgqeOvzxjUvKGWd5JYa",
      //EC_NIST_P256
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEOxrCXfSciCFJxxTtq8q/wSg/ZcoegJtUxPB7hlHOMHMAmQxPFwA2iTdJBZaYSAA5KfU2xsf26MAmgIPCdhlWOA==",
      "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCA4Az1QZsV8o3oNbr2AeQTPRS8tLrOG398ESJgU183uCQ==",
      //EC_NIST_P384
      "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEYAFwtgkwjSLDCWcSONc0QLVK2LpnVlUSOhhsOpY6FuZRhYIg8g/99RaTfOT+JqQrZThgFfUiK+R725FCyPbixvzSqe2WCthyb5oyjeP6fsFbNO2R1Jm96"
          + "nV5N0u2YSNB",
      "ME4CAQAwEAYHKoZIzj0CAQYFK4EEACIENzA1AgEBBDChJphRNR7zw1cEdogf0CbHphgft0TVaLMk/4uNWsw++fISNKraduh98W4/EtPp0N8=",
      //EC_NIST_P521
      "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBpigsSWLj53PdHZ67bF16boXjJcgP33EVZ7xbYiuSnvmFoQFN1ZU0rnEJtPrRbih1iwNGwslTCeG7UG8IEm7lZEUBQ9q/jFub27vzAEfLvYyQf9oSp"
          + "nIflyR18nnDef7mjB45st6Khi/C519FYNjFWFvp2mM4MjZROj6RjaPF2xaWtA8=",
      "MF8CAQAwEAYHKoZIzj0CAQYFK4EEACMESDBGAgEBBEFnHPJ/qsgalfdRIVBxCODCayNlUGbiOhtRJsCLgteUN67ZZ63f93jNuXlg9VRgONBPUGKW6DeyOe5tOIZe90mawQ==",
      //ED25519
      "MCowBQYDK2VwAyEAncX5qomyikB6R4pGriSA5drz9M43wRDSSDYLMaICMU4=",
      "MC4CAQAwBQYDK2VwBCIEICWZpQC5KSBgHFMcfMi9+yMUOHgC8SCsmeeVBFOugfWq",
      //ED25519_RAW
      "Gc1PON25wkuNb4SLTTvAs+nvvMIDdgY4SA6LLPTthpU=",
      "7ck5+jVrBMkvkzUl9PyoJ8CriUfg7RlBhl87rSPl/OQ=",
      //RSA 1024
      "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCqWAzLamPsKc9WnVq8huSwey9BM9mt48pb0xdf9YNAVsLLcvV5DQ37Iec8E/AyYWjWLkoz9xOOf34Ifh9l7M1lGbAC+z2Y914Xz5jVIsYl1lADD"
          + "CaqbQ/5niidjK7vPZ9cgL9Zp3Kjvj9mgCPzFi0yRor8JHFod2OVgMx8X77xOwIDAQAB",
      "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKpYDMtqY+wpz1adWryG5LB7L0Ez2a3jylvTF1/1g0BWwsty9XkNDfsh5zwT8DJhaNYuSjP3E45/fgh+H2XszWUZsAL7PZj3XhfPm"
          + "NUixiXWUAMMJqptD/meKJ2Mru89n1yAv1mncqO+P2aAI/MWLTJGivwkcWh3Y5WAzHxfvvE7AgMBAAECgYANViVyS5sVI1TbWCiBeCIxewOBHFkflyUBuW4sEiG7iM9pdE+psY71qXrcJkd4"
          + "+k1l72vZ069RScQJUC6PIVIXRfmAYE+iQD5rWNKN8nKrnNSjUOyXLSnKgmakv8YJaHCrW2pnULxbAX0lNce7zV7jRyBcMsvJ9oj24pTkTTbLaQJBAPxLn373526upQ1RtjHGtTawgNVifzG"
          + "3QPWM8EiuUcYkgYMcaT3TLOaW4Q2teIgwk/8gBxeGTAr6DeISZh2nNCUCQQCs2F80Of2ToNmgNLjPifB5Svuf/rAolqeV8MlVoBA/DBqABLFKjl0nx1H2IfssT+grZWclxAHQgDXEwaBn9O"
          + "HfAkEAqKNrwxS1j1gRXyvFGtR1gE7ObtNdrb+OHhPM8OgEJOt13kt5VSf4lVE4sUnIjU/bPOcNln3gCl5GNVyTATMoOQJACGx7J1Z0L0+1i4NGRocYezDWVpPZDgFZpYsYpN2qLQhv+jFAW"
          + "rvZemjSa8Z4MU6rqAHHJyYIWJsHA9HT+X0LzQJAYe/weBFEgfBZjbzzqEKLbJ9vMe3wf+Fm0IgEIyTANO18xUhzYXTwKI84SZvDqrAcjB5PGKanxOBdnlEhfQJPtw==",
      //RSA 2048
      "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",
      //RSA 3072
      "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAnsirOzn0KoSkjVedJ21vrPryagWos9NjpSzDwToOutsy7j6fiI7N3xijDQJgsoqOVxrNl7Vh6C6g9EoS0e5mP7mzV5VytsGbDA6eWG"
          + "q+2MfdOfYeFnHYJjWsNuCZCzIS59YTBVwX3xn2tzgMv0wd2xCSc6D6ZXn2NYTNRLEeVu1O5HUitmXcDP1QwiY8JFcYrOTrzZQ7wzUiMMVSCDoIpbGRRp3ivct+3crKHZ6ZoTRT8M9b6cr"
          + "yNCAcdlYWkb6c9"
          + "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",
      // RSA 7680
      "MIID4jANBgkqhkiG9w0BAQEFAAOCA88AMIIDygKCA8EAzIV3VrOo/NzMZ6IfNI5hZcs3Wht5UwiR22W5DaHmwBb+qkn5J3XWSjR3qjSiusSiGeUZ7VB3exxF+pHCIBe9sj7lBr1UCOvwEcCFfmA9/D"
          + "uHZDMnmtCOSa6ixYw7irH70pDRUbcvZAeLgLninARIhcSdPC0OH5hPlUjOBm3Ds7iJUpiuwNsoYwNBqJ+eQwgsC8QbZ7D3pCJhKOWDt2cvjiLXqshX4whPaJ0mFiUsE7r8SHfffGH3DlMqOE"
          + "AkDiplU2ixmjL+OkMo57S7VpQegqtCLZvfF5L0i0uVZ463zVf6MYRRQGv1cr6LPfW4pG71x6Y2V+VI0arM9iNQQalIjU+Cf6yqeocX69Kc3Q+oAwgnVuN9+innqhLZCIJNoCLfHF6GcLeNMt"
          + "EiMY8Ldq5adb5/UQRx76hIWMYbE4Pn765Z988GFA/C5pH6ORT1xOLo4dN3gZTOk9Yv97TQE7NOdBcXZkBtMEIVMGnxEyUlspLFtBr0cYgol6N8NYVf3OCXSyMIJGHqvQBWNdmnQjWUQ+qVci"
          + "bkCRI/XJWni6wjpBMdoZlgioDhiScr3Lc8SiXF0Lqc6rcDYfF7ou76j91Nqqz7TxkUTow9SRSvV9w1+0YrA6sCClb4rEqjbxFgDbjchDISYCtvKY9J9jq636gszjvvZKCzzlXvNCXa9tYBMI"
          + "CpMkaIIgfIVWwIG7b/KgssTQjOlrZYjZZx2RGpI/+FEg87SVqCV4/oRof3I2GloXCTkX0EXjMOC6xkNwogxbiAd3Mjaarm35t1DKNFIGavb6y3vAMRuVRRU3YHNjqt/guuByXPK6jlGi0Hn7"
          + "uD7IUrU+9A3u/tQX3b1nUhF2gh8BfWqHU7NwWOf7wbmFb1yGzNZ5OFvK+0m74tF/hbMGt2Kr+2twjt69AjaJdh0Lfky0YMpx6bBHuCkUIjiNtA91cop72NqqUx9f8CHTXLEHSij2zCV5sJT+"
          + "RjYQpGxuhcj8RY/o5iQ7t3BM20WhdxPTCdY9L8OLSf+X8LlRvXvZb2gEuSyoJl+/2LeAJ+iWPKCw1qtOxzuwkFO5wkDWoy6OYkq2SZWJ5v4dY0dsR89lDHOzuOAVDuIukFK0FFQZLrP6MrXW"
          + "VqSmtCY56hEyGUdiivvJGsozjCQLtoVnzRjcJttcWfVfvVPU6KqGYEMIDga4lrAQZEf4TDkvA/W/opTi5VVOaxIFk+z9C4Jm9E2xDkaj2DMC0PbDxo2ib+szGgSLPbdSAhYQZ7WoxCT+bN4G"
          + "xzNaBo/KtLGFiUL6hrdtk1AgMBAAE=",
      "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"
          + "gYnTeO9FmT"
          + "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",
      //RSA 15360
      "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"
          + "Yt8eDM0WgLpY"
          + "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"
          + "lwfH2pxLJwqqlBO"
  };

  private static KeyPair[] keyPairs;

  private static String[] signatures = new String[]{
      "MCUCEAWlbOkiCFMepH/bnAI8U18CEQC76MEgytz7bVYF2cZtEZfr",
      "MD4CHQDhUUfyiNdD0YVMx4oHf+hsOtYuDtUoSI9h9f9vAh0ApT6pFRkjlRtjF+0sL9EgTqydycDBkAWOt788wQ==",
      "MEUCIQC2KFiwcBA151rSmkxVhcklOdbfDCYKEoFfCSIYQSuRVwIgEOqkkl1yp1xnExodwJmkfyxi+ecfw7zQ7c9fmNz1avQ=",
      "MGYCMQDAigH3Ri7JHiQKgHbLyKZffOsRiCDAHaV0ljWyTbncpuwXY2OUG1jIIcxHDm1hTTICMQCVHxRc00RJJaDg2aBV1gEtbFWp3l2fjlcgkTkQfDCL85K/WYPWKHL6Mi7QruA1Tik=",
      "MIGIAkIBYojgieqnjj/OGH99wiw0nOWsUdeUYansJlZIknPOfy4GB0osrCi5JFGQuQhIz8PxncLRZx9BtVNJXa1mkWwbJZACQgF7YXrE4kfANnlvFMc/Bh78A5NhO3f88ZJV9LYRwK3MnYqVF9P"
          + "AEKnir1W1aNa3mXBDD/mcmAWPduFjaoOw6TBEcQ==",
      "TuWxAHqdKz2xekufRzuArmYTyDupiflflfH75xR0guCOR5fSi+JheFzUYz3mt0zUVrFjZwFdTj4AtEs5RcgJAA==",
      "Xr+ndBcD5I3TmYyeWSRQr3C61rsj43udZCplP1GOlbJP6f+P5nPb2OoU6JRMg2op3M23J1jDJEWgPqSkThxQDA==",
      "mpAm+/6Xlcf97LkHqN0v9QqQbNbrtj1b4AqF/YyuuP23/5AUc++OhDJ5XmPhS1CILdcqJokQTlGAsdpAu07F04ita17NHoKqi8md+krAeya8UDq6LCUeYxIdz2GHEi8bkn6xiD6rlyssF+wqlc//v"
          + "Uq9h/Hom8D3QMF3/eQJ5xs=",
      "FxgnMVz/jvdxL+WCCR77kcovveR90rBOSG4saSGiXvP6+Eily75TkZjo5jMG2ivtWPeNPg7LCZTtxjp1wypZYPyrNt2mUjd2snH9kSr5U6x4MINz0LDWj22ZP2MnopfIajpW7kG6GI1iwcNKML3QN"
          + "jxNUEsjP1a7e2m0a9WQ16nBbhd65MXu5bCze8XS88PqBHAWg+oiHu9lD1wylmqCe2BD+i2szoW4CEYdOjol3f66h6oLQNMwdBrEWi658VLGU/6MiMs6fIk6UiOobV5itQtjjb/xc4nLjZMZL"
          + "XeVjiw0Qedy13sQGpmujClltIlyfN7KfDYcb2H+WFOLTEcckA==",
      "SDt+ciPq6Ls0NDi2YXbDkUvPGXN7bu12GbVD7oRFVjRjpoPRoHRkmK7XfcDIuHvsL7QbVwQEGgeuUNfsmDkF6boTX2Jae/dNlQEH+b90s71qyjbOvirVJ2xBJNOLKDyVTSvUS72Nf5E4iHOtIWqth"
          + "K/AHB+x6MTCUzxoboDyWs6IZv6K5JrON5HBqJRYmoaIo6dhTbOFBAbBmFoY4mtEKznFGllr6FawvPr7r76t/t06GxiIJEGjgHLBf7RqlRrO8BIEddh/4oPev8Zx3qdoSpxXTLmye5kBOPsm"
          + "syTiqcgSjJI8h9hs9ozKREVrkcDqMrh5jdBtShdK7LP4A17CfmrddcQ2mshrHCqj59z08GhGc/pJipG11OOI73UWi6occJAAfC5ld2vtKig8nI0mtkqb13VjmQqGyRDcC/ZkWE6S7IICUFd"
          + "MAnpKBNrmSugCmQDYviEI5FI2YYK8L2ViOoaABWBWDK6Kg4kumwzi+GnLVZGLrrZ+cx6pvIKqauI8",
      "W3D05FeC4DXiSTFFYE11r7ATgpWjIYeq+f+d1+6Cb7OoH24Y6g8R+hvzkiqVnab5sRdpMaOZ3rNvSAIJmixvkmDk4et7OsMGlLVNUYMzT7cni62BkD/0N2jddgJukhzyLi4+oWtbG4QF4BvaADVAc"
          + "mJT4UpUFVsQnTZCqMW+mGmajfrc5+4gSiyMYGp2hFeBtTYHmwpGdroszA0QO9bx0fPRMjj250QF+tFMJevMUGtbxCTq7Li4X4/Q1V7KEe1GeEwYhjys6aWb57VLOLQxUSbnQORVWCr2ZWfp"
          + "629vbgWDAF6DOoaYZDC+MJDiVqLQLCnB0KrIhDigAGJ6XMo+TGy4/m3jZtQ4761X6+a3V+bEhqJGDiLmEIBVPHxouGt+tewDGAVMT2Vs7kfCQzWkqJe60tIn6dnd4JuvqKGu5u79KDykU3A"
          + "/M5N6VH4/cA3rzlH+n33CUKlsDaaoqNrSczqNWMcHucxCAOpMqEzn2I4EnvcwTFb3aQPRI5rqpMr54gJz+qVCyhSAvrATf1SlWsYck7S8rG0mN8HiYMkLgidQJdZ3FeeOiR4+ieDZeE+nnV"
          + "4hdUNwml6AXAMNwCCtzeepWbOld0RXTOmdUU07q5SzMsCxdNQfXHWqp+WRmltgPnS0jNUCyQuCAXO2O4rFw8CnueOm6c2PB5+FW6uexQg0Hn6+TT9SAzt5d/40DVUT/6tiPVqoKZlk1IfSK"
          + "bgY09KoEHEbd24oBW9wzntYWk5S1zOOrdt8lY5tCiI41pdFl3fxtEHQN+LLqI5Pql6TlC94DUXjNfhJoQnxDC0wuNPwHT7uZi7YUO+ypN2nA13j7ttoGnJEBYkFseD9g+IoGtY7utGoINp7"
          + "vZgWuzFCNCuEhrP4G9zLkmEFBh8zu+bcabgCB8Ae9Lfsi1Bfe1X3WLRFMXYTDgLUWEmEstnSdryVyaiaz8eX+GGUpiaFQvi3k5zUsVqefJ4gvxto/E5kJkkjllu2iF2gm3gmDAfAqGuolQU"
          + "HBT7o0ooMRklf6tfZ0gs+zN7zA2L2WeqihYdR7O0zWF5mNZDgplmZrVYYPD5k2pA29FmPNYOwNTkunHR1ThbFM5e6qP5KLXgGdpmWE2lktLfdByExQzNqmEzs6nWWUAxkX31mH/3+EY"
          + "008so58R3O2GXABBO4iRvfwg2AVAL4xDQiSyfafzkKJsYQDtvg1Lq0PX01IwYH6PlvCCUQuyqzP5rE2Y6TJWjo2DEMc9Qe2UGd0Ivvd6N8C6ecBcpZjAUymRqg9CTvbHsZ/qMT",
      "VT9czWdz8Me0+JnAbn8TcZH8lw3rG5sJ5+x3DQesfskQSWVAOy/lsxCD5+NPD9cEQSRVDB7V5FF6ImMmQMos/3LuCKJ5qUiAeuXyf272ihXbOGi2TEJovT8dRvJKui0768CAvnAGR0njy9KAxZLQx"
          + "U8LbzOIehyJlN9BID3GZoWomdzmfyizqHBppvKeEd6E9DGUWdZ8/YmjNIIOy0UG6lQBOhHX4oPS71Ho43LwmqLLwwhPVpqJG9WxzFrGtnso/IKtvBHGzXP6d8Obm4t4WlkyLHccsWfiZQ0S"
          + "vgInMOQRgrxMCzuTBvwfwDNtqGrkn2kSucTpGZO9usW+f5B2ro/0NPB7XIU/2GfSZs8+7phN44RPQH9lqWdJZYr3fQYLcagAb2lndZ4RSxSX3iD4oh54U5ucuLjjcIV/KnMzsNjDeDPnPj9"
          + "/fvv85srgqCrAzexL0YNAzuoZa8hnW9lCApmfE3jWGofqUuUgXo+inOD4T3mKcOXNh9N4aS6AZHf0kuN5eJlmFp20ibmzR0QLHLVobEjrM09ewllR1uOJ6UJ+GbbxOMG+BJM4uoCsO3wZ8q"
          + "ARMKx1z2tpDAeoYGYJdCxzqqx6jZplIWTurIGkeYeEzye9aApd6fGLpk6omceQuxPgsJjBmb1Kt87+w3f+lusuDXM1PD4FysjZMXZqxnGUriIxO4x60te6vLaup9k/VmLWxwtRK1LH93jsn"
          + "Zq5MaOuSXCc7+Xpelj5l4+D3+p+86OxQHzxhSRZjqidiwMJnnlXmXQYbBIXyGtlJwEWHVMY6jBsVpIlNVC9gaTYX3EboFKwtzZ2k1UlLGcsLBQIxX2h77veRw9k7lP9J9sMUeltE04qCIwG"
          + "HW+y7dWOFJg6tV24c3KKfEmS9gposJVWl9M5khup85jTW0iappXgMNrIyIJMOVaMVJIrNuy9ppNmLGSxyJMSnfziYzXUj/Jz5QACzekN+gRYa8Nsd/iwkMbQkekhrwJD1xn6xdLKWSGpTIw"
          + "bnC05cl0AbVNHh9wxiRqGaBYnQjNbdKCWlBg2G93x5wMuRiVMnMsbK8KBGxEDMls2CTokELbfGCwwQlL/NT5TL0yC5EeP8PUand4nEXYQMJqkQuiMwWU2N+RvnNHKfztyus7CrfS1jJP/hf"
          + "M5lRyGlmGfT8mOlNmAanfue7zPA5cNZub+a22wcwaB0ClA9gX0GXJZv/wfhm/qtGkKY/B286hRx3Ae3gjKRW1qvA2c/CTnLq5j7ajutzqgmqgnb2pGogdl+Ebxi/fLTfbZIyx2CmsTG4KaW"
          + "20nGAJTwY0FKiLu/CEFug0/WotU/3mkCNt7jptUuU0wn5kozp8QPP8UfaXfkWnfKyJ8fWzPSvJ9ROLT1jjyp1LRP05GHpvlGnDdRekmMvEymHxx5mvO0MVXOivS2YL9D9HVk8tDtci/j5ot"
          + "WaWJfUhdjtfy3htc1QDcrjGD2quRSHM2SchQnSl/eV6gnZbXkeGeqsTCLRsUxK5o7cmKywnCyVMusZSeSqbSXVJRxkl0xCmfjzyvRL1J3IlQWmEGVCH7QQd6VrjPu6XJsBcv8pyR9mLCpL/"
          + "NAIgDPRHljC00ZZUtT+yycAtvI+wPVtvyGBHfFui9td2VcmjgfGtWjk2w+jOGMVTn+Z1ZfDpquh7ycrCDWmb6A1u/Q7K+smTCvEeb52IqgVj8UVREfa4x04O158vwedB1fAaqOVE2eRdX+F"
          + "L+7LqKnMVPBcbfNYoE1gBviOrqS9dhO7R606Jr5d+okpVGGUHMM3GFGOp+x73S7anrDw7I6mNlIC/PEPzR6S8jk84u882mzVcA4RxmxEoIiuCgiE/wRaDoCY4aMqYB6nVICZfV8GLFNCsYi"
          + "eGaR7CKvk3oaN4Qy4gdfIEMXwxEb58Ydo3fubuVerO268+FIEFfiB4BLl6PQg5bti2SA5YeXrJG1hn1+fmb9jiEb1Ps3z5hg/KQ9Q0rLV4QK18LZtj6/yjiXlA4FgHJ6lZYTaEqVlTjEJ2eM"
          + "e+KPBPXCTY7A1ZWatGMvOklHPnqWAmLZSIQgqOGpjyAU1sHpk29pxEMhjX2jODdaWM3UIfVgVS9wflCk1vu2c17BlGH65IZmyC58CLE8/CGicaVOWG2L7F87wBjJnIEu5COaRcVl3qxg7f1c"
          + "DcYJo9naChHm5tsO9kZDqTw4L+pVS1oL/i4RPLVwK7P3bIOfuo1ZxMF0AAkdcw1jHLdsnGfocvxBX03XFtwJv3puTmvU15eNsfcXsPGZIJZpJG/6OuVNTV97N+bwB36mdkDhyUOLcgyI49Mi"
          + "HZOhJhzZL6fVgp1VD8QMF+se9V5/hK0b+Sntvlmb9Q54OEK2RLVdZH8KnXzhIdKI6nfY+zI5g89RzxVtBU+ExDTKU0B1koBfRh0kLrpFyVSrREm4uv+Ekanat5HEVxZEhcy0tOUkAz5CiiS"
          + "JJrO9UO/ItasAMHdmROJ0IXCOrhxm2QPX+IzcwSgWPFgXXyy/SqIVGMoIw+sIQBdhcSATqESk5gip4/kLzgeJG9saYf0pJF+0Z4Cxl+Fbz4uI4I4oLjyThai"
  };

  static {
    keyPairs = new KeyPair[b64KeyPairs.length / 2];
    for (int i = 0; i < keyPairs.length; i++) {
      PublicKey publicKey = KeyGen.getPublicKey(Base64.getDecoder().decode(b64KeyPairs[i * 2]));
      PrivateKey privateKey = KeyGen.getPrivateKey(Base64.getDecoder().decode(b64KeyPairs[i * 2 + 1]));
      keyPairs[i] = new KeyPair(publicKey, privateKey);
    }
  }

  byte[] message = (
      "The difference between a signature and an autograph is that the second one has higher face value on "
          + "a plain paper than the first one even on a cheque. - Vikram Verma")
      .getBytes(StandardCharsets.UTF_8);


  @Test
  public void signAndVerify() throws Exception {
    DefaultSignerVerifier signerVerifier = new DefaultSignerVerifier();

    for (int i = 0; i < keyPairs.length; i++) {
      KeyPair kp = keyPairs[i];
      byte[] sign = signerVerifier.createSignature(message, kp.getPrivate());
      assertTrue(signerVerifier.verifySignature(message, kp.getPublic(), sign));

      for (int j = 0; j < keyPairs.length; j++) {
        if (i != j) {
          assertFalse(signerVerifier.verifySignature(message, keyPairs[j].getPublic(), sign));
        }
      }

      // change a bit in the message
      message[8] ^= 4;
      assertFalse(signerVerifier.verifySignature(message, kp.getPublic(), sign));

      // change a bit in the signature
      sign[8] ^= 8;
      assertFalse(signerVerifier.verifySignature(message, kp.getPublic(), sign));

      // reset the message bit
      message[8] ^= 4;
      assertFalse(signerVerifier.verifySignature(message, kp.getPublic(), sign));
    }
  }


  private void signAndVerify(KeyPair kp) {
    DefaultSignerVerifier signerVerifier = new DefaultSignerVerifier();
    byte[] signature = signerVerifier.createSignature(message, kp.getPrivate());
    assertTrue(signerVerifier.verifySignature(message, kp.getPublic(), signature));
  }


  @Test
  @Deprecated
  public void verify() throws Exception {
    DefaultSignerVerifier signerVerifier = new DefaultSignerVerifier();

    // Verify pre-generated signatures
    for (int i = 0; i < signatures.length; i++) {
      assertTrue(signerVerifier.verifySignature(message,
          Hex.encode(Base64.getDecoder().decode(b64KeyPairs[i * 2])),
          Base64.getDecoder().decode(signatures[i])));
    }
  }


  @Test
  public void verifyWithEdwards() throws Exception {
    signAndVerify(Type.ED25519.generate());
  }
}
