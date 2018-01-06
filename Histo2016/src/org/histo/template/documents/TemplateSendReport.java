package org.histo.template.documents;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.notification.NotificationContainer;
import org.histo.util.pdf.PDFGenerator;

public class TemplateSendReport extends DocumentTemplate {
	private Patient patient;

	private Task task;

	private boolean useMail;
	private List<NotificationContainer> mailHolders;
	private boolean useFax;
	private List<NotificationContainer> faxHolders;
	private boolean useLetter;
	private List<NotificationContainer> letterHolders;
	private boolean usePhone;
	private List<NotificationContainer> phoneHolders;

	private Date dateOfReport;

	/**
	 * Arguments: 0 = task, 1 = useMail (bool), 2 useFax (bool), 3 = useLetter
	 * (bool), 4 = usePhone (bool), 5 = mailHolders (List<NotificationContainer>), 6
	 * = faxHolders (List<NotificationContainer>), 7 = letterHolders
	 * (List<NotificationContainer>), 8 = phoneHolders
	 * (List<NotificationContainer>), 9 = dateOfReport (date), 10 = list
	 */
	@Override
	public void initializeTempalte(Object... objects) {
		if (objects.length != 11)
			throw new IllegalArgumentException("Number of Arguments does not match");

		if (!(objects[0] instanceof Task))
			throw new IllegalArgumentException("Type of Argument 0 (task) does not match");

		this.patient = ((Task) objects[0]).getPatient();
		this.task = ((Task) objects[0]);

		if (!(objects[1] instanceof Boolean || objects[2] instanceof Boolean || objects[3] instanceof Boolean
				|| objects[4] instanceof Boolean))
			throw new IllegalArgumentException("Type of Argument 2-4 (bool) does not match");

		this.useMail = (Boolean) objects[1];
		this.useFax = (Boolean) objects[2];
		this.useLetter = (Boolean) objects[3];
		this.usePhone = (Boolean) objects[4];

		if (!(objects[5] instanceof List<?> || objects[6] instanceof List<?> || objects[7] instanceof List<?>
				|| objects[8] instanceof List<?>))
			throw new IllegalArgumentException("Type of Argument 5-8 (NotificationContainer) does not match");

		this.mailHolders = (List<NotificationContainer>) objects[5];
		this.faxHolders = (List<NotificationContainer>) objects[6];
		this.letterHolders = (List<NotificationContainer>) objects[7];
		this.phoneHolders = (List<NotificationContainer>) objects[8];

		if (!(objects[9] instanceof Date))
			throw new IllegalArgumentException("Type of Argument 9 (Date) does not match");

		this.dateOfReport = (Date) objects[9];

	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("useMail", useMail);
		generator.getConverter().replace("mailHolders", mailHolders);
		generator.getConverter().replace("useFax", useFax);
		generator.getConverter().replace("faxHolders", faxHolders);
		generator.getConverter().replace("useLetter", useLetter);
		generator.getConverter().replace("letterHolders", letterHolders);
		generator.getConverter().replace("usePhone", usePhone);
		generator.getConverter().replace("phoneHolders", phoneHolders);
		generator.getConverter().replace("reportDate", dateOfReport);
		generator.getConverter().replace("date", new DateTool());
	}

	public PDFContainer onAfterPDFCreation(PDFContainer container) {
		List<PDFContainer> attachPdf = new ArrayList<PDFContainer>();

		attachPdf.add(container);

		attachPdf.addAll(
				mailHolders.stream().filter(p -> p.getPdf() != null).map(p -> p.getPdf()).collect(Collectors.toList()));
		attachPdf.addAll(
				faxHolders.stream().filter(p -> p.getPdf() != null).map(p -> p.getPdf()).collect(Collectors.toList()));
		attachPdf.addAll(letterHolders.stream().filter(p -> p.getPdf() != null).map(p -> p.getPdf())
				.collect(Collectors.toList()));
		attachPdf.addAll(phoneHolders.stream().filter(p -> p.getPdf() != null).map(p -> p.getPdf())
				.collect(Collectors.toList()));

		return PDFGenerator.mergePdfs(attachPdf, "Send Report", DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);
	}
}
