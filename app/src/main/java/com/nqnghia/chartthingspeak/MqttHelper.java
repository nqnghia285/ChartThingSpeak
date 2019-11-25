package com.nqnghia.chartthingspeak;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttHelper extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private int _Port;
    private String _Server;
    private String _User;
    private String _Password;
    private MqttAndroidClient _Client;

    private MqttHandler mqttHandler;
    private MqttSubscribe mqttSubscribe;

    public interface MqttHandler {
        public void handle(String topic, MqttMessage message);
    }

    public interface MqttSubscribe {
        public void setSubscribe(IMqttToken asyncActionToken);
    }

    /**
     * @param context
     * @param server
     * @param port
     * @param user
     * @param password
     *
     * @see #MqttHelper(Context, String, int, String, String)
     */
    public MqttHelper(Context context, String server, int port, String user, String password) {
        _Port = port;
        _Server = server;
        _User = user;
        _Password = password;

        String serverURL = "tcp://" + _Server + ":" + _Port;
        _Client = new MqttAndroidClient(context, serverURL, MqttClient.generateClientId());
    }

    /**
     * @param context
     * @param server
     * @param port
     *
     * @see #MqttHelper(Context, String, int)
     */
    public MqttHelper(Context context, String server, int port) {
        _Port = port;
        _Server = server;
        _User = "";
        _Password = "";

        String serverURL = "tcp://" + _Server + ":" + _Port;
        _Client = new MqttAndroidClient(context, serverURL, MqttClient.generateClientId());
    }

    public void connect() {
        if (_Client != null) {
            // Tao mot listener
            _Client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    Log.w(TAG, cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (mqttHandler != null) {
                        mqttHandler.handle(topic, message);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            // Khoi tao user, password
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setUserName(_User);
            options.setPassword(_Password.toCharArray());

            // Ket noi den broker
            try {
                _Client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Connection is success.");
                        if (mqttSubscribe != null) {
                            mqttSubscribe.setSubscribe(asyncActionToken);
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Connection is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    public void disconnect() {
        if (_Client != null) {
            try {
                _Client.disconnect(null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Disconnect is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Disconnect is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     * @param mqttHandler
     *
     * @see #setMqttHandler(MqttHandler)
     */
    public void setMqttHandler(MqttHandler mqttHandler) {
        this.mqttHandler = mqttHandler;
    }

    public void setMqttSubscribe(MqttSubscribe mqttSubscribe) {
        this.mqttSubscribe = mqttSubscribe;
    }

    /**
     * @param user
     *
     * @see #setUser(String)
     */
    public void setUser(String user) {
        _User = user;
    }

    /**
     * @param password
     *
     * @see #setPassword(String)
     */
    public void setPassword(String password) {
        _Password = password;
    }

    /**
     * @return
     *
     * @see #getServer()
     */
    public String getServer() {
        return _Server;
    }

    /**
     * @return
     *
     * @see #getPort()
     */
    public int getPort() {
        return _Port;
    }

    /**
     * @return
     *
     * @see #getUser()
     */
    public String getUser() {
        return _User;
    }

    /**
     * @return
     *
     * @see #getPassword()
     */
    public String getPassword() {
        return _Password;
    }

    /**
     * @return
     *
     * @see #isConnected()
     */
    public boolean isConnected() {
        return _Client.isConnected();
    }

    /**
     * @return
     *
     * @see #getClientId()
     */
    public String getClientId() {
        return _Client.getClientId();
    }

    /**
     * @param topic
     * @param qos
     *
     * @see #subscribe(String, int)
     */
    public void subscribe(String topic, int qos) {
        if (_Client != null) {
            try {
                _Client.subscribe(topic, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Subscribe is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Subscribe is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     * @param topic
     * @param qos
     *
     * @see #subscribe(String[], int[])
     */
    public void subscribe(String[] topic, int[] qos) {
        if (_Client != null) {
            try {
                _Client.subscribe(topic, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Subscribe is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Subscribe is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     * @param topic
     *
     * @see #unsubscribe(String)
     */
    public void unsubscribe(String topic) {
        if (_Client != null) {
            try {
                _Client.unsubscribe(topic, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Unsubscribe is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Unsubscribe is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     * @param topic
     *
     * @see #unsubscribe(String[])
     */
    public void unsubscribe(String[] topic) {
        if (_Client != null) {
            try {
                _Client.unsubscribe(topic, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Unsubscribe is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Unsubscribe is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     *
     * @param topic
     * @param message
     *
     * @see #publish(String, byte[])
     */
    public void publish(String topic, byte[] message) {
        if (_Client != null) {
            try {
                _Client.publish(topic, message, 0, true, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Publish is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "Publish is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     *
     * @param topic
     * @param message
     * @param qos
     * @param retained
     *
     * @see #publish(String, byte[], int, boolean)
     */
    public void publish(String topic, byte[] message, int qos, boolean retained) {
        if (_Client != null) {
            try {
                _Client.publish(topic, message, qos, retained, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Publish is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "Publish is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     *
     * @param topic
     * @param message
     * @param qos
     *
     * @see #publish(String, byte[], int)
     */
    public void publish(String topic, byte[] message, int qos) {
        if (_Client != null) {
            try {
                _Client.publish(topic, message, qos, true, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Publish is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "Publish is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }

    /**
     *
     * @param topic
     * @param message
     * @param retained
     *
     * @see MqttHelper#publish(String, byte[], boolean)
     */
    public void publish(String topic, byte[] message, boolean retained) {
        if (_Client != null) {
            try {
                _Client.publish(topic, message, 0, retained, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Publish is success.");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "Publish is failure.");
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Client is NULL.");
        }
    }
}
