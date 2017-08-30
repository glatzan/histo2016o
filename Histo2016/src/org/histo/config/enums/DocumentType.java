package org.histo.config.enums;

/**
 * U_REPORT = Eingangsbogen print at blank page U_REPORT_EMTY = Eingangsbogen
 * print at template, only infill of missing date U_REPORT_COMPLETED = A
 * completed Eingangsbogen with handwriting DIAGNOSIS_REPORT = Report for
 * internal diagnoses, multiple can exist for printing DIAGNOSIS_REPORT_EXTERN =
 * Report for external diagnoses, multiple can exist for printing LABLE = Labels
 * for printing with zebra printers BIOBANK_INFORMED_CONSENT = upload of
 * informed consent for biobank CASE_CONFERENCE = upload and printing of case
 * confernces OTHER
 * 
 * @author andi
 */
public enum DocumentType {
	U_REPORT, U_REPORT_EMTY, U_REPORT_COMPLETED, DIAGNOSIS_REPORT, DIAGNOSIS_REPORT_EXTERN, LABLE, BIOBANK_INFORMED_CONSENT, TEST_LABLE, COUNCIL_REQUEST, COUNCIL_REPLY, OTHER, EMPTY, NOTIFICATION_SEND_REPORT, MEDICAL_FINDINGS_SEND_REPORT_COMPLETED;

	public static DocumentType fromString(String text) {
		for (DocumentType b : DocumentType.values()) {
			if (b.name().equalsIgnoreCase(text)) {
				return b;
			}
		}
		throw new IllegalArgumentException("No constant with text " + text + " found");
	}
}
