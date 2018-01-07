package org.histo.util.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.dao.ContactDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.patient.Task;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Class Containing data for mail notification
 * 
 * @author andi
 *
 */
@Configurable
@Getter
@Setter
public class MailContainerList extends NotificationContainerList {

	private DiagnosisReportMail selectedMail;

	public MailContainerList(NotificationTyp notificationTyp) {
		super(notificationTyp);
	}
}
