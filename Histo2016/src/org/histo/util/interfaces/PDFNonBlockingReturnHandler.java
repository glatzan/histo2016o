package org.histo.util.interfaces;

import org.histo.model.PDFContainer;

public interface PDFNonBlockingReturnHandler {

	public void setPDFContent(PDFContainer container);
	
	public void setPDFGenerationStatus(boolean status);
}
