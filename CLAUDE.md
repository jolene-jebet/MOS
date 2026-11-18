@AGENTS.md

## Person B
### initial setup using osm

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

### troubleshooting OSM

- `The app is running but i want to fix some bugs. The open street map section of the app has a bug where it is saying that the app is not following the tile usage policy of OpenStreetMap's volunteer-run servers: http://osm.wiki/Blocked`
- `I am still getting the access blocked, app is not following the tile usage policy of OpenStreetMap's volunteer-run servers: http://osm.wiki/Blocked. Despite the fix implemented. It has not worked out fully. Use the instructions from https://operations.osmfoundation.org/policies/tiles/ to see what you can do to make sure that the screen is actually showcasing a map.`
- `Make the map to become the main thing on the explore screen. That is, 1. make the origin and destination tiles to become a search button at the top of the page just below the nairobi safety navigator part. 2. The search can be a simple bar and then drop down to showcase a more complicated search similar to google maps functionality. 3. Reduce the initial screen real estate occupied by the scrollable widget at the bottom of the app.`
- `next rather than selecting the start and the destination sections from a select list of a few items, allow searching of places and get whether such places exist from osm itself and do the same for destinations.`
- `Good fix for the 403 error, however sometimes during the act of zooming or when i come back to the view after going to a different view it still issues the 403 error.`
- `1. Make it such that the on tapping the search widget search screen takes up the whole screen just like on the actual google maps app. 2. Also, make it so that when you go back to the main screen, the new routes are shown. 3. Finally, make is so that incase the routes are not able to be obtained or there is an issue in finding the route, the app displays a dismissable warning alert saying that a route could not be found.`
