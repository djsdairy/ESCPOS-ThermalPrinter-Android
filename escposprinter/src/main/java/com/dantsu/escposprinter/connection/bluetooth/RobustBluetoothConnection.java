package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class RobustBluetoothConnection extends DeviceConnection {

    private static final String TAG = "RobustBTConnection";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    private BluetoothDevice device;
    private BluetoothSocket socket = null;
    private int retryCount = 3;
    private int connectionTimeout = 10000; // 10 seconds
    private boolean useAlternativeMethod = false;
    
    /**
     * Create an instance of RobustBluetoothConnection.
     *
     * @param device an instance of BluetoothDevice
     */
    public RobustBluetoothConnection(BluetoothDevice device) {
        super();
        this.device = device;
    }

    /**
     * Create an instance of RobustBluetoothConnection with custom parameters.
     *
     * @param device an instance of BluetoothDevice
     * @param retryCount number of connection retries
     * @param connectionTimeout connection timeout in milliseconds
     */
    public RobustBluetoothConnection(BluetoothDevice device, int retryCount, int connectionTimeout) {
        super();
        this.device = device;
        this.retryCount = retryCount;
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get the instance BluetoothDevice connected.
     *
     * @return an instance of BluetoothDevice
     */
    public BluetoothDevice getDevice() {
        return this.device;
    }

    /**
     * Check if OutputStream is open.
     *
     * @return true if is connected
     */
    @Override
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected() && super.isConnected();
    }

    /**
     * Start socket connection with the bluetooth device with retry mechanism.
     */
    @SuppressLint("MissingPermission")
    public RobustBluetoothConnection connect() throws EscPosConnectionException {
        if (this.isConnected()) {
            return this;
        }

        if (this.device == null) {
            throw new EscPosConnectionException("Bluetooth device is not connected.");
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Try to connect with retries
        EscPosConnectionException lastException = null;
        for (int attempt = 0; attempt <= this.retryCount; attempt++) {
            try {
                Log.d(TAG, "Connection attempt " + (attempt + 1) + " of " + (this.retryCount + 1));
                
                // Cancel discovery to prevent slow connection
                bluetoothAdapter.cancelDiscovery();
                
                // Get the appropriate UUID
                UUID uuid = this.getDeviceUUID();
                
                // Try standard connection method first
                if (!useAlternativeMethod) {
                    try {
                        Log.d(TAG, "Trying standard connection method");
                        this.socket = this.device.createRfcommSocketToServiceRecord(uuid);
                        this.socket.connect();
                    } catch (IOException e) {
                        Log.w(TAG, "Standard connection method failed, trying alternative method");
                        useAlternativeMethod = true;
                        throw e; // Rethrow to be caught by the outer try-catch
                    }
                }
                
                // If standard method failed, try alternative method using reflection
                if (useAlternativeMethod) {
                    Log.d(TAG, "Using alternative connection method");
                    this.socket = this.createFallbackSocket();
                    this.socket.connect();
                }
                
                // If we get here, connection was successful
                this.outputStream = this.socket.getOutputStream();
                this.data = new byte[0];
                Log.d(TAG, "Connection successful");
                return this;
                
            } catch (IOException e) {
                Log.e(TAG, "Connection attempt " + (attempt + 1) + " failed: " + e.getMessage());
                this.disconnect();
                lastException = new EscPosConnectionException("Unable to connect to bluetooth device: " + e.getMessage());
                
                // If not the last attempt, wait before retrying
                if (attempt < this.retryCount) {
                    try {
                        Thread.sleep(1000); // Wait 1 second before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        // If we get here, all connection attempts failed
        if (lastException != null) {
            throw lastException;
        } else {
            throw new EscPosConnectionException("Unable to connect to bluetooth device after " + (this.retryCount + 1) + " attempts.");
        }
    }

    /**
     * Create a fallback socket using reflection for problematic devices
     */
    @SuppressLint("MissingPermission")
    private BluetoothSocket createFallbackSocket() throws IOException {
        try {
            Log.d(TAG, "Creating fallback socket for device: " + device.getName());
            
            // Different approaches based on Android version
            if (Build.VERSION.SDK_INT >= 10) {
                try {
                    // Try using the hidden createInsecureRfcommSocket method
                    Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
                    return (BluetoothSocket) m.invoke(device, 1);
                } catch (Exception e) {
                    Log.w(TAG, "Fallback method 1 failed: " + e.getMessage());
                }
            }
            
            // Try using reflection to access createRfcommSocket
            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
            return (BluetoothSocket) m.invoke(device, 1);
            
        } catch (Exception e) {
            Log.e(TAG, "All fallback methods failed: " + e.getMessage());
            throw new IOException("Could not create fallback socket: " + e.getMessage());
        }
    }

    /**
     * Get bluetooth device UUID
     */
    protected UUID getDeviceUUID() {
        ParcelUuid[] uuids = device.getUuids();
        if (uuids != null && uuids.length > 0) {
            if (Arrays.asList(uuids).contains(new ParcelUuid(RobustBluetoothConnection.SPP_UUID))) {
                return RobustBluetoothConnection.SPP_UUID;
            }
            return uuids[0].getUuid();
        } else {
            return RobustBluetoothConnection.SPP_UUID;
        }
    }

    /**
     * Close the socket connection with the bluetooth device.
     */
    public RobustBluetoothConnection disconnect() {
        this.data = new byte[0];
        if (this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing output stream: " + e.getMessage());
            }
            this.outputStream = null;
        }
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket: " + e.getMessage());
            }
            this.socket = null;
        }
        return this;
    }
} 