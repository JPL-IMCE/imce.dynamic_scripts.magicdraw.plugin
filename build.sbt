import better.files.{File => BFile, _}
import java.io.File
import java.nio.file.Files

import sbt.Keys._
import sbt._

import com.typesafe.sbt.pgp.PgpKeys
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

import scala.xml.{Node => XNode}
import scala.xml.transform._

import scala.collection.JavaConversions._

import gov.nasa.jpl.imce.sbt._

useGpg := true

developers := List(
  Developer(
    id="rouquett",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/rouquett/default.aspx")))

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

cleanFiles <+=
  baseDirectory { base => base / "imce.md.package" }

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in Global :=
  baseDirectory.value / "imce.md.package" / ("imce.md18_0sp5.dynamic-scripts-" + Versions.version)

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val extractArchives = TaskKey[Seq[Attributed[File]]]("extract-archives", "Extracts ZIP files")

lazy val updateInstall = TaskKey[Unit]("update-install", "Update the MD Installation directory")

lazy val md5Install = TaskKey[Unit]("md5-install", "Produce an MD5 report of the MD Installation directory")

lazy val zipInstall = TaskKey[File]("zip-install", "Zip the MD Installation directory")

lazy val buildUTCDate = SettingKey[String]("build-utc-date", "The UDC Date of the build")

buildUTCDate in Global := {
  import java.util.{ Date, TimeZone }
  val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm")
  formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
  formatter.format(new Date)
}

lazy val jpl_dynamicScripts_magicDraw_plugin = Project("dynamic-scripts-plugin", file("dynamic-scripts"))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(addArtifact(Artifact("imce_md18_0_sp5_dynamic-scripts_resource", "zip", "zip"), artifactZipFile).settings: _*)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    name := "imce_md18_0_sp5_dynamic-scripts",
    organization := "gov.nasa.jpl.imce.magicdraw.plugins",

    projectID := {
      val previous = projectID.value
      previous.extra("build.date.utc" -> buildUTCDate.value)
    },

    artifactZipFile := {
      baseDirectory.value / "target" / "imce_md18_0_sp5_dynamic-scripts_resource.zip"
    },

    addArtifact(Artifact("imce_md18_0_sp5_dynamic-scripts_resource", "zip", "zip"), artifactZipFile),

    resourceDirectory in Compile := baseDirectory.value / "resources",

    scalaSource in Compile := baseDirectory.value / "src",

    classDirectory in Compile := baseDirectory.value / "bin",
    cleanFiles += (classDirectory in Compile).value,

    unmanagedClasspath in Compile <++= unmanagedJars in Compile,
    libraryDependencies ++= Seq(

      "gov.nasa.jpl.cae.magicdraw.packages" %% "cae_md18_0_sp5_aspectj_scala" % Versions.aspectj_scala_package %
        "compile" artifacts Artifact("cae_md18_0_sp5_aspectj_scala", "zip", "zip"),

      "gov.nasa.jpl.imce.secae" %% "jpl-dynamic-scripts-generic-dsl" % Versions.dynamic_scripts_generic_dsl %
      "compile" withSources() withJavadoc()

    ),

    extractArchives <<= (baseDirectory, libraryDependencies, update, streams,
      mdInstallDirectory in Global, scalaBinaryVersion) map {
      (base, libs, up, s, mdInstallDir, sbV) =>

        if (!mdInstallDir.exists) {

          val zfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              val ok1 = a.`type` == "zip" && a.extension == "zip"
              val ok2 = libs.find { dep: ModuleID =>
                ok1 && dep.organization == m.organization && m.name == dep.name + "_" + sbV
              }
              ok2.isDefined
            }
          }
          val zs: Seq[File] = up.matching(zfilter)
          zs.foreach { zip =>
            val files = IO.unzip(zip, mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip: ${zip.getName}")
          }
          val mdRootFolder = mdInstallDir / s"cae.md18_0sp5.aspectj_scala-${Versions.aspectj_scala_package}"
          require(
            mdRootFolder.exists && mdRootFolder.canWrite,
            s"mdRootFolder: $mdRootFolder")
          IO.listFiles(mdRootFolder).foreach { f =>
            val fp = f.toPath
            Files.move(
              fp,
              mdInstallDir.toPath.resolve(fp.getFileName),
              java.nio.file.StandardCopyOption.REPLACE_EXISTING)
          }
          IO.delete(mdRootFolder)

          val mdBinFolder = mdInstallDir / "bin"
          require(mdBinFolder.exists, "md bin: $mdBinFolder")
          val mdPropertiesFiles: Seq[File] = mdBinFolder.listFiles(new java.io.FilenameFilter() {
            override def accept(dir: File, name: String): Boolean =
              name.endsWith(".properties")
          })

          mdPropertiesFiles.foreach { mdPropertyFile: File =>

            val mdPropertyName = mdPropertyFile.name
            val unpatchedContents: String = IO.read(mdPropertyFile)

            // Remove "-Dlocal.config.dir.ext\=<value>" or "-Dlocal.config.dir.ext=<value>" regardless of what <value> is.
            val patchedContents1 = unpatchedContents.replaceAll(
              "-Dlocal.config.dir.ext\\\\?=[a-zA-Z0-9_.\\\\-]*",
              "-Dlocal.config.dir.ext\\\\=-dynamic-scripts-" + Versions.version)

            IO.write(file = mdPropertyFile, content = patchedContents1, append = false)
          }

        } else {
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")
        }

        val libPath = (mdInstallDir / "lib").toPath
        val mdJars = for {
          jar <- Files.walk(libPath).iterator().filter(_.toString.endsWith(".jar")).map(_.toFile)
        } yield Attributed.blank(jar)

        mdJars.toSeq
    },

    unmanagedJars in Compile <++= extractArchives,

    compile <<= (compile in Compile) dependsOn extractArchives,

    publish <<= publish dependsOn zipInstall,
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn zipInstall,

    publishLocal <<= publishLocal dependsOn zipInstall,
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn zipInstall,

    zipInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in Global,
        artifactZipFile,
        packageBin in Compile,
        packageSrc in Compile,
        packageDoc in Compile,
        makePom, buildUTCDate,
        scalaBinaryVersion
        ) map {
        (base, up, s, mdInstallDir, zip, libJar, libSrc, libDoc, pom, d, sbV) =>

          import com.typesafe.sbt.packager.universal._

          val root = base / "target" / "imce_md18_0_sp5_dynamic-scripts"
          s.log.info(s"\n*** top: $root")

          IO.copyDirectory(base / "profiles", root / "profiles/", overwrite=true, preserveLastModified=true)

          val pluginDir = root / "plugins" / "gov.nasa.jpl.magicdraw.dynamicScripts"
          IO.createDirectory(pluginDir)

          IO.copyFile(libJar, pluginDir / "lib" / libJar.getName)
          IO.copyFile(libSrc, pluginDir / "lib" / libSrc.getName)
          IO.copyFile(libDoc, pluginDir / "lib" / libDoc.getName)

          val lfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              val ok1 = "compile" == c
              val ok2 = a.`type` == "jar" && a.extension == "jar"
              val ok3 =
                "gov.nasa.jpl.imce.secae" == m.organization &&
                  "jpl-dynamic-scripts-generic-dsl_" + sbV == m.name
              ok1 && ok2 && ok3
            }
          }
          val ls: Seq[File] = up.matching(lfilter)
          ls.foreach { libJar: File =>
            IO.copyFile(libJar, pluginDir / "lib" / libJar.getName)
          }

          val dfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              val ok1 = "compile" == c
              val ok2 = a.`type` == "doc" && a.extension == "jar"
              val ok3 =
                "gov.nasa.jpl.imce.secae" == m.organization &&
                  "jpl-dynamic-scripts-generic-dsl_" + sbV == m.name
              ok1 && ok2 && ok3
            }
          }
          val ds: Seq[File] = up.matching(dfilter)
          ds.foreach { libDoc: File =>
            IO.copyFile(libDoc, pluginDir / "lib" / libDoc.getName)
          }

          val sfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              val ok1 = "compile" == c
              val ok2 = a.`type` == "src" && a.extension == "jar"
              val ok3 =
                "gov.nasa.jpl.imce.secae" == m.organization &&
                  "jpl-dynamic-scripts-generic-dsl_" + sbV == m.name
              ok1 && ok2 && ok3
            }
          }
          val ss: Seq[File] = up.matching(sfilter)
          ss.foreach { libSrc: File =>
            IO.copyFile(libSrc, pluginDir / "lib" / libSrc.getName)
          }

          val pluginInfo =
            <plugin id="gov.nasa.jpl.magicdraw.dynamicScripts"
                    name="IMCE Dynamic Scripts Plugin"
                    version={Versions.version} internalVersion={Versions.version + "0"}
                    provider-name="JPL"
                    class="gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin">
              <requires>
                <api version="1.0"></api>
              </requires>
              <runtime>
                <library name={"lib/" + libJar.getName}/>
                {ls.map { l => <library name={"lib/"+l.getName}/> }}
              </runtime>
            </plugin>

          xml.XML.save(
            filename=(pluginDir / "plugin.xml").getAbsolutePath,
            node=pluginInfo,
            enc="UTF-8")

          val resourceManager = root / "data" / "resourcemanager"
          IO.createDirectory(resourceManager)
          val resourceDescriptorFile = resourceManager / "MDR_Plugin_govnasajpldynamicScriptsmagicdraw_72516_descriptor.xml"
          val resourceDescriptorInfo =
            <resourceDescriptor critical="false" date={d}
                                description="IMCE Dynamic Scripts Plugin"
                                group="IMCE Resource"
                                homePage="https://github.jpl.nasa.gov/imce/jpl-dynamicscripts-magicdraw-plugin"
                                id="72516"
                                mdVersionMax="higher"
                                mdVersionMin="18.0"
                                name="IMCE Dynamic Scripts Plugin"
                                product="IMCE Dynamic Scripts Plugin"
                                restartMagicdraw="false" type="Plugin">
              <version human={Versions.version} internal={Versions.version} resource={Versions.version + "0"}/>
              <provider email="nicolas.f.rouquette@jpl.nasa.gov"
                        homePage="https://github.jpl.nasa.gov/imce/jpl-dynamicscripts-magicdraw-plugin"
                        name="IMCE"/>
              <edition>Reader</edition>
              <edition>Community</edition>
              <edition>Standard</edition>
              <edition>Professional Java</edition>
              <edition>Professional C++</edition>
              <edition>Professional C#</edition>
              <edition>Professional ArcStyler</edition>
              <edition>Professional EFFS ArcStyler</edition>
              <edition>OptimalJ</edition>
              <edition>Professional</edition>
              <edition>Architect</edition>
              <edition>Enterprise</edition>
              <requiredResource id="1440">
                <minVersion human="17.0" internal="169010"/>
              </requiredResource>
              <installation>
                <file from="profiles/SSCAEProjectUsageIntegrityProfile.mdzip"
                      to="profiles/SSCAEProjectUsageIntegrityProfile.mdzip"/>

                <file from={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libJar.getName}
                      to={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+libJar.getName}/>
                {ls.map { l =>
                  <file from={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+l.getName}
                        to={"plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/"+l.getName}/> }
                }

                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/MDTeamworkProjectIDSuffixes.txt"
                        to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/MDTeamworkProjectIDSuffixes.txt"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.ant" 
                        to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.ant"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.README.txt" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/runOpenAuditTests.README.txt"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1.jar" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/commons-exec-1.1.jar"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/jgrapht-0.8.3-jdk1.6.jar" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/lib/jgrapht-0.8.3-jdk1.6.jar"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCONSISTENT.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCONSISTENT.png"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-WARNING.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-WARNING.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-ERROR.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/DEPRECATED-ERROR.png"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-WARNING.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-WARNING.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-ERROR.png"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/INCUBATOR-ERROR.png"/>
                
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-WARNING.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-WARNING.png"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-ERROR.png" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/icons/RECOMMENDED-ERROR.png"/>

                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/INCONSISTENT.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/INCONSISTENT.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P1_DEPRECATED.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P1_DEPRECATED.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P2_DEPRECATED_constrains_INCUBATOR_as_ERROR.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P2_DEPRECATED_constrains_INCUBATOR_as_ERROR.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P3_INCUBATOR.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P3_INCUBATOR.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P4_excludes_P1.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P4_excludes_P1.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P5_uses_P1,P4.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/P5_uses_P1,P4.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Supplier-Client-Example.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Supplier-Client-Example.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3_with_P2.mdzip" 
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3_with_P2.mdzip"/>
                <file from="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3.mdzip"
                      to="plugins/gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker/samples/ProjectUsageIntegrityChecker/Use_P1_P3.mdzip"/>
              </installation>
            </resourceDescriptor>

          xml.XML.save(
            filename=resourceDescriptorFile.getAbsolutePath,
            node=resourceDescriptorInfo,
            enc="UTF-8")

          val fileMappings = (root.*** --- root) pair relativeTo(root)
          ZipHelper.zipNIO(fileMappings, zip)

          s.log.info(s"\n*** Created the zip: $zip")
          zip
      }
  )
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)

