package org.histo.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;

public class WorklistSortUtil {



	/**
	 * Sorts an array list of patients by the piz.
	 * @param patiens
	 * @param asc
	 * @return
	 */
	public static final List<Patient> orderListByPIZ(List<Patient> patiens, boolean asc) {

		// Sorting
		Collections.sort(patiens, new Comparator<Patient>() {
			@Override
			public int compare(Patient patientOne, Patient patientTwo) {
				if (patientOne.getPiz() == null && patientTwo.getPiz() == null)
					return 0;
				else if (patientOne.getPiz() == null)
					return asc ? -1 : 1;
				else if (patientTwo.getPiz() == null)
					return asc ? 1 : -1;
				else {
					int res = patientOne.getPiz().compareTo(patientTwo.getPiz());
					return asc ? res : res * -1;
				}
			}
		});

		return patiens;
	}

	/**
	 * sorts an array list of patients ascending or descending by name.
	 * @param patiens
	 * @param asc
	 * @return
	 */
	public static final List<Patient> orderListByName(List<Patient> patiens, boolean asc) {

		// Sorting
		Collections.sort(patiens, new Comparator<Patient>() {
			@Override
			public int compare(Patient patientOne, Patient patientTwo) {
				if (patientOne.getPerson().getName() == null && patientTwo.getPerson().getName() == null)
					return 0;
				else if (patientOne.getPerson().getName() == null)
					return asc ? -1 : 1;
				else if (patientTwo.getPerson().getName() == null)
					return asc ? 1 : -1;
				else {
					int res = patientOne.getPerson().getName().compareTo(patientTwo.getPerson().getName());
					return asc ? res : res * -1;
				}
			}
		});

		return patiens;
	}
	

}
