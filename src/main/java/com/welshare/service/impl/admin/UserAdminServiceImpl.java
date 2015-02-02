package com.welshare.service.impl.admin;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.welshare.model.User;
import com.welshare.model.WaitingUser;
import com.welshare.service.EmailService;
import com.welshare.service.admin.UserAdminService;
import com.welshare.service.impl.BaseServiceImpl;

@Service
public class UserAdminServiceImpl extends BaseServiceImpl implements UserAdminService {

    @Inject
    private EmailService emailService;

    @Value("${base.url}")
    private String baseUrl;

    @Value("${invitation.email.sender}")
    private String invitationEmailSender;

    @Value("${information.email.sender}")
    private String emailSender;

    @Override
    public void sendInvitationEmail(List<String> selectedIds,
            String invitationText) {
        for (String id : selectedIds) {
            WaitingUser wuser = getDao().getById(WaitingUser.class, Integer.parseInt(id));

            String url = baseUrl + "/signup?invitationCode=" + wuser.getInvitationCode();

            EmailService.EmailDetails details = new EmailService.EmailDetails();
            details.setFrom(invitationEmailSender)
                .setTo(wuser.getEmail())
                .setSubjectKey("welshareInvitationSubject")
                .setMessage(invitationText + " " + url);

            emailService.send(details);

            wuser.setInvitationSent(true);
            save(wuser);
        }
    }

    @Override
    public void sendEmail(List<String> selectedIds, String subject, String messageText) {
        for (String id : selectedIds) {
            User user = getDao().getById(User.class, id);

            EmailService.EmailDetails details = new EmailService.EmailDetails();
            details.setFrom(emailSender)
                .setTo(user.getEmail())
                .setSubject(subject)
                .setMessage(messageText);

            emailService.send(details);
        }

    }
}
