package org.histo.dao;

import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PdfDAO extends AbstractDAO {

	public void attachPDF(Task task, PDFContainer pdfContainer) {
		
	}
}
