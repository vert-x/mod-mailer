package org.vertx.mods.test.integration.java;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import static org.vertx.testtools.VertxAssert.*;

/**
 *
 */
public class DKIMTest extends TestVerticle {

  @Override
  public void start() {
    JsonObject dkimConfig = new JsonObject();
    /* Keys should be PKCS8 & Base64 encoded:
     * http://blog.hintcafe.com/post/33696297862/dkim-private-and-public-key
     */
    dkimConfig.putString("domain", "example.com")
      .putString("selector", "mysel")
      .putString("key", "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAL2+1f5BPqYO6DS4mS5K4fWVJLRUk31Ayi4hP351z8oZblilV65J5IOLRFRB7wGbpERinTCgOdOVqORlxGaAvfUyJlo306npgULLCoZn9M81EjW/MnXr1i54dlMvEhJz1CpNJTzu2hLi/qheSWPrf5Lr/IpfPN5LmZgPoSejhZYHAgMBAAECgYEAm2BEF7oRtSWL3HA1b4T+V33T2p9PT0fYx8GOHt4WU2ZTx99NV3lG2LkYMO706poq/5zJH2J1N9/bt3vmQhG2ekC0Em4/RS5eXhvaX+0imimmAKZtGazp87fD2dz15d3fkrT957RPJdmyJZZfr/Y43PqOuOyoqv27lnX5W8nlgUECQQD6iAYBabhRp+sD47Pa2+jZZ6lND1KvhfkhIDxr9dwz/OrEou7Cp9d2o7nCD54+RttfQE3nX/0BoBWptEMacH53AkEAweMjnJzuSztBMc1wS8VgTa3WsovahJWVdBMFYpI0ay+Zkt33kpSpHaVrCOyH36/TBQOBkqxs8NXHgIP8kk+48QJAA14qvoAeUzKvrRi1hT6pjaqgEwIeuW9Snwhg546hjC1dNpF+Ji29bsHasGonVWz8a/ZgVbjrnMb7I5HhyQw0FwJBALL2QUyx3ZI7Y3XgtqJ50OZiayXqyQxQNd8qH3JYmWHGUe6qS3ZVGbRvl1hpWxExbYnXLqZ/2R6DdN4+9cbnJ0ECQQCw8CyRMlpFHNJT31pil+Z5wmrbRONng0ciPtJOEqeApb6A8hX4zgGKyzG8+o+23CDqF9q8TcbU2n9xNxVckxBH")
      .putString("identity", "dkim1024._domainkey.example.com");
    JsonObject conf = new JsonObject();
    conf.putString("address", "test.mailer").putBoolean("fake", true).putObject("dkim", dkimConfig);
    container.deployModule(System.getProperty("vertx.modulename"), conf, new Handler<String>() {
      @Override
      public void handle(String deploymentID) {
        assertNotNull("deploymentID should not be null", deploymentID);
        DKIMTest.super.start();
      }
    });
  }

  @Test
  public void testSendMultiple() throws Exception {
    final int numMails = 10;
    Handler<Message<JsonObject>> replyHandler = new Handler<Message<JsonObject>>() {
      int count;
      public void handle(Message<JsonObject> message) {
        assertEquals("ok", message.body.getString("status"));
        if (++count == numMails) {
          testComplete();
        }
      }
    };
    for (int i = 0; i < numMails; i++) {
      JsonObject jsonObject = createBaseMessage();
      vertx.eventBus().send("test.mailer", jsonObject, replyHandler);
    }
  }

  private JsonObject createBaseMessage() {
    String user = System.getProperty("user.name");
    JsonObject jsonObject = new JsonObject().putString("from", user + "@localhost").putString("to", user + "@localhost")
        .putString("subject", "This is a test").putString("body", "This is the body\nof the mail");
    return jsonObject;
  }

}
