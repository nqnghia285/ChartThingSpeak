package com.nqnghia.chartthingspeak;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    // Parameter ThingSpeak
    private static final String TAG = "UsingThingSpeakAPI";
    private static final String THINGSPEAK_CHANNEL_ID = "866855";
    private static final String THINGSPEAK_WRITE_API_KEY = "R8MCYSHH20RIBJWV";
    private static final String THINGSPEAK_READ_API_KEY = "QTRKZPHW65T7IHP2";

    private static final String THINGSPEAK_FEEDS = "/feeds.json?api_key=";
    private static final String THINGSPEAK_FIEDS = "/fields/1&2.json?api_key=";
    private static final String THINGSPEAK_FIELD1 = "&field1=";
    private static final String THINGSPEAK_FIELD2 = "&field2=";
    private static final String THINGSPEAK_RESULTS = "&results=";
    private static final String THINGSPEAK_INIT_RESULTS_SIZE = "75";
    private static final String THINGSPEAK_RESULTS_SIZE = "1";
    private static final String THINGSPEAK_UPDATE_URL = "https://api.thingspeak.com/update?api_key=";
    private static final String THINGSPEAK_CHANNEL_URL = "https://api.thingspeak.com/channels/";

    private static final int MAX_DATA_POINTS = 80;
    private static final int GET_TIME = 4000;

    private TextView textView;
    private GraphView graph;
    private BarGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;

    private int lastPointField1;
    private int lastPointField2;

    // Parameter CloudMQTT
    private static final String TOPIC1 = "Application_Channel";
    private static final String TOPIC2 = "Lights_Channel";
    private static final int QoS0 = 0;
    private static final int QoS2 = 2;
    private static final boolean retained = true;
    private static final int PORT = 15596;
    private static final String SERVER = "m11.cloudmqtt.com";
    private static final String USER = "wvtkpmil";
    private static final String PASSWORD = "PqCp38yh_Wnz";
    private static final String SENT_MESSAGE = "SENT";

    private MqttHelper mqttHelper;
    private Boolean FlagStarted;
    private Boolean Connected;

    private Random random;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();
        lastPointField1 = 0;
        lastPointField2 = 0;

        setupGraph();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlagStarted = false;
        mqttHelper = new MqttHelper(getApplicationContext(), SERVER, PORT, USER, PASSWORD);
        mqttHelper.connect();
        mqttHelper.setMqttHandler(new MqttHelper.MqttHandler() {
            @Override
            public void handle(String topic, final MqttMessage message) {
                if (FlagStarted) {
                    encoding(message);
                } else {
                    FlagStarted = true;
                }
            }
        });
        mqttHelper.setMqttSubscribe(new MqttHelper.MqttSubscribe() {
            @Override
            public void setSubscribe(IMqttToken asyncActionToken) {
                mqttHelper.subscribe(TOPIC2, 0);
            }
        });
    }

    private void encoding(MqttMessage m) {
        if(m.toString().equals("SENT")) {
            getDataFromThingSpeak();
        }
    }

    public void setupGraph() {
        textView = findViewById(R.id.textView);

        graph = findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScalable(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setTextColor(Color.rgb(0, 255, 255));
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graph.getLegendRenderer().setMargin(40);
        graph.getLegendRenderer().setPadding(10);
        graph.getViewport().setMaxY(80);
        graph.getViewport().setMinY(0);
        graph.setTitle("Field 1 & Field 2");
        graph.setTitleColor(Color.BLUE);
        graph.setBackgroundColor(Color.rgb(255, 165, 0));
        graph.getGridLabelRenderer().setHorizontalAxisTitle("X");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Y");
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLUE);
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.GREEN);

        series1 = new BarGraphSeries<>();
        series1.setTitle("Field 1");
        series1.setDataWidth(0.5);
        series1.setColor(Color.BLUE);
        graph.addSeries(series1);

        series2 = new LineGraphSeries<>();
        series2.setTitle("Field 2");
        series2.setDrawDataPoints(true);
        series2.setDataPointsRadius(10);
        series2.setThickness(1);
        series2.setColor(Color.GREEN);
        graph.addSeries(series2);

        setupChart();
    }

    private void setupChart() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(THINGSPEAK_CHANNEL_URL +
                THINGSPEAK_CHANNEL_ID +
                THINGSPEAK_FIEDS +
                THINGSPEAK_READ_API_KEY +
                THINGSPEAK_RESULTS +
                THINGSPEAK_INIT_RESULTS_SIZE).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d("Request to receive message", "Failure");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final String jsonString = response.body().string();
                Log.d("Response", jsonString);
                initJsonAnalyze(jsonString);
            }
        });
    }

    private void initJsonAnalyze(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            final JSONArray jsonArray = jsonObject.getJSONArray("feeds");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        textView.setTextColor(Color.rgb(
                                random.nextInt(255),
                                random.nextInt(255),
                                random.nextInt(255)));
                        if(Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field1")) == lastPointField1 &&
                                Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field2")) == lastPointField2) {
                            textView.setText("Data request failed.");
                        }
                        else {
                            for(int i = 0; i < jsonArray.length(); i++) {
                                series1.appendData(new DataPoint(i,
                                                Integer.parseInt(jsonArray.getJSONObject(i).getString("field1"))),
                                        true, MAX_DATA_POINTS);
                                series2.appendData(new DataPoint(i,
                                                Integer.parseInt(jsonArray.getJSONObject(i).getString("field2"))),
                                        true, MAX_DATA_POINTS);
                            }

                            textView.setText("Data request success.");
                        }

                        lastPointField1 = Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field1"));
                        lastPointField2 = Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field2"));
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getDataFromThingSpeak() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(THINGSPEAK_CHANNEL_URL +
                THINGSPEAK_CHANNEL_ID +
                THINGSPEAK_FIEDS +
                THINGSPEAK_READ_API_KEY +
                THINGSPEAK_RESULTS +
                THINGSPEAK_RESULTS_SIZE).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d("Request to receive message", "Failure");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final String jsonString = response.body().string();
                Log.d("Response", jsonString);
                jsonAnalyze(jsonString);
            }
        });
    }

    private void jsonAnalyze(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            final JSONArray jsonArray = jsonObject.getJSONArray("feeds");

            if (jsonArray.length() > 0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            textView.setTextColor(Color.rgb(
                                    random.nextInt(255),
                                    random.nextInt(255),
                                    random.nextInt(255)));
                            if(Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field1")) == lastPointField1 &&
                                    Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field2")) == lastPointField2) {
                                textView.setText("Data request failed.");
                            }
                            else {
                                series1.appendData(new DataPoint(series1.getHighestValueX() + 1,
                                                Integer.parseInt(jsonArray.getJSONObject(0).getString("field1"))),
                                        true, MAX_DATA_POINTS);
                                series2.appendData(new DataPoint(series2.getHighestValueX() + 1,
                                                Integer.parseInt(jsonArray.getJSONObject(0).getString("field2"))),
                                        true, MAX_DATA_POINTS);

                                textView.setText("Data request success.");
                            }

                            lastPointField1 = Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field1"));
                            lastPointField2 = Integer.parseInt(jsonArray.getJSONObject(jsonArray.length() - 1).getString("field2"));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
