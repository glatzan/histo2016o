package org.histo.model.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public interface GsonAble {

	/**
	 * Returns the implementing object as gson string. All variables to parse
	 * have to be marked with exposed
	 * 
	 * @return
	 */
	public default String asGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}
}
