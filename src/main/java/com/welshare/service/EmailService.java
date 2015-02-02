package com.welshare.service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.welshare.model.User;

public interface EmailService {

    /**
     * Sends an email. No exception is thrown even if the message sending fails.
     * (This service may decide to reattempt sending after a while)
     * @param details
     */
    void send(EmailDetails details);

    class EmailDetails {
        private String from;
        private List<String> to = Lists.newArrayList();
        private String messageTemplate;
        private Map<String, Object> messageTemplateModel = Maps.newHashMap();
        private String subjectKey;
        private String messageKey;
        private String extraMessageKey;
        private String subject;
        private String message;
        private String[] subjectParams = new String[0];
        private String[] messageParams = new String[0];
        private String[] extraMessageParams = new String[0];
        private boolean html;
        private Locale locale = Locale.ENGLISH;
        private User currentUser;

        public String getSubjectKey() {
            return subjectKey;
        }
        public EmailDetails setSubjectKey(String subjectKey) {
            this.subjectKey = subjectKey;
            return this;
        }
        public String getMessageKey() {
            return messageKey;
        }
        public EmailDetails setMessageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }
        public String[] getSubjectParams() {
            return subjectParams;
        }
        public EmailDetails setSubjectParams(String[] sibjectParams) {
            this.subjectParams = sibjectParams;
            return this;
        }
        public String[] getMessageParams() {
            return messageParams;
        }
        public EmailDetails setMessageParams(String[] messageParams) {
            this.messageParams = messageParams;
            return this;
        }
        public boolean isHtml() {
            return html;
        }
        public EmailDetails setHtml(boolean html) {
            this.html = html;
            return this;
        }
        public Locale getLocale() {
            return locale;
        }
        public EmailDetails setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }
        public String getFrom() {
            return from;
        }
        public EmailDetails setFrom(String from) {
            this.from = from;
            return this;
        }
        public List<String> getTo() {
            return to;
        }
        public EmailDetails setTo(String to) {
            return addTo(to);
        }

        public EmailDetails addTo(String to) {
            this.to.add(to);
            return this;
        }
        public String getExtraMessageKey() {
            return extraMessageKey;
        }
        public EmailDetails setExtraMessageKey(String extraMessageKey) {
            this.extraMessageKey = extraMessageKey;
            return this;
        }
        public String[] getExtraMessageParams() {
            return extraMessageParams;
        }
        public EmailDetails setExtraMessageParams(String[] extraMessageParams) {
            this.extraMessageParams = extraMessageParams;
            return this;
        }
        public String getSubject() {
            return subject;
        }
        public EmailDetails setSubject(String subject) {
            this.subject = subject;
            return this;
        }
        public String getMessage() {
            return message;
        }
        public EmailDetails setMessage(String message) {
            this.message = message;
            return this;
        }
        public String getMessageTemplate() {
            return messageTemplate;
        }
        public EmailDetails setMessageTemplate(String messageTemplate) {
            this.messageTemplate = messageTemplate;
            return this;
        }
        public Map<String, Object> getMessageTemplateModel() {
            return messageTemplateModel;
        }
        public EmailDetails setMessageTemplateModel(Map<String, Object> messageTemplateModel) {
            this.messageTemplateModel = messageTemplateModel;
            return this;
        }
        public EmailDetails addToMessageTemplateModel(String key, Object value) {
            messageTemplateModel.put(key, value);
            return this;
        }
        public User getCurrentUser() {
            return currentUser;
        }
        public EmailDetails setCurrentUser(User recipientUser) {
            this.currentUser = recipientUser;
            return this;
        }
    }
}
