package nl.knaw.huygens.timbuctoo.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import nl.knaw.huygens.timbuctoo.model.Relation;
import nl.knaw.huygens.timbuctoo.model.RelationType;
import nl.knaw.huygens.timbuctoo.model.util.MockRelationBuilder;
import nl.knaw.huygens.timbuctoo.storage.StorageManager;
import nl.knaw.huygens.timbuctoo.storage.ValidationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class RelationTypeConformationValidatorTest {

  private String relationTypeId = "relationTypeId";
  private Relation relationMock;
  private RelationType relationType;
  private StorageManager storage;
  private RelationTypeConformationValidator validator;

  @Before
  public void setUp() {
    relationMock = MockRelationBuilder.createRelation(Relation.class)//
        .withRelationTypeId(relationTypeId)//
        .build();

    relationType = new RelationType();
    storage = mock(StorageManager.class);
    validator = new RelationTypeConformationValidator(storage);
  }

  @Test
  public void testValidate() throws IOException, ValidationException {
    // when
    when(storage.getRelationType(relationTypeId)).thenReturn(relationType);
    when(relationMock.conformsToRelationType(relationType)).thenReturn(true);

    // action
    validator.validate(relationMock);

    // verify
    InOrder inOrder = Mockito.inOrder(storage, relationMock);
    inOrder.verify(relationMock).getTypeId();
    inOrder.verify(storage).getRelationType(relationTypeId);
    inOrder.verify(relationMock).conformsToRelationType(relationType);
  }

  @Test(expected = ValidationException.class)
  public void testValidateRelationTypeDoesNotExist() throws IOException, ValidationException {
    // when
    when(storage.getRelationType(relationTypeId)).thenReturn(null);

    try {
      // action
      validator.validate(relationMock);
    } finally {
      // verify
      verify(relationMock).getTypeId();
      verify(storage).getRelationType(relationTypeId);
      verifyNoMoreInteractions(relationMock);
    }
  }

  @Test(expected = ValidationException.class)
  public void testValidateRelationDoesNotConformToRelationType() throws IOException, ValidationException {
    // when
    when(storage.getRelationType(relationTypeId)).thenReturn(relationType);
    when(relationMock.conformsToRelationType(relationType)).thenReturn(false);

    try {
      // action
      validator.validate(relationMock);
    } finally {
      // verify
      verify(relationMock).getTypeId();
      verify(storage).getRelationType(relationTypeId);
      verify(relationMock).conformsToRelationType(relationType);
    }
  }

}
