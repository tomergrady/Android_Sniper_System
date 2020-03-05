package com.elbit.systems.sniper;

import java.util.ArrayList;

enum EventID {
    REVIVE,
    KILL,
    RELOAD,
    FIRE,
    ERROR
}
public class EventData {

    private EventID sEventID;
    private int nPlayerID; // the Player sent an event
    private int nOtherPlayerID; // second player relevant
    private String sTime;
    private ArrayList<String> vEventParams;

    public EventData() { }

    public EventData(EventID sEventID, int nPlayerID,  int nOtherPlayerID, String sTime) {
        this.sEventID = sEventID;
        this.nPlayerID = nPlayerID;
        this.nOtherPlayerID = nOtherPlayerID;
        this.sTime = sTime;
        this.vEventParams = new ArrayList<String>();
    }

    public EventData(EventID sEventID, int nPlayerID, ArrayList<String> vEventParams) {
        this.sEventID = sEventID;
        this.nPlayerID = nPlayerID;
        this.vEventParams = vEventParams;
    }


    public EventID getsEventID() {
        return sEventID;
    }

    public void setEventID(EventID sEventID) {
        this.sEventID = sEventID;
    }

    public ArrayList<String> getvEventParams() {
        return vEventParams;
    }

    public void setvEventParams(ArrayList<String> vEventParams) {
        this.vEventParams = vEventParams;
    }

    public int getPlayerID() {
        return nPlayerID;
    }

    public void setPlayerID(int nPlayerID) {
        this.nPlayerID = nPlayerID;
    }




    public int getnOtherPlayerID() {
        return nOtherPlayerID;
    }

    public void setOtherPlayerID(int nOtherPlayerID) {
        this.nOtherPlayerID = nOtherPlayerID;
    }

    public String getsTime() {
        return sTime;
    }

    public void setsTime(String sTime) {
        this.sTime = sTime;
    }
}
