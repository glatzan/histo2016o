package org.histo.model.interfaces;

import org.histo.util.TimeUtil;

public interface StainingInfo extends CreationDate {

	public default boolean isNew() {
		if (TimeUtil.isDateOnSameDay(getCreationDate(), System.currentTimeMillis()))
			return true;
		return false;
	}

	public boolean isStainingPerformed();

	public boolean isStainingNeeded();

	public boolean isRestainingNeeded();
}
