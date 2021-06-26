package bsa.boot.giphy.controllers;


import bsa.boot.giphy.dto.QueryDto;
import bsa.boot.giphy.service.Service;
import bsa.boot.giphy.validator.UserIdValidator;
import java.util.Optional;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

  private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

  @Autowired
  Service service;
  @Autowired
  UserIdValidator userIdValidator;

  @PostMapping("/cache/generate")
  public ResponseEntity<?> GenerateGif(@RequestBody QueryDto query,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    if (service.generateGifToCache(query)) {
      return ResponseEntity.status(HttpStatus.OK)
          .body((service.getFromCacheByQuery(query.getQuery()).getBody()));
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }

  @GetMapping("cache")
  public ResponseEntity<?> GetCache(@RequestParam(required = false) QueryDto query,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    ResponseEntity<?> responseEntity = query == null ?
        service.getCache() : service.getFromCacheByQuery(query.getQuery());
    return responseEntity;
  }

  @DeleteMapping("cache/delete")
  public ResponseEntity<?> DeleteCache(
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    service.deleteCache();
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @GetMapping("gifs/get")
  public ResponseEntity<?> GetAllGifs(@RequestHeader(value = "X-BSA-GIPHY", required = false)
      String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    Optional<JSONArray> optional = service.getAllGifs();
    if (optional.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(optional.get());
    } else {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Cache is empty");
    }
  }

  @GetMapping("user/{id}/all")
  public ResponseEntity<?> GetAllGifsFromUser(@PathVariable String id,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    return service.getAllFromUser(id);
  }

  @PostMapping("user/{id}/generate")
  public ResponseEntity<?> GenerateGifByUserId(@PathVariable String id,
      @RequestBody QueryDto query, boolean force,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    if (!userIdValidator.isValidFilePath(id)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("User_id mast be valid path in OS");
    } else {
      String newGif;
      if (force) {
        newGif = service.generateGifForUserForce(query, id);
      } else {
        newGif = service.generateGifForUser(query, id);
      }
      if (newGif.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      } else {
        service.addToUserHistory(id, query.getQuery(), newGif);
        return ResponseEntity.status(HttpStatus.OK).body(newGif);
      }
    }
  }

  @GetMapping("user/{id}/history")
  public ResponseEntity<?> getHistory(@PathVariable String id,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    return service.getUserHistory(id);
  }

  @DeleteMapping("user/{id}/history/clean")
  public void cleanHistory(@PathVariable String id) {
    service.cleanHistory(id);
  }

  @GetMapping("user/{id}/search")
  public ResponseEntity<?> searchGif(@RequestParam QueryDto query,
      @RequestParam boolean force,
      @PathVariable String id,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    if (force) {
      return service.searchGifForce(id, query.getQuery());
    } else {
      Optional<String> optional = service.searchInMemoryCache(id, query.getQuery());
      if (optional.isPresent()) {
        return ResponseEntity.status(HttpStatus.OK).body(optional.get());
      } else {
        return service.searchGifForce(id, query.getQuery());
      }
    }
  }

  @DeleteMapping("user/{id}/reset")
  public ResponseEntity<?> resetMemoryCache(@RequestParam(required = false) QueryDto query,
      @PathVariable String id,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    service.resetMemoryCache(id, query.getQuery());
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @DeleteMapping("user/{id}/clean")
  public ResponseEntity<?> resetMemoryCache(@PathVariable String id,
      @RequestHeader(value = "X-BSA-GIPHY", required = false) String header) {
    if (header == null) {
      return ResponseEntity.status(403).build();
    }
    service.resetMemoryCache(id, null);
    service.cleanUserFolder(id);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
