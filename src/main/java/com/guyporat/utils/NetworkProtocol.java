package com.guyporat.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class NetworkProtocol {

    public static byte[] receive(InputStream inputStream) {
        byte[] result;
        // Take 8 bytes from socket, and that is the length of the packets. Continue until all bytes are received.
        try {
            byte[] lenBytes = inputStream.readNBytes(4);
            int len = ByteBuffer.wrap(lenBytes).getInt();

            result = inputStream.readNBytes(len);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void send(OutputStream outputStream, byte[] data) {
        // Send the length of the data, then the data itself
        try {
            outputStream.write(ByteBuffer.allocate(4).putInt(data.length).array());
            outputStream.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
