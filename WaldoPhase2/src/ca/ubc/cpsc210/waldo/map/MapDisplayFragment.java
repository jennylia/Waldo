package ca.ubc.cpsc210.waldo.map;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import ca.ubc.cpsc210.waldo.R;
import ca.ubc.cpsc210.waldo.model.BusRoute;
import ca.ubc.cpsc210.waldo.model.BusStop;
import ca.ubc.cpsc210.waldo.model.Trip;
import ca.ubc.cpsc210.waldo.model.Waldo;
import ca.ubc.cpsc210.waldo.translink.TranslinkService;
import ca.ubc.cpsc210.waldo.util.LatLon;
import ca.ubc.cpsc210.waldo.util.Segment;
import ca.ubc.cpsc210.waldo.waldowebservice.WaldoService;

/**
 * Fragment holding the map in the UI.
 * 
 * @author CPSC 210 Instructor
 */
public class MapDisplayFragment extends Fragment implements LocationListener{
// this gave me 4 new method
	/**
	 * Log tag for LogCat messages
	 */
	private final static String LOG_TAG = "MapDisplayFragment";

	/**
	 * Location of some points in lat/lon for testing and for centering the map
	 */
	private final static GeoPoint ICICS = new GeoPoint(49.261182, -123.2488201);
	private final static GeoPoint CENTERMAP = ICICS;

	/**
	 * Preference manager to access user preferences
	 */
	private SharedPreferences sharedPreferences;

	/**
	 * View that shows the map
	 */
	private MapView mapView;

	/**
	 * Map controller for zooming in/out, centering
	 */
	private MapController mapController;

	// **************** Overlay fields **********************

	/**
	 * Overlay for the device user's current location.
	 */
	private SimpleLocationOverlay userLocationOverlay;

	/**
	 * Overlay for bus stop to board at
	 */
	private ItemizedIconOverlay<OverlayItem> busStopToBoardOverlay;

	/**
	 * Overlay for bus stop to disembark
	 */
	private ItemizedIconOverlay<OverlayItem> busStopToDisembarkOverlay;

	/**
	 * Overlay for Waldo
	 */
	private ItemizedIconOverlay<OverlayItem> waldosOverlay;

	/**
	 * Overlay for displaying bus routes
	 */
	private List<PathOverlay> routeOverlays;

	/**
	 * Selected bus stop on map
	 */
	private OverlayItem selectedStopOnMap;

	/**
	 * Bus selected by user
	 */
	private OverlayItem selectedBus;

	// ******************* Application-specific *****************

	/**
	 * Wraps Translink web service
	 */
	private TranslinkService translinkService;

	/**
	 * Wraps Waldo web service
	 */
	private WaldoService waldoService;

	/**
	 * Waldo selected by user
	 */
	private Waldo selectedWaldo;

	/*
	 * The name the user goes by
	 */
	private String userName;
	// SOMETHING i added
	// maybe need location manager
	private LocationManager lm;

	// some string reference towers we can delete
	String towers;

	// ***************** Android hooks *********************

