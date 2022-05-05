package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private EditText editTextCity;
    private Button buttonCheckWeather;
    private TextView textViewCityName;
    private TextView textViewCityTime;
    private ImageView imageViewIconWeather;
    private TextView textViewTemp;
    private TextView TextViewTempFeels;
    private TextView textViewDesc;
    private TextView textViewHumidity;
    private TextView textViewPressure;
    private TextView textViewWind;

    // My API key from openweathermap.org
    private final String API_KEY = "54f059acd2f0721c92a650bf6944aa66";
    // URL request to openweathermap.org
    private final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=%s";
    // URL weather icon from openweathermap.org
    private final String WEATHER_ICON_URL = "https://openweathermap.org/img/wn/%s@4x.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextCity = findViewById(R.id.editTextCity);
        buttonCheckWeather = findViewById(R.id.buttonCheckWeather);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewCityTime = findViewById(R.id.textViewCityTime);
        imageViewIconWeather = findViewById(R.id.imageViewWetherIcon);
        imageViewIconWeather.setVisibility(View.INVISIBLE);
        textViewTemp = findViewById(R.id.textViewTemp);
        TextViewTempFeels = findViewById(R.id.textViewCityTempFeels);
        textViewDesc = findViewById(R.id.textViewDesc);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewPressure = findViewById(R.id.textViewPressure);
        textViewWind = findViewById(R.id.textViewWind);
    }

    public void onClickCheckTheWeather(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        String inputCity = editTextCity.getText().toString().trim();

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (manager != null) {
            networkInfo = manager.getActiveNetworkInfo();
        }
        if (!inputCity.isEmpty()) {
            if (networkInfo != null && networkInfo.isConnected()) {
                GetWeatherTask task = new GetWeatherTask();
                String url = String.format(WEATHER_URL, inputCity, API_KEY, Locale.getDefault().getLanguage());
                task.execute(url);
            }
            else {
                Toast.makeText(getApplicationContext(), R.string.error_connect, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetWeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            URL url = null;
            HttpsURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    result.append(line);
                    line = reader.readLine();
                }
                return result.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s == null) {
                Toast.makeText(getApplicationContext(), R.string.error_connect, Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(s);

                    String city = jsonObject.getString("name");
                    textViewCityName.setText(city);

                    long dt = Long.parseLong(jsonObject.getString("dt")) * 1000L;
                    long timezone = Long.parseLong(jsonObject.getString("timezone")) * 1000L;
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, dd MMM HH:mm");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT0"));
                    String time = sdf.format(new java.util.Date(dt + timezone));
                    textViewCityTime.setText(time);

                    JSONObject main = jsonObject.getJSONObject("main");

                    int temp = (int) Double.parseDouble(main.getString("temp"));
                    textViewTemp.setText(String.format(getString(R.string.textViewTemp), temp));

                    int tempFeelsLike = (int) Double.parseDouble(main.getString("feels_like"));
                    TextViewTempFeels.setText(String.format(getString(R.string.textViewTempFeels), tempFeelsLike));

                    String description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                    textViewDesc.setText(description.substring(0, 1).toUpperCase(Locale.ROOT) + description.substring(1));

                    String iconID = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
                    String iconURL = String.format(WEATHER_ICON_URL, iconID);
                    DownloadImageTask task = new DownloadImageTask();
                    Bitmap bitmap = null;
                    try {
                        bitmap = task.execute(iconURL).get();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    imageViewIconWeather.setImageBitmap(bitmap);
                    imageViewIconWeather.setVisibility(View.VISIBLE);

                    String humidity = main.getString("humidity");
                    textViewHumidity.setText(String.format(getString(R.string.textViewHumidity), humidity));

                    String pressure = main.getString("pressure");
                    textViewPressure.setText(String.format(getString(R.string.textViewPressure), pressure));

                    String windSpeed = jsonObject.getJSONObject("wind").getString("speed");
                    int windDeg = Integer.parseInt(jsonObject.getJSONObject("wind").getString("deg"));
                    String windDegStr = "";
                    if (windDeg >= 338 || windDeg < 23) windDegStr = getString(R.string.North);
                    else if (windDeg >= 23 && windDeg < 68)
                        windDegStr = getString(R.string.NorthEast);
                    else if (windDeg >= 68 && windDeg < 112) windDegStr = getString(R.string.East);
                    else if (windDeg >= 112 && windDeg < 157)
                        windDegStr = getString(R.string.SouthEast);
                    else if (windDeg >= 157 && windDeg < 202)
                        windDegStr = getString(R.string.South);
                    else if (windDeg >= 202 && windDeg < 247)
                        windDegStr = getString(R.string.SouthWest);
                    else if (windDeg >= 247 && windDeg < 292) windDegStr = getString(R.string.West);
                    else if (windDeg >= 292 && windDeg < 338)
                        windDegStr = getString(R.string.NorthWest);
                    textViewWind.setText(String.format(getString(R.string.textViewWind), windSpeed, windDegStr));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            URL url2 = null;
            HttpsURLConnection urlConnection2 = null;
            try {
                url2 = new URL(strings[0]);
                urlConnection2 = (HttpsURLConnection) url2.openConnection();
                InputStream inputStream = urlConnection2.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection2 != null) {
                    urlConnection2.disconnect();
                }
            }
            return null;
        }
    }

}