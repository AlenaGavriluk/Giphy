package bsa.boot.giphy.configuration;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ApiConfiguration {

  @Bean
  @Scope(value = "prototype") // singleton by default
  public HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }

}
