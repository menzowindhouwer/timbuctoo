package nl.knaw.huygens.timbuctoo.v5.dropwizard.endpoints.auth;

import com.google.common.collect.Sets;
import nl.knaw.huygens.timbuctoo.security.Authorizer;
import nl.knaw.huygens.timbuctoo.security.LoggedInUsers;
import nl.knaw.huygens.timbuctoo.security.dto.Authorization;
import nl.knaw.huygens.timbuctoo.security.dto.User;
import nl.knaw.huygens.timbuctoo.v5.dataset.dto.PromotedDataSet;
import nl.knaw.huygens.timbuctoo.v5.security.PermissionFetcher;
import nl.knaw.huygens.timbuctoo.v5.security.dto.Permission;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static nl.knaw.huygens.timbuctoo.v5.dropwizard.endpoints.auth.AuthCheck.checkAdminAccess;
import static nl.knaw.huygens.timbuctoo.v5.dropwizard.endpoints.auth.AuthCheck.checkWriteAccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AuthCheckTest {

  @Test
  public void checkAdminAccessReturnsNullIfTheUserHasAdminPermissionsForTheDataSet() throws Exception {
    User notOwner = User.create(null, "user");
    LoggedInUsers loggedInUsers = mock(LoggedInUsers.class);
    given(loggedInUsers.userFor(anyString())).willReturn(Optional.of(notOwner));
    PermissionFetcher permissionFetcher = mock(PermissionFetcher.class);
    given(permissionFetcher.getPermissions(anyString(), anyString(), anyString())).willReturn(permissionsForAdmin());
    Response response = checkAdminAccess(
      permissionFetcher,
      loggedInUsers,
      "auth",
      PromotedDataSet.promotedDataSet("ownerid", "datasetid", "http://ex.org", "http://example.org/prefix/", false)
    );

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void checkAdminAccessReturnsAnUnauthorizedResponseIfTheUserIsUnknown() throws Exception {
    LoggedInUsers loggedInUsers = mock(LoggedInUsers.class);
    given(loggedInUsers.userFor(anyString())).willReturn(Optional.empty());
    Response response = checkAdminAccess(
      null,
      loggedInUsers,
      "auth",
      PromotedDataSet.promotedDataSet("ownerid", "datasetid", "http://ex.org", "http://example.org/prefix/", false)
    );

    assertThat(response.getStatus(), is(UNAUTHORIZED.getStatusCode()));
  }

  @Test
  public void checkAdminAccessReturnsAForbiddenResponseIfTheUserIsNotAnAdminForTheDataSet() throws Exception {
    User notOwner = User.create(null, "user");
    LoggedInUsers loggedInUsers = mock(LoggedInUsers.class);
    given(loggedInUsers.userFor(anyString())).willReturn(Optional.of(notOwner));
    PermissionFetcher permissionFetcher = mock(PermissionFetcher.class);
    given(permissionFetcher.getPermissions(anyString(), anyString(), anyString())).willReturn(permissionsForNonAdmin());
    Response response = checkAdminAccess(
      permissionFetcher,
      loggedInUsers,
      "auth",
      PromotedDataSet.promotedDataSet("ownerid", "datasetid", "http://ex.org", "http://example.org/prefix/", false)
    );

    assertThat(response.getStatus(), is(FORBIDDEN.getStatusCode()));
  }

  @Test
  public void checkAdminAccessReturnsNullIfTheUserIsAnAdminForTheDataSet() throws Exception {
    User notOwner = User.create(null, "user");
    LoggedInUsers loggedInUsers = mock(LoggedInUsers.class);
    given(loggedInUsers.userFor(anyString())).willReturn(Optional.of(notOwner));
    PermissionFetcher permissionFetcher = mock(PermissionFetcher.class);
    given(permissionFetcher.getPermissions(anyString(), anyString(), anyString())).willReturn(permissionsForAdmin());
    Response response = checkAdminAccess(
      permissionFetcher,
      loggedInUsers,
      "auth",
      PromotedDataSet.promotedDataSet("ownerid", "datasetid", "http://ex.org", "http://example.org/prefix/", false)
    );

    assertThat(response.getStatus(), is(200));
  }

  private static Set<Permission> permissionsForNonAdmin() {
    return Sets.newHashSet(Permission.WRITE, Permission.READ);
  }

  private static Set<Permission> permissionsForAdmin() {
    return Sets.newHashSet(Permission.WRITE, Permission.ADMIN, Permission.READ);
  }

}
