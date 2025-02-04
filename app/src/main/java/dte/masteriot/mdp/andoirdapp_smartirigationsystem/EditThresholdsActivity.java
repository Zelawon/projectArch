package dte.masteriot.mdp.andoirdapp_smartirigationsystem;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Activity for editing threshold values in a smart irrigation system.
 * This activity allows users to set and modify various environmental parameter thresholds
 * through a user interface and communicates these changes via MQTT protocol.
 */
public class EditThresholdsActivity extends AppCompatActivity {
    private static final String TAG = "HiveMQ";
    private static final String MQTT_HOST = "broker.hivemq.com";
    private static final int MQTT_Port = 1883;
    private static final String TOPIC_App_thresholds = "app/data/thresholds";
    private static final String TOPIC_App_ping = "app/data/ping";
    private ProgressDialog progressDialog;
    // Declare the views
    private EditText rainProbabilityMinInput, rainProbabilityMaxInput;
    private EditText temperatureMinInput, temperatureMaxInput;
    private EditText windSpeedMinInput, windSpeedMaxInput;
    private EditText humidityMinInput, humidityMaxInput;
    private EditText soilMoistureMinInput, soilMoistureMaxInput;
    private EditText pressureMinInput, pressureMaxInput;
    private EditText brightnessMinInput, brightnessMaxInput;

    private MaterialButton saveButton, cancelButton;

    private String rainMinValue = "", rainMaxValue = "";
    private String temperatureMinValue = "", temperatureMaxValue = "";
    private String windSpeedMinValue = "", windSpeedMaxValue = "";
    private String humidityMinValue = "", humidityMaxValue = "";
    private String soilMoistureMin = "", soilMoistureMax = "";
    private String pressureMin = "", pressureMax = "";
    private String brightnessMin = "", brightnessMax = "";