lazy val root = Project("dynamic-scripts-package", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .aggregate(jpl_dynamicScripts_magicDraw_plugin)
  .dependsOn(jpl_dynamicScripts_magicDraw_plugin)
  .settings(addArtifact(Artifact("imce_md18_0_sp5_dynamic-scripts", "zip", "zip"), artifactZipFile).settings: _*)
  .settings(
    IMCEKeys.licenseYearOrRange := "2013-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,
    
    organization := "gov.nasa.jpl.cae.magicdraw.packages",
    name := "imce_md18_0_sp5_dynamic-scripts",
    homepage := Some(url("https://github.jpl.nasa.gov/imce/jpl-dynamicscripts-magicdraw-plugin")),

    git.baseVersion := Versions.version,

    projectID := {
      val previous = projectID.value
      previous.extra("build.date.utc" -> buildUTCDate.value)
    },

    pomPostProcess <<= (pomPostProcess, mdInstallDirectory in Global) {
      (previousPostProcess, mdInstallDir) => { (node: XNode) =>
        val processedNode: XNode = previousPostProcess(node)
        val mdUpdateDir = UpdateProperties(mdInstallDir)
        val resultNode: XNode = new RuleTransformer(mdUpdateDir)(processedNode)
        resultNode
      }
    },

    artifactZipFile := {
      baseDirectory.value / "target" / "imce_md18_0_sp5_dynamic-scripts.zip"
    },

    addArtifact(Artifact("imce_md18_0_sp5_dynamic-scripts", "zip", "zip"), artifactZipFile),

    // disable publishing the main jar produced by `package`
    publishArtifact in(Compile, packageBin) := false,

    // disable publishing the main API jar
    publishArtifact in(Compile, packageDoc) := false,

    // disable publishing the main sources jar
    publishArtifact in(Compile, packageSrc) := false,

    // disable publishing the jar produced by `test:package`
    publishArtifact in(Test, packageBin) := false,

    // disable publishing the test API jar
    publishArtifact in(Test, packageDoc) := false,

    // disable publishing the test sources jar
    publishArtifact in(Test, packageSrc) := false,

    publish <<= publish dependsOn zipInstall,
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn zipInstall,

    publish <<= publish dependsOn (publish in jpl_dynamicScripts_magicDraw_plugin),
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn (PgpKeys.publishSigned in jpl_dynamicScripts_magicDraw_plugin),

    publishLocal <<= publishLocal dependsOn zipInstall,
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn zipInstall,

    publishLocal <<= publishLocal dependsOn (publishLocal in jpl_dynamicScripts_magicDraw_plugin),
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn (PgpKeys.publishLocalSigned in jpl_dynamicScripts_magicDraw_plugin),

    makePom <<= makePom dependsOn md5Install,

    md5Install <<=
      ((baseDirectory, update, streams,
        mdInstallDirectory in Global,
        version
        ) map {
        (base, up, s, mdInstallDir, buildVersion) =>

          s.log.info(s"***(2) MD5 of md.install.dir=$mdInstallDir")

      }) dependsOn updateInstall,

    updateInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in Global,
        artifactZipFile in jpl_dynamicScripts_magicDraw_plugin) map {
        (base, up, s, mdInstallDir, dynamicScriptsResource) =>

          s.log.info(s"***(1) Updating md.install.dir=$mdInstallDir")
          s.log.info(s"***    Installling resource=$dynamicScriptsResource")

          val files = IO.unzip(dynamicScriptsResource, mdInstallDir)
          s.log.info(
            s"=> installed resource in md.install.dir=$mdInstallDir with ${files.size} " +
              s"files extracted from zip: ${dynamicScriptsResource.getName}")
      },

    zipInstall <<=
      (baseDirectory, update, streams,
        mdInstallDirectory in Global,
        artifactZipFile,
        makePom, scalaBinaryVersion
        ) map {
        (base, up, s, mdInstallDir, zip, pom, sbV) =>

          import java.nio.file.attribute.PosixFilePermission
          import com.typesafe.sbt.packager.universal._

          s.log.info(s"\n*** Creating the zip: $zip")

          val parentDir = mdInstallDir.getParentFile
          val top: BFile = mdInstallDir.toScala

          val macosExecutables: Iterator[BFile] = top.glob("**/*.app/Content/MacOS/*")
          macosExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val windowsExecutables: Iterator[BFile] = top.glob("**/*.exe")
          windowsExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val javaExecutables: Iterator[BFile] = top.glob("jre*/**/bin/*")
          javaExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val unixExecutables: Iterator[BFile] = top.glob("bin/{magicdraw,submit_issue}")
          unixExecutables.foreach { f: BFile =>
            s.log.info(s"* +X $f")
            f.addPermission(PosixFilePermission.OWNER_EXECUTE)
          }

          val zipDir = zip.getParentFile.toScala
          Cmds.mkdirs(zipDir)

          val fileMappings = mdInstallDir.*** pair relativeTo(parentDir)
          ZipHelper.zipNative(fileMappings, zip)

          s.log.info(s"\n*** Created the zip: $zip")

          zip
      }
  )
  .settings(IMCEReleasePlugin.packageReleaseProcessSettings)

