package org.histo.worklist.search;

import java.util.ArrayList;
import java.util.List;

import org.histo.dao.FavouriteListDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class WorklistFavouriteSearch extends WorklistSearch {

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	private FavouriteList favouriteList;

	@Override
	public List<Patient> getWorklist() {
		logger.debug("Searching current worklist");

		List<Patient> patient = favouriteListDAO.getPatientFromFavouriteList(favouriteList.getId(), true);

		return patient;
	}
}
