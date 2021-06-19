package main.service;

import main.config.MailConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailSenderService {

    @Autowired
    private final MailConfig mailConfig;

    public EmailSenderService(MailConfig mailConfig) {
        this.mailConfig = mailConfig;
    }

    public void sendMessage(String recipient, String urlForRestore){

        Properties properties = System.getProperties();
        properties.setProperty("mail.transport.protocol", mailConfig.getProtocol());
        properties.setProperty("mail.smtp.host", mailConfig.getSmtpHost());
        properties.setProperty("mail.smtp.port", mailConfig.getSmtpPort());
        properties.setProperty("mail.smtp.user", mailConfig.getLogin());
        properties.setProperty("mail.smtp.ssl.enable", mailConfig.getSmtpSslEnable());
        properties.setProperty("mail.smtp.auth", mailConfig.getSmtpAuth());
        properties.setProperty("mail.debug", mailConfig.getDebug());

        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.getLogin(), mailConfig.getPassword());
            }
        };
        Session session = Session.getDefaultInstance(properties, authenticator);

        try {
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(mailConfig.getLogin()));

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            message.setSubject("Restore password");

            message.setText("Password change " + urlForRestore);

            Transport.send(message);
        } catch (MessagingException mex){ mex.printStackTrace(); }

    }

}