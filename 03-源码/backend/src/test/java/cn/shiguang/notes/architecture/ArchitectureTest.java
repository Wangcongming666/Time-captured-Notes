package cn.shiguang.notes.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  private final JavaClasses classes = new ClassFileImporter().importPackages("cn.shiguang.notes");

  @Test
  void domainDoesNotDependOnFrameworkOrTransport() {
    noClasses()
        .that()
        .resideInAPackage("..modules..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "..web..", "..workflows..", "..platform..")
        .check(classes);
  }

  @Test
  void modulesCannotCallOtherModuleImplementations() {
    for (String module :
        new String[] {
          "identity",
          "content",
          "social",
          "collection",
          "governance",
          "discovery",
          "notification",
          "statistics"
        }) {
      noClasses()
          .that()
          .resideInAPackage("..modules." + module + "..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..web..", "..workflows..")
          .check(classes);
      for (String other :
          new String[] {
            "identity",
            "content",
            "social",
            "collection",
            "governance",
            "discovery",
            "notification",
            "statistics"
          })
        if (!module.equals(other))
          noClasses()
              .that()
              .resideInAPackage("..modules." + module + "..")
              .should()
              .dependOnClassesThat()
              .resideInAnyPackage(
                  "..modules." + other + ".application..",
                  "..modules." + other + ".infrastructure..",
                  "..modules." + other + ".domain..")
              .check(classes);
    }
  }
}
