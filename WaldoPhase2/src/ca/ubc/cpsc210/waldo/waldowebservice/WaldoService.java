package ca.ubc.cpsc210.waldo.waldowebservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ca.ubc.cpsc210.waldo.exceptions.IllegalBusStopException;
import ca.ubc.cpsc210.waldo.exceptions.WaldoException;
import ca.ubc.cpsc210.waldo.model.Bus;
import ca.ubc.cpsc210.waldo.model.BusRoute;
import ca.ubc.cpsc210.waldo.model.BusStop;
import ca.ubc.cpsc210.waldo.model.Waldo;
import ca.ubc.cpsc210.waldo.util.LatLon;

public class WaldoService {

	private final static String WALDO_WEB_SERVICE_URL = "http://kramer.nss.cs.ubc.ca:8080/";

	/**
	 * Jenny added some constructor fields
	 * 
	 */
	String key;
	LatLon loc;
	String name;
	List<Waldo> waldos; // what waldos did we return

	private Waldo waldo;

	/**
	 * Constructor
	 */
	public WaldoService() {
		this.key = "";
		loc = null;
		name = "";
		waldos = new ArrayList<Waldo>();
	}

	/**
	 * Initialize a session with the Waldo web service. The session can time out
	 * even while the app is active...
	 * 
	 * @param nameToUse
	 *            The name to go register, can be null if you want Waldo to
	 *            generate a name
	 * @return The name that Waldo gave you
	 */
	public String initSession(String nameToUse) {
		// CPSC 210 Students. You will need to complete this method
		//
		// going to build a url
		InputStream in = null;
		try {
			if (nameToUse != null) {
				String name = nameToUse;
				String s = makeJSONQuery(WALDO_WEB_SERVICE_URL + "/initsession"
						+ "/" + name);
				parseWaldoFromJSON(s);
				System.out.println(s);
				return name;

			}
			// see the above line gives you good stops if stuff actually parsed}
			else {

				String name = nameToUse;
				String s = makeJSONQuery(WALDO_WEB_SERVICE_URL + "/initsession"
						+ "/");
				parseWaldoFromJSON(s);
				return name;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// if its not null, it means stuff actually loaded
				// if it is null, it was never working in the first place
				if (in != null)
					in.close();
			} catch (IOException ioe) {
				throw new WaldoException("Waldo not found");
			}
		}
		// something went wrong, so we give it a new waldo
		return name;

	}

	private void parseWaldoFromJSON(String input) {
		// TODO Auto-generated method stub
		// Initialize

		// JSONArray obj;
		try {
			// Parse each bus stop
			JSONObject waldo1 = (JSONObject) new JSONTokener(input).nextValue();
			if (waldo1 != null) {
				// for (int i = 0; i < obj.length(); i++) {

				// Retrieve key, location, name, date and location of waldo
				// JSONObject waldo1 = obj.getJSONObject(i);
				String key = waldo1.getString("Key");
				String loc = waldo1.getString("Loc");
				String name = waldo1.getString("Name");
				System.out.println("json string: " + waldo1.toString());
				System.out.println("key: " + key);
				System.out.println("loc: " + loc);
				System.out.println("name: " + name);

				this.key = key;
				this.name = name;

				// Construct waldo: name, date and location
				Date date = new Date();
				// loc = new LatLon(loc.lat; loc.long);
				/*
				 * if(name.length()>10 || name.length()<4||name.contains(int
				 * [0,1])){ throw new WaldoException
				 * ("Name is not in the right format"); }
				 */
				// this.waldo = new Waldo(name, date, null); // the last null
				// could be
				// changes
				// add this waldo in the system
				// WaldoService ws = new WaldoService();
				// ws.key = key;
				// ws.waldos.add(waldo);

				// }

			}
		} catch (JSONException e) {
			// Let the developer know but just return whatever is in stopsFound.
			// Probably there was an
			// error in the JSON returned.
			e.printStackTrace();
		}

		// return waldo;
	}

