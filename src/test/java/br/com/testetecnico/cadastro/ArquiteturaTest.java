package br.com.testetecnico.cadastro;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.testetecnico.cadastro", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String BASE = "br.com.testetecnico.cadastro.";

    @ArchTest
    static final ArchRule controllerNaoAcessaRepository = noClasses()
            .that().resideInAPackage(BASE + "controller..")
            .should().dependOnClassesThat().resideInAPackage(BASE + "repository..");

    @ArchTest
    static final ArchRule modelERepositoryNaoSobemDeCamada = noClasses()
            .that().resideInAnyPackage(BASE + "model..", BASE + "repository..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + "service..", BASE + "controller..", BASE + "dto..", BASE + "client..");

    @ArchTest
    static final ArchRule serviceNaoConheceHttp = noClasses()
            .that().resideInAPackage(BASE + "service..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + "controller..", BASE + "dto..");

    @ArchTest
    static final ArchRule clientNaoConheceONegocio = noClasses()
            .that().resideInAPackage(BASE + "client..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + "service..", BASE + "controller..", BASE + "repository..", BASE + "dto..", BASE + "model..");

    @ArchTest
    static final ArchRule controllerNaoAcessaClient = noClasses()
            .that().resideInAPackage(BASE + "controller..")
            .should().dependOnClassesThat().resideInAPackage(BASE + "client..");
}
