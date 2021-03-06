package com.example.android.sunshine.app;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * {@link ForecastAdapter} exposes a list of weather forecasts 
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}. 
 */
public class ForecastAdapter extends CursorAdapter {

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    /**
     * Prepare the weather high/lows for presentation. 
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(high, isMetric) + "/" + Utility.formatTemperature(low, isMetric);
        return highLowStr;
    }

    @Override
    public int getItemViewType(int position) {
            return (position== 0) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /*
                This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
                string.
             */
    private String convertCursorRowToUXFormat(Cursor cursor) {
        // get row indices for our cursor 
       /* int idx_max_temp = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
        int idx_min_temp = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);
        int idx_date = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
        int idx_short_desc = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC);*/


        String highAndLow = formatHighLows(
               /* cursor.getDouble(idx_max_temp),
                cursor.getDouble(idx_min_temp));*/
        cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP));


        return Utility.formatDate(cursor.getLong(MainActivityFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(MainActivityFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }


    /* 
        Remember that these views are reused as needed. 
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType =getItemViewType(cursor.getPosition());
        int layoutId = -1;
        if (viewType == VIEW_TYPE_TODAY){
            layoutId = R.layout.list_item_forecast_today;
        }else if (viewType == VIEW_TYPE_FUTURE_DAY) {
            layoutId =R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }


    /* 
        This is where we fill-in the views with the contents of the cursor. 
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view 
        // we'll keep the UI functional with a simple (and slow!) binding. 


        /*TextView tv = (TextView)view;
        tv.setText(convertCursorRowToUXFormat(cursor));*/
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // Read weather icon ID from cursor
        int weatherId = cursor.getInt(MainActivityFragment.COL_WEATHER_ID);
        // Use placeholder image for now
        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(
                        cursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID)));
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(
                        cursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID)));
                break;
            }
        }

        // Read date from cursor
        long dateInMillis = cursor.getLong(MainActivityFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        // Read weather forecast from cursor
        String description = cursor.getString(MainActivityFragment.COL_WEATHER_DESC);
        // Find TextView and set weather forecast on it


        viewHolder.descriptionView.setText(description);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(context);

        // Read high temperature from cursor
        double high = cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context,high, isMetric));

        // Read low temperature from cursor
        double low = cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP);
        TextView lowView = (TextView) view.findViewById(R.id.list_item_low_textview);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context,low, isMetric));

    }


} 