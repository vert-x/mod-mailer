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
public class MailerTest extends TestVerticle {

  @Override
  public void start() {
    JsonObject conf = new JsonObject();
    conf.putString("address", "test.mailer").putBoolean("fake", true);
    container.deployModule(System.getProperty("vertx.modulename"), conf, new Handler<String>() {
      @Override
      public void handle(String deploymentID) {
        assertNotNull("deploymentID should not be null", deploymentID);
        MailerTest.super.start();
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

  @Test
  public void testSendWithSingleRecipient() throws Exception {
    String rec = System.getProperty("user.name") + "@localhost";
    JsonObject jsonObject = new JsonObject().putString("to", rec);
    sendWithOverrides(jsonObject, null);
  }

  @Test
  public void testSendWithRecipientList() throws Exception {
    String rec = System.getProperty("user.name") + "@localhost";
    JsonArray recipients = new JsonArray(new String[] { rec, rec, rec });
    JsonObject jsonObject = new JsonObject().putArray("to", recipients);
    sendWithOverrides(jsonObject, null);
  }

  @Test
  public void testSendWithSingleCC() throws Exception {
    String rec = System.getProperty("user.name") + "@localhost";
    JsonObject jsonObject = new JsonObject().putString("to", rec).putString("cc", rec);
    sendWithOverrides(jsonObject, null);
  }

  @Test
  public void testSendWithCCList() throws Exception {
    String rec = System.getProperty("user.name") + "@localhost";
    JsonArray recipients = new JsonArray(new String[] { rec, rec, rec });
    JsonObject jsonObject = new JsonObject().putArray("cc", recipients);
    sendWithOverrides(jsonObject, null);
  }

  @Test
  public void testSendWithSingleBCC() throws Exception {
    String rec = System.getProperty("user.name") + "@localhost";
    JsonObject jsonObject = new JsonObject().putString("to", rec).putString("bcc", rec);
    sendWithOverrides(jsonObject, null);
  }

  @Test
  public void testSendWithBCCList() throws Exception {
    String rec = System.getProperty("user.name") + "@localhost";
    JsonArray recipients = new JsonArray(new String[] { rec, rec, rec });
    JsonObject jsonObject = new JsonObject().putArray("bcc", recipients);
    sendWithOverrides(jsonObject, null);
  }

  @Test
  public void testInvalidSingleFrom() throws Exception {
    JsonObject jsonObject = new JsonObject().putString("from", "wqdqwd qwdqwd qwdqwd ");
    sendWithOverrides(jsonObject, "Invalid from");
  }

  @Test
  public void testInvalidSingleRecipient() throws Exception {
    JsonObject jsonObject = new JsonObject().putString("to", "wqdqwd qwdqwd qwdqwd ");
    sendWithOverrides(jsonObject, "Invalid to");
  }

  @Test
  public void testInvalidRecipientList() throws Exception {
    JsonArray recipients = new JsonArray(new String[] { "tim@localhost", "qwdqwd qwdqw d", "qwdkiwqdqwd d" });
    JsonObject jsonObject = new JsonObject().putArray("to", recipients);
    sendWithOverrides(jsonObject, "Invalid to");
  }

  @Test
  public void testNoSubject() throws Exception {
    JsonObject jsonObject = createBaseMessage();
    jsonObject.removeField("subject");
    send(jsonObject, "subject must be specified");
  }

  @Test
  public void testNoBody() throws Exception {
    JsonObject jsonObject = createBaseMessage();
    jsonObject.removeField("body");
    send(jsonObject, "body must be specified");
  }

  @Test
  public void testNoTo() throws Exception {
    JsonObject jsonObject = createBaseMessage();
    jsonObject.removeField("to");
    send(jsonObject, "to address(es) must be specified");
  }

  @Test
  public void testNoFrom() throws Exception {
    JsonObject jsonObject = createBaseMessage();
    jsonObject.removeField("from");
    send(jsonObject, "from address must be specified");
  }

  private void sendWithOverrides(JsonObject overrides, final String error) throws Exception {
    Handler<Message<JsonObject>> replyHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        if (error == null) {
          assertEquals("ok", message.body.getString("status"));
        } else {
          assertEquals("error", message.body.getString("status"));
          assertTrue(message.body.getString("message").startsWith(error));
        }
        testComplete();
      }
    };
    JsonObject jsonObject = createBaseMessage();
    jsonObject.mergeIn(overrides);
    vertx.eventBus().send("test.mailer", jsonObject, replyHandler);
  }

  private void send(JsonObject message, final String error) throws Exception {
    Handler<Message<JsonObject>> replyHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {

        if (error == null) {
          assertEquals("ok", message.body.getString("status"));
        } else {
          assertEquals("error", message.body.getString("status"));
          assertTrue(message.body.getString("message").startsWith(error));
        }
        testComplete();
      }
    };
    vertx.eventBus().send("test.mailer", message, replyHandler);
  }

  private JsonObject createBaseMessage() {
    String user = System.getProperty("user.name");
    JsonObject jsonObject = new JsonObject().putString("from", user + "@localhost").putString("to", user + "@localhost")
        .putString("subject", "This is a test").putString("body", "This is the body\nof the mail");
    return jsonObject;
  }


}
