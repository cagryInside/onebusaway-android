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
import android.content.SharedPreferences;
import android.location.Location;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.onebusaway.android.R;
import org.onebusaway.android.app.Application;
import org.onebusaway.android.util.PreferenceHelp;

/**
 * Analytics class for tracking the app
 *
 * @author Cagri Cetin
 */

public class ObaAnalytics {

    private static final float LOCATION_ACCURACY_THRESHOLD = 0.1f;

    /**
     * To measure the distance when the bus stop tapped.
     */
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

    /**
     * Event categories for segmentation
     * app_settings, ui_action, submit is similar with OBA IOS
     */
    public enum ObaEventCategory {

        APP_SETTINGS("app_settings"), UI_ACTION("ui_action"),
        SUBMIT("`"), STOP_ACTION("stop_action_region[");

        private final String stringValue;

        private ObaEventCategory(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }
    }

    /**
     * Reports events with categories. Helps segmentation in GA admin console.
     *
     * @param category category name
     * @param action   action name
     * @param label    label name
     */
    public static void reportEventWithCategory(String category, String action, String label) {
        if (isAnalyticsActive()) {
            Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .build());
        }
    }

    /**
     * Tracks stop tap distance between bus stop location and users current location
     *
     * @param regionName   region name for category
     * @param stopId       for action
     * @param myLocation   the users location
     * @param stopLocation tapped stop location
     */
    public static void trackBusStopDistance(String regionName, String stopId, Location myLocation, Location stopLocation) {
        if (isAnalyticsActive()) {
            try {
                if (myLocation.getAccuracy() > LOCATION_ACCURACY_THRESHOLD) {

                    float distanceInMeters = myLocation.distanceTo(stopLocation);
                    ObaStopDistance stopDistance = null;

                    if (distanceInMeters < ObaStopDistance.DISTANCE_CLOSE) {
                        stopDistance = ObaStopDistance.CLOSE;
                    } else if (distanceInMeters < ObaStopDistance.DISTANCE_MEDIUM) {
                        stopDistance = ObaStopDistance.MEDIUM;
                    } else {
                        stopDistance = ObaStopDistance.FAR;
                    }

                    Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory(ObaEventCategory.STOP_ACTION.toString() + regionName + "]")
                            .setAction("Stop Id: " + stopId)
                            .setLabel("Search Distance: " + stopDistance.toString())
                            .setValue(1)
                            .build());
                }

            } catch (Exception e) {
                //If location comes null
                e.printStackTrace();
            }
        }
    }

    /**
     * For reporting activities on Start
     *
     * @param activity
     */
    public static void reportActivityStart(Activity activity) {
        if (isAnalyticsActive()) {
            GoogleAnalytics.getInstance(activity).reportActivityStart(activity);
        }
    }

    /**
     * For reporting activities on Stop
     *
     * @param activity
     */
    public static void reportActivityStop(Activity activity) {
        if (isAnalyticsActive()) {
            GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
        }
    }

    /**
     * For reporting fragments on Start
     *
     * @param fragment
     */
    public static void reportFragmentStart(Fragment fragment) {
        if (isAnalyticsActive()) {
            Tracker tracker = Application.get().getTracker(Application.TrackerName.APP_TRACKER);
            tracker.setScreenName(fragment.getClass().getSimpleName());
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    /**
     * @return is GA enabled or disabled from settings
     */
    private static Boolean isAnalyticsActive() {
        SharedPreferences settings = Application.getPrefs();
        return settings.getBoolean(Application.get().getString(R.string.preferences_key_analytics), Boolean.TRUE);
    }

}
