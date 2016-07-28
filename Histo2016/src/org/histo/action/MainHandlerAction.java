package org.histo.action;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PatientDao;
import org.histo.model.History;
import org.histo.model.Patient;
import org.histo.model.Person;
import org.histo.model.UserAcc;
import org.histo.model.UserRole;
import org.histo.util.UserUtil;
import org.primefaces.context.RequestContext;
import org.primefaces.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class MainHandlerAction implements Serializable {

    
    @Autowired
    PatientDao patientDao;
    @Autowired
    GenericDAO genericDAO;
    @Autowired
    HelperDAO helperDAO;

    public void echo(String echo){
	System.out.println(echo + " !");
	System.out.println(Constants.LIBRARY);
	System.out.println(RequestContext.getCurrentInstance().getApplicationContext().getConfig().getBuildVersion());
    }
}
