package com.borda.zebra_rfid_reader_sdk

import android.util.Log
import android.content.Context
import com.borda.zebra_rfid_reader_sdk.utils.BordaReaderDevice
import com.borda.zebra_rfid_reader_sdk.utils.ConnectionStatus
import com.borda.zebra_rfid_reader_sdk.utils.LOG_TAG
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** ZebraRfidReaderSdkPlugin */
class ZebraRfidReaderSdkPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var methodChannel: MethodChannel
    private lateinit var connectionHelper: ZebraConnectionHelper

    private lateinit var tagHandlerEvent: EventChannel
    private lateinit var tagFindingEvent: EventChannel
    private lateinit var readTagEvent: EventChannel

    private lateinit var tagDataEventHandler: TagDataEventHandler
    private lateinit var tagFindingEventHandler: TagDataEventHandler
    private lateinit var readTagEventHandler: TagDataEventHandler

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "Plugin attached to engine – initializing channels and handlers")

        // Method Channel
        methodChannel = MethodChannel(binding.binaryMessenger, "borda/zebra_rfid_reader_sdk")
        methodChannel.setMethodCallHandler(this)

        // Event Channels
        tagHandlerEvent  = EventChannel(binding.binaryMessenger, "tagHandlerEvent")
        tagFindingEvent  = EventChannel(binding.binaryMessenger, "tagFindingEvent")
        readTagEvent     = EventChannel(binding.binaryMessenger, "readTagEvent")

        // Event Handlers
        tagDataEventHandler    = TagDataEventHandler()
        tagFindingEventHandler = TagDataEventHandler()
        readTagEventHandler    = TagDataEventHandler()

        tagHandlerEvent.setStreamHandler(tagDataEventHandler)
        tagFindingEvent.setStreamHandler(tagFindingEventHandler)
        readTagEvent.setStreamHandler(readTagEventHandler)

        // Connection helper
        connectionHelper = ZebraConnectionHelper(
            binding.applicationContext,
            tagDataEventHandler,
            tagFindingEventHandler,
            readTagEventHandler
        )

        Log.d(LOG_TAG, "Plugin initialization completed")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "connect" -> {
                Log.d(LOG_TAG, "connect called")
                val name = call.argument<String>("name")!!
                val config = call.argument<Map<String, Any>>("readerConfig")!!
                connectionHelper.connect(name, config)
            }
            "disconnect" -> {
                Log.d(LOG_TAG, "disconnect called")
                connectionHelper.disconnect()
            }
            "setAntennaPower" -> {
                Log.d(LOG_TAG, "setAntennaPower called")
                val power = call.argument<Int>("transmitPowerIndex")!!
                connectionHelper.setAntennaConfig(power)
            }
            "setDynamicPower" -> {
                Log.d(LOG_TAG, "setDynamicPower called")
                val enabled = call.argument<Boolean>("isEnable")!!
                connectionHelper.setDynamicPower(enabled)
            }
            "setBeeperVolume" -> {
                Log.d(LOG_TAG, "setBeeperVolume called")
                val level = call.argument<Int>("level")!!
                connectionHelper.setBeeperVolumeConfig(level)
            }
            "findTheTag" -> {
                Log.d(LOG_TAG, "findTheTag called")
                val tag = call.argument<String>("tag")!!
                connectionHelper.findTheTag(tag)
            }
            "stopFindingTheTag" -> {
                Log.d(LOG_TAG, "stopFindingTheTag called")
                connectionHelper.stopFindingTheTag()
            }
            "getAvailableReaderList" -> {
                Log.d(LOG_TAG, "getAvailableReaderList called")
                getAvailableReaderList(result)
            }
            else -> {
                Log.w(LOG_TAG, "Unknown method: ${call.method}")
                result.notImplemented()
            }
        }
    }

    private fun getAvailableReaderList(result: MethodChannel.Result) {
        try {
            val readers = connectionHelper.getAvailableReaderList()
            val devices = readers.map {
                BordaReaderDevice(
                    ConnectionStatus.notConnected,
                    it.name.toString(),
                    null,
                    null
                )
            }
            result.success(Gson().toJson(devices))
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error retrieving readers: ${e.message}", e)
            result.error("READER_LIST_ERROR", "Failed to get available readers: ${e.message}", null)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "Plugin detaching – cleaning up")
        methodChannel.setMethodCallHandler(null)
        tagHandlerEvent.setStreamHandler(null)
        tagFindingEvent.setStreamHandler(null)
        readTagEvent.setStreamHandler(null)
        connectionHelper.dispose()
        Log.d(LOG_TAG, "Cleanup completed")
    }
}
