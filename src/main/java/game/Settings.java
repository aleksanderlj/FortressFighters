package game;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Settings {
    public static String name = "";
    public static boolean fixedTeams = false;
    public static boolean unevenTeams = false;
    public static String preferredTeam = "None";
    public static String ip = Server.getIP();

    public static void save(){
        Preferences pref = Preferences.userRoot().node("fortressfighters");
        pref.put("name", name);
        pref.putBoolean("fixedTeams", fixedTeams);
        pref.putBoolean("unevenTeams", unevenTeams);
        pref.put("preferredTeam", preferredTeam);
        pref.put("ip", ip);
        try {
            pref.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public static void load(){
        Preferences pref = Preferences.userRoot().node("fortressfighters");
        name = pref.get("name", name);
        fixedTeams = pref.getBoolean("fixedTeams", fixedTeams);
        unevenTeams = pref.getBoolean("unevenTeams", unevenTeams);
        preferredTeam = pref.get("preferredTeam", preferredTeam);
        ip = pref.get("ip", ip);
    }
}
