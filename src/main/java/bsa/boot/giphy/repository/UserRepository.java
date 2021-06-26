package bsa.boot.giphy.repository;

import bsa.boot.giphy.dto.GifDto;
import bsa.boot.giphy.util.GifDownloader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
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
public class UserRepository {

  private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
  private final GifDownloader gifDownloader;

  @Getter
  private final File BSA_GIPHY_FOLDER = new File("bsa_giphy");
  @Getter
  private final File USER_FOLDER = new File("bsa_giphy/user");

  @Autowired
  UserRepository(GifDownloader gifDownloader) {
    this.gifDownloader = gifDownloader;
  }

  public File createUserFolder(String id) {
    if (!BSA_GIPHY_FOLDER.exists()) {
      BSA_GIPHY_FOLDER.mkdir();
    }
    if (!USER_FOLDER.exists()) {
      USER_FOLDER.mkdir();
    }
    File userIdFolder = new File(USER_FOLDER + "/" + id);
    if (!userIdFolder.exists()) {
      userIdFolder.mkdir();
    }
    createUserHistoryFile(id);
    return userIdFolder;
  }

  public void createUserHistoryFile(String userId) {
    File history = new File(USER_FOLDER + "/" + userId + "/" + "history.csv");
    try {
      history.createNewFile();
    } catch (IOException e) {
      logger.error("Cannot create history.csv for user");
    }
  }

  public String save(GifDto gif, String id) {
    String folder = createUserFolder(id).getPath();
    File userQueryFolder = new File(folder + File.separator + gif.getQuery());
    if (!userQueryFolder.exists()) {
      userQueryFolder.mkdir();
    }
    gifDownloader.download(userQueryFolder.getPath(), gif);
    return folder + File.separator + gif.getQuery() + File.separator + gif.getName();
  }

  public Optional<JSONArray> getAll(String userId) {
    JSONArray userGifs = new JSONArray();
    File userIdFolder = new File(USER_FOLDER + File.separator + userId);
    if (!userIdFolder.exists()) {
      logger.warn("Folder with these user_id dose not exist");
      return Optional.empty();
    }
    File[] queries = userIdFolder.listFiles();
    for (File queryFolder : queries) {
      if (queryFolder.isFile()) {  //It may by history.csv file
        continue;
      }
      JSONObject queryJson = new JSONObject();
      queryJson.put("query", queryFolder.getName());
      JSONArray gifsArray = new JSONArray();
      File[] queryGifs = queryFolder.listFiles();
      for (File gif : queryGifs) {
        gifsArray.add(gif.getAbsolutePath());
      }
      queryJson.put("gifs", gifsArray);
      userGifs.add(queryJson);
    }
    return Optional.of(userGifs);
  }

  public Optional<String> getOneRandomGifByQuery(String query, String userId) {
    File file = new File(USER_FOLDER + File.separator + userId + File.separator + query);
    if (!file.exists()) {
      logger.warn("No such query in userFolder");
      return Optional.empty();
    }
    File[] queryGifs = file.listFiles();
    String randomGif = queryGifs[new Random().nextInt(queryGifs.length)].getAbsolutePath();
    return Optional.of(randomGif);
  }

  public void cleanUserFolder(String userId) {
    File file = new File(USER_FOLDER + File.separator + userId);
    Arrays.stream(Objects.requireNonNull(file.listFiles())).flatMap(file1 ->
        Arrays.stream(Objects.requireNonNull(file1.listFiles())))
        .forEach(File::delete);
    Arrays.stream(Objects.requireNonNull(file.listFiles())).forEach(File::delete);
  }
}