	/**
	 * Help initialize the state of the fragment
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);

		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());

		

		waldoService = new WaldoService();
		translinkService = new TranslinkService();
		routeOverlays = new ArrayList<PathOverlay>();
		initializeWaldo();
		// on the tutorial, it was going to draw stuff
		/**
		 * int lat = 0;
		 * int long = 0;
		 * geo point
		 * overlay item (with our location)
		 * custome this that what ever
		 * 
		 */
		//hello
		// I'm going to try and add the location manager
		// should I also initiate ICICS?
		lm = (LocationManager) getActivity().getSystemService(
				Context.LOCATION_SERVICE);
		if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			lm.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 1000, 1, this);
		} else {
			lm.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
			
			
		}
		Criteria crit = new Criteria();
		// we can get some sort of criteria back
		towers = lm.getBestProvider(crit, false);
		// the best last known
		
		Location location =lm.getLastKnownLocation(towers);
		System.out.println(location);
		// I might just delete these
		
		if(location != null){
			this.updateLocation(location);
			Double lat = location.getLatitude();
			Double lon = location.getLongitude();
			//other fancy stuff
			// and an else too
		}
		/**
		 * the tutorial also got if last location is known blah
		 */
		
		
	}
	// criteria
	/**
	 * Initialize the Waldo web service
	 */
	private void initializeWaldo() {
		String s = null;
		new InitWaldo().execute(s);
	}

	/**
	 * Set up map view with overlays for buses, selected bus stop, bus route and
	 * current location.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (mapView == null) {
			mapView = new MapView(getActivity(), null);

			mapView.setTileSource(TileSourceFactory.MAPNIK);
			mapView.setClickable(true);
			mapView.setBuiltInZoomControls(true);

			mapController = mapView.getController();
			mapController.setZoom(mapView.getMaxZoomLevel() - 4);
			mapController.setCenter(CENTERMAP);

			userLocationOverlay = createLocationOverlay();
			busStopToBoardOverlay = createBusStopToBoardOverlay();
			busStopToDisembarkOverlay = createBusStopToDisembarkOverlay();
			waldosOverlay = createWaldosOverlay();

			// Order matters: overlays added later are displayed on top of
			// overlays added earlier.
			mapView.getOverlays().add(waldosOverlay);
			mapView.getOverlays().add(busStopToBoardOverlay);
			mapView.getOverlays().add(busStopToDisembarkOverlay);
			mapView.getOverlays().add(userLocationOverlay);
			
			//userLocationOverlay.setLocation(ICICS);
		}

		return mapView;
	}

	/**
	 * Helper to reset overlays
	 */
	private void resetOverlays() {
		OverlayManager om = mapView.getOverlayManager();
		om.clear();
		om.addAll(routeOverlays);
		om.add(busStopToBoardOverlay);
		om.add(busStopToDisembarkOverlay);
		om.add(userLocationOverlay);
		om.add(waldosOverlay);
	}

	/**
	 * Helper to clear overlays
	 */
	private void clearOverlays() {
		waldosOverlay.removeAllItems();
		clearAllOverlaysButWaldo();
		OverlayManager om = mapView.getOverlayManager();
		om.add(waldosOverlay);
	}

	/**
	 * Helper to clear overlays, but leave Waldo overlay untouched
	 */
	private void clearAllOverlaysButWaldo() {
		if (routeOverlays != null) {
			routeOverlays.clear();
			busStopToBoardOverlay.removeAllItems();
			busStopToDisembarkOverlay.removeAllItems();

			OverlayManager om = mapView.getOverlayManager();
			om.clear();
			om.addAll(routeOverlays);
			om.add(busStopToBoardOverlay);
			om.add(busStopToDisembarkOverlay);
			om.add(userLocationOverlay);
		}
	}

	/**
	 * When view is destroyed, remove map view from its parent so that it can be
	 * added again when view is re-created.
	 */
	@Override
	public void onDestroyView() {
		((ViewGroup) mapView.getParent()).removeView(mapView);
		super.onDestroyView();
	}

	/**
	 * Shut down the various services
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Update the overlay with user's current location. Request location
	 * updates.
	 */
	@Override
	public void onResume() {

		// CPSC 210 students, you'll need to handle parts of location updates
		// here...

		initializeWaldo();
		//LocationManager locationManager = null;
		// It's going to refresh listening
		// request location update
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, this);
		super.onResume();
	}

	/**
	 * Cancel location updates.
	 */
	@Override
	public void onPause() {
		// CPSC 210 students, you'll need to do some work with location updates
		// here...
		//removeUpdates(listener);
		super.onPause();
		lm.removeUpdates(this);
	}

	/**
	 * Update the marker for the user's location and repaint.
	 */
	public void updateLocation(Location location) {
		// CPSC 210 Students: Implement this method. mapView.invalidate is
		// needed to redraw
		// the map and should come at the end of the method.
		//clearAllOverlaysButWaldo();
		
		//waldosOverlay.removeAllItems();
		
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		GeoPoint geo = new GeoPoint(lat,lon);
		
		System.out.println(lat + " " + lon);
		userLocationOverlay.setLocation(geo);
		//userLocationOverlay.getMyLocation();
	
		mapView.invalidate();
	}

	/**
	 * Save map's zoom level and centre.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mapView != null) {
			outState.putInt("zoomLevel", mapView.getZoomLevel());
			IGeoPoint cntr = mapView.getMapCenter();
			outState.putInt("latE6", cntr.getLatitudeE6());
			outState.putInt("lonE6", cntr.getLongitudeE6());
		}
	}

	/**
	 * Retrieve Waldos from the Waldo web service
	 */
	public void findWaldos() {
		clearOverlays();
		// Find out from the settings how many waldos to retrieve, default is 1
		String numberOfWaldosAsString = sharedPreferences.getString(
				"numberOfWaldos", "1");
		int numberOfWaldos = Integer.valueOf(numberOfWaldosAsString);
		new GetWaldoLocations().execute(numberOfWaldos);
		mapView.invalidate();
	}

	/**
	 * Clear waldos from view
	 */
	public void clearWaldos() {
		clearOverlays();
		mapView.invalidate();

	}

	// ******************** Overlay Creation ********************

	/**
	 * Create the overlay for bus stop to board at marker.
	 */
	private ItemizedIconOverlay<OverlayItem> createBusStopToBoardOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {

			/**
			 * Display bus stop description in dialog box when user taps stop.
			 * 
			 * @param index
			 *            index of item tapped
			 * @param oi
			 *            the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {

				new AlertDialog.Builder(getActivity())
						.setPositiveButton(R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								if (selectedStopOnMap != null) {
									selectedStopOnMap.setMarker(getResources()
											.getDrawable(R.drawable.pin_blue));

									mapView.invalidate();
								}
							}
						}).setTitle(oi.getTitle()).setMessage(oi.getSnippet())
						.show();

				oi.setMarker(getResources().getDrawable(R.drawable.pin_blue));
				selectedStopOnMap = oi;
				mapView.invalidate();
				return true;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};

		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), getResources().getDrawable(
						R.drawable.pin_blue), gestureListener, rp);
	}

	/**
	 * Create the overlay for bus stop to disembark at marker.
	 */
	private ItemizedIconOverlay<OverlayItem> createBusStopToDisembarkOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {

			/**
			 * Display bus stop description in dialog box when user taps stop.
			 * 
			 * @param index
			 *            index of item tapped
			 * @param oi
			 *            the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {

				new AlertDialog.Builder(getActivity())
						.setPositiveButton(R.string.ok, new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								if (selectedStopOnMap != null) {
									selectedStopOnMap.setMarker(getResources()
											.getDrawable(R.drawable.pin_blue));

									mapView.invalidate();
								}
							}
						}).setTitle(oi.getTitle()).setMessage(oi.getSnippet())
						.show();

				oi.setMarker(getResources().getDrawable(R.drawable.pin_blue));
				selectedStopOnMap = oi;
				mapView.invalidate();
				return true;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};

		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), getResources().getDrawable(
						R.drawable.pin_blue), gestureListener, rp);
	}

	/**
	 * Create the overlay for Waldo markers.
	 */
	private ItemizedIconOverlay<OverlayItem> createWaldosOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());
		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {

			/**
			 * Display Waldo point description in dialog box when user taps
			 * icon.
			 * 
			 * @param index
			 *            index of item tapped
			 * @param oi
			 *            the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {

				selectedWaldo = waldoService.getWaldos().get(index);
				Date lastSeen = selectedWaldo.getLastUpdated();
				SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
						"MMM dd, hh:mmaa", Locale.CANADA);

				new AlertDialog.Builder(getActivity())
						.setPositiveButton(R.string.get_route,
								new OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {

										// CPSC 210 STUDENTS. You must set
										// currCoord to
										// the user's current location.
										LatLon currCoord = null;

										// CPSC 210 Students: Set currCoord...

										LatLon destCoord = selectedWaldo
												.getLastLocation();

										new GetRouteTask().execute(currCoord,
												destCoord);

									}
								})
						.setNegativeButton(R.string.ok, null)
						.setTitle(selectedWaldo.getName())
						.setMessage(
								"Last seen  " + dateTimeFormat.format(lastSeen))
						.show();

				mapView.invalidate();
				return true;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};

		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), getResources().getDrawable(
						R.drawable.map_pin_thumb_blue), gestureListener, rp);
	}

	/**
	 * Create overlay for a bus route.
	 */
	private PathOverlay createPathOverlay() {
		PathOverlay po = new PathOverlay(Color.parseColor("#cf0c7f"),
				getActivity());
		Paint pathPaint = new Paint();
		pathPaint.setColor(Color.parseColor("#cf0c7f"));
		pathPaint.setStrokeWidth(4.0f);
		pathPaint.setStyle(Style.STROKE);
		po.setPaint(pathPaint);
		return po;
	}

	/**
	 * Create the overlay for the user's current location.
	 */
	private SimpleLocationOverlay createLocationOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		return new SimpleLocationOverlay(getActivity(), rp) {
			@Override
			public boolean onLongPress(MotionEvent e, MapView mapView) {
				new GetMessagesFromWaldo().execute();
				return true;
			}

		};
	}

	/**
	 * Plot endpoints
	 */
	private void plotEndPoints(Trip trip) {
		GeoPoint pointStart = new GeoPoint(trip.getStart().getLatLon()
				.getLatitude(), trip.getStart().getLatLon().getLongitude());

		OverlayItem overlayItemStart = new OverlayItem(Integer.valueOf(
				trip.getStart().getNumber()).toString(), trip.getStart()
				.getDescriptionToDisplay(), pointStart);
		GeoPoint pointEnd = new GeoPoint(trip.getEnd().getLatLon()
				.getLatitude(), trip.getEnd().getLatLon().getLongitude());
		OverlayItem overlayItemEnd = new OverlayItem(Integer.valueOf(
				trip.getEnd().getNumber()).toString(), trip.getEnd()
				.getDescriptionToDisplay(), pointEnd);
		busStopToBoardOverlay.removeAllItems();
		busStopToDisembarkOverlay.removeAllItems();

		busStopToBoardOverlay.addItem(overlayItemStart);
		busStopToDisembarkOverlay.addItem(overlayItemEnd);
	}

	/**
	 * Plot bus route onto route overlays
	 * 
	 * @param rte
	 *            : the bus route
	 * @param start
	 *            : location where the trip starts
	 * @param end
	 *            : location where the trip ends
	 */
	private void plotRoute(Trip trip) {

		// Put up the end points
		plotEndPoints(trip);

		// CPSC 210 STUDENTS: Complete the implementation of this method

		// This should be the last method call in this method to redraw the map
		mapView.invalidate();
	}

	/**
	 * Plot a Waldo point on the specified overlay.
	 */
	private void plotWaldos(List<Waldo> waldos) {

		// CPSC 210 STUDENTS: Complete the implementation of this method
		/**
		 * OverlayItem overlayItemStart = new OverlayItem("my first waldo",
		 * waldo.name, null); From each point on the wal
		 */
		// String wa = waldoService.initSession("jenny");
		List<Waldo> wd = waldoService.getWaldos();

		waldosOverlay.removeAllItems();
		for (Waldo w : wd) {
			// make the geo point
			GeoPoint wloc = new GeoPoint(w.getLastLocation().getLatitude(), w
					.getLastLocation().getLongitude());
			// make the overlay item
			OverlayItem o = new OverlayItem(w.getName(), w.getLastLocation()
					.toString(), wloc);

			waldosOverlay.addItem(o);

		}

		/*
		 * //the test for waldos overlay GeoPoint pointEnd = ICICS;
		 * 
		 * OverlayItem overlayItemEnd = new OverlayItem("bye", "hello",
		 * pointEnd); waldosOverlay.addItem(overlayItemEnd);
		 * 
		 * 
		 * 
		 * // This should be the last method call in this method to redraw the
		 * map mapView.invalidate();
		 */

		// experiments start here
		// waldosOverlay.removeAllItems();

		/*
		 * GeoPoint pointStart = new GeoPoint(49.248523, -123.108800);
		 * 
		 * OverlayItem overlayItemStart = new OverlayItem("start 1", "start 2",
		 * pointStart);
		 * 
		 * GeoPoint pointEnd = ICICS;
		 * 
		 * OverlayItem overlayItemEnd = new OverlayItem("bye 1 ", "bye 2",
		 * pointEnd);
		 * 
		 * 
		 * //busStopToDisembarkOverlay.removeAllItems();
		 * 
		 * waldosOverlay.addItem(overlayItemStart);
		 * waldosOverlay.addItem(overlayItemEnd);
		 */

	}

	/**
	 * Helper to create simple alert dialog to display message
	 * 
	 * @param msg
	 *            message to display in alert dialog
	 * @return the alert dialog
	 */
	private AlertDialog createSimpleDialog(String msg) {
		AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
		dialogBldr.setMessage(msg);
		dialogBldr.setNeutralButton(R.string.ok, null);
		return dialogBldr.create();
	}

	/**
	 * Asynchronous task to get a route between two endpoints. Displays progress
	 * dialog while running in background.
	 */
	
	
	private class GetRouteTask extends AsyncTask<LatLon, Void, Trip> {
		private ProgressDialog dialog = new ProgressDialog(getActivity());
		private LatLon startPoint;
		private LatLon endPoint;

		@Override
		protected void onPreExecute() {
			translinkService.clearModel();
			dialog.setMessage("Retrieving route...");
			dialog.show();
		}

		@Override
		protected Trip doInBackground(LatLon... routeEndPoints) {

			// THe start and end point for the route
			startPoint = routeEndPoints[0];
			endPoint = routeEndPoints[1];
			String direction = null;
			BusRoute route = null;
			boolean walkingDistance = false;
			
			String distance = sharedPreferences.getString("stopDistance", "500");
			int distanceInt = Integer.parseInt(distance);
			
			// Find BusStops that are near startPoint and Waldo
			Set<BusStop> stopsNearUser = new HashSet<BusStop>();
			Set<BusStop> stopsNearWaldo = new HashSet<BusStop>();
			stopsNearUser = translinkService.getBusStopsAround(startPoint, distanceInt);
			stopsNearWaldo = translinkService.getBusStopsAround(endPoint, distanceInt);
			
			// Find BusRoutes that are in startPoint and Waldo
			
			// Function that sees if the user and waldo share a nearby busstop,
			// thus indicating both are within walking distance.
			for(BusStop a : stopsNearUser){
				for(BusStop b : stopsNearWaldo){
					if(a.equals(b)){
						walkingDistance = true;
					}	
				}
			}
			
			// Function that figures out which direction the user should travel to reach waldo
			direction = findDirection(startPoint, endPoint);
			
			// We need a function to find all common routes between the two bus stops
			// Do we need to find all possible routes or just one?
			// Assuming we're taking the closest stop to waldo, and finding some
			// reasonably close stop to Waldo...
			// I think we can do this by...
			// 1. iterating through stopsNearUser for the closest stop to User
			// 		Use distanceBetweenTwoLatLon(LatLon point1, LatLon point2)
			// 2. Once we have the closest stop, we use getRoutes to find what
			//		routes that go through the busStop.
			// 3. We look at every route and see if any of them contain a busStop
			//		that matches any of the busStops in stopsNearWaldo
			//		(*question, what if there are none?*)
			// 		if there are none... we go back to step 1 somehow... and 
			//		repeat the process except with the next closest stop to User.
			//		If none exist, then...
			// 4. If there is one, return that route.
			// 5. Use translinkService.parseKMZ(route) (*not sure how this works*)
			// 6. Return the LatLon of the two busStops (needed to create Trip obj)
			// 7. Pretty much done. Just return that Trip object we made.
			
			// decides whether to find route that has busStop closest to user (me) or closest to Waldo
			// "closest_stop_dest"
			
			// Method to find all bus routes in common between StopsNearUser and StopsNearWaldo
			
			/*
				Set<BusRoute> RoutesOfA = a.getRoutes();
				for(BusStop b : stopsNearWaldo){
					Set<BusRoute> RoutesOfA = a.getRoutes();
					Set<BusRoute> RoutesOfB = b.getRoutes();
					for(BusRoute c : RoutesOfA)
					
				}
			}
			*/
			
			Set<BusRoute> routesNearUser = null; 
			Set<BusRoute> routesNearWaldo = null; 
			Set<BusRoute> routesCombined = null;

			for(BusStop a : stopsNearUser){
				routesNearUser.addAll(a.getRoutes());
			}
			for(BusStop b : stopsNearWaldo){
				routesNearWaldo.addAll(b.getRoutes());
			}
			
			routesCombined = routesNearUser;
			routesCombined.retainAll(routesNearWaldo); // This should be the routes shared by both.
			
			// Now that we know the shared routes, we must find closest stop to
			// either User or Waldo.
			Set<BusStop> sharedStopsNearUser = new HashSet<BusStop>();
			Set<BusStop> sharedStopsNearWaldo = new HashSet<BusStop>();
			
				// we create a set of busStops close to user, except this time the 
				// busStops share routes with the busStops near Waldo
				// Code below iterates through stopsNearUser to find which of the stops
				// are the ones containing shared routes, and places those stops in
				// sharedStopsNearUser
			for(BusStop a : stopsNearUser){
				for(BusRoute a1 : a.getRoutes()){
					if(routesCombined.contains(a1)){
						if(sharedStopsNearUser.contains(a) == false)
						sharedStopsNearUser.add(a);
					}
				}
			}
			for(BusStop b : stopsNearWaldo){
				for(BusRoute b1 : b.getRoutes()){
					if(routesCombined.contains(b1)){
						if(sharedStopsNearWaldo.contains(b) == false)
						sharedStopsNearWaldo.add(b);
					}
				}
			}
			
			// Now that we have two sets containing busStops with shared routes,
			// we just choose the "best" route based on preferences
			// and we choose the two busStops
			BusRoute bestRoute = null;
			BusStop closestStopUser = findClosestBusStop(sharedStopsNearUser, startPoint);
			BusStop closestStopWaldo = findClosestBusStop(sharedStopsNearWaldo, endPoint);

			String routingType = sharedPreferences.getString("routingOptions", "closest_stop_me");
			if(routingType == "closest_stop_me"){
				for(BusRoute a1 : routesCombined){
					for(BusRoute a : closestStopUser.getRoutes()){
						if(a1.equals(a))
							bestRoute = a;
						// ugly method of finding best route.
						// open to suggestions on better algorithm to find.
					}
				}
			}
			else{
				for(BusRoute b1 : routesCombined){
					for(BusRoute b : closestStopWaldo.getRoutes()){
						if(b1.equals(b))
							bestRoute = b;
					}
				}
			}
			
			// FINALLY MAKING THE TRIP
			Trip trip = new Trip(closestStopUser, closestStopWaldo , direction, bestRoute, walkingDistance);
			// CPSC 210 Students: Complete this method. It must return a trip.
			 
			 
			return trip;
		}

		

		@Override
		protected void onPostExecute(Trip trip) {
			dialog.dismiss();

			if (trip != null && !trip.inWalkingDistance()) {
				// Remove previous start/end stops
				busStopToBoardOverlay.removeAllItems();
				busStopToDisembarkOverlay.removeAllItems();

				// Removes all but the selected Waldo
				waldosOverlay.removeAllItems();
				List<Waldo> waldos = new ArrayList<Waldo>();
				waldos.add(selectedWaldo);
				plotWaldos(waldos);

				// Plot the route
				plotRoute(trip);

				// Move map to the starting location
				LatLon startPointLatLon = trip.getStart().getLatLon();
				mapController.setCenter(new GeoPoint(startPointLatLon
						.getLatitude(), startPointLatLon.getLongitude()));
				mapView.invalidate();
			} else if (trip != null && trip.inWalkingDistance()) {
				AlertDialog dialog = createSimpleDialog("You are in walking distance!");
				dialog.show();
			} else {
				AlertDialog dialog = createSimpleDialog("Unable to retrieve bus location info...");
				dialog.show();
			}
		}
		
		// H==E==L==P==E==R====M==E==T==H==O==D==S
		// Helper method I wrote to help find direction of busroute
		private String findDirection(LatLon startPoint, LatLon endPoint){
			String dir = null;
			if(startPoint.getLongitude() < endPoint.getLongitude()){
				// aka if USER starts EAST of WALDO
				if((startPoint.getLongitude() - endPoint.getLongitude()) < 
						(startPoint.getLatitude() - endPoint.getLatitude())){
					dir = "NORTH";
				}
				else if((startPoint.getLongitude() - endPoint.getLongitude()) < 
						(endPoint.getLatitude() - startPoint.getLatitude())){
					dir = "SOUTH";
				}
				else{
					dir = "EAST";
				}
			}
			else{
				if((endPoint.getLongitude() - startPoint.getLongitude()) < 
						(startPoint.getLatitude() - endPoint.getLatitude())){
					dir = "NORTH";
				}
				else if((endPoint.getLongitude() - startPoint.getLongitude()) < 
						(endPoint.getLatitude() - startPoint.getLatitude())){
					dir = "SOUTH";
				}
				else{
					dir = "WEST";
				}
			}
			return dir;
		}
		
		// Method that takes
		private BusStop findClosestBusStop(Set<BusStop> stopsNearUser, LatLon endPoint){
			BusStop closestStopUser = null;
			for(BusStop a : stopsNearUser){
				if(closestStopUser != null){
					double closestDistOld = LatLon.distanceBetweenTwoLatLon(endPoint, closestStopUser.getLatLon());
					double closestDistNew = LatLon.distanceBetweenTwoLatLon(endPoint, a.getLatLon());
					if(closestDistNew < closestDistOld){
						// if busstop a is closer to user than old one, then replace closestStopUser with it.
						closestStopUser = a;
					}
				}
				else{
					closestStopUser = a;
				}
			}
			return closestStopUser;
		}
		
		

	}


	/**
	 * Asynchronous task to initialize or re-initialize access to the Waldo web
	 * service.
	 */
	private class InitWaldo extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... arg0) {

			// Initialize the service passing the name of the Waldo to use. If
			// you have
			// passed an argument to this task, then it will be used as the
			// name, otherwise
			// nameToUse will be null
			String nameToUse = arg0[0];
			userName = waldoService.initSession(nameToUse);

			return null;
		}

	}

	/**
	 * Asynchronous task to get Waldo points from Waldo web service. Displays
	 * progress dialog while running in background.
	 */
	private class GetWaldoLocations extends
			AsyncTask<Integer, Void, List<Waldo>> {
		private ProgressDialog dialog = new ProgressDialog(getActivity());

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Retrieving locations of waldos...");
			dialog.show();
		}

		@Override
		protected List<Waldo> doInBackground(Integer... i) {
			Integer numberOfWaldos = i[0];
			return waldoService.getRandomWaldos(numberOfWaldos);
		}

		@Override
		protected void onPostExecute(List<Waldo> waldos) {
			dialog.dismiss();
			if (waldos != null) {
				plotWaldos(waldos);
			}
		}
	}

	/**
	 * Asynchronous task to get messages from Waldo web service. Displays
	 * progress dialog while running in background.
	 */
	private class GetMessagesFromWaldo extends
			AsyncTask<Void, Void, List<String>> {

		private ProgressDialog dialog = new ProgressDialog(getActivity());

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Retrieving messages...");
			dialog.show();
		}

		@Override
		protected List<String> doInBackground(Void... params) {
			return waldoService.getMessages();
		}

		@Override
		protected void onPostExecute(List<String> messages) {
			// CPSC 210 Students: Complete this method
		}

	}

	@Override
	public void onLocationChanged(Location l) {
		// TODO Auto-generated method stub
		// I hope this draws something
		updateLocation(l);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

}