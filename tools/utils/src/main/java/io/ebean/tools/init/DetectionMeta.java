package io.ebean.tools.init;

import java.io.File;

public class DetectionMeta {

  private boolean gradle;
  private boolean maven;
  private MavenPom mavenPom;
  private GradleBuild gradleBuild;

  private File mainResource;
  private File testResource;

  private File sourceJava;
  private File sourceKotlin;

  private File sourceTestJava;
  private File sourceTestKotlin;

  private File mainOutput;
  private File testOutput;

  public void setSourceJava(File sourceJava) {
    this.sourceJava = sourceJava;
  }

  public void setSourceKotlin(File sourceKotlin) {
    this.sourceKotlin = sourceKotlin;
  }

  public void setSourceTestJava(File sourceTestJava) {
    this.sourceTestJava = sourceTestJava;
  }

  public void setSourceTestKotlin(File sourceTestKotlin) {
    this.sourceTestKotlin = sourceTestKotlin;
  }

  public File getMainResource() {
    return mainResource;
  }

  public void setMainResource(File mainResource) {
    this.mainResource = mainResource;
  }

  public File getTestResource() {
    return testResource;
  }

  public void setTestResource(File testResource) {
    this.testResource = testResource;
  }

  public void setMainOutput(File mainOutput) {
    this.mainOutput = mainOutput;
  }

  public File getMainOutput() {
    return mainOutput;
  }

  public void setTestOutput(File testOutput) {
    this.testOutput = testOutput;
  }

  public File getTestOutput() {
    return testOutput;
  }

  public File getSourceJava() {
    return sourceJava;
  }

  public File getSourceKotlin() {
    return sourceKotlin;
  }

  public File getSourceTestJava() {
    return sourceTestJava;
  }

  public File getSourceTestKotlin() {
    return sourceTestKotlin;
  }

  /**
   * Return the source mode that we think should be used.
   */
  public SourceMode getSourceMode() {
    if (sourceKotlin != null) {
      return SourceMode.KOTLIN;
    }
    return SourceMode.JAVA;
  }

  public boolean createSrcMainResources() {
    mainResource = new File("src/main/resources");
    if (mainResource.mkdir()) {
      return true;
    }

    mainResource = null;
    return false;
  }

  public boolean createSrcTestResources() {
    testResource = new File("src/test/resources");
    if (testResource.mkdirs()) {
      return true;
    }

    testResource = null;
    return false;
  }


  public File getSourceTestKotlinWithCreate() {
    if (sourceTestKotlin == null) {
      sourceTestKotlin = new File("src/test/kotlin");
      sourceTestKotlin.mkdirs();
    }
    return sourceTestKotlin;
  }

  public File getSourceTestJavaWithCreate() {
    if (sourceTestJava == null) {
      sourceTestJava = new File("src/test/java");
      sourceTestJava.mkdirs();
    }
    return sourceTestJava;
  }

  public boolean createSourceTestJava() {
    sourceTestJava = new File("src/test/java");
    if (sourceTestJava.mkdirs()) {
      return true;
    }
    sourceTestJava = null;
    return false;
  }

  public boolean createSourceTestKotlin() {
    sourceTestKotlin = new File("src/test/kotlin");
    if (sourceTestKotlin.mkdirs()) {
      return true;
    }
    sourceTestKotlin = null;
    return false;
  }

  /**
   * Return true if we don't think we are in a project directory.
   */
  public boolean unexpectedLocation() {
    return !maven && !gradle && sourceJava == null && sourceKotlin == null;
  }

  public void setMaven(MavenPom mavenPom) {
    this.maven = true;
    this.mavenPom = mavenPom;
  }

  public void setGradle(GradleBuild gradleBuild) {
    this.gradleBuild = gradleBuild;
    gradle = true;
  }

  public MavenPom getMavenPom() {
    return mavenPom;
  }

  public GradleBuild getGradleBuild() {
    return gradleBuild;
  }

}
