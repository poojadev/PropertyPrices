package asesix.sussex.propertylocations;

import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import asesix.sussex.R;
import asesix.sussex.common.util.sharedprefernce.CustomSharedPreferences;
import asesix.sussex.databinding.ActivityMapsBinding;
import asesix.sussex.network.RetrofitAPI;
import asesix.sussex.propertylocations.locationdetails.savelocation.model.SaveLocationPoJo;
import asesix.sussex.common.util.AppConstants.ApiConstants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO: Register/Sign-In activities done
 * TODO: Search for post code
 * TODO: Track user location
 * TODO: Attribution for backend APIs
 * TODO: Save/unsave locations
 * TODO: Location list
 * TODO: Change marker icon to text with post code or avg price
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private HeatmapTileProvider mProvider;
    private LatLng london;
   // private List<Marker> markerList = new ArrayList<Marker>();
    private ClusterManager<Property> clusterManager;
    private Property selectedClusterItem = null;
    private Property prevSelectedClusterItem = null;
    private PriceClusterRenderer priceClusterRenderer;
    private View starButton;
    Double lat,lng;
    CustomSharedPreferences customSharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();




        starButton = findViewById(R.id.starButton);
        starButton.setVisibility(View.GONE);
        starAction(starButton);

        customSharedPreferences=new CustomSharedPreferences(MapsActivity.this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
        mMap = googleMap;
        clusterManager = new ClusterManager<Property>(this, mMap);
        clusterManager.setAnimation(false);

        priceClusterRenderer = new PriceClusterRenderer(this, mMap, clusterManager);
        priceClusterRenderer.setMinClusterSize(5);

        //Move camera to sydney
        int[] colors = {
                Color.rgb(0, 255, 248),
                Color.rgb(56, 235, 169),
                Color.rgb(123, 207, 79),
                Color.rgb(175, 172, 0),
                Color.rgb(221, 122, 0),
                Color.rgb(255, 0, 0)
        };

        float[] startPoints = {
                0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f
        };
        if(getIntent().getExtras() !=null) {
            lat = Double.valueOf(getIntent().getStringExtra("lat"));
            lng = Double.valueOf(getIntent().getStringExtra("long"));


            String coordinates = String.valueOf(lat) + " " + String.valueOf(lng);

            clusterManager.addItem(new Property(new LatLng(lng,lng), coordinates, "intensityStr", 100));
            ArrayList<WeightedLatLng> locations = generateLocations();
            LatLng newLocation = new LatLng( lat, lng);
            locations.add(new WeightedLatLng(newLocation, 11));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));

        }
        else {
            london = new LatLng(51.5072, 0.1276);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(london));
            clusterManager.setRenderer(priceClusterRenderer);

        }




       if(getIntent().getExtras() ==null) {
            ArrayList<WeightedLatLng> locations = generateLocations();
            mProvider = new HeatmapTileProvider.Builder().weightedData(locations).build();
            mProvider.setRadius(HeatmapTileProvider.DEFAULT_RADIUS);
            mProvider.setGradient(new Gradient(colors, startPoints));
            mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }

       }



    private ArrayList<WeightedLatLng> generateLocations() {
        ArrayList<WeightedLatLng> locations = new ArrayList<WeightedLatLng>();
        double lat;
        double lng;
        double intensity;
        Random generator = new Random();
        for( int i = 0; i < 1000; i++ ) {
            lat = generator.nextDouble();
            lng = generator.nextDouble();
            //intensity = generator.nextDouble() / 3;
            if( generator.nextBoolean() ) {
                lat = -lat;
            }
            if( generator.nextBoolean() ) {
                lng = -lng;
            }

            int randomPrice = ThreadLocalRandom.current().nextInt(50000, 1000001);
            if(getIntent().getExtras() ==null) {

            LatLng newLocation = new LatLng(london.latitude + lat, london.longitude + lng);
            locations.add(new WeightedLatLng(newLocation, randomPrice));
            //Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(sydney.latitude + lat, sydney.longitude + lng)));
            //markerList.add(marker);

            String coordinates = String.valueOf(london.latitude + lat) + " " + String.valueOf(london.longitude + lng);
            String intensityStr = "£ " + String.valueOf(randomPrice);
            //Add markers to clusterManager
                clusterManager.addItem(new Property(newLocation, coordinates, intensityStr, randomPrice));
            }
        }
        setUpClusterer();
        return locations;
    }

    private void setUpClusterer() {
        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
        mMap.getMaxZoomLevel();

        /**
         * Override default onClick behaviour for cluster items (aka markers)
         *  while retaining default behaviour (info window opening) by returning false
         */
        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<Property>() {
            @Override
            public boolean onClusterItemClick(Property item) {
                //If cluster item is already selected, tapping should deselect it
                if(selectedClusterItem == item) {
                    prevSelectedClusterItem = item;
                    selectedClusterItem = null;
                    //Make save button invisible
                    starButton.setVisibility(View.GONE);
                    //Unhighlight selected cluster item
                    if(getIntent().getExtras() ==null) {

                        priceClusterRenderer.getMarker(item).setIcon(BitmapDescriptorFactory.defaultMarker());
                    }


                    //Debug
                    System.out.print("Location Deselected!");
                }
                //If no markers are currently selected, prev and selected should be the same
                else if (selectedClusterItem == null){
                    selectedClusterItem = item;
                    prevSelectedClusterItem = item;
                    //Make save button visible
                    starButton.setVisibility(View.VISIBLE);

                    //Highlight selected cluster item
                    if(getIntent().getExtras() ==null) {

                        priceClusterRenderer.getMarker(item).setIcon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    }

                    //Debug
                    System.out.print("Location Selected!");
                    System.out.println(selectedClusterItem.getTitle() + " " + selectedClusterItem.getSnippet());
                }
                else {
                    //Set value for previously selected cluster item so we can un-highlight its marker
                    prevSelectedClusterItem = selectedClusterItem;
                    selectedClusterItem = item;

                    //Highlight selected cluster item
                    priceClusterRenderer.getMarker(item).setIcon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    //Remove highlight of previously selected cluster item
                    priceClusterRenderer.getMarker(prevSelectedClusterItem).setIcon(BitmapDescriptorFactory.defaultMarker());

                    //Make save button visible
                    starButton.setVisibility(View.VISIBLE);


                    //Debug
                    System.out.print("Location Selected!");
                    System.out.println(selectedClusterItem.getTitle() + " " + selectedClusterItem.getSnippet());
                }
                return false;
            }
        });
    }














    //Activated when user favorites a location
    public void starAction(View view) {
        View starButton = findViewById(R.id.starButton);
        starButton.setOnClickListener(v -> {
            Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(selectedClusterItem.getPosition().latitude,
                        selectedClusterItem.getPosition().latitude, 1);
                Address obj = addresses.get(0);
                String add = obj.getAddressLine(0);
                add = add + "\n" + obj.getCountryName();
                add = add + "\n" + obj.getCountryCode();
                add = add + "\n" + obj.getAdminArea();
                add = add + "\n" + obj.getPostalCode();
                add = add + "\n" + obj.getSubAdminArea();
                add = add + "\n" + obj.getLocality();
                add = add + "\n" + obj.getSubThoroughfare();

                Log.e("IGA", "Address" +selectedClusterItem.getPosition().latitude );
                // Toast.makeText(this, "Address=>" + add,
                // Toast.LENGTH_SHORT).show();
                saveLocation("61aa13b743c47e5b3173a716",selectedClusterItem.getPosition().latitude,selectedClusterItem.getPosition().longitude,obj.getLocality(),"Red");


                // TennisAppActivity.showDialog(add);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        //TODO Change icon to bin or X and change colour to red
        //TODO Save location on server
        //TODO Make marker yellow
        //TODO Remove cluster item from cluster manager and make regular marker
    }

    //Activated when a user un-favorites a location
    public void unStarAction(View view) {
        //View starButton = findViewById(R.id.starButton);
        //TODO Change icon back to star and change colour to yellow
        //TODO Remove saved location on server
        //TODO Maker marker default colour
        //TODO Remove regular marker and make cluster item to add to cluster manager
    }


    public  void saveLocation(String userId, double dbrLat, Double dbrLong, String strLocationName,String colorName) {

        // below line is for displaying our progress bar.
        //loadingPB.setVisibility(View.VISIBLE);

        // on below line we are creating a retrofit
        // builder and passing our base url
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConstants.BASE_URL)
                // as we are sending data in json format so
                // we have to add Gson converter factory
                .addConverterFactory(GsonConverterFactory.create())
                // at last we are building our retrofit builder.
                .build();
        // below line is to create an instance for our retrofit api class.
        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

        // passing data from our text fields to our modal class.
        SaveLocationPoJo saveLocationPoJo = new SaveLocationPoJo(userId,dbrLat,dbrLong,strLocationName,"red");

        // calling a method to create a post and passing our modal class.
        Call<SaveLocationPoJo> call = retrofitAPI.addUserLocation("Bearer " + customSharedPreferences.getStringValue("token") ,saveLocationPoJo);

        // on below line we are executing our method.
        call.enqueue(new Callback<SaveLocationPoJo>() {
            @Override
            public void onResponse(@NotNull Call<SaveLocationPoJo> call, @NotNull Response<SaveLocationPoJo> response) {
                
                SaveLocationPoJo responseFromAPI = response.body();

                // on below line we are getting our data from modal class and adding it to our string.
//                assert responseFromAPI != null;
               

//                Intent login = new Intent(MapsActivity.this, FavouriteLocationsListActivity.class);
//                startActivity(login);

                // below line we are setting our
                // string to our text view.
                // responseTV.setText(responseString);

            }

            @Override
            public void onFailure(@NotNull Call<SaveLocationPoJo> call, Throwable t) {
                // setting text to our text view when
                // we get error response from API.
                //responseTV.setText("Error found is : " + t.getMessage());
                Log.e("Fail" ,""+ t.getMessage().toString());

            }
        });
    }

}