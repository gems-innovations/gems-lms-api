package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.*;
import com.gems.admin.application.command.InstitutionCommand;
import com.gems.admin.application.response.InstitutionMetadataResponse;
import com.gems.admin.application.response.InstitutionResponse;
import com.gems.admin.infrastructure.driving.rest.request.InstitutionMetadataRequest;
import com.gems.admin.infrastructure.driving.rest.request.InstitutionRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InstitutionControllerTest {

  private CreateInstitutionUseCase createInstitutionUseCase;
  private GetInstitutionByIdUseCase getInstitutionByIdUseCase;
  private GetAllInstitutionsUseCase getAllInstitutionsUseCase;
  private UpdateInstitutionUseCase updateInstitutionUseCase;
  private DeleteInstitutionUseCase deleteInstitutionUseCase;
  private WebTestClient webTestClient;

  private InstitutionResponse institutionResponse;

  @BeforeEach
  void setUp() {
    createInstitutionUseCase = Mockito.mock(CreateInstitutionUseCase.class);
    getInstitutionByIdUseCase = Mockito.mock(GetInstitutionByIdUseCase.class);
    getAllInstitutionsUseCase = Mockito.mock(GetAllInstitutionsUseCase.class);
    updateInstitutionUseCase = Mockito.mock(UpdateInstitutionUseCase.class);
    deleteInstitutionUseCase = Mockito.mock(DeleteInstitutionUseCase.class);

    InstitutionController controller = new InstitutionController(
      createInstitutionUseCase,
      getInstitutionByIdUseCase,
      getAllInstitutionsUseCase,
      updateInstitutionUseCase,
      deleteInstitutionUseCase
    );

    webTestClient = WebTestClient.bindToController(controller).build();

    InstitutionMetadataResponse metadataResponse = new InstitutionMetadataResponse(
      "inst-1", "Description", "http://gems.edu", "contact@gems.edu",
      "123456", "Address", "PREMIUM", 100, LocalDateTime.now()
    );

    institutionResponse = new InstitutionResponse(
      "inst-1", "Gems College", "COLLEGE", "ACTIVE", 0,
      LocalDateTime.now(), LocalDateTime.now(), metadataResponse
    );
  }

  @Test
  void shouldCreateInstitutionSuccessfully() {
    InstitutionMetadataRequest metaReq = new InstitutionMetadataRequest(
      "Description", "http://gems.edu", "contact@gems.edu", "123456",
      "Address", "PREMIUM", 100
    );
    InstitutionRequest request = new InstitutionRequest(
      "inst-1", "Gems College", "COLLEGE", "ACTIVE", metaReq
    );

    when(createInstitutionUseCase.execute(any(InstitutionCommand.class)))
      .thenReturn(Mono.just(institutionResponse));

    webTestClient.post()
      .uri("/api/v1/institutions")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(InstitutionResponse.class)
      .value(response -> {
        Assertions.assertEquals("inst-1", response.id());
        Assertions.assertEquals("Gems College", response.name());
        Assertions.assertEquals("COLLEGE", response.type());
      });

    verify(createInstitutionUseCase, times(1)).execute(any(InstitutionCommand.class));
  }

  @Test
  void shouldGetInstitutionByIdSuccessfully() {
    when(getInstitutionByIdUseCase.execute("inst-1"))
      .thenReturn(Mono.just(institutionResponse));

    webTestClient.get()
      .uri("/api/v1/institutions/inst-1")
      .exchange()
      .expectStatus().isOk()
      .expectBody(InstitutionResponse.class)
      .value(response -> {
        Assertions.assertEquals("inst-1", response.id());
        Assertions.assertEquals("Gems College", response.name());
      });

    verify(getInstitutionByIdUseCase, times(1)).execute("inst-1");
  }

  @Test
  void shouldGetAllInstitutionsSuccessfully() {
    when(getAllInstitutionsUseCase.execute())
      .thenReturn(Flux.just(institutionResponse));

    webTestClient.get()
      .uri("/api/v1/institutions")
      .exchange()
      .expectStatus().isOk()
      .expectBodyList(InstitutionResponse.class)
      .hasSize(1);

    verify(getAllInstitutionsUseCase, times(1)).execute();
  }

  @Test
  void shouldUpdateInstitutionSuccessfully() {
    InstitutionMetadataRequest metaReq = new InstitutionMetadataRequest(
      "Description", "http://gems.edu", "contact@gems.edu", "123456",
      "Address", "PREMIUM", 100
    );
    InstitutionRequest request = new InstitutionRequest(
      "inst-1", "Gems College", "COLLEGE", "ACTIVE", metaReq
    );

    when(updateInstitutionUseCase.execute(anyString(), any(InstitutionCommand.class)))
      .thenReturn(Mono.just(institutionResponse));

    webTestClient.put()
      .uri("/api/v1/institutions/inst-1")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk();

    verify(updateInstitutionUseCase, times(1)).execute(eq("inst-1"), any(InstitutionCommand.class));
  }

  @Test
  void shouldDeleteInstitutionSuccessfully() {
    when(deleteInstitutionUseCase.execute("inst-1"))
      .thenReturn(Mono.empty());

    webTestClient.delete()
      .uri("/api/v1/institutions/inst-1")
      .exchange()
      .expectStatus().isNoContent();

    verify(deleteInstitutionUseCase, times(1)).execute("inst-1");
  }
}
