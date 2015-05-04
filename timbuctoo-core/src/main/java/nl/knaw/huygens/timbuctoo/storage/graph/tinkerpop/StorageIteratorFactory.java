package nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop;

import java.util.Iterator;

import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Relation;
import nl.knaw.huygens.timbuctoo.storage.StorageException;
import nl.knaw.huygens.timbuctoo.storage.StorageIterator;
import nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop.conversion.ElementConverterFactory;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

class StorageIteratorFactory {

  private final ElementConverterFactory elementConverterFactory;

  public StorageIteratorFactory(ElementConverterFactory elementConverterFactory) {
    this.elementConverterFactory = elementConverterFactory;
  }

  public <T extends Entity> StorageIterator<T> create(Class<T> type, Iterator<Vertex> iterator) throws StorageException {
    VertexConverter<T> converter = elementConverterFactory.forType(type);

    return new TinkerpopIterator<T, Vertex>(converter, iterator);
  }

  public <T extends Relation> StorageIterator<T> createForRelation(Class<T> type, Iterable<Edge> edges) {
    EdgeConverter<T> converter = elementConverterFactory.forRelation(type);

    return new TinkerpopIterator<T, Edge>(converter, edges.iterator());
  }

}
