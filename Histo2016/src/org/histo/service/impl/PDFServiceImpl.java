package org.histo.service.impl;

import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.service.PDFService;
import org.histo.service.dao.PDFDao;
import org.histo.util.dataList.HasDataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
@Service
@Getter
@Setter
public class PDFServiceImpl extends AbstractService implements PDFService {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PDFDao pDFDao;

	public void attachPDF(HasDataList dataList, PDFContainer pdfContainer) {
		attachPDF(null, dataList, pdfContainer);
	}

	public void attachPDF(Patient patient, HasDataList dataList, PDFContainer pdfContainer) {
		dataList.getAttachedPdfs().add(pdfContainer);
		pDFDao.save(dataList, patient, "log.pdf.attached", dataList.toString(), pdfContainer.toString());
	}

	public void removePdf(HasDataList dataList, PDFContainer pdfContainer) {

	}

	public void movePdf() {

	}

	public void copyPdf() {

	}
}
