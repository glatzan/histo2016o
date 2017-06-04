package org.histo.settings;

import java.lang.reflect.Type;

import org.histo.model.interfaces.GsonAble;
import org.histo.util.interfaces.FileHandlerUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class Version implements GsonAble {

	@Expose
	private String version;

	@Expose
	private String[] changes;

	public String getVersion() {
		return version;
	}

	public String[] getChanges() {
		return changes;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setChanges(String[] changes) {
		this.changes = changes;
	}

	/**
	 * Factory loads a list of Version Objects from a json file
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final Version[] factroy(String jsonFile) {

		Type type = new TypeToken<Version[]>() {
		}.getType();

		Gson gson = new Gson();
		Version[] result = gson.fromJson(FileHandlerUtil.getContentOfFile(jsonFile), type);
		return result;
	}

}
