package io.ebean.tools.init;

import java.io.File;

public class ProjectDetection {

  public DetectionMeta run() {

    DetectionMeta meta = new DetectionMeta();

    File pom = new File("pom.xml");
    if (pom.exists()) {
      meta.setMaven(new MavenPom(pom));
    }
    File gradleBuild = new File("build.gradle");
    if (gradleBuild.exists()) {
      meta.setGradle(new GradleBuild(gradleBuild));
    }

    File sourceJava = new File("src/main/java");
    if (sourceJava.exists()) {
      meta.setSourceJava(sourceJava);
    }

    File sourceKotlin = new File("src/main/kotlin");
    if (sourceKotlin.exists()) {
      meta.setSourceKotlin(sourceKotlin);
    }

    File testJava = new File("src/test/java");
    if (testJava.exists()) {
      meta.setSourceTestJava(testJava);
    }

    File testKotlin = new File("src/test/kotlin");
    if (testKotlin.exists()) {
      meta.setSourceTestKotlin(testKotlin);
    }

    File mainRes = new File("src/main/resources");
    if (mainRes.exists()) {
      meta.setMainResource(mainRes);
    }

    File testRes = new File("src/test/resources");
    if (testRes.exists()) {
      meta.setTestResource(testRes);
    }

    File targetClass = new File("target/classes");
    if (targetClass.exists()) {
      meta.setMainOutput(targetClass);
    } else {
      File outClass = new File("out/production/classes");
      if (outClass.exists()) {
        meta.setMainOutput(outClass);
      }
    }

    meta.setTestOutput(meta.getMainOutput());

    return meta;
  }

}
