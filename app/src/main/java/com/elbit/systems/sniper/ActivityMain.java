package com.elbit.systems.sniper;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

//import com.felhr.usbserial.*;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import android.location.Location;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;

import android.hardware.usb.*;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

// =============================================================================================
// CLASS MAIN ACTIVITY
// =============================================================================================
public class ActivityMain extends AppCompatActivity implements LocationListener
{
    private static final int  GPS_CODE_PERMISSION = 1;
    private static final long UPDATE_GPS_INTERVAL_IN_MILLISECONDS = 1000;
    private static final int  REQUEST_CAPTURE_IMAGE = 100;

    @Override
    public void onStart()
    {
        super.onStart();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, GPS_CODE_PERMISSION);
        }
        else
        {
            startGpsPermission();
        }
    }

    @Override
    public void onLocationChanged(final Location location)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // Update Local Counter
                CGlobalGpsState.UpdateGPSCounter();

                // Number Data
                CGlobalGpsState.m_nLatitude  = location.getLatitude();
                CGlobalGpsState.m_nLongitude = location.getLongitude();
                CGlobalGpsState.m_nAltitude  = location.getAltitude();
                CGlobalGpsState.m_nTime      = location.getTime();

                // String Data
                CGlobalGpsState.m_sLatitude  = String.format ("%,.6f", CGlobalGpsState.m_nLatitude);
                CGlobalGpsState.m_sLongitude = String.format ("%,.6f", CGlobalGpsState.m_nLongitude);
                CGlobalGpsState.m_sAltitude  = String.format ("%,.2f", CGlobalGpsState.m_nAltitude);
                CGlobalGpsState.m_sTime      = m_cDateFormat.format(new Date(location.getTime()));
            }
        });

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }

    // =============================================================================================
    // Firebase Data Members
    // =============================================================================================
    private FirebaseDatabase m_cDatabaseUtil = null;
    private DatabaseReference m_cDatabaseCurrentPlayerStateReference = null;
    private DatabaseReference m_cDatabaseHistoryPlayerStateReference = null;
    private ChildEventListener m_cChildEventListener = null;

    // =============================================================================================
    // General Status Data Members
    // =============================================================================================
    boolean m_bGeneralError = false;
    String  m_sGeneralErrorString = "";

    // =============================================================================================
    // Data Message From CCU
    // =============================================================================================
    private static int m_nMessageDataFromCCU_MsgCnt                 = 0;     // Header Message Counter
    private static int m_nMessageDataFromCCU_MsgTypeID 		        = 0; 	 // 0 = Reply for Status update request, 1 = Event status – Hit, 2 = Event status – Fire, 3 = Periodic Update
    private static int m_nMessageDataFromCCU_ValidityBitMask        = 0;     // Bit 0 - Location + Time Valid, Bit 1 - Last Hit Present, Bits 2-15 - Spare
    private static int m_nMessageDataFromCCU_HarnessID 		        = 0;     // For demo own harness ID
    private static int m_nMessageDataFromCCU_ExerciseID 		    = 0;     // TBD (For demo, keep ‘00’hex)
    private static int m_nMessageDataFromCCU_LocCoordX 		        = 0;     // DDDMMSSsss. Own harness location GEOTime format. To be used if ValidityBitMask & 0x01.
    private static double m_dMessageDataFromCCU_LocCoordX           = 0;
    private static int m_nMessageDataFromCCU_LocCoordY 		     = 0;     // DDDMMSSsss. Own harness location GEOTime format. To be used if ValidityBitMask & 0x01.
    private static double m_dMessageDataFromCCU_LocCoordY           = 0;
    private static int m_nMessageDataFromCCU_LocCoordZ 		        = 0;     // Height from sea level. Own harness altitude m. To be used if ValidityBitMask & 0x01.
    private static int m_nMessageDataFromCCU_EventTime 		        = 0;     // Seconds since Jan 1, 1970 (EPOCH time)	Seconds. To be used if ValidityBitMask & 0x01.
    private static int m_eMessageDataFromCCU_SenderType 		    = 0;     // 1 = IOS, 2 = Field instructor, 3 = Trainee / Mobile device (Use for Demo)
    private static int m_eMessageDataFromCCU_RoleID 			    = 0;     // TBD (For demo, keep ‘00000000’hex)
    private static int m_eMessageDataFromCCU_HealthState 	        = 0;     // 1 = Health / Revive (Default value), 2 = Damage / Injured, 3 = Destroyed / Killed
    private static int m_nMessageDataFromCCU_ThresholdDistance      = 0;     // TBD (For demo, keep ‘0000’hex)
    private static int m_nMessageDataFromCCU_ThresholdTime 	        = 0;     // TBD (For demo, keep ‘0000’hex)
    private static int m_eMessageDataFromCCU_WeaponType 		    = 0;     // Own harness weapon type
    private static int m_eMessageDataFromCCU_AmmoType 		        = 0;     // Own harness munition type
    private static int m_nMessageDataFromCCU_CurrentAmmoVal 	    = 0;     // Own harness current munition count
    private static int m_nMessageDataFromCCU_LastAttackerHarnessID  = 0;     // HarnessID of the attacking entity received via laser – To be used in case of Hit event (MsgTypeID = 3)
    private static int m_nMessageDataFromCCU_LastHitMunitionTypeID  = 0;     // Munition type of the attacking entity received via laser – To be used in case of Hit event (MsgTypeID = 3)
    private static int m_nMessageDataFromCCU_LastAttackerLocX       = 0;     // Attacker harness location GEOTime format. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
    private static int m_nMessageDataFromCCU_LastAttackerLocY       = 0;     // Attacker harness location GEOTime format. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
    private static int m_nMessageDataFromCCU_LastAttackerLocZ       = 0;     // Attacker harness location GEOTime format. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
    private static int m_eMessageDataFromCCU_HitLocation 	        = 0;     // 1 = Front, 2 = Back, 3 = Left side, 4 = Right Sidem, 5 = Head, 6 = Left leg, 7 = Right leg. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
    private static int m_nMessageDataFromCCU_PU_BatteryPower        = 0;     // Percent
    private static int m_nMessageDataFromCCU_HB_BatteryPower        = 0;     // Percent
    private static int m_nMessageDataFromCCU_LE1_BatteryPower       = 0;     // Percent
    private static int m_eMessageDataFromCCU_HitType 		        = 0;     // To be used in case of Hit event (MsgTypeID = 3). 1 = Miss (Default value), 2 = Near hit, 3 = Hit
    private static int m_nMessageDataFromCCU_LE2_BatteryPower       = 0;     // Percent

    // =============================================================================================
    // Message Help Enums
    // =============================================================================================
    // PLAYER TYPE ENUM
    public static final int PLAYER_TYPE_UNKOWN        = 0;
    public static final int PLAYER_TYPE_AS_IOS        = 1;
    public static final int PLAYER_TYPE_AS_INSTRACTOR = 2;
    public static final int PLAYER_TYPE_AS_TRAINEE    = 3;

    // PLAYER HEALTH STATE ENUM
    public static final int PLAYER_HEALTH_STATE_UNKOWN     = 0;
    public static final int PLAYER_HEALTH_STATE_AS_ALIVE   = 1;
    public static final int PLAYER_HEALTH_STATE_AS_INJURED = 2;
    public static final int PLAYER_HEALTH_STATE_AS_KILLED  = 3;

    // PLAYER_HIT_LOCATION_ENUM
    public static final int PLAYER_HIT_LOCATION_UNKOWN       = 0;
    public static final int PLAYER_HIT_LOCATION_IN_FRONT     = 1;
    public static final int PLAYER_HIT_LOCATION_IN_BACK      = 2;
    public static final int PLAYER_HIT_LOCATION_IN_LEFT      = 3;
    public static final int PLAYER_HIT_LOCATION_IN_RIGHT     = 4;
    public static final int PLAYER_HIT_LOCATION_IN_HEAD      = 5;
    public static final int PLAYER_HIT_LOCATION_IN_LEFT_LEG  = 6;
    public static final int PLAYER_HIT_LOCATION_IN_RIGHT_LEG = 7;

    // Message Help Data
    private static String m_sPlayerType        = "Trainee";
    private static String m_sPlayerHealthState = "Alive";
    private static CPlayerState m_cLastPlayerState = null;

    // =============================================================================================
    // Location Data Members
    // =============================================================================================    
    private LocationManager          m_cLocationManager   = null;
    private double                   m_cLocationLatitude  = 0;
    private double                   m_cLocationLongitude = 0;
    private long                     m_cLocationTime      = 0;
    private DateFormat               m_cDateFormat        = new SimpleDateFormat("HH:mm:ss");    

    // =============================================================================================
    // Serial Comm Port Main Class
    // =============================================================================================
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private CSimpleSerialCommPort m_cSerialCommPort = null;

    // =============================================================================================
    // Serial Comm Port Read Data
    // =============================================================================================
    private boolean  m_bConnectionWithCCU_Valid                     = false;
    private int      m_nCCUMsgBufferHandlerIndex                    = 0;
    byte[]           m_byteSerialCommPortReadDataBufferFromCCU      = new byte[128]; // byte[73]
    private byte     m_byteSerialCommPortReadDataBuffer[]           = new byte[128]; // byte[73]
    private byte     m_byteSerialCommPortReadDataMinimumSlot[]      = new byte[128]; // byte[73]
    private int      m_cSerialCommPortReadGoodMessageFromCCUCounter = 0;
    private int      m_cSerialCommPortreadErrorMessageFromCCUCounter  = 0;


    // =============================================================================================
    // Serial Comm Message From Mobile
    // =============================================================================================
    CMessageFromMobile m_cMessageFromMobile = new CMessageFromMobile();
    private int  m_cSerialCommPortSendGoodMessageToCCUCounter    = 0;
    private int  m_cSerialCommPortSendErrorMessageToCCUCounter   = 0;


    // =============================================================================================
    // Help Data
    // =============================================================================================
    private HandlerThread m_cHandlerThread = new HandlerThread("MyNetThread");
    private Handler m_cLooperHandler       = null;
    private Looper  m_cLooper              = null;
    private Handler m_cHandler             = null;

    // =============================================================================================
    // UI Text Help Fields
    // =============================================================================================
    private TextView m_cTextView_VestCommStatus = null;
    private TextView m_cTextView_CentralStationCommStatus = null;
    private TextView m_cTextView_Exercise_ID = null;
    private TextView m_cTextView_Player_ID = null;
    private TextView m_cTextView_GPSStatus = null;
    private TextView m_cTextView_GPSLat = null;
    private TextView m_cTextView_GPSLon = null;
    private TextView m_cTextView_GPSAlt = null;
    private TextView m_cTextView_GPSTime = null;
    private TextView m_cTextView_HealthStatus = null;
    private TextView m_cTextView_MainWeaponStatus = null;
    private TextView m_cTextView_SlaveWeaponStatus = null;
    private TextView m_cTextView_BatteryStatus = null;
    private TextView m_cTextView_GeneralLog = null;
    private TextView m_cTextView_CCULastMessageStatus = null;

    // =============================================================================================
    // UI Navigation Buttons
    // =============================================================================================
    Button m_btnPlayerConfig         = null;
    Button m_btnPlayer2DMap          = null;
    Button m_btnPlayerNotification   = null;
    Button m_btnPlayerHistory        = null;
    Button m_btnPlayerRequestSave    = null;
    Button m_btnPlayerRequestRevive  = null;
    Button m_btnPlayerRequestDestroy = null;
    Button m_btnPlayerRequestReload  = null;
    Button m_btnTakePlayerCapture    = null;

    // =============================================================================================
    // Camera Section (https://stackoverflow.com/questions/5991319/capture-image-from-camera-and-display-in-activity)
    // =============================================================================================
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static ImageView m_cPlayerCaptureImageView = null;

    // =============================================================================================
    // Class Methods
    // =============================================================================================
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Init Firebase Data Members
        CFirebaseUtil.openFirebaseReferenceByPlayerCurrentState("Global_Players_Current_State");
        CFirebaseUtil.openFirebaseReferenceByPlayerHistoryState("Global_Players_History_State");
        m_cDatabaseUtil = CFirebaseUtil.m_cFirebaseDatabase;
        m_cDatabaseCurrentPlayerStateReference = CFirebaseUtil.m_cDatabaseCurrentPlayerReference;
        m_cDatabaseHistoryPlayerStateReference = CFirebaseUtil.m_cDatabaseHistoryPlayerReference;

        // Join Buttons
        m_btnPlayerConfig         = (Button) findViewById(R.id.btnDoPlayerConfig);
        m_btnPlayer2DMap          = (Button) findViewById(R.id.btnDoViewOn2DMap);
        m_btnPlayerNotification   = (Button) findViewById(R.id.btnDoNotificationToIOS);
        m_btnPlayerHistory        = (Button) findViewById(R.id.btnDoShowPlayerHistory);
        m_btnPlayerRequestSave    = (Button) findViewById(R.id.btnPlayerRequestSaveState);
        m_btnPlayerRequestRevive  = (Button) findViewById(R.id.btnPlayerRequestRevive);
        m_btnPlayerRequestDestroy = (Button) findViewById(R.id.btnPlayerRequestDestroy);
        m_btnPlayerRequestReload  = (Button) findViewById(R.id.btnPlayerRequestReloadAmmo);
        m_btnTakePlayerCapture    = (Button) findViewById(R.id.btnTakePlayerCapture);

        // Join UI Fields
        m_cTextView_CCULastMessageStatus = (TextView) findViewById(R.id.TextView_CCULastMessageStatus);
        m_cTextView_GeneralLog        = (TextView) findViewById(R.id.TextView_SystemViewLog) ;
        m_cTextView_VestCommStatus    = (TextView) findViewById(R.id.TextView_VestCommStatus);
        m_cTextView_CentralStationCommStatus = (TextView) findViewById(R.id.TextView_CentralStationCommStatus);
        m_cTextView_Exercise_ID       = (TextView) findViewById(R.id.TextView_Exercise_ID);
        m_cTextView_Player_ID         = (TextView) findViewById(R.id.TextView_Player_ID);
        m_cTextView_GPSStatus         = (TextView) findViewById(R.id.TextView_GPSStatus);
        m_cTextView_GPSLat            = (TextView) findViewById(R.id.TextView_GPSLat);
        m_cTextView_GPSLon            = (TextView) findViewById(R.id.TextView_GPSLon);
        m_cTextView_GPSAlt            = (TextView) findViewById(R.id.TextView_GPSAlt);
        m_cTextView_GPSTime           = (TextView) findViewById(R.id.TextView_GPSTime);
        m_cTextView_HealthStatus      = (TextView) findViewById(R.id.TextView_HealthStatus);
        m_cTextView_MainWeaponStatus  = (TextView) findViewById(R.id.TextView_MainWeaponStatus);
        m_cTextView_SlaveWeaponStatus = (TextView) findViewById(R.id.TextView_SlaveWeaponStatus);
        m_cTextView_BatteryStatus     = (TextView) findViewById(R.id.TextView_BatteryStatus);

        // Image View
        m_cPlayerCaptureImageView     = (ImageView) findViewById(R.id.ImagePlayerView);

        // Set Color Status
        {
            m_cTextView_VestCommStatus.setTypeface(null, Typeface.BOLD);
            m_cTextView_VestCommStatus.setTextColor((Color.RED));

            m_cTextView_CentralStationCommStatus.setTypeface(null, Typeface.BOLD);
            m_cTextView_CentralStationCommStatus.setTextColor((Color.RED));

            m_cTextView_GPSStatus.setTypeface(null, Typeface.BOLD);
            m_cTextView_GPSStatus.setTextColor((Color.RED));
        }

        // ==========================================================
        // Serial Comm
        // ==========================================================
        m_cSerialCommPort = new CSimpleSerialCommPort();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ==========================================================
        // Thread Helpers Class
        // ==========================================================
        m_cHandler = new Handler();
        m_cHandlerThread.start();
        m_cLooper = m_cHandlerThread.getLooper();
        m_cLooperHandler = new Handler(m_cLooper);

        // ==========================================================
        // GPS Location
        // ==========================================================
        m_cLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        // ==============================================================
        // Go To Player Configuration
        // ==============================================================
        m_btnPlayerConfig = (Button)findViewById(R.id.btnDoPlayerConfig);
        if (m_btnPlayerConfig != null)
        {
            m_btnPlayerConfig.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    if (m_bConnectionWithCCU_Valid == true) {
                        Toast.makeText(view.getContext(), "Config Service Not Active\nSystem Busy In CCU Communication", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Intent intent = new Intent(getApplicationContext(), ActivityConfigPlayer.class);
                        startActivity(intent);
                    }
                }
            });
        }

        // ==============================================================
        // Go To Player View On 2D Map
        // ==============================================================
        m_btnPlayer2DMap = (Button)findViewById(R.id.btnDoViewOn2DMap);
        if (m_btnPlayer2DMap != null)
        {
                m_btnPlayer2DMap.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        //if (m_bConnectionWithCCU_Valid == true) {
                        //    Toast.makeText(view.getContext(), "2D Map Viewer Not Active\nSystem Busy In CCU Communication", Toast.LENGTH_LONG).show();
                        //}
                        //else {
                            Intent intent = new Intent(getApplicationContext(), Activity2DMaps.class/*ActivityViewOn2DMap.class*/);
                            startActivity(intent);
                        //}
                    }
                });

        }

        // ==============================================================
        // Go To Player Notification (Report)
        // ==============================================================
        m_btnPlayerNotification = (Button)findViewById(R.id.btnDoNotificationToIOS);
        if (m_btnPlayerNotification != null)
        {
            m_btnPlayerNotification.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {

                    /*
                    double latitude  = 0;
                    double longitude = 0;
                    double r_earth   = 6378; // in km
                    double dx = 0;
                    double dy = 0;
                    double new_latitude  = latitude  + (dy / r_earth) * (180 / pi);
                    double new_longitude = longitude + (dx / r_earth) * (180 / pi) / cos(latitude * pi/180);
                    */

                    {
                        double my_lat = 0;
                        double my_long = 0;
                        double meters = 50;

                        /*
                        number of km per degree = ~111km (111.32 in google maps, but range varies between 110.567km at the equator and 111.699km at the poles)
                        1km in degree = 1 / 111.32km = 0.0089
                        1m in degree = 0.0089 / 1000 = 0.0000089
                        pi / 180 = 0.018
                        */

                        double coef = meters * 0.0000089;
                        double new_lat = my_lat + coef;
                        double new_long = my_long + coef / Math.cos(my_lat * 0.018);
                    }


                    if (m_bConnectionWithCCU_Valid == true) {
                        Toast.makeText(view.getContext(), "Report system Not Active\nSystem Busy In CCU Communication", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Intent intent = new Intent(getApplicationContext(), ActivityPlayerReportNotifacation.class);
                        startActivity(intent);
                    }
                }
            });
        }

        // ==============================================================
        // Go To Player History View
        // ==============================================================
        m_btnPlayerHistory = (Button)findViewById(R.id.btnDoShowPlayerHistory);
        if (m_btnPlayerHistory != null)
        {
            m_btnPlayerHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    if (m_bConnectionWithCCU_Valid == true) {
                        Toast.makeText(view.getContext(), "Player History Monitor Not Active\nSystem Busy In CCU Communication", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Intent intent = new Intent(getApplicationContext(), ActivityPlayerHistory.class);
                        startActivity(intent);
                    }
                }
            });
        }

        // ==============================================================
        // Go To Player Request Save
        // ==============================================================
        m_btnPlayerRequestSave = (Button)findViewById(R.id.btnPlayerRequestSaveState);
        if (m_btnPlayerRequestSave != null)
        {
            m_btnPlayerRequestSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    try
                    {
                        // Elbit Netanya => Latitude=32.288389,Longitude=34.864182
                        String sExerciseID      = "1";
                        int    nPlayerID        = 1;
                        eHealthState ePlayerState     = eHealthState.ALIVE;
                        double sPlayerLongitude = 34.864182;
                        double sPlayerLatitude  = 32.288389;
                        double sPlayerAltitude  = 81.2;
                        String sMainWeapon      = "M-16";
                        String sSlaveWeaponBullets = "29";
                        String sTimestramp =        "12:00:00";
                        String sforceID = "BLUE";
                        String [] arrayForce = {"BLUE", "BLUE", "RED", "RED"};
                        eHealthState [] arraylife = {eHealthState.ALIVE, eHealthState.HITTED, eHealthState.ALIVE,  eHealthState.HITTED};
                        int nMaxPlayers = 4;
                        for (int i = 0; i < nMaxPlayers; i++)
                        {
                            m_cLastPlayerState = new CPlayerState(sExerciseID, nPlayerID + i, arraylife[i], sPlayerLongitude + i, sPlayerLatitude + i, sPlayerAltitude, arrayForce[i]);
                            CPlayerState cPlayerState = new CPlayerState(sExerciseID, nPlayerID + i, arraylife[i], sPlayerLongitude + i, sPlayerLatitude + i, sPlayerAltitude, arrayForce[i]);
                            if (m_cDatabaseCurrentPlayerStateReference != null)
                            {
                                m_cDatabaseCurrentPlayerStateReference.child(cPlayerState.getPlayerIDStr()).setValue(cPlayerState);
                            }

                            if (m_cDatabaseHistoryPlayerStateReference != null) {
                                m_cDatabaseHistoryPlayerStateReference.push().setValue(cPlayerState);
                            }
                        }

                        Toast.makeText(view.getContext(), "Player Last State Saved ...", Toast.LENGTH_LONG).show();
                    }
                    catch(Exception e)
                    {
                        Log.e("Firebase",e.getMessage());
                    }
                }
            });
        }

        // ==============================================================
        // Go To Player Request Revive
        // ==============================================================
        m_btnPlayerRequestRevive = (Button)findViewById(R.id.btnPlayerRequestRevive);
        if (m_btnPlayerRequestRevive != null)
        {
            m_btnPlayerRequestRevive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    if (m_cLastPlayerState != null) {
                        m_cLastPlayerState.setsPlayerState(eHealthState.ALIVE);
                        CFirebaseUtil.UpdatePlayerState(m_cLastPlayerState);

                    Toast.makeText(view.getContext(), "Player Revived ...", Toast.LENGTH_LONG).show();
                    }


                }
            });
        }

        // ==============================================================
        // Go To Player Request Destroy
        // ==============================================================
        m_btnPlayerRequestDestroy = (Button)findViewById(R.id.btnPlayerRequestDestroy);
        if (m_btnPlayerRequestDestroy != null)
        {
            m_btnPlayerRequestDestroy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    if (m_cLastPlayerState != null)
                        m_cLastPlayerState.setsPlayerState(eHealthState.KILLED);
                    Toast.makeText(view.getContext(), "Player Killed ...", Toast.LENGTH_LONG).show();
                }
            });
        }

        // ==============================================================
        // Go To Player Request Reload Ammo
        // ==============================================================
        m_btnPlayerRequestReload = (Button)findViewById(R.id.btnPlayerRequestReloadAmmo);
        if (m_btnPlayerRequestReload != null)
        {
            m_btnPlayerRequestReload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    Toast.makeText(view.getContext(), "Reload Ammo ...", Toast.LENGTH_LONG).show();
                }
            });
        }

        // ==============================================================
        // Go To Player Take Capture
        // ==============================================================
        m_btnTakePlayerCapture = (Button)findViewById(R.id.btnTakePlayerCapture);
        if (m_btnTakePlayerCapture != null)
        {
            m_btnTakePlayerCapture.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onClick(View view)
                {

                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                    }
                    else
                    {
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    }

                    /*
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, TAKE_PICTURE_CODE);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQUEST_CODE);

                    setContentView(R.layout.imagelayout);
                    ImageView imageView = (ImageView)findViewById(R.id.image_view);
                    Bitmap cameraBitmap = (Bitmap)intent.getExtras().get("data");
                    imageView.setImageBitmap(cameraBitmap);
                    */

                    Toast.makeText(view.getContext(), "Take Player Capture ...", Toast.LENGTH_LONG).show();
                }
            });
        }

        /* Register To Database Change
        m_cChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s)
            {
                try {
                    CPlayerState cPlayerState = dataSnapshot.getValue(CPlayerState.class);
                }
                catch(Exception e) {

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
        m_cDatabaseCurrentPlayerStateReference.addChildEventListener(m_cChildEventListener);*/
    }

    private void openCameraIntent()
    {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE
        );
        if(pictureIntent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(pictureIntent, REQUEST_CAPTURE_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* Code Example
        https://github.com/sunpengkai1011/AutomaticCamera
        */

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap cCapturePhoto = (Bitmap) data.getExtras().get("data");
            if (m_cPlayerCaptureImageView != null) {
                m_cPlayerCaptureImageView.setImageBitmap(cCapturePhoto);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        //
        // GPS Permission
        //
        if (requestCode == GPS_CODE_PERMISSION)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                startGpsPermission();
            }
        }

        //
        // Camera Permission
        //
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            else
            {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    void startGpsPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

         if (m_cLocationManager != null)
            m_cLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_GPS_INTERVAL_IN_MILLISECONDS, 1.0f, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void BuildPlayerTypeString(int nPlayerType, int nPlayerID)
    {
        switch (nPlayerType)
        {
            case ActivityMain.PLAYER_TYPE_AS_IOS:
            {
                m_sPlayerType = "IOS (Central Station " + nPlayerID + ")";
                break;
            }
            case ActivityMain.PLAYER_TYPE_AS_INSTRACTOR:
            {
                m_sPlayerType = "Instractor " + nPlayerID;
                break;
            }
            case ActivityMain.PLAYER_TYPE_AS_TRAINEE:
            {
                m_sPlayerType = "Trainee " + nPlayerID;
                break;
            }
        }
    }

    public static void BuildPlayerHealthStateString(int eHealthState, int eHitType, int eHitLocation, int nAttackerHarnessID)
    {
        switch (eHealthState)
        {
            case ActivityMain.PLAYER_HEALTH_STATE_AS_ALIVE:
            {
                m_sPlayerHealthState = "Alive";
                break;
            }
            case ActivityMain.PLAYER_HEALTH_STATE_AS_INJURED:
            {
                switch (eHitLocation)
                {
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_FRONT:
                    {
                        m_sPlayerHealthState = "Injured In Front";
                        break;
                    }
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_BACK:
                    {
                        m_sPlayerHealthState = "Injured In Back";
                        break;
                    }
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_LEFT:
                    {
                        m_sPlayerHealthState = "Injured In Left";
                        break;
                    }
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_RIGHT:
                    {
                        m_sPlayerHealthState = "Injured In Right";
                        break;
                    }
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_HEAD:
                    {
                        m_sPlayerHealthState = "Injured In Head";
                        break;
                    }
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_LEFT_LEG:
                    {
                        m_sPlayerHealthState = "Injured In Left Leg";
                        break;
                    }
                    case ActivityMain.PLAYER_HIT_LOCATION_IN_RIGHT_LEG:
                    {
                        m_sPlayerHealthState = "Injured In Right Leg";
                        break;
                    }
                }
                break;
            }
            case ActivityMain.PLAYER_HEALTH_STATE_AS_KILLED:
            {
                m_sPlayerHealthState = "Killed";
                break;
            }
        }
    }

    /**********************************************************************************************/
    /*                           INTERNAL CLASS SERIAL PORT                                       */
    /**********************************************************************************************/
    public class CSimpleSerialCommPort extends BroadcastReceiver {
        // Data Members
        private UsbManager m_cUsbManger = null;
        private UsbDevice m_cUsbDevice = null;
        private UsbDeviceConnection m_сUsbDeviceConnection = null;
        private int m_nDeviceVID = 0;
        private int m_nDevicePID = 0;

        public CSimpleSerialCommPort()
        {
            // open the first usb device connected, excluding usb root hubs
            m_cUsbManger = (UsbManager) getSystemService(Context.USB_SERVICE);

            if (m_cTextView_GeneralLog != null)
                m_cTextView_GeneralLog.setText("No Connection With Vest");

            HashMap<String, UsbDevice> usbDevices = m_cUsbManger.getDeviceList();
            if (usbDevices.size() <= 0)
            {
                m_bGeneralError = true;
                m_sGeneralErrorString = "No Connection To USB Com Port";
            }

            try
            {
                if (!usbDevices.isEmpty()) {
                    boolean bContinueFindUsbCommPort = true;
                    for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
                    {
                        m_cUsbDevice = entry.getValue();

                        if (m_cTextView_GeneralLog != null)
                            m_cTextView_GeneralLog.setText("Try Connect To Vest Comm" + m_cUsbDevice.getDeviceName());

                        m_nDeviceVID = m_cUsbDevice.getVendorId();
                        m_nDevicePID = m_cUsbDevice.getProductId();

                        if (m_nDeviceVID != 0x1d6b || (m_nDevicePID != 0x0001 || m_nDevicePID != 0x0002 || m_nDevicePID != 0x0003)) {
                            // We are supposing here there is only one device connected and it is our serial device
                            bContinueFindUsbCommPort = false;

                            if (m_cTextView_GeneralLog != null) {
                                m_cTextView_GeneralLog.setText("Connected To USB, No CCU Data");
                            }

                            if (m_cTextView_VestCommStatus!= null) {
                                m_cTextView_VestCommStatus.setText("Connected To USB, No CCU Data");
                                m_cTextView_VestCommStatus.setTextColor((Color.RED));
                            }

                            try
                            {
                                PendingIntent permissionIntent = PendingIntent.getBroadcast(ActivityMain.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                                IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                                registerReceiver(this, filter);
                                m_cUsbManger.requestPermission(m_cUsbDevice, permissionIntent);

                            }
                            catch (Exception cc)
                            {
                                if (m_cTextView_GeneralLog != null)
                                    m_cTextView_GeneralLog.setText("Error From Vest " + cc.getMessage());
                            }
                        } else {
                            m_сUsbDeviceConnection = null;
                            m_cUsbDevice = null;
                        }

                        if (!bContinueFindUsbCommPort)
                            break;
                    }
                }
            }
            catch (Exception cc)
            {
                System.out.println(cc.getMessage());
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Toast.makeText(context, "Permision granted!", Toast.LENGTH_SHORT).show();
                            m_cSerialCommPort.startService();
                        }
                    } else {
                        Log.d("XXX", "permission denied for device " + device);
                        Toast.makeText(context, "Permision NOT granted", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
        // =============================================================================================
        //        Serial Comm Port => Try Connect & Send Message To CCU & Receive Message From CCU
        // =============================================================================================
        public void startService()
        {
            Thread cLocalThreadServiceCommPort = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    UsbSerialPort m_cSerialCommPort = null;

                    try
                    {
                        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(m_cUsbDevice);
                        m_сUsbDeviceConnection = m_cUsbManger.openDevice(m_cUsbDevice);
                        m_cSerialCommPort = driver.getPorts().get(0);
                        m_cSerialCommPort.open(m_сUsbDeviceConnection);
                        m_cSerialCommPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                        m_bConnectionWithCCU_Valid = true;

                        while (true)
                        {
                            try
                            {
                                // Update General Status
                                m_cHandler.post(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        m_cTextView_VestCommStatus.setText("Vest Status : Valid (Rx=" + m_cSerialCommPortReadGoodMessageFromCCUCounter + ":" + m_cSerialCommPortreadErrorMessageFromCCUCounter + ", Tx=" + m_cSerialCommPortSendGoodMessageToCCUCounter + ":" + m_cSerialCommPortSendErrorMessageToCCUCounter + ")");
                                        m_cTextView_VestCommStatus.setTextColor((Color.BLUE));

                                        if (CGlobalGpsState.IsValidGPS() == false)
                                        {
                                            m_cTextView_GPSStatus.setText("GPS Status : Not Valid");
                                            m_cTextView_GPSStatus.setTextColor((Color.RED));
                                            m_cTextView_GPSLat.setTextColor((Color.RED));
                                            m_cTextView_GPSLon.setTextColor((Color.RED));
                                            m_cTextView_GPSAlt.setTextColor((Color.RED));
                                            m_cTextView_GPSTime.setTextColor((Color.RED));

                                            m_cTextView_GPSLat.setTypeface(null, Typeface.BOLD);
                                            m_cTextView_GPSLon.setTypeface(null, Typeface.BOLD);
                                            m_cTextView_GPSAlt.setTypeface(null, Typeface.BOLD);
                                            m_cTextView_GPSTime.setTypeface(null, Typeface.BOLD);
                                        }
                                        else
                                        {
                                            m_cTextView_GPSStatus.setText("GPS Status : Valid");
                                            m_cTextView_GPSStatus.setTextColor((Color.BLACK));
                                            m_cTextView_GPSLat.setTextColor((Color.BLACK));
                                            m_cTextView_GPSLon.setTextColor((Color.BLACK));
                                            m_cTextView_GPSAlt.setTextColor((Color.BLACK));
                                            m_cTextView_GPSTime.setTextColor((Color.BLACK));

                                            m_cTextView_GPSLat.setTypeface(null,  Typeface.NORMAL);
                                            m_cTextView_GPSLon.setTypeface(null,  Typeface.NORMAL);
                                            m_cTextView_GPSAlt.setTypeface(null,  Typeface.NORMAL);
                                            m_cTextView_GPSTime.setTypeface(null, Typeface.NORMAL);
                                        }
                                    }
                                });

                                // Idel State - Wait Interval
                                Thread.sleep(200);

                                // Write Data To Serial Comm Port
                                CGlobalGpsState.UpdateSystemCounter(m_cSerialCommPortSendGoodMessageToCCUCounter);
                                final int nRealSendDataSize = m_cSerialCommPort.write(m_cMessageFromMobile.CreateMsgFrPhone(new Date(m_cLocationTime), m_cLocationLatitude, m_cLocationLongitude), 1000);
                                if (nRealSendDataSize == 49)
                                {
                                    Log.d("Debug", "Send Success Message To CCU ...");
                                    m_cSerialCommPortSendGoodMessageToCCUCounter++;
                                }
                                else
                                {
                                    Log.d("Error", "Error in write data to serial comm port ...");
                                    m_cSerialCommPortSendErrorMessageToCCUCounter++;
                                }

                                // Delay Wait Message From CCU
                                Thread.sleep(400);

                                // Read Data From Serial Comm Port
                                boolean bTryReadCCUMessage = true;
                                if (bTryReadCCUMessage == true)
                                {
                                    final int nRealReadDataSize = m_cSerialCommPort.read(m_byteSerialCommPortReadDataMinimumSlot, 800);
                                    if (nRealReadDataSize > 0)
                                    {
                                        for (int nNewNetBufferIndex = 0; nNewNetBufferIndex < nRealReadDataSize; nNewNetBufferIndex++)
                                        {
                                            int CrtChr = CMessageHandlerUtil.byteToUnsignedInt(m_byteSerialCommPortReadDataMinimumSlot[nNewNetBufferIndex]);
                                            if (m_nCCUMsgBufferHandlerIndex == 0 || m_nCCUMsgBufferHandlerIndex == 1)
                                            {
                                                if (CrtChr != 0xDD)
                                                {
                                                    m_cSerialCommPortreadErrorMessageFromCCUCounter++;
                                                    m_nCCUMsgBufferHandlerIndex = 0;
                                                    m_cSerialCommPort.purgeHwBuffers(true, false);
                                                    break;
                                                }
                                            }

                                            if (m_nCCUMsgBufferHandlerIndex == 2)
                                            {
                                                if (CrtChr != 'T')
                                                {
                                                    m_cSerialCommPortreadErrorMessageFromCCUCounter++;
                                                    m_nCCUMsgBufferHandlerIndex = 0;
                                                    m_cSerialCommPort.purgeHwBuffers(true, false);
                                                    break;
                                                }
                                            }

                                            if (m_nCCUMsgBufferHandlerIndex == 3)
                                            {
                                                if (CrtChr != 61) // Message Payload Size, Size Equal To 61,  [sizeof(S_MSG_TO_SMRTPHONE) - sizeof(S_MSG_STD_HEADER)]
                                                {
                                                    m_cSerialCommPortreadErrorMessageFromCCUCounter++;
                                                    System.out.println("\nMobile Received a wrong message from CCU!\n");
                                                    m_nCCUMsgBufferHandlerIndex = 0;
                                                    m_cSerialCommPort.purgeHwBuffers(true, false);
                                                    break;
                                                }
                                            }

                                            m_byteSerialCommPortReadDataBufferFromCCU[m_nCCUMsgBufferHandlerIndex] = m_byteSerialCommPortReadDataMinimumSlot[nNewNetBufferIndex];
                                            m_nCCUMsgBufferHandlerIndex++;

                                            if (m_nCCUMsgBufferHandlerIndex == 73) // Message Size = 73, sizeof(S_MSG_TO_SMRTPHONE))
                                            {
                                                m_nCCUMsgBufferHandlerIndex = 0;

                                                int nCrtChr1 = CMessageHandlerUtil.byteToUnsignedInt(m_byteSerialCommPortReadDataBufferFromCCU[0]);
                                                int nCrtChr2 = CMessageHandlerUtil.byteToUnsignedInt(m_byteSerialCommPortReadDataBufferFromCCU[1]);
                                                int nCrtChr3 = CMessageHandlerUtil.byteToUnsignedInt(m_byteSerialCommPortReadDataBufferFromCCU[3]);

                                                if ((nCrtChr1 == 0xDD) && (nCrtChr2 == 0xDD) && (nCrtChr3 == 61))
                                                {
                                                    // ==============================
                                                    // Receive New Valid CCU Message
                                                    // ==============================
                                                    m_cSerialCommPortReadGoodMessageFromCCUCounter++;

                                                    // =======================
                                                    // Get CCU Message Counter
                                                    // =======================
                                                    m_nMessageDataFromCCU_MsgCnt = CMessageHandlerUtil.GetU16(m_byteSerialCommPortReadDataBufferFromCCU, 4);

                                                    // =======================
                                                    // Get CCU Message Data
                                                    // =======================
                                                    int CrtOffset = 12;
                                                    m_nMessageDataFromCCU_MsgTypeID 		= CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,      CrtOffset); CrtOffset+=1; 	// 0 = Reply for Status update request, 1 = Event status – Hit, 2 = Event status – Fire, 3 = Periodic Update
                                                    m_nMessageDataFromCCU_ValidityBitMask   = CMessageHandlerUtil.GetU16(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset); CrtOffset+=2;   // Bit 0 - Location + Time Valid, Bit 1 - Last Hit Present, Bits 2-15 - Spare
                                                    m_nMessageDataFromCCU_HarnessID 		= CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=4;    // For demo own harness ID
                                                    m_nMessageDataFromCCU_ExerciseID 		= CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,      CrtOffset);CrtOffset+=1;    // TBD (For demo, keep ‘00’hex)
                                                    m_nMessageDataFromCCU_LocCoordY 		= CMessageHandlerUtil.GetU32(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=4;    // DDDMMSSsss. Own harness location GEOTime format. To be used if ValidityBitMask & 0x01.
                                                    m_dMessageDataFromCCU_LocCoordY = m_nMessageDataFromCCU_LocCoordY;
                                                    m_nMessageDataFromCCU_LocCoordX 		= CMessageHandlerUtil.GetU32(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=4;    // DDDMMSSsss. Own harness location GEOTime format. To be used if ValidityBitMask & 0x01.
                                                    m_dMessageDataFromCCU_LocCoordX = m_nMessageDataFromCCU_LocCoordX;
                                                    m_nMessageDataFromCCU_LocCoordZ 		= CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=4;    // Height from sea level. Own harness altitude m. To be used if ValidityBitMask & 0x01.
                                                    m_nMessageDataFromCCU_EventTime 		= CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=4;    // Seconds since Jan 1, 1970 (EPOCH time)	Seconds. To be used if ValidityBitMask & 0x01.
                                                    m_eMessageDataFromCCU_SenderType 		= CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,      CrtOffset);CrtOffset+=1;    // 1 = IOS, 2 = Field instructor, 3 = Trainee / Mobile device (Use for Demo)
                                                    m_eMessageDataFromCCU_RoleID 			= CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=4;    // TBD (For demo, keep ‘00000000’hex)
                                                    m_eMessageDataFromCCU_HealthState 	    = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,      CrtOffset);CrtOffset+=1;    // 1 = Health / Revive (Default value), 2 = Damage / Injured, 3 = Destroyed / Killed
                                                    m_nMessageDataFromCCU_ThresholdDistance = CMessageHandlerUtil.GetU16(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=2;    // TBD (For demo, keep ‘0000’hex)
                                                    m_nMessageDataFromCCU_ThresholdTime 	= CMessageHandlerUtil.GetU16(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=2;    // TBD (For demo, keep ‘0000’hex)
                                                    m_eMessageDataFromCCU_WeaponType 		= CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,      CrtOffset);CrtOffset+=1;    // Own harness weapon type
                                                    m_eMessageDataFromCCU_AmmoType 		    = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,      CrtOffset);CrtOffset+=1;    // Own harness munition type
                                                    m_nMessageDataFromCCU_CurrentAmmoVal 	= CMessageHandlerUtil.GetU16(m_byteSerialCommPortReadDataBufferFromCCU,     CrtOffset);CrtOffset+=2;    // Own harness current munition count
                                                    m_nMessageDataFromCCU_LastAttackerHarnessID = CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU, CrtOffset);CrtOffset+=4;    // HarnessID of the attacking entity received via laser – To be used in case of Hit event (MsgTypeID = 3)
                                                    m_nMessageDataFromCCU_LastHitMunitionTypeID = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // Munition type of the attacking entity received via laser – To be used in case of Hit event (MsgTypeID = 3)
                                                    m_eMessageDataFromCCU_HitType 		        = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // To be used in case of Hit event (MsgTypeID = 3). 1 = Miss (Default value), 2 = Near hit, 3 = Hit
                                                    m_nMessageDataFromCCU_LE2_BatteryPower      = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // Percent
                                                    m_nMessageDataFromCCU_LastAttackerLocX      = CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU, CrtOffset);CrtOffset+=4;    // Attacker harness location GEOTime format. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
                                                    m_nMessageDataFromCCU_LastAttackerLocY      = CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU, CrtOffset);CrtOffset+=4;    // Attacker harness location GEOTime format. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
                                                    m_nMessageDataFromCCU_LastAttackerLocZ      = CMessageHandlerUtil.GetS32(m_byteSerialCommPortReadDataBufferFromCCU, CrtOffset);CrtOffset+=4;    // Attacker harness location GEOTime format. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
                                                    m_eMessageDataFromCCU_HitLocation        	= CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // 1 = Front, 2 = Back, 3 = Left side, 4 = Right Sidem, 5 = Head, 6 = Left leg, 7 = Right leg. To be used in case of Hit event (MsgTypeID = 1) or ValidityBitMask & 0x02.
                                                    m_nMessageDataFromCCU_PU_BatteryPower       = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // Percent
                                                    m_nMessageDataFromCCU_HB_BatteryPower       = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // Percent
                                                    m_nMessageDataFromCCU_LE1_BatteryPower      = CMessageHandlerUtil.GetU8(m_byteSerialCommPortReadDataBufferFromCCU,  CrtOffset);CrtOffset+=1;    // Percent

                                                    BuildPlayerTypeString(m_eMessageDataFromCCU_SenderType, m_nMessageDataFromCCU_HarnessID);
                                                    BuildPlayerHealthStateString(m_eMessageDataFromCCU_HealthState, m_eMessageDataFromCCU_HitType, m_eMessageDataFromCCU_HitLocation, m_nMessageDataFromCCU_LastAttackerHarnessID);

                                                    m_cHandler.post(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            // =======================
                                                            // Update Log Status
                                                            // =======================
                                                            if (m_cTextView_GeneralLog != null) {
                                                                m_cTextView_GeneralLog.setText(" => Receive CCU Msg Counter : " + m_nMessageDataFromCCU_MsgCnt + "\n"
                                                                + "Latitude : " + m_dMessageDataFromCCU_LocCoordX + "\n"
                                                                + "Longitude : " + m_dMessageDataFromCCU_LocCoordY + "\n");
                                                            }

                                                            if (m_cTextView_CCULastMessageStatus != null)
                                                                m_cTextView_CCULastMessageStatus.setText(" Last CCU Msg Counter :  " + m_nMessageDataFromCCU_MsgCnt);
                                                            // =======================
                                                            // Update UI Status Fields
                                                            // =======================
                                                            m_cTextView_Exercise_ID.setText       ("Exercise ID          : " + m_nMessageDataFromCCU_ExerciseID);
                                                            m_cTextView_Player_ID.setText         ("Player ID            : " + m_sPlayerType);
                                                            m_cTextView_HealthStatus.setText      ("Health Status        : " + m_sPlayerHealthState);
                                                            m_cTextView_MainWeaponStatus.setText  ("Main Weapon Status   : M-16 Ammo 10/29");
                                                            m_cTextView_SlaveWeaponStatus.setText ("Slave Weapon Status  : Gun Ammo 8/12");
                                                            m_cTextView_BatteryStatus.setText     ("Battery Status       : "    + m_nMessageDataFromCCU_PU_BatteryPower);
                                                            m_cTextView_GPSLat.setText            ("GPS Latitude  : Mobile = " + CGlobalGpsState.m_sLatitude  + " (CCU = "  + m_dMessageDataFromCCU_LocCoordX + ")");
                                                            m_cTextView_GPSLon.setText            ("GPS Longitude : Mobile = " + CGlobalGpsState.m_sLongitude + " (CCU = "  + m_dMessageDataFromCCU_LocCoordY+ ")");
                                                            m_cTextView_GPSAlt.setText            ("GPS Altitude  : Mobile = " + CGlobalGpsState.m_sAltitude  + " (CCU = "  + m_nMessageDataFromCCU_LocCoordZ+ ")");
                                                            m_cTextView_GPSTime.setText           ("GPS Time      : Mobile = " + CGlobalGpsState.m_sTime      + " (CCU = "  + m_nMessageDataFromCCU_EventTime+ ")");

                                                            // =======================
                                                            // Set Health Status Color
                                                            // =======================
                                                            if (m_eMessageDataFromCCU_HealthState == PLAYER_HEALTH_STATE_AS_INJURED)
                                                            {
                                                                m_cTextView_HealthStatus.setTypeface(null, Typeface.BOLD);
                                                                m_cTextView_HealthStatus.setTextColor((Color.BLUE));
                                                            }
                                                            else if (m_eMessageDataFromCCU_HealthState == PLAYER_HEALTH_STATE_AS_KILLED)
                                                            {
                                                                m_cTextView_HealthStatus.setTypeface(null, Typeface.BOLD);
                                                                m_cTextView_HealthStatus.setTextColor((Color.RED));
                                                            }
                                                            else
                                                            {
                                                                m_cTextView_HealthStatus.setTypeface(null, Typeface.NORMAL);
                                                                m_cTextView_HealthStatus.setTextColor((Color.BLACK));
                                                            }

                                                            // =======================
                                                            // Set Battery State Color
                                                            // =======================
                                                            if (m_nMessageDataFromCCU_PU_BatteryPower >= 60)
                                                            {
                                                                m_cTextView_BatteryStatus.setTypeface(null, Typeface.BOLD);
                                                                m_cTextView_BatteryStatus.setTextColor((Color.BLUE));
                                                            }
                                                            else if (m_nMessageDataFromCCU_PU_BatteryPower >= 30)
                                                            {
                                                                m_cTextView_BatteryStatus.setTypeface(null, Typeface.BOLD);
                                                                m_cTextView_BatteryStatus.setTextColor((Color.RED));
                                                            }
                                                            else
                                                            {
                                                                m_cTextView_BatteryStatus.setTypeface(null, Typeface.BOLD);
                                                                m_cTextView_BatteryStatus.setTextColor((Color.RED));
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    final int nRealReadDataSize = m_cSerialCommPort.read(m_byteSerialCommPortReadDataBuffer, 100);
                                    if (nRealReadDataSize > 0) {
                                        m_cHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                m_cSerialCommPortReadGoodMessageFromCCUCounter++;
                                                m_cTextView_GeneralLog.setText("Read => " + nRealReadDataSize + " bytes from CCU");
                                                Log.d("Serial-Com ", "Read " + nRealReadDataSize + " bytes from CCU");
                                            }
                                        });
                                    }
                                }
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        try
                        {
                            if (m_cSerialCommPort != null)
                            {
                                m_cSerialCommPort.close();
                            }
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            });
            cLocalThreadServiceCommPort.start();
        }
    }
}


