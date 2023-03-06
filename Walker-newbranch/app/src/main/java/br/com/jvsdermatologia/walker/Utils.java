package br.com.jvsdermatologia.walker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static Utils utils;

    private LocationManager locationManager;

    private Location location;

    private List<Location> locationList = new ArrayList<>();

    private LocationUser locationUser;

    private CallbackView linkToView;

    private Listener listener = new Listener();

    private String weightVar = "peso";

    private String heightVar = "altura";

    private String soundAlertVar = "soundAlert";

    private String gpsSensibVar = "sensibilidadeGPS";

    private String turnOffDistVar = "turnoffDistance";

    private String turnOffActiveVar = "turnoffDistanceActive";

    private Boolean makeAlert;

    private Integer weight;

    private Integer height;

    private Integer distanceTurnoff;

    private Boolean turnoffDistActive;

    private Boolean highSensibGPS;

    public static int HIGH_SENSIB_GPS = 15;
    public static int LOW_SENSIB_GPS = 50;

    public String getGpsSensibVar() {
        return gpsSensibVar;
    }

    public Boolean getHighSensibGPS() {
        return highSensibGPS;
    }

    public void setHighSensibGPS(Boolean highSensibGPS) {
        this.highSensibGPS = highSensibGPS;
    }

    public String getSoundAlertVar() {
        return soundAlertVar;
    }

    public String getTurnOffDistVar() {
        return turnOffDistVar;
    }

    public String getTurnOffActiveVar() {
        return turnOffActiveVar;
    }

    public Boolean getMakeAlert() {
        return makeAlert;
    }

    public void setMakeAlert(Boolean makeAlert) {
        this.makeAlert = makeAlert;
    }


    public Integer getDistanceTurnoff() {
        return distanceTurnoff;
    }

    public void setDistanceTurnoff(Integer distanceTurnoff) {
        this.distanceTurnoff = distanceTurnoff;
    }

    public Boolean getTurnoffDistActive() {
        return turnoffDistActive;
    }

    public void setTurnoffDistActive(Boolean turnoffDistActive) {
        this.turnoffDistActive = turnoffDistActive;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getWeightVar() {
        return weightVar;
    }

    public String getHeightVar() {
        return heightVar;
    }

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

    public List<Location> getLocationList() {
        return locationList;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public void setLocationManager(Context context) {
        if (locationManager == null)
            this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public Utils.listenerError setListenerGPS(int interval, int distance, Context context) {

        if (locationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return listenerError.noPermission;
                }
            } else {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return listenerError.noPermission;
                }
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, listener);
            return listenerError.ok;
        }
        return listenerError.noManager;
    }

    public void changeListenerGPS(int newInterval, int newDistance, Context context) {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(listener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, newInterval, newDistance, listener);
        }
    }

    public void locationChangedAction(Location location) {
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

    public String getOneLineStringDataFile(Context context, String fileName) {

        fileName = fileName + ".txt";

        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String result = bufferedReader.readLine();
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else return null;
    }

    public boolean updateOneLineStringDataFile(Context context, String fileName, String newValue) {

        fileName = fileName + ".txt";

        File file = new File(context.getFilesDir(), fileName);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            bufferedWriter.write(newValue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveTrack(Context context, String fileName, String newValue) {

        fileName = fileName + ".trk";

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            bufferedWriter.write(newValue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String readTrack(Context context, String fileName) {

        fileName = fileName + ".trk";

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else return null;
    }

    public boolean deleteFile(Context context, String fileName) {
        fileName = fileName + ".trk";
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        if (file.exists()) {
            if (file.delete()) return true;
            else return false;
        }
        return false;
    }

    public List<File> listFiles(File dir) {

        List<File> filesList = new ArrayList<>();
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file1 : files) {
                    if (file1.isFile()) {
                        filesList.add(file1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filesList;
    }

    public static class CircBuffer {
        private int size;
        private int position;
        private Double[] values;
        private Double sum = 0.0;

        public CircBuffer(int size) {
            this.size = size;
            this.values = new Double[size];
            this.position = 0;
        }

        public void insertValue(Double aDouble) {
            if (values[position] != null) sum -= values[position];
            sum += aDouble;
            values[position] = aDouble;
            position++;
            if (position == values.length) position = 0;
        }

        public Double sumAll() {
            Double total = 0.0;
            for (Double value : values) {
                if (value != null) {
                    total += value;
                } else return null;
            }
            return total;
        }

        public Double getSum() {
            return sum;
        }

        public Double sumLast(int number) {
            number = Math.min(number, size);
            int count = 0;
            int sumPosition = position;
            Double sum = 0.0;
            while (count < number) {
                if (values[sumPosition] != null) sum += values[sumPosition];
                sumPosition--;
                if (sumPosition < 0) sumPosition = size - 1;
                count++;
            }

            return sum;
        }
    }

    class Listener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Utils.getUtils().locationChangedAction(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            // System.out.println("---... gps disabled");
        }

    }

    public Track makeTrack(long end, double distance, double time, List<Location> Locations) {

        List<Double[]> points = new ArrayList<>();
        for (Location location : Locations) {
            points.add(new Double[]{location.getLatitude(), location.getLongitude()});
        }

        return new Track(end, distance, time, points);
    }

}
