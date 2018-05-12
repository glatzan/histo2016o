package org.histo.service;

import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.util.dataList.HasDataList;

public interface PDFService {
	public void attachPDF(HasDataList dataList, PDFContainer pdfContainer);

	public void attachPDF(Patient patient, HasDataList dataList, PDFContainer pdfContainer);

	public void removePdf(HasDataList dataList, PDFContainer pdfContainer);

	public void movePdf();

	public void copyPdf();
}
