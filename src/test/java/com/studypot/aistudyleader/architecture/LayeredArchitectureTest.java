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
		.should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "..application..", "..adapter..");

	@ArchTest
	static final ArchRule applicationCodeDoesNotDependOnAdapters = noClasses()
		.that().resideInAPackage("..application..")
		.should().dependOnClassesThat().resideInAnyPackage("..adapter..");

	@ArchTest
	static final ArchRule productionControllersLiveInInboundWebAdapters = classes()
		.that().areAnnotatedWith(RestController.class)
		.should().resideInAPackage("..adapter.in.web..")
		.allowEmptyShould(true);
}
