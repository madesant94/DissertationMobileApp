package com.example.dissertationapp;

import static android.content.ContentValues.TAG;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.HtmlCompat;

import com.example.dissertationapp.R.color;
import com.example.dissertationapp.R.drawable;
import com.example.dissertationapp.R.id;
import com.example.dissertationapp.R.string;
import com.example.dissertationapp.databinding.ActivityMainBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.Place.Field;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import com.google.android.material.slider.Slider;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    Circle circleInit;
    Circle circleEnd;
    private Marker targetMarker;
    private Marker startMarker;

    private float targetDistanceValue;
    private Marker markerShortest;
    private Marker markerCleanest;
    private Boolean pollutionState = false;

    float ZOOM = 13;

    Map<String, List<Polygon>> polygonListsMap = new HashMap<>();

    private Boolean poly1Show = false;
    private Boolean poly2Show = false;

    private Boolean switchFlag = false;

    private Polyline polyline1;
    private Polyline polyline2;
    private ActivityMainBinding binding;

    private HeatmapTileProvider provider;
    private TileOverlay overlay;
    HashMap<String, node> nodesHashMap;
    List<edge> edgesList = new ArrayList<>();
    List<tile> tilesList = new ArrayList<>();
    boolean routeType = false;
    HashMap<String, node> nodesHashMap_b;
    List<edge> edgesList_b = new ArrayList<>();

    String latLngStr = "";
    double targetLat = 51.493278;
    double targetLong = -0.185933;

    double sourceLat = 51.493278;
    double sourceLong = -0.185933;
    String startText = "Your Location";
    String targetText = "";

    public Marker getEndMarker() {
        return targetMarker;
    }


    DirectedWeightedMultigraph<String, edge> graph = new DirectedWeightedMultigraph<>(edge.class);
    DirectedWeightedMultigraph<String, edge> graph_b = new DirectedWeightedMultigraph<>(edge.class);

    private final int REQUEST_LOCATION_PERMISSION = 1;
    private final int REQUEST_LATEST_LOCATION_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //--------------------------------------------------------------------

        //--------------------------------------------------------------------

        //nodesList = CSVLoader.loadNodes(this, "nodes-in-grid.csv");
        nodesHashMap = CSVLoader.loadNodes(this, "nodes-in-grid.csv");
        edgesList = CSVLoader.loadEdges(this, "square-grid-edges.csv");
        tilesList = CSVLoader.loadTiles(this, "grid-geom.csv");

        // CREATE GRAPH WALKING
        //for (node node : nodesList) {
        for (node node : nodesHashMap.values()){
            graph.addVertex(node.getID());
        }

        for (edge edge : edgesList) {
            graph.addEdge(edge.getSource(), edge.getTarget(), edge);
        }

        int edgeCount = graph.edgeSet().size();

        Log.i("Graph", "The graph has " + edgeCount + " edges.");

        setWeightsNodes(nodesHashMap, tilesList);
        setWeightsEdges(edgesList, nodesHashMap, "Pollution");

        // END GRAPH WALKING
        // ---------------------------------------------------
        // CREATE GRAPH BIKING

        nodesHashMap_b = CSVLoader.loadNodes(this, "nodes-in-grid-bike.csv");
        edgesList_b = CSVLoader.loadEdgesBike(this, "square-grid-edges-bike.csv");

        //for (node node : nodesList) {
        for (node node : nodesHashMap_b.values()){
            graph_b.addVertex(node.getID());
        }

        for (edge edge : edgesList_b) {
            graph_b.addEdge(edge.getSource(), edge.getTarget(), edge);
        }

        int edgeCount_b = graph_b.edgeSet().size();

        Log.i("Graph Bike", "The graph has " + edgeCount_b + " edges.");

        setWeightsNodes(nodesHashMap_b, tilesList);
        setWeightsEdges(edgesList_b, nodesHashMap_b,"Pollution");

        // END GRAPH BIKE
        // ---------------------------------------------------

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Toolbar toolbar = binding.toolbar;
        //setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final ImageButton arrow = binding.arrowButton;
        final ImageButton exchangeButton = binding.exchangeFields;
        final LinearLayout hiddenView = binding.hiddenView;
        final ImageButton cyclingButton = binding.buttonCycle;
        final ImageButton walkingButton = binding.buttonWalk;
        final Button submitButton = binding.btnSubmit;
        final Button btnShowPollution = binding.btnShowPollution;

        final Switch circuitSwitch = binding.switch1;
        final Slider targetDistanceSlider = binding.distanceTargetSlider;

        final LinearLayout distanceTargetLayout = binding.distanceTargetView;

        String apiKey = this.getString(string.api_key);

        walkingButton.setColorFilter(this.getColor(color.white));

        if (!Places.isInitialized()) {
            Places.initialize(this.getApplicationContext(), apiKey);
        }

        AutocompleteSupportFragment autocompleteSupportFragment1 = (AutocompleteSupportFragment) this.getSupportFragmentManager().findFragmentById(id.autocomplete_fragment);
        Intrinsics.checkNotNull(autocompleteSupportFragment1);
        autocompleteSupportFragment1.setPlaceFields(CollectionsKt.listOf(new Place.Field[]{Field.NAME, Field.ADDRESS, Field.LAT_LNG}));
        autocompleteSupportFragment1.setOnPlaceSelectedListener((PlaceSelectionListener) (new PlaceSelectionListener() {
            public void onPlaceSelected(@NotNull Place place) {
                Intrinsics.checkNotNullParameter(place, "place");

                TextView textView = binding.autoTextView;
                String namePlace = place.getName();
                LatLng latLng = place.getLatLng();

                targetText = namePlace;

                textView.setText("To: " + namePlace);

                clearMarker();
                targetLat = latLng.latitude;
                targetLong = latLng.longitude;

                targetMarker = map.addMarker((new MarkerOptions())
                        .position(latLng)
                        .title("Destination"));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0F));

            }

            public void onError(@NotNull Status status) {
                Intrinsics.checkNotNullParameter(status, "status");
                Toast.makeText(MainActivity.this.getApplicationContext(), "No Location selected", 1).show();
            }
        }));

        TextView autoTextViewStart = binding.autoTextViewStart;
        autoTextViewStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the fields to specify which types of place data to return.
                List<Place.Field> fields = Arrays.asList(Field.NAME,  Field.ADDRESS, Field.LAT_LNG);
                //autocompleteSupportFragment1.setPlaceFields(CollectionsKt.listOf(new Place.Field[]{Field.NAME, Field.ADDRESS, Field.LAT_LNG}));

                // Start the autocomplete intent.
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .build(MainActivity.this);

                startAutocomplete.launch(intent);
                //startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        arrow.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public void onClick(View it) {
                if (hiddenView.getVisibility() == VISIBLE) {
                    hiddenView.setVisibility(View.GONE);
                    arrow.setImageResource(drawable.baseline_expand_more_24);
                } else {
                    hiddenView.setVisibility(VISIBLE);
                    arrow.setImageResource(drawable.baseline_expand_less_24);
                }

            }
        }));

        exchangeButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public void onClick(View it) {
                TextView textViewStart = binding.autoTextViewStart;
                TextView textViewDest = binding.autoTextView;

                String interString = startText;

                startText = targetText;
                targetText = interString;

                textViewStart.setText("From: " + startText);
                textViewDest.setText("To: "+ targetText);

                Log.i("exchange", "clicked");

                double interLat = sourceLat;
                double interLong = sourceLong;

                sourceLat = targetLat;
                sourceLong = targetLong;

                targetLat = interLat;
                targetLong = interLong;

                if (targetMarker != null) {
                    targetMarker.remove();
                }

                if (startMarker != null) {
                    startMarker.remove();
                }

                startMarker = map.addMarker((new MarkerOptions()).position(new LatLng(sourceLat, sourceLong))
                        .title(MainActivity.this.getString(string.startPoint)));

                targetMarker = map.addMarker((new MarkerOptions()).position(new LatLng(targetLat, targetLong))
                        .title(MainActivity.this.getString(string.dropped_pin)));

            }
        }));

        walkingButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public void onClick(View it) {
                routeType = false;
                cyclingButton.setBackgroundResource(drawable.round_corner_un);
                cyclingButton.setColorFilter(MainActivity.this.getColor(androidx.appcompat.R.color.material_blue_grey_800));
                walkingButton.setBackgroundResource(drawable.round_corner);
                walkingButton.setColorFilter(MainActivity.this.getColor(color.white));
            }
        }));
        cyclingButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public void onClick(View it) {
                routeType = true;
                walkingButton.setBackgroundResource(drawable.round_corner_un);
                walkingButton.setColorFilter(MainActivity.this.getColor(androidx.appcompat.R.color.material_blue_grey_800));
                cyclingButton.setBackgroundResource(drawable.round_corner);
                cyclingButton.setColorFilter(MainActivity.this.getColor(color.white));
            }
        }));

        circuitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Code to be executed when the switch is toggled
                if (isChecked) {
                    switchFlag = true;

                    //Toast.makeText(MainActivity.this.getApplicationContext(), "Toggled switch to true", 1).show();
                    distanceTargetLayout.setVisibility(VISIBLE);

                    // Circuit Logic
                    // Switch is on
                    // Perform action for switch on
                } else {
                    switchFlag = false;
                    //Toast.makeText(MainActivity.this.getApplicationContext(), "Toggled switch to false", 1).show();
                    distanceTargetLayout.setVisibility(View.GONE);

                    // Route Logic
                    // Switch is off
                    // Perform action for switch off
                }
            }
        });

        btnShowPollution.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public void onClick(View it) {
                pollutionState = !pollutionState;

                if (pollutionState){
                    createPolygons(tilesList);
                    //createHeatmap(nodesHashMap);
                    btnShowPollution.setText(string.hide_pollution);

                }
                else{
                    removePolygons("polys");
                    btnShowPollution.setText(string.show_pollution);
                    //overlay.remove();

                }

            }
        }));
        //targetDistanceSlider.addOnChangeListener(new Slider.OnChangeListener() {
        targetDistanceSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener()  {
            /*@Override
            public void onValueChange(@NonNull Slider targetDistanceSlider, float value, boolean fromUser) {
                //Use the value
                //targetDistanceValue = value;
                //TextView autoDistanceTarget = binding.autoDistanceTarget;
                //autoDistanceTarget.setText("Target Distance (km): " + String.valueOf(value));

                Log.i("Testing on value change",String.valueOf(value));
            }*/

            @Override
            public void onStartTrackingTouch(@NonNull Slider targetDistanceSlider) {
                // This method is called when the user starts moving the slider
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider targetDistanceSlider) {
                // This method is called when the user stops moving the slider
                // The final value of the slider is in slider.getValue()
                float finalValue = targetDistanceSlider.getValue();
                // Now you can use the final value
                targetDistanceValue = finalValue;
                TextView autoDistanceTarget = binding.autoDistanceTarget;
                autoDistanceTarget.setText("Target Distance (km): " + String.valueOf(finalValue));

                Log.i("Testing on value change",String.valueOf(finalValue));
            }

            /*public void onStopTrackingTouch(@NonNull Slider targetDistanceSlider) {
                // This method is called when the user stops moving the slider
                // The final value of the slider is in seekBar.getProgress()
                float finalValue = targetDistanceSlider.getValue();
                // Now you can use the final value
                targetDistanceValue = finalValue;
                TextView autoDistanceTarget = binding.autoDistanceTarget;
                autoDistanceTarget.setText("Target Distance (km): " + String.valueOf(finalValue));

                Log.i("Testing on value change",String.valueOf(finalValue));
            }*/

        });



        submitButton.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public void onClick(View it) {

                //LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location sourceLocation = retrieveLocation();

                //double sourceLat = sourceLocation.getLatitude();
                //double sourceLong = sourceLocation.getLongitude();

                if (startMarker == null){
                    //double sourceLat = sourceLocation.getLatitude();
                    //double sourceLong = sourceLocation.getLongitude();
                    double sourceLat = 51.498066;
                    double sourceLong = -0.1756636;
                }

                node sourceNearestNode = null;
                node targetNearestNode = null;



                if (switchFlag){
                    //Toast.makeText(MainActivity.this.getApplicationContext(), "Submit while switch is true", 1).show();

                    //Polyline runNearestNeighbor(Graph<String, edge> graph, node sourceNearestNode, node targetNearestNode,
                    //
                    //                                HashMap<String, node> nodesHashMap, String Route, float tarDistance, Polyline polyline)
                    //sourceNearestNode = findNearestNode(nodesHashMap, sourceLat, sourceLong);
                    //targetNearestNode = findNearestNode(nodesHashMap, targetLat, targetLong);
                    float targetDistance = 5000;

                    if (polyline2 != null){
                        polyline2.remove();
                    }

                    if (routeType) {

                        sourceNearestNode = findNearestNode(nodesHashMap_b, sourceLat, sourceLong);
                        targetNearestNode = findNearestNode(nodesHashMap_b, targetLat, targetLong);

                        polyline1 = runBestNearestNeighbor(graph_b, sourceNearestNode, targetNearestNode, nodesHashMap_b, "Bike", targetDistanceValue*1000, polyline1);
                    }
                    else{

                        sourceNearestNode = findNearestNode(nodesHashMap, sourceLat, sourceLong);
                        targetNearestNode = findNearestNode(nodesHashMap, targetLat, targetLong);


                        polyline1 = runBestNearestNeighbor(graph, sourceNearestNode, targetNearestNode, nodesHashMap, "Walk", targetDistanceValue*1000, polyline1);
                    }
                }
                else {
                    if (routeType) {
                        // Graph
                        Log.i("Route Type", "will Run Dijkstra for Bike");
                        //node sourceNearestNode = findNearestNode(nodesList,sourceLat, sourceLong);
                        //node targetNearestNode = findNearestNode(nodesList,targetLat, targetLong);

                        sourceNearestNode = findNearestNode(nodesHashMap_b, sourceLat, sourceLong);
                        targetNearestNode = findNearestNode(nodesHashMap_b, targetLat, targetLong);

                        polyline1 = runDijkstra(graph_b, sourceNearestNode, targetNearestNode, nodesHashMap_b, "Pollution", "Bike", polyline1);
                        polyline2 = runDijkstra(graph_b, sourceNearestNode, targetNearestNode, nodesHashMap_b, "Length", "Bike", polyline2);

                    } else {
                        Log.i("Route Type", "will Run Dijkstra for Walk");

                        sourceNearestNode = findNearestNode(nodesHashMap, sourceLat, sourceLong);
                        targetNearestNode = findNearestNode(nodesHashMap, targetLat, targetLong);

                        polyline1 = runDijkstra(graph, sourceNearestNode, targetNearestNode, nodesHashMap, "Pollution", "Walk", polyline1);
                        polyline2 = runDijkstra(graph, sourceNearestNode, targetNearestNode, nodesHashMap, "Length", "Walk", polyline2);

                    }
                }

                if (polyline1 != null) {

                    LatLng startPoint = polyline1.getPoints().get(0);//.size();
                    LatLng endPoint = polyline1.getPoints().get(polyline1.getPoints().size() - 1);

                    if (startMarker != null) {
                        startMarker.remove();
                    }
                    if (targetMarker != null) {
                        targetMarker.remove();
                    }

                    startMarker = map.addMarker((new MarkerOptions()).position(startPoint)
                            .title(MainActivity.this.getString(string.startPoint)));

                    targetMarker = map.addMarker((new MarkerOptions()).position(endPoint)
                            .title(MainActivity.this.getString(string.dropped_pin)));
                    //map.moveCamera(CameraUpdateFactory.newLatLngZoom(infoLatLng, 14.0f));
                }
            }
        }));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
        //return(super.onCreateOptionsMenu(menu));
    }

    // Test for autocomplete textview
    private final ActivityResultLauncher<Intent> startAutocomplete = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Place place = Autocomplete.getPlaceFromIntent(intent);
                        Log.i(TAG, "Place: ${place.getName()}, ${place.getId()}");

                        TextView textView = binding.autoTextViewStart;
                        String namePlace = place.getName();
                        LatLng latLng = place.getLatLng();

                        textView.setText("From: " + namePlace);
                        startText = namePlace;

                        clearMarker();
                        sourceLat = latLng.latitude;
                        sourceLong = latLng.longitude;

                        if (startMarker != null) {
                            startMarker.remove();
                        }

                        MainActivity.this.startMarker = map.addMarker((new MarkerOptions())
                                .position(latLng)
                                .title("Start"));
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0F));
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    // The user canceled the operation.
                    Log.i(TAG, "User canceled autocomplete");
                }
            });

    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        Intrinsics.checkNotNullParameter(item, "item");
        int var2 = item.getItemId();
        GoogleMap var10000;
        boolean var3;
        if (var2 == id.normal_map) {
            var10000 = this.map;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("map");
            }

            var10000.setMapType(1);
            var3 = true;
        } else if (var2 == id.hybrid_map) {
            var10000 = this.map;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("map");
            }

            var10000.setMapType(4);
            var3 = true;
        } else if (var2 == id.satellite_map) {
            var10000 = this.map;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("map");
            }

            var10000.setMapType(2);
            var3 = true;
        } else if (var2 == id.terrain_map) {
            var10000 = this.map;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("map");
            }

            var10000.setMapType(3);
            var3 = true;
        } else {
            var3 = super.onOptionsItemSelected(item);
        }
        return var3;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e("Map rendering", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("Map rendering", "Can't find style. Error: ", e);
        }

        ArrayList<WeightedLatLng> latLngsW = new ArrayList<WeightedLatLng>();

        latLngsW.add(new WeightedLatLng(new LatLng(51.496715, -0.1763672), 5));

        provider = new HeatmapTileProvider.Builder()
                .weightedData(latLngsW)
                .maxIntensity(35.0)
                .build();

        overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provider));

        LatLng london = new LatLng(51.496715, -0.1763672);
        //float zoomLevel = 15.0F;
        float zoomLevel = 14.0F;
        //(-0.1465312673602718 51.51569143211838,
        // -0.2043147326397282 51.51569143211838,
        // -0.2043147326397282 51.479718567881626,
        // -0.1465312673602718 51.479718567881626,
        // -0.1465312673602718 51.51569143211838)
        Polygon polygonInit = map.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(51.51569143211838, -0.1465312673602718),
                        new LatLng(51.51569143211838, -0.2043147326397282),
                        new LatLng( 51.479718567881626, -0.2043147326397282),
                        new LatLng( 51.479718567881626, -0.1465312673602718)
                ));

        //polygonInit.setFillColor(0x7Fe5e5e5);
        polygonInit.setStrokeColor(ContextCompat.getColor(getApplicationContext(),color.muted_blue));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(london, zoomLevel));
        setMapLongClick(map);
        //setOnPolylineClickListener(map);
        map.setBuildingsEnabled(true);
        enableMyLocation();
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission((Context)this, "android.permission.ACCESS_FINE_LOCATION") == 0;
    }

    private void enableMyLocation() {
        if (this.isPermissionGranted()) {
            GoogleMap var10000 = this.map;
            if (var10000 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("map");
            }

            var10000.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions((Activity)this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, this.REQUEST_LOCATION_PERMISSION);
        }
    }

    // ---- REQUEST PERMISSIONS ----

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        Intrinsics.checkNotNullParameter(grantResults, "grantResults");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == 0) {
            this.enableMyLocation();
        }
    }

    public void onRequestPermissionsResultLocation(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == this.REQUEST_LATEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                retrieveLocation();
            } else {
                Log.e("MainActivity", "Permission denied!");
            }
        }
    }

    private Location retrieveLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = null;
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission Not Granted", "User did not grant Location Permission");
                return null;
            }
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);
            } else {
                Log.e("MainActivity", "Location not available!");
            }
        }
        return location;
    }
    // ----------------------
    private void setMapOnZoom(final GoogleMap map, HeatmapTileProvider provider) {
        map.setOnCameraIdleListener((GoogleMap.OnCameraIdleListener)(new GoogleMap.OnCameraIdleListener(){
            public void onCameraIdle() {
                float newZoom = map.getCameraPosition().zoom;

                if ((newZoom >= 15) & (newZoom != ZOOM)) {

                    int radius = 30;

                    if (newZoom > 15) {
                        radius = 40;
                    }
                    if (newZoom >= 17) {

                        radius = 120;
                    }

                    ZOOM = newZoom;

                    Log.i("Zoom", String.valueOf(newZoom));

                    provider.setRadius(radius);
                    //overlay.clearTileCache();
                }
            }
        }));
    }

    private void setMapLongClick(final GoogleMap map) {
        map.setOnMapLongClickListener((GoogleMap.OnMapLongClickListener)(new GoogleMap.OnMapLongClickListener() {
            public void onMapLongClick(@NotNull LatLng latLng) {
                Intrinsics.checkNotNullParameter(latLng, "latLng");

                TextView textView = binding.autoTextView;
                Intrinsics.checkNotNullExpressionValue(textView, "binding.autoTextView");
                //StringCompanionObject var4 = StringCompanionObject.INSTANCE;
                Locale var5 = Locale.getDefault();
                String var6 = "Lat: %1$.5f, Long: %2$.5f";
                Object[] var7 = new Object[]{latLng.latitude, latLng.longitude};
                String var9 = String.format(var5, var6, Arrays.copyOf(var7, var7.length));
                Intrinsics.checkNotNullExpressionValue(var9, "format(locale, format, *args)");
                String snippet = var9;
                if (MainActivity.this.targetMarker != null) {
                    targetMarker.remove();
                }
                DecimalFormat dfZero = new DecimalFormat("0.0000");

                targetLat = latLng.latitude;
                targetLong = latLng.longitude;

                targetMarker = map.addMarker((new MarkerOptions()).position(latLng)
                        .title(MainActivity.this.getString(string.dropped_pin))
                        .snippet(snippet));
                //.icon(BitmapDescriptorFactory.defaultMarker(hsv[0])));

                targetText = "(" + String.valueOf(dfZero.format(targetLat)) + " , " + String.valueOf(dfZero.format(targetLong)) + ")";
                textView.setText( "To: " +targetText);
            }
        }));
    }

    private void setOnPolylineClickListener(final GoogleMap map) {

        map.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline clickedPolyline) {
                if (clickedPolyline.equals(polyline1)) {  // Check if the clicked polyline is the one we're interested in
                    // Do something here
                    if (poly1Show){
                        markerCleanest.setVisible(true);
                        //markerCleanest.isNotVisible();
                    }else{
                        markerCleanest.setVisible(false);
                    }
                    poly1Show =! poly1Show;
                }
                if (clickedPolyline.equals(polyline2)) {
                    if (poly2Show){
                        markerShortest.setVisible(true);
                        //markerCleanest.isNotVisible();
                    }else{
                        markerShortest.setVisible(false);
                    }
                    poly2Show =! poly2Show;
                }
            }
        });
    }

    public Polyline runDijkstra(Graph<String, edge> graph, node sourceNearestNode, node targetNearestNode,

                                HashMap<String, node> nodesHashMap, String Type, String Route , Polyline polyline){

        if (polyline != null) {
            polyline.remove();
        }

        DecimalFormat dfZero = new DecimalFormat("0.00");

        long t1 = System.currentTimeMillis();

        String startVertex = sourceNearestNode.getID();
        Map<String, Double> cleanestPollution = new HashMap<>();
        Map<String, Double> cleanestLength = new HashMap<>();

        List<String> Path;
        double PathPollution;
        double PathLength;

        if (Route.equals("Bike")){


            Map<String, String> previousVertices = new HashMap<>();

            Algorithms.dijkstra(graph, sourceNearestNode.getID(), cleanestPollution, cleanestLength,
                    previousVertices, Type);

            // Custom Method
            Path = Algorithms.getShortestPath(startVertex, targetNearestNode.getID(), previousVertices);
            PathPollution = cleanestPollution.get(targetNearestNode.getID());
            PathLength = cleanestLength.get(targetNearestNode.getID());
            long t2 = System.currentTimeMillis();
            System.out.println("dijkstraV1 Bike.getPath           " + Path);
            System.out.println("DijsktraV1Bike Running Time (ms): "+ (t2-t1));

        }
        else{
            DijkstraAlgorithm dijkstra = new DijkstraAlgorithm();
            for (edge edge : edgesList) {
                dijkstra.addEdge(edge.getSource(), edge.getTarget(), edge);
                                    }
            dijkstra.execute(sourceNearestNode.getID(), Type);


            Path = dijkstra.getPath(targetNearestNode.getID());
            long t2 = System.currentTimeMillis();
            PathPollution = dijkstra.getShortestMain(targetNearestNode.getID());
            PathLength = dijkstra.getShortestSecond(targetNearestNode.getID());

            System.out.println("dijkstraV2.getPath Walk           " + Path);
            System.out.println("DijsktraV2 Walk Running Time (ms): "+ (t2-t1));

        }

        node newNode = null;
        List<LatLng> polylines = new ArrayList<>();

        for (String node: Path){
            //newNode = findNode(nodesList, node);
            newNode = nodesHashMap.get(node);
            polylines.add(new LatLng(newNode.getLongitude(), newNode.getLatitude()));
        }

        List<PatternItem> pattern = Arrays.asList(new Dot(), new Gap(10));
        int polyColor = ContextCompat.getColor(getApplicationContext(),color.green_100);

        if (Type.equals("Length")){
            int alpha = 128; // This is an example alpha value.
            polyColor = ColorUtils.setAlphaComponent(ContextCompat.getColor(getApplicationContext(),color.muted_blue), alpha);
        }
        if (Route.equals("Bike")){
            pattern = Collections.singletonList(new Dash(10));
        }

        polyline = map.addPolyline(new PolylineOptions().addAll(polylines)
                .pattern(pattern)
                .color(polyColor)
                //.geodesic(true)
                .clickable(true)
                .width(15));

        if (Type.equals("Pollution")){

            LinearLayout box = binding.cleanestInfo;
            box.setVisibility(VISIBLE);
            TextView textView = binding.cleanestInfoText;
            String sourceString = "<b>Cleanest</b><br> " + String.valueOf(dfZero.format(PathPollution)) + " pm2.5 </br><br> " + String.valueOf(dfZero.format(PathLength)) +" meters</b>";
            textView.setText(HtmlCompat.fromHtml(sourceString, HtmlCompat.FROM_HTML_MODE_LEGACY));
            //addInfoWindow(String title, String messagePollution, String messageLength, Polyline polyline1, Marker marker, Boolean shortest)
            //markerCleanest = addInfoWindow("Cleanest",String.valueOf(dfZero.format(PathPollution)),String.valueOf(dfZero.format(PathLength)), polyline, markerCleanest, false);

        }
        if (Type.equals("Length")){
            LinearLayout box = binding.shortestInfo;
            box.setVisibility(VISIBLE);
            TextView textView = binding.shortestInfoText;
            String sourceString = "<b>Shortest</b><br> " + String.valueOf(dfZero.format(PathLength)) + " pm2.5 </br><br> " + String.valueOf(dfZero.format(PathPollution)) +" meters</b>";
            textView.setText(HtmlCompat.fromHtml(sourceString, HtmlCompat.FROM_HTML_MODE_LEGACY));
            //addInfoWindowShortest("Cleanest",String.valueOf(dfZero.format(cleanestPathPollution)),String.valueOf(dfZero.format(cleanestPathLength)), polyline);
            //markerShortest = addInfoWindow("Shortest",String.valueOf(dfZero.format(PathLength)),String.valueOf(dfZero.format(PathPollution)), polyline, markerShortest, true);
        }
        clearMarker();

        return polyline;
    }

    public Polyline runBestNearestNeighbor(Graph<String, edge> graph, node sourceNearestNode, node targetNearestNode,

                                HashMap<String, node> nodesHashMap, String Route, float tarDistance, Polyline polyline) {

        if (polyline != null) {
            polyline.remove();
        }

        //List<String> nnPath = Algorithms.bestNearestNeighbor(graph,  sourceNearestNode, targetNearestNode, tarDistance, nodesHashMap);
        bnn BNN = Algorithms.bestNearestNeighbor(graph,  sourceNearestNode, targetNearestNode, tarDistance, nodesHashMap);
        List<LatLng> polylines = new ArrayList<>();
        node newNode = null;
        for (String node: BNN.getRoute()){
            //newNode = findNode(nodesList, node);
            newNode = nodesHashMap.get(node);
            polylines.add(new LatLng(newNode.getLongitude(), newNode.getLatitude()));
        }

        List<PatternItem> pattern = Arrays.asList(new Dot(), new Gap(10));
        int polyColor = ContextCompat.getColor(getApplicationContext(),color.green_100);

        if (Route.equals("Bike")){
            pattern = Collections.singletonList(new Dash(10));
        }

        polyline = map.addPolyline(new PolylineOptions().addAll(polylines)
                .pattern(pattern)
                .color(polyColor)
                //.geodesic(true)
                .clickable(true)
                .width(15));

        LinearLayout boxLength = binding.shortestInfo;
        boxLength.setVisibility(View.GONE);

        DecimalFormat dfZero = new DecimalFormat("0.00");

        LinearLayout box = binding.cleanestInfo;
        box.setVisibility(VISIBLE);
        TextView textView = binding.cleanestInfoText;
        String sourceString = "<b>Circuit Info </b><br> " + String.valueOf(dfZero.format(BNN.getPollution())) + " pm2.5 </br><br> " + String.valueOf(dfZero.format(BNN.getLength())) +" meters</b>";
        textView.setText(HtmlCompat.fromHtml(sourceString, HtmlCompat.FROM_HTML_MODE_LEGACY));

        return polyline;
    }

    // ------------------------------------------------------------

    public static double calculateHaversine(double lon1, double lat1, double lon2, double lat2) {
        // Calculate Haversine Distance

        final double EARTH_RADIUS = 6371;

        double dLon = Math.toRadians(lon2 - lon1);//lonR2 - lonR1;
        double dLat = Math.toRadians(lat2 - lat1);//latR2 - latR1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    //public static node findNearestNode(List<node> nodesCList, double pointX, double pointY) {
    public static node findNearestNode(HashMap<String, node> nodesHashMap, double pointX, double pointY) {
        // Find Nearest Node in graph

        double minDistance = Double.MAX_VALUE;
        node closestNode = null;

        for (node node : nodesHashMap.values()) {

            double distance = calculateHaversine(node.getLongitude(), node.getLatitude(), pointX, pointY);

            if (distance < minDistance) {
                minDistance = distance;
                closestNode = node;
            }
        }
        return closestNode ;
    }

    public static node findNode(List<node> nodesCList, String nodeID) {
        // Find Node in graph

        node searchedNode = null;

        for (node node : nodesCList) {
            if (nodeID.equals(node.getID())) {
                searchedNode = node;
                break;
            }
        }
        return searchedNode ;
    }

    public static void setWeightsEdges(List<edge> edgesList, HashMap<String, node> nodesHashMap, String weightType) {
        //public static void setWeightsEdges(List<edge> edgesList, List<node> nodesList, DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph) {

        for  (edge edge : edgesList) {

            String edgeSource = edge.getSource();
            String edgeTarget = edge.getTarget();

            float nodeSourceWeight = 0.0F;
            float nodeTargetWeight = 0.0F;

            nodeSourceWeight = nodesHashMap.get(edgeSource).getValue();
            nodeTargetWeight = nodesHashMap.get(edgeTarget).getValue();

            /*for (node eNode : nodesList) {
                if (eNode.getID().equals(edgeSource)) {
                    nodeSourceWeight = eNode.getValue();
                    break;
                }
            }
            for (node eNode : nodesList) {
                if (eNode.getID().equals(edgeTarget)) {
                    nodeTargetWeight = eNode.getValue();
                    //Log.i("Found!!!", "Target " + edgeTarget + " value: " + eNode.getValue());
                    break;
                }
            }*/

            float edgeWeight = (nodeSourceWeight + nodeTargetWeight) / 2;
            //edgeWeight = edgeWeight * edge.getLength();
            edge.setPollution(edgeWeight);

        }
        //return graph;
    }

    //public static void setWeightsNodes(List<node> nodesList, List<tile> tilesList) {
    public static void setWeightsNodes(HashMap<String, node> nodesHashMap, List<tile> tilesList) {
        // Find Node in graph

        for  (tile tile : tilesList) {

            int tileID = tile.getID();
            Float tileValue = tile.getValue();

            for (node eNode : nodesHashMap.values()){
                if (eNode.getGrid() == tileID){
                    eNode.setValue(tileValue);
                }
            }
        }
    }

    public void createPolygons(List<tile> tilesList) {

        List<Float> valuesTiles = new ArrayList<>();

        for  (tile tile : tilesList) {
            valuesTiles.add(tile.getValue());
        }

        float maximum = Collections.max(valuesTiles);
        // Find the minimum value
        float minimum = Collections.min(valuesTiles);

        List<Integer> colorList = new ArrayList<>();

        //return Color.argb(50,

        colorList.add(Color.argb( 80,0, 104, 55 ));
        colorList.add(Color.argb(80, 33, 155 , 81 ));
        colorList.add(Color.argb(80, 114, 194 , 100 ));
        colorList.add(Color.argb(80, 183, 224 , 117 ));
        colorList.add(Color.argb(80, 233, 245 , 161 ));
        colorList.add(Color.argb(80, 254, 237 , 161 ));
        colorList.add(Color.argb(80, 253, 190 , 111 ));
        colorList.add(Color.argb(80, 245, 121 , 72 ));
        colorList.add(Color.argb(80, 217, 53 , 41 ));
        colorList.add(Color.argb(80, 165, 0 , 38 ));

        List<Polygon> polygonList = new ArrayList<>();

        for  (tile tile : tilesList) {

            List<LatLng> polygonLatLng = new ArrayList<>();
            String polygon = tile.getGeometry();
            String[] parts = polygon.split("_");


            for (String part : parts){

                String[] latLngStr = part.split(" ");
                polygonLatLng.add(new LatLng(Double.parseDouble(latLngStr[2]), Double.parseDouble(latLngStr[1])));

            }


            float value = Math.round(tile.getValue());
            // assignColor(double value, double min, double max, List<Integer> colorList)
            int color = assignColor(value, minimum, maximum, colorList);

            int POLYGON_STROKE_WIDTH_PX = 5;

            Polygon polygonX = map.addPolygon(new PolygonOptions()
                    .addAll(polygonLatLng)
                    .fillColor(color)
                    .strokeWidth(POLYGON_STROKE_WIDTH_PX));

            polygonList.add(polygonX);
        }

        polygonListsMap.put("polys", polygonList);
    }

    public void removePolygons(String id) {
        List<Polygon> polygonList = polygonListsMap.get(id);
        if (polygonList != null) {
            for (Polygon polygon : polygonList) {
                polygon.remove();
            }
            polygonListsMap.remove(id);
        }
    }

    public int getShadeOfRed(int value) {
        if(value < 0 || value > 100) {
            throw new IllegalArgumentException("Value must be between 0 and 100");
        }

        //int redComponent = (int) (255 * ((100 - value) / 100.0));
        int redComponent = (int) (255 * ((35 - value) / 13.0));
        return Color.argb(50, redComponent, 0, 0);

    }

    public int assignColor(double value, double min, double max, List<Integer> colorList) {
        // Calculate the range for each class
        double range = (max - min) / colorList.size();
        // Find the class index for the given value
        int classIndex = (int) ((value - min) / range);
        // Ensure the class index is within the valid range
        classIndex = Math.max(0, Math.min(classIndex, colorList.size() - 1));

        // Get the color from the color list for the class index
        return colorList.get(classIndex);
    }

    public void createHeatmap(HashMap<String, node> nodesHashMap) {

        int[] colors = {
                Color.rgb(1, 50, 32),
                Color.rgb(46, 184, 110), // green
                Color.rgb(219, 224, 150),
                Color.rgb(255, 165, 0), //orange
                //Color.rgb(201, 48, 74),   // red
                Color.rgb(178, 34, 34) // firebrick
        };

        //android:endColor="#c9304a"
        //        android:startColor="#2eb86e"

        float[] startPoints = {0.0F, 0.25F, 0.5F, 0.75F, 0.9F};

        Gradient gradient = new Gradient(colors, startPoints);

        ArrayList<WeightedLatLng> latLngsW = new ArrayList<WeightedLatLng>();

        //for  (tile tile : tilesList) {
        int value = 0;
        for (node node : nodesHashMap.values()){
            value = Math.round(node.getValue());
            latLngsW.add(new WeightedLatLng(
                    new LatLng(node.getLongitude(), node.getLatitude()),
                    value));
        }
        //HeatmapTileProvider
        provider = new HeatmapTileProvider.Builder()
                .weightedData(latLngsW)
                .gradient(gradient)
                .maxIntensity(100.0)
                .build();

        provider.setRadius(23);

        // Add a tile overlay to the map, using the heat map tile provider.
        overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provider));

        provider.setOpacity(0.50);
        overlay.clearTileCache();

        //setMapOnZoom(map, provider, overlay);
    }

    // MAPS Styling

    public Marker addInfoWindow(String title, String messagePollution, String messageLength, Polyline polyline, Marker marker, Boolean shortest) {
        int pointsOnLine = polyline.getPoints().size();
        //LatLng infoLatLng = polyline.getPoints().get(4 * pointsOnLine / 5);
        LatLng infoLatLng = polyline.getPoints().get(pointsOnLine / 2);

        if (marker != null) {
            marker.remove();
        }

        marker = map.addMarker(new MarkerOptions()
                .position(infoLatLng)
                .icon(bitmapDescriptorFromVector(this, title ,messagePollution, messageLength, shortest)));
        return marker;
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, String id,String label, String label2, boolean Shortest) {
        // Load the custom layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.marker_info_window, null);

        // Set the text of the marker label
        TextView textView = view.findViewById(R.id.marker_label);

        if (Shortest){
            textView.setBackgroundResource(drawable.talking_box_shortest);
        }

        String sourceString = "<b>" + id + "</b><br> " + label + " pm2.5 </br><br> " + label2+" meters</b>";

        textView.setText(HtmlCompat.fromHtml(sourceString, HtmlCompat.FROM_HTML_MODE_LEGACY));

        // Convert the layout to a BitmapDescriptor
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        //view.layout(-10,0);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // helper function clean maps
    public void clearMarker(){
        if (targetMarker != null) {
            targetMarker.remove();
        }
    }

    public void clearCircles(){
        if (MainActivity.this.circleInit != null) {
            MainActivity.this.circleInit.remove();
        }
        if (MainActivity.this.circleEnd != null) {
            MainActivity.this.circleEnd.remove();
        }
    }
}