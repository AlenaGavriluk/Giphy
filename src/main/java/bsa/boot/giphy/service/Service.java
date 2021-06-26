package bsa.boot.giphy.service;

import bsa.boot.giphy.cache.MemoryCache;
import bsa.boot.giphy.dto.GifDto;
import bsa.boot.giphy.dto.QueryDto;
import bsa.boot.giphy.repository.CacheRepository;
import bsa.boot.giphy.repository.UserRepository;
import bsa.boot.giphy.util.ParserCsvToJson;
import bsa.boot.giphy.util.ParserJsonGifToGifDto;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class Service {

  private static final Logger logger = LoggerFactory.getLogger(Service.class);

  private final HttpClient client;
  private final ParserJsonGifToGifDto parserJsonGifToGifDto;
  private final CacheRepository cacheRepository;
  private final UserRepository userRepository;
  private final MemoryCache memoryCache;
  private final ParserCsvToJson parserCsvToJson;

  @Value("${api.giphy-search-url}")
  private String apiGihpySearchUrl;

  @Value("${api.giphy-key}")
  private String apiGihpyKey;

  @Autowired
  Service(HttpClient client, ParserJsonGifToGifDto parserJsonGifToGifDto,
      CacheRepository cacheRepository, UserRepository userRepository,
      MemoryCache memoryCache, ParserCsvToJson parserCsvToJson) {
    this.client = client;
    this.parserJsonGifToGifDto = parserJsonGifToGifDto;
    this.cacheRepository = cacheRepository;
    this.userRepository = userRepository;
    this.memoryCache = memoryCache;
    this.parserCsvToJson = parserCsvToJson;
  }

  public boolean generateGifToCache(QueryDto query) {
    Optional<GifDto> gif = generateGif(query);
    if (gif.isPresent()) {
      cacheRepository.save(gif.get());
      return true;
    } else {
      logger.error("Cannot generate gif");
      return false;
    }
  }

  public String generateGifForUserForce(QueryDto query, String userId) {
    Optional<GifDto> gif = generateGif(query);
    if (gif.isPresent()) {
      cacheRepository.save(gif.get());
      String path = userRepository.save(gif.get(), userId);
      memoryCache.add(userId, query.getQuery(), path);
      return path;
    } else {
      logger.error("Cannot generate gif");
      return "";
    }
  }

  public String generateGifForUser(QueryDto query, String userId) {
    Optional<String> optional = cacheRepository.getOneRandomGifForUser(query.getQuery(), userId);
    if (optional.isPresent()) {
      String path = optional.get();
      memoryCache.add(userId, query.getQuery(), path);
      return path;
    } else {
      return generateGifForUserForce(query, userId);
    }
  }


  private Optional<GifDto> generateGif(QueryDto query) {
    logger.info("Searching on Giphy with query \"" + query.getQuery() + "\"");
    try {
      var response = client.send(buildGetRequest(query.getQuery()),
          HttpResponse.BodyHandlers.ofString());
      String stringRequestFromGiphy = response.body();
      JSONParser parser = new JSONParser();
      JSONObject jsonData = (JSONObject) parser.parse(stringRequestFromGiphy);
      return Optional.of(parserJsonGifToGifDto.parse(jsonData, query.getQuery()).get());
    } catch (ParseException | IOException | InterruptedException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  private HttpRequest buildGetRequest(String query) {
    String url = buildUrlWithParams(query);
    return HttpRequest
        .newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();
  }

  private String buildUrlWithParams(String query) {
    StringBuilder builder = new StringBuilder();
    builder.append(apiGihpySearchUrl)
        .append("?")
        .append("q=").append(query).append("&")
        .append("api_key=").append(apiGihpyKey).append("&")
        .append("limit=").append("1");
    return builder.toString();
  }

  public ResponseEntity<?> getCache() {
    logger.info("Getting cache");
    Optional<?> optional = cacheRepository.getAll();
    if (optional.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(optional.get());
    } else {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
  }

  public ResponseEntity<?> getFromCacheByQuery(String query) {
    logger.info("Getting gif from cache by query " + "\"" + query + "\"");
    Optional<?> optional = cacheRepository.getByQuery(query);
    if (optional.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(optional.get());
    } else {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
  }

  public void deleteCache() {
    File cacheFolder = new File("bsa_giphy/cache");
    if (cacheFolder.exists()) {
      Arrays.stream(Objects.requireNonNull(cacheFolder.listFiles())).flatMap(file -> Arrays.stream(
          Objects.requireNonNull(file.listFiles()))).forEach(
          File::delete);
      Arrays.stream(cacheFolder.listFiles()).forEach(File::delete);
      cacheFolder.delete();
    }
  }

  public Optional<JSONArray> getAllGifs() {
    logger.info("Getting gifs from cache and users without query");
    JSONArray gifsArray = new JSONArray();
    File cacheFolder = new File("bsa_giphy/cache");
    File usersFolder = new File("bsa_giphy/users");
    if (!cacheFolder.exists() && !usersFolder.exists()) {
      logger.warn("Cache and users are empty");
      return Optional.empty();
    }
    Set<String> gifsSet = new HashSet<>();
    if (cacheFolder.exists()) {
      File[] queryGifs = cacheFolder.listFiles();
      Arrays.stream(queryGifs).forEach(file -> gifsSet.add(file.getAbsolutePath()));
    }
    if (usersFolder.exists()) {
      File[] userGifs = cacheFolder.listFiles();
      Arrays.stream(userGifs).forEach(file -> gifsSet.add(file.getAbsolutePath()));
    }
    gifsArray.addAll(gifsSet);
    return Optional.of(gifsArray);
  }

  public ResponseEntity<?> getAllFromUser(String userId) {
    Optional<?> optional = userRepository.getAll(userId);
    if (optional.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(optional.get());
    } else {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
  }

  public void addToUserHistory(String userId, String query, String path) {
    try (FileWriter writer = new FileWriter(new File(userRepository.getUSER_FOLDER()
        + File.separator + userId + "/history.csv"), true)) {

      StringBuilder sb = new StringBuilder();
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
      LocalDateTime now = LocalDateTime.now();
      sb.append(dtf.format(now));
      sb.append(',');
      sb.append(query);
      sb.append(',');
      sb.append(path);
      sb.append(System.lineSeparator());

      writer.write(sb.toString());

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public ResponseEntity<?> getUserHistory(String userId) {
    File historyFile = new File(userRepository.getUSER_FOLDER() + File.separator
        + userId + File.separator + "history.csv");
    if (!historyFile.exists()) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
          .body("History for these user dose not exist");
    } else {
      return ResponseEntity.status(HttpStatus.OK).body(parserCsvToJson.parse(historyFile));
    }
  }

  public void cleanHistory(String userId) {
    File historyFile = new File(userRepository.getUSER_FOLDER() + File.separator
        + userId + File.separator + "history.csv");
    if (!historyFile.exists()) {
      logger.warn("Can not clean history, history.csv file dose not exist");
    } else {
      historyFile.delete();
      userRepository.createUserHistoryFile(userId);
    }
  }

  public ResponseEntity<?> searchGifForce(String userId, String query) {
    Optional<String> optional = userRepository.getOneRandomGifByQuery(query, userId);
    if (!optional.isPresent()) {
      optional = cacheRepository.getOneRandomGifForUser(query, userId);
    }
    if (optional.isPresent()) {
      String gif = optional.get();
      memoryCache.add(userId, query, gif);
      return ResponseEntity.status(HttpStatus.OK).body(gif);
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  public Optional<String> searchInMemoryCache(String userId, String query) {
    return memoryCache.getGif(userId, query);
  }

  public void resetMemoryCache(String userId, String query) {
    if (query == null) {
      memoryCache.resetUser(userId);
    } else {
      memoryCache.resetUser(userId, query);
    }
  }

  public void cleanUserFolder(String id) {
    userRepository.cleanUserFolder(id);
  }


}