	private String makeJSONQuery(String string) {
		// TODO Auto-generated method stub
		try {
			URL url = new URL(string.toString());
			HttpURLConnection client = (HttpURLConnection) url.openConnection();
			client.setRequestProperty("accept", "application/json");
			InputStream in = client.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String returnString = br.readLine();
			client.disconnect();
			return returnString;
		} catch (Exception e) {
			throw new WaldoException("Unable to make JSON query:"
					+ string.toString());
		}
	}

	/**
	 * Get waldos from the Waldo web service.
	 * 
	 * @param numberToGenerate
	 *            The number of Waldos to try to retrieve
	 * @return Waldo objects based on information returned from the Waldo web
	 *         service
	 */
	public List<Waldo> getRandomWaldos(int numberToGenerate) {
		// CPSC 210 Students: You will need to complete this method
		/**
		 * the idea is: you use a key to return it's waldos then use a for loop
		 * to spit out the first few waldos
		 */
		InputStream in = null;
		try {
			String num = Integer.toString(numberToGenerate);
			String s = makeJSONQuery(WALDO_WEB_SERVICE_URL + "/getwaldos" + "/"
					+ key + "/" + num);
			System.out.println(s);
			waldos = parseAListOfWaldoFromJSON(s);
			System.out.println(waldos);
			// x.get(numberToGenerate);
			// see the above line gives you good stops if stuff actually parsed
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// if its not null, it means stuff actually loaded
				// if it is null, it was never working in the first place
				if (in != null)
					in.close();
			} catch (IOException ioe) {
				throw new WaldoException("Waldos not generated");
			}
		}
		// something went wrong, so we give it a new set of bus stops
		return new ArrayList<Waldo>();
	}

	private List<Waldo> parseAListOfWaldoFromJSON(String input) {
		// TODO Auto-generated method stub
		List<Waldo> waldosFound = new ArrayList<Waldo>(); // initialize
		JSONArray obj;
		try {
			// Parse each bus stop
			obj = (JSONArray) new JSONTokener(input).nextValue();
			if (obj != null) {
				for (int i = 0; i < obj.length(); i++) {

					// Retrieve key, location, name, date and location of waldo
					JSONObject waldo = obj.getJSONObject(i);
					// We have the name
					String name = waldo.getString("Name");
					// Location is an object
					JSONObject loc = (JSONObject) waldo.getJSONObject("Loc");
//Location has 3 fields						
						double lat = loc.getDouble("Lat");
						double lon = loc.getDouble("Long");
						int timestamp = loc.getInt("Tstamp");
						// make the fields into stuff waldo likes
						Date date = new Date(timestamp);
						LatLon ll = new LatLon(lat, lon);

						if (!ll.isIllegal()) {
							Waldo tempWaldo = new Waldo(name, date, ll);
							System.out.println(tempWaldo.getName());
							System.out.println(tempWaldo.getLastUpdated());
							System.out.println(tempWaldo.getLastLocation());
							waldosFound.add(tempWaldo);
							
						}

					}
					return waldosFound;
				}

			
		} catch (JSONException e) {
			// Let the developer know but just return whatever is in stopsFound.
			// Probably there was an
			// error in the JSON returned.
			e.printStackTrace();
		}
		return (List<Waldo>) waldosFound;
	}

	/**
	 * Return the current list of Waldos that have been retrieved
	 * 
	 * @return The current Waldos
	 */
	@SuppressWarnings("unchecked")
	public List<Waldo> getWaldos() {
		// CPSC 210 Students: You will need to complete this method
		return (List<Waldo>) waldos;
	}

	/**
	 * Retrieve messages available for the user from the Waldo web service
	 * 
	 * @return A list of messages
	 */
	public List<String> getMessages() {
		// CPSC 210 Students: You will need to complete this method
		return null;
	}

}
