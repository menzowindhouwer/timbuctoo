package nl.knaw.huygens.repository.tools.importer.database;

import java.io.IOException;
import java.util.List;

import nl.knaw.huygens.repository.model.Entity;
import nl.knaw.huygens.repository.storage.StorageManager;
import nl.knaw.huygens.repository.tools.util.Progress;

public class GenericImporter extends GenericDataHandler {

  protected StorageManager storageManager;

  public GenericImporter(StorageManager storageManager) {
    this.storageManager = storageManager;
  }

  @Override
  protected <T extends Entity> void save(Class<T> type, List<T> objects) throws IOException {
    Progress progress = new Progress();
    for (T object : objects) {
      progress.step();
      storageManager.addEntity(type, object);
    }
    progress.done();
  }

}
