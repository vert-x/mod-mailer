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

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;
import java.security.KeyFactory;
import org.apache.commons.codec.binary.Base64;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.PrivateKey;
import de.agitos.dkim.DKIMSigner;
import de.agitos.dkim.SMTPDKIMMessage;
import de.agitos.dkim.DKIMSignerException;

/**
 * Mailer Bus Module<p>
 * Please see the busmods manual for a full description<p>
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Mailer extends BusModBase implements Handler<Message<JsonObject>> {

  private Session session;
  private Transport transport;

  private String address;
  private boolean ssl;
  private String bindAddr;
  private int connectionTimeout;
  private int timeout;
  private String host;
  private int port;
  private boolean auth;
  private String username;
  private String password;
  private boolean fake;
  private boolean dkim;
  private DKIMSigner dkimSigner;

  private Properties getProperties()
  {
    address = getOptionalStringConfig("address", "vertx.mailer");
    ssl = getOptionalBooleanConfig("ssl", false);
    bindAddr = getOptionalStringConfig("bindAddr", "localhost");
    connectionTimeout = getOptionalIntConfig("connectionTimeout", 120000);
    timeout = getOptionalIntConfig("timeout", 120000);
    host = getOptionalStringConfig("host", "localhost");
    port = getOptionalIntConfig("port", 25);
    auth = getOptionalBooleanConfig("auth", false);
    username = getOptionalStringConfig("username", null);
    password = getOptionalStringConfig("password", null);
    fake = getOptionalBooleanConfig("fake", false);

    Properties props = new Properties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.localaddress", bindAddr);
    props.put("mail.smtp.connectiontimeout", Integer.toString(connectionTimeout));
    props.put("mail.smtp.timeout", Integer.toString(timeout));
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.socketFactory.port", Integer.toString(port));
    if (ssl) {
      props.put("mail.smtp.socketFactory.class",
        "javax.net.ssl.SSLSocketFactory");
    }
    props.put("mail.smtp.socketFactory.fallback", Boolean.toString(false));
    props.put("mail.smtp.auth", Boolean.toString(auth));
    return props;
  }

  private void createDKIMSigner(JsonObject dkimObject) {
    if(dkimObject.size() > 0) {
      try {
        String dkimPrivateKey = dkimObject.getString("key");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] privateKeyPKCS8 = Base64.decodeBase64(dkimPrivateKey.getBytes());
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyPKCS8);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateSpec);

        dkimSigner = new DKIMSigner(
            dkimObject.getString("domain"),
            dkimObject.getString("selector"),
            privateKey
            );

        dkimSigner.setIdentity(dkimObject.getString("identity"));
        dkim = true;
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public void start() {
    super.start();

    Properties props = getProperties();
    createDKIMSigner(getOptionalObjectConfig("dkim", new JsonObject()));

    eb.registerHandler(address, this);

    if (!fake) {
      session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(username, password);
            }
          });

      try {
        transport = session.getTransport();
        transport.connect();
      } catch (MessagingException e) {
        logger.error("Failed to setup mail transport", e);
      }
    }
  }

  @Override
  public void stop() {
    try {
      if (transport != null) {
        transport.close();
      }
    } catch (MessagingException e) {
      logger.error("Failed to stop mail transport", e);
    }
  }


  public void handle(Message<JsonObject> message) {
    try {
      MimeMessage msg = new MailerMessage(session, message.body());
      msg.setSentDate(new Date());

      if (dkim) msg = new SMTPDKIMMessage(session, msg.getInputStream(), dkimSigner);
      if (! fake) transport.send(msg);

      sendOK(message);
    } catch (MessagingException e) {
      sendError(message, e.getMessage(), e);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

}

