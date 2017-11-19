package org.histo.worklist.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.model.patient.Patient;

public class WorklistSearch {
	
	protected static Logger logger = Logger.getLogger("org.histo");
	
	public List<Patient> getWorklist() {
		return new ArrayList<Patient>();
	}
}