def UpdateProperties(mdInstall: File): RewriteRule = {

  println(s"update properties for md.install=$mdInstall")
  val binDir = mdInstall / "bin"
  require(binDir.exists, binDir)
  val binSub = MD5SubDirectory(
    name = "bin",
    files = IO
      .listFiles(binDir, GlobFilter("*.properties"))
      .sorted.map(MD5.md5File(binDir)))

  val docGenScriptsDir = mdInstall / "DocGenUserScripts"
  require(docGenScriptsDir.exists, docGenScriptsDir)
  val scriptsSub = MD5SubDirectory(
    name = "DocGenUserScripts",
    dirs = IO
      .listFiles(docGenScriptsDir, DirectoryFilter)
      .sorted.map(MD5.md5Directory(docGenScriptsDir)))

  val libDir = mdInstall / "lib"
  require(libDir.exists, libDir)
  val libSub = MD5SubDirectory(
    name = "lib",
    files = IO
      .listFiles(libDir, GlobFilter("*.jar"))
      .sorted.map(MD5.md5File(libDir)))

  val pluginsDir = mdInstall / "plugins"
  require(pluginsDir.exists)
  val pluginsSub = MD5SubDirectory(
    name = "plugins",
    dirs = IO
      .listFiles(pluginsDir, DirectoryFilter)
      .sorted.map(MD5.md5Directory(pluginsDir)))

  val modelsDir = mdInstall / "modelLibraries"
  require(modelsDir.exists, libDir)
  val modelsSub = MD5SubDirectory(
    name = "modelLibraries",
    files = IO
      .listFiles(modelsDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(modelsDir)))

  val profilesDir = mdInstall / "profiles"
  require(profilesDir.exists, libDir)
  val profilesSub = MD5SubDirectory(
    name = "profiles",
    files = IO
      .listFiles(profilesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(profilesDir)))

  val samplesDir = mdInstall / "samples"
  require(samplesDir.exists, libDir)
  val samplesSub = MD5SubDirectory(
    name = "samples",
    files = IO
      .listFiles(samplesDir, GlobFilter("*.mdzip") || GlobFilter("*.mdxml"))
      .sorted.map(MD5.md5File(samplesDir)))

  val all = MD5SubDirectory(
    name = ".",
    sub = Seq(binSub, libSub, pluginsSub, modelsSub, profilesSub, scriptsSub, samplesSub))

  new RewriteRule {

    import spray.json._
    import MD5JsonProtocol._

    override def transform(n: XNode): Seq[XNode] = n match {
      case <md5></md5> =>
        <md5>
          {all.toJson}
        </md5>
      case _ =>
        n
    }
  }
}

