@AGENTS.md

## Person B
### initial setup using osm *(Gemini Agent)*

- `@Guide I am person B in this document. Let's begin with the first task under the guide.`
- `@Guide Okay you are person B, let us start with task 1. adding the osmdroid dependency and removing the maps compose dependency`
- `@Guide Now to do the next part of task 1, • Before the map loads, set Configuration.getInstance().userAgentValue = context.packageName. OSM's tile servers block apps that don't identify themselves.`
- `Show the map in Compose: AndroidView(factory = { MapView(it).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); controller.setZoom(14.0); controller.setCenter(GeoPoint(-1.2864, 36.8172)) } }), centred on Nairobi CBD.`
- `Forward the screen lifecycle to the map (mapView.onResume() / onPause()) with a DisposableEffect + LifecycleEventObserver, and call onDetach() when the screen closes.`
- `Show "© OpenStreetMap contributors" on the map. The OSM licence requires it.`
- `Ask for ACCESS_FINE_LOCATION with rememberLauncherForActivityResult(RequestMultiplePermissions()).`
- `Add MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) and call enableMyLocation(). If permission is denied, fall back to the CBD.`
- `A location card with two dropdowns (ExposedDropdownMenuBox) filled from places in fake_data.json (~10 Nairobi places).`
- `FakeRouteRepository returns routes from fake_data.json (C provides the loader). Build this first.`
- `OsrmRouteRepository: a Retrofit interface on https://router.project-osrm.org/ calling route/v1/driving/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson&alternatives=true.`
- `Watch the order: OSRM takes longitude first, then latitude. The response gives routes[].geometry.coordinates as [lng, lat] pairs, duration in seconds and distance in metres. GeoJSON means no polyline decoding.`
- `Boda, Matatu and Private use the driving profile. Walking (optional): https://routing.openstreetmap.de/routed-foot/route/v1/foot/....`
- `For each route, make an osmdroid Polyline, setPoints(...) with GeoPoint(lat, lng), and set outlinePaint.color to the risk colour (Safe / Caution / Risk .toArgb()).`
- `Add the safest route last and thicker, so it sits on top. Call mapView.invalidate() after changing overlays.`
- `Material 3 BottomSheetScaffold with A's RouteCards, safest first. Tapping a card calls navController.navigate("routedetail/${route.id}")`

### troubleshooting OSM *(Gemini Agent)*

- `The app is running but i want to fix some bugs. The open street map section of the app has a bug where it is saying that the app is not following the tile usage policy of OpenStreetMap's volunteer-run servers: http://osm.wiki/Blocked`
- `I am still getting the access blocked, app is not following the tile usage policy of OpenStreetMap's volunteer-run servers: http://osm.wiki/Blocked. Despite the fix implemented. It has not worked out fully. Use the instructions from https://operations.osmfoundation.org/policies/tiles/ to see what you can do to make sure that the screen is actually showcasing a map.`
- `Make the map to become the main thing on the explore screen. That is, 1. make the origin and destination tiles to become a search button at the top of the page just below the nairobi safety navigator part. 2. The search can be a simple bar and then drop down to showcase a more complicated search similar to google maps functionality. 3. Reduce the initial screen real estate occupied by the scrollable widget at the bottom of the app.`
- `next rather than selecting the start and the destination sections from a select list of a few items, allow searching of places and get whether such places exist from osm itself and do the same for destinations.`
- `Good fix for the 403 error, however sometimes during the act of zooming or when i come back to the view after going to a different view it still issues the 403 error.`
- `1. Make it such that the on tapping the search widget search screen takes up the whole screen just like on the actual google maps app. 2. Also, make it so that when you go back to the main screen, the new routes are shown. 3. Finally, make is so that incase the routes are not able to be obtained or there is an issue in finding the route, the app displays a dismissable warning alert saying that a route could not be found.`

### Migration to Google Maps SDK & Google Places/Directions APIs *(Gemini Agent)*

- `Switch MOS app from OpenStreetMap (osmdroid) to Google Maps SDK (`maps-compose`), Google Directions API, Google Geocoding API, and Google Places API.`
- `Read MAPS_API_KEY, ROUTE_API_KEY, GEOCODING_API_KEY, and PLACES_API_KEY from local.properties with trim support and expose as BuildConfig fields and manifestPlaceholders["MAPS_API_KEY"].`
- `Created GoogleRouteRepository implementing RouteRepository with Retrofit interface for directions/json, geocode/json, and place/textsearch/json.`
- `Migrated ExploreScreen to GoogleMap with Polyline overlays (safest route rendered thickest and on top) and remember-scoped MarkerState.`
- `Removed default fake/criss-crossing lines on initial startup and empty states.`
- `Implemented full-screen Google Maps style search overlay with live suggestion cards and animated camera positioning (LatLng) on selection.`
- `Added CancellationException re-throwing in ExploreViewModel coroutines for clean job cancellations and debouncing.`
- `Added multi-layered place search (Google Places API + Geocoding API + Local Nairobi preset fallback) and guaranteed route fallbacks.`

