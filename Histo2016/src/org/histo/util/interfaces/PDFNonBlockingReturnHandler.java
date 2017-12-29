package org.histo.util.interfaces;

import org.histo.model.PDFContainer;

public interface PDFNonBlockingReturnHandler {
	public void setPDFContent(PDFContainer container, String uuid);
}
