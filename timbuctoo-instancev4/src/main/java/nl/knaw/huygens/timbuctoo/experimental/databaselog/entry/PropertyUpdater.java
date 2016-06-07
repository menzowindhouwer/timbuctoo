package nl.knaw.huygens.timbuctoo.experimental.databaselog.entry;

import com.google.common.collect.Sets;
import nl.knaw.huygens.timbuctoo.experimental.databaselog.DatabaseLog;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class PropertyUpdater {
  private final Element element;
  private final Element prevElement;
  private final Set<String> newKeys;
  private final Set<String> oldKeys;

  public PropertyUpdater(Element element, Element prevElement, Set<String> propertiesToIgnore) {
    this.element = element;
    this.prevElement = prevElement;
    this.newKeys = getKeys(element, propertiesToIgnore);
    this.oldKeys = getKeys(prevElement, propertiesToIgnore);
  }

  private Set<String> getKeys(Element element, Set<String> propertiesToIgnore) {
    return element.keys().stream().filter(key -> !propertiesToIgnore.contains(key)).collect(Collectors.toSet());
  }

  private void addNewProperties(DatabaseLog dbLog) {
    Set<String> newProperties = Sets.difference(newKeys, oldKeys);
    newProperties.forEach(key -> dbLog.newProperty(element.property(key)));
  }

  private void updateExistingProperties(DatabaseLog dbLog) {
    Set<String> existing = Sets.intersection(newKeys, oldKeys);
    existing.forEach(key -> {
      Property<Object> latestProperty = element.property(key);
      if (!Objects.equals(latestProperty.value(), prevElement.value(key))) {
        dbLog.updateProperty(latestProperty);
      }
    });
  }

  private void removeDeletedProperties(DatabaseLog dbLog) {
    Set<String> deletedProperties = Sets.difference(oldKeys, newKeys);
    deletedProperties.forEach(dbLog::deleteProperty);
  }

  public void updateProperties(DatabaseLog dbLog) {
    addNewProperties(dbLog);
    updateExistingProperties(dbLog);
    removeDeletedProperties(dbLog);
  }
}
