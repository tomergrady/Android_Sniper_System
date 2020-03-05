package com.elbit.systems.sniper;

import java.io.Serializable;

enum eHealthState {
    ALIVE,
    HITTED,
    KILLED,
}

enum enumerateForceID {
    BLUE,
    RED,
    NEUTRAL,
}

public class CPlayerState implements Serializable
{
    private double nPlayerLatitude;
    private double nPlayerLongitude;
    private double nPlayerAltitude;

    private String sExerciseID;
    private int nPlayerID; // harnes ID
    private String sPlayerID;
    private String sPlayerIMEI;
    private String sForceID;

    public enumerateForceID getEnumForceID() {
        return this.enumForceID;
    }

    public void setEnumForceID(enumerateForceID eForceID) {
        this.enumForceID = eForceID;
    }

    private enumerateForceID enumForceID;
    private String sPlayerLatitude;
    private String sPlayerLongitude;
    private String sPlayerAltitude;
    private String sMainWeapon;
    private String sMainWeaponBullets;
    private String sSlaveWeapon;
    private String sSlaveWeaponBullets;
    private String sTimestramp;
    private String sBattery; // main. headbend, main weapon laser, secondery wepon laser

    private eHealthState sPlayerState;
    // to be used in a case of Heath State = hitted
    private String sHealthArea; // 1- front, 2 back, 3 left side, 4 right side, 5 head. 6 left leg, 7 right leg
    private String sLastAttackerHarnessID;
    private String SLastHitMunitionTypeID;
    private String sHitType;

    public double getPlayerLatitude() {
        return nPlayerLatitude;
    }

    public void setPlayerLatitude(double nPlayerLatitude) {
        this.nPlayerLatitude = nPlayerLatitude;
    }

    public double getPlayerLongitude() {
        return nPlayerLongitude;
    }

    public void setPlayerLongitude(double nPlayerLongitude) {
        this.nPlayerLongitude = nPlayerLongitude;
    }

    public double getPlayerAltitude() {
        return nPlayerAltitude;
    }

    public void setPlayerAltitude(double nPlayerAltitude) {
        this.nPlayerAltitude = nPlayerAltitude;
    }

    public String getForceID() {
        return this.sForceID;
    }

    public void seteForceID(String forceID) {
        this.sForceID = forceID;
    }

    public CPlayerState()
    {
    }

    public String getPlayerIDStr() {
        return sPlayerID;
    }

    public void setPlayerIDStr(String sPlayerID) {
        this.sPlayerID = sPlayerID;
    }

    public CPlayerState(String sExerciseID, int nID, eHealthState sState, double nLongitude, double nLatitude, double nAltitude, String forceID)
    {
        this.sExerciseID = sExerciseID;
        nPlayerID = nID;
        sPlayerState = sState;
        nPlayerLongitude = nLongitude;
        nPlayerLatitude = nLatitude;
        nPlayerAltitude = nAltitude;
        this.sForceID = forceID;
        sPlayerID = String.valueOf(nPlayerID);
        if(forceID == "BLUE")
        {
            this.enumForceID = enumerateForceID.BLUE;
        }
        else {
            this.enumForceID = enumerateForceID.RED;
        }
    }

    public void setsExerciseID(String sExerciseID) {
        this.sExerciseID = sExerciseID;
    }

    public void setPlayerID(int playerID) {
        nPlayerID = playerID;
    }

    public void setsPlayerState(eHealthState sPlayerState) {
        this.sPlayerState = sPlayerState;
    }

    public void setsPlayerLongitude(String sPlayerLongitude) {
        this.sPlayerLongitude = sPlayerLongitude;
    }

    public void setsPlayerLatitude(String sPlayerLatitude) {  this.sPlayerLatitude = sPlayerLatitude;  }

    public void setsPlayerAltitude(String sPlayerAltitude) {
        this.sPlayerAltitude = sPlayerAltitude;
    }

    public void setsMainWeapon(String sMainWeapon) {
        this.sMainWeapon = sMainWeapon;
    }

    public void setsMainWeaponBullets(String sMainWeaponBullets) {  this.sMainWeaponBullets = sMainWeaponBullets;  }

    public void setsSlaveWeapon(String sSlaveWeapon) {
        this.sSlaveWeapon = sSlaveWeapon;
    }

    public void setsSlaveWeaponBullets(String sSlaveWeaponBullets) {  this.sSlaveWeaponBullets = sSlaveWeaponBullets;  }

    public void setsTimestramp(String sTimestramp) {
        this.sTimestramp = sTimestramp;
    }

    public String getsExerciseID() {
        return sExerciseID;
    }

    public int getPlayerID() {
        return nPlayerID;
    }

    public eHealthState getsPlayerState() {
        return sPlayerState;
    }

    public String getsPlayerLongitude() { return sPlayerLongitude;  }

    public String getsPlayerLatitude() {
        return sPlayerLatitude;
    }

    public String getsPlayerAltitude() {
        return sPlayerAltitude;
    }

    public String getsMainWeapon() {
        return sMainWeapon;
    }

    public String getsMainWeaponBullets() {
        return sMainWeaponBullets;
    }

    public String getsSlaveWeapon() {
        return sSlaveWeapon;
    }

    public String getsSlaveWeaponBullets() {
        return sSlaveWeaponBullets;
    }

    public String getsTimestramp() {
        return sTimestramp;
    }
}
