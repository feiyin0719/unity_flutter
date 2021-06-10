package com.example.unitylibrary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.flutter.Log;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.renderer.FlutterRenderer;
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener;

public class FlutterSurface {
    private FlutterEngine flutterEngine;
    private int pointerId = 1;
    private static final int POINTER_DATA_FIELD_COUNT = 28;
    private static final int BYTES_PER_FIELD = 8;

    public void attachToFlutterEngine(FlutterEngine flutterEngine) {
        this.flutterEngine = flutterEngine;
        FlutterRenderer flutterRenderer = flutterEngine.getRenderer();

        flutterRenderer.startRenderingToSurface(GLTexture.instance.surface);
        flutterRenderer.surfaceChanged(GLTexture.instance.getStreamTextureWidth(), GLTexture.instance.getStreamTextureHeight());
        FlutterRenderer.ViewportMetrics viewportMetrics = new FlutterRenderer.ViewportMetrics();
        viewportMetrics.width = GLTexture.instance.getStreamTextureWidth();
        viewportMetrics.height = GLTexture.instance.getStreamTextureHeight();
        viewportMetrics.devicePixelRatio = GLTexture.instance.context.getResources().getDisplayMetrics().density;
        flutterRenderer.setViewportMetrics(viewportMetrics);
        flutterRenderer.addIsDisplayingFlutterUiListener(new FlutterUiDisplayListener() {
            @Override
            public void onFlutterUiDisplayed() {
                GLTexture.instance.setNeedUpdate(true);
                GLTexture.instance.updateTexture();
            }

            @Override
            public void onFlutterUiNoLongerDisplayed() {

            }
        });
        GLTexture.instance.attachFlutterSurface(this);
    }

    public void onTouchEvent(int type, double x, double y) {
        ByteBuffer packet =
                ByteBuffer.allocateDirect(1 * POINTER_DATA_FIELD_COUNT * BYTES_PER_FIELD);
        packet.order(ByteOrder.LITTLE_ENDIAN);
        double x1, y1;
        x1 = GLTexture.instance.getStreamTextureWidth() * x;
        y1 = GLTexture.instance.getStreamTextureHeight() * y;
        Log.i("myyf", "x:" + x1 + "&y:" + y1 + "&type:" + type);
        addPointerForIndex(x1, y1, type + 4, 0, packet);
        if (packet.position() % (POINTER_DATA_FIELD_COUNT * BYTES_PER_FIELD) != 0) {
            throw new AssertionError("Packet position is not on field boundary");
        }

        flutterEngine.getRenderer().dispatchPointerDataPacket(packet,packet.position());

    }


    // TODO(mattcarroll): consider creating a PointerPacket class instead of using a procedure that
    // mutates inputs.
    private void addPointerForIndex(
            double x, double y, int pointerChange, int pointerData, ByteBuffer packet) {
        if (pointerChange == -1) {
            return;
        }

        int pointerKind = 0;

        int signalKind =
                0;

        long timeStamp = System.currentTimeMillis() * 1000; // Convert from milliseconds to microseconds.

        packet.putLong(timeStamp); // time_stamp
        packet.putLong(pointerChange); // change
        packet.putLong(pointerKind); // kind
        packet.putLong(signalKind); // signal_kind
        packet.putLong(pointerId); // device
        packet.putLong(0); // pointer_identifier, will be generated in pointer_data_packet_converter.cc.
        packet.putDouble(x); // physical_x
        packet.putDouble(y); // physical_y
        packet.putDouble(
                0.0); // physical_delta_x, will be generated in pointer_data_packet_converter.cc.
        packet.putDouble(
                0.0); // physical_delta_y, will be generated in pointer_data_packet_converter.cc.

        long buttons = 0;

        packet.putLong(buttons); // buttons

        packet.putLong(0); // obscured

        packet.putLong(0); // synthesized

        packet.putDouble(1.0); // pressure
        double pressureMin = 0.0;
        double pressureMax = 1.0;
//        if (event.getDevice() != null) {
//            InputDevice.MotionRange pressureRange =
//                    event.getDevice().getMotionRange(MotionEvent.AXIS_PRESSURE);
//            if (pressureRange != null) {
//                pressureMin = pressureRange.getMin();
//                pressureMax = pressureRange.getMax();
//            }
//        }
        packet.putDouble(pressureMin); // pressure_min
        packet.putDouble(pressureMax); // pressure_max


        packet.putDouble(0.0); // distance
        packet.putDouble(0.0); // distance_max


        packet.putDouble(0.5); // size

        packet.putDouble(6); // radius_major
        packet.putDouble(7); // radius_minor

        packet.putDouble(0.0); // radius_min
        packet.putDouble(0.0); // radius_max

        packet.putDouble(0); // orientation


        packet.putDouble(0.0); // tilt


        packet.putLong(pointerData); // platformData


        packet.putDouble(0.0); // scroll_delta_x
        packet.putDouble(0.0); // scroll_delta_x

    }


}
