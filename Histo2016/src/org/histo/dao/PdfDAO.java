package org.histo.dao;

import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
// TODO implement, see mediaDialog (remove pdf)
public class PdfDAO extends AbstractDAO {

	public void attachPDF(HasDataList dataList, PDFContainer pdfContainer) {
		dataList.getAttachedPdfs().add(pdfContainer);
		save(dataList, "log.pdf.attached", new Object[] { dataList.toString(), pdfContainer.toString() });
	}

	public void removePdf(HasDataList dataList, PDFContainer pdfContainer) {

	}

	public void movePdf() {

	}

	public void copyPdf() {

	}
}
