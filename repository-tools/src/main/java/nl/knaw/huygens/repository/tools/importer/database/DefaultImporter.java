package nl.knaw.huygens.repository.tools.importer.database;

import java.io.IOException;

import nl.knaw.huygens.repository.config.DocTypeRegistry;
import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.model.DocumentRef;
import nl.knaw.huygens.repository.storage.StorageManager;

public abstract class DefaultImporter {

  protected final DocTypeRegistry docTypeRegistry;
  private final StorageManager storageManager;

  private String prevMessage;
  private int errors;

  public DefaultImporter(DocTypeRegistry registry, StorageManager storageManager) {
    this.docTypeRegistry = registry;
    this.storageManager = storageManager;
    prevMessage = "";
    errors = 0;
  }

  // --- error handling ------------------------------------------------

  protected void handleError(String format, Object... args) {
    errors++;
    String message = String.format(format, args);
    if (!message.equals(prevMessage)) {
      System.out.print("## ");
      System.out.printf(message);
      System.out.println();
      prevMessage = message;
    }
  }

  protected void displayErrorSummary() {
    if (errors > 0) {
      System.out.printf("%n## Error count = %d%n", errors);
    }
  }

  // --- storage -------------------------------------------------------

  protected <T extends Document> T getDocument(Class<T> type, String id) {
    return storageManager.getDocument(type, id);
  }

  protected <T extends Document> String addDocument(Class<T> type, T document, boolean isComplete) {
    try {
      storageManager.addDocumentWithoutPersisting(type, document, isComplete);
      return document.getId();
    } catch (IOException e) {
      handleError("Failed to add %s; %s", document.getDisplayName(), e.getMessage());
      return null;
    }
  }

  protected <T extends Document> T modDocument(Class<T> type, T document) {
    try {
      storageManager.modifyDocumentWithoutPersisting(type, document);
      return document;
    } catch (IOException e) {
      handleError("Failed to modify %s; %s", document.getDisplayName(), e.getMessage());
      return null;
    }
  }

  // -------------------------------------------------------------------

  protected <T extends Document> DocumentRef newDocumentRef(Class<T> type, T document) {
    String itype = docTypeRegistry.getINameForType(type);
    String xtype = docTypeRegistry.getXNameForType(type);
    return new DocumentRef(itype, xtype, document.getId(), document.getDisplayName());
  }

  protected <T extends Document> DocumentRef newDocumentRef(Class<T> type, String id) {
    String itype = docTypeRegistry.getINameForType(type);
    String xtype = docTypeRegistry.getXNameForType(type);
    return new DocumentRef(itype, xtype, id, null);
  }

}
