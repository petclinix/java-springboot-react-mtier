package tech.petclinix.persistence.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tech.petclinix.persistence.entity.ActivityLogEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link ActivityLogJpaRepository}.
 *
 * Verifies that paging and sorting via {@code findAll(Pageable)} returns entries newest-first.
 * Happy path only — no mocking, full JPA stack loaded via {@code @DataJpaTest}.
 */
@DataJpaTest
class ActivityLogJpaRepositoryIntegrationTest {

    @Autowired
    ActivityLogJpaRepository repository;

    /** Returns the most recent activity log entries first when paged and sorted by timestamp descending. */
    @Test
    void findAllWithPagingAndSortingReturnsEntriesNewestFirst() throws InterruptedException {
        //arrange
        var first = repository.save(new ActivityLogEntity("alice", "USER_REGISTERED"));
        Thread.sleep(5);
        var second = repository.save(new ActivityLogEntity("alice", "USER_LOGIN"));
        Thread.sleep(5);
        var third = repository.save(new ActivityLogEntity("bob", "PET_CREATED"));

        //act
        var page = repository.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "timestamp")));

        //assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getId()).isEqualTo(third.getId());
        assertThat(page.getContent().get(1).getId()).isEqualTo(second.getId());
        assertThat(page.getContent().get(2).getId()).isEqualTo(first.getId());
    }

    /** Returns an empty page when no activity log entries exist. */
    @Test
    void findAllWithPagingAndSortingReturnsEmptyWhenNoEntriesExist() {
        //act
        var page = repository.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "timestamp")));

        //assert
        assertThat(page.getContent()).isEmpty();
    }
}
