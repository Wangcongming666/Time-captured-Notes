package cn.shiguang.notes.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.*;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("cn.shiguang.notes");

  @Test
  void classicLayersExistAndLegacyPackagesAreRemoved() {
    for (String type :
        new String[] {
          "controller.ContentController",
          "controller.IdentityController",
          "service.ContentService",
          "service.application.ContentApplicationService",
          "service.impl.ContentServiceImpl",
          "mapper.UserMapper",
          "entity.UserEntity"
        })
      assertTrue(classes.contain("cn.shiguang.notes." + type), "Missing classic layer: " + type);
    for (var type : classes)
      for (String legacy : new String[] {"modules", "platform", "shared", "web", "workflows"})
        assertFalse(
            type.getPackageName().startsWith("cn.shiguang.notes." + legacy + "."), type.getName());
  }

  @Test
  void controllersUseServicesInsteadOfPersistenceOrStorage() {
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..repository..", "..mapper..", "..service.impl..", "..storage..")
        .check(classes);
  }

  @Test
  void serviceAndPersistenceDoNotDependOnTransport() {
    noClasses()
        .that()
        .resideInAnyPackage("..service..", "..repository..", "..mapper..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..controller..")
        .check(classes);
    noClasses()
        .that()
        .resideInAnyPackage("..repository..", "..mapper..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..service..")
        .check(classes);
  }

  @Test
  void productionPersistenceUsesMappersInsteadOfGenericJdbc() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework.jdbc.core..")
        .check(classes);
    assertFalse(classes.contain("cn.shiguang.notes.repository.support.Database"));
    assertFalse(classes.contain("cn.shiguang.notes.service.CommunityService"));
    assertFalse(classes.contain("cn.shiguang.notes.service.QueryService"));
  }

  @Test
  void pureValidationRemainsIndependentOfFrameworksAndIo() {
    noClasses()
        .that()
        .resideInAPackage("..validation..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "..repository..", "..controller..", "..storage..")
        .check(classes);
  }
}
