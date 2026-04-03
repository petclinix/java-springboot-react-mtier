package tech.petclinix;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "tech.petclinix", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final String ROOT = "tech.petclinix";

    // -------------------------------------------------------------------------
    // Design Constraint 1 — Dependency direction
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

            // web → logic only; persistence is not accessible from the web layer
            .whereLayer("Web Controllers").mayOnlyAccessLayers("Web Mappers", "Web DTOs", "Logic Services", "Logic Domain", "Security")
            .whereLayer("Web Mappers").mayOnlyAccessLayers("Web DTOs", "Logic Domain")
            .whereLayer("Web DTOs").mayOnlyAccessLayers("Logic Domain")

            // services may call other services (orchestrating services pattern)
            .whereLayer("Logic Services").mayOnlyAccessLayers("Logic Services", "Logic Mappers", "Logic Domain", "JPA Repositories", "JPA Entities")
            .whereLayer("Logic Mappers").mayOnlyAccessLayers("Logic Domain", "JPA Entities")

            .whereLayer("JPA Repositories").mayOnlyAccessLayers("JPA Entities", "Logic Domain")
            .whereLayer("JPA Entities").mayOnlyAccessLayers("Logic Domain")

            .whereLayer("Security").mayOnlyAccessLayers("Logic Domain", "Logic Services");

    // -------------------------------------------------------------------------
    // Design Constraint 2 — logic/domain has no framework dependencies
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule logic_domain_does_not_depend_on_application_layers = noClasses()
            .that().resideInAPackage(ROOT + ".logic.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + ".persistence..",
                    ROOT + ".web..",
                    ROOT + ".security.."
            )
            .as("logic/domain must not depend on persistence, web, or security");

    @ArchTest
    static final ArchRule logic_domain_does_not_depend_on_spring = noClasses()
            .that().resideInAPackage(ROOT + ".logic.domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .as("logic/domain must not depend on any Spring framework class");

    // -------------------------------------------------------------------------
    // Design Constraints 3 & 4 — No persistence entity crosses the service boundary
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule web_layer_must_not_depend_on_persistence = noClasses()
            .that().resideInAPackage(ROOT + ".web..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".persistence..")
            .because("Entities must not cross the service boundary into the web layer. " +
                     "Services must map entities to domain records before returning them.");

    // -------------------------------------------------------------------------
    // Design Constraint 5 — Services do not depend on the web layer
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule logic_must_not_depend_on_web = noClasses()
            .that().resideInAPackage(ROOT + ".logic..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".web..")
            .as("The logic layer must not depend on the web layer");

    // -------------------------------------------------------------------------
    // Design Constraint 6 — Services receive Username, not Authentication
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule services_must_not_depend_on_spring_authentication = noClasses()
            .that().resideInAPackage(ROOT + ".logic.service")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.security.core.Authentication")
            .as("Services must receive Username, not Authentication. " +
                "Controllers must extract and wrap the username before calling a service.");

    // -------------------------------------------------------------------------
    // Design Constraint 8 — Each controller depends on exactly one service
    // -------------------------------------------------------------------------

    @ArchTest
    static final ArchRule each_controller_depends_on_exactly_one_service = classes()
            .that().resideInAPackage(ROOT + ".web.controller")
            .and().areNotInterfaces()
            .should(dependOnExactlyOneService())
            .as("Each controller must depend on exactly one service from logic.service");

    private static ArchCondition<JavaClass> dependOnExactlyOneService() {
        return new ArchCondition<JavaClass>("depend on exactly one class from logic.service") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long serviceCount = javaClass.getDirectDependenciesFromSelf().stream()
                        .filter(dep -> dep.getTargetClass().getPackageName().equals(ROOT + ".logic.service"))
                        .map(dep -> dep.getTargetClass().getName())
                        .distinct()
                        .count();
                if (serviceCount != 1) {
                    events.add(SimpleConditionEvent.violated(javaClass, String.format(
                            "%s depends on %d service(s), expected exactly 1",
                            javaClass.getName(), serviceCount)));
                }
            }
        };
    }

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
            .and().areTopLevelClasses()
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
            .and().areAssignableTo(JpaRepository.class)
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
            .and().areTopLevelClasses()
            .should().beAnnotatedWith(Service.class)
            .as("All service classes must be annotated with @Service");
}
