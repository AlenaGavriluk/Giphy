package bsa.boot.giphy.util;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParserCsvToJson {

  @Autowired
  ParserCsvToJson() {
  }

  public JSONArray parse(File file) {
    JSONArray result = new JSONArray();
    try {
      List<String> lines = Files.readAllLines(file.toPath());
      lines.forEach(line -> {
            String[] lineComponents = line.split(",");

            JSONObject lineJson = new JSONObject();
            lineJson.put("date", lineComponents[0]);
            lineJson.put("query", lineComponents[1]);
            lineJson.put("gif", lineComponents[2]);

            result.add(lineJson);
          }
      );
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

}
