package nl.knaw.huygens.repository.index;

import nl.knaw.huygens.repository.model.Entity;

// T must be a base type
public interface DocumentIndexer<T extends Entity> {

  void add(Class<T> docType, String docId) throws IndexException;

  void modify(Class<T> docType, String docId) throws IndexException;

  void remove(String docId) throws IndexException;

  void removeAll() throws IndexException;

  void flush() throws IndexException;

}
