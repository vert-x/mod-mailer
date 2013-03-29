package org.vertx.mods.test.integration.java;

import org.junit.Test;
import junit.framework.TestCase;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;
import org.vertx.mods.MailerMessage;
import javax.mail.Session;
import javax.mail.Address;
import javax.mail.Multipart;
import javax.mail.BodyPart;
import java.util.Properties;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MailerMessageTest extends TestCase {
  private Session session;
  private MailerMessage message;
  private JsonObject messageJson;

  @Override
  public void setUp() throws Exception {
    Properties props = new Properties();
    session = Session.getInstance(props);
    message = new MailerMessage(session);
  }

  @Test
  public void testMailerMessageConstructorPlainText() throws Exception {
    message = new MailerMessage(session, getPlainTestMessage());

    String contentType = message.getContentType().split(";")[0];
    assertEquals("text/plain", contentType);

    assertEquals("from.address@example.com", message.getFrom()[0].toString());

    Address[] toAddresses = message.getRecipients(javax.mail.Message.RecipientType.TO);
    assertEquals("recipient.address@example.com", toAddresses[0].toString());

    Address[] ccAddresses = message.getRecipients(javax.mail.Message.RecipientType.CC);
    assertEquals("cc01.address@example.com", ccAddresses[0].toString());
    assertEquals("cc02.address@example.com", ccAddresses[1].toString());

    String[] replyHeaders = message.getHeader("reply-to");
    assertEquals("reply.address@example.com", replyHeaders[0]);

    assertEquals("message subject", message.getSubject());

    assertEquals("com.example.my.custom.message.id",  message.getMessageID());

    assertEquals("This is a text body.", message.getContent());
  }

  @Test
  public void testMailerMessageConstructorMultipart() throws Exception {
    message = new MailerMessage(session, getMultipartMessage());

    String multipartContentType = message.getContentType().split(";")[0];
    assertEquals("multipart/mixed", multipartContentType);

    assertEquals("from.address@example.com", message.getFrom()[0].toString());

    Address[] toAddresses = message.getRecipients(javax.mail.Message.RecipientType.TO);
    assertEquals("recipient.address@example.com", toAddresses[0].toString());

    Address[] ccAddresses = message.getRecipients(javax.mail.Message.RecipientType.CC);
    assertEquals("cc01.address@example.com", ccAddresses[0].toString());
    assertEquals("cc02.address@example.com", ccAddresses[1].toString());

    String[] replyHeaders = message.getHeader("reply-to");
    assertEquals("reply.address@example.com", replyHeaders[0]);

    assertEquals("message subject", message.getSubject());

    assertEquals("com.example.my.custom.message.id",  message.getMessageID());

    Multipart multipart = (Multipart)message.getContent();
    assertEquals(2, multipart.getCount());

    BodyPart htmlBody = (BodyPart)multipart.getBodyPart(0);
    String htmlContentType = htmlBody.getContentType().toString().split(";")[0];
    assertEquals("text/html", htmlContentType);
    assertEquals("<p>This is an html body.</p>", htmlBody.getContent());
    String htmlHeader = htmlBody.getHeader("Content-Transfer-Encoding")[0];
    assertEquals("quoted-printable", htmlHeader);

    BodyPart textBody = (BodyPart)multipart.getBodyPart(1);
    String textContentType = textBody.getContentType().toString().split(";")[0];
    assertEquals("text/plain", textContentType);
    assertEquals("This is a text body.", textBody.getContent());
    String textHeader = textBody.getHeader("Content-Transfer-Encoding")[0];
    assertEquals("8bit", textHeader);
  }

  @Test
  public void testParseJsonHeaders() throws Exception {
    JsonObject headersJson = new JsonObject("{\"reply-to\": \"reply.address@example.com\"}");
    message.parseJsonHeaders(headersJson);
    String[] replyToHeaders = message.getHeader("reply-to");
    assertEquals("reply.address@example.com", replyToHeaders[0]);
  }

  private JsonObject getPlainTestMessage() {
    JsonObject mainHeaders = new JsonObject("{\"reply-to\": \"reply.address@example.com\",\"Content-Transfer-Encoding\": \"8bit\"}");
    JsonArray ccAddrs = new JsonArray("[\"cc01.address@example.com\", \"cc02.address@example.com\"]");

    return new JsonObject()
      .putString("from", "from.address@example.com")
      .putString("to", "recipient.address@example.com")
      .putArray("cc", ccAddrs)
      .putString("subject", "message subject")
      .putString("messageID", "com.example.my.custom.message.id")
      .putObject("headers", mainHeaders)
      //.putString("contentType", "text/plain")
      .putString("body", "This is a text body.");
  }

  private JsonObject getMultipartMessage() {
    JsonObject mainHeaders = new JsonObject("{\"reply-to\": \"reply.address@example.com\"}");
    JsonArray ccAddrs = new JsonArray("[\"cc01.address@example.com\", \"cc02.address@example.com\"]");

    JsonObject textObject = new JsonObject("{\"contentType\": \"text/plain; charset='utf-8\", \"body\": \"This is a text body.\"}");
    JsonObject textHeaders = new JsonObject("{\"Content-Transfer-Encoding\": \"8bit\"}");
    textObject.putObject("headers", textHeaders);

    JsonObject htmlObject = new JsonObject("{\"contentType\": \"text/html; charset='utf-8'\", \"body\": \"<p>This is an html body.</p>\"}");
    JsonObject htmlHeaders = new JsonObject("{\"Content-Transfer-Encoding\": \"quoted-printable\"}");
    htmlObject.putObject("headers", htmlHeaders);

    JsonArray bodies = new JsonArray()
      .addObject(htmlObject)
      .addObject(textObject);

    return new JsonObject()
      .putString("from", "from.address@example.com")
      .putString("to", "recipient.address@example.com")
      .putArray("cc", ccAddrs)
      .putString("subject", "message subject")
      .putString("messageID", "com.example.my.custom.message.id")
      .putObject("headers", mainHeaders)
      .putString("contentType", "multipart/mixed")
      .putArray("body", bodies);
  }

}
