package io.github.darklight606.paymentwebhook;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(
        packages = "io.github.darklight606.paymentwebhook",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ARCHFIXTURE_PACKAGE = "io.github.darklight606.paymentwebhook.archfixture";

    // The hexagonal packages hold no classes until later milestones add them, so each rule
    // allows an empty "should" instead of failing on a selection with nothing in it yet.
    private static final ArchRule DOMAIN_INDEPENDENT_OF_FRAMEWORKS = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "javax.persistence..", "org.hibernate..")
            .allowEmptyShould(true);

    private static final ArchRule DOMAIN_INDEPENDENT_OF_APPLICATION_AND_ADAPTER = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..")
            .allowEmptyShould(true);

    private static final ArchRule APPLICATION_INDEPENDENT_OF_ADAPTER = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainIndependentOfFrameworks = DOMAIN_INDEPENDENT_OF_FRAMEWORKS;

    @ArchTest
    static final ArchRule domainIndependentOfApplicationAndAdapter = DOMAIN_INDEPENDENT_OF_APPLICATION_AND_ADAPTER;

    @ArchTest
    static final ArchRule applicationIndependentOfAdapter = APPLICATION_INDEPENDENT_OF_ADAPTER;

    @Test
    void domainIndependentOfFrameworks_domainFixtureImportsSpring_violationDetected() {
        JavaClasses fixtureClasses = new ClassFileImporter().importPackages(ARCHFIXTURE_PACKAGE);

        assertThatThrownBy(() -> DOMAIN_INDEPENDENT_OF_FRAMEWORKS.check(fixtureClasses))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void domainIndependentOfApplicationAndAdapter_domainFixtureImportsApplication_violationDetected() {
        JavaClasses fixtureClasses = new ClassFileImporter().importPackages(ARCHFIXTURE_PACKAGE);

        assertThatThrownBy(() -> DOMAIN_INDEPENDENT_OF_APPLICATION_AND_ADAPTER.check(fixtureClasses))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void applicationIndependentOfAdapter_applicationFixtureImportsAdapter_violationDetected() {
        JavaClasses fixtureClasses = new ClassFileImporter().importPackages(ARCHFIXTURE_PACKAGE);

        assertThatThrownBy(() -> APPLICATION_INDEPENDENT_OF_ADAPTER.check(fixtureClasses))
                .isInstanceOf(AssertionError.class);
    }
}
