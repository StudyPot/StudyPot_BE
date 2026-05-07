package com.studypot.aistudyleader.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "com.studypot.aistudyleader", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

	@ArchTest
	static final ArchRule domainCodeDoesNotDependOnSpringOrAdapters = noClasses()
		.that().resideInAPackage("..domain..")
		.should().dependOnClassesThat().resideInAnyPackage(
			"org.springframework..",
			"jakarta..",
			"..controller..",
			"..service..",
			"..repository..",
			"..infrastructure.."
		);

	@ArchTest
	static final ArchRule serviceCodeDoesNotDependOnControllersOrInfrastructure = noClasses()
		.that().resideInAPackage("..service..")
		.should().dependOnClassesThat().resideInAnyPackage("..controller..", "..infrastructure..");

	@ArchTest
	static final ArchRule repositoryCodeDoesNotDependOnControllers = noClasses()
		.that().resideInAPackage("..repository..")
		.should().dependOnClassesThat().resideInAnyPackage("..controller..");

	@ArchTest
	static final ArchRule productionControllersLiveInControllerPackages = classes()
		.that().areAnnotatedWith(RestController.class)
		.should().resideInAPackage("..controller..")
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule legacyHexagonalAndSharedPackagesAreNotUsed = noClasses()
		.should().resideInAnyPackage("..adapter..", "..identity.application..", "..shared..");
}
