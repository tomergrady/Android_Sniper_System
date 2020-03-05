package com.elbit.systems.sniper;

public class CGlobalGpsState
{
    public static double m_nLastGPSCounter    = 0;
    public static double m_nLastSystemCounter = 0;
    public static double m_nLatitude          = 0;
    public static double m_nLongitude         = 0;
    public static long m_nTime              = 0;
    public static double m_nAltitude          = 0;

    public static String m_sLatitude  = "##.####";
    public static String m_sLongitude = "##.####";
    public static String m_sAltitude  = "##.####";
    public static String m_sTime      = "##:##:##";

    public static void UpdateGPSCounter()
    {
        m_nLastGPSCounter = m_nLastSystemCounter;
    }
    public static void UpdateSystemCounter(int nSystemCounter)
    {
        m_nLastSystemCounter = nSystemCounter;
    }
    public static boolean IsValidGPS()
    {
        if ((m_nLatitude == 0) && (m_nLongitude == 0))
            return false;

        if( (m_nLastSystemCounter - m_nLastGPSCounter) > 10)
            return false;

        return true;
    }
}
