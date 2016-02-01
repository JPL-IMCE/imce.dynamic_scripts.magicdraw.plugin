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

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in Global := baseDirectory.value / "imce.md.package"

cleanFiles <+= baseDirectory { base => base / "imce.md.package" }

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

lazy val jpl_dynamicScripts_magicDraw_plugin = Project("dynamic-scripts-plugin", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    buildInfoPackage := "gov.nasa.jpl.imce.dynamic_scripts.plugin",
    buildInfoKeys ++= Seq[BuildInfoKey](BuildInfoKey.action("buildDateUTC") { buildUTCDate.value }),

    name := "imce_md18_0_sp5_dynamic-scripts",
    organization := "gov.nasa.jpl.imce.magicdraw.plugins",

    projectID := {
      val previous = projectID.value
      previous.extra("build.date.utc" -> buildUTCDate.value)
    },

    artifactZipFile := {
      baseDirectory.value / "target" / "imce_md18_0_sp5_dynamic-scripts_resource.zip"
    },

    addArtifact(
      Artifact("imce_md18_0_sp5_dynamic-scripts_resource", "zip", "zip"),
      artifactZipFile),

    resourceDirectory in Compile := baseDirectory.value / "resources",

    scalaSource in Compile := baseDirectory.value / "src",

    unmanagedClasspath in Compile <++= unmanagedJars in Compile,
    libraryDependencies ++= Seq(

      "gov.nasa.jpl.cae.magicdraw.packages" % "cae_md18_0_sp5_mdk" % Versions.mdk_package %
        "compile" artifacts Artifact("cae_md18_0_sp5_mdk", "zip", "zip"),

      "gov.nasa.jpl.imce.magicdraw.libraries" %% "imce-magicdraw-library-enhanced_api" % Versions.enhanced_api %
        "compile" withSources(),

      "gov.nasa.jpl.imce.secae" %% "jpl-dynamic-scripts-generic-dsl" % Versions.dynamic_scripts_generic_dsl %
        "compile" withSources() withJavadoc(),

      "gov.nasa.jpl.imce.thirdParty" %% "all-graph-libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-graph-libraries", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "all-jena-libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-jena-libraries", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "all-owlapi-libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("all-owlapi-libraries", "zip", "zip"),

      "gov.nasa.jpl.imce.thirdParty" %% "other-scala-libraries" % Versions.jpl_mbee_common_scala_libraries artifacts
        Artifact("other-scala-libraries", "zip", "zip")

    ),

    extractArchives <<= (baseDirectory, libraryDependencies, update, streams,
      mdInstallDirectory in Global, scalaBinaryVersion) map {
      (base, libs, up, s, mdInstallDir, sbV) =>

        if (!mdInstallDir.exists) {

          val zfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              a.`type` == "zip" && a.extension == "zip" && a.name == "cae_md18_0_sp5_mdk"
            }
          }
          val zs: Seq[File] = up.matching(zfilter)
          zs.foreach { zip =>
            val files = IO.unzip(zip, mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip: ${zip.getName}")
          }

          val mdBinFolder = mdInstallDir / "bin"
          require(mdBinFolder.exists, "md bin: $mdBinFolder")
          val mdPropertiesFiles: Seq[File] = mdBinFolder.listFiles(new java.io.FilenameFilter() {
            override def accept(dir: File, name: String): Boolean =
              name.endsWith(".properties")
          })

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
          import java.nio.file.attribute.PosixFilePermission

          val root = base / "target" / "imce_md18_0_sp5_dynamic-scripts"
          s.log.info(s"\n*** top: $root")

          val fileArtifacts = for {
            cReport <- up.configurations
            if Configurations.Compile.name == cReport.configuration
            oReport <- cReport.details
            mReport <- oReport.modules
            (artifact, file) <- mReport.artifacts
            if "jar" == artifact.extension
          } yield (oReport.organization, oReport.name, file, artifact)

          val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, a) =>
            a.`classifier`.getOrElse(a.`type`)
          }
          val jarArtifacts = fileArtifactsByType("jar")
          val srcArtifacts = fileArtifactsByType("sources")
          val docArtifacts = fileArtifactsByType("javadoc")

          val jars = {
            val libs = jarArtifacts.map { case (o, _, jar, _) =>
              s.log.info(s"* copying jar: $o/${jar.name}")
              IO.copyFile(jar, root / "lib" / o / jar.name)
              "lib/" + o + "/" + jar.name
            }
            libs
          }

          val weaverJar: String = {
            val weaverJars = jarArtifacts.flatMap {
              case ("org.aspectj", "aspectjweaver", jar, _) =>
                Some("lib/org.aspectj/" + jar.name)
              case _ =>
                None
            }
            require(1 == weaverJars.size)
            weaverJars.head
          }

          val bootJars = jarArtifacts.flatMap {
            case ("org.scala-lang", "scala-library", jar, _) =>
              Some("lib/org.scala-lang/" + jar.name)
            case ("org.aspectj", "aspectjrt", jar, _) =>
              Some("lib/org.aspectj/" + jar.name)
            case ("org.aspectj", "aspectjweaver", jar, _) =>
              Some("lib/org.aspectj/" + jar.name)
            case _ =>
              None
          }

          val bootClasspathPrefix = bootJars.mkString("", "\\\\:", "\\\\:")

          srcArtifacts.foreach { case (o, _, jar, _) =>
            s.log.info(s"* copying source: $o/${jar.name}")
            IO.copyFile(jar, root / "lib.sources" / o / jar.name)
            "lib.sources/" + o + "/" + jar.name
          }

          docArtifacts.foreach { case (o, _, jar, _) =>
            s.log.info(s"* copying javadoc: $o/${jar.name}")
            IO.copyFile(jar, root / "lib.javadoc" / o / jar.name)
            "lib.javadoc/" + o + "/" + jar.name
          }

          val md_imce_script = root / "bin" / "magicdraw.imce"
          IO.copyFile(
            mdInstallDir / "bin" / "magicdraw",
            md_imce_script)
          md_imce_script.toScala.addPermission(PosixFilePermission.OWNER_EXECUTE)
          md_imce_script.toScala.addPermission(PosixFilePermission.GROUP_EXECUTE)
          md_imce_script.toScala.addPermission(PosixFilePermission.OTHERS_EXECUTE)

          val md_imce_exe = root / "bin" / "magicdraw.imce.exe"
          IO.copyFile(
            mdInstallDir / "bin" / "magicdraw.exe",
            md_imce_exe)
          md_imce_exe.toScala.addPermission(PosixFilePermission.OWNER_EXECUTE)
          md_imce_exe.toScala.addPermission(PosixFilePermission.GROUP_EXECUTE)
          md_imce_exe.toScala.addPermission(PosixFilePermission.OTHERS_EXECUTE)

          IO.copyFile(
            mdInstallDir / "bin" / "magicdraw.properties",
            root / "bin" / "magicdraw.imce.properties")

          val mdBinFolder = root / "bin"
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
              "-Dlocal.config.dir.ext\\\\=-imce-" + Versions.version)

            // Add AspectJ weaver agent & settings
            val patchedContents2 = patchedContents1.replaceFirst(
              "JAVA_ARGS=",
              s"JAVA_ARGS=-javaagent:$weaverJar " +
                "-Daj.weaving.verbose\\\\=true " +
                "-Dorg.aspectj.weaver.showWeaveInfo\\\\=true ")

            val patchedContents3 = patchedContents2.replaceFirst(
              "BOOT_CLASSPATH=",
              "BOOT_CLASSPATH=" + bootClasspathPrefix)

            val patchedContents4 = patchedContents3.replaceFirst(
              "([^_])CLASSPATH=(.*)",
              jars.mkString("$1CLASSPATH=", "\\\\:", "\\\\:$2"))

            val patchedContents5 = patchedContents4.replaceFirst(
              "JAVA_HOME=\\S*",
              "JAVA_HOME=")

            IO.write(file = mdPropertyFile, content = patchedContents5, append = false)
          }

          IO.copyDirectory(base / "profiles", root / "profiles", overwrite=true, preserveLastModified=true)

          val pluginDir = root / "plugins" / "gov.nasa.jpl.magicdraw.dynamicScripts"
          IO.createDirectory(pluginDir)

          IO.copyDirectory(base / "icons", pluginDir / "icons", overwrite=true, preserveLastModified=true)

          IO.copyFile(libJar, pluginDir / "lib" / libJar.getName)
          IO.copyFile(libSrc, pluginDir / "lib" / libSrc.getName)
          IO.copyFile(libDoc, pluginDir / "lib" / libDoc.getName)

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
              </runtime>
            </plugin>

          xml.XML.save(
            filename=(pluginDir / "plugin.xml").getAbsolutePath,
            node=pluginInfo,
            enc="UTF-8")

          val resourceFiles =
            ((root.*** --- root) pair relativeTo(root))
              .filter(! _._1.isDirectory)
              .sortBy(_._2)

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
                {
                  resourceFiles.map { case (_, path) =>
                    <file
                       from={path}
                       to={path}>
                    </file>
                }}
              </installation>
            </resourceDescriptor>

          xml.XML.save(
            filename=resourceDescriptorFile.getAbsolutePath,
            node=resourceDescriptorInfo,
            enc="UTF-8")

          val fileMappings = (root.*** --- root) pair relativeTo(root)
          ZipHelper.zip(fileMappings, zip)

          s.log.info(s"\n*** Created the zip: $zip")
          zip
      }
  )
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
