package org.histo.util.dataList;

import java.util.List;

import javax.persistence.Transient;

import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasID;

/**
 * Classes have a data list
 * 
 * @author andi
 *
 */
public interface HasDataList extends HasID {
	public List<PDFContainer> getAttachedPdfs();

	public void setAttachedPdfs(List<PDFContainer> attachedPdfs);

	public String getDatalistIdentifier();

	public default boolean contaisPDFofType(DocumentType type) {
		if (getAttachedPdfs() != null) {
			return getAttachedPdfs().stream().anyMatch(p -> p.getType().equals(type));
		}

		return false;
	}

	public default void addReport(PDFContainer pdfTemplate) {
		getAttachedPdfs().add(pdfTemplate);
	}

	/**
	 * Removes a report with a specific type from the database
	 * 
	 * @param type
	 * @return
	 */
	@Transient
	public default PDFContainer removeReport(DocumentType type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType().equals(type)) {
				getAttachedPdfs().remove(pdfContainer);
				return pdfContainer;
			}
		}
		return null;
	}
}
