package org.histo.model.transitory.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultDocuments {

	/**
	 * Document-Template which is used on diagnosis phase exit.
	 */
	private long diagnosisApprovedDocument;

	/**
	 * Document which can be printed on task creation.
	 */
	private long taskCreationDocument;

	/**
	 * Default document for email notification
	 */
	private long notificationDefaultEmailDocument;

	/**
	 * Default Email template which is used to notify physicians if task was
	 * completed
	 */
	private long notificationDefaultEmail;

	/**
	 * Default document for fax notification
	 */
	private long notificationDefaultFaxDocument;

	/**
	 * Default document for letter notification
	 */
	private long notificationDefaultLetterDocument;

	/**
	 * Default document for printing in order to sign
	 */
	private long notificationDefaultPrintDocument;

	/**
	 * Sendreport which is created after the notification dialog was processed.
	 */
	private long notificationSendReport;
}
