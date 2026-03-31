package tech.petclinix;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "tech.petclinix", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final String ROOT = "tech.petclinix";

    // -------------------------------------------------------------------------
    // Layer definitions
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule layer_dependencies_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(ROOT + "..")

            .layer("Web Controllers").definedBy(ROOT + ".web.controller")
            .layer("Web Mappers").definedBy(ROOT + ".web.controller.mapper..")
            .layer("Web DTOs").definedBy(ROOT + ".web.dto..")

            .layer("Logic Services").definedBy(ROOT + ".logic.service")
            .layer("Logic Mappers").definedBy(ROOT + ".logic.service.mapper..")
            .layer("Logic Domain").definedBy(ROOT + ".logic.domain..")

            .layer("JPA Repositories").definedBy(ROOT + ".persistence.jpa..")
            .layer("JPA Entities").definedBy(ROOT + ".persistence.entity..")

            .layer("Security").definedBy(ROOT + ".security..")

            // Web Controllers and Web mappers are allowed to access JPA Entities, this is a pragmatic choice to avoid having to create separate
            // DTOs for all entities, especially since the controllers only return a subset of the entity data and do not
            // expose any sensitive information. In a larger application, it might be better to enforce that controllers
            // only access services and mappers, and never directly access entities.
            .whereLayer("Web Controllers").mayOnlyAccessLayers("Web Mappers", "Web DTOs", "Logic Services", "Logic Domain", "JPA Entities", "Security")
            .whereLayer("Web Mappers").mayOnlyAccessLayers("Web DTOs", "Logic Domain", "JPA Entities")
            .whereLayer("Web DTOs").mayOnlyAccessLayers("Logic Domain")

            .whereLayer("Logic Services").mayOnlyAccessLayers("Logic Mappers", "Logic Domain", "JPA Repositories", "JPA Entities")
            .whereLayer("Logic Mappers").mayOnlyAccessLayers("Logic Domain", "JPA Entities")

            .whereLayer("JPA Repositories").mayOnlyAccessLayers("JPA Entities")
            .whereLayer("JPA Entities").mayOnlyAccessLayers("Logic Domain");

    // -------------------------------------------------------------------------
    // Naming conventions
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule controllers_are_named_controller = classes()
            .that().resideInAPackage(ROOT + ".web.controller")
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("Controller")
            .as("Classes in web.controller must be named *Controller");

    @ArchTest
    static final ArchRule services_are_named_service = classes()
            .that().resideInAPackage(ROOT + ".logic.service")
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("Service")
            .as("Classes in logic.service must be named *Service");

    @ArchTest
    static final ArchRule entities_are_named_entity = classes()
            .that().resideInAPackage(ROOT + ".persistence.entity..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .and().haveSimpleNameNotEndingWith("_")   // exclude JPA metamodel
            .should().haveSimpleNameEndingWith("Entity")
            .as("Classes in persistence.entity must be named *Entity");

    @ArchTest
    static final ArchRule repositories_are_named_jpa_repository = classes()
            .that().resideInAPackage(ROOT + ".persistence.jpa..")
            .and().areInterfaces()
            .and().areNotInnerClasses()
            .should().haveSimpleNameEndingWith("JpaRepository")
            .as("Interfaces in persistence.jpa must be named *JpaRepository");

    // -------------------------------------------------------------------------
    // Annotation conventions
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule controllers_are_annotated_with_rest_controller = classes()
            .that().resideInAPackage(ROOT + ".web.controller")
            .and().areNotInterfaces()
            .should().beAnnotatedWith(RestController.class)
            .as("All controller classes must be annotated with @RestController");

    @ArchTest
    static final ArchRule services_are_annotated_with_service = classes()
            .that().resideInAPackage(ROOT + ".logic.service")
            .and().areNotInterfaces()
            .should().beAnnotatedWith(Service.class)
            .as("All service classes must be annotated with @Service");

    // -------------------------------------------------------------------------
    // Forbidden dependencies
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule entities_do_not_depend_on_services = noClasses()
            .that().resideInAPackage(ROOT + ".persistence.entity..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".logic.service..")
            .as("Entities must not depend on services");

    @ArchTest
    static final ArchRule entities_do_not_depend_on_controllers = noClasses()
            .that().resideInAPackage(ROOT + ".persistence.entity..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".web..")
            .as("Entities must not depend on controllers or DTOs");

    @ArchTest
    static final ArchRule dtos_do_not_depend_on_services = noClasses()
            .that().resideInAPackage(ROOT + ".web.dto..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".logic.service..")
            .as("DTOs must not depend on services");

    @ArchTest
    static final ArchRule repositories_do_not_depend_on_services = noClasses()
            .that().resideInAPackage(ROOT + ".persistence.jpa..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".logic..")
            .as("Repositories must not depend on services");

    @ArchTest
    static final ArchRule repositories_do_not_depend_on_controllers = noClasses()
            .that().resideInAPackage(ROOT + ".persistence.jpa..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".web..")
            .as("Repositories must not depend on controllers or DTOs");
}
