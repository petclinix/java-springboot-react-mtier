package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.domain.VetAppointment;
import tech.petclinix.logic.service.mapper.EntityMapper;

import java.util.List;

@Service
public class VetAppointmentService {
    private final AppointmentService appointmentService;

    public VetAppointmentService(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Transactional(readOnly = true)
    public List<VetAppointment> findAllByVet(Username vetUsername) {
        return appointmentService.findAllByVet(vetUsername).stream()
                .map(EntityMapper::toVetAppointment)
                .toList();
    }

    @Transactional
    public void cancelByVet(Username username, Long id) {
        appointmentService.cancelByVet(username, id);
    }
}
