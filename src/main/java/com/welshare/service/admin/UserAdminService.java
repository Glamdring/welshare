package com.welshare.service.admin;

import java.util.List;

import com.welshare.service.BaseService;

public interface UserAdminService extends BaseService {

    void sendInvitationEmail(List<String> selectedIds, String invitationText);

    void sendEmail(List<String> selectedIds, String messageText, String subject);

}
