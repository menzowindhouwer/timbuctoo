package nl.knaw.huygens.timbuctoo.storage;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.knaw.huygens.timbuctoo.config.BusinessRules;
import nl.knaw.huygens.timbuctoo.config.Configuration;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.model.Relation;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.model.SearchResult;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;
import nl.knaw.huygens.timbuctoo.model.User;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.util.KV;
import nl.knaw.huygens.timbuctoo.vre.Scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StorageManager {

  private static final Logger LOG = LoggerFactory.getLogger(StorageManager.class);

  private final Configuration config;
  private final Storage storage;

  @Inject
  public StorageManager(Configuration config, Storage storage) {
    this.config = config;
    this.storage = storage;
  }

  /**
   * Closes the data store.
   */
  public void close() {
    storage.close();
  }

  // -------------------------------------------------------------------

  public StorageStatus getStatus() {
    StorageStatus status = new StorageStatus();

    Scope scope = config.getScopes().get(0);
    for (Class<? extends DomainEntity> type : scope.getBaseEntityTypes()) {
      status.addDomainEntityCount(getCount(type));
    }

    status.addSystemEntityCount(getCount(RelationType.class));
    status.addSystemEntityCount(getCount(SearchResult.class));
    status.addSystemEntityCount(getCount(User.class));

    return status;
  }

  private KV<Long> getCount(Class<? extends Entity> type) {
    return new KV<Long>(type.getSimpleName(), storage.count(type));
  }

  // --- add entities --------------------------------------------------

  public <T extends SystemEntity> String addSystemEntity(Class<T> type, T entity) throws IOException {
    checkArgument(BusinessRules.allowSystemEntityAdd(type), "Not allowed to add %s", type);
    return storage.addSystemEntity(type, entity);
  }

  public <T extends DomainEntity> String addDomainEntity(Class<T> type, T entity, Change change) throws IOException {
    checkArgument(BusinessRules.allowDomainEntityAdd(type), "Not allowed to add %s", type);
    return storage.addDomainEntity(type, entity, change);
  }

  // --- update entities -----------------------------------------------

  public <T extends SystemEntity> void updateSystemEntity(Class<T> type, T entity) throws IOException {
    storage.updateSystemEntity(type, entity);
  }

  public <T extends DomainEntity> void updateDomainEntity(Class<T> type, T entity, Change change) throws IOException {
    storage.updateDomainEntity(type, entity, change);
  }

  public <T extends DomainEntity> void setPID(Class<T> type, String id, String pid) throws IOException {
    storage.setPID(type, id, pid);
  }

  // --- delete entities -----------------------------------------------

  public <T extends SystemEntity> void deleteSystemEntity(T entity) throws IOException {
    storage.deleteSystemEntity(entity.getClass(), entity.getId());
  }

  public <T extends DomainEntity> void deleteDomainEntity(T entity) throws IOException {
    storage.deleteDomainEntity(entity.getClass(), entity.getId(), entity.getModified());
  }

  /**
   * Deletes non-persistent domain entities with the specified type and id's..
   * The idea behind this method is that domain entities without persistent identifier are not validated yet.
   * After a bulk import non of the imported entity will have a persistent identifier, until a user has agreed with the imported collection.  
   * 
   * @param <T> extends {@code DomainEntity}, because system entities have no persistent identifiers.
   * @param type the type all of the objects should removed permanently from
   * @param ids the id's to remove permanently
   * @throws IOException when the storage layer throws an exception it will be forwarded
   */
  public <T extends DomainEntity> void deleteNonPersistent(Class<T> type, List<String> ids) throws IOException {
    storage.deleteNonPersistent(type, ids);
  }

  public int deleteAllSearchResults() {
    return storage.deleteAll(SearchResult.class);
  }

  public int deleteSearchResultsBefore(Date date) {
    return storage.deleteByDate(SearchResult.class, SearchResult.DATE_FIELD, date);
  }

  // -------------------------------------------------------------------

  public <T extends Entity> T getEntity(Class<T> type, String id) {
    try {
      return storage.getItem(type, id);
    } catch (IOException e) {
      LOG.error("Error in getEntity({}.class, {}): " + e.getMessage(), type.getSimpleName(), id);
      return null;
    }
  }

  public <T extends DomainEntity> T getEntityWithRelations(Class<T> type, String id) {
    T entity = null;
    try {
      entity = storage.getItem(type, id);
      if (entity != null) {
        storage.addRelationsTo(type, id, entity);
      }
    } catch (IOException e) {
      LOG.error("Error while handling {} {}", type.getName(), id);
    }
    return entity;
  }

  public <T extends SystemEntity> T findEntity(Class<T> type, String key, String value) {
    try {
      return storage.findItemByKey(type, key, value);
    } catch (IOException e) {
      LOG.error("Error while handling {}", type.getName());
      return null;
    }
  }

  /**
   * Returns a single system entity matching the non-null fields of
   * the specified entity, or null if no such entity exists.
   */
  public <T extends SystemEntity> T findEntity(Class<T> type, T example) {
    try {
      return storage.findItem(type, example);
    } catch (IOException e) {
      LOG.error("Error while handling {} {}", type.getName(), example.getId());
      return null;
    }
  }

  public <T extends DomainEntity> T getVariation(Class<T> type, String id, String variation) {
    try {
      return storage.getVariation(type, id, variation);
    } catch (Exception e) {
      LOG.error("Error while handling {} {}", type.getName(), id);
      return null;
    }
  }

  public <T extends DomainEntity> List<T> getAllVariations(Class<T> type, String id) {
    try {
      return storage.getAllVariations(type, id);
    } catch (IOException e) {
      LOG.error("Error while handling {} {}", type.getName(), id);
      return null;
    }
  }

  public <T extends Entity> StorageIterator<T> getAll(Class<T> type) {
    return storage.getAllByType(type);
  }

  public <T extends DomainEntity> RevisionChanges<T> getVersions(Class<T> type, String id) {
    try {
      return storage.getAllRevisions(type, id);
    } catch (IOException e) {
      LOG.error("Error while handling {} {}", type.getName(), id);
      return null;
    }
  }

  /**
   * Retrieves all the id's of type {@code <T>} that does not have a persistent id. 
   * 
   * @param type the type of the id's that should be retrieved
   * @return a list with all the ids.
   * @throws IOException when the storage layer throws an exception it will be forwarded.
   */
  public <T extends DomainEntity> List<String> getAllIdsWithoutPIDOfType(Class<T> type) throws IOException {
    return storage.getAllIdsWithoutPIDOfType(type);
  }

  /**
   * Returns the id's of the relations, connected to the entities with the input id's.
   * The input id's can be the source id as well as the target id of the Relation. 
   * 
   * @param ids a list of id's to find the relations for
   * @return a list of id's of the corresponding relations
   * @throws IOException re-throws the IOExceptions of the storage
   */
  public List<String> getRelationIds(List<String> ids) throws IOException {
    return storage.getRelationIds(ids);
  }

  public <T extends Entity> List<T> getAllLimited(Class<T> type, int offset, int limit) {
    if (limit == 0) {
      return Collections.<T> emptyList();
    }
    return resolveIterator(storage.getAllByType(type), offset, limit);
  }

  private <T extends Entity> List<T> resolveIterator(StorageIterator<T> iterator, int offset, int limit) {
    if (offset > 0) {
      iterator.skip(offset);
    }
    List<T> list = iterator.getSome(limit);
    iterator.close();
    return list;
  }

  public boolean relationExists(Relation relation) {
    try {
      return storage.relationExists(relation);
    } catch (IOException e) {
      LOG.error("Error while retrieving relation");
      return false;
    }
  }

}
