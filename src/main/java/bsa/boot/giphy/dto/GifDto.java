package bsa.boot.giphy.dto;

import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GifDto {

  private final String name;
  private final String query;
  private final URL url;
}
