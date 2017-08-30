package org.histo.template.documents;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.action.dialog.notification.NotificationDialog.AbstractTab.ContactHolder;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.PDFGenerator;

public class TemplateSendReport extends DocumentTemplate {
	private Patient patient;

	private Task task;

	private List<ContactHolder> mailHolders;
	private List<ContactHolder> faxHolders;
	private List<ContactHolder> letterHolders;
	private List<ContactHolder> phoneHolders;

	private Date dateOfReport;

	public void initData(Patient patient, Task task, List<ContactHolder> mailHolders, List<ContactHolder> faxHolders,
			List<ContactHolder> letterHolders, List<ContactHolder> phoneHolders, Date dateOfReport) {
		this.patient = patient;
		this.task = task;

		this.mailHolders = mailHolders;
		this.faxHolders = faxHolders;
		this.letterHolders = letterHolders;
		this.phoneHolders = phoneHolders;

		this.dateOfReport = dateOfReport;

	}

	public PDFContainer generatePDF(PDFGenerator generator) {
		generator.openNewPDf(this);

		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("mailHolders", mailHolders);
		generator.getConverter().replace("faxHolders", faxHolders);
		generator.getConverter().replace("letterHolders", letterHolders);
		generator.getConverter().replace("phoneHolders", phoneHolders);
		generator.getConverter().replace("reportDate", dateOfReport);
		generator.getConverter().replace("date", new DateTool());

		List<PDFContainer> attachPdf = new ArrayList<PDFContainer>();

		attachPdf.add(generator.generatePDF());

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
