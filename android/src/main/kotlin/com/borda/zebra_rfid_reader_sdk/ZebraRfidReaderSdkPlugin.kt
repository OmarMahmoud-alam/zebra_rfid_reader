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
import io.flutter.plugin.common.BinaryMessenger // Import BinaryMessenger
import io.flutter.plugin.common.PluginRegistry.Registrar // Import Registrar
import android.content.Context // Import Context

/** ZebraRfidReaderSdkPlugin */
class ZebraRfidReaderSdkPlugin : FlutterPlugin, MethodCallHandler {
    companion object {
        @JvmStatic
        fun register(registrar: Registrar) {
            val plugin = ZebraRfidReaderSdkPlugin()
            plugin.initialize(registrar.messenger(), registrar.context())
        }
    }
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
        Log.d(LOG_TAG, "Calllllllledd:::::::")
        methodChannel =
            MethodChannel(flutterPluginBinding.binaryMessenger, "borda/zebra_rfid_reader_sdk")
        methodChannel.setMethodCallHandler(this)

        tagHandlerEvent = EventChannel(flutterPluginBinding.binaryMessenger, "tagHandlerEvent")
        tagFindingEvent = EventChannel(flutterPluginBinding.binaryMessenger, "tagFindingEvent")
        readTagEvent = EventChannel(flutterPluginBinding.binaryMessenger, "readTagEvent")

        tagDataEventHandler = TagDataEventHandler()
        tagFindingEventHandler = TagDataEventHandler()
        readTagEventHandler = TagDataEventHandler()


        tagHandlerEvent.setStreamHandler(tagDataEventHandler)
        tagFindingEvent.setStreamHandler(tagFindingEventHandler)
        readTagEvent.setStreamHandler(readTagEventHandler)
        Log.d(LOG_TAG, "onAttachedToEngine called")
        connectionHelper = ZebraConnectionHelper(flutterPluginBinding.applicationContext, tagDataEventHandler, tagFindingEventHandler, readTagEventHandler)
        connectionHelperInitializationListener?.onConnectionHelperInitialized() // Notify
    }



    private fun initialize(messenger: BinaryMessenger, context: Context) {
        Log.d(LOG_TAG, "Calllllllledd:::::::")
        methodChannel = MethodChannel(messenger, "borda/zebra_rfid_reader_sdk")
        methodChannel.setMethodCallHandler(this)

        tagHandlerEvent = EventChannel(messenger, "tagHandlerEvent")
        tagFindingEvent = EventChannel(messenger, "tagFindingEvent")
        readTagEvent = EventChannel(messenger, "readTagEvent")

        tagDataEventHandler = TagDataEventHandler()
        tagFindingEventHandler = TagDataEventHandler()
        readTagEventHandler = TagDataEventHandler()

        tagHandlerEvent.setStreamHandler(tagDataEventHandler)
        tagFindingEvent.setStreamHandler(tagFindingEventHandler)
        readTagEvent.setStreamHandler(readTagEventHandler)

        Log.d(LOG_TAG, "registerWith called")
        connectionHelper = ZebraConnectionHelper(context, tagDataEventHandler, tagFindingEventHandler, readTagEventHandler)
        connectionHelperInitializationListener?.onConnectionHelperInitialized()
    }

    override fun onMethodCall(call: MethodCall, result: Result) =
        when (call.method) {
            "connect" -> {
                Log.d(LOG_TAG, "connect called")
                val name = call.argument<String>("name")!!
                val readerConfig = call.argument<HashMap<String, Any>>("readerConfig")!!
                Log.d(LOG_TAG, "will try to connect to -> $name")
                Log.d(LOG_TAG, "USER CONFIG -> $readerConfig")
                connectionHelper.connect(name, readerConfig)
            }

            "disconnect" -> {
                Log.d(LOG_TAG, "disconnect called")
                connectionHelper.disconnect()
            }

            "setAntennaPower" -> {
                val transmitPowerIndex = call.argument<Int>("transmitPowerIndex")!!
                connectionHelper.setAntennaConfig(transmitPowerIndex)
            }

            "setDynamicPower" -> {
                val isEnable = call.argument<Boolean>("isEnable")!!
                connectionHelper.setDynamicPower(isEnable)
            }

            "setBeeperVolume" -> {
                val level = call.argument<Int>("level")!!
                connectionHelper.setBeeperVolumeConfig(level)
            }

            "findTheTag" -> {
                val tag = call.argument<String>("tag")!!
                Log.d("ENGIN", "findTheTag called with tag -> $tag")
                connectionHelper.findTheTag(tag)
            }

            "stopFindingTheTag" -> {
                connectionHelper.stopFindingTheTag()
            }

            "getAvailableReaderList" -> {
                if (::connectionHelper.isInitialized) { // Check for initialization
                    getAvailableReaderList(result)
                } else {
                    result.error("CONNECTION_HELPER_NOT_INITIALIZED", "Connection Helper not initialized yet", null)
                }
            }
            else -> result.notImplemented()
        }

    private fun getAvailableReaderList(result: Result) {
        Log.d(LOG_TAG, "getAvailableReaderList called")
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
        result.success(Gson().toJson(dataList))
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        tagHandlerEvent.setStreamHandler(null)
        tagFindingEvent.setStreamHandler(null)
        connectionHelper.dispose()
    }
}
