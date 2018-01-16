package org.histo.util.pdf;

import org.histo.model.PDFContainer;
import org.histo.template.DocumentTemplate;

import lombok.Getter;
import lombok.Setter;

/**
 * Is returned by template in order to print.
 * 
 * @author andi
 *
 */
@Getter
@Setter
public class PrintOrder {

	private PDFContainer pdfContainer;
	private int copies;
	private boolean duplex;
	private String args;

	public PrintOrder(PDFContainer container) {
		this(container, null);
	}

	public PrintOrder(PDFContainer container, DocumentTemplate documentTemplate) {
		this.pdfContainer = container;
		if (documentTemplate != null) {
			this.duplex = documentTemplate.isPrintDuplex();
			this.args = documentTemplate.getAttributes();
			this.copies = documentTemplate.getCopies();
		}
	}

	public PrintOrder(PDFContainer container, int copies, boolean duplex, String args) {
		this.pdfContainer = container;
		this.duplex = duplex;
		this.args = args;
		this.copies = copies;
	}
}
