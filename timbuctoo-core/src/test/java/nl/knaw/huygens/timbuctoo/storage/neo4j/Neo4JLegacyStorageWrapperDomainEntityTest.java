package nl.knaw.huygens.timbuctoo.storage.neo4j;

import static nl.knaw.huygens.timbuctoo.model.Entity.ID_PROPERTY_NAME;
import static nl.knaw.huygens.timbuctoo.storage.neo4j.DomainEntityMatcher.likeDomainEntity;
import static nl.knaw.huygens.timbuctoo.storage.neo4j.NodeMockBuilder.aNode;
import static nl.knaw.huygens.timbuctoo.storage.neo4j.RelationshipMockBuilder.aRelationship;
import static nl.knaw.huygens.timbuctoo.storage.neo4j.SearchResultBuilder.aSearchResult;
import static nl.knaw.huygens.timbuctoo.storage.neo4j.SearchResultBuilder.anEmptySearchResult;
import static nl.knaw.huygens.timbuctoo.storage.neo4j.SubADomainEntityBuilder.aDomainEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;
import nl.knaw.huygens.timbuctoo.model.util.Change;
import nl.knaw.huygens.timbuctoo.storage.NoSuchEntityException;
import nl.knaw.huygens.timbuctoo.storage.StorageException;
import nl.knaw.huygens.timbuctoo.storage.UpdateException;

import org.junit.Test;
import org.mockito.InOrder;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import test.model.projecta.SubADomainEntity;

public class Neo4JLegacyStorageWrapperDomainEntityTest extends Neo4JLegacyStorageWrapperTest {

  private static final Class<SubADomainEntity> DOMAIN_ENTITY_TYPE = SubADomainEntity.class;
  private static final Label DOMAIN_ENTITY_LABEL = DynamicLabel.label(TypeNames.getInternalName(DOMAIN_ENTITY_TYPE));
  private static final int FOURTH_REVISION = 4;

  @Test
  public void addDomainEntitySavesTheProjectVersionAndThePrimitiveAndReturnsTheId() throws Exception {
    // setup
    Node nodeMock = aNode().createdBy(dbMock);
    idGeneratorMockCreatesIDFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, ID);

