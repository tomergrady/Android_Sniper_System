package com.elbit.systems.sniper;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;

public class CFirebaseUtil
{
    // Singelton Interface
    public static CFirebaseUtil           m_cFirebaseUtil      = null;
    public static FirebaseDatabase        m_cFirebaseDatabase  = null;

    // Global Firebase Data Members For Current Player
    public static DatabaseReference       m_cDatabaseCurrentPlayerReference = null;
    public static ArrayList<CPlayerState> m_arrPlayerCurrentState           = null;

    // Global Firebase Data Members For History Player
    public static DatabaseReference       m_cDatabaseHistoryPlayerReference = null;
    public static ArrayList<CPlayerState> m_arrPlayerHistoryState           = null;

    public static DatabaseReference        m_cDataEventsRef                        = null;
    private CFirebaseUtil()
    {}

    public static void openFirebaseReferenceByPlayerCurrentState(String sReference)
    {
        if (m_cFirebaseUtil == null)
            m_cFirebaseUtil = new CFirebaseUtil();

        if (m_cFirebaseDatabase == null)
            m_cFirebaseDatabase = FirebaseDatabase.getInstance();

        if (m_cDatabaseCurrentPlayerReference == null)
            m_cDatabaseCurrentPlayerReference = m_cFirebaseDatabase.getReference().child(sReference);

        if (m_arrPlayerCurrentState == null)
            m_arrPlayerCurrentState = new ArrayList<CPlayerState>();
    }

    public static CFirebaseUtil getFirebaseUtil() {
        if(null == m_cFirebaseUtil)
            m_cFirebaseUtil = new CFirebaseUtil();
        return m_cFirebaseUtil;
    }

    public static void openFirebaseReferenceByPlayerHistoryState(String sReference)
    {
        if (m_cFirebaseUtil == null)
            m_cFirebaseUtil = new CFirebaseUtil();

        if (m_cFirebaseDatabase == null)
            m_cFirebaseDatabase = FirebaseDatabase.getInstance();

        if (m_cDatabaseHistoryPlayerReference == null)
            m_cDatabaseHistoryPlayerReference = m_cFirebaseDatabase.getReference().child(sReference);

        if (m_arrPlayerHistoryState == null)
            m_arrPlayerHistoryState = new ArrayList<CPlayerState>();
    }

    public static void openFirebaseReferenceByEvent(String sReference)
    {
        if (m_cFirebaseUtil == null)
            m_cFirebaseUtil = new CFirebaseUtil();

        if (m_cFirebaseDatabase == null)
            m_cFirebaseDatabase = FirebaseDatabase.getInstance();

        if (m_cDataEventsRef == null)
            m_cDataEventsRef = m_cFirebaseDatabase.getReference().child(sReference);
    }

    public static void UpdatePlayerState(CPlayerState cPlayerData)
    {
        m_cDatabaseCurrentPlayerReference.child(cPlayerData.getPlayerIDStr()).setValue(cPlayerData);
        m_cDatabaseHistoryPlayerReference.push().setValue(cPlayerData);
    }
    public void RemovePlayer(CPlayerState cPlayerData)
    {
        m_cDatabaseCurrentPlayerReference.child(cPlayerData.getPlayerIDStr()).removeValue();
    }


    public void SendEvent(EventID eEventID, int sPlayerID, String sPlayerFrom)
    {
        ArrayList<String> params = new ArrayList<String>();
        String sTime = "time";
        params.add(sTime);
        params.add(sPlayerFrom);

        EventData cEventData = new EventData(eEventID, sPlayerID, params);
        m_cDataEventsRef.push().setValue(cEventData);
    }
    public void SendFireEvent(int nPlayerID, String sLat, String sLong, String sAlt, String weaponType)
    {
        EventData cFireData = new EventData();
        cFireData.setsPlayerID(nPlayerID);
        cFireData.setEventID(EventID.FIRE);
        String sTime = "time";
        ArrayList<String> stringArrayList = new ArrayList<String>(Arrays.asList(sTime, sLat, sLong, sAlt, weaponType));
     //   m_cDataFireRef.push().setValue(cFireData);
    }

}
