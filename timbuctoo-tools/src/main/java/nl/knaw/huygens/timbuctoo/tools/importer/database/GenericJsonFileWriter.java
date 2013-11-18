package nl.knaw.huygens.timbuctoo.tools.importer.database;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nl.knaw.huygens.timbuctoo.model.Entity;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

public class GenericJsonFileWriter extends GenericDataHandler {
  private final String testDataDir;

  public GenericJsonFileWriter(String testDataDir) {
    super();
    this.testDataDir = testDataDir;
  }

  @Override
  protected <T extends Entity> void save(Class<T> type, List<T> objects) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    //Make sure the type is added to the json.
    mapper.enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE, As.PROPERTY);

    File file = new File(testDataDir + type.getSimpleName() + ".json");
    System.out.println("file: " + file.getAbsolutePath());

    mapper.writeValue(file, objects);
  }

}
