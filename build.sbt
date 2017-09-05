import better.files.{File => BFile, _}
import java.io.File
import java.nio.file.Files

import aether.AetherKeys
import sbt.Keys._
import sbt._
import net.virtualvoid.sbt.graph._
import com.typesafe.sbt.pgp.PgpKeys
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._
import com.typesafe.sbt.packager.SettingsHelper

import scala.xml.{Node => XNode}
import scala.xml.transform._
import scala.collection.JavaConversions._
import scala.collection.immutable._
import gov.nasa.jpl.imce.sbt._

updateOptions := updateOptions.value.withCachedResolution(true)

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

lazy val thisVersion = SettingKey[String]("this-version", "This Module's version")

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in ThisBuild :=
  (baseDirectory in ThisBuild).value / "target" / "md.package"

cleanFiles += (mdInstallDirectory in ThisBuild).value

lazy val artifactZipFile = taskKey[File]("Location of the aggregate zip artifact file")

lazy val artifactZip1File = taskKey[File]("Location of the part1 zip artifact file")

lazy val artifactZip2File = taskKey[File]("Location of the part2 zip artifact file")

lazy val zipInstall = TaskKey[Unit]("zip-install", "Zip the MD Installation directory")

val extractArchives: TaskKey[Unit] = TaskKey[Unit]("extract-archives", "Extracts ZIP files")

lazy val mdLibs = taskKey[Seq[Attributed[File]]]("magidraw libraries")

/**
  * Constructs a jar name from components...(ModuleID/Artifact)
  */
def makeJarName(org: String,
                name: String,
                revision: String,
                artifactName: String,
                artifactClassifier: Option[String]): String =
  org + "." +
    name + "-" +
    Option(artifactName.replace(name, "")).filterNot(_.isEmpty).map(_ + "-").getOrElse("") +
    revision +
    artifactClassifier.filterNot(_.isEmpty).map("-" + _).getOrElse("") +
    ".jar"

def getJarFullFilename(dep: Attributed[File]): String = {
  val filename: Option[String] = for {
    module <- dep.metadata
      // sbt 0.13.x key
      .get(AttributeKey[ModuleID]("module-id"))
      // sbt 1.x key
      .orElse(dep.metadata.get(AttributeKey[ModuleID]("moduleID")))
    artifact <- dep.metadata.get(AttributeKey[Artifact]("artifact"))
  } yield makeJarName(module.organization, module.name, module.revision, artifact.name, artifact.classifier)
  filename.getOrElse(dep.data.getName)
}

def versionComparator
(t1: (String, Module, String, ModuleGraph),
 t2: (String, Module, String, ModuleGraph))
: Boolean
= t1._1.compare(t2._1) >= 0

def GroupFile2Folder
(group: String,
 fileName: String)
: String
= group match {
  case "imce.third_party.aspectj" =>
    if (fileName.startsWith("aspectjrt-") || fileName.startsWith("aspectjweaver-"))
      "bootstrap/" + group
    else
      group
  case "imce.third_party.scala" =>
    if (fileName.startsWith("scala-library-"))
      "bootstrap/" + group
    else
      group
  case "gov.nasa.jpl.imce" =>
    if (fileName.startsWith("imce.magicdraw.library.enhanced_api_"))
      "bootstrap/" + group
    else
      group
  case _ =>
    group
}

