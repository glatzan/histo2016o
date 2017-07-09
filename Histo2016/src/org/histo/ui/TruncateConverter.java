package org.histo.ui;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

@FacesConverter("org.histo.ui.TruncateConverter")
public class TruncateConverter implements Converter {

	private int truncateAt = 0;

	private String continuationMark;

	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
		// Should never happend - TruncateConverter is only usable for output.
		throw new AssertionError(getClass().getName() + " does not support Input conversion.");
	}

	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {

		if (value == null) {
			return null;
		}

		try{
			setTruncateAt(Integer.valueOf((String)component.getAttributes().get("truncateAt")));
		}catch(NumberFormatException e){
			return "truncateAT no Int";
		}
		
		setContinuationMark((String)component.getAttributes().get("continuationMark"));
		
		StringBuffer buff = new StringBuffer();
		buff.append(value);

		if (getTruncateAt() > 0 && buff.length() > getTruncateAt()) {
			buff.setLength(getTruncateAt());
			if (getContinuationMark() != null) {
				buff.append(getContinuationMark());
			}
		}

		return buff.toString();
	}

	/**
	 * @return Returns the continuationMark.
	 */
	public String getContinuationMark() {
		return continuationMark;
	}

	/**
	 * @param continuationMark
	 *            The continuationMark to set.
	 */
	public void setContinuationMark(String continuationMark) {
		this.continuationMark = continuationMark;
	}

	/**
	 * @return Returns the truncateAt.
	 */
	public int getTruncateAt() {
		return truncateAt;
	}

	/**
	 * @param truncateAt
	 *            The truncateAt to set.
	 */
	public void setTruncateAt(int truncateAt) {
		this.truncateAt = truncateAt;
	}
}