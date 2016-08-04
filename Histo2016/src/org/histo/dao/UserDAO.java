package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Person;
import org.histo.model.StainingPrototype;
import org.histo.model.UserAcc;
import org.histo.model.UserRole;
import org.histo.util.UserUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserDAO extends AbstractDAO implements Serializable {

	public List<UserAcc> loadAllUsers() {
		Criteria c = getSession().createCriteria(UserAcc.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public UserAcc loadUserByName(String name) {
		Criteria c = getSession().createCriteria(UserAcc.class);
		c.add(Restrictions.eq("username", name)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<UserAcc> res = c.list();

		if (res.size() != 1) {
			return null;
		}

		return res.get(0);
	}
	
	public void saveUser(UserAcc userAcc){
		getSession().save(userAcc);
		getSession().flush();
	}

}
