// This file is part of Almond
//
// Copyright 2016-2017 The Board of Trustees of the Leland Stanford Junior University
//
// See COPYING for details
//
package edu.stanford.thingengine.engine.jsapi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import edu.stanford.thingengine.engine.service.EngineService;
import edu.stanford.thingengine.engine.ui.InteractionCallback;

/**
 * Created by gcampagn on 5/6/16.
 */
public class GpsAPI extends JavascriptAPI {
    public static final String LOG_TAG = "thingengine.Service";

    private final EngineService context;
    private final Handler handler;
    private final GpsLocationCallback callback;
    private final GoogleApiClient mGoogleApiClient;

    private class GpsLocationCallback extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult result) {
            reportLocation(result.getLastLocation());
        }
    }

    public GpsAPI(Handler handler, EngineService context) {
        super("Gps");
        this.handler = handler;
        this.context = context;
        this.callback = new GpsLocationCallback();
        this.mGoogleApiClient = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .setHandler(this.handler)
                .build();

        registerAsync("start", new GenericCall() {
            @Override
            public Object run(Object... args) throws Exception {
                start();
                return null;
            }
        });

        registerAsync("stop", new GenericCall() {
            @Override
            public Object run(Object... args) throws Exception {
                stop();
                return null;
            }
        });

        registerAsync("getCurrentLocation", new GenericCall() {
            @Override
            public Object run(Object... args) throws Exception {
                return getCurrentLocation();
            }
        });
    }

    private JSONObject makeLocation(@NonNull Location location) throws JSONException {
        JSONObject jsonLocation = new JSONObject();

        jsonLocation.put("latitude", location.getLatitude());
        jsonLocation.put("longitude", location.getLongitude());
        jsonLocation.put("altitude", location.getAltitude());
        jsonLocation.put("bearing", location.getBearing());
        jsonLocation.put("provider", location.getProvider());
        jsonLocation.put("speed", location.getSpeed());
        jsonLocation.put("time", location.getTime());

        return jsonLocation;
    }

    private void reportLocation(@Nullable Location location) {
        try {
            if (location != null) {
                invokeAsync("onlocationchanged", makeLocation(location));
            } else {
                invokeAsync("onlocationchanged", null);
            }
        } catch(JSONException e) {
            Log.i(LOG_TAG, "Failed to serialize Location", e);
        }
    }

    private LocationRequest requestLocationSync() throws InterruptedException {
        LocationRequest request = new LocationRequest();
        // don't request updates more frequently than every 30 seconds
        request.setInterval(30000);
        // rate limit ourselves because the server is ratelimited anyway
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        PendingResult<LocationSettingsResult> pendingResult =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
        LocationSettingsResult result = pendingResult.await();

        final Status status = result.getStatus();

        InteractionCallback callback = context.getInteractionCallback();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                break;

            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                if (callback != null) {
                    if (callback.resolveResult(status, InteractionCallback.ENABLE_GPS)) {
                        break;
                    }
                }
                /* fallback */

            default:
                throw new SecurityException("Location services are disabled by the user");
        }

        return request;
    }

    private void start() throws InterruptedException, IOException {
        ConnectionResult result = mGoogleApiClient.blockingConnect();
        if (!result.isSuccess())
            throw new IOException("Failed to connect to Google Play Services: " + result.getErrorMessage());

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
            requestPermission();

        LocationRequest request = requestLocationSync();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this.callback, this.handler.getLooper());
        reportLocation(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
    }

    private JSONObject getCurrentLocation() throws InterruptedException, IOException, JSONException {
        ConnectionResult result = mGoogleApiClient.blockingConnect();
        if (!result.isSuccess())
            throw new IOException("Failed to connect to Google Play Services: " + result.getErrorMessage());

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
            requestPermission();

        LocationRequest request = requestLocationSync();
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        return location != null ? makeLocation(location) : null;
    }

    private void requestPermission() throws InterruptedException {
        InteractionCallback callback = context.getInteractionCallback();
        if (callback == null)
            return;

        callback.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, InteractionCallback.REQUEST_GPS);
    }

    private void stop() {
        if (!mGoogleApiClient.isConnected())
            return;

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this.callback);
        mGoogleApiClient.disconnect();
    }
}
