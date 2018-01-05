package org.histo.util.notification;

import java.util.Date;

import org.apache.log4j.Logger;
import org.histo.model.AssociatedContact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.util.pdf.PDFGenerator;

/**
 * Class for executing notification. Should be extended by notification types.
 */
public class NotificationExecutor<T extends NotificationContainer> {

	protected static Logger logger = Logger.getLogger("org.histo");

	protected PDFContainer genericPDF;

	/**
	 * Pattern check of the Address, returns true if matches
	 * 
	 * @return
	 */
	public boolean isAddressApproved(String address) {
		return false;
	}

	/**
	 * Returns a pdf container for an contact. IF the contact has its own container,
	 * that container is returned. IF individualAddresses is true a new pdf with the
	 * address of the of the container will be generated. Otherwise a generic pdf
	 * will be returned.
	 */
	public PDFContainer getPDF(T container, Task task, TemplateDiagnosisReport template, boolean individualAddresses) {
		if (container.getPdf() != null) {
			// pdf was selected for the individual
			// contact
			return container.getPdf();
		} else if (template != null) {

			// generating pdf for contact
			PDFContainer result;

			if (!individualAddresses) {
				// creating default report if not present
				if (genericPDF == null) {
					logger.debug("Generating generic pdf");
					template.initData(task.getPatient(), task, "");
					genericPDF = new PDFGenerator().getPDF(template);
				}

				result = genericPDF;
			} else {
				// individual address
				String reportAddressField = AssociatedContact.generateAddress(container.getContact());
				logger.debug("Generating pdf for " + reportAddressField);
				template.initData(task.getPatient(), task, reportAddressField);
				result = new PDFGenerator().getPDF(template);
			}

			return result;
		}

		return null;
	}

	/**
	 * Should perform the notification
	 * 
	 * @param container
	 * @return
	 */
	public boolean performSend(T container) {
		return false;
	}

	/**
	 * Sets data for the AssociatedContactNotification on notification performed.
	 * 
	 * @param container
	 * @param success
	 * @param message
	 */
	public void finishSendProecess(T container, boolean success, String message) {
		container.getNotification().setPerformed(true);
		container.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
		container.getNotification().setCommentary(message);
		// if success = performed, nothing to do = inactive, if failed = active
		container.getNotification().setActive(!success);
		// if success = !failed = false
		container.getNotification().setFailed(!success);
	}
}
