package org.histo.model.interfaces;

import java.util.List;

import javax.persistence.Transient;

import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;

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
}
