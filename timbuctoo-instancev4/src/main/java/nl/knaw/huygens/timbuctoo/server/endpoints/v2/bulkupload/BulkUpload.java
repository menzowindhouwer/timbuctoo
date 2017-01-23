package nl.knaw.huygens.timbuctoo.server.endpoints.v2.bulkupload;

import nl.knaw.huygens.timbuctoo.bulkupload.BulkUploadService;
import nl.knaw.huygens.timbuctoo.bulkupload.InvalidFileException;
import nl.knaw.huygens.timbuctoo.core.TransactionEnforcer;
import nl.knaw.huygens.timbuctoo.core.TransactionStateAndResult;
import nl.knaw.huygens.timbuctoo.model.vre.Vre;
import nl.knaw.huygens.timbuctoo.security.LoggedInUsers;
import nl.knaw.huygens.timbuctoo.security.VreAuthorizationCrud;
import nl.knaw.huygens.timbuctoo.security.dto.User;
import nl.knaw.huygens.timbuctoo.security.dto.UserRoles;
import nl.knaw.huygens.timbuctoo.security.exceptions.AuthorizationCreationException;
import nl.knaw.huygens.timbuctoo.server.security.UserPermissionChecker;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static nl.knaw.huygens.timbuctoo.model.vre.Vre.PublishState.MAPPING_EXECUTION;
import static nl.knaw.huygens.timbuctoo.model.vre.Vre.PublishState.UPLOADING;
import static org.apache.poi.util.IOUtils.copy;

@Path("/v2.1/bulk-upload")
public class BulkUpload {

  public static final Logger LOG = LoggerFactory.getLogger(BulkUpload.class);
  private final BulkUploadService uploadService;
  private final BulkUploadVre bulkUploadVre;
  private final LoggedInUsers loggedInUsers;
  private final VreAuthorizationCrud authorizationCreator;
  private final int maxCache;
  private final UserPermissionChecker permissionChecker;
  private final TransactionEnforcer transactionEnforcer;

