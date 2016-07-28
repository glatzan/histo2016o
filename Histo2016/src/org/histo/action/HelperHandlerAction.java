package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.History;
import org.histo.model.Patient;
import org.histo.model.StainingPrototype;
import org.histo.model.UserAcc;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class HelperHandlerAction {

    @Autowired
    private GenericDAO genericDAO;
    @Autowired
    private PatientDao patientDao;
    @Autowired
    private TaskDAO taskDAO;
    @Autowired
    private HelperDAO helperDAO;

    public CustomLog log = new CustomLog();

    /**
     * Shows a Dialog using no options
     * 
     * @param dilalog
     */
    public void showDialog(String dilalog) {
	RequestContext.getCurrentInstance().openDialog(dilalog);
    }

    /**
     * Displays a dilaog using the primefaces dialog engine.
     */
    public void showDialog(String dilalog, boolean resizeable, boolean draggable, boolean modal) {
	showDialog(dilalog, -1, -1, resizeable, draggable, modal);
    }

    /**
     * Displays a dilaog using the primefaces dialog engine.
     * 
     * @param dilalog
     * @param width
     * @param height
     * @param resizeable
     * @param draggable
     * @param modal
     */
    public void showDialog(String dilalog, int width, int height, boolean resizeable, boolean draggable, boolean modal) {
	showDialog(dilalog, width, height, resizeable, draggable, modal, new HashMap<String, Object>());
    }

    /**
     * Opens a dialog. In contrast to the other showDialog methods it is possible to pass custom settings using the options parameter.
     * 
     * @param dilalog
     * @param width
     * @param height
     * @param resizeable
     * @param draggable
     * @param modal
     * @param options
     */
    public void showDialog(String dilalog, int width, int height, boolean resizeable, boolean draggable, boolean modal, Map<String, Object> options) {
	if (width != -1) {
	    options.put("width", width);
	    options.put("contentWidth", "100%");
	} else
	    options.put("width", "auto");

	if (height != -1)
	    options.put("height", height);
	else
	    options.put("height", "auto");

	options.put("resizable", resizeable);
	options.put("draggable", draggable);
	options.put("modal", modal);
	RequestContext.getCurrentInstance().openDialog(dilalog, options, null);
    }

    /**
     * Closes a dialog using primefaces dilaog engine
     * 
     * @param dialog
     */
    public void hideDialog(String dialog) {
	RequestContext.getCurrentInstance().closeDialog(dialog);
    }

    public String simpleDateFormatterUnix(long date) {
	return simpleDateFormatter(new Date(date));
    }

    /**
     * Shows a simple date
     * 
     * @param date
     * @return
     */
    public String simpleDateFormatter(Date date) {
	return dateFormatter(date, "dd MMM yyyy");
    }

    public String dateFormatterUnix(long date, String formatString) {
	return dateFormatter(new Date(date), formatString);
    }

    /**
     * Returns a date formatted with the given string
     * 
     * @param date
     * @param formatString
     * @return
     */
    public String dateFormatter(Date date, String formatString) {
	String dateString = "";

	try {
	    SimpleDateFormat sdfr = new SimpleDateFormat(formatString);
	    dateString = sdfr.format(date);
	} catch (Exception ex) {
	    System.out.println(ex);
	}
	return dateString;
    }

    /**
     * Login Class for storing log-data
     * 
     * @author andi
     *
     */
    class CustomLog {

	public void debug(String message, Logger log) {
	    log(message, log, null, History.LEVEL_DEBUG);
	}

	public void info(String message, Logger log) {
	    log(message, log, null, History.LEVEL_INFO);
	}

	public void info(String message, Logger log, Patient patient) {
	    log(message, log, patient, History.LEVEL_INFO);
	}

	public void log(String message, Logger log, Patient patient, int level) {
	    UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

	    History newHistroy = new History();
	    newHistroy.setLevel(level);
	    newHistroy.setUserAcc(user);
	    newHistroy.setPatient(patient);
	    newHistroy.setMessages(message);
	    newHistroy.setDate(System.currentTimeMillis());

	    genericDAO.save(newHistroy);

	    log.info(user.getUsername() + " - " + message);
	}

	public void print(String message, Logger log) {
	    UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	    log.info("#" + user.getUsername() + " - " + message);
	}
    };

    public void timeout() throws IOException {
	showDialog(HistoSettings.dialog(HistoSettings.DIALOG_LOGOUT));
	FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
	FacesContext.getCurrentInstance().getExternalContext().redirect("/Histo16/login.xhtml");

    }

}