    NodeConverter<? super SubADomainEntity> compositeConverter = propertyContainerConverterFactoryHasCompositeConverterFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE);
    SubADomainEntity domainEntity = aDomainEntity().build();

    // action
    String actualId = instance.addDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);

    // verify
    verify(dbMock).beginTx();
    verify(dbMock).createNode();
    verify(compositeConverter).addValuesToPropertyContainer( //
        argThat(equalTo(nodeMock)), // 
        argThat(likeDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE) //
            .withId(actualId) //
            .withACreatedValue() //
            .withAModifiedValue() //
            .withRevision(FIRST_REVISION)//
            .withoutAPID()));
    verify(transactionMock).success();
  }

  @Test
  public void addDomainEntityRemovesThePIDBeforeSaving() throws Exception {
    // setup
    Node nodeMock = aNode().createdBy(dbMock);
    idGeneratorMockCreatesIDFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, ID);

    NodeConverter<? super SubADomainEntity> compositeConverter = propertyContainerConverterFactoryHasCompositeConverterFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE);
    SubADomainEntity domainEntityWithAPID = aDomainEntity().withAPid().build();

    // action
    instance.addDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntityWithAPID, CHANGE);

    // verify
    verify(dbMock).beginTx();
    verify(dbMock).createNode();
    verify(compositeConverter).addValuesToPropertyContainer( //
        argThat(equalTo(nodeMock)), // 
        argThat(likeDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE) //
            .withoutAPID()));
    verify(transactionMock).success();
  }

  private <T extends DomainEntity> NodeConverter<? super T> propertyContainerConverterFactoryHasCompositeConverterFor(Class<T> type) {
    @SuppressWarnings("unchecked")
    NodeConverter<? super T> converter = mock(NodeConverter.class);
    doReturn(converter).when(propertyContainerConverterFactoryMock).createCompositeForType(type);
    return converter;
  }

  @Test(expected = StorageException.class)
  public void addDomainEntityRollsBackTheTransactionAndThrowsAStorageExceptionWhenTheDomainEntityConverterThrowsAConversionException() throws Exception {
    // setup
    Node nodeMock = aNode().createdBy(dbMock);

    idGeneratorMockCreatesIDFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, ID);

    SubADomainEntity domainEntity = aDomainEntity().build();
    NodeConverter<? super SubADomainEntity> compositeConverter = propertyContainerConverterFactoryHasCompositeConverterFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE);
    doThrow(ConversionException.class).when(compositeConverter).addValuesToPropertyContainer(nodeMock, domainEntity);

    try {
      // action
      instance.addDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);
    } finally {
      // verify
      verify(dbMock).beginTx();
      verify(dbMock).createNode();
      verify(compositeConverter).addValuesToPropertyContainer( //
          argThat(equalTo(nodeMock)), // 
          argThat(likeDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE) //
              .withId(ID) //
              .withACreatedValue() //
              .withAModifiedValue() //
              .withRevision(FIRST_REVISION)));
      verify(transactionMock).failure();
      verifyNoMoreInteractions(compositeConverter);
    }
  }

  @Test
  public void getEntityReturnsTheLatestIfMoreThanOneItemIsFound() throws Exception {
    // setup
    Node nodeWithThirdRevision = aNode().withRevision(THIRD_REVISION).build();

    aSearchResult().forLabel(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(aNode().withRevision(FIRST_REVISION).build()) //
        .andNode(aNode().withRevision(SECOND_REVISION).build()) //
        .andNode(nodeWithThirdRevision)//
        .foundInDB(dbMock);

    SubADomainEntity domainEntity = aDomainEntity().withId(ID).build();

    NodeConverter<SubADomainEntity> domainEntityConverterMock = propertyContainerConverterFactoryHasANodeConverterTypeFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE);
    when(domainEntityConverterMock.convertToEntity(nodeWithThirdRevision)).thenReturn(domainEntity);

    // action
    SubADomainEntity actualEntity = instance.getEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, ID);

    // verify
    assertThat(actualEntity, is(equalTo(domainEntity)));

    InOrder inOrder = inOrder(dbMock, propertyContainerConverterFactoryMock, domainEntityConverterMock, transactionMock);
    inOrder.verify(dbMock).findNodesByLabelAndProperty(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
    inOrder.verify(domainEntityConverterMock).convertToEntity(nodeWithThirdRevision);
    inOrder.verify(transactionMock).success();
  }

  @Test
  public void updateDomainEntityRetrievesTheNodeAndUpdatesItsValues() throws Exception {
    // setup
    Node nodeMock = aNode().withRevision(FIRST_REVISION).build();

    aSearchResult().forLabel(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(nodeMock) //
        .foundInDB(dbMock);

    NodeConverter<SubADomainEntity> domainEntityConverterMock = propertyContainerConverterFactoryHasANodeConverterTypeFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE);;

    Change oldModified = CHANGE;
    SubADomainEntity domainEntity = aDomainEntity() //
        .withId(ID) //
        .withRev(FIRST_REVISION)//
        .withAPid()//
        .withModified(oldModified)//
        .build();

    instance.updateDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);

    // verify
    InOrder inOrder = inOrder(dbMock, domainEntityConverterMock, transactionMock);
    inOrder.verify(dbMock).beginTx();
    inOrder.verify(dbMock).findNodesByLabelAndProperty(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
    inOrder.verify(domainEntityConverterMock).updatePropertyContainer(argThat(equalTo(nodeMock)), //
        argThat(likeDomainEntity(SubADomainEntity.class) //
            .withAModifiedValueNotEqualTo(oldModified) //
            .withRevision(SECOND_REVISION) //
            .withoutAPID()));
    inOrder.verify(domainEntityConverterMock).updateModifiedAndRev(argThat(equalTo(nodeMock)), //
        argThat(likeDomainEntity(SubADomainEntity.class) //
            .withAModifiedValueNotEqualTo(oldModified) //
            .withRevision(SECOND_REVISION) //
            .withoutAPID()));
    inOrder.verify(transactionMock).success();
    verifyNoMoreInteractions(dbMock, domainEntityConverterMock);
  }

  @Test
  public void updateDomainEntityUpdatesTheLatestIfMultipleAreFound() throws Exception {
    // setup
    Node nodeWithThirdRevision = aNode().withRevision(THIRD_REVISION).build();

    aSearchResult().forLabel(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(aNode().withRevision(FIRST_REVISION).build()) //
        .andNode(aNode().withRevision(SECOND_REVISION).build()) //
        .andNode(nodeWithThirdRevision)//
        .foundInDB(dbMock);

    NodeConverter<SubADomainEntity> domainEntityConverterMock = propertyContainerConverterFactoryHasANodeConverterTypeFor(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE);

    Change oldModified = CHANGE;
    SubADomainEntity domainEntity = aDomainEntity() //
        .withId(ID) //
        .withRev(THIRD_REVISION)//
        .withAPid()//
        .withModified(oldModified)//
        .build();

    instance.updateDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);

    // verify
    InOrder inOrder = inOrder(dbMock, domainEntityConverterMock, transactionMock);
    inOrder.verify(dbMock).beginTx();
    inOrder.verify(dbMock).findNodesByLabelAndProperty(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
    inOrder.verify(domainEntityConverterMock).updatePropertyContainer(argThat(equalTo(nodeWithThirdRevision)), //
        argThat(likeDomainEntity(SubADomainEntity.class) //
            .withAModifiedValueNotEqualTo(oldModified) //
            .withRevision(Neo4JLegacyStorageWrapperDomainEntityTest.FOURTH_REVISION) //
            .withoutAPID()));
    inOrder.verify(domainEntityConverterMock).updateModifiedAndRev(argThat(equalTo(nodeWithThirdRevision)), //
        argThat(likeDomainEntity(SubADomainEntity.class) //
            .withAModifiedValueNotEqualTo(oldModified) //
            .withRevision(Neo4JLegacyStorageWrapperDomainEntityTest.FOURTH_REVISION) //
            .withoutAPID()));
    inOrder.verify(transactionMock).success();
    verifyNoMoreInteractions(dbMock, domainEntityConverterMock);
  }

  @Test(expected = UpdateException.class)
  public void updateDomainEntityThrowsAnUpdateExceptionWhenTheEntityCannotBeFound() throws Exception {
    // setup
    anEmptySearchResult().forLabel(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL).andId(ID).foundInDB(dbMock);

    Change oldModified = CHANGE;
    SubADomainEntity domainEntity = aDomainEntity() //
        .withId(ID) //
        .withRev(FIRST_REVISION)//
        .withAPid()//
        .withModified(oldModified)//
        .build();

    try {
      // action
      instance.updateDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);
    } finally {
      // verify
      InOrder inOrder = inOrder(dbMock, transactionMock);
      inOrder.verify(dbMock).beginTx();
      inOrder.verify(dbMock).findNodesByLabelAndProperty(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
      inOrder.verify(transactionMock).failure();
      verifyZeroInteractions(propertyContainerConverterFactoryMock);
      verifyNoMoreInteractions(dbMock);
    }
  }

  @Test(expected = UpdateException.class)
  public void updateDomainEntityThrowsAnUpdateExceptionWhenRevOfTheNodeIsHigherThanThatOfTheEntity() throws Exception {
    // setup
    Node nodeMock = aNode().withRevision(SECOND_REVISION).build();

    aSearchResult().forLabel(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(nodeMock) //
        .foundInDB(dbMock);

    Change oldModified = CHANGE;
    SubADomainEntity domainEntity = aDomainEntity() //
        .withId(ID) //
        .withRev(FIRST_REVISION)//
        .withAPid()//
        .withModified(oldModified)//
        .build();

    try {
      // action
      instance.updateDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);
    } finally {
      // verify
      InOrder inOrder = inOrder(dbMock, transactionMock);
      inOrder.verify(dbMock).beginTx();
      inOrder.verify(dbMock).findNodesByLabelAndProperty(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
      inOrder.verify(transactionMock).failure();
      verifyZeroInteractions(propertyContainerConverterFactoryMock);
      verifyNoMoreInteractions(dbMock);
    }
  }

  @Test(expected = UpdateException.class)
  public void updateDomainEntityThrowsAnUpdateExceptionWhenRevOfTheNodeIsLowerThanThatOfTheEntity() throws Exception {
    // setup
    Node nodeMock = aNode().withRevision(SECOND_REVISION).build();
    aSearchResult().forLabel(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(nodeMock) //
        .foundInDB(dbMock);

    Change oldModified = CHANGE;
    SubADomainEntity domainEntity = aDomainEntity() //
        .withId(ID) //
        .withRev(FIRST_REVISION)//
        .withAPid()//
        .withModified(oldModified)//
        .build();

    try {
      // action
      instance.updateDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, domainEntity, CHANGE);
    } finally {
      // verify
      InOrder inOrder = inOrder(dbMock, transactionMock);
      inOrder.verify(dbMock).beginTx();
      inOrder.verify(dbMock).findNodesByLabelAndProperty(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
      inOrder.verify(transactionMock).failure();
      verifyZeroInteractions(propertyContainerConverterFactoryMock);
      verifyNoMoreInteractions(dbMock);
    }
  }

  @Test
  public void deleteDomainEntityFirstRemovesTheNodesRelationShipsAndThenTheNodeItselfTheDatabase() throws Exception {
    // setup
    Relationship relMock1 = aRelationship().build();
    Relationship relMock2 = aRelationship().build();
    Node nodeMock = aNode().withOutgoingRelationShip(relMock1).andOutgoingRelationship(relMock2).build();

    aSearchResult().forLabel(PRIMITIVE_DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(nodeMock) //
        .foundInDB(dbMock);

    // action
    instance.deleteDomainEntity(Neo4JLegacyStorageWrapperTest.PRIMITIVE_DOMAIN_ENTITY_TYPE, ID, CHANGE);

    // verify
    InOrder inOrder = inOrder(dbMock, nodeMock, relMock1, relMock2, transactionMock);
    inOrder.verify(dbMock).beginTx();
    verifyNodeAndItsRelationAreDelete(nodeMock, relMock1, relMock2, inOrder);
    inOrder.verify(transactionMock).success();

  }

  @Test
  public void deleteDomainEntityRemovesAllTheFoundNodes() throws Exception {
    // setup
    Relationship relMock1 = aRelationship().build();
    Relationship relMock2 = aRelationship().build();
    Node nodeMock = aNode().withOutgoingRelationShip(relMock1).andOutgoingRelationship(relMock2).build();

    Relationship relMock3 = aRelationship().build();
    Relationship relMock4 = aRelationship().build();
    Node nodeMock2 = aNode().withOutgoingRelationShip(relMock3).andOutgoingRelationship(relMock4).build();

    Relationship relMock5 = aRelationship().build();
    Relationship relMock6 = aRelationship().build();
    Node nodeMock3 = aNode().withOutgoingRelationShip(relMock5).andOutgoingRelationship(relMock6).build();

    aSearchResult().forLabel(PRIMITIVE_DOMAIN_ENTITY_LABEL).andId(ID) //
        .withNode(nodeMock) //
        .andNode(nodeMock2) //
        .andNode(nodeMock3) //
        .foundInDB(dbMock);

    // action
    instance.deleteDomainEntity(Neo4JLegacyStorageWrapperTest.PRIMITIVE_DOMAIN_ENTITY_TYPE, ID, CHANGE);

    // verify
    InOrder inOrder = inOrder(dbMock, nodeMock, relMock1, relMock2, nodeMock2, relMock3, relMock4, nodeMock3, relMock5, relMock6, transactionMock);
    inOrder.verify(dbMock).beginTx();
    verifyNodeAndItsRelationAreDelete(nodeMock, relMock1, relMock2, inOrder);
    verifyNodeAndItsRelationAreDelete(nodeMock2, relMock3, relMock4, inOrder);
    verifyNodeAndItsRelationAreDelete(nodeMock3, relMock5, relMock6, inOrder);
    inOrder.verify(transactionMock).success();
  }

  @Test(expected = NoSuchEntityException.class)
  public void deleteDomainEntityThrowsANoSuchEntityExceptionWhenTheEntityCannotBeFound() throws Exception {
    // setup
    anEmptySearchResult().forLabel(PRIMITIVE_DOMAIN_ENTITY_LABEL).andId(ID).foundInDB(dbMock);
    try {
      // action
      instance.deleteDomainEntity(Neo4JLegacyStorageWrapperTest.PRIMITIVE_DOMAIN_ENTITY_TYPE, ID, CHANGE);
    } finally {
      // verify
      verify(dbMock).beginTx();
      verify(dbMock).findNodesByLabelAndProperty(PRIMITIVE_DOMAIN_ENTITY_LABEL, ID_PROPERTY_NAME, ID);
      verify(transactionMock).failure();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void deleteThrowsAnIllegalArgumentExceptionWhenTheEntityIsNotAPrimitiveDomainEntity() throws Exception {

    try {
      // action
      instance.deleteDomainEntity(Neo4JLegacyStorageWrapperDomainEntityTest.DOMAIN_ENTITY_TYPE, ID, CHANGE);
    } finally {
      // verify
      verifyZeroInteractions(dbMock);
    }
  }

  @Test
  public void setPIDAddsAPIDToTheLatestNodeIfMultipleAreFound() throws InstantiationException, IllegalAccessException, Exception {
    // setup
    Node nodeWithLatestRevision = aNode().withRevision(SECOND_REVISION).build();
    aSearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID)//
        .withNode(aNode().withRevision(FIRST_REVISION).build()).withNode(nodeWithLatestRevision)//
        .foundInDB(dbMock);

    NodeConverter<SubADomainEntity> converterMock = propertyContainerConverterFactoryHasANodeConverterTypeFor(DOMAIN_ENTITY_TYPE);
    when(converterMock.convertToEntity(nodeWithLatestRevision)).thenReturn(aDomainEntity().withId(ID).build());

    // action
    instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);

    verify(converterMock).addValuesToPropertyContainer( //
        argThat(equalTo(nodeWithLatestRevision)), //
        argThat(likeDomainEntity(DOMAIN_ENTITY_TYPE).withId(ID).withPID(PID)));

  }

  @Test
  public void setPIDAddsAPIDToTheNodeAndDuplicatesTheNode() throws InstantiationException, IllegalAccessException, Exception {
    // setup
    Node node = aNode().build();
    aSearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID)//
        .withNode(node)//
        .foundInDB(dbMock);

    NodeConverter<SubADomainEntity> converterMock = propertyContainerConverterFactoryHasANodeConverterTypeFor(DOMAIN_ENTITY_TYPE);
    when(converterMock.convertToEntity(node)).thenReturn(aDomainEntity().withId(ID).build());

    // action
    instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);

    InOrder inOrder = inOrder(converterMock, transactionMock, nodeDuplicatorMock);
    inOrder.verify(converterMock).addValuesToPropertyContainer( //
        argThat(equalTo(node)), //
        argThat(likeDomainEntity(DOMAIN_ENTITY_TYPE).withId(ID).withPID(PID)));
    inOrder.verify(nodeDuplicatorMock).saveDuplicate(node);
    inOrder.verify(transactionMock).success();
  }

  @Test(expected = IllegalStateException.class)
  public void setPIDThrowsAnIllegalStateExceptionWhenTheEntityAlreadyHasAPID() throws Exception {
    // setup
    Node aNodeWithAPID = aNode().withAPID().build();
    aSearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID)//
        .withNode(aNodeWithAPID)//
        .foundInDB(dbMock);

    SubADomainEntity entityWithPID = aDomainEntity().withAPid().build();

    NodeConverter<SubADomainEntity> nodeConverter = propertyContainerConverterFactoryHasANodeConverterTypeFor(DOMAIN_ENTITY_TYPE);
    when(nodeConverter.convertToEntity(aNodeWithAPID)).thenReturn(entityWithPID);

    try {
      // action
      instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);
    } finally {
      // verify
      verify(nodeConverter).convertToEntity(aNodeWithAPID);
      verify(transactionMock).failure();
    }
  }

  @Test(expected = ConversionException.class)
  public void setPIDThrowsAConversionExceptionWhenTheNodeCannotBeConverted() throws Exception {
    // setup
    Node aNodeWithAPID = aNode().withAPID().build();
    aSearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID)//
        .withNode(aNodeWithAPID)//
        .foundInDB(dbMock);

    NodeConverter<SubADomainEntity> nodeConverter = propertyContainerConverterFactoryHasANodeConverterTypeFor(DOMAIN_ENTITY_TYPE);
    when(nodeConverter.convertToEntity(aNodeWithAPID)).thenThrow(new ConversionException());

    try {
      // action
      instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);
    } finally {
      // verify
      verify(nodeConverter).convertToEntity(aNodeWithAPID);
      verify(transactionMock).failure();
    }
  }

  @Test(expected = ConversionException.class)
  public void setPIDThrowsAConversionsExceptionWhenTheUpdatedEntityCannotBeCovnverted() throws Exception {
    // setup
    Node aNode = aNode().build();
    aSearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID)//
        .withNode(aNode)//
        .foundInDB(dbMock);

    SubADomainEntity entity = aDomainEntity().build();

    NodeConverter<SubADomainEntity> nodeConverter = propertyContainerConverterFactoryHasANodeConverterTypeFor(DOMAIN_ENTITY_TYPE);
    when(nodeConverter.convertToEntity(aNode)).thenReturn(entity);
    doThrow(ConversionException.class).when(nodeConverter).addValuesToPropertyContainer(aNode, entity);

    try {
      // action
      instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);
    } finally {
      // verify
      verify(nodeConverter).addValuesToPropertyContainer(aNode, entity);
      verify(transactionMock).failure();
    }
  }

  @Test(expected = StorageException.class)
  public void setPIDThrowsAStorageExceptionWhenTheEntityDoesNotExist() throws Exception {
    // setup
    anEmptySearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID).foundInDB(dbMock);

    try {
      // action
      instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);
    } finally {
      // verify
      verify(transactionMock).failure();
    }

  }

  @Test(expected = StorageException.class)
  public void setPIDThrowsAStorageExceptionWhenTheEntityCannotBeInstatiated() throws Exception {

    // setup
    Node aNode = aNode().build();
    aSearchResult().forLabel(DOMAIN_ENTITY_LABEL).andId(ID)//
        .withNode(aNode)//
        .foundInDB(dbMock);

    NodeConverter<SubADomainEntity> converterMock = propertyContainerConverterFactoryHasANodeConverterTypeFor(DOMAIN_ENTITY_TYPE);
    when(converterMock.convertToEntity(aNode)).thenThrow(new InstantiationException());

    try {
      // action
      instance.setPID(DOMAIN_ENTITY_TYPE, ID, PID);
    } finally {
      // verify
      verify(transactionMock).failure();
    }
  }

  @Test
  public void getRevisionDelegatesTheGetRevisionCallToTheNeo4JStorageGetDomainEntityRevision() throws Exception {
    // setup
    SubADomainEntity domainEntity = aDomainEntity().build();
    when(neo4JStorageMock.getDomainEntityRevision(DOMAIN_ENTITY_TYPE, ID, FIRST_REVISION)).thenReturn(domainEntity);

    // action
    SubADomainEntity revision = instance.getRevision(DOMAIN_ENTITY_TYPE, ID, FIRST_REVISION);

    // verify
    assertThat(revision, is(sameInstance(domainEntity)));

    verify(neo4JStorageMock).getDomainEntityRevision(DOMAIN_ENTITY_TYPE, ID, FIRST_REVISION);

  }

  @Test(expected = StorageException.class)
  public void getRevisionThrowsAStorageExceptionWhenTheNeo4JStorageDoes() throws Exception {
    // setup
    when(neo4JStorageMock.getDomainEntityRevision(DOMAIN_ENTITY_TYPE, ID, FIRST_REVISION)).thenThrow(new StorageException());

    // action
    instance.getRevision(DOMAIN_ENTITY_TYPE, ID, FIRST_REVISION);
  }
}
