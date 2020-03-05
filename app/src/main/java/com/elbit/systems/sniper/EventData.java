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
    private int sPlayerID;
    private ArrayList<String> vEventParams;

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

    public EventData(EventID sEventID, int nPlayerID, ArrayList<String> vEventParams) {
        this.sEventID = sEventID;
        this.sPlayerID = sPlayerID;
        this.vEventParams = vEventParams;
    }
    public EventData()
    {
    }

    public int getsPlayerID() {
        return sPlayerID;
    }

    public void setsPlayerID(int sPlayerID) {
        this.sPlayerID = sPlayerID;
    }
}
