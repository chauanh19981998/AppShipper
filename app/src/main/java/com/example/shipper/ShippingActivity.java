package com.example.shipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.shipper.common.Common;
import com.example.shipper.common.LatLngInterpolator;
import com.example.shipper.common.MakerAnimation;
import com.example.shipper.model.FCMSendData;
import com.example.shipper.model.ShippingOrderModel;
import com.example.shipper.model.TokenModel;
import com.example.shipper.model.eventbus.UpdateShippingOrderEvent;
import com.example.shipper.remote.IFCMService;
import com.example.shipper.remote.IGoogleAPI;
import com.example.shipper.remote.RetrofitClient;
import com.example.shipper.remote.RetrofitFCMClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ShippingActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Marker shipperMarker;
    private ShippingOrderModel shippingOrderModel;

    //Animation
    private Handler handler;
    private int index, next;
    private LatLng start, end;
    private float v;
    private double lat, lng;
    private Polyline blackPolyline, greyPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;
    private IGoogleAPI iGoogleAPI;
    private IFCMService ifcmService;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.txt_order_number)
    TextView txt_order_number;
    @BindView(R.id.txt_name)
    TextView txt_name;
    @BindView(R.id.txt_address)
    TextView txt_address;
    @BindView(R.id.txt_date)
    TextView txt_date;

    @BindView(R.id.btn_start_trip)
    MaterialButton btn_start_trip;
    @BindView(R.id.btn_call)
    MaterialButton btn_call;
    @BindView(R.id.btn_done)
    MaterialButton btn_done;
    @BindView(R.id.btn_show)
    MaterialButton btn_show;
    @BindView(R.id.expandable_layout)
    ExpandableLayout expandable_layout;

    @BindView(R.id.img_food_image)
    ImageView img_food_image;
    private Polyline yellowPolyline;

    @OnClick(R.id.btn_show)
    void onShowClick() {
        if (expandable_layout.isExpanded())
            btn_show.setText("SHOW");
        else
            btn_show.setText("HIDE");
        expandable_layout.toggle();
    }

    @OnClick(R.id.btn_call)
            void onCallclick() {
        if (shippingOrderModel != null)
        {
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                //request permission
                Dexter.withActivity(this)
                        .withPermission(Manifest.permission.CALL_PHONE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {

                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(ShippingActivity.this, "You must accept this permission to Call User", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                            }
                        }).check();
                return;
            }

                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse(new StringBuilder("tel:")
                .append(shippingOrderModel.getOrderModel().getUserPhone()).toString()));
                startActivity(intent);
        }
    }

    @OnClick(R.id.btn_done)
            void onDoneClick() {
                if (shippingOrderModel != null)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Done Order")
                            .setMessage("Confirm you already shipped this order")
                            .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                            .setPositiveButton("YES", (dialogInterface, i) -> {

                                AlertDialog dialog = new AlertDialog.Builder(this)
                                        .setCancelable(false)
                                        .setMessage("Waiting...")
                                        .create();
                                //update order
                                Map<String,Object> update_data = new HashMap<>();
                                update_data.put("orderStatus", 2);
                                update_data.put("shipperUid",Common.currentShipperUser.getUid());

                                FirebaseDatabase.getInstance()
                                        .getReference(Common.ORDER_REF)
                                        .child(shippingOrderModel.getKey())
                                        //.child(Common.SHIPPER_REF)
                                        //.child(shippingOrderModel.getOrderModel().getKey())
                                        .updateChildren(update_data)
                                        .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                                        .addOnSuccessListener(aVoid -> {

                                            //delete shipping order information
                                            FirebaseDatabase.getInstance()
//                                                    .getReference(Common.ORDER_REF)
//                                                    .child(shippingOrderModel.getKey())
                                                    .getReference(Common.SHIPPING_ORDER_REF)
                                                    .child(shippingOrderModel.getOrderModel().getKey()) //delete only order
                                                    .removeValue()
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnSuccessListener(aVoid1 -> {

                                                        //delete done
                                                        //get token and send notification for user
                                                        FirebaseDatabase.getInstance()
                                                                .getReference(Common.TOKEN_REF)
                                                                .child(shippingOrderModel.getOrderModel().getUserId())
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        if (snapshot.exists())
                                                                        {
                                                                            TokenModel tokenModel = snapshot.getValue(TokenModel.class);
                                                                            Map<String,String> notiData = new HashMap<>();
                                                                            notiData.put(Common.NOTI_TITLE,"Your Trip is Done");
                                                                            notiData.put(Common.NOTI_CONTENT, new StringBuilder("By Tour Guid: ")
                                                                                    .append(Common.currentShipperUser.getName()).append(" Thank You").toString());

                                                                            FCMSendData sendData = new FCMSendData(tokenModel.getToken(),notiData);

                                                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                                                    .subscribeOn(Schedulers.io())
                                                                                    .observeOn(AndroidSchedulers.mainThread())
                                                                                    .subscribe(fcmResponse -> {
                                                                                        dialog.dismiss();
                                                                                        if (fcmResponse.getSuccess() == 1)
                                                                                        {
                                                                                            Toast.makeText(ShippingActivity.this, "Finish", Toast.LENGTH_SHORT).show();
                                                                                        }
                                                                                        else
                                                                                        {
                                                                                            Toast.makeText(ShippingActivity.this, "Update order success but notification don't send", Toast.LENGTH_SHORT).show();
                                                                                        }

                                                                                        if (!TextUtils.isEmpty(Paper.book().read(Common.TRIP_START)))
                                                                                            Paper.book().delete(Common.TRIP_START);
                                                                                        EventBus.getDefault().postSticky(new UpdateShippingOrderEvent());
                                                                                        finish();

                                                                                    }, throwable -> {
                                                                                        dialog.dismiss();
                                                                                        Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    }));



                                                                        }
                                                                        else
                                                                        {
                                                                            dialog.dismiss();
                                                                            Toast.makeText(ShippingActivity.this, "Token not found", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        dialog.dismiss();
                                                                        Toast.makeText(ShippingActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });

                                                    });
                                        });

                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
    }

    AutocompleteSupportFragment places_fragment;
    PlacesClient placesClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    private Polyline redPolyline;

    @OnClick(R.id.btn_start_trip)
    void onStartTripClick() {
        String data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        Paper.book().write(Common.TRIP_START, data);

        btn_start_trip.setEnabled(false);

        shippingOrderModel = new Gson().fromJson(data,new TypeToken<ShippingOrderModel>(){}.getType());
        //update
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    compositeDisposable.add(iGoogleAPI.getDirections("driving",
                            "less_driving",
                            Common.buildLocationString(location),
                            new StringBuilder().append(shippingOrderModel.getOrderModel().getLat())
                    .append(",")
                    .append(shippingOrderModel.getOrderModel().getLng()).toString(),
                            getString(R.string.google_maps_key))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {

                        //get estimate time from APi
                        String estimateTime = "UNKNOWN";
                        JSONObject jsonObject = new JSONObject(s);
                        JSONArray routes = jsonObject.getJSONArray("routes");
                        JSONObject object = routes.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legsObject = legs.getJSONObject(0);
                        //time
                        JSONObject time = legsObject.getJSONObject("duration");
                        estimateTime = time.getString("text");

                        Map<String, Object> update_data = new HashMap<>();
                        update_data.put("currentLat", location.getLatitude());
                        update_data.put("currenLng", location.getLongitude());
                        update_data.put("estimateTime", estimateTime);

                        FirebaseDatabase.getInstance()
                                .getReference(Common.SHIPPING_ORDER_REF)
                                .child(shippingOrderModel.getKey())
                                .updateChildren(update_data)
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                })
                                .addOnSuccessListener(aVoid -> {
                                    drawRoutes(data);
                                });

                    }, throwable -> {
                        Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }));

                })
                .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean isInit = false;
    private Location previousLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping);

        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        initPlaces();
        setupAutocompletePlaces();

        ButterKnife.bind(this);
        buildLocationRequest();
        buildLocationCallback();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(ShippingActivity.this::onMapReady);

                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ShippingActivity.this);
                        if (ActivityCompat.checkSelfPermission(ShippingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ShippingActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(ShippingActivity.this, "You must allow this location permission", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }

    private void setupAutocompletePlaces() {
        places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                drawRoutes(place);
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(ShippingActivity.this, "" + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoutes(Place place) {

        mMap.addMarker(new MarkerOptions()
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        .title(place.getName())
        .snippet(place.getAddress())
        .position(place.getLatLng()));
        //add destination
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(location -> {
                    String to = new StringBuilder()
                            .append(place.getLatLng().latitude)
                            .append(",")
                            .append(place.getLatLng().longitude)
                            .toString();
                    String from = new StringBuilder()
                            .append(location.getLatitude())
                            .append(",")
                            .append(location.getLongitude())
                            .toString();

                    compositeDisposable.add(iGoogleAPI.getDirections("driving", "less_driving",
                            from,to,
                            getString(R.string.google_maps_key))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(s -> {
                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                    for (int i = 0; i < jsonArray.length(); i++)
                                    {
                                        JSONObject route = jsonArray.getJSONObject(i);
                                        JSONObject poly = route.getJSONObject("overview_polyline");
                                        String polyline = poly.getString("points");
                                        polylineList = Common.decodePoly(polyline);
                                    }

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.YELLOW);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    yellowPolyline = mMap.addPolyline(polylineOptions);
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }, throwable -> Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show()));
                });
    }

    private void drawRoutes(String data) {
        ShippingOrderModel shippingOrderModel = new Gson()
                .fromJson(data, new TypeToken<ShippingOrderModel>() {
                }.getType());

        //add box
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.passenger))
                .title(shippingOrderModel.getOrderModel().getUserName())
                .snippet(shippingOrderModel.getOrderModel().getShippingAddress())
                .position(new LatLng(shippingOrderModel.getOrderModel().getLat(), shippingOrderModel.getOrderModel().getLng())));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(location -> {
                    String to = new StringBuilder()
                            .append(shippingOrderModel.getOrderModel().getLat()) //shippingOrderModel.getOrderModel().getLat()
                            .append(",")
                            .append(shippingOrderModel.getOrderModel().getLng()) //shippingOrderModel.getOrderModel().getLng()
                            .toString();
                    String from = new StringBuilder()
                            .append(location.getLatitude())
                            .append(",")
                            .append(location.getLongitude())
                            .toString();

                    compositeDisposable.add(iGoogleAPI.getDirections("driving", "less_driving",
                            from,to,
                            getString(R.string.google_maps_key))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(s -> {
                                try {
                                    JSONObject jsonObject = new JSONObject(s);
                                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                    for (int i = 0; i < jsonArray.length(); i++)
                                    {
                                        JSONObject route = jsonArray.getJSONObject(i);
                                        JSONObject poly = route.getJSONObject("overview_polyline");
                                        String polyline = poly.getString("points");
                                        polylineList = Common.decodePoly(polyline);
                                    }

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.RED);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    redPolyline = mMap.addPolyline(polylineOptions);
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }, throwable -> Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show()));
                });
    }

    private void initPlaces() {
        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    private void setShippingOrder() {
        Paper.init(this);
        String data;
        if (TextUtils.isEmpty(Paper.book().read(Common.TRIP_START))) {
            //if empty just do normal
            btn_start_trip.setEnabled(true);
            data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        } else {
            btn_start_trip.setEnabled(false);
            data = Paper.book().read(Common.TRIP_START);
        }
        if (!TextUtils.isEmpty(data)) {
            drawRoutes(data);
            shippingOrderModel = new Gson()
                    .fromJson(data, new TypeToken<ShippingOrderModel>() {
                    }.getType());
            if (shippingOrderModel != null) {
                Common.setSpanStringColor("Name: ",
                        shippingOrderModel.getOrderModel().getUserName(),
                        txt_name,
                        Color.parseColor("#1e272e"));

//                txt_date.setText(new StringBuilder()
//                        .append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
//                                .format(shippingOrderModel.getOrderModel().getCreateDate())));
                Common.setSpanStringColor("Total: ",
                        String.valueOf(shippingOrderModel.getOrderModel().getFinalPayment()),
                        txt_date,
                        Color.parseColor("#ff3f34"));

                Common.setSpanStringColor("No: ",
                        shippingOrderModel.getOrderModel().getKey(),
                        txt_order_number,
                        Color.parseColor("#673ab7"));

                Common.setSpanStringColor("Address: ",
                        shippingOrderModel.getOrderModel().getShippingAddress(),
                        txt_address,
                        Color.parseColor("#1e272e"));

                Glide.with(this)
                        .load(shippingOrderModel.getOrderModel().getCartItemList().get(0)
                                .getFoodImage())
                        .into(img_food_image);
            }
        } else {
            Toast.makeText(this, "Shipping order is null", Toast.LENGTH_SHORT).show();
        }
    }



    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Add a marker in Sydney and move the camera
                LatLng locationShipper = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());

                updateLocation(locationResult.getLastLocation());
                if (shipperMarker == null)
                {
                    int height, width;
                    height = width = 80;
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat
                            .getDrawable(ShippingActivity.this,R.drawable.shippernew);
                    Bitmap resized = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(),width,height,false);

                    shipperMarker = mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(resized))
                            .position(locationShipper).title("You"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18));
                }

                if (isInit && previousLocation != null)
                {
                    String from = new StringBuilder()
                            .append(previousLocation.getLatitude())
                            .append(",")
                            .append(previousLocation.getLongitude())
                            .toString();
                    String to = new StringBuilder()
                            .append(locationShipper.latitude)
                            .append(",")
                            .append(locationShipper.longitude)
                            .toString();

                    moveMarkerAnimation(shipperMarker,from,to);

                    previousLocation = locationResult.getLastLocation();
                }
                if (!isInit)
                {
                    isInit=true;
                    previousLocation = locationResult.getLastLocation();
                }
            }
        };
    }

    private void updateLocation(Location lastLocation) {


        String data = Paper.book().read(Common.TRIP_START);
        if (!TextUtils.isEmpty(data))
        {



            ShippingOrderModel shippingOrderModel = new Gson()
                    .fromJson(data,new TypeToken<ShippingOrderModel>(){}.getType());
            if (shippingOrderModel != null)
            {
                compositeDisposable.add(iGoogleAPI.getDirections("driving",
                        "less_driving",
                        Common.buildLocationString(lastLocation),
                        new StringBuilder().append(shippingOrderModel.getOrderModel().getLat())
                                .append(",")
                                .append(shippingOrderModel.getOrderModel().getLng()).toString(),
                        getString(R.string.google_maps_key))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> {

                            //get estimate time from APi
                            String estimateTime = "UNKNOWN";
                            JSONObject jsonObject = new JSONObject(s);
                            JSONArray routes = jsonObject.getJSONArray("routes");
                            JSONObject object = routes.getJSONObject(0);
                            JSONArray legs = object.getJSONArray("legs");
                            JSONObject legsObject = legs.getJSONObject(0);
                            //time
                            JSONObject time = legsObject.getJSONObject("duration");
                            estimateTime = time.getString("text");

                            Map<String, Object> update_data = new HashMap<>();
                            update_data.put("currentLat", lastLocation.getLatitude());
                            update_data.put("currenLng", lastLocation.getLongitude());
                            update_data.put("estimateTime", estimateTime);

                            FirebaseDatabase.getInstance()
                                    .getReference(Common.SHIPPING_ORDER_REF)
                                    .child(shippingOrderModel.getKey())
                                    .updateChildren(update_data)
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }); //we don't need draw again


                        }, throwable -> {
                            Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }));
            }
        }
        else {
            Toast.makeText(this, "Please press START TRIP", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveMarkerAnimation(Marker maker, String from, String to) {
        //Request direction API to get data
        compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "less_driving",
                from,to,
                getString(R.string.google_maps_key))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(returnResult -> {

            try {
                //parse json
                JSONObject jsonObject = new JSONObject(returnResult);
                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                for (int i = 0; i < jsonArray.length(); i++)
                {
                    JSONObject route = jsonArray.getJSONObject(i);
                    JSONObject poly = route.getJSONObject("overview_polyline");
                    String polyline = poly.getString("points");
                    polylineList = Common.decodePoly(polyline);
                }

                polylineOptions = new PolylineOptions();
                polylineOptions.color(Color.GRAY);
                polylineOptions.width(5);
                polylineOptions.startCap(new SquareCap());
                polylineOptions.jointType(JointType.ROUND);
                polylineOptions.addAll(polylineList);
                greyPolyline = mMap.addPolyline(polylineOptions);

                blackPolylineOptions = new PolylineOptions();
                blackPolylineOptions.color(Color.BLACK);
                blackPolylineOptions.width(5);
                blackPolylineOptions.startCap(new SquareCap());
                blackPolylineOptions.jointType(JointType.ROUND);
                blackPolylineOptions.addAll(polylineList);
                blackPolyline = mMap.addPolyline(blackPolylineOptions);

                //Animator
                ValueAnimator polylineAnimator = ValueAnimator.ofInt(0,100);
                polylineAnimator.setDuration(2000);
                polylineAnimator.setInterpolator(new LinearInterpolator());
                polylineAnimator.addUpdateListener(valueAnimator -> {
                    List<LatLng> points = greyPolyline.getPoints();
                    int percentValue = (int)valueAnimator.getAnimatedValue();
                    int size = points.size();
                    int newPoints = (int)(size*(percentValue/100.0f));
                    List<LatLng> p = points.subList(0,newPoints);
                    blackPolyline.setPoints(p);
                });

                polylineAnimator.start();

                //Shipper moving
                handler = new Handler();
                index = -1;
                next = 1;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (index < polylineList.size() - 1)
                        {
                            index++;
                            next = index+1;
                            start = polylineList.get(index);
                            end = polylineList.get(next);
                        }
                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                        valueAnimator.setDuration(1500);
                        valueAnimator.setInterpolator(new LinearInterpolator());
                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                v = valueAnimator.getAnimatedFraction();
                                lng = v*end.longitude+(1-v)
                                        *start.longitude;
                                lat = v*end.latitude+(1-v)
                                        *start.latitude;
                                LatLng newPos = new LatLng(lat,lng);
                                maker.setPosition(newPos);
                                maker.setAnchor(0.5f,0.5f);
                                maker.setRotation(Common.getBearing(start,newPos));

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(maker.getPosition())); //move camera
                            }
                        });
                        valueAnimator.start();
                        if (index < polylineList.size() - 2) //reach destination
                            handler.postDelayed(this,1500);
                    }
                }, 1500);

            }catch (Exception e)
            {
                Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }, throwable -> {
            if (throwable != null)
                Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }));
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000); //15s
        locationRequest.setFastestInterval(10000); //10s
        locationRequest.setSmallestDisplacement(20f); //20m
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
        mMap = googleMap;

        setShippingOrder();

        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                    R.raw.uber_light_with_label));
            if (!success)
                Log.e("EMMTDEV", "Style parsing failed");
        }catch (Resources.NotFoundException ex)
        {
            Log.e("EDMTDEV", "Resource not found");
        }
    }

    @Override
    protected void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onDestroy();
    }
}