package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import tech.petclinix.logic.domain.OwnerVisit;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.logic.service.mapper.EntityMapper;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import javax.swing.text.html.parser.Entity;
import java.util.List;

@Service
public class PetVisitService {

    private final PetService petService;
    private final VisitService visitService;

    public PetVisitService(PetService petService, VisitService visitService) {
        this.petService = petService;
        this.visitService = visitService;
    }

    public List<OwnerVisit> findAllVisitsByOwnerAndPet(Username ownerUsername, Long petId) {
        PetEntity pet = petService.retrieveByOwnerAndId(ownerUsername, petId);
        return visitService.findAllByPet(pet).stream()
                .map(EntityMapper::toOwnerVisit)
                .toList();
    }

}