lazy val imce_dynamic_scripts_magicdraw_plugin = Project("imce-dynamic_scripts-magicdraw-plugin", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(UniversalPlugin)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2017",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    thisVersion := version.value,

    buildInfoPackage := "gov.nasa.jpl.imce.dynamic_scripts.plugin",
    buildInfoKeys ++= Seq[BuildInfoKey](BuildInfoKey.action("buildDateUTC") { buildUTCDate.value }),

    projectID := {
      val previous = projectID.value
      previous.extra(
        "build.date.utc" -> buildUTCDate.value,
        "artifact.kind" -> "magicdraw.resource.plugin")
    },

    artifactZipFile := baseDirectory.value / "target" / "bundle.zip",

    artifactZip1File := baseDirectory.value / "target" / "bundle1.zip",

    artifactZip2File := baseDirectory.value / "target" / "bundle2.zip",

    addArtifact(
      Artifact(s"imce_${Versions.md_version}_dynamic-scripts", "zip", "zip", Some("part1"), Seq(), None, Map()),
      artifactZip1File),

    addArtifact(
      Artifact(s"imce_${Versions.md_version}_dynamic-scripts", "zip", "zip", Some("part2"), Seq(), None, Map()),
      artifactZip2File),

    resourceDirectory in Compile := baseDirectory.value / "resources",

    unmanagedClasspath in Compile ++= (unmanagedJars in Compile).value,

    resolvers += Resolver.bintrayRepo("jpl-imce", "gov.nasa.jpl.imce"),
    resolvers += Resolver.bintrayRepo("tiwg", "org.omg.tiwg"),

    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",
    scalacOptions in (Compile, compile) += s"-P:artima-supersafe:config-file:${baseDirectory.value}/project/supersafe.cfg",
    scalacOptions in (Test, compile) += s"-P:artima-supersafe:config-file:${baseDirectory.value}/project/supersafe.cfg",
    scalacOptions in (Compile, doc) += "-Xplugin-disable:artima-supersafe",
    scalacOptions in (Test, doc) += "-Xplugin-disable:artima-supersafe",

    libraryDependencies ++= Seq(

      "gov.nasa.jpl.imce" %% "imce.magicdraw.library.enhanced_api"
        % Versions_enhanced_api.version
        % "compile" withSources(),

      "gov.nasa.jpl.imce" %% "imce.dynamic_scripts.generic_dsl"
        % Versions_dynamic_scripts_generic_dsl.version
        % "compile" withSources() withJavadoc(),

      "gov.nasa.jpl.imce" %% "imce.dynamic_scripts.magicdraw.launcher"
        % Versions_launcher.version artifacts
        Artifact("imce.dynamic_scripts.magicdraw.launcher", "tgz", "tgz", "resource"),

      "gov.nasa.jpl.imce" %% "imce.third_party.scala_graph_libraries"
        % Versions_scala_graph_libraries.version artifacts
        Artifact("imce.third_party.scala_graph_libraries", "zip", "zip", "resource"),

      "gov.nasa.jpl.imce" %% "imce.third_party.jena_libraries"
        % Versions_jena_libraries.version artifacts
        Artifact("imce.third_party.jena_libraries", "zip", "zip", "resource"),

      "gov.nasa.jpl.imce" %% "imce.third_party.owlapi_libraries"
        % Versions_owlapi_libraries.version artifacts
        Artifact("imce.third_party.owlapi_libraries", "zip", "zip", "resource")

    ),

    extractArchives := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value
      val showDownloadProgress = "true" == System.getProperty("MagicDrawDownloader.progress", "true")

      if (!mdInstallDir.exists) {

        MagicDrawDownloader.fetchMagicDraw(
          s.log, showDownloadProgress,
          up,
          credentials.value,
          mdInstallDir, base / "target" / "no_install.zip")

      } else
        s.log.info(
          s"=> use existing md.install.dir=$mdInstallDir")
    },

    unmanagedJars in Compile := (unmanagedJars in Compile).dependsOn(extractArchives).value,

    mdLibs := {
      val _ = extractArchives.value
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value
      val libJars = ((mdInstallDir / "lib") ** "*.jar").get
      val mdJars = libJars.map { jar => Attributed.blank(jar) }.to[Seq]

      s.log.info(s"=> Adding ${mdJars.size} unmanaged jars")

      mdJars
    },

    unmanagedJars in Compile ++= mdLibs.value,

    // Needed to transitively get dependencies from the gov.nasa.jpl.imce:imce.third_party.* zip aggregates
    classpathTypes += "zip",
    classpathTypes += "tgz",

    // Skip doc when building 'stage'
    // @see http://www.scala-sbt.org/sbt-native-packager/formats/universal.html#skip-packagedoc-task-on-stage
    mappings in (Compile, packageDoc) := Seq(),

    mainClass in Compile := Some("gov.nasa.jpl.dynamicScripts.magicdraw.launcher.MagicDrawDynamicScriptsLauncher"),

    executableScriptName := "magicdraw.imce",

    SettingsHelper.makeDeploymentSettings(Universal, packageZipTarball in Universal, "tgz"),

    SettingsHelper.makeDeploymentSettings(UniversalDocs, packageXzTarball in UniversalDocs, "tgz"),

    AetherKeys.aetherArtifact := AetherKeys.aetherArtifact.dependsOn(zipInstall).value,

    AetherKeys.aetherDeploy := AetherKeys.aetherDeploy.dependsOn(zipInstall).value,

