package nl.knaw.huygens.timbuctoo.storage;

import nl.knaw.huygens.timbuctoo.model.DomainEntity;

public class NoOpValidator<T extends DomainEntity> implements Validator<T> {

  @Override
  public void validate(T entityToValidate) throws ValidationException {
    // do nothing
  }

}
