package com.dantsu.escposprinter.connection.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RobustBluetoothPrintersConnections extends BluetoothConnections {

    private static final String TAG = "RobustBTPrinters";
    private int retryCount = 3;
    private int connectionTimeout = 10000; // 10 seconds

    /**
     * Create a new instance of RobustBluetoothPrintersConnections
     */
    public RobustBluetoothPrintersConnections() {
        super();
    }

    /**
     * Create a new instance of RobustBluetoothPrintersConnections with custom parameters
     * 
     * @param retryCount number of connection retries
     * @param connectionTimeout connection timeout in milliseconds
     */
    public RobustBluetoothPrintersConnections(int retryCount, int connectionTimeout) {
        super();
        this.retryCount = retryCount;
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get a list of bluetooth printers using RobustBluetoothConnection.
     *
     * @return an array of BluetoothConnection
     */
    @SuppressLint("MissingPermission")
    @Nullable
    @Override
    public BluetoothConnection[] getList() {
        BluetoothConnection[] bluetoothDevicesList = super.getList();

        if (bluetoothDevicesList == null) {
            return null;
        }

        List<BluetoothConnection> printersList = new ArrayList<>();
        
        for (BluetoothConnection bluetoothConnection : bluetoothDevicesList) {
            BluetoothDevice device = bluetoothConnection.getDevice();
            
            try {
                int majDeviceCl = device.getBluetoothClass().getMajorDeviceClass();
                int deviceCl = device.getBluetoothClass().getDeviceClass();
                
                Log.d(TAG, "Device: " + device.getName() + 
                      ", Address: " + device.getAddress() + 
                      ", Major Class: " + majDeviceCl + 
                      ", Device Class: " + deviceCl);

                // Check if device is a printer
                // Major class: IMAGING (1536) and device class: PRINTER (1664)
                // Or device name contains "printer" (case insensitive)
                if ((majDeviceCl == BluetoothClass.Device.Major.IMAGING && 
                     (deviceCl == 1664 || deviceCl == BluetoothClass.Device.Major.IMAGING)) || 
                    (device.getName() != null && device.getName().toLowerCase().contains("printer")) ||
                    (device.getName() != null && device.getName().equals("InnerPrinter"))) {
                    
                    Log.d(TAG, "Found printer device: " + device.getName());
                    printersList.add(new RobustBluetoothConnection(device, retryCount, connectionTimeout));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking device: " + e.getMessage());
            }
        }
        
        return printersList.toArray(new BluetoothConnection[0]);
    }

    /**
     * Easy way to get the first bluetooth printer paired / connected.
     *
     * @return a RobustBluetoothConnection instance
     */
    public static RobustBluetoothConnection selectFirstPaired() {
        RobustBluetoothPrintersConnections printers = new RobustBluetoothPrintersConnections();
        BluetoothConnection[] bluetoothPrinters = printers.getList();

        if (bluetoothPrinters != null && bluetoothPrinters.length > 0) {
            try {
                BluetoothConnection printer = bluetoothPrinters[0];
                if (printer instanceof RobustBluetoothConnection) {
                    Log.d(TAG, "Attempting to connect to first printer: " + printer.getDevice().getName());
                    return ((RobustBluetoothConnection) printer).connect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to first printer: " + e.getMessage());
            }
        }
        
        Log.d(TAG, "No printers found");
        return null;
    }

    /**
     * Easy way to get the first bluetooth printer paired / connected with custom parameters.
     *
     * @param retryCount number of connection retries
     * @param connectionTimeout connection timeout in milliseconds
     * @return a RobustBluetoothConnection instance
     */
    public static RobustBluetoothConnection selectFirstPaired(int retryCount, int connectionTimeout) {
        RobustBluetoothPrintersConnections printers = new RobustBluetoothPrintersConnections(retryCount, connectionTimeout);
        BluetoothConnection[] bluetoothPrinters = printers.getList();

        if (bluetoothPrinters != null && bluetoothPrinters.length > 0) {
            try {
                BluetoothConnection printer = bluetoothPrinters[0];
                if (printer instanceof RobustBluetoothConnection) {
                    Log.d(TAG, "Attempting to connect to first printer: " + printer.getDevice().getName());
                    return ((RobustBluetoothConnection) printer).connect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to first printer: " + e.getMessage());
            }
        }
        
        Log.d(TAG, "No printers found");
        return null;
    }
} 