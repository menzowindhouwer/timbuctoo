package nl.knaw.huygens.timbuctoo.v5.graphql.mutations;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import nl.knaw.huygens.timbuctoo.util.Tuple;
import nl.knaw.huygens.timbuctoo.util.UriHelper;
import nl.knaw.huygens.timbuctoo.util.UserUriCreator;
import nl.knaw.huygens.timbuctoo.v5.dataset.DataSetRepository;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.DataSet;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.DataSetMetaData;
import nl.knaw.huygens.timbuctoo.v5.filestorage.exceptions.LogStorageFailedException;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.berkeleydb.datafetchers.QuadStoreLookUpSubjectByUriFetcher;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.ImmutableContextData;
import nl.knaw.huygens.timbuctoo.v5.security.dto.Permission;
import nl.knaw.huygens.timbuctoo.v5.security.dto.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class EditMutation implements DataFetcher {
  private final DataSetRepository dataSetRepository;
  private final QuadStoreLookUpSubjectByUriFetcher subjectFetcher;
  private final String dataSetName;
  private final String ownerId;
  private final UserUriCreator userUriCreator;

  public EditMutation(DataSetRepository dataSetRepository, UriHelper uriHelper,
                      QuadStoreLookUpSubjectByUriFetcher subjectFetcher,
                      String dataSetId) {
    this.dataSetRepository = dataSetRepository;
    this.subjectFetcher = subjectFetcher;
    Tuple<String, String> dataSetIdSplit = DataSetMetaData.splitCombinedId(dataSetId);
    dataSetName = dataSetIdSplit.getRight();
    ownerId = dataSetIdSplit.getLeft();
    userUriCreator = new UserUriCreator(uriHelper);
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    String uri = environment.getArgument("uri");
    Map entity = environment.getArgument("entity");
    ImmutableContextData contextData = environment.getContext();
    Optional<User> userOpt = contextData.getUser();
    if (!userOpt.isPresent()) {
      throw new RuntimeException("User should be logged in.");
    }

    User user = userOpt.get();
    Optional<DataSet> dataSetOpt = dataSetRepository.getDataSet(user, ownerId, dataSetName);
    if (!dataSetOpt.isPresent()) {
      throw new RuntimeException("Data set is not available.");
    }

    DataSet dataSet = dataSetOpt.get();
    if (!contextData.getUserPermissionCheck().getPermissions(dataSet.getMetadata()).contains(Permission.WRITE)) {
      throw new RuntimeException("User should have data 'write' permissions on the data set.");
    }

    try {
      dataSet.getImportManager().generateLog(
        dataSet.getMetadata().getBaseUri(),
        dataSet.getMetadata().getBaseUri(),
        new GraphQlToRdfPatch(user, uri, userUriCreator, entity)
      ).get(); // Wait until the data is processed
    } catch (LogStorageFailedException | JsonProcessingException | InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    return subjectFetcher.getItem(uri, dataSet);
  }
}
