package org.histo.ui.interfaces;

import java.io.ByteArrayInputStream;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.model.PDFContainer;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

public interface PdfGuiProvider {

	/**
	 * PDF to render
	 * 
	 * @return
	 */
	public PDFContainer getPDFContainerToRender();

	/**
	 * Determines if the waiting poll should start
	 * 
	 * @return
	 */
	public default boolean isAutoStartPoll() {
		return false;
	}

	/**
	 * Determines if the waiting poll should be stooped
	 * 
	 * @return
	 */
	public default boolean isStopPoll() {
		return false;
	}

	/**
	 * Content-Stream for rendering by gui
	 * 
	 * @return
	 */
	public default StreamedContent getPdfContent() {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE || getPDFContainerToRender() == null) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream(getPDFContainerToRender().getData()),
					"application/pdf", getPDFContainerToRender().getName());
		}
	}
}
