/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */

package org.vertx.mods;


import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Multipart;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Iterator;
import java.util.Map;

public class MailerMessage extends MimeMessage
{
  private String messageID;

  public MailerMessage(Session session)
  {
    super(session);
  }

  public MailerMessage(Session session, JsonObject messageJson) throws MessagingException, AddressException
  {
    // Extract all message fields from messageJson
    super(session);

    String from = messageJson.getString("from");
    if (from == null) throw new MessagingException("from address must be specified");

    String subject = messageJson.getString("subject");
    if (subject == null) throw new MessagingException("subject must be specified");

    String contentType = messageJson.getString("contentType");
    if (contentType == null) contentType = "text/plain";
    contentType = contentType.toLowerCase();

    InternetAddress fromAddress;
    try {
      fromAddress = new InternetAddress(from, true);
    } catch (AddressException e) {
      throw new AddressException("Invalid from field");
    }

    InternetAddress[] recipients = parseAddresses(messageJson, "to", true);
    InternetAddress[] cc = parseAddresses(messageJson, "cc", false);
    InternetAddress[] bcc = parseAddresses(messageJson, "bcc", false);

    JsonObject headers = messageJson.getObject("headers");
    String messageID = messageJson.getString("messageID");

    // Set the message fields
    setFrom(fromAddress);
    setRecipients(javax.mail.Message.RecipientType.TO, recipients);
    setRecipients(javax.mail.Message.RecipientType.CC, cc);
    setRecipients(javax.mail.Message.RecipientType.BCC, bcc);
    setSubject(subject);
    setMessageID(messageID);

    if (headers != null) parseJsonHeaders(headers);

    switch (contentType) {
      case "multipart/alternative":
      case "multipart/mixed":
        MimeMultipart multipart = new MimeMultipart();

        JsonArray messageParts = messageJson.getArray("body");
        if (messageParts == null) throw new MessagingException("body must be specified");

        Iterator messagePartsIterator = messageParts.iterator();
        while (messagePartsIterator.hasNext()) {
          Object messagePartObj = messagePartsIterator.next();
          if (messagePartObj instanceof JsonObject) {
             JsonObject messagePartJson = (JsonObject)messagePartObj;
             MailerMimeBodyPart messagePart = parseMessagePart(messagePartJson);
             multipart.addBodyPart(messagePart);
          } else {
            throw new MessagingException("multipart message content should be JsonObject");
          }
        }
        setContent(multipart);
        break;
      default:
        String body = messageJson.getString("body");
        if (body == null) throw new MessagingException("body must be specified");
        setContent(body, contentType);
        break;
    }
    saveChanges();
  }

  public void parseJsonHeaders(JsonObject headersJson) throws MessagingException
  {
    Iterator<java.util.Map.Entry<String, Object>> headers = headersJson.toMap().entrySet().iterator();
    while (headers.hasNext()) {
      Map.Entry<String, Object> header = headers.next();
      setHeader(header.getKey(), header.getValue().toString());
    }
  }

  private InternetAddress[] parseAddresses(JsonObject messageJson, String fieldName, boolean required)
    throws MessagingException
  {
    Object oto = messageJson.getField(fieldName);
    if (oto == null) {
      if (required) {
        throw new AddressException(fieldName + " address(es) must be specified");
      }
      return null;
    }
    try {
      InternetAddress[] addresses = null;
      if (oto instanceof String) {
        addresses = InternetAddress.parse((String)oto, true);
      } else if (oto instanceof JsonArray) {
        JsonArray loto = (JsonArray)oto;
        addresses = new InternetAddress[loto.size()];
        int count = 0;
        for (Object addr: loto) {
          if (addr instanceof String == false) {
            throw new MessagingException("Invalid " + fieldName + " field");
          }
          InternetAddress[] ia = InternetAddress.parse((String)addr, true);
          addresses[count++] = ia[0];
        }
      }
      return addresses;
    } catch (AddressException e) {
      throw new MessagingException("Invalid " + fieldName + " field");
    }
  }

  private MailerMimeBodyPart parseMessagePart(JsonObject mimePartJson) throws MessagingException
  {
    MailerMimeBodyPart part = new MailerMimeBodyPart();

    String contentType = mimePartJson.getString("contentType");
    if (contentType == null) {
      throw new MessagingException("mime part content must contain content-type");
    }

    String body = mimePartJson.getString("body");
    if (body == null) {
      throw new MessagingException("mime part content must content body");
    }

    part.setContent(body, contentType);

    String fileName = mimePartJson.getString("fileName");
    if (fileName != null) {
      part.setFileName(fileName);
    }

    JsonObject headers = mimePartJson.getObject("headers");
    if (headers != null) {
      part.parseJsonHeaders(headers);
    }

    return(part);
  }

  @Override
  protected void updateMessageID() throws MessagingException
  {
    setHeader("Message-ID", messageID);
  }

  public String getMessageID()
  {
    return messageID;
  }

  public void setMessageID(String messageID)
  {
    this.messageID = messageID;
  }
}
