package org.histo.model;

import java.util.List;

import org.histo.model.patient.Task;

public class FavouriteList {

	private long id;
	private HistoUser owner;
	private String name;
	
	private List<Task> tasks;
	
}
