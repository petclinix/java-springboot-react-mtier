package tech.petclinix.logic.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import tech.petclinix.logic.domain.ActionEvent;
import tech.petclinix.logic.domain.ActivityLogEntry;
import tech.petclinix.logic.service.mapper.EntityMapper;
import tech.petclinix.persistence.entity.ActivityLogEntity;
import tech.petclinix.persistence.jpa.ActivityLogJpaRepository;

import java.util.List;

@Service
public class ActivityLogService {

    private static final int MAX_ENTRIES = 200;

    private final ActivityLogJpaRepository repository;

    public ActivityLogService(ActivityLogJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Runs after the triggering transaction commits, and asynchronously (off the request
     * thread), so an activity-log write failure never rolls back the business transaction
     * that triggered it, and logging never adds latency to the critical path. By the time
     * AFTER_COMMIT fires, the original transaction has already completed, so there is no
     * transaction to join — {@code REQUIRES_NEW} opens a fresh one for this write (Spring
     * requires REQUIRES_NEW or NOT_SUPPORTED here; plain REQUIRED is rejected at startup).
     * Failures here are logged by Spring's default {@code AsyncUncaughtExceptionHandler}
     * and do not propagate — this log is best-effort by design.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAction(ActionEvent event) {
        repository.save(new ActivityLogEntity(event.actor().value(), event.action()));
    }

    @Transactional(readOnly = true)
    public List<ActivityLogEntry> findRecent() {
        return repository.findAll(PageRequest.of(0, MAX_ENTRIES, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent().stream()
                .map(EntityMapper::toActivityLogEntry)
                .toList();
    }
}