  public BulkUpload(BulkUploadService uploadService, BulkUploadVre bulkUploadVre,
                    LoggedInUsers loggedInUsers, VreAuthorizationCrud authorizationCreator, int maxCache,
                    UserPermissionChecker permissionChecker, TransactionEnforcer transactionEnforcer) {
    this.uploadService = uploadService;
    this.bulkUploadVre = bulkUploadVre;
    this.loggedInUsers = loggedInUsers;
    this.authorizationCreator = authorizationCreator;
    this.maxCache = maxCache;
    this.permissionChecker = permissionChecker;
    this.transactionEnforcer = transactionEnforcer;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/plain")
  public Response uploadExcelFile(
    @FormDataParam("file") InputStream fileUpload,
    @FormDataParam("file") FormDataContentDisposition fileDetails,
    @FormDataParam("vreName") String vreName,
    @HeaderParam("Authorization") String authorization) {
    if (fileUpload == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("The file is missing").build();
    } else {
      Optional<User> user = loggedInUsers.userFor(authorization);
      if (!user.isPresent()) {
        return Response.status(Response.Status.FORBIDDEN).entity("User not known").build();
      } else {
        final String unNamespacedVreName = vreName == null ? fileDetails.getFileName() : vreName;
        String namespacedVre = user.get().getPersistentId() + "_" + stripFunnyCharacters(unNamespacedVreName);
        try {
          authorizationCreator.createAuthorization(namespacedVre, user.get().getId(), UserRoles.ADMIN_ROLE);
        } catch (AuthorizationCreationException e) {
          LOG.error("Cannot add authorization for user {} and VRE {}", user.get().getId(), namespacedVre);
          LOG.error("Exception thrown", e);
          return Response.status(Response.Status.FORBIDDEN).entity("Unable to create authorization for user").build();
        }

        try {
          final ChunkedOutput<String> output = executeUpload(
            fileDetails,
            unNamespacedVreName,
            namespacedVre,
            fileUpload
          );
          // the output will be probably returned even before
          // a first chunk is written by the new thread
          return Response.ok()
                         .entity(output)
                         .location(bulkUploadVre.createUri(namespacedVre))
                         .build();

        } catch (IOException e) {
          LOG.error("Reading upload failed", e);
          return Response.status(Response.Status.BAD_REQUEST)
                         .entity(e.getMessage())
                         .build();
        } catch (IllegalArgumentException e) {
          return Response.status(Response.Status.BAD_REQUEST)
                         .entity(e.getMessage())
                         .build();
        }

      }
    }
  }

  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/plain")
  public Response reUploadExcelFile(
    @FormDataParam("file") InputStream fileUpload,
    @FormDataParam("file") FormDataContentDisposition fileDetails,
    @FormDataParam("vreId") String vreName,
    @HeaderParam("Authorization") String authorization) {

    // First check permission
    final Optional<Response> filterResponse = permissionChecker.checkPermissionWithResponse(vreName, authorization);
    if (filterResponse.isPresent()) {
      return filterResponse.get();
    }
    return transactionEnforcer.executeAndReturn(timbuctooActions -> {

      // Try to find the vre
      final Vre vre = timbuctooActions.getVre(vreName);
      if (vre == null) {
        // not found
        return TransactionStateAndResult.commitAndReturn(Response.status(Response.Status.NOT_FOUND).build());
      }

      // Check whether vre is currently in a transitional state
      if (vre.getPublishState() == UPLOADING || vre.getPublishState() == MAPPING_EXECUTION) {
        // Data from this vre is currently being transformed, so re-upload is dangerous
        return TransactionStateAndResult.commitAndReturn(Response.status(Response.Status.PRECONDITION_FAILED).build());
      }

      try {
        final ChunkedOutput<String> output = executeUpload(
          fileDetails,
          vre.getMetadata().getLabel(),
          vreName,
          fileUpload
        );
        return TransactionStateAndResult.commitAndReturn(
          Response.ok()
                  .location(bulkUploadVre.createUri(vreName))
                  .entity(output)
                  .build());
      } catch (IOException e) {
        return TransactionStateAndResult.commitAndReturn(
          Response.status(Response.Status.BAD_REQUEST)
                  .entity(e.getMessage())
                  .build()
        );
      } catch (IllegalArgumentException e) {
        return TransactionStateAndResult.commitAndReturn(
          Response.status(Response.Status.BAD_REQUEST)
                  .entity(e.getMessage())
                  .build()
        );
      }
    });
  }

  private ChunkedOutput<String> executeUpload(@FormDataParam("file") final FormDataContentDisposition fileDetails,
                                              final String vreLabel, final String vreName,
                                              final InputStream inputStream) throws IOException {
    final ChunkedOutput<String> output = new ChunkedOutput<>(String.class);
    final AtomicInteger writeErrors = new AtomicInteger(0);

    File tempFile = File.createTempFile(fileDetails.getName(), null, null);
    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
      copy(inputStream, fos);
    }

    new Thread() {
      public void run() {
        try {
          uploadService.saveToDb(vreName, tempFile, fileDetails.getFileName(), vreLabel, msg -> {
            try {
              //write json objects
              if (writeErrors.get() < 5) {
                output.write(msg + "\n");
              }
            } catch (IOException e) {
              LOG.error("Could not write to output stream", e);
              writeErrors.incrementAndGet();
            }
          });
        } catch (InvalidFileException | IOException e) {
          LOG.error("Something went wrong while importing a file", e);
          try {
            if (writeErrors.get() < 5) {
              output.write("failure: " + e.getMessage());
            }
          } catch (IOException outputError) {
            LOG.error("Couldn't write to output stream", outputError);
          }
        } finally {
          try {
            output.close();
          } catch (IOException e) {
            LOG.error("Couldn't close the output stream", e);
          }
          try {
            String filePath = tempFile.getAbsolutePath();
            try {
              if (tempFile.delete()) {
                LOG.info("deleted tempfile " + filePath + " after import");
              } else {
                LOG.error("Couldn't remove file " + filePath);
              }
            } catch (Exception e) {
              LOG.error("Couldn't remove file " + filePath, e);
            }
          } catch (Exception e) {
            LOG.error("Error while getting path of tempfile, tempfile was not deleted", e);
          }
        }
      }
    }.start();
    return output;
  }

  private String stripFunnyCharacters(String vre) {
    return vre.replaceFirst("\\.[a-zA-Z]+$", "").replaceAll("[^a-zA-Z-]", "_");
  }

}
