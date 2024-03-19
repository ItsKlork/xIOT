package com.guyporat.utils;

import java.util.zip.Deflater;

public class CompressionUtils {

    public static byte[] compressData(byte[] data) {
        try {
            Deflater deflater = new Deflater();
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[data.length];
            int compressedSize = deflater.deflate(buffer);

            byte[] compressedData = new byte[compressedSize];
            System.arraycopy(buffer, 0, compressedData, 0, compressedSize);

            deflater.end();
            return compressedData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