    // MQTT Client
    private Mqtt3AsyncClient mqttClient;

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
     * Upon successful connection, enables the save button and publishes a ping message.
     * Connection status is logged for debugging purposes.
     */
    private void connectToBroker() {
        mqttClient.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        // Handle connection failure
                        Log.e(TAG, "Connection failed", throwable);
                    } else {
                        // Successfully connected
                        Log.d(TAG, "Connected to HiveMQ broker");
                        runOnUiThread(() -> {
                            saveButton.setEnabled(true); // Enable Save button when connected
                        });
                        publishPing();
                    }
                });
    }

    /**
     * Publishes a ping message to verify MQTT connection.
     * Sends a JSON formatted ping message to the thresholds topic and subscribes to ping responses.
     */
    private void publishPing() {
        String message = "{\"ping\": \"ping\"}";
        // Publish the message to the MQTT topic
        mqttClient.publishWith()
                .topic(TOPIC_App_thresholds)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .send()
                .whenComplete((pubAck, throwable) -> {
                    runOnUiThread(() -> {
                        subscribeToTopic(TOPIC_App_ping);
                    });
                    if (throwable != null) {
                        Log.e(TAG, "Failed to publish message", throwable);
                        return;
                    }
                    Log.d(TAG, "Message published successfully");
                });
    }

    /**
     * Subscribes to a specified MQTT topic.
     * Sets up message handling callback for receiving messages on the subscribed topic.
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
                        processMessage(payload);
                    });
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Subscription failed for topic " + topic + ": " + throwable.getMessage());
                    } else {
                        System.out.println("Successfully subscribed to topic: " + topic);
                    }
                });
    }

    /**
     * Processes incoming MQTT messages containing threshold values.
     * Parses JSON message and updates UI input fields with received values.
     *
     * @param message JSON formatted string containing threshold values
     */
    private void processMessage(String message) {
        try {
            // Create a JSON object for the input values
            JSONObject inputValues = new JSONObject(message);

            // Set the input values from the JSON object into the EditText fields
            runOnUiThread(() -> {
                // Safely retrieve values from the input JSON and set them to the EditText fields
                rainProbabilityMinInput.setText(String.valueOf(inputValues.optInt("rainProbabilityMin", 0)));
                rainProbabilityMaxInput.setText(String.valueOf(inputValues.optInt("rainProbabilityMax", 0)));
                temperatureMinInput.setText(String.valueOf(inputValues.optInt("temperatureMin", 0)));
                temperatureMaxInput.setText(String.valueOf(inputValues.optInt("temperatureMax", 0)));
                windSpeedMinInput.setText(String.valueOf(inputValues.optInt("windSpeedMin", 0)));
                windSpeedMaxInput.setText(String.valueOf(inputValues.optInt("windSpeedMax", 0)));
                humidityMinInput.setText(String.valueOf(inputValues.optInt("humidityMin", 0)));
                humidityMaxInput.setText(String.valueOf(inputValues.optInt("humidityMax", 0)));
                soilMoistureMinInput.setText(String.valueOf(inputValues.optInt("soilMoistureMin", 0)));
                soilMoistureMaxInput.setText(String.valueOf(inputValues.optInt("soilMoistureMax", 0)));
                pressureMinInput.setText(String.valueOf(inputValues.optInt("pressureMin", 0)));
                pressureMaxInput.setText(String.valueOf(inputValues.optInt("pressureMax", 0)));
                brightnessMinInput.setText(String.valueOf(inputValues.optInt("brightnessMin", 0)));
                brightnessMaxInput.setText(String.valueOf(inputValues.optInt("brightnessMax", 0)));
                dismissLoadingDialog();
            });

            // Optionally log or handle the inputValues JSON
            Log.d("Input Values", inputValues.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Initializes the activity and sets up UI components.
     * Creates MQTT client, establishes broker connection, and sets up button click listeners.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_thresholds);

        showLoadingDialog();

        rainProbabilityMinInput = findViewById(R.id.rainProbabilityMinInput);
        rainProbabilityMaxInput = findViewById(R.id.rainProbabilityMaxInput);
        temperatureMinInput = findViewById(R.id.temperatureMinInput);
        temperatureMaxInput = findViewById(R.id.temperatureMaxInput);
        windSpeedMinInput = findViewById(R.id.windSpeedMinInput);
        windSpeedMaxInput = findViewById(R.id.windSpeedMaxInput);
        humidityMinInput = findViewById(R.id.humidityMinInput);
        humidityMaxInput = findViewById(R.id.humidityMaxInput);
        soilMoistureMinInput = findViewById(R.id.soilMoistureMinInput);
        soilMoistureMaxInput = findViewById(R.id.soilMoistureMaxInput);
        pressureMinInput = findViewById(R.id.pressureMinInput);
        pressureMaxInput = findViewById(R.id.pressureMaxInput);
        brightnessMinInput = findViewById(R.id.brightnessMinInput);
        brightnessMaxInput = findViewById(R.id.brightnessMaxInput);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        saveButton.setEnabled(false); // Disable Save button initially

        initializeViews();
        createMQTTClient();
        connectToBroker();

        // Set up the Save button click listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveValues();
            }
        });

        // Set up the Cancel button click listener
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Validates and saves the threshold values.
     * Collects input values, performs validation, and publishes updated thresholds via MQTT.
     * Displays appropriate error messages for invalid inputs.
     */
    private void saveValues() {
        try {
            // Retrieve values from the EditText fields
            rainMinValue = rainProbabilityMinInput.getText().toString();
            rainMaxValue = rainProbabilityMaxInput.getText().toString();
            temperatureMinValue = temperatureMinInput.getText().toString();
            temperatureMaxValue = temperatureMaxInput.getText().toString();
            windSpeedMinValue = windSpeedMinInput.getText().toString();
            windSpeedMaxValue = windSpeedMaxInput.getText().toString();
            humidityMinValue = humidityMinInput.getText().toString();
            humidityMaxValue = humidityMaxInput.getText().toString();
            soilMoistureMin = soilMoistureMinInput.getText().toString();
            soilMoistureMax = soilMoistureMaxInput.getText().toString();
            pressureMin = pressureMinInput.getText().toString();
            pressureMax = pressureMaxInput.getText().toString();
            brightnessMin = brightnessMinInput.getText().toString();
            brightnessMax = brightnessMaxInput.getText().toString();

            // Check if any value is empty or not a valid number
            if (rainMinValue.isEmpty() || rainMaxValue.isEmpty() ||
                    temperatureMinValue.isEmpty() || temperatureMaxValue.isEmpty() ||
                    windSpeedMinValue.isEmpty() || windSpeedMaxValue.isEmpty() ||
                    humidityMinValue.isEmpty() || humidityMaxValue.isEmpty() ||
                    soilMoistureMin.isEmpty() || soilMoistureMax.isEmpty() ||
                    pressureMin.isEmpty() || pressureMax.isEmpty() || brightnessMin.isEmpty() || brightnessMax.isEmpty()) {

                // Show a Toast message if any field is empty
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify that the values are valid numbers (e.g., no leading zeros like 036)
            if (!isValidNumber(rainMinValue) || !isValidNumber(rainMaxValue) ||
                    !isValidNumber(temperatureMinValue) || !isValidNumber(temperatureMaxValue) ||
                    !isValidNumber(windSpeedMinValue) || !isValidNumber(windSpeedMaxValue) ||
                    !isValidNumber(humidityMinValue) || !isValidNumber(humidityMaxValue) ||
                    !isValidNumber(soilMoistureMin) || !isValidNumber(soilMoistureMax) ||
                    !isValidNumber(pressureMin) || !isValidNumber(pressureMax) || !isValidNumber(brightnessMin) || !isValidNumber(brightnessMax)) {

                // Show a Toast message if any value is not a valid number
                Toast.makeText(this, "Please enter valid numeric values (no leading zeros)!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a JSON object with the updated values
            JSONObject message = new JSONObject();
            message.put("rainProbabilityMin", rainMinValue);
            message.put("rainProbabilityMax", rainMaxValue);
            message.put("temperatureMin", temperatureMinValue);
            message.put("temperatureMax", temperatureMaxValue);
            message.put("windSpeedMin", windSpeedMinValue);
            message.put("windSpeedMax", windSpeedMaxValue);
            message.put("humidityMin", humidityMinValue);
            message.put("humidityMax", humidityMaxValue);
            message.put("soilMoistureMin", soilMoistureMin);
            message.put("soilMoistureMax", soilMoistureMax);
            message.put("pressureMin", pressureMin);
            message.put("pressureMax", pressureMax);
            message.put("brightnessMin", brightnessMin);
            message.put("brightnessMax", brightnessMax);

            // Log the message before publishing
            Log.d(TAG, "Publishing message: " + message.toString());

            // Publish the message to the MQTT topic
            mqttClient.publishWith()
                    .topic(TOPIC_App_thresholds)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .payload(message.toString().getBytes(StandardCharsets.UTF_8))
                    .send()
                    .whenComplete((pubAck, throwable) -> {
                        if (throwable != null) {
                            Log.e(TAG, "Failed to publish message", throwable);
                            Toast.makeText(this, "Failed to publish message: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Message published successfully");
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Thresholds changes successfully!", Toast.LENGTH_SHORT).show();
                                // Add a log statement to confirm when finish() is called
                                Log.d(TAG, "Closing activity...");
                                finish(); // This should close the activity
                            });

                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error saving values", e);
        }
    }


    /**
     * Validates if a string represents a valid number without leading zeros.
     *
     * @param value The string to validate
     * @return boolean indicating if the string is a valid number
     */
    private boolean isValidNumber(String value) {
        // Check if the value is numeric and doesn't have leading zeros
        try {
            Integer.parseInt(value);
            return !value.startsWith("0") || value.equals("0");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Initializes all UI view components.
     * Binds layout elements to their respective view variables.
     */
    private void initializeViews() {
        rainProbabilityMinInput = findViewById(R.id.rainProbabilityMinInput);
        rainProbabilityMaxInput = findViewById(R.id.rainProbabilityMaxInput);
        temperatureMinInput = findViewById(R.id.temperatureMinInput);
        temperatureMaxInput = findViewById(R.id.temperatureMaxInput);
        windSpeedMinInput = findViewById(R.id.windSpeedMinInput);
        windSpeedMaxInput = findViewById(R.id.windSpeedMaxInput);
        humidityMinInput = findViewById(R.id.humidityMinInput);
        humidityMaxInput = findViewById(R.id.humidityMaxInput);
        soilMoistureMinInput = findViewById(R.id.soilMoistureMinInput);
        soilMoistureMaxInput = findViewById(R.id.soilMoistureMaxInput);
        pressureMinInput = findViewById(R.id.pressureMinInput);
        pressureMaxInput = findViewById(R.id.pressureMaxInput);
        brightnessMinInput = findViewById(R.id.brightnessMinInput);
        brightnessMaxInput = findViewById(R.id.brightnessMaxInput);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    /**
     * Displays a loading dialog while fetching initial values.
     * Shows an indeterminate progress dialog that cannot be canceled by the user.
     */
    private void showLoadingDialog() {
        // Create a new ProgressDialog instance
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false); // Make the dialog non-cancelable
        progressDialog.setIndeterminate(true); // Make it show an indeterminate progress bar
        progressDialog.show();
    }

    /**
     * Dismisses the loading dialog if it is currently showing.
     */
    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}