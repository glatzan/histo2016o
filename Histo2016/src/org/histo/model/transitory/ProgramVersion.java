package org.histo.model.transitory;

import java.lang.reflect.Type;

import org.histo.model.interfaces.GsonAble;
import org.histo.util.HistoUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class ProgramVersion implements GsonAble {

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
	 * Factory loads a list of ProgramVersion Objects from a json file
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static final ProgramVersion[] factroy(String jsonFile) {

		Type type = new TypeToken<ProgramVersion[]>() {
		}.getType();

		Gson gson = new Gson();
		ProgramVersion[] result = gson.fromJson(HistoUtil.loadTextFile(jsonFile), type);
		return result;
	}

}
