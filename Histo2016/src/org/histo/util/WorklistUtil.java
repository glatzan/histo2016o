package org.histo.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.histo.model.Patient;

public class WorklistUtil {

	/**
	 * Sorts a List of patients by the task id. The tasknumber will be ascending
	 * or descending depending on the asc parameter.
	 * 
	 * @param patiens
	 * @return
	 */
	public List<Patient> orderListByTaskID(List<Patient> patiens, boolean asc) {

		// Sorting
		Collections.sort(patiens, new Comparator<Patient>() {
			@Override
			public int compare(Patient patientOne, Patient patientTwo) {
				//patientOne.getActivTasks()
				// TODO Auto-generated method stub
				return 0;
			}
		});

		return patiens;
	}
}
