package bsa.boot.giphy.repository;

import bsa.boot.giphy.dto.GifDto;
import bsa.boot.giphy.util.GifDownloader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Random;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CacheRepository {

  private static final Logger logger = LoggerFactory.getLogger(CacheRepository.class);
  private final GifDownloader gifDownloader;

  @Getter
  private final File BSA_GIPHY_FOLDER = new File("bsa_giphy");
  @Getter
  private final File CACHE_FOLDER = new File("bsa_giphy/cache");

  @Autowired
  CacheRepository(GifDownloader gifDownloader) {
    this.gifDownloader = gifDownloader;
  }

  public void save(GifDto gifDto) {
    if (!BSA_GIPHY_FOLDER.exists()) {
      BSA_GIPHY_FOLDER.mkdir();
    }

    if (!CACHE_FOLDER.exists()) {
      CACHE_FOLDER.mkdir();
    }
    File queryFolder = new File(CACHE_FOLDER + File.separator + gifDto.getQuery());
    if (!queryFolder.exists()) {
      queryFolder.mkdir();
    }
    gifDownloader.download(queryFolder.getPath(), gifDto);
  }

  public Optional<JSONArray> getAll() {
    if (!CACHE_FOLDER.exists()) {
      logger.warn("Cache is empty");
      return Optional.empty();
    }
    File[] queries = CACHE_FOLDER.listFiles();
    JSONArray cacheJson = new JSONArray();
    for (File query : queries) {
      JSONObject queryJson = getByQuery(query.getName()).get();
      cacheJson.add(queryJson);
    }
    return Optional.of(cacheJson);
  }

  public Optional<JSONObject> getByQuery(String query) {
    JSONObject queryJson = new JSONObject();
    queryJson.put("query", query);
    JSONArray gifsArray = new JSONArray();
    File file = new File(CACHE_FOLDER + File.separator + query);
    if (!file.exists()) {
      logger.warn("No such query in cache");
      return Optional.empty();
    }
    File[] queryGifs = file.listFiles();
    for (File gif : queryGifs) {
      gifsArray.add(gif.getAbsolutePath());
    }
    queryJson.put("gifs", gifsArray);
    return Optional.of(queryJson);
  }

  public Optional<String> getOneRandomGifForUser(String query, String userId) {
    File file = new File(CACHE_FOLDER + File.separator + query);
    if (!file.exists()) {
      logger.warn("No such query in cache");
      return Optional.empty();
    }
    File[] queryGifs = file.listFiles();
    Path randomGif = queryGifs[new Random().nextInt(queryGifs.length)].toPath();
    File userFolder = new File("bsa_giphy/user");
    if (!userFolder.exists()) {
      userFolder.mkdir();
    }
    File userIdFolder = new File("user/" + userId);
    if (!userIdFolder.exists()) {
      userIdFolder.mkdir();
    }
    File userQueryFolder = new File(userIdFolder + File.separator + query);
    if (!userQueryFolder.exists()) {
      userQueryFolder.mkdir();
      System.out.println(userQueryFolder.getPath());
    }
    Path userFile = Path.of(userQueryFolder + File.separator + randomGif.getFileName());
    try {
      Files.copy(randomGif, userFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      logger.error("cannot copy gif file from cache");
      return Optional.empty();
    }
    return Optional.of(userFile.toString());
  }
}


