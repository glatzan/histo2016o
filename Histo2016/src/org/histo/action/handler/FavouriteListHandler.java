package org.histo.action.handler;

import org.apache.log4j.Logger;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.FavouriteList;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.net.httpserver.Authenticator.Success;

@Component
@Scope(value = "session")
public class FavouriteListHandler {

//	private static Logger logger = Logger.getLogger("org.histo");
//
//	public static final int StainingList_ID = 1;
//	public static final int ReStainingList_ID = 2;
//	public static final int DiagnosisList_ID = 3;
//	public static final int ReDiagnosisList_ID = 4;
//	public static final int NotificationList_ID = 5;
//
//	public static final int RETRY_SAVE_TIMES = 3;
//
//	@Autowired
//	private FavouriteListDAO favouriteListDAO;
//
//	@Autowired
//	private GenericDAO genericDAO;
//
//	public boolean addTaskToList(Task task, long id) {
//		FavouriteList favouriteList = favouriteListDAO.getFavouriteListById(id);
//		return addTaskToList(task, favouriteList);
//	}
//
//	public boolean addTaskToList(Task task, FavouriteList favouriteList) {
//
//		if (favouriteList == null)
//			return false;
//
//		int retry = 0;
//		boolean success;
//
//		do {
//			favouriteList = genericDAO.updateRollbackSave(favouriteList);
//			favouriteList.getTasks().add(task);
//			success = favouriteListDAO.saveFavouriteList(favouriteList, "log.favouriteList.addTask",
//					new Object[] { task.getLogPath(), favouriteList });
//			retry++;
//			logger.info("Updating FavouriteList " + favouriteList.getName() + ", Try " + retry);
//		} while (success && retry < RETRY_SAVE_TIMES);
//
//		return success;
//	}
//
//	public boolean removeTaskFormList(Task task, long id) {
//		FavouriteList favouriteList = favouriteListDAO.getFavouriteListById(id);
//		return removeTaskFormList(task, favouriteList);
//	}
//
//	public boolean removeTaskFormList(Task task, FavouriteList favouriteList) {
//		if (favouriteList == null)
//			return false;
//
//		int retry = 0;
//		boolean success;
//
//		do {
//			favouriteList = genericDAO.updateRollbackSave(favouriteList);
//			favouriteList.getTasks().remove(task);
//			success = favouriteListDAO.saveFavouriteList(favouriteList, "log.favouriteList.addTask",
//					new Object[] { task.getLogPath(), favouriteList });
//			retry++;
//			logger.info("Updating FavouriteList " + favouriteList.getName() + ", Try " + retry);
//		} while (success && retry < RETRY_SAVE_TIMES);
//
//		return success;
//
//	}
	
}
