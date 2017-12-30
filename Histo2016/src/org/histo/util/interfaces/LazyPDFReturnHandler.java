package org.histo.util.interfaces;

import org.histo.model.PDFContainer;

public interface LazyPDFReturnHandler {
	public void returnPDFContent(PDFContainer container, String uuid);
}
