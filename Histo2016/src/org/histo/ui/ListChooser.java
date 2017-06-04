package org.histo.ui;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ListChooser<T> {

	private T listItem;

	/**
	 * Unique, temporary id of the object
	 */
	private int id;

	/**
	 * True if selected
	 */
	private boolean choosen;

	public static final <T> List<ListChooser<T>> getListAsIDList(List<T> items) {
		AtomicInteger i = new AtomicInteger(0);
		return items.stream().map(p -> new ListChooser<T>(p, i.getAndIncrement())).collect(Collectors.toList());
	}

	public ListChooser(T item) {
		this.listItem = item;
	}

	public ListChooser(T item, int id) {
		this.listItem = item;
		this.id = id;
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
