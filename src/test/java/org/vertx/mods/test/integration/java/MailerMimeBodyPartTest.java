package org.vertx.mods.test.integration.java;

import org.junit.Test;
import junit.framework.TestCase;
import org.vertx.java.core.json.JsonObject;
import org.vertx.mods.MailerMimeBodyPart;

public class MailerMimeBodyPartTest extends TestCase{
  @Test
  public void testParseJsonHeaders() throws Exception {
    JsonObject headersJson = new JsonObject("{\"reply-to\": \"reply.address@example.com\"}");
    MailerMimeBodyPart mimeBody = new MailerMimeBodyPart();
    mimeBody.parseJsonHeaders(headersJson);
    String[] replyToHeaders = mimeBody.getHeader("reply-to");
    assertEquals("reply.address@example.com", replyToHeaders[0]);
  }
}
