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

    private lateinit var methodChannel: MethodChannel
    private var connectionHelper: ZebraConnectionHelper? = null

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

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToEngine called")

        val messenger = binding.binaryMessenger
        val context = binding.applicationContext

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

        connectionHelper = ZebraConnectionHelper(
            context,
            tagDataEventHandler,
            tagFindingEventHandler,
            readTagEventHandler
        )

        connectionHelperInitializationListener?.onConnectionHelperInitialized()
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val helper = connectionHelper
        if (helper == null) {
            result.error("CONNECTION_HELPER_NOT_INITIALIZED", "Connection Helper not initialized yet", null)
            return
        }

        when (call.method) {
            "connect" -> {
                val name = call.argument<String>("name") ?: return result.error("INVALID_ARGUMENT", "Missing name", null)
                val readerConfig = call.argument<HashMap<String, Any>>("readerConfig")
                    ?: return result.error("INVALID_ARGUMENT", "Missing readerConfig", null)

                Log.d(LOG_TAG, "Connecting to -> $name with config -> $readerConfig")
                helper.connect(name, readerConfig)
                result.success(null)
            }

            "disconnect" -> {
                helper.disconnect()
                result.success(null)
            }

            "setAntennaPower" -> {
                val transmitPowerIndex = call.argument<Int>("transmitPowerIndex")
                    ?: return result.error("INVALID_ARGUMENT", "Missing transmitPowerIndex", null)

                helper.setAntennaConfig(transmitPowerIndex)
                result.success(null)
            }

            "setDynamicPower" -> {
                val isEnable = call.argument<Boolean>("isEnable")
                    ?: return result.error("INVALID_ARGUMENT", "Missing isEnable", null)

                helper.setDynamicPower(isEnable)
                result.success(null)
            }

            "setBeeperVolume" -> {
                val level = call.argument<Int>("level")
                    ?: return result.error("INVALID_ARGUMENT", "Missing level", null)

                helper.setBeeperVolumeConfig(level)
                result.success(null)
            }

            "findTheTag" -> {
                val tag = call.argument<String>("tag")
                    ?: return result.error("INVALID_ARGUMENT", "Missing tag", null)

                Log.d(LOG_TAG, "findTheTag called with tag -> $tag")
                helper.findTheTag(tag)
                result.success(null)
            }

            "stopFindingTheTag" -> {
                helper.stopFindingTheTag()
                result.success(null)
            }

            "getAvailableReaderList" -> {
                getAvailableReaderList(helper, result)
            }

            else -> result.notImplemented()
        }
    }

    private fun getAvailableReaderList(helper: ZebraConnectionHelper, result: Result) {
        val readers = helper.getAvailableReaderList()
        val dataList = readers.map { reader ->
            BordaReaderDevice(
                ConnectionStatus.notConnected,
                reader.name.toString(),
                null,
                null
            )
        }
        result.success(Gson().toJson(dataList))
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        tagHandlerEvent.setStreamHandler(null)
        tagFindingEvent.setStreamHandler(null)
        readTagEvent.setStreamHandler(null)
        connectionHelper?.dispose()
        connectionHelper = null
    }
}
