package tech.petclinix.logic.service;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.petclinix.logic.domain.AppointmentData;
import tech.petclinix.logic.domain.AppointmentType;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.exception.AppointmentOverlapException;
import tech.petclinix.persistence.entity.LocationEntity;
import tech.petclinix.persistence.entity.OpeningPeriodEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VetEntity;
import tech.petclinix.persistence.jpa.AppointmentJpaRepository;
import tech.petclinix.persistence.jpa.LocationJpaRepository;
import tech.petclinix.persistence.jpa.OwnerJpaRepository;
import tech.petclinix.persistence.jpa.PetJpaRepository;
import tech.petclinix.persistence.jpa.VetJpaRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for {@link OwnerAppointmentService#persist} / {@link AppointmentService#persist}.
 *
 * Uses a real, full Spring context (not {@code @DataJpaTest}, whose per-test transaction
 * rollback would prevent either thread's transaction from ever really committing) and two
 * threads racing to book the exact same vet/slot at the same instant. Exactly one booking
 * must succeed; the other must fail with {@link AppointmentOverlapException} — whether caught
 * by the pessimistic-lock overlap check or by the database's unique constraint backstop.
 *
 * <p>Repeated several times to flush out timing-dependent flakiness before being trusted.
 */
@SpringBootTest
class AppointmentServiceConcurrencyTest {

    @Autowired
    OwnerAppointmentService ownerAppointmentService;

    @Autowired
    VetJpaRepository vetRepository;

    @Autowired
    OwnerJpaRepository ownerRepository;

    @Autowired
    PetJpaRepository petRepository;

    @Autowired
    LocationJpaRepository locationRepository;

    @Autowired
    AppointmentJpaRepository appointmentRepository;

    /** Exactly one of two simultaneous booking attempts for the same vet/slot succeeds; the other is rejected as an overlap. */
    @RepeatedTest(5)
    void concurrentBookingsForSameSlotOnlyOneSucceeds() throws Exception {
        //arrange
        var suffix = "-" + System.nanoTime();
        var vet = vetRepository.save(new VetEntity("conc-vet" + suffix, "hash"));
        var location = locationRepository.save(new LocationEntity(vet, "Concurrency Clinic", "UTC"));

        var startsAt = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
        int dayOfWeek = startsAt.getDayOfWeek().getValue();
        var period = new OpeningPeriodEntity(location, dayOfWeek, LocalTime.MIN, LocalTime.of(23, 59), 0);
        location.addWeeklyPeriod(period);
        locationRepository.save(location);

        var ownerA = ownerRepository.save(new OwnerEntity("conc-ownerA" + suffix, "hash"));
        var ownerB = ownerRepository.save(new OwnerEntity("conc-ownerB" + suffix, "hash"));
        var petA = petRepository.save(new PetEntity("Fluffy", ownerA));
        var petB = petRepository.save(new PetEntity("Rex", ownerB));

        AppointmentData dataA = appointmentData(location.getId(), petA.getId(), startsAt);
        AppointmentData dataB = appointmentData(location.getId(), petB.getId(), startsAt);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Object> futureA = pool.submit(bookingTask(ready, start, new Username(ownerA.getUsername()), dataA));
            Future<Object> futureB = pool.submit(bookingTask(ready, start, new Username(ownerB.getUsername()), dataB));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Object resultA = futureA.get(10, TimeUnit.SECONDS);
            Object resultB = futureB.get(10, TimeUnit.SECONDS);

            //assert
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger overlapRejections = new AtomicInteger();
            for (Object result : List.of(resultA, resultB)) {
                if (result instanceof AppointmentOverlapException) {
                    overlapRejections.incrementAndGet();
                } else {
                    successes.incrementAndGet();
                }
            }

            assertThat(successes.get()).isEqualTo(1);
            assertThat(overlapRejections.get()).isEqualTo(1);

            var booked = appointmentRepository.findAll(AppointmentJpaRepository.Specifications.byVet(vet));
            assertThat(booked).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private java.util.concurrent.Callable<Object> bookingTask(CountDownLatch ready, CountDownLatch start,
                                                                Username ownerUsername, AppointmentData data) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                return ownerAppointmentService.persist(ownerUsername, data);
            } catch (AppointmentOverlapException e) {
                return e;
            }
        };
    }

    private AppointmentData appointmentData(Long locationId, Long petId, LocalDateTime startsAt) {
        return new AppointmentData() {
            public Long locationId() { return locationId; }
            public Long petId() { return petId; }
            public LocalDateTime startsAt() { return startsAt; }
            public AppointmentType appointmentType() { return AppointmentType.CHECKUP; }
        };
    }
}
