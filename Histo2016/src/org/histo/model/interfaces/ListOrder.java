package org.histo.model.interfaces;

import java.util.List;

public interface ListOrder<T extends ListOrder<?>> {

	@SuppressWarnings("unchecked")
	public default boolean moveOrderUp(List<T> parentList) {
		int index = parentList.indexOf(this);
		if (index > 0) {

			parentList.remove(this);
			parentList.add(index - 1, (T)this);
			
			reOrderList(parentList);
			
			return true;
		}

		return false;
	}
	
	@SuppressWarnings("unchecked")
	public default boolean moveOrderDown(List<T> parentList) {
		int index = parentList.indexOf(this);
		if (index < parentList.size()-1) {

			parentList.remove(this);
			parentList.add(index+1, (T)this);
			
			reOrderList(parentList);
			
			return true;
		}

		return false;
	}
	
	public static void reOrderList(List<? extends ListOrder<?>> parentList){
		int i = 0;
		for (ListOrder<?> listOrder : parentList) {
			listOrder.setIndexInList(i);
			i++;
		}
	}
	
	public int getIndexInList();

	public void setIndexInList(int indexInList);
}
