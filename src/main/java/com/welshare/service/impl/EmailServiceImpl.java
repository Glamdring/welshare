package com.welshare.service.impl;

import java.io.StringWriter;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.welshare.service.EmailService;
import com.welshare.util.WebUtils;
import com.welshare.util.collection.DelegatingI18nMap;

@Service("emailService")
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${smtp.host}")
    private String smtpHost;

    @Value("${smtp.user}")
    private String smtpUser;

    @Value("${smtp.password}")
    private String smtpPassword;

    @Value("${smtp.bounce.email}")
    private String smtpBounceEmail;

    @Value("${base.url}")
    private String baseUrl;

    @Autowired
    @Qualifier("emailsMessageSource")
    private MessageSource emailsMessageSource;

    @Autowired
    @Qualifier("messageSource")
    private MessageSource messageSource;

    @Inject
    private VelocityEngine engine;

    private Email createEmail(boolean html) {
        Email e = null;
        if (html) {
            e = new HtmlEmail();
        } else {
            e = new SimpleEmail();
        }
        e.setHostName(smtpHost);
        if (!StringUtils.isEmpty(smtpUser)) {
            e.setAuthentication(smtpUser, smtpPassword);
        }

        if (!StringUtils.isEmpty(smtpBounceEmail)) {
            e.setBounceAddress(smtpBounceEmail);
        }

        e.setTLS(true);
        e.setSmtpPort(587); //tls port
        e.setCharset("UTF8");
        //e.setDebug(true);

        return e;
    }

    @Override
    @Async
    public void send(EmailDetails details) {
        if ((details.getSubject() != null && details.getSubjectKey() != null)
            || !BooleanUtils.xor(ArrayUtils.toArray(details.getMessage() != null, details.getMessageKey() != null, details.getMessageTemplate() != null))) {
            throw new IllegalStateException("Either subject or subjectKey / either template/message/messageKey should be specified");
        }

        Email email = createEmail(details.isHtml());
        String subject = constructSubject(details);
        email.setSubject(subject);

        String emailMessage = constructEmailMessages(details);

        try {
            if (details.isHtml()) {
                ((HtmlEmail) email).setHtmlMsg(emailMessage);
            } else {
                email.setMsg(emailMessage);
            }

            for (String to : details.getTo()) {
                email.addTo(to);
            }
            email.setFrom(details.getFrom());

            email.send();
        } catch (EmailException ex) {
            logger.error("Exception occurred when sending email", ex);
        }
    }

    private String constructSubject(EmailDetails details) {
        String subject = "";
        if (details.getSubjectKey() != null) {
            subject = emailsMessageSource.getMessage(
                    details.getSubjectKey(), details.getSubjectParams(),
                    details.getLocale());
        }

        if (details.getSubject() != null) {
            subject = details.getSubject();
        }
        return subject;
    }

    private String constructEmailMessages(EmailDetails details) {
        String emailMessage = null;

        if (details.getMessageTemplate() != null) {
            Template template = engine.getTemplate(details.getMessageTemplate(), "UTF-8");
            StringWriter writer = new StringWriter();
            Context ctx = new VelocityContext();
            for (Map.Entry<String, Object> entry : details.getMessageTemplateModel().entrySet()) {
                ctx.put(entry.getKey(), entry.getValue());
            }
            ctx.put("msg", new DelegatingI18nMap(messageSource, details.getLocale()));
            ctx.put("baseUrl", baseUrl);
            ctx.put("currentUser", details.getCurrentUser());
            // not using the whole toolbox mechanics http://velocity.apache.org/tools/devel/config.java.html
            ctx.put("web", WebUtils.INSTANCE);

            template.merge(ctx, writer);
            return writer.toString();
        }

        if (details.getMessageKey() != null) {
            emailMessage = emailsMessageSource.getMessage(
                    details.getMessageKey(), details.getMessageParams(),
                    details.getLocale());
        }

        if (details.getExtraMessageKey() != null) {
            emailMessage += emailsMessageSource.getMessage(
                    details.getExtraMessageKey(), details.getExtraMessageParams(),
                    details.getLocale());
        }

        if (details.getMessage() != null) {
            emailMessage = details.getMessage();
        }

        return emailMessage;
    }

}
