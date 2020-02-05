package com.example.opizon.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageHelper {

    // The maximum side length of the image to detect, to keep the size of image less than 4MB.
    // Resize the image if its side length is larger than the maximum.
    private static final int IMAGE_MAX_SIDE_LENGTH = 1280;

    // Ratio to scale a detected face rectangle, the face rectangle scaled up looks more natural.
    private static final double FACE_RECT_SCALE_RATIO = 1.3;

    // Decode image from imageUri, and resize according to the expectedMaxImageSideLength
    // If expectedMaxImageSideLength is
    //     (1) less than or equal to 0,
    //     (2) more than the actual max size length of the bitmap
    //     then return the original bitmap
    // Else, return the scaled bitmap
    public static Bitmap loadSizeLimitedBitmapFromImage(Image image, int rotation) {
        try {

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);



            // For saving memory, only decode the image meta and get the side length.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0 ,bytes.length, options);


            // Calculate shrink rate when loading the image into memory.
            int maxSideLength =
                    options.outWidth > options.outHeight ? options.outWidth : options.outHeight;
            options.inSampleSize = 1;
            options.inSampleSize = calculateSampleSize(maxSideLength, IMAGE_MAX_SIDE_LENGTH);
            options.inJustDecodeBounds = false;



            // Load the bitmap and resize it to the expected size length
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0 ,bytes.length, options);
            maxSideLength = bitmap.getWidth() > bitmap.getHeight()
                    ? bitmap.getWidth(): bitmap.getHeight();
            double ratio = IMAGE_MAX_SIDE_LENGTH / (double) maxSideLength;
            if (ratio < 1) {
                bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        (int)(bitmap.getWidth() * ratio),
                        (int)(bitmap.getHeight() * ratio),
                        false);
            }

            return bitmapRotator(bitmap, rotation);
//            return flipBitmap(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    // Return the number of times for the image to shrink when loading it into memory.
    // The SampleSize can only be a final value based on powers of 2.
    private static int calculateSampleSize(int maxSideLength, int expectedMaxImageSideLength) {
        int inSampleSize = 1;

        while (maxSideLength > 2 * expectedMaxImageSideLength) {
            maxSideLength /= 2;
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    private static int fixOrientation(Bitmap bitmap) {
        if (bitmap.getWidth() > bitmap.getHeight()) {
            return 90;
        }
        return 0;
    }

    private static Bitmap flipBitmap(Bitmap bitmap) {

        Matrix matrix = new Matrix();
        int rotation = fixOrientation(bitmap);
        matrix.postRotate(rotation);
        matrix.preScale(-1, 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // ROTATE


    private static Bitmap bitmapRotator(Bitmap oldBitmap, int rotation) {
        if(rotation == 0) return oldBitmap;
        //rotate
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap rotatedBitmap = Bitmap.createBitmap(oldBitmap, 0, 0, oldBitmap.getWidth(), oldBitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }
//
//
//
//    // Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
//    public static int getOrientation(byte[] jpeg) throws IOException {
//        ExifInterface exifInterface = new ExifInterface(new ByteArrayInputStream(jpeg));
//        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
//        int rotationDegrees = 0;
//        switch (orientation) {
//            case ExifInterface.ORIENTATION_ROTATE_90:
//                rotationDegrees = 90;
//                break;
//            case ExifInterface.ORIENTATION_ROTATE_180:
//                rotationDegrees = 180;
//                break;
//            case ExifInterface.ORIENTATION_ROTATE_270:
//                rotationDegrees = 270;
//                break;
//        }
//        return  rotationDegrees;
//    }
}
