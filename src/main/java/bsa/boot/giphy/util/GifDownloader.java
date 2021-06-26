package bsa.boot.giphy.util;

import bsa.boot.giphy.dto.GifDto;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GifDownloader {

  private static final Logger logger = LoggerFactory.getLogger(GifDownloader.class);

  @Autowired
  GifDownloader() {
  }

  public void download(String folder, GifDto gifDto) {
    ReadableByteChannel readableChannelForHttpResponseBody = null;
    FileChannel fileChannelForDownloadedFile = null;

    try {
      URL url = gifDto.getUrl();
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

      readableChannelForHttpResponseBody = Channels.newChannel(urlConnection.getInputStream());

      FileOutputStream fosForDownloadedFile = new FileOutputStream(folder + "/" +
          gifDto.getName() + ".gif");
      fileChannelForDownloadedFile = fosForDownloadedFile.getChannel();

      fileChannelForDownloadedFile.transferFrom(readableChannelForHttpResponseBody,
          0, Long.MAX_VALUE);
    } catch (IOException ioException) {
      logger.error("IOException occurred while contacting server.");
    } finally {
      if (readableChannelForHttpResponseBody != null) {
        try {
          readableChannelForHttpResponseBody.close();
        } catch (IOException ioe) {
          logger.error("Error while closing response body channel");
        }
      }
      if (fileChannelForDownloadedFile != null) {
        try {
          fileChannelForDownloadedFile.close();
        } catch (IOException ioe) {
          logger.error("Error while closing file channel for downloaded file");
        }
      }
    }
  }
}
