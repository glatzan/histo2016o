package org.histo.util.dataList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.histo.model.PDFContainer;

/**
 * Default class for displaying media 
 * @author andi
 *
 */
public class DefaultDataList implements HasDataList {

	private List<PDFContainer> attachedPdfs = new ArrayList<PDFContainer>();

	public DefaultDataList(PDFContainer container) {
		this(Arrays.asList(container));
	}

	public DefaultDataList(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
	}

	@Override
	public long getId() {
		return 0;
	}

	@Override
	public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
	}

	@Override
	public String getDatalistIdentifier() {
		return "";
	}

	@Override
	public List<PDFContainer> getAttachedPdfs() {
		return attachedPdfs;
	}

}
