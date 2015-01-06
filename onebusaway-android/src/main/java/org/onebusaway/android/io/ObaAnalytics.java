/*
 * Copyright (C) 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.android.io;

import android.app.Activity;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.onebusaway.android.app.Application;

/**
 * Analytics class for tracking the app
 *
 * @author Cagri Cetin
 */

public class ObaAnalytics {

    public enum ObaStopDistance {

        CLOSE("CLOSE"), MEDIUM("MEDIUM"), FAR("FAR");

        private final String stringValue;

        //Distance in meters
        public static final int DISTANCE_CLOSE = 75;
        public static final int DISTANCE_MEDIUM = 200;

        private ObaStopDistance(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }
    }

    public enum ObaEventCategory {

        APP_SETTINGS("app_settings"), UI_ACTION("ui_action"), ACCESSIBILITY("accessibility"),
        SUBMIT("submit"), STOP_ACTION("stop_action");

        private final String stringValue;

        private ObaEventCategory(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }
    }

    public static void reportEventWithCategory(String category, String action, String label){
        Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    public static void trackBusStop(String stopName) {

        Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Bus Stop")
                .setAction("Search")
                .setLabel(stopName)
                .setValue(1)
                .build());
    }

    public static void trackBusStopByRegion(String region) {

        Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Bus Stop")
                .setAction("Search")
                .setLabel(region)
                .setValue(1)
                .build());
    }

    public static void trackBusStopDistance(Location myLocation, Location stopLocation) {
        float distanceInMeters = myLocation.distanceTo(stopLocation);
        ObaStopDistance stopDistance = null;

        if (distanceInMeters < ObaStopDistance.DISTANCE_CLOSE) {
            stopDistance = ObaStopDistance.CLOSE;
        } else if (distanceInMeters < ObaStopDistance.DISTANCE_MEDIUM) {
            stopDistance = ObaStopDistance.MEDIUM;
        } else {
            stopDistance = ObaStopDistance.FAR;
        }

        Log.v("stopDistance ----> ", stopDistance.toString());
        Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Bus Stop")
                .setAction("Search")
                .setLabel(stopDistance.toString())
                .setValue(1)
                .build());
    }

    /**
     * For reporting activities on Start
     *
     * @param activity
     */
    public static void reportActivityStart(Activity activity) {
        GoogleAnalytics.getInstance(activity).reportActivityStart(activity);
    }

    /**
     * For reporting activities on Stop
     *
     * @param activity
     */
    public static void reportActivityStop(Activity activity) {
        GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
    }

}
