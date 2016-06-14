package com.example.android.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by praveena on 3/17/2016.
 */

public class FetchWeatherTask extends AsyncTask<String, Void,Void> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
   // private ArrayAdapter<String> mForecastAdapter;
    private final Context mContext;
    public FetchWeatherTask(Context context) {
        mContext = context;
     //   mForecastAdapter = forecastAdapter;
    }

    private boolean DEBUG = true;


    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db

        long locationId;
        Cursor locationCursor = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + "=?",
                new String[]{locationSetting},
                null);
        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();
            // Then add the data, along with the corresponding name of the data typ
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            // Finally, insert location data into the database.
            Uri insertedUri = mContext.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // If it exists, return the current ID
            locationId = ContentUris.parseId(insertedUri);
            // Otherwise, insert it using the content resolver and the base URI


        }
        locationCursor.close();
        return locationId;
    }

    private void getWeatherDataFromJson(String forecastStr,String locationSetting)
            throws JSONException {
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE ="temp";
        final String OWM_MIN = "min";
        final String OWM_MAX = "max";
        final String OWM_DESCRIPTION ="main";
        final String OWM_WEATHER_ID = "id";

        try {

        JSONObject forecastJson = new JSONObject(forecastStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        Time dayTime = new Time();
        dayTime.setToNow();
        int julianStartDate = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        dayTime = new Time();
      //  String[] resultStr = new String[numDays];
        for(int i = 0; i <weatherArray.length(); i++){
            String day;
            String description;
            String highAndLow;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;
            int weatherId;

            JSONObject dayForecast = weatherArray.getJSONObject(i);
            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // Description is in a child array called "weather", which is 1 element long.
            // That element also contains a weather code.
            JSONObject weatherObject =
                    dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);
            // The date/time is returned as a long. Convert it in human readable format
            long dateTime;
            dateTime = dayTime.setJulianDay(julianStartDate + i);
        //    day = getReadableDateString(dateTime);


            JSONObject tempObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = tempObject.getDouble(OWM_MAX);
            double low = tempObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);

            //highAndLow = formatHighLows(high,low);
            //resultStr[i] = day + " - " + description +" - " + highAndLow;

        }
            int inserted = 0;
            if ( cVVector.size() > 0 ) {
            ContentValues[] cvArray =new  ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
               inserted = mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            }

            Log.d(LOG_TAG, "FetchWeatherTask Complete. " + inserted + " Inserted");



        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

    }

    @Override
    protected Void doInBackground(String... params) {

        if (params.length == 0) {
            return null;
        }
        String locationQuery = params[0];

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null ;
        String forecastJsonStr = null;
        int i = 7;
        try {
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARM = "q";
            final String UNITS ="units";
            final String COUNT ="cnt";
            final String APPID = "APPID";
            Uri builturi = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARM,params[0])
                    .appendQueryParameter(UNITS,mContext.getString(R.string.metric))
                    .appendQueryParameter(COUNT,Integer.toString(i))
                    .appendQueryParameter(APPID,mContext.getString(R.string.appkey)).build();
            URL url = new URL(builturi.toString());
            Log.v(LOG_TAG,"Built URL" +builturi.toString());
            //URL url = new URL ("http://api.openweathermap.org/data/2.5/forecast/daily?q=07307&units=metric&cnt=7&APPID=f0028fa5696d2c211c0679c9c85f93e1");
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            //Read the input stream into a string
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if(inputStream == null){
                forecastJsonStr = null;

            }
            reader =    new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                forecastJsonStr = null;
            }
            forecastJsonStr = buffer.toString();
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        }catch (IOException e){
            Log.e("MainActivityFragment","Unable to open the URL",e);
            forecastJsonStr = null;
        } catch (JSONException e) {
             Log.e(LOG_TAG, e.getMessage(), e);
             e.printStackTrace();
        }finally {
            if (urlConnection == null){
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("MainActivityFragment", "Error closing stream", e);
                }
            }
        }

        return null;
    }




}
