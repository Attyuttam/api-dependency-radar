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

import com.apidependencyradar.infrastructure.persistence.AppUserEntity;
import com.apidependencyradar.infrastructure.persistence.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
        @Mock
        AppUserRepository userRepository;

        @InjectMocks
        ApiDependencyService service;

        @BeforeEach
        void setUpAuthenticatedUser() {
                AppUserEntity owner = new AppUserEntity(
                                "user-1",
                                "test-user",
                                "test-password-hash");

                when(userRepository.findByEmailIgnoreCase("test-user"))
                                .thenReturn(Optional.of(owner));

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "test-user",
                                                "not-a-real-password",
                                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }

        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void createFetchesAndStoresTheInitialBaseline() {
                when(dependencyRepository.existsByOwner_IdAndSourceUrl(
                                "user-1", "https://example.com/openapi.yaml"))
                                .thenReturn(false);
                when(specFetcher.fetch("https://example.com/openapi.yaml")).thenReturn(VALID_SPEC);
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
                                new AppUserEntity("user-1", "test-user", "test-password-hash"),
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

                when(dependencyRepository.findByIdAndOwner_Id("dep-1", "user-1"))
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
                when(dependencyRepository.findByIdAndOwner_Id("missing", "user-1")).thenReturn(Optional.empty());
                assertThrows(ResourceNotFoundException.class, () -> service.get("missing"));
        }

        @Test
        void checkNowWithNoChangesPersistsNoChangeRecords() {
                ApiDependencyEntity dependency = new ApiDependencyEntity(
                                "dep-2",
                                new AppUserEntity("user-1", "test-user", "test-password-hash"),
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                when(dependencyRepository.findByIdAndOwner_Id("dep-2", "user-1"))
                                .thenReturn(Optional.of(dependency));

                when(specFetcher.fetch(dependency.getSourceUrl()))
                                .thenReturn(VALID_SPEC);

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
                                new AppUserEntity("user-1", "test-user", "test-password-hash"),
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                when(dependencyRepository.findByIdAndOwner_Id("dep-fail-fetch", "user-1"))
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
                                new AppUserEntity("user-1", "test-user", "test-password-hash"),
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                String malformedSpec = "not a valid openapi specification";

                when(dependencyRepository.findByIdAndOwner_Id("dep-invalid-spec", "user-1"))
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
                                new AppUserEntity("user-1", "test-user", "test-password-hash"),
                                "Example API",
                                null,
                                "https://example.com/openapi.yaml",
                                "OPENAPI",
                                null,
                                VALID_SPEC);

                String newSpec = VALID_SPEC + "# comment\n";

                when(dependencyRepository.findByIdAndOwner_Id("dep-detector-error", "user-1"))
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
