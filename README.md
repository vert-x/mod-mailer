# Mailer

This module allows emails to be sent via SMTP.

## Dependencies

This module requires a mail server to be available.

## Name

The module name is `mailer`.

## Configuration

The mailer module requires the following configuration:

    {
        "address": <address>,
        "host": <host>,
        "port": <port>,
        "ssl": <ssl>,
        "auth": <auth>,
        "username": <username>,
        "password": <password>,
        "content_type": <content_type>
    }

Let's take a look at each field in turn:

* `address` The main address for the busmod. Every busmod has a main address.
* `host` Host name or ip address of the mail server instance. Defaults to `localhost`.
* `port` Port at which the mail server is listening. Defaults to `25`.
* `ssl` If `true` then use ssl, otherwise don't use ssl. Default is `false`.
* `auth` If `true` use authentication, otherwise don't use authentication. Default is `false`.
* `username` If using authentication, the username. Default is `null`.
* `password` If using authentication, the password. Default is `null`.
* `content_type` If you want to send HTML email body, then set to `text/html`. Default is `text/plain`.

For example, to send to a local server on port 25:

    {
        "address": "test.my_mailer"
    }

Or to a gmail account:

    {
        "address": "test.my_mailer"
        "host": "smtp.googlemail.com",
        "port": 465,
        "ssl": true,
        "auth": true,
        "username": "tim",
        "password": "password"
    }

## Sending Emails

To send an email just send a JSON message to the main address of the mailer. The JSON message representing the email should have the following structure:

    {
        "from": <from>,
        "to": <to|to_list>,
        "cc": <cc|cc_list>
        "bcc": <bcc|bcc_list>
        "subject": <subject>
        "body": <body>
    }

Where:

* `from` is the sender address of the email. Must be a well-formed email address.
* `to` to is either a single well formed email address representing the recipient or a JSON array of email addresses representing the recipients. This field is mandatory.
* `cc` to is either a single well formed email address representing a cc recipient or a JSON array of email addresses representing the cc list. This field is optional.
* `bcc` to is either a single well formed email address representing a bcc recipient or a JSON array of email addresses representing the bcc list. This field is optional.
* `subject` is the subject of the email. This field is mandatory.
* `body` is the body of the email. This field is mandatory.

For example, to send a mail to a single recipient:

    {
        "from": "tim@wibble.com",
        "to": "bob@wobble.com",
        "subject": "Congratulations on your new armadillo!",
        "body": "Dear Bob, great to here you have purchased......"
    }

Or to send to multiple recipients:

    {
        "from": "tim@wibble.com",
        "to": ["bob@wobble.com", "jane@tribble.com"],
        "subject": "Sadly, my aardvark George, has been arrested.",
        "body": "All, I'm afraid George was found...."
    }