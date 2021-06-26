package bsa.boot.giphy.validator;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserIdValidator {

  @Autowired
  UserIdValidator() {
  }

  public Boolean isValidFilePath(String id) {
    try {
      Paths.get(id);
    } catch (InvalidPathException e) {
      return false;
    }
    return true;
  }
}
