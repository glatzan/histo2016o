package org.histo.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.histo.model.Patient;
import org.histo.model.Person;
import org.primefaces.json.JSONObject;

//    {
//	   "vorname":"Andreas",
//	   "mode":null,
//	   "status":null,
//	   "piz":"20366346",
//	   "sonderinfo":null,
//	   "iknr":null,
//	   "kvnr":null,
//	   "titel":null,
//	   "versichertenstatus":" ",
//	   "tel":null,
//	   "anschrift":"Tannenweg 34",
//	   "wop":null,
//	   "plz":"79183",
//	   "name":"Glatz",
//	   "geburtsdatum":"1988-10-04",
//	   "gueltig_bis":null,
//	   "krankenkasse":null,
//	   "versnr":null,
//	   "land":"D",
//	   "weiblich":"",
//	   "ort":"Waldkirch",
//	   "status2":null
//	}

//{
//
//+      "vorname":"Ioana Maria",
//->       "mode":"K",
//->       "status":"1000",
//ok      "piz":"29017379",
//?      "sonderinfo":"",
//->      "iknr":"61125",
//->      "kvnr":"108018121",
//ok      "titel":null,
//->      "versichertenstatus":"1 ",
//ok     "tel":"0176 62346167",
//ok      "anschrift":"Habsburgerstr. 25",
//?      "wop":"",
//ok      "plz":"79104",
//ok      "name":"Cazana",
//ok      "geburtsdatum":"1989-10-09",
//?     "gueltig_bis":null,
//ok      "krankenkasse":"AOK Baden-Württemberg",
//?     "versnr":"U367703198",
//ok      "land":"D",
//ok      "weiblich":"1",
//ok      "ort":"Freiburg",
// "status2":"1"
//
//}

public class PersonAdministration {
    private final String USER_AGENT = "Mozilla/5.0";

    public String getRequest(String url) {

	StringBuffer response = new StringBuffer();

	try {
	    URL obj = new URL(url);
	    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

	    // optional default is GET
	    con.setRequestMethod("GET");

	    // add request header
	    con.setRequestProperty("User-Agent", USER_AGENT);

	    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	    String inputLine;
	    while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
	    }
	    in.close();

	} catch (Exception e) {
	    e.printStackTrace();
	    return "";
	}

	return response.toString();
    }

    /**
     * Updates a patient objekt with a given json array from the clinic backend
     *
     * { "vorname":"Test", "mode":"W", "status":null, "piz":"25201957", "sonderinfo":"", "iknr":"00190", "kvnr":null, "titel":"Prof. Dr. med.", "versichertenstatus":" ", "tel":"12-4085", "anschrift":
     * "Gillenweg 4", "wop":null, "plz":"79110", "name":"Test", "geburtsdatum":"1972-08-22", "gueltig_bis":null, "krankenkasse":"Wissenschaftliche Unters.", "versnr":null, "land":"D", "weiblich":"",
     * "ort":"Freiburg", "status2":null }
     * 
     * @param patient
     * @param json
     * @return
     */
    public Patient updatePatient(Patient patient, String json) {

	if (patient.getPerson() == null)
	    patient.setPerson(new Person());

	Person person = patient.getPerson();

	JSONObject obj = new JSONObject(json);

	// Person data
	person.setTitle(obj.getString("titel"));
	person.setName(obj.getString("name"));
	person.setSurname(obj.getString("vorname"));
	// parsing date
	DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
	Date date;
	try {
	    date = format.parse(obj.getString("geburtsdatum"));
	} catch (ParseException e) {
	    date = new Date();
	}
	person.setBirthday(date);

	person.setTown(obj.getString("ort"));
	person.setLand(obj.getString("land"));
	person.setPostcode(obj.getString("plz"));
	person.setStreet(obj.getString("anschrift"));
	person.setPhoneNumber(obj.getString("tel"));
	// 1 equals female, empty equals male
	person.setGender(obj.getString("weiblich").equals("1") ? Person.GENDER_FEMALE : Person.GENDER_MALE);

	// TODO
	person.setEmail("");
	// todo
	person.setHouseNumber("");

	// patient data
	patient.setInsurance(obj.getString("krankenkasse"));
	patient.setAddDate(new Date(System.currentTimeMillis()));

	return patient;
    }
}
