package org.histo.model.interfaces;

import java.util.List;

import org.histo.model.PDFContainer;

/**
 * Classes have a data list 
 * @author andi
 *
 */
public interface HasDataList extends HasID{
	public List<PDFContainer> getAttachedPdfs();
	public void setAttachedPdfs(List<PDFContainer> attachedPdfs);

	public String getDatalistIdentifier();
}
