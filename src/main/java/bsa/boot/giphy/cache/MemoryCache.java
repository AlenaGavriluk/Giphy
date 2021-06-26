package bsa.boot.giphy.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class MemoryCache {

  private static MemoryCache uniqInstance;
  private final Map<String, Map<String, ArrayList<String>>> map;

  private MemoryCache() {
    map = new HashMap<>();
  }

  public static MemoryCache getInstance() {
    if (uniqInstance == null) {
      uniqInstance = new MemoryCache();
    }

    return uniqInstance;
  }

  public Map<String, Map<String, ArrayList<String>>> getCacheMap() {
    return this.map;
  }

  public boolean contains(String userId, String query, String gifPath) {
    return (this.map.containsKey(userId) && this.map.get(userId).containsKey(query) &&
        this.map.get(userId).get(query).contains(gifPath));
  }

  public void add(String userId, String query, String gifPath) {
    if (this.contains(userId, query, gifPath)) {
      return;
    }

    var tempList = new ArrayList<String>();

    if (this.map.containsKey(userId)) {
      if (this.map.get(userId).containsKey(query)) {
        tempList = this.map.get(userId).get(query);
        tempList.add(tempList.size(), gifPath);
        this.map.get(userId).put(query, tempList);
      } else {
        tempList.add(gifPath);
        this.map.get(userId).put(query, tempList);
      }
    } else {
      var userMap = new HashMap<String, ArrayList<String>>();
      tempList.add(gifPath);
      userMap.put(query, tempList);
      this.map.put(userId, userMap);
    }
  }

  public Optional<String> getGif(String userId, String query) {
    if (this.map.containsKey(userId) && this.map.get(userId).containsKey(query)) {
      var queriedList = this.map.get(userId).get(query);
      return Optional.of(queriedList.get(new Random().nextInt(queriedList.size())));
    }
    return Optional.empty();
  }

  public void resetUser(String userId) {
    this.map.remove(userId);
  }

  public void resetUser(String userId, String query) {
    if (this.map.containsKey(userId)) {
      this.map.get(userId).remove(query);
    }
  }
}
