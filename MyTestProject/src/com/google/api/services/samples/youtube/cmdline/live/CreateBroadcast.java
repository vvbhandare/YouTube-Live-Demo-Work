/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.services.samples.youtube.cmdline.live;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.samples.youtube.cmdline.live.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;
import com.xuggle.xuggler.Configuration;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Use the YouTube Live Streaming API to insert a broadcast and a stream
 * and then bind them together. Use OAuth 2.0 to authorize the API requests.
 *
 * @author Ibrahim Ulukaya
 */
public class CreateBroadcast {

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;
    

    /**
     * Create and insert a liveBroadcast resource.
     */
    public static void main(String[] args) {

        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");

        try {
            // Authorize the request.
            Credential credential = Auth.authorize(scopes, "createbroadcast");

            // This object is used to make YouTube Data API requests.
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("MyTestProject").build();

            // Prompt the user to enter a title for the broadcast.
            String title = getBroadcastTitle();
            System.out.println("You chose " + title + " for broadcast title.");

            // Create a snippet with the title and scheduled start and end
            // times for the broadcast. Currently, those times are hard-coded.
            LiveBroadcastSnippet broadcastSnippet = new LiveBroadcastSnippet();
            broadcastSnippet.setTitle(title);
            broadcastSnippet.setScheduledStartTime(new DateTime("2015-06-15T04:40:00.000Z"));
            broadcastSnippet.setScheduledEndTime(new DateTime("2015-06-16T00:00:00.000Z"));

            // Set the broadcast's privacy status to "private". See:
            // https://developers.google.com/youtube/v3/live/docs/liveBroadcasts#status.privacyStatus
            LiveBroadcastStatus status = new LiveBroadcastStatus();
            status.setPrivacyStatus("public");
            
            LiveBroadcast broadcast = new LiveBroadcast();
            broadcast.setKind("youtube#liveBroadcast");
            broadcast.setSnippet(broadcastSnippet);
            broadcast.setStatus(status);

            // Construct and execute the API request to insert the broadcast.
            YouTube.LiveBroadcasts.Insert liveBroadcastInsert =
                    youtube.liveBroadcasts().insert("snippet,status", broadcast);
            LiveBroadcast returnedBroadcast = liveBroadcastInsert.execute();

            // Print information from the API response.
            System.out.println("\n================== Returned Broadcast ==================\n");
            System.out.println("  - Id: " + returnedBroadcast.getId());
            System.out.println("  - Title: " + returnedBroadcast.getSnippet().getTitle());
            System.out.println("  - Description: " + returnedBroadcast.getSnippet().getDescription());
            System.out.println("  - Published At: " + returnedBroadcast.getSnippet().getPublishedAt());
            System.out.println(
                    "  - Scheduled Start Time: " + returnedBroadcast.getSnippet().getScheduledStartTime());
            System.out.println(
                    "  - Scheduled End Time: " + returnedBroadcast.getSnippet().getScheduledEndTime());

            // Prompt the user to enter a title for the video stream.
            title = getStreamTitle();
            System.out.println("You chose " + title + " for stream title.");

            // Create a snippet with the video stream's title.
            LiveStreamSnippet streamSnippet = new LiveStreamSnippet();
            streamSnippet.setTitle(title);

            // Define the content distribution network settings for the
            // video stream. The settings specify the stream's format and
            // ingestion type. See:
            // https://developers.google.com/youtube/v3/live/docs/liveStreams#cdn
            CdnSettings cdnSettings =  new CdnSettings();
            cdnSettings.setFormat("240p");
            cdnSettings.setIngestionType("rtmp");

            Video video = new Video();
            video.set("video", "/resources/vid_bigbuckbunny.mov");
            
            LiveStream stream = new LiveStream();
            stream.set("video", video);
            stream.setKind("youtube#liveStream");
            stream.setSnippet(streamSnippet);
            stream.setCdn(cdnSettings);

            // Construct and execute the API request to insert the stream.
            YouTube.LiveStreams.Insert liveStreamInsert =
                    youtube.liveStreams().insert("snippet,cdn", stream);
            liveStreamInsert.set("video", "/resources/vid_bigbuckbunny.mov");
            LiveStream returnedStream = liveStreamInsert.execute();
            returnedStream.set("video", "/resources/vid_bigbuckbunny.mov");
            
            // Print information from the API response.
            System.out.println("\n================== Returned Stream ==================\n");
            System.out.println("  - Id: " + returnedStream.getId());
            System.out.println("  - Title: " + returnedStream.getSnippet().getTitle());
            System.out.println("  - Description: " + returnedStream.getSnippet().getDescription());
            System.out.println("  - Published At: " + returnedStream.getSnippet().getPublishedAt());
            System.out.println("  - Ingestion Address: " + returnedStream.getCdn().getIngestionInfo().getIngestionAddress());
            
            
            // Construct and execute a request to bind the new broadcast
            // and stream.
            YouTube.LiveBroadcasts.Bind liveBroadcastBind =
                    youtube.liveBroadcasts().bind(returnedBroadcast.getId(), "id,contentDetails");
            liveBroadcastBind.setStreamId(returnedStream.getId());
            returnedBroadcast = liveBroadcastBind.execute();

            // Print information from the API response.
            System.out.println("\n================== Returned Bound Broadcast ==================\n");
            System.out.println("  - Broadcast Id: " + returnedBroadcast.getId());
            System.out.println(
                    "  - Bound Stream Id: " + returnedBroadcast.getContentDetails().getBoundStreamId());
            
            
            //call to upload code
//            String url =  returnedStream.getCdn().getIngestionInfo().getIngestionAddress(); 
//            String fileName = returnedStream.getCdn().getIngestionInfo().getStreamName() ;
//
//            IContainer container = IContainer.make();
//            IContainerFormat containerFormat_live = IContainerFormat.make();
//            containerFormat_live.setOutputFormat("flv", url + "/"+ fileName, null);
//            container.setInputBufferLength(0);
//            int retVal = container.open(url + "/"+ fileName, IContainer.Type.WRITE, containerFormat_live);
//            if (retVal < 0) {
//                System.err.println("Could not open output container for live stream");
//                System.exit(1);
//            }
//
//            Dimension size = WebcamResolution.QVGA.getSize();
//
//            IStream stream1 = container.addNewStream(0);
//            IStreamCoder coder = stream1.getStreamCoder();
//            ICodec codec = ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_H264);
//            coder.setNumPicturesInGroupOfPictures(4);
//            coder.setCodec(codec);
//            coder.setBitRate(500000);
//            coder.setPixelType(IPixelFormat.Type.YUV420P);
//            coder.setHeight(size.height);
//            coder.setWidth(size.width);
//
//            System.out.println("[ENCODER] video size is " + size.width + "x" + size.height);
//            coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
//            coder.setGlobalQuality(0);
//            IRational frameRate = IRational.make(24, 1);
//            coder.setFrameRate(frameRate);
//            coder.setTimeBase(IRational.make(frameRate.getDenominator(), frameRate.getNumerator()));
//
//            coder.open();
//            container.writeHeader();
//            long firstTimeStamp = System.currentTimeMillis();
//            long lastTimeStamp = -1;
//            int i = 0;
//            try {
//             //   Robot robot = new Robot();
//                Webcam webcam = Webcam.getDefault();
//                webcam.setViewSize(size);
//                webcam.open();
//
//                while (i < 180) {
//                    //long iterationStartTime = System.currentTimeMillis();
//                    long now = System.currentTimeMillis();
//                    //grab the screenshot
//                    BufferedImage image = webcam.getImage();
//                    //convert it for Xuggler
//                    BufferedImage currentScreenshot = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
//                    currentScreenshot.getGraphics().drawImage(image, 0, 0, null);
//                    //start the encoding process
//                    IPacket packet = IPacket.make();
//                    IConverter converter = ConverterFactory.createConverter(currentScreenshot, IPixelFormat.Type.YUV420P);
//                    long timeStamp = (now - firstTimeStamp) * 1000; 
//                    IVideoPicture outFrame = converter.toPicture(currentScreenshot, timeStamp);
//                    if (i == 0) {
//                        //make first frame keyframe
//                        outFrame.setKeyFrame(true);
//                    }
//                    outFrame.setQuality(0);
//                    coder.encodeVideo(packet, outFrame, 0);
//                    outFrame.delete();
//                    if (packet.isComplete()) {
//                        container.writePacket(packet);
//                        System.out.println("[ENCODER] writing packet of size " + packet.getSize() + " for elapsed time " + ((timeStamp - lastTimeStamp) / 1000));
//                        lastTimeStamp = timeStamp;
//                    }
//                    System.out.println("[ENCODER] encoded image " + i + " in " + (System.currentTimeMillis() - now));
//                    i++;
//                    try {
//                        Thread.sleep(Math.max((long) (1000 / frameRate.getDouble()) - (System.currentTimeMillis() - now), 0));
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            //end of uploading code
            
            
            //*****************//
            String url = returnedStream.getCdn().getIngestionInfo().getIngestionAddress();
            String fileName = returnedStream.getCdn().getIngestionInfo().getStreamName();
            ArrayList<IPacket> arrayList = new ArrayList<IPacket>();
            arrayList = callMyDataPacket(url, fileName);
            
            int framesToEncode = 280;
            int x = 0;
            int y = 0;
            int height = 480;
            int width = 640;
            
            int l = 0;
            while (l <= 100) {
            
            IContainer container = IContainer.make();
            IContainerFormat containerFormat_live = IContainerFormat.make();
            containerFormat_live.setOutputFormat("flv", url + "/" + fileName, null);
            container.setInputBufferLength(0);
            int retVal = container.open(url + "/" + fileName, IContainer.Type.WRITE, containerFormat_live);
            if (retVal < 0) {
                System.err.println("Could not open output container for live stream");
                System.exit(1);
            }
            IStream stream1 = container.addNewStream(0);
            IStreamCoder coder = stream1.getStreamCoder();
            ICodec codec = ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_H264);
            coder.setNumPicturesInGroupOfPictures(5);
            coder.setCodec(codec);
            coder.setBitRate(200000);
            coder.setPixelType(IPixelFormat.Type.YUV420P);
            coder.setHeight(height);
            coder.setWidth(width);
            System.out.println("[ENCODER] video size is " + width + "x" + height);
            coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
            coder.setGlobalQuality(0);
            IRational frameRate = IRational.make(24, 1);
            coder.setFrameRate(frameRate);
            coder.setTimeBase(IRational.make(frameRate.getDenominator(), frameRate.getNumerator()));
            Properties props = new Properties();
//            props.setProperty("x264opts", "bitrate=10500:qpmin=7:min-keyint=5:keyint=15:cabac=1:pass=1:stats=/home/surfdogisHappyDog/Downloads/my888file.stats:no-mbtree=1");
//            props.setProperty("tune", "film");
//            props.setProperty("preset", "slow");
            InputStream is = CreateBroadcast.class.getClass().getResourceAsStream("/resources/libx264-normal.ffpreset");
            try {
                props.load(is);
            } catch (IOException e) {
                System.err.println("You need the libx264-normal.ffpreset file from the Xuggle distribution in your classpath.");
                System.exit(1);
            }
            Configuration.configure(props, coder);
            coder.open();
            int retvall = container.writeHeader();
            if (retvall < 0)
                throw new RuntimeException("Could not write header for: .................." );
            long firstTimeStamp = System.currentTimeMillis();
            long lastTimeStamp = -1;
            int i = 0;
//            IPacket packet = null;
            try {
                Robot robot = new Robot();
//                while (i < framesToEncode) {
                    //long iterationStartTime = System.currentTimeMillis();
                    long now = System.currentTimeMillis();
                    //grab the screenshot
                    BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, width, height));
                    //convert it for Xuggler
                    BufferedImage currentScreenshot = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                    currentScreenshot.getGraphics().drawImage(image, 0, 0, null);
                    //start the encoding process
                    IPacket packet = IPacket.make();
                    IConverter converter = ConverterFactory.createConverter(currentScreenshot, IPixelFormat.Type.YUV420P);
                    long timeStamp = (now - firstTimeStamp) * 1000; 
                    IVideoPicture outFrame = converter.toPicture(currentScreenshot, timeStamp);
                    if (i == 0) {
                        //make first frame keyframe
                        outFrame.setKeyFrame(true);
                    }
                    outFrame.setQuality(0);
                    coder.encodeVideo(packet, outFrame, 0);
                    outFrame.delete();
                    if (arrayList.get(2).isComplete()) {
                    	packet.setStreamIndex(0);
                        container.writePacket(arrayList.get(2));
                        System.out.println("[ENCODER Packet Completed] writing packet of size " + arrayList.get(2).getSize() + " for elapsed time " + ((timeStamp - lastTimeStamp) / 1000));
                        lastTimeStamp = timeStamp;
                    }
                    System.out.println("[ENCODER] encoded image " + i + " in " + (System.currentTimeMillis() - now));
                    i++;
//                    try {
//                        Thread.sleep(Math.max((long) (1000 / frameRate.getDouble()) - (System.currentTimeMillis() - now), 0));
////                    	Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            } catch (AWTException e) {
                e.printStackTrace();
            }
            int retval = container.writeTrailer();
            if (retval < 0)
                throw new RuntimeException("Could not write trailer to output file");
            coder.close();
            container.close();
            //*****************//
    		l++;
    		}
            System.out.println("Stream Name : " + fileName);
            
        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
        
        
    }

    private static ArrayList<IPacket> callMyDataPacket(String url, String fileName) throws AWTException {
		// TODO Auto-generated method stub
    	ArrayList<IPacket> arrayList = new ArrayList<IPacket>();
        IContainer container = IContainer.make();
        IContainerFormat containerFormat_live = IContainerFormat.make();
        containerFormat_live.setOutputFormat("flv", url + "/" + fileName, null);
        container.setInputBufferLength(0);
        int retVal = container.open(url + "/" + fileName, IContainer.Type.WRITE, containerFormat_live);
        if (retVal < 0) {
            System.err.println("Could not open output container for live stream");
            System.exit(1);
        }
        IStream stream1 = container.addNewStream(0);
        IStreamCoder coder = stream1.getStreamCoder();
        ICodec codec = ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_H264);
        coder.setNumPicturesInGroupOfPictures(5);
        coder.setCodec(codec);
        coder.setBitRate(200000);
        coder.setPixelType(IPixelFormat.Type.YUV420P);
        coder.setHeight(480);
        coder.setWidth(640);
        System.out.println("[ENCODER] video size is " + 640 + "x" + 480);
        coder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        coder.setGlobalQuality(0);
        IRational frameRate = IRational.make(24, 1);
        coder.setFrameRate(frameRate);
        coder.setTimeBase(IRational.make(frameRate.getDenominator(), frameRate.getNumerator()));
        Properties props = new Properties();
