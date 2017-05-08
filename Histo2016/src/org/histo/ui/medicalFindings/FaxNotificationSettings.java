package org.histo.ui.medicalFindings;

import java.util.List;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.NotificationOption;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.printer.PrintTemplate;

public class FaxNotificationSettings {
	/**
	 * True if fax should be send;
	 */
	private boolean useFax;

	/**
	 * List of physician notify via fax
	 */
	private List<MedicalFindingsChooser> notificationFaxList;

	/**
	 * List of all templates to select from
	 */
	private PrintTemplate[] printTemplates;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<PrintTemplate> templateTransformer;
	
	/**
	 * Temporary Task
	 */
	private Task task;
	
	public FaxNotificationSettings(Task task) {
		setTask(task);
		updateNotificationFaxList();
		setUseFax(!getNotificationFaxList().isEmpty() ? true : false);

		setPrintTemplates(PrintTemplate.getTemplatesByTypes(
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN }));

		setTemplateTransformer(new DefaultTransformer<PrintTemplate>(getPrintTemplates()));
	}

	/**
	 * Checks if pdf should be attached or not, if so a default pdf will be set
	 * for every contact
	 */
	public void onAttachPdfToFaxChange() {

		for (MedicalFindingsChooser notificationChooser : getNotificationFaxList()) {
			
			if (notificationChooser.getNotificationAttachment() != NotificationOption.NONE) {

				if(notificationChooser.getContact().isFaxNotificationPerformed()){
					notificationChooser.setNotificationAttachment(NotificationOption.NONE);
					continue;
				}
				
				notificationChooser.setNotificationAttachment(NotificationOption.FAX);
				
				// external physician get an other template then clinic
				// employees, sets the default template
				if (notificationChooser.getContact().getRole() == ContactRole.FAMILY_PHYSICIAN
						|| notificationChooser.getContact().getRole() == ContactRole.PRIVATE_PHYSICIAN)
					notificationChooser.setPrintTemplate(PrintTemplate.getDefaultTemplate(PrintTemplate
							.getTemplatesByType(getPrintTemplates(), DocumentType.DIAGNOSIS_REPORT_EXTERN)));
				else
					notificationChooser.setPrintTemplate(PrintTemplate.getDefaultTemplate(
							PrintTemplate.getTemplatesByType(getPrintTemplates(), DocumentType.DIAGNOSIS_REPORT)));
			} else {
				notificationChooser.setPrintTemplate(null);
			}
		}
	}
	
	/**
	 * Updates the fax list, if the contact page was used to edit contacts.
	 */
	public void updateNotificationFaxList(){
		// TODO don't overwrite old list!
		
		setNotificationFaxList(MedicalFindingsChooser.getSublist(task.getContacts(), ContactMethod.FAX));
		onAttachPdfToFaxChange();
	}
	
	/**
	 * Returns an array containing all values of the {@link NotificationOption}
	 * enumeration.
	 * 
	 * @return
	 */
	public NotificationOption[] getNotificationFaxOptions() {
		return new NotificationOption[] { NotificationOption.NONE, NotificationOption.FAX };
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public boolean isUseFax() {
		return useFax;
	}

	public List<MedicalFindingsChooser> getNotificationFaxList() {
		return notificationFaxList;
	}

	public PrintTemplate[] getPrintTemplates() {
		return printTemplates;
	}

	public void setUseFax(boolean useFax) {
		this.useFax = useFax;
	}

	public void setNotificationFaxList(List<MedicalFindingsChooser> notificationFaxList) {
		this.notificationFaxList = notificationFaxList;
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

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
