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
package io.setl.rest;

import io.setl.rest.util.TwitterStatus;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@RestController
@RequestMapping("/")
public class TwitterFeed {


  public static final String CONSUMER_KEY = "NjO0dYYLvFTZFcEEMgNmVJfPG";

  public static final String CONSUMER_SECRET = "jXpbJHpUuhD721hc5hQuzpsKFQYtVaecR6LCiYoUc8zBmd3O1q";

  public static final String OAUTH_ACCESS_TOKEN = "2591053862-XmgAW2Jq9W9NZ7nhXebMg7iklrxgSop4DRKV4Ve";

  public static final String OAUTH_ACCESS_TOKEN_SECRET = "to1xW1Ny6fyw0cIC3i9qFQzvFa7aGUInmhV2zv9jk1HHd";

  public static final String SCREEN_NAME = "bankofengland";


  public static void main(String[] args) throws TwitterException {
    new TwitterFeed().doIt();
  }


  /**
   * Get the status of entries in the user's Twitter time line.
   *
   * @return the list of statuses
   */
  @RequestMapping(method = RequestMethod.GET, path = "getTwitterFeed.php")
  @ResponseBody
  public List<TwitterStatus> doIt() throws TwitterException {
    SimpleDateFormat df = new SimpleDateFormat("EEEEE MMMMM dd HH:mm:ss SSSZ yyyy");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));

    ConfigurationBuilder cb = new ConfigurationBuilder();

    cb.setDebugEnabled(true)
        .setOAuthConsumerKey(CONSUMER_KEY)
        .setOAuthConsumerSecret(CONSUMER_SECRET)
        .setOAuthAccessToken(OAUTH_ACCESS_TOKEN)
        .setOAuthAccessTokenSecret(OAUTH_ACCESS_TOKEN_SECRET);
    TwitterFactory tf = new TwitterFactory(cb.build());

    Twitter twitter = tf.getInstance();

    List<Status> statusList;
    statusList = twitter.getUserTimeline(SCREEN_NAME);

    List<TwitterStatus> r = new ArrayList<>();
    statusList.forEach(s -> r.add(new TwitterStatus(s, df)));

    return r;
  }

}
