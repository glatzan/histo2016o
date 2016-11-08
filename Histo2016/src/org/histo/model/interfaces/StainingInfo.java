package org.histo.model.interfaces;

import java.util.List;

import org.histo.config.enums.StainingStatus;
import org.histo.util.TimeUtil;

public interface StainingInfo<T extends StainingInfo<?> & ArchivAble & CreationDate>{
	
	public boolean isNew();
	
	public default boolean isNew(long date){
		if (TimeUtil.isDateOnSameDay(date, System.currentTimeMillis()))
			return true;
		return false;
	}
	
	public  StainingStatus getStainingStatus();
	
	public default StainingStatus getStainingStatus(List<T> list){
		// if empty return staining needed
		if (list.isEmpty())
			return StainingStatus.STAINING_NEEDED;

		boolean stainingNeeded = false;

		for (T listObjects : list) {
			// contiune if archived
			if (listObjects.isArchived())
				continue;
			
			StainingStatus stainingStatusofChild = listObjects.getStainingStatus();
			
			// continue if no staining is needed
			if (stainingStatusofChild == StainingStatus.PERFORMED)
				continue;
			else {
				// check if restaining is needed (restaining > staining) so
				// return that it is needed
				if (stainingStatusofChild == StainingStatus.RE_STAINING_NEEDED)
					return StainingStatus.RE_STAINING_NEEDED;
				else
					stainingNeeded = true;
			}
		}
		return stainingNeeded ? StainingStatus.STAINING_NEEDED : StainingStatus.PERFORMED;
	}
}
