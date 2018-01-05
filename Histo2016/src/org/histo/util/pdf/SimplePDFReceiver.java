package org.histo.util.pdf;

import org.histo.model.PDFContainer;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

/**
 * Simple return handler for thread based pdf generation
 * 
 * @author andi
 *
 */
@Getter
@Setter
public class SimplePDFReceiver implements LazyPDFReturnHandler {

	private String taskID;

	private PDFContainer container;

	@Override
	@Synchronized
	public void returnPDFContent(PDFContainer container, String uuid) {
		if (taskID != null && taskID.equals(uuid)) {
			setContainer(container);
		}
	}

	@Synchronized
	public PDFContainer getContainer() {
		return container;
	}

	@Synchronized
	public void setContainer(PDFContainer container) {
		this.container = container;
	}
}
