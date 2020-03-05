package com.elbit.systems.sniper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Activity2DMaps extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener {

    // =============================================================================================
    // Firebase Data Members
    // =============================================================================================
    private CFirebaseUtil   m_cFirebaseUtil                            = null;
    private DatabaseReference  m_cDatabaseCurrentPlayerStateReference  = null;
    private DatabaseReference  m_cDatabaseEventsReference              = null;
    private ChildEventListener m_cCurrentPlayersListener               = null;
    private ChildEventListener m_cEventsListener                       = null;
    private int m_nLastPlayerID                                        = -1;
    private static final int nIOSPlayerID = 999;
    // =============================================================================================
    // Internal Marker Collections
    // =============================================================================================
    protected static Map<Integer, Marker> m_mapPlayerID2Marker = new HashMap<Integer, Marker>();;
    protected static Map<Integer, MarkerOptions> m_mapPlayerID2MarkerOptions = new HashMap<Integer, MarkerOptions>();;
    protected static Map<Integer, CPlayerState> m_mapPlayerID2PlayerData = new HashMap<Integer, CPlayerState>();;

    // =============================================================================================
    // Google Map Data
    // =============================================================================================
    private static final long           UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static Boolean              m_bMapReady                     = false;
    private static GoogleMap            m_cGoogleMap                    = null;
    private static Marker               m_cGoogleMarker                 = null;

    // =============================================================================================
    // Player Request Buttons
    // =============================================================================================
    Button m_btnPlayerRequestRevive  = null;
    Button m_btnPlayerRequestReload  = null;
    Button m_btnPlayerRequestKill    = null;

    // =============================================================================================
    // Player Details Fields
    // =============================================================================================
    private TextView m_cTextView_Player_ID = null;


    // =============================================================================================
    // GPS Location Manager
    // =============================================================================================
    LocationManager m_cLocationManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try
        {
            setContentView(R.layout.activity_maps);

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            // The service is being created
            m_cLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Init Firebase Data Members
            InitFireBase();

            m_btnPlayerRequestRevive  = (Button) findViewById(R.id.BtnRevive);
            if (m_btnPlayerRequestRevive != null)
            {
                m_btnPlayerRequestRevive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        CFirebaseUtil.SendEvent(EventID.REVIVE, m_nLastPlayerID, nIOSPlayerID);
                    }
                });
            }
            m_btnPlayerRequestReload  = (Button) findViewById(R.id.BtnReload);
            if (m_btnPlayerRequestReload != null)
            {
                m_btnPlayerRequestReload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        CFirebaseUtil.SendEvent(EventID.RELOAD, m_nLastPlayerID, nIOSPlayerID);
                    }
                });
            }
            m_btnPlayerRequestKill  = (Button) findViewById(R.id.BtnRequestKillByIOS);
            if (m_btnPlayerRequestKill != null)
            {
                m_btnPlayerRequestKill.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        CFirebaseUtil.SendEvent(EventID.KILL, m_nLastPlayerID, nIOSPlayerID);
                    }
                });
            }
            m_cTextView_Player_ID   = (TextView) findViewById(R.id.PlayerIDTextView);
        }
        catch(Exception e)
        {
            Log.d("Database", e.toString());
        }
    }
    @Override
    protected void onStart()
    {
        super.onStart();
        startGPS();
    }

    void startGPS()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        m_cLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_IN_MILLISECONDS, 1.0f, this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        m_cGoogleMap = googleMap;
        m_bMapReady  = true;
        m_cGoogleMap.setOnMarkerClickListener(this);


        // ====================================================
        // Register To Database On Change
        // ====================================================
        CurrentPlayerListener();
        EventsListener();
        //EventData eventData = new EventData(EventID.FIRE, 1,2, "TIME");
        //CFirebaseUtil.SendFireEvent(1,2, "WEAPON", eHealthState.ALIVE);




    }

    private void OnUpdateOwnshipPositionOn2DMap(double latitude, double longitude)
    {
        LatLng currentPos = new LatLng(latitude, longitude);
        if (m_cGoogleMarker != null)
        {
            m_cGoogleMarker.setPosition(currentPos);
        }
        m_cGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentPos));
    }

    @Override
    public void onLocationChanged(Location location)
    {
        OnUpdateOwnshipPositionOn2DMap(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onMarkerClick(Marker cMarker)
    {
        // Android: creating a Custom view as Marker in Google Maps API
        // https://stackoverflow.com/questions/42233560/android-creating-a-custom-view-as-marker-in-google-maps-api

        String sTitle   = cMarker.getTitle();
        String sSnippet = cMarker.getSnippet();


        Iterator<Map.Entry<Integer, Marker>> it = m_mapPlayerID2Marker.entrySet().iterator();

        while(it.hasNext())
        {
            Map.Entry<Integer, Marker> entry = it.next();

            Marker cPlayerMarker = entry.getValue();
            if (cMarker.equals(cPlayerMarker))
            {
                m_nLastPlayerID     = entry.getKey();
                // Get Player ID
                if(m_mapPlayerID2PlayerData.containsKey(m_nLastPlayerID))
                {
                    CPlayerState cPlayerState = m_mapPlayerID2PlayerData.get(m_nLastPlayerID);
                    String sPlayerID = "Player ID : " + cPlayerState.getPlayerID();
                    m_cTextView_Player_ID.setText(sPlayerID);
                }
            }
        }
        return false;
    }

    void moveCameraPosition(double dLat, double dLon, int nZoomLevel)
    {
        CameraPosition cCameraPosition = new CameraPosition.Builder().target(new LatLng(dLat, dLon)).zoom(nZoomLevel).build();
        m_cGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cCameraPosition));
    }

    void SetMarkerOptions(CPlayerState cPlayerState)
    {
        enumerateForceID enumForceID = cPlayerState.getEnumForceID();
        eHealthState healthState = cPlayerState.getsPlayerState();
        Marker cTempMarker = null;
        MarkerOptions cTempMarkerOptions = null;
        //create markers
        if(m_mapPlayerID2MarkerOptions.containsKey(cPlayerState.getPlayerID()))
        {
            cTempMarkerOptions = m_mapPlayerID2MarkerOptions.get(cPlayerState.getPlayerID());
        }
        else {
            cTempMarkerOptions = new MarkerOptions().position(new LatLng(cPlayerState.getPlayerLatitude(), cPlayerState.getPlayerLongitude())).title(cPlayerState.getPlayerIDStr());
        }
        cTempMarkerOptions.alpha(0.7f);
        switch (enumForceID)
        {
            case BLUE:
                if(healthState == eHealthState.ALIVE) {
                    cTempMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_blue_48));
                }
                else if(healthState == eHealthState.KILLED ||eHealthState.HITTED == healthState) {
                    cTempMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_blue_x_48));
                }
                break;
            case RED:
                if(healthState == eHealthState.ALIVE) {
                    cTempMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_red_48));
                }
                else if(healthState == eHealthState.KILLED || eHealthState.HITTED == healthState) {
                    cTempMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_red_x_48));
                }
                break;
        }
        cTempMarker = m_cGoogleMap.addMarker(cTempMarkerOptions);

        if (null != cTempMarker)
        {
            m_mapPlayerID2Marker.put(cPlayerState.getPlayerID(), cTempMarker);
            m_mapPlayerID2MarkerOptions.put(cPlayerState.getPlayerID(), cTempMarkerOptions);
            m_mapPlayerID2PlayerData.put(cPlayerState.getPlayerID(), cPlayerState);
            cTempMarker.showInfoWindow();
            cTempMarker.setPosition(new LatLng(cPlayerState.getPlayerLatitude(), cPlayerState.getPlayerLongitude()));
        }
        else
        {
            Log.d("database", "onChildAdded: marker is null");
        }
    }

    void ChangeIcon(CPlayerState cPlayerState)
    {
        enumerateForceID enumForceID = cPlayerState.getEnumForceID();
        eHealthState healthState = cPlayerState.getsPlayerState();
        Marker cTempMarker = null;
        //create markers
        if(m_mapPlayerID2Marker.containsKey(cPlayerState.getPlayerID()))
        {
            cTempMarker = m_mapPlayerID2Marker.get(cPlayerState.getPlayerID());
        }
        switch (enumForceID)
        {
            case BLUE:
                if(healthState == eHealthState.ALIVE) {
                    cTempMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_blue_48));
                }
                else if(healthState == eHealthState.KILLED ||eHealthState.HITTED == healthState) {
                    cTempMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_blue_x_48));
                }
                break;
            case RED:
                if(healthState == eHealthState.ALIVE) {
                    cTempMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_red_48));
                }
                else if(healthState == eHealthState.KILLED || eHealthState.HITTED == healthState) {
                    cTempMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.soldier_icon_red_x_48));
                }
                break;
        }
        if (null != cTempMarker)
        {
            m_mapPlayerID2Marker.put(cPlayerState.getPlayerID(), cTempMarker);
            m_mapPlayerID2PlayerData.put(cPlayerState.getPlayerID(), cPlayerState);
            cTempMarker.showInfoWindow();
            cTempMarker.setPosition(new LatLng(cPlayerState.getPlayerLatitude(), cPlayerState.getPlayerLongitude()));
        }
        else
        {
            Log.d("database", "onChildAdded: marker is null");
        }
    }

    void CurrentPlayerListener()
    {
        m_cCurrentPlayersListener = new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s)
            {
                CPlayerState cPlayerState = dataSnapshot.getValue(CPlayerState.class);
                SetMarkerOptions(cPlayerState);
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // player state changed
                CPlayerState cPlayerState = dataSnapshot.getValue(CPlayerState.class);
                CPlayerState cOldPlayerState = m_mapPlayerID2PlayerData.get(cPlayerState.getPlayerID());
                MarkerOptions markerOptions = null;
                if(cOldPlayerState.getsPlayerState() != cPlayerState.getsPlayerState())
                {
                    ChangeIcon(cPlayerState);
                }
                else {
                    m_mapPlayerID2PlayerData.put(cPlayerState.getPlayerID(), cPlayerState);
                    Marker markerPlayer = m_mapPlayerID2Marker.get(cPlayerState.getPlayerID());
                    markerPlayer.showInfoWindow();
                    markerPlayer.setPosition(new LatLng(cPlayerState.getPlayerLatitude(), cPlayerState.getPlayerLongitude()));
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        m_cDatabaseCurrentPlayerStateReference.addChildEventListener(m_cCurrentPlayersListener);
    }

    void EventsListener()
    {
        m_cEventsListener = new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s)
            {
                EventData cEvent = dataSnapshot.getValue(EventData.class);
                switch (cEvent.getsEventID())
                {
                    case FIRE:
                        DrawFireLine(cEvent);
                        break;
                }

            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        m_cDatabaseEventsReference.addChildEventListener(m_cEventsListener);
    }

    void InitFireBase()
    {
        CFirebaseUtil.openFirebaseReferenceByPlayerCurrentState(getString(R.string.Global_Player_Current_State));
        CFirebaseUtil.openFirebaseReferenceByPlayerHistoryState(getString(R.string.Global_Players_History_State));
        CFirebaseUtil.openFirebaseReferenceByEvent(getString(R.string.Global_Events));
        m_cFirebaseUtil = CFirebaseUtil.getFirebaseUtil();
        m_cDatabaseCurrentPlayerStateReference = CFirebaseUtil.m_cDatabaseCurrentPlayerReference;
        m_cDatabaseEventsReference = CFirebaseUtil.m_cDataEventsRef;
    }
    void DrawFireLine(EventData eventData)
    {
        if(m_mapPlayerID2PlayerData.containsKey(eventData.getnOtherPlayerID())&& m_mapPlayerID2PlayerData.containsKey(eventData.getPlayerID()))
        {
            CPlayerState player1 = m_mapPlayerID2PlayerData.get(eventData.getPlayerID());
            CPlayerState player2 = m_mapPlayerID2PlayerData.get(eventData.getnOtherPlayerID());

            // Double HeadingRotation = SphericalUtil.computeHeading(LatLng from, LatLng to)
            Polyline polyline1 = m_cGoogleMap.addPolyline(new PolylineOptions()
                    .clickable(true).color(Color.RED).width(3)
                    .add(
                            new LatLng(player1.getPlayerLatitude(), player1.getPlayerLongitude()),
                            new LatLng(player2.getPlayerLatitude(), player2.getPlayerLongitude())));
            polyline1.setTag("Fire");

            Runnable runnable = new Runnable(){
                public void run() {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            polyline1.remove();
                        }
                    }, 10000);

                }
            };
            runnable.run();
        }
    }
}