### Switching from OSM to Google Maps (search + routes) *(Claude Web App - Sent an archive of the project)*

- `@Guide I am person B. The map has a perfect Google Maps implementation, but I thought I needed all the different API keys to search for places and get routes. I even commented out some keys to fall back to the default Maps SDK for Android, but they still did not work. Console: Geocoding API 33 requests, 100% errors. Maps SDK for Android 14 requests, 0% errors.`
- `Make the system allow searching for places and show them in a list, just like Google Maps.`
- `Show the routes on the Google Map that is already there.`
- `Remove the basic route overlay that appears when the route set is not proper.`
- `Do not create the entire project again. Only give the code for the files that need to change.`
- `Do not change how the architecture works unless it is extremely necessary.`
- `Do not change anything outside this request or outside Person B's scope in the Guide. If in doubt, use the Guide but substitute the OSM requirements with Google Maps ones.`

#### Result

- Cause: legacy Directions and Places APIs cannot be enabled on new Google projects. Geocoding calls were probably rejected because an Android-restricted key needs the app package and SHA-1 on web calls.
- One key only: `MAPS_API_KEY`. The `ROUTE`, `GEOCODING` and `PLACES` keys are no longer read.
- `data/GoogleRouteRepository.kt`
    - Places API (New) Text Search for place search.
    - Routes API (`computeRoutes`) for routes. Walk uses `WALK`; Boda, Matatu and Private use `DRIVE`.
    - Sends `X-Android-Package` and `X-Android-Cert` on every call.
    - Removed the fake straight-line fallback routes. A failed or empty result now shows the dismissable "route not found" alert.
    - Caches the last routes so Route Detail can find them by id.
- `screens/explore/ExploreScreen.kt`
    - Map zooms to fit the routes.
    - Search list shows a name plus an address.
    - Passes `context` to the repository.
- `screens/explore/ExploreViewModel.kt`
    - Search waits 400 ms after typing stops.
    - A new route request cancels the old one.
- Google Cloud setup: enable **Places API (New)** and **Routes API**, and allow both on the key.
- Debugging: filter Logcat by `MOS-Google` to see Google's exact error.

# Connecting the mobile app to the cloud AI model
1. **Overview and full working copy.** Asked for an overview of what Person C was meant to do from the Guide, especially connecting the final model to the app, and for a full working copy of that output to send to another agent.
2. **Taking over Person C's work.** Said Person C could not do their part, so I was helping. Attached the model notebook, the earlier agent output and the Guide PDF (a possible working solution from scratch to working). Noted we had moved from OSM to Google Maps, and asked that anything suggested use Google Maps.
3. **Single comprehensive guide.** Asked for one step-by-step guide that does not send me to multiple files.
4. **Grid rounding question.** Gave Nairobi's approximate coordinates (about -1.2833 to -1.2921 lat, 36.8167 to 36.8219 lng) and asked what the likely issue is, given the model rounds to 1 decimal place.
5. **Results after the change.** Attached the updated notebook, now rounding to 2 decimal places, and asked for the results explained and what to do next.
6. **Server abort.** Shared a screenshot of `uvicorn main:app` printing "Aborted!" even though the scikit-learn and joblib versions matched.
7. **Prompt-history summary.** Asked for a summary of what we had done, to paste into my prompt-list file, then asked for it as markdown. Then clarified that I wanted a summary of my prompts, not of the work.
9. **App shows fake data.** Reported that the app still showed the old fake data after switching it to the cloud model.
10. **First Logcat.** Attached a Logcat file containing errors.
11. **Second Logcat.** Attached a larger Logcat export ("that is what I got").
12. **Third Logcat.** Attached another Logcat export, after adding the extra logging suggested.
13. **Render logs.** Shared a screenshot of the server logs showing only health checks, and said the remote risk request never happened.
14. **Source files and test result.** Attached the app's source files (risk, route and Explore code), a Render log screenshot showing `POST /predict 200 OK`, and the Logcat line `TEST predict OK`.
15. **This request.** Asked for a summary of all my prompts for my AI-use declaration.

# Adding stops between routes
- create a new feature that allows me to enter a stop in between my start and final destinations
- Make it such that the different routes have different shades of intensity depending on the index, so that the first one is like a strong brightness, second one a bit duller and the third option even more dull and so on and so forth. This is to make it such that teh different routes have different shades rather than all of them just coming out as green and you can't tell the difference between anything.