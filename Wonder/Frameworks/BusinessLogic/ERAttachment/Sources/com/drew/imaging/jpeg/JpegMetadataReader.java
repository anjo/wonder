/*
 * Created by dnoakes on 12-Nov-2002 18:51:36 using IntelliJ IDEA.
 */
package com.drew.imaging.jpeg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.ExifReader;
import com.drew.metadata.iptc.IptcReader;
import com.drew.metadata.jpeg.JpegCommentReader;
import com.drew.metadata.jpeg.JpegReader;
import com.sun.image.codec.jpeg.JPEGDecodeParam;

/**
 *
 */
public class JpegMetadataReader
{
    public static Metadata readMetadata(InputStream in) throws JpegProcessingException
    {
        JpegSegmentReader segmentReader = new JpegSegmentReader(in);
        return extractJpegSegmentReaderMetadata(segmentReader);
    }

    public static Metadata readMetadata(File file) throws JpegProcessingException
    {
        JpegSegmentReader segmentReader = new JpegSegmentReader(file);
        return extractJpegSegmentReaderMetadata(segmentReader);
    }

    private static Metadata extractJpegSegmentReaderMetadata(JpegSegmentReader segmentReader)
    {
        final Metadata metadata = new Metadata();
        try {
            byte[] exifSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_APP1);
            new ExifReader(exifSegment).extract(metadata);
        } catch (JpegProcessingException e) {
            // in the interests of catching as much data as possible, continue
            // TODO lodge error message within exif directory?
        }

        try {
            byte[] iptcSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_APPD);
            new IptcReader(iptcSegment).extract(metadata);
        } catch (JpegProcessingException e) {
            // TODO lodge error message within iptc directory?
        }

		try {
			byte[] jpegSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_SOF0);
			new JpegReader(jpegSegment).extract(metadata);
		} catch (JpegProcessingException e) {
			// TODO lodge error message within jpeg directory?
		}

		try {
			byte[] jpegCommentSegment = segmentReader.readSegment(JpegSegmentReader.SEGMENT_COM);
			new JpegCommentReader(jpegCommentSegment).extract(metadata);
		} catch (JpegProcessingException e) {
			// TODO lodge error message within jpegcomment directory?
		}

        return metadata;
    }

    public static Metadata readMetadata(JPEGDecodeParam decodeParam)
    {
        final Metadata metadata = new Metadata();

        /* We should only really be seeing Exif in _data[0]... the 2D array exists
         * because markers can theoretically appear multiple times in the file.
         */
        // TODO test this method
        byte[][] exifSegment = decodeParam.getMarkerData(JPEGDecodeParam.APP1_MARKER);
        if (exifSegment != null && exifSegment[0].length>0) {
            new ExifReader(exifSegment[0]).extract(metadata);
        }

        // similarly, use only the first IPTC segment
        byte[][] iptcSegment = decodeParam.getMarkerData(JPEGDecodeParam.APPD_MARKER);
        if (iptcSegment != null && iptcSegment[0].length>0) {
            new IptcReader(iptcSegment[0]).extract(metadata);
        }

        // NOTE: Unable to utilise JpegReader for the SOF0 frame here, as the decodeParam doesn't contain the byte[]

        // similarly, use only the first Jpeg Comment segment
        byte[][] jpegCommentSegment = decodeParam.getMarkerData(JPEGDecodeParam.COMMENT_MARKER);
        if (jpegCommentSegment != null && jpegCommentSegment[0].length>0) {
            new JpegCommentReader(jpegCommentSegment[0]).extract(metadata);
        }

        return metadata;
    }

    private JpegMetadataReader()
    {
    }

    public static void main(String[] args) throws MetadataException, IOException
    {
        JpegMetadataReader jpegReader = new JpegMetadataReader();
        Metadata metadata = null;
        try {
            metadata = JpegMetadataReader.readMetadata(new File(args[0]));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // iterate over the exif data and print to System.out
        Iterator directories = metadata.getDirectoryIterator();
        while (directories.hasNext()) {
            Directory directory = (Directory)directories.next();
            Iterator tags = directory.getTagIterator();
            while (tags.hasNext()) {
                Tag tag = (Tag)tags.next();
                try {
                    System.out.println("[" + directory.getName() + "] " + tag.getTagName() + " = " + tag.getDescription());
                } catch (MetadataException e) {
                    System.err.println(e.getMessage());
                    System.err.println(tag.getDirectoryName() + " " + tag.getTagName() + " (error)");
                }
            }
            if (directory.hasErrors()) {
                Iterator errors = directory.getErrors();
                while (errors.hasNext()) {
                    System.out.println("ERROR: " + errors.next());
                }
            }
        }

        if (args.length>1 && args[1].trim().equals("/thumb"))
        {
            ExifDirectory directory = (ExifDirectory)metadata.getDirectory(ExifDirectory.class);
            if (directory.containsThumbnail())
            {
                System.out.println("Writing thumbnail...");
                directory.writeThumbnail(args[0].trim() + ".thumb.jpg");
            }
            else
            {
                System.out.println("No thumbnail data exists in this image");
            }
        }
    }
}
