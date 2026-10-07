package com.needlos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

/** Reglas de arquitectura (Reglas §2) verificadas en cada build. */
@AnalyzeClasses(packages = "com.needlos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule controladoresNoUsanRepositorios =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Repository")
                    .because("el controller delega en el service (Reglas §2.2)");

    @ArchTest
    static final ArchRule controladoresNoExponenEntidades =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .dependOnClassesThat()
                    .areAnnotatedWith(Entity.class)
                    .because("la API solo recibe y devuelve DTOs (Reglas §2.2)");

    @ArchTest
    static final ArchRule serviciosNoConocenHttp =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Service")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
                    .because("el service no conoce HTTP (Reglas §2.2)");

    @ArchTest
    static final ArchRule commonNoDependeDeFuncionalidades =
            noClasses()
                    .that()
                    .resideInAPackage("com.needlos.common..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.needlos.security..",
                            "com.needlos.tenant..",
                            "com.needlos.clientes..",
                            "com.needlos.ordenes..",
                            "com.needlos.tipoprenda..",
                            "com.needlos.dev..")
                    .because("common es transversal y no conoce las funcionalidades (Reglas §2.1)");

    @ArchTest
    static final ArchRule funcionalidadesSinCiclos =
            slices().matching("com.needlos.(*)..").should().beFreeOfCycles();
}
