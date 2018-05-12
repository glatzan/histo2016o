package org.histo.service.dao;

import java.util.List;

import org.histo.model.patient.Patient;

public interface GenericDao<E, K> {

	void add(E entity);

	void update(E entity);

	void remove(E entity);

	E find(K key);

	List<E> list();

	public void save(Object entity);

	public void save(Object entity, String resourcesKey, Object... resourcesKeyInsert);

	public void save(Object entity, Patient patient, String resourcesKey, Object... resourcesKeyInsert);

	public void saveCollection(List<Object> objects);

	public void saveCollection(List<Object> objects, String resourcesKey, Object... resourcesKeyInsert);

	public void saveCollection(List<Object> objects, Patient patient, String resourcesKey,
			Object... resourcesKeyInsert);

	public void delete(Object entity);

	public void delete(Object entity, String resourcesKey, Object... resourcesKeyInsert);

	public void delete(Object entity, Patient patient, String resourcesKey, Object... resourcesKeyInsert);

	public void reattach(E entity);
}