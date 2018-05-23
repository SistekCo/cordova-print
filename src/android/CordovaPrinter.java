package com.silverchip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.text.TextUtils;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CordovaPrinter extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            callbackContext.error("The device you are using does not support bluetooth.");
        }else{
            String bluetoothNotEnabledError = "Bluetooth is not enabled.";

            switch (action){
                case "cordovaGetPrinters":

                    if (!mBluetoothAdapter.isEnabled()){
                        callbackContext.error(bluetoothNotEnabledError);
                    }else{
                        List<String> retDevices = new ArrayList<String>();

                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                        for(BluetoothDevice bt : pairedDevices)
                        {
                            // Check if the signature of this device is consistent with zebra printers
                            BluetoothClass btClass = bt.getBluetoothClass();

                            if (
                                    btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING &&
                                            btClass.getDeviceClass() == 1664 &&
                                            btClass.hasService(BluetoothClass.Service.NETWORKING) &&
                                            btClass.hasService(BluetoothClass.Service.RENDER)
                                    ){

                                //Signature match!

                                String btName = bt.getName();
                                String btAddress = bt.getAddress();

                                Connection connection = new BluetoothConnection(btAddress);

                                try {
                                    connection.open();
                                    ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);

                                    connection.close();

                                    retDevices.add(btName);
                                } catch (ConnectionException e) {
                                    // Error is either due to bluetooth not being enabled, or device not being within range (or currently rejecting connections)

                                } catch (ZebraPrinterLanguageUnknownException e) {
                                    // Error is expected if the device is not a zebra printer.

                                } catch (Exception e) {
                                    // Unknown error.

                                }
                            }
                        }

                        callbackContext.success(TextUtils.join(",", retDevices));
                    }

                    break;
                case "cordovaPrint":

                    if (!mBluetoothAdapter.isEnabled()){
                        callbackContext.error(bluetoothNotEnabledError);
                    }else{
                        String labelData = data.getString(0);
                        String serialNumber = data.getString(1);

                        // Find our printer in the paired devices list
                        BluetoothDevice matchedDevice = getMatchedDevice(mBluetoothAdapter, serialNumber);


                        if (matchedDevice == null){
                            callbackContext.error("The selected printer is no longer paired.");
                        }else{
                            new Thread(() -> {
                                Looper.prepare();

                                Connection connection = new BluetoothConnection(matchedDevice.getAddress());

                                try {
                                    connection.open();
                                    ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                                    printer.sendCommand(labelData);

                                    connection.close();

                                    callbackContext.success("OK");
                                } catch (ConnectionException e) {
                                    // Error is either due to bluetooth not being enabled, or device not being within range (or currently rejecting connections)

                                    callbackContext.error("Bluetooth is not enabled, or the device is not in range.");
                                } catch (ZebraPrinterLanguageUnknownException e) {
                                    // Error is expected if the device is not a zebra printer.

                                    callbackContext.error("Unknown Error: ERR-ZPLUE");
                                } catch (Exception e) {
                                    // Unknown error.

                                    callbackContext.error("Unknown Error: ERR-UNK");
                                }

                                Looper.loop();
                                Looper.myLooper().quit();
                            }).start();
                        }
                    }

                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private BluetoothDevice getMatchedDevice(BluetoothAdapter mBluetoothAdapter, String serialNumber){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bt : pairedDevices)
        {
            if (bt.getName().equals(serialNumber)){
                return bt;
            }
        }

        return null;
    }
}