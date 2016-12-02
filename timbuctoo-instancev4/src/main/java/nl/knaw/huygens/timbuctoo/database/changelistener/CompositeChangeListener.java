package nl.knaw.huygens.timbuctoo.database.changelistener;

import com.google.common.collect.Sets;
import nl.knaw.huygens.timbuctoo.database.dto.dataset.Collection;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Optional;
import java.util.Set;

public class CompositeChangeListener implements ChangeListener {

  private final Set<ChangeListener> subListeners;

  public CompositeChangeListener(ChangeListener... listeners) {
    subListeners = Sets.newHashSet(listeners);
  }

  @Override
  public void onCreate(Collection collection, Vertex vertex) {
    subListeners.forEach(l -> l.onCreate(collection, vertex));
  }

  @Override
  public void onPropertyUpdate(Collection collection, Optional<Vertex> oldVertex, Vertex newVertex) {
    subListeners.forEach(l -> l.onPropertyUpdate(collection, oldVertex, newVertex));
  }

  @Override
  public void onRemoveFromCollection(Collection collection, Optional<Vertex> oldVertex, Vertex newVertex) {
    subListeners.forEach(l -> l.onRemoveFromCollection(collection, oldVertex, newVertex));
  }

  @Override
  public void onAddToCollection(Collection collection, Optional<Vertex> oldVertex, Vertex newVertex) {
    subListeners.forEach(l -> l.onAddToCollection(collection, oldVertex, newVertex));
  }
}
