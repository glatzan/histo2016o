package org.histo.template.documents;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.velocity.tools.generic.DateTool;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.util.notification.MailContainerList;
import org.histo.util.notification.NotificationContainerList;
import org.histo.util.pdf.PDFGenerator;

public class SendReport extends DocumentTemplate {

	private MailContainerList mailContainerList;
	private NotificationContainerList faxContainerList;
	private NotificationContainerList letterContainerList;
	private NotificationContainerList phoneContainerList;

	private Date dateOfReport;
	
	private boolean temporarayNotification;

	public void initializeTempalte(Task task, MailContainerList mailContainerList,
			NotificationContainerList faxContainerList, NotificationContainerList letterContainerList,
			NotificationContainerList phoneContaienrList, Date dateOfReport, boolean temporarayNotification) {

		this.afterPDFCreationHook = true;
		
		super.initData(task);

		this.mailContainerList = mailContainerList;
		this.faxContainerList = faxContainerList;
		this.letterContainerList = letterContainerList;
		this.phoneContainerList = phoneContaienrList;

		this.dateOfReport = dateOfReport;
		this.temporarayNotification = temporarayNotification;
	}

	public void fillTemplate(PDFGenerator generator) {
		generator.getConverter().replace("patient", patient);
		generator.getConverter().replace("task", task);
		generator.getConverter().replace("temporarayNotification", temporarayNotification);
		generator.getConverter().replace("useMail", mailContainerList.isUse());
		generator.getConverter().replace("mailHolders", mailContainerList.getContainerToNotify());
		generator.getConverter().replace("useFax", faxContainerList.isUse());
		generator.getConverter().replace("faxHolders", faxContainerList.getContainerToNotify());
		generator.getConverter().replace("useLetter", letterContainerList.isUse());
		generator.getConverter().replace("letterHolders", letterContainerList.getContainerToNotify());
		generator.getConverter().replace("usePhone", phoneContainerList.isUse());
		generator.getConverter().replace("phoneHolders", phoneContainerList.getContainerToNotify());
		generator.getConverter().replace("reportDate", dateOfReport);
		generator.getConverter().replace("date", new DateTool());
	}

	public PDFContainer onAfterPDFCreation(PDFContainer container) {
		List<PDFContainer> attachPdf = new ArrayList<PDFContainer>();

		attachPdf.add(container);

		attachPdf.addAll(mailContainerList.getContainerToNotify().stream().filter(p -> p.getPdf() != null)
				.map(p -> p.getPdf()).collect(Collectors.toList()));
		attachPdf.addAll(faxContainerList.getContainerToNotify().stream().filter(p -> p.getPdf() != null)
				.map(p -> p.getPdf()).collect(Collectors.toList()));
		attachPdf.addAll(letterContainerList.getContainerToNotify().stream().filter(p -> p.getPdf() != null)
				.map(p -> p.getPdf()).collect(Collectors.toList()));
		attachPdf.addAll(phoneContainerList.getContainerToNotify().stream().filter(p -> p.getPdf() != null)
				.map(p -> p.getPdf()).collect(Collectors.toList()));

		return PDFGenerator.mergePdfs(attachPdf, "Send Report", DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);
	}
}
