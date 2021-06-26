package bsa.boot.giphy.util;

import bsa.boot.giphy.dto.GifDto;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParserJsonGifToGifDto {

  @Autowired
  ParserJsonGifToGifDto() {
  }

  public Optional<GifDto> parse(JSONObject jsonGif, String query) {
    JSONArray data = (JSONArray) jsonGif.get("data");
    JSONObject images = (JSONObject) ((JSONObject) data.get(0)).get("images");
    String name = ((JSONObject) data.get(0)).get("id").toString();
    try {
      StringBuilder stringUrl = new StringBuilder(
          ((JSONObject) images.get("original")).get("url").toString());
      stringUrl.replace(8, 14, "i");
      URL url = new URL(stringUrl.toString());
      GifDto gifDto = new GifDto(name, query, url);
      return Optional.of(gifDto);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }
}
