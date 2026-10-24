package com.apidependencyradar.application;

import com.apidependencyradar.domain.detector.ApiChangeDetector;
import com.apidependencyradar.domain.model.ApiChange;
import com.apidependencyradar.infrastructure.openapi.OpenApiSpecFetcher;
import com.apidependencyradar.infrastructure.persistence.ApiChangeEntity;
import com.apidependencyradar.infrastructure.persistence.ApiChangeRepository;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyEntity;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyRepository;
import com.apidependencyradar.web.dto.CheckResultResponse;
import com.apidependencyradar.web.dto.CreateApiDependencyRequest;
import com.apidependencyradar.web.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiDependencyServiceTest {

        private static final String VALID_SPEC = "openapi: 3.0.3\ninfo:\n  title: Example\n  version: '1.0'\npaths: {}\n";

        @Mock
        ApiDependencyRepository dependencyRepository;
        @Mock
        ApiChangeRepository changeRepository;
        @Mock
        OpenApiSpecFetcher specFetcher;
        @Mock
        ApiChangeDetector changeDetector;

        @InjectMocks
        ApiDependencyService service;

        @Test
        void createFetchesAndStoresTheInitialBaseline() {
                when(specFetcher.fetch("https://example.com/openapi.yaml")).thenReturn(VALID_SPEC);
                when(changeDetector.detect(VALID_SPEC, VALID_SPEC)).thenReturn(List.of());
                when(dependencyRepository.saveAndFlush(any(ApiDependencyEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                var response = service.create(new CreateApiDependencyRequest(
                                " Example API ", " Example Provider ", "https://example.com/openapi.yaml", " test "));

                ArgumentCaptor<ApiDependencyEntity> captor = ArgumentCaptor.forClass(ApiDependencyEntity.class);
                verify(dependencyRepository).saveAndFlush(captor.capture());
                ApiDependencyEntity saved = captor.getValue();
                assertNotNull(saved.getId());
                assertEquals("Example API", saved.getName());
                assertEquals("Example Provider", saved.getProviderName());
                assertEquals(VALID_SPEC, saved.getBaselineSpec());
                assertEquals(saved.getId(), response.id());
                assertEquals("OPENAPI", response.sourceType());
        }

        @Test
        void checkNowPersistsDetectedChangesAndAdvancesTheBaseline() {
                ApiDependencyEntity dependency = new ApiDependencyEntity(
                                "dep-1",
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                String updatedSpec = VALID_SPEC + "# updated\n";

                ApiChange detected = new ApiChange(
                                "OPENAPI",
                                "RESPONSE_PROPERTY_REMOVED",
                                "GET /products/{id}",
                                "Response property 'name' was removed",
                                true);

                when(dependencyRepository.findById("dep-1"))
                                .thenReturn(Optional.of(dependency));

                when(specFetcher.fetch(dependency.getSourceUrl()))
                                .thenReturn(updatedSpec);

                when(changeDetector.detect(VALID_SPEC, updatedSpec))
                                .thenReturn(List.of(detected));

                when(changeRepository.saveAll(any()))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                when(dependencyRepository.save(any(ApiDependencyEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CheckResultResponse result = service.checkNow("dep-1");

                assertEquals("dep-1", result.dependencyId());
                assertEquals(1, result.changesDetected());
                assertEquals(1, result.breakingChangesDetected());

                assertEquals(
                                "RESPONSE_PROPERTY_REMOVED",
                                result.changes().get(0).type());

                assertEquals(
                                "GET /products/{id}",
                                result.changes().get(0).endpoint());

                assertEquals(
                                "Response property 'name' was removed",
                                result.changes().get(0).description());

                assertEquals(updatedSpec, dependency.getBaselineSpec());
                assertNotNull(dependency.getLastCheckedAt());

                verify(changeRepository).saveAll(argThat(changes -> {
                        if (changes == null) {
                                return false;
                        }

                        List<ApiChangeEntity> changeList = new java.util.ArrayList<>();
                        changes.forEach(changeList::add);

                        if (changeList.size() != 1) {
                                return false;
                        }

                        Object item = changeList.get(0);

                        if (!(item instanceof ApiChangeEntity entity)) {
                                return false;
                        }

                        return entity.isBreaking()
                                        && "RESPONSE_PROPERTY_REMOVED".equals(entity.getType())
                                        && "GET /products/{id}".equals(entity.getEndpoint())
                                        && "Response property 'name' was removed"
                                                        .equals(entity.getDescription());
                }));

                verify(dependencyRepository).save(dependency);
        }

        @Test
        void getUnknownDependencyThrowsNotFound() {
                when(dependencyRepository.findById("missing")).thenReturn(Optional.empty());
                assertThrows(ResourceNotFoundException.class, () -> service.get("missing"));
        }

        @Test
        void checkNowWithNoChangesPersistsNoChangeRecords() {
                ApiDependencyEntity dependency = new ApiDependencyEntity(
                                "dep-2",
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                when(dependencyRepository.findById("dep-2"))
                                .thenReturn(Optional.of(dependency));

                when(specFetcher.fetch(dependency.getSourceUrl()))
                                .thenReturn(VALID_SPEC);

                when(changeDetector.detect(VALID_SPEC, VALID_SPEC))
                                .thenReturn(List.of());

                when(dependencyRepository.save(any(ApiDependencyEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CheckResultResponse result = service.checkNow("dep-2");

                assertEquals(0, result.changesDetected());
                assertEquals(0, result.breakingChangesDetected());
                assertEquals(VALID_SPEC, dependency.getBaselineSpec());
                assertNotNull(dependency.getLastCheckedAt());

                verify(changeRepository).saveAll(argThat(changes -> changes != null && !changes.iterator().hasNext()));
                verify(dependencyRepository).save(dependency);
        }

        @Test
        void checkNowWhenFetchFailsDoesNotMutateState() {
                ApiDependencyEntity dependency = new ApiDependencyEntity(
                                "dep-fail-fetch",
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                when(dependencyRepository.findById("dep-fail-fetch"))
                                .thenReturn(Optional.of(dependency));
                when(specFetcher.fetch(dependency.getSourceUrl()))
                                .thenThrow(new IllegalStateException("Failed to fetch OpenAPI specification"));

                assertThrows(IllegalStateException.class, () -> service.checkNow("dep-fail-fetch"));

                assertEquals(VALID_SPEC, dependency.getBaselineSpec());
                verify(dependencyRepository, never()).save(any());
                verifyNoInteractions(changeRepository);
                verifyNoInteractions(changeDetector);
        }

        @Test
        void checkNowWhenSpecIsInvalidPreservesErrorTranslationAndDoesNotMutateState() {
                ApiDependencyEntity dependency = new ApiDependencyEntity(
                                "dep-invalid-spec",
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                String malformedSpec = "not a valid openapi specification";

                when(dependencyRepository.findById("dep-invalid-spec"))
                                .thenReturn(Optional.of(dependency));
                when(specFetcher.fetch(dependency.getSourceUrl()))
                                .thenReturn(malformedSpec);
                when(changeDetector.detect(VALID_SPEC, malformedSpec))
                                .thenThrow(new IllegalArgumentException("Unable to parse OpenAPI specification"));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> service.checkNow("dep-invalid-spec"));

                assertEquals("The source returned an invalid OpenAPI specification", exception.getMessage());
                assertNotNull(exception.getCause());
                assertEquals("Unable to parse OpenAPI specification", exception.getCause().getMessage());

                assertEquals(VALID_SPEC, dependency.getBaselineSpec());
                verify(dependencyRepository, never()).save(any());
                verifyNoInteractions(changeRepository);
        }

        @Test
        void checkNowWhenChangeDetectorThrowsUnexpectedExceptionDoesNotMutateState() {
                ApiDependencyEntity dependency = new ApiDependencyEntity(
                                "dep-detector-error",
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                String newSpec = VALID_SPEC + "# comment\n";

                when(dependencyRepository.findById("dep-detector-error"))
                                .thenReturn(Optional.of(dependency));
                when(specFetcher.fetch(dependency.getSourceUrl()))
                                .thenReturn(newSpec);
                when(changeDetector.detect(VALID_SPEC, newSpec))
                                .thenThrow(new RuntimeException("Unexpected parser failure"));

                RuntimeException exception = assertThrows(
                                RuntimeException.class,
                                () -> service.checkNow("dep-detector-error"));

                assertEquals("Unexpected parser failure", exception.getMessage());

                assertEquals(VALID_SPEC, dependency.getBaselineSpec());
                verify(dependencyRepository, never()).save(any());
                verifyNoInteractions(changeRepository);
        }

        @Test
        void getWithBlankIdThrowsIllegalArgumentException() {
                assertThrows(IllegalArgumentException.class, () -> service.get("   "));
        }

        @Test
        void getWithNullIdThrowsIllegalArgumentException() {
                assertThrows(IllegalArgumentException.class, () -> service.get(null));
        }

}
