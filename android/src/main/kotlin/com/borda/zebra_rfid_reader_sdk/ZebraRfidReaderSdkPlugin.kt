package com.borda.zebra_rfid_reader_sdk

import android.util.Log
import com.borda.zebra_rfid_reader_sdk.utils.BordaReaderDevice
import com.borda.zebra_rfid_reader_sdk.utils.ConnectionStatus
import com.borda.zebra_rfid_reader_sdk.utils.LOG_TAG
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ZebraRfidReaderSdkPlugin */
class ZebraRfidReaderSdkPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var methodChannel: MethodChannel
    private lateinit var connectionHelper: ZebraConnectionHelper

    private lateinit var tagHandlerEvent: EventChannel
    private lateinit var tagFindingEvent: EventChannel
    private lateinit var readTagEvent: EventChannel
    private lateinit var tagDataEventHandler: TagDataEventHandler
    private lateinit var readTagEventHandler: TagDataEventHandler
    private lateinit var tagFindingEventHandler: TagDataEventHandler

    private var connectionHelperInitializationListener: ConnectionHelperInitializationListener? = null

    interface ConnectionHelperInitializationListener {
        fun onConnectionHelperInitialized()
    }

    fun setConnectionHelperInitializationListener(listener: ConnectionHelperInitializationListener) {
        connectionHelperInitializationListener = listener
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "Plugin attached to engine - initializing all components")
        
        // Setup Method Channel
        methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "borda/zebra_rfid_reader_sdk")
        methodChannel.setMethodCallHandler(this)

        // Setup Event Channels
        tagHandlerEvent = EventChannel(flutterPluginBinding.binaryMessenger, "tagHandlerEvent")
        tagFindingEvent = EventChannel(flutterPluginBinding.binaryMessenger, "tagFindingEvent")
        readTagEvent = EventChannel(flutterPluginBinding.binaryMessenger, "readTagEvent")

        // Create Event Handlers
        tagDataEventHandler = TagDataEventHandler()
        tagFindingEventHandler = TagDataEventHandler()
        readTagEventHandler = TagDataEventHandler()

        // Connect Event Handlers to Channels
        tagHandlerEvent.setStreamHandler(tagDataEventHandler)
        tagFindingEvent.setStreamHandler(tagFindingEventHandler)
        readTagEvent.setStreamHandler(readTagEventHandler)

        Log.d(LOG_TAG, "Channels and handlers initialized - creating connection helper")
        
        // Initialize Connection Helper with application context
        connectionHelper = ZebraConnectionHelper(
            flutterPluginBinding.applicationContext, 
            tagDataEventHandler, 
            tagFindingEventHandler, 
            readTagEventHandler
        )
        
        // Notify that initialization is complete
        connectionHelperInitializationListener?.onConnectionHelperInitialized()
        Log.d(LOG_TAG, "Plugin initialization completed successfully")
    }

    override fun onMethodCall(call: MethodCall, result: Result) =
        when (call.method) {
            "connect" -> {
                Log.d(LOG_TAG, "connect method called")
                val name = call.argument<String>("name")!!
                val readerConfig = call.argument<HashMap<String, Any>>("readerConfig")!!
                Log.d(LOG_TAG, "Attempting to connect to reader: $name")
                Log.d(LOG_TAG, "Reader configuration: $readerConfig")
                connectionHelper.connect(name, readerConfig)
            }

            "disconnect" -> {
                Log.d(LOG_TAG, "disconnect method called")
                connectionHelper.disconnect()
            }

            "setAntennaPower" -> {
                Log.d(LOG_TAG, "setAntennaPower method called")
                val transmitPowerIndex = call.argument<Int>("transmitPowerIndex")!!
                Log.d(LOG_TAG, "Setting antenna power to index: $transmitPowerIndex")
                connectionHelper.setAntennaConfig(transmitPowerIndex)
            }

            "setDynamicPower" -> {
                Log.d(LOG_TAG, "setDynamicPower method called")
                val isEnable = call.argument<Boolean>("isEnable")!!
                Log.d(LOG_TAG, "Setting dynamic power enabled: $isEnable")
                connectionHelper.setDynamicPower(isEnable)
            }

            "setBeeperVolume" -> {
                Log.d(LOG_TAG, "setBeeperVolume method called")
                val level = call.argument<Int>("level")!!
                Log.d(LOG_TAG, "Setting beeper volume to level: $level")
                connectionHelper.setBeeperVolumeConfig(level)
            }

            "findTheTag" -> {
                Log.d(LOG_TAG, "findTheTag method called")
                val tag = call.argument<String>("tag")!!
                Log.d(LOG_TAG, "Starting tag search for: $tag")
                connectionHelper.findTheTag(tag)
            }

            "stopFindingTheTag" -> {
                Log.d(LOG_TAG, "stopFindingTheTag method called")
                connectionHelper.stopFindingTheTag()
            }

            "getAvailableReaderList" -> {
                Log.d(LOG_TAG, "getAvailableReaderList method called")
                if (::connectionHelper.isInitialized) {
                    getAvailableReaderList(result)
                } else {
                    Log.e(LOG_TAG, "Connection helper not initialized when getAvailableReaderList was called")
                    result.error("CONNECTION_HELPER_NOT_INITIALIZED", "Connection Helper not initialized yet", null)
                }
            }
            
            else -> {
                Log.w(LOG_TAG, "Unknown method called: ${call.method}")
                result.notImplemented()
            }
        }

    private fun getAvailableReaderList(result: Result) {
        Log.d(LOG_TAG, "Retrieving available reader list")
        try {
            val readers = connectionHelper.getAvailableReaderList()
            val dataList = mutableListOf<BordaReaderDevice>()
            
            for (reader in readers) {
                val device = BordaReaderDevice(
                    ConnectionStatus.notConnected,
                    reader.name.toString(),
                    null,
                    null
                )
                dataList.add(device)
            }
            
            val jsonResult = Gson().toJson(dataList)
            Log.d(LOG_TAG, "Found ${dataList.size} available readers")
            result.success(jsonResult)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting available reader list: ${e.message}", e)
            result.error("READER_LIST_ERROR", "Failed to get available readers: ${e.message}", null)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "Plugin detaching from engine - starting cleanup")
        
        // Clean up method channel
        methodChannel.setMethodCallHandler(null)
        
        // Clean up event channels
        tagHandlerEvent.setStreamHandler(null)
        tagFindingEvent.setStreamHandler(null)
        readTagEvent.setStreamHandler(null)
        
        // Clean up connection helper
        if (::connectionHelper.isInitialized) {
            try {
                connectionHelper.dispose()
                Log.d(LOG_TAG, "Connection helper disposed successfully")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error disposing connection helper: ${e.message}", e)
            }
        }
        
        Log.d(LOG_TAG, "Plugin cleanup completed")
    }
}
