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
package io.setl.websocket.handlers;

import java.util.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.socket.WebSocketSession;

public class TextMessageQueueHandlerTest {
  private static final String packedMessage = "eNpVkUFvgzAMhf8KypnDGF1X7RYoU6uxdgJ2Qj2k1KuiMWBJYJuq/vfZNFTpLf7"
      + "8/OK8nFgG3z1ow56809n32FIYQWdmftP2GNVt9Ynlh6g1YDfv97pSsjOybfQo00aYno532KYCBgk"
      + "/tjZKNFpUpEYSINmLWjTVpCFiQH3JRtSTBagB1NWVFJ1qu1aPitHC7hTQurhjCgNQL6T9KmiEki2NliULmH9dPV7"
      + "xt5ztfK9koYtTC+9dmCV5ccEPDo54bMUzhz5Pto+uw3ZTZDy2JnOnk643Lxe6cOgq4VkRJRwHdhQKhT89xsl"
      + "/i9kclBzApoUqrkDQc8PZIphTJPZD10uE7L074JfgRewVtBZHKP46Gr7N+fwPpOmZLg==";
  private TextMessageQueueHandler textMessageQueueHandler;

  @Mock
  private WebSocketSession session;

  @Before
  public void setUp() throws Exception {
    textMessageQueueHandler = new TextMessageQueueHandler();
    session = Mockito.mock(WebSocketSession.class);
  }

  @Test
  public void sendMessage() throws Exception {
    Assert.assertTrue(textMessageQueueHandler.sendMessage(Base64.getDecoder().decode(packedMessage), session));
  }
}
