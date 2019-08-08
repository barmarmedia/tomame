package com.example.fotoshooting

import android.Manifest
import android.R.attr.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.View
import java.io.IOException
import java.util.*
import javax.xml.datatype.DatatypeConstants.SECONDS
import android.view.animation.AlphaAnimation
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.*
import org.json.JSONObject
import top.defaults.colorpicker.ColorPickerPopup
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate


private const val SCAN_PERIOD: Long = 20000
private const val REQUEST_ENABLE_BT: Int = 1
private const val REQUEST_ACCESS_COARSE_LOCATION = 2
//private const val BT_DEVICE_NAME = "BT05"
private const val BT_DEVICE_NAME = "MMMT_ITS"
private lateinit var listView: ListView
private val lampList:ArrayList<Lamp> = ArrayList()
private lateinit var adapter: BaseAdapter

private val PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN)

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity() : Activity(), View.OnClickListener, AdapterView.OnItemLongClickListener{

    lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var characteristic:BluetoothGattCharacteristic
    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var mScanning: Boolean = false

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = LampAdapter(this, lampList)

        listView = findViewById(R.id.lamp_list_view)
        listView.onItemLongClickListener = this
        listView.adapter = adapter
        lampList.add(Lamp(color=0x00ff00))
        lampList.add(Lamp( ""))
        lampList.add(Lamp())

        listView.setOnItemClickListener { parent, view, position, id ->

            ColorPickerPopup.Builder(this)
                .initialColor(lampList[position].color) // Set initial color
                .enableBrightness(true) // Enable brightness slider or not
                .enableAlpha(false) // Enable alpha slider or not
                .okTitle("Farbe senden")
                .cancelTitle("Abbrechen")
                .showIndicator(true)
                .showValue(false)
                .build()
                .show(view,object: ColorPickerPopup.ColorPickerObserver() {
                    @SuppressLint("NewApi")
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onColorPicked(color: Int) {
                        setColor(position, color,  true)
                    }
                })
        }

        //setLoadingState(true)
    }

    override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
        lampList.removeAt(position)
        adapter?.notifyDataSetChanged()
        return true
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setColor(index:Int, color:Int, sendToBT:Boolean) {
        if(lampList.size <= index)
            return
        lampList[index].color = color
        adapter.notifyDataSetChanged()

        if(sendToBT)
            sendValueToBluetooth(getJsonStringForModule(index))
    }

    private fun checkPermission() : Boolean {
        var result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_ACCESS_COARSE_LOCATION)
    }

    private fun setLoadingState(loading:Boolean) {
        var contentView = findViewById<View>(R.id.contentView)
        var progressBarHolder = findViewById<View>(R.id.progressBarHolder) as FrameLayout
        var progressBar = findViewById<ProgressBar>(R.id.loadingIndicator)

        runOnUiThread {
            if(loading) {
                progressBarHolder.visibility = View.VISIBLE
                contentView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
            } else {
                progressBarHolder.visibility = View.GONE
                contentView.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            }
        }

    }

    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scanLeDevice(enable: Boolean) {
        // Permission has already been granted
        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show()
            Log.d("asdf", "NOT SUPPORTED")
            finish()
        }

        bluetoothAdapter?.takeIf { !it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        var bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        val builder = ScanFilter.Builder()
        builder.setDeviceName(BT_DEVICE_NAME)
        val filter = Vector<ScanFilter>()
        filter.add(builder.build())
        val builderScanSettings = ScanSettings.Builder()
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        builderScanSettings.setReportDelay(0)

        when (enable) {
            true -> {
                Log.w("asdf", "starting le scan")
                // Stops scanning after a pre-defined scan period.
                Handler().postDelayed({
                    mScanning = false
                    bluetoothLeScanner.stopScan(mLeScanCallback)
                    Log.w("asdf","scan stopped by handler")
                }, SCAN_PERIOD)
                mScanning = true
                //bluetoothLeScanner.startScan(filter, builderScanSettings.build(), mLeScanCallback)
                bluetoothLeScanner.startScan(filter, builderScanSettings.build(), mLeScanCallback)
            }
            else -> {
                Log.w("asdf", "stopping le scan")
                mScanning = false
                bluetoothLeScanner?.stopScan(mLeScanCallback)
            }
        }
    }

    private val mLeScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            scanLeDevice(false)
            bluetoothGatt = result.device.connectGatt(application, false, gattCallback)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w("asdf", "Scan failed")
        }
    }

    // Various callback methods defined by the BLE API.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("asdf", "Connected to GATT server.")
                    Log.i("asdf", "Attempting to start service discovery: " +
                            bluetoothGatt.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    setLoadingState(true)
                    runOnUiThread{
                        findViewById<ProgressBar>(R.id.loadingIndicator).visibility = View.VISIBLE
                        findViewById<Button>(R.id.connectButton).visibility = View.INVISIBLE

                    }
                    Log.i("asdf", "Disconnected from GATT server.")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d("asdf", "onCharacteristicChanges: ${characteristic?.uuid}")
        }

        // New services discovered
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            setLoadingState(false)
            var service = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
            characteristic = service.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))

        }

        override fun onCharacteristicWrite(gatt:BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status:Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            gatt.executeReliableWrite()
        }


        // Result of a characteristic read operation
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View) {
        when (v.id) {
            R.id.setup_button_1-> {
                setColor(0, Color.RED, false )
                setColor(1, Color.GREEN, false)
                setColor(2, Color.BLUE, false)
                sendValueToBluetooth(getJsonStringForAllLamps())
            }
            R.id.setup_button_2-> {
                setColor(0, Color.CYAN, false)
                setColor(1, Color.BLACK, false)
                setColor(2, Color.MAGENTA, false)
                sendValueToBluetooth(getJsonStringForAllLamps())
            }
            R.id.setup_button_3-> {
                setColor(0, Color.YELLOW, false)
                setColor(1, Color.MAGENTA, false)
                setColor(2, Color.LTGRAY, false)
                sendValueToBluetooth(getJsonStringForAllLamps())
            }
            R.id.add_lamp_button-> {
                if(lampList.count() < 6) {
                    lampList.add(Lamp())
                    adapter?.notifyDataSetChanged()
                    listView.invalidate()
                }
            }
            R.id.connectButton-> {
                if (!checkPermission()) {
                    requestPermission()
                    Log.d("asdf", "no permission")
                } else {
                    Log.d("asdf", "scanning device")
                    setLoadingState(true)
                    findViewById<Button>(R.id.connectButton).visibility = View.GONE
                    scanLeDevice(true)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun logList() {
        lampList.forEachIndexed{i, lamp ->
            run {
                Log.d("asdf", ""+i+lamp.toString())
            }
        }
    }

    private fun sendValueToBluetooth(str : String) {

        var restString : String? = null
        var stringToSend : String? = null

        if(str.length > 17) {
            stringToSend = str.substring(0, 16)
            restString = str.substring(17)
        }
        else {
            stringToSend = str
        }

        bluetoothGatt.beginReliableWrite()
        characteristic.setValue(stringToSend)


        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        if(bluetoothGatt.writeCharacteristic(characteristic)) {
            Log.d("asdf", "characteristic written")
            if(restString != null) {
                Timer().schedule(300) {
                    sendValueToBluetooth(restString)
                }
            }
        } else {
            Log.d("asdf", "error writing characteristic")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getJsonStringForAllLamps() : String {
        var str = ""
        lampList.forEachIndexed {
            index, lamp ->
            run {
                str += getJsonStringForModule(index)
            }
        }
        return str
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getJsonStringForModule(lamp: Int) : String {
        var lampEntry = lampList[lamp]
        val hsv = FloatArray(3)
        Color.colorToHSV(lampEntry.color, hsv)

        var json = JSONObject()
        json.put("lamp", ""+(lamp+1))
        json.put("hue", ""+(hsv[0].toInt() * 17/24))
        json.put("sat", ""+(hsv[1] * 255).toInt())
        json.put("val", ""+(hsv[2] * 255).toInt())

        Log.d("asdf", json.toString())

        return json.toString()+"\r\n"

    }


}