//    // See the imce.sbt.plugin dependency:
//    // https://github.com/JPL-IMCE/imce.sbt.plugin/blob/4.12.0/src/main/scala/gov/nasa/jpl/imce/sbt/IMCEPlugin.scala#L155
    packagedArtifacts in Compile := (packagedArtifacts in Compile).dependsOn(zipInstall).value,

    zipInstall := {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val ver = version.value
      val zip = artifactZipFile.value
      val zip1 = artifactZip1File.value
      val zip2 = artifactZip2File.value
      val libJar = (packageBin in Compile).value
      val libSrc = (packageSrc in Compile).value
      val libDoc = (packageDoc in Compile).value
      val pom = makePom.value
      val d = buildUTCDate.value
      val sbV = scalaBinaryVersion.value

      import com.typesafe.sbt.packager.universal._
      import java.nio.file.attribute.PosixFilePermission

      if (zip.exists() && zip1.exists() && zip2.exists()) {
        s.log.warn(s"Zip file already created (${zip.length()} bytes)\n$zip")
        s.log.warn(s"Part1 file already created (${zip1.length()} bytes)\n$zip1")
        s.log.warn(s"Part2 file already created (${zip2.length()} bytes)\n$zip2")

      } else {
        val mdInstallDir = base / "target" / "md.package"
        val root = base / "target" / s"imce_${Versions.md_version}_dynamic-scripts"
        IO.createDirectory(root)

        s.log.info(s"\n*** top: $root")

        val compileConfig: ConfigurationReport = {
          up.configurations.find((c: ConfigurationReport) => Configurations.Compile.name == c.configuration).get
        }

        def transitiveScope
        (modules: Set[Module],
         g: ModuleGraph,
         includeZips: Boolean): Seq[Module] = {

          @annotation.tailrec
          def acc(focus: Set[Module], result: Set[Module]): Set[Module] = {
            val next = g.edges.flatMap { case (fID, tID) =>
              focus.find(m => m.id == fID && (m.jarFile.isDefined || includeZips)).flatMap { _ =>
                g.nodes.find(m => !m.isEvicted && m.id == tID && (m.jarFile.isDefined || includeZips))
              }
            }.to[Set]
            if (next.isEmpty)
              result
            else
              acc(next, result ++ next)
          }

          acc(modules, Set()).to[Seq].sortBy(m => m.id.organisation + m.id.name)
        }

        for {
          oReport <- compileConfig.details
          groupName = oReport.name.stripSuffix("_" + sbV)
          mReport <- oReport.modules
          (artifact, file) <- mReport.artifacts
          if "imce.dynamic_scripts.magicdraw.launcher" == groupName && "tgz" == artifact.extension
        } {
          s.log.info(s"=> Extracting $groupName / ${artifact.name} from ${file.name}")
          Process(Seq("tar", "--strip-components", "1", "-zxf", file.getAbsolutePath), Some(root)).! match {
            case 0 =>
              s.log.info(s"=> Extracted $groupName / ${artifact.name} from ${file.name}")
            case n =>
              sys.error(s"Error extracting $file; exit code: $n")
          }
        }

        val zipGroups: Map[String, scala.collection.Seq[(String, (String, Module, String, ModuleGraph))]] = (for {
          oReport <- compileConfig.details
          groupName = oReport.name.stripSuffix("_" + sbV)
            .stripSuffix("_libraries")
            .stripSuffix("-libraries")
            .stripSuffix(".libraries")
          mReport <- oReport.modules
          (artifact, file) <- mReport.artifacts
          if "zip" == artifact.extension
          graph = backend.SbtUpdateReport.fromConfigurationReport(compileConfig, mReport.module)
          root <- graph.nodes.filter { m =>
            !m.isEvicted &&
              m.id.organisation == mReport.module.organization &&
              m.id.name == mReport.module.name &&
              m.id.version == mReport.module.revision
          }
        } yield root.id.name -> (root.id.version, root, groupName, graph)).groupBy(_._1)

        val zipRoots: Map[Module, (String, ModuleGraph)]
        = zipGroups
          .map { case (_, seq) =>
            val rG = seq.map(_._2).sortWith(versionComparator).head
            rG._2 -> (rG._3, rG._4)
          }

        val allModules: Set[Module] = zipRoots.keySet

        s.log.info(s"zipRoots: ${zipRoots.size}")
        val zipAllFiles: Map[Module, (String, Set[File], Set[Module])] = zipRoots.map { case (r, (group, g)) =>
          val rootScope: Seq[Module] = transitiveScope(Set(r), g, includeZips = true)
          s.log.info(s"group: $group root: ${r.id.organisation} ${r.id.name} ${r.id.version} => scope: ${rootScope.size}")
          val includes: Set[Module] = allModules & rootScope.to[Set]
          val files: Set[File] = rootScope.flatMap(_.jarFile).to[Set]
          s.log.info(s"group: $group root: ${r.id.organisation} ${r.id.name} ${r.id.version} => all files: ${files.size}")
          r -> (group, files, includes)
        }

        val zipFiles: Map[Module, Seq[File]] = zipAllFiles.map { case (r, (group, allFiles, includes)) =>
          val others: Set[File] = for {
            i <- includes
            (_, files, _) = zipAllFiles(i)
            f <- files
          } yield f


          val files: Seq[File] = (allFiles -- others).to[Seq].sortBy(_.name)
          s.log.info(s"group: $group root: ${r.id.organisation} ${r.id.name} ${r.id.version} => group files: ${files.size}")
          files.foreach { f =>
            val folder = GroupFile2Folder(group, f.name)
            IO.copyFile(f, root / "lib" / folder / f.name)
            s.log.info(s" jar: ${f.name}")
            val fs = f.getParentFile.getParentFile / "srcs" / (f.name.stripSuffix(".jar") + "-sources.jar")
            if (fs.exists && fs.canRead) {
              IO.copyFile(fs, root / "lib.sources" / folder / fs.name)

            }
            //              val fd = f.getParentFile.getParentFile / "docs" / (f.name.stripSuffix(".jar") + "-javadoc.jar")
            //              if (fd.exists && fd.canRead) {
            //                IO.copyFile(fd, root / "lib.javadoc" / folder / fd.name)
            //              }
          }
          r -> files
        }

        val groupedFiles: Set[File] = zipFiles.values.flatten.to[Set]

        val fileArtifacts = for {
          oReport <- compileConfig.details
          mReport <- oReport.modules
          (artifact, file) <- mReport.artifacts
          if !mReport.evicted && "jar" == artifact.extension && !groupedFiles.contains(file)
        } yield {
          s.log.info(s"additional: ${file.getParentFile.getParentFile.name}/${file.getParentFile.name}/${file.name}")
          (oReport.organization, oReport.name, file, artifact)
        }

        val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, a) =>
          a.`classifier`.getOrElse(a.`type`)
        }

        // -----

        val jarArtifacts = fileArtifactsByType("jar").map { case (o, _, jar, _) => o -> jar }.to[Set].to[Seq].sortBy { case (o, jar) => s"$o/${jar.name}" }
        val srcArtifacts = fileArtifactsByType("sources").map { case (o, _, jar, _) => o -> jar }.to[Set].to[Seq].sortBy { case (o, jar) => s"$o/${jar.name}" }
        val docArtifacts = fileArtifactsByType("javadoc").map { case (o, _, jar, _) => o -> jar }.to[Set].to[Seq].sortBy { case (o, jar) => s"$o/${jar.name}" }

        jarArtifacts.foreach { case (o, jar) =>
          s.log.info(s"* copying jar: $o/${jar.name}")
          val folder = GroupFile2Folder(o, jar.name)
          IO.copyFile(jar, root / "lib" / folder / jar.name)
        }

        srcArtifacts.foreach { case (o, jar) =>
          s.log.info(s"* copying source: $o/${jar.name}")
          val folder = GroupFile2Folder(o, jar.name)
          IO.copyFile(jar, root / "lib.sources" / folder / jar.name)
        }

        docArtifacts.foreach { case (o, jar) =>
          s.log.info(s"* copying javadoc: $o/${jar.name}")
          val folder = GroupFile2Folder(o, jar.name)
          IO.copyFile(jar, root / "lib.javadoc" / folder / jar.name)
        }

        IO.copyDirectory(
          base / "profiles", root / "profiles",
          overwrite=true,
          preserveLastModified=true)

        val pluginDir = root / "plugins" / "gov.nasa.jpl.magicdraw.dynamicScripts"
        IO.createDirectory(pluginDir)

        IO.copyDirectory(base / "icons", pluginDir / "icons", overwrite=true, preserveLastModified=true)

        IO.copyFile(libJar, pluginDir / "lib" / libJar.getName)
        IO.copyFile(libSrc, pluginDir / "lib" / libSrc.getName)
        IO.copyFile(libDoc, pluginDir / "lib" / libDoc.getName)

        val blibs = ((root / "lib" / "bootstrap") ** "*.jar").get.to[Set]
        val libs = (((root / "lib") ** "*.jar").get.to[Set] -- blibs).to[Seq].sortBy(_.name)
        val libResources = (libs pair relativeTo(root)).map(_._2).sorted

        val pluginInfo =
          <plugin id="gov.nasa.jpl.magicdraw.dynamicScripts"
                  name="IMCE Dynamic Scripts Plugin"
                  version={ver}
                  internalVersion={ver + "0"}
                  provider-name="JPL"
                  class="gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin">
            <requires>
              <api version="1.0"></api>
            </requires>
            <runtime>
              <library name={"lib/" + libJar.getName}/>
              { libResources.map { r =>
                <library name={r}>
                </library>
              }}
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
                              mdVersionMin="18.5"
                              name="IMCE Dynamic Scripts Plugin"
                              product="IMCE Dynamic Scripts Plugin"
                              restartMagicdraw="false" type="Plugin">
            <version human={ver}
                     internal={ver}
                     resource={ver + "0"}/>
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
              { resourceFiles.map { case (_, path) =>
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

        // reduce the size to 240mb to force creating 2 zip files
        val result = sbt.Process(
          command = "/usr/bin/zipsplit",
          arguments = Seq[String](
            "-n", "251658240",
            "-b", zip1.getParent,
            zip.getAbsolutePath
          )).!

        require(0 == result, s"Failed to execute zipsplit")

        s.log.warn(s"Zip created (${zip.length()} bytes)\n$zip")
        s.log.warn(s"Part1 created (${zip1.length()} bytes)\n$zip1")
        s.log.warn(s"Part2 created (${zip2.length()} bytes)\n$zip2")

        ()
      }
    }


  )
