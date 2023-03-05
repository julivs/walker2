package br.com.jvsdermatologia.walker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

public class Utils {

    private static Utils utils;

    private LocationManager locationManager;

    private  Location location;

    private LocationUser locationUser;

    private CallbackView linkToView;

    public CallbackView getLinkToView() {
        return linkToView;
    }

    public void setLinkToView(CallbackView linkToView) {
        this.linkToView = linkToView;
    }

    public static Utils getUtils() {
        if (utils != null) return utils;
        else {
            utils = new Utils();
            return utils;
        }
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public void setLocationManager(Context context) {
        if (locationManager == null)
            this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public Utils.listenerError setListenerGPS(LocationListener listenerGPS, int interval, int distance, Context context) {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return listenerError.noPermission;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, listenerGPS);
            return listenerError.ok;
        }
        return listenerError.noManager;
    }

    public void setLocationChanged(Location location) {
        this.location = location;
        if (this.locationUser != null) this.locationUser.changedLocation(location);
    }

    public Location getLocation() {
        return location;
    }

    public enum listenerError {
        ok,
        noPermission,
        noManager;
    }

    public LocationUser getLocationUser() {
        return locationUser;
    }

    public void setLocationUser(LocationUser locationUser) {
        this.locationUser = locationUser;
    }
}
