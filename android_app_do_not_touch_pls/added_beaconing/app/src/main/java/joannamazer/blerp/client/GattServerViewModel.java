package joannamazer.blerp.client;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.databinding.BaseObservable;
import android.databinding.Bindable;


@SuppressLint("NewApi")
public class GattServerViewModel extends BaseObservable {

    private BluetoothDevice mBluetoothDevice;

    public GattServerViewModel(BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }

    @Bindable
    public String getServerName() {
        if (mBluetoothDevice == null) {
            return "";
        }
        return mBluetoothDevice.getAddress();
    }
}