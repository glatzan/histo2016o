package org.histo.ui;

public class ListChooser<T> {

	private T listItem;

	private boolean choosen;

	public ListChooser(T item) {
		this.listItem = item;
	}

	public T getListItem() {
		return listItem;
	}

	public void setListItem(T listItem) {
		this.listItem = listItem;
	}

	public boolean isChoosen() {
		return choosen;
	}

	public void setChoosen(boolean choosen) {
		this.choosen = choosen;
	}
}
