package org.histo.ui.medicalFindings;

import java.util.List;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.NotificationOption;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.transformer.DefaultTransformer;

public class EmailNotificationSettings {
	/**
	 * True if email should be send;
	 */
	private boolean useEmail;

	/**
	 * The subject of the email to send
	 */
	private String emailSubject;

	/**
	 * The text of the email to send
	 */
	private String emailText;

	/**
	 * True if the report should be send as well
	 */
	private boolean attachPdfToEmail;

	/**
	 * List of physician notify via email
	 */
	private List<MedicalFindingsChooser> notificationEmailList;

	/**
	 * List of all templates to select from
	 */
	private PrintTemplate[] printTemplates;
	
	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<PrintTemplate> templateTransformer;
	
	public EmailNotificationSettings(Task task) {
		setNotificationEmailList(MedicalFindingsChooser.getSublist(task.getContacts(), ContactMethod.EMAIL));
		setUseEmail(!getNotificationEmailList().isEmpty() ? true : false);

		setPrintTemplates(PrintTemplate.getTemplatesByTypes(
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN }));

		setTemplateTransformer(new DefaultTransformer<PrintTemplate>(getPrintTemplates()));
		onAttachPdfToEmailChange();
	}

	/**
	 * Checks if pdf should be attached or not, if so a default pdf will be set
	 * for every contact
	 */
	public void onAttachPdfToEmailChange() {

		for (MedicalFindingsChooser notificationChooser : getNotificationEmailList()) {
			if (isAttachPdfToEmail()) {
				if (notificationChooser.getNotificationAttachment() != NotificationOption.NONE) {
					notificationChooser.setNotificationAttachment(NotificationOption.PDF);

					// external physician get an other template then clinic
					// employees, sets the default template
					if (notificationChooser.getContact().getRole() == ContactRole.FAMILY_PHYSICIAN
							|| notificationChooser.getContact().getRole() == ContactRole.PRIVATE_PHYSICIAN)
						notificationChooser.setPrintTemplate(PrintTemplate.getDefaultTemplate(PrintTemplate
								.getTemplatesByType(getPrintTemplates(), DocumentType.DIAGNOSIS_REPORT_EXTERN)));
					else
						notificationChooser.setPrintTemplate(PrintTemplate.getDefaultTemplate(
								PrintTemplate.getTemplatesByType(getPrintTemplates(), DocumentType.DIAGNOSIS_REPORT)));
				}
			} else {
				if (notificationChooser.getNotificationAttachment() != NotificationOption.NONE) {
					notificationChooser.setNotificationAttachment(NotificationOption.TEXT);
					notificationChooser.setPrintTemplate(null);
				}
			}
		}
	}


	/**
	 * Returns an array containing all values of the {@link NotificationOption}
	 * enumeration.
	 * 
	 * @return
	 */
	public NotificationOption[] getNotificationEmailOptions() {
		return new NotificationOption[] { NotificationOption.NONE, NotificationOption.TEXT, NotificationOption.PDF };
	}
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public boolean isUseEmail() {
		return useEmail;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public String getEmailText() {
		return emailText;
	}

	public boolean isAttachPdfToEmail() {
		return attachPdfToEmail;
	}

	public List<MedicalFindingsChooser> getNotificationEmailList() {
		return notificationEmailList;
	}

	public void setUseEmail(boolean useEmail) {
		this.useEmail = useEmail;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public void setEmailText(String emailText) {
		this.emailText = emailText;
	}

	public void setAttachPdfToEmail(boolean attachPdfToEmail) {
		this.attachPdfToEmail = attachPdfToEmail;
	}

	public void setNotificationEmailList(List<MedicalFindingsChooser> notificationEmailList) {
		this.notificationEmailList = notificationEmailList;
	}

	public PrintTemplate[] getPrintTemplates() {
		return printTemplates;
	}

	public void setPrintTemplates(PrintTemplate[] printTemplates) {
		this.printTemplates = printTemplates;
	}

	public DefaultTransformer<PrintTemplate> getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(DefaultTransformer<PrintTemplate> templateTransformer) {
		this.templateTransformer = templateTransformer;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
