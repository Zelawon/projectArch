package dte.masteriot.mdp.andoirdapp_smartirigationsystem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ekn.gruzer.gaugelibrary.ArcGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.material.button.MaterialButton;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main activity for the Smart Irrigation System Android application.
 * Handles the primary user interface, MQTT communication for real-time monitoring,
 * and control of the irrigation system components including water pump, sensors,
 * and weather data visualization.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HiveMQ";
    private static final String MQTT_HOST = "broker.hivemq.com";
    private static final int MQTT_Port = 1883;
    private static final String TOPIC_Weather = "app/weather/telemetry";
    private static final String TOPIC_Sensors = "app/sensors/telemetry";
    private static final String TOPIC_Pump = "app/pump/telemetry";
    private static final String TOPIC_App_Data = "app/data/pump_control";
    private static final String TOPIC_App_Alarms = "yellow/app/alarm";

    private AlertDialog connectionDialog; // Global dialog instance

    private ArcGauge editWaterLevel;
    private TextView editPumpStatus, editRain, editWind, editTemperature, editHumidity, editPressure, editBrightness, editMoisture;
    private Button editChangeThresholdsButton, resetWaterLevelButton;
    private MaterialButton modeSwitch, pumpSwitch;
    private Mqtt3AsyncClient mqttClient; // MQTT Client
    private LabeledSwitch labeledSwitch;
    private ScheduledExecutorService scheduler;
    private boolean isSubscribed = false;

    /**
     * Creates an MQTT client instance with specified configuration.
     * Initializes an asynchronous MQTT v3 client with connection parameters for HiveMQ broker.
     */
    private void createMQTTClient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .serverHost(MQTT_HOST)
                .serverPort(MQTT_Port)
                .identifier("AndroidClient_" + System.currentTimeMillis())
                .buildAsync();
    }

    /**
     * Establishes connection to the MQTT broker.
     * Handles connection status, displays connection dialog on failure,
     * and initiates topic subscriptions on successful connection.
     */
    private void connectToBroker() {
        mqttClient.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        // Handle connection failure
                        Log.e(TAG, "Connection failed", throwable);
                        runOnUiThread(() -> {
                            // Show the dialog when connection fails
                            if (!connectionDialog.isShowing()) {
                                connectionDialog.show();
                            }
                        });
                    } else {
                        // Successfully connected
                        Log.d(TAG, "Connected to HiveMQ broker");
                        runOnUiThread(() -> {
                            // Dismiss the dialog once connected
                            if (connectionDialog.isShowing()) {
                                connectionDialog.dismiss();
                            }
                        });
                        subscribeToTopic(TOPIC_Weather);
                        subscribeToTopic(TOPIC_Sensors);
                        subscribeToTopic(TOPIC_Pump);
                    }
                });
    }

    /**
     * Subscribes to a specified MQTT topic and sets up message handling.
     * Routes incoming messages to appropriate processing methods based on topic.
     *
     * @param topic The MQTT topic to subscribe to
     */
    private void subscribeToTopic(String topic) {
        mqttClient.subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    System.out.println("Received message on topic " + topic + ": " + payload);
                    runOnUiThread(() -> {
                        if (topic.equals(TOPIC_Weather)) {
                            processWeatherMessage(payload);
                        } else if (topic.equals(TOPIC_Sensors)) {
                            processSensorMessage(payload);
                        } else if (topic.equals(TOPIC_Pump)) {
                            processPumpMessage(payload);
                        } else if (topic.equals(TOPIC_App_Alarms)) {
                            processAlarms(payload);
                        }
                    });
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Subscription failed for topic " + topic + ": " + throwable.getMessage());
                        runOnUiThread(() -> {
                            // Show the dialog when connection fails
                            if (!connectionDialog.isShowing()) {
                                connectionDialog.show();
                            }
                        });
                    } else {
                        System.out.println("Successfully subscribed to topic: " + topic);
                    }
                });
    }

    /**
     * Initializes activity components and sets up UI event handlers.
     * Configures MQTT client, establishes broker connection, and initializes
     * system controls and monitoring displays.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        editWaterLevel = findViewById(R.id.edit_waterlevel);
        editPumpStatus = findViewById(R.id.edit_pumpStatus);
        modeSwitch = findViewById(R.id.edit_modeSwitch);
        pumpSwitch = findViewById(R.id.edit_pumpSwitch);
        resetWaterLevelButton = findViewById(R.id.resetWaterLevelButton);
        editChangeThresholdsButton = findViewById(R.id.edit_changeThresholdsButton);
        editRain = findViewById(R.id.edit_rain);
        editWind = findViewById(R.id.edit_wind);
        editTemperature = findViewById(R.id.edit_temperature);
        editHumidity = findViewById(R.id.edit_humidity);
        editPressure = findViewById(R.id.edit_pressure);
        editBrightness = findViewById(R.id.edit_brightness);
        editMoisture = findViewById(R.id.edit_moisture);
        labeledSwitch = findViewById(R.id.switchNotifications);


        setInitialValues(); // Set initial values
        // Create and connect the MQTT client
        createMQTTClient();
        connectToBroker();

        // Toggle between Automatic/Manual modes
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (modeSwitch.getText().equals("Auto")) {
                    modeSwitch.setText("Manual");
                    modeSwitch.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.orange));
                    pumpSwitch.setEnabled(true);
                    pumpSwitch.setAlpha(1.0f);
                } else {
                    modeSwitch.setText("Auto");
                    modeSwitch.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue));
                    pumpSwitch.setEnabled(false);
                    pumpSwitch.setAlpha(0.5f);
                }
                publishTelemetryData(false); // Send updated values for both mode and pump state
            }
        });

        // Toggle between On/Off states for the pump switch
        pumpSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pumpSwitch.getText().equals("Off")) {
                    pumpSwitch.setText("On");
                    pumpSwitch.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                } else {
                    pumpSwitch.setText("Off");
                    pumpSwitch.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                }
                publishTelemetryData(false); // Send updated values for both mode and pump state
            }
        });

        resetWaterLevelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send "reset_water_level": "true"
                publishTelemetryData(true);

                // Delay 1 second, then send "reset_water_level": "false"
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        publishTelemetryData(false);
                    }
                }, 1000); // 1000ms = 1 second
            }
        });

        editChangeThresholdsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to open the new activity
                Intent intent = new Intent(MainActivity.this, EditThresholdsActivity.class);
                startActivity(intent);
            }
        });

        // Initialize the connection dialog
        connectionDialog = new AlertDialog.Builder(this)
                .setTitle("Connection Lost")
                .setMessage("The app is unable to connect to the MQTT broker. Please wait while we reconnect.")
                .setCancelable(false) // Prevent dismissal by tapping outside
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connectToBroker(); // Retry connection when clicked
                    }
                })
                .create();

        labeledSwitch.setOnToggledListener((view, isOn) -> {
            if (isOn) {
                // Start a scheduled task when the switch is ON
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleWithFixedDelay(() -> {
                    if (!isSubscribed) {
                        subscribeToTopic(TOPIC_App_Alarms);
                        isSubscribed = true;

                        // Unsubscribe after 1 second
                        scheduler.schedule(() -> {
                            unsubscribeFromTopic(TOPIC_App_Alarms);
                            isSubscribed = false;
                        }, 1, TimeUnit.SECONDS);
                    }
                }, 0, 3, TimeUnit.SECONDS);  // Initial delay of 0, then 3-second delay between tasks
            } else {
                // Stop subscribing when the switch is OFF
                if (scheduler != null && !scheduler.isShutdown()) {
                    scheduler.shutdownNow();
                }
                if (isSubscribed) {
                    unsubscribeFromTopic(TOPIC_App_Alarms);
                    isSubscribed = false;
                }
            }
        });
    }

    /**
     * Publishes control data to the MQTT broker.
     * Sends system mode, pump state, and water level reset commands.
     *
     * @param resetWaterLevel Boolean flag indicating whether to reset water level
     */
    private void publishTelemetryData(boolean resetWaterLevel) {
        String mode = modeSwitch.getText().toString().toLowerCase();
        String state = pumpSwitch.getText().toString().toLowerCase();
        String reset = resetWaterLevel ? "true" : "false"; // Send "true" only when reset is triggered

        String message = String.format("{\"manual_mode\": \"%s\", \"manual_state\": \"%s\", \"reset_water_level\": \"%s\"}", mode, state, reset);

        mqttClient.publishWith()
                .topic(TOPIC_App_Data)
                .qos(MqttQos.AT_LEAST_ONCE)  // Ensure QoS 1
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .send()
                .whenComplete((mqtt3Publish, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Publish failed", throwable);
                        runOnUiThread(() -> {
                            // Show the dialog when connection fails
                            if (!connectionDialog.isShowing()) {
                                connectionDialog.show();
                            }
                        });
                    } else {
                        Log.d(TAG, "Message published: " + message);
                    }
                });
    }

    /**
     * Sets initial values for all UI components.
     * Configures default states for displays, switches, and gauge ranges.
     */
    private void setInitialValues() {
        editPressure.setText("Waiting...");
        editWind.setText("Waiting...");
        editRain.setText("Waiting...");
        editTemperature.setText("Waiting...");
        editHumidity.setText("Waiting...");
        editMoisture.setText("Waiting...");
        editBrightness.setText("Waiting...");
        // Set initial state for mode switch (Automatic)
        modeSwitch.setText("Auto");
        modeSwitch.setBackgroundColor(ContextCompat.getColor(this, R.color.blue)); // Blue for Automatic
        // Set initial state for power switch (Off)
        pumpSwitch.setText("Off");
        pumpSwitch.setBackgroundColor(ContextCompat.getColor(this, R.color.red)); // Red for Off
        pumpSwitch.setEnabled(false); // Disable the pump switch initially in Automatic mode
        pumpSwitch.setAlpha(0.5f); // Reduce opacity to gray out
        // Water level Gauge
        Range humidityRange1 = new Range();
        humidityRange1.setColor(Color.parseColor("#03A9F4")); // Red color for range 1
        humidityRange1.setFrom(0);
        humidityRange1.setTo(100);
        editWaterLevel.addRange(humidityRange1);
        editWaterLevel.setMinValue(0);
        editWaterLevel.setMaxValue(100);
        editWaterLevel.setValue(0);

    }

    /**
     * Processes weather telemetry data received via MQTT.
     * Updates UI components with pressure, wind speed, and rain probability.
     *
     * @param message JSON formatted string containing weather data
     */
    private void processWeatherMessage(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            final String pressure = String.valueOf(jsonObject.getInt("pressure"));
            final String windSpeed = String.valueOf(jsonObject.getDouble("wind_speed"));
            final String rainProbability = String.valueOf(jsonObject.getInt("rain_probability"));

            runOnUiThread(() -> {
                editPressure.setText(pressure);
                editWind.setText(windSpeed);
                editRain.setText(rainProbability);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes sensor telemetry data received via MQTT.
     * Updates UI components with brightness, humidity, moisture, and temperature readings.
     *
     * @param message JSON formatted string containing sensor data
     */
    private void processSensorMessage(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            final String brightness = String.valueOf(jsonObject.getInt("brightness_sensor"));
            final String humidity = String.valueOf(jsonObject.getInt("humidity_sensor"));
            final String moisture = String.valueOf(jsonObject.getInt("moisture_sensor"));
            final String temperature = String.valueOf(jsonObject.getInt("temperature_sensor"));

            runOnUiThread(() -> {
                editBrightness.setText(brightness);
                editHumidity.setText(humidity);
                editMoisture.setText(moisture);
                editTemperature.setText(temperature);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes pump status data received via MQTT.
     * Updates UI components with pump state and water level information.
     *
     * @param message JSON formatted string containing pump status data
     */
    private void processPumpMessage(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            String state = jsonObject.getString("state");
            int waterLevel = jsonObject.getInt("waterLevel");
            Log.d(TAG, "Received message: " + message);

            runOnUiThread(() -> {
                editPumpStatus.setText(state);
                editWaterLevel.setValue(Double.parseDouble(String.valueOf(waterLevel)));
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes system alarm messages received via MQTT.
     * Displays alarm notifications using Toast messages.
     *
     * @param message JSON formatted string containing alarm data
     */
    private void processAlarms(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            String alarm = jsonObject.getString("alarm");
            runOnUiThread(() -> {
                Toast.makeText(this, alarm, Toast.LENGTH_SHORT).show();

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles activity pause state.
     * Disconnects MQTT client and disables notification switch.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mqttClient != null && mqttClient.getState().isConnected()) {
            mqttClient.disconnect();
            Log.d(TAG, "MQTT Disconnected onPause");
        }
        labeledSwitch.setOn(false);
    }

    /**
     * Handles activity resume state.
     * Reconnects MQTT client and resets notification switch.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mqttClient == null || !mqttClient.getState().isConnected()) {
            createMQTTClient();
            connectToBroker();
            Log.d(TAG, "MQTT Reconnected onResume");
        }
        labeledSwitch.setOn(false);
    }

    /**
     * Unsubscribes from a specified MQTT topic.
     * Handles unsubscription status and logs results.
     *
     * @param topic The MQTT topic to unsubscribe from
     */
    private void unsubscribeFromTopic(String topic) {
        mqttClient.unsubscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete((unsubAck, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Unsubscription failed for topic " + topic + ": " + throwable.getMessage());
                    } else {
                        System.out.println("Successfully unsubscribed from topic: " + topic);
                    }
                });
    }
}
