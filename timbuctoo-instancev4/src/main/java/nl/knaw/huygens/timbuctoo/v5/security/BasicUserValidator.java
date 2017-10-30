package nl.knaw.huygens.timbuctoo.v5.security;

import nl.knaw.huygens.security.client.AuthenticationHandler;
import nl.knaw.huygens.security.client.UnauthorizedException;
import nl.knaw.huygens.security.client.model.SecurityInformation;
import nl.knaw.huygens.timbuctoo.security.UserStore;
import nl.knaw.huygens.timbuctoo.security.dto.User;
import nl.knaw.huygens.timbuctoo.security.exceptions.AuthenticationUnavailableException;
import nl.knaw.huygens.timbuctoo.v5.security.exceptions.UserValidationException;

import java.io.IOException;
import java.util.Optional;

public class BasicUserValidator implements UserValidator {

  private final AuthenticationHandler authenticationHandler;
  private final UserStore userStore;

  public BasicUserValidator(AuthenticationHandler authenticationHandler, UserStore userStore) {

    this.authenticationHandler = authenticationHandler;
    this.userStore = userStore;
  }

  @Override
  public Optional<User> getUserFromAccessToken(String accessToken) throws UserValidationException {
    if (accessToken != null) {
      try {
        SecurityInformation securityInformation = authenticationHandler.getSecurityInformation(accessToken);

        if (securityInformation != null) {
          return userStore.userFor(securityInformation.getPersistentID());
        }
      } catch (UnauthorizedException | IOException | AuthenticationUnavailableException e) {
        throw new UserValidationException(e);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<User> getUserFromId(String userId) throws UserValidationException {
    if (userId != null) {
      try {
        return userStore.userForId(userId);
      } catch (AuthenticationUnavailableException e) {
        throw new UserValidationException(e);
      }
    }
    return Optional.empty();
  }

}
