package tech.petclinix.logic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.ActivityLogEntity;
import tech.petclinix.persistence.jpa.ActivityLogJpaRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ActivityLogService}.
 *
 * Repository is mocked — no database.
 */
@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogJpaRepository repository;

    @Captor
    private ArgumentCaptor<ActivityLogEntity> entityCaptor;

    private ActivityLogService activityLogService;

    @BeforeEach
    void setUp() {
        activityLogService = new ActivityLogService(repository);
    }

    /** Saves a correctly-shaped ActivityLogEntity when an ActionEvent is published. */
    @Test
    void onActionSavesActivityLogEntity() {
        //arrange
        var event = new ActionEvent(new Username("alice"), "PET_CREATED");
        when(repository.save(any(ActivityLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //act
        activityLogService.onAction(event);

        //assert
        verify(repository).save(entityCaptor.capture());
        var saved = entityCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getAction()).isEqualTo("PET_CREATED");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    /** Maps entities to ActivityLogEntry records, newest first, capped at 200 entries. */
    @Test
    void findRecentMapsEntitiesToActivityLogEntries() {
        //arrange
        var entity1 = new ActivityLogEntity("alice", "PET_CREATED");
        var entity2 = new ActivityLogEntity("bob", "USER_LOGIN");
        var page = new PageImpl<>(List.of(entity1, entity2));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        //act
        var result = activityLogService.findRecent();

        //assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("alice");
        assertThat(result.get(0).action()).isEqualTo("PET_CREATED");
        assertThat(result.get(1).username()).isEqualTo("bob");
        assertThat(result.get(1).action()).isEqualTo("USER_LOGIN");
    }

    /** Returns an empty list when there are no activity log entries. */
    @Test
    void findRecentReturnsEmptyListWhenNoEntriesExist() {
        //arrange
        when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        //act
        var result = activityLogService.findRecent();

        //assert
        assertThat(result).isEmpty();
    }
}
