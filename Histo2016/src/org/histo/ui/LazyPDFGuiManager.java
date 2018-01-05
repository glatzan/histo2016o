package org.histo.ui;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.apache.log4j.Logger;
import org.histo.model.PDFContainer;
import org.histo.template.DocumentTemplate;
import org.histo.ui.interfaces.PdfStreamProvider;
import org.histo.util.pdf.LazyPDFReturnHandler;
import org.histo.util.pdf.PDFGenerator;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

@Getter
@Setter
/**
 * Class for lazy media creation, used with dynamicMedia component
 * 
 * @author andi
 *
 */
public class LazyPDFGuiManager implements PdfStreamProvider, LazyPDFReturnHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	/**
	 * If true the component wil be rendered.
	 */
	private boolean renderComponent;

	/**
	 * If true the poll element at the view page will start
	 */
	private AtomicBoolean stopPoll = new AtomicBoolean(true);

	/**
	 * If true the poll element will start
	 */
	private AtomicBoolean autoStartPoll = new AtomicBoolean(false);

	/**
	 * If true the pdf will be rendered
	 */
	private AtomicBoolean renderPDF = new AtomicBoolean(false);

	/**
	 * pdf container
	 */
	private PDFContainer PDFContainerToRender;

	/**
	 * Thread id of the last pdf generating thread
	 */
	private String currentTaskUuid;

	/**
	 * Resets render state
	 */
	public void reset() {
		getRenderPDF().set(false);
		getStopPoll().set(true);
		getAutoStartPoll().set(false);
		setCurrentTaskUuid("");
	}

	/**
	 * If the pdf was created manually is can be set using this method.
	 * 
	 * @param container
	 */
	public void setManuallyCreatedPDF(PDFContainer container) {
		setPDFContainerToRender(container);
		getRenderPDF().set(true);
		getStopPoll().set(true);
		getAutoStartPoll().set(false);
	}

	/**
	 * Starts rendering in other thread
	 * 
	 * @param template
	 */
	public void startRendering(DocumentTemplate template) {
		currentTaskUuid = new PDFGenerator().getPDFNoneBlocking(template, this);
		getStopPoll().set(false);
		getAutoStartPoll().set(true);
	}

	/**
	 * Return method for thread
	 */
	@Override
	@Synchronized
	public void returnPDFContent(PDFContainer container, String uuid) {
		if (getCurrentTaskUuid().equals(uuid)) {
			logger.debug("Setting PDf for rendering");
			setPDFContainerToRender(container);
			getRenderPDF().set(true);
			getStopPoll().set(true);
			getAutoStartPoll().set(false);
		} else {
			logger.debug("More then one Thread! Old Thread");
		}
	}

	/**
	 * Returns the pdf as stream
	 */
	public StreamedContent getPdfContent() {
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

	@Synchronized
	public PDFContainer getPDFContainerToRender() {
		return PDFContainerToRender;
	}

	@Synchronized
	public void setPDFContainerToRender(PDFContainer container) {
		this.PDFContainerToRender = container;
	}
}
