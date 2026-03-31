package tech.petclinix.logic.service;

import org.springframework.stereotype.Service;
import tech.petclinix.logic.domain.Username;
import tech.petclinix.persistence.entity.PetEntity;
import tech.petclinix.persistence.entity.VisitEntity;

import java.util.List;

@Service
public class PetVisitService {

    private final PetService petService;
    private final VisitService visitService;

    public PetVisitService(PetService petService, VisitService visitService) {
        this.petService = petService;
        this.visitService = visitService;
    }

    public List<VisitEntity> findAllVisitsByOwnerAndPet(Username ownerUsername, Long petId) {
        PetEntity pet = petService.retrieveByOwnerAndId(ownerUsername, petId);
        return visitService.findAllByPet(pet);
    }

}
