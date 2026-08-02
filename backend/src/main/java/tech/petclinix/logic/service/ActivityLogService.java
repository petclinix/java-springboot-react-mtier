package tech.petclinix.logic.service;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @EventListener
    @Transactional
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