//        props.setProperty("x264opts", "bitrate=10500:qpmin=7:min-keyint=5:keyint=15:cabac=1:pass=1:stats=/home/surfdogisHappyDog/Downloads/my888file.stats:no-mbtree=1");
//        props.setProperty("tune", "film");
//        props.setProperty("preset", "slow");
        InputStream is = CreateBroadcast.class.getClass().getResourceAsStream("/resources/libx264-normal.ffpreset");
        try {
            props.load(is);
        } catch (IOException e) {
            System.err.println("You need the libx264-normal.ffpreset file from the Xuggle distribution in your classpath.");
            System.exit(1);
        }
        Configuration.configure(props, coder);
        coder.open();
        int retvall = container.writeHeader();
        if (retvall < 0)
            throw new RuntimeException("Could not write header for: .................." );
        long firstTimeStamp = System.currentTimeMillis();
        long lastTimeStamp = -1;
        int i = 0;
    	Robot robot = new Robot();
      while (i < 60) {
        //long iterationStartTime = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        //grab the screenshot
        BufferedImage image = robot.createScreenCapture(new Rectangle(0, 0, 640, 480));
        //convert it for Xuggler
        BufferedImage currentScreenshot = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        currentScreenshot.getGraphics().drawImage(image, 0, 0, null);
        //start the encoding process
        IPacket packet = IPacket.make();
        IConverter converter = ConverterFactory.createConverter(currentScreenshot, IPixelFormat.Type.YUV420P);
        long timeStamp = (now - firstTimeStamp) * 1000; 
        IVideoPicture outFrame = converter.toPicture(currentScreenshot, timeStamp);
        if (i == 0) {
            //make first frame keyframe
            outFrame.setKeyFrame(true);
        }
        outFrame.setQuality(0);
        coder.encodeVideo(packet, outFrame, 0);
        outFrame.delete();
        if (packet.isComplete()) {
        	packet.setStreamIndex(0);
            container.writePacket(packet);
            System.out.println("[ENCODER Packet Completed] writing packet of size " + packet.getSize() + " for elapsed time " + ((timeStamp - lastTimeStamp) / 1000));
            lastTimeStamp = timeStamp;
            arrayList.add(packet);
        }
        System.out.println("[ENCODER] encoded image " + i + " in " + (System.currentTimeMillis() - now));
        i++;
        try {
            Thread.sleep(Math.max((long) (1000 / frameRate.getDouble()) - (System.currentTimeMillis() - now), 0));
//        	Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
      int retval = container.writeTrailer();
      if (retval < 0)
          throw new RuntimeException("Could not write trailer to output file");
	return arrayList;
	}

	/*
     * Prompt the user to enter a title for a broadcast.
     */
    private static String getBroadcastTitle() throws IOException {

        String title = "";

        System.out.print("Please enter a broadcast title: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        title = bReader.readLine();

        if (title.length() < 1) {
            // Use "New Broadcast" as the default title.
            title = "New Broadcast";
        }
        return title;
    }

    /*
     * Prompt the user to enter a title for a stream.
     */
    private static String getStreamTitle() throws IOException {

        String title = "";

        System.out.print("Please enter a stream title: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        title = bReader.readLine();

        if (title.length() < 1) {
            // Use "New Stream" as the default title.
            title = "New Stream";
        }
        return title;
    }

}