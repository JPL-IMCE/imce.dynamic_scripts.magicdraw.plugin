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

lazy val imce_dynamic_scripts_magicdraw_plugin = Project("imce-dynamic_scripts-magicdraw-plugin", file("."))
  .enablePlugins(IMCEGitPlugin)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
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

      "gov.nasa.jpl.imce" %% "imce.third_party.scala_graph_libraries"
        % Versions_scala_graph_libraries.version artifacts
        Artifact("imce.third_party.scala_graph_libraries", "zip", "zip", Some("resource"), Seq(), None, Map()),

      "gov.nasa.jpl.imce" %% "imce.third_party.jena_libraries"
        % Versions_jena_libraries.version artifacts
        Artifact("imce.third_party.jena_libraries", "zip", "zip", Some("resource"), Seq(), None, Map()),

      "gov.nasa.jpl.imce" %% "imce.third_party.owlapi_libraries"
        % Versions_owlapi_libraries.version artifacts
        Artifact("imce.third_party.owlapi_libraries", "zip", "zip", Some("resource"), Seq(), None, Map())

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

    compile in Compile := (compile in Compile).dependsOn(unmanagedJars in Compile).value,

    compileIncremental in Compile := (compileIncremental in Compile).dependsOn(unmanagedJars in Compile).value,

    doc in Compile := (doc in Compile).dependsOn(unmanagedJars in Compile).value,

    unmanagedJars in Compile := (unmanagedJars in Compile).dependsOn(extractArchives).value,

    unmanagedJars in Compile ++= {
      val base = baseDirectory.value
      val up = update.value
      val s = streams.value
      val mdInstallDir = (mdInstallDirectory in ThisBuild).value

      val _ = extractArchives.value

      val libJars = ((mdInstallDir / "lib") ** "*.jar").get
      val mdJars = libJars.map { jar => Attributed.blank(jar) }

      s.log.info(s"=> Adding ${mdJars.size} unmanaged jars")

      mdJars
    },

    logLevel in AetherKeys.aetherDeploy := Level.Debug,

    AetherKeys.aetherArtifact := AetherKeys.aetherArtifact.dependsOn(zipInstall).value,

    AetherKeys.aetherDeploy := AetherKeys.aetherDeploy.dependsOn(zipInstall).value,

    // See the imce.sbt.plugin dependency:
    // https://github.com/JPL-IMCE/imce.sbt.plugin/blob/4.12.0/src/main/scala/gov/nasa/jpl/imce/sbt/IMCEPlugin.scala#L155
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

        val zipRoots: Map[Module, (String, ModuleGraph)] = (for {
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
        } yield root -> (groupName, graph)).toMap

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
            IO.copyFile(f, root / "lib" / group / f.name)
            s.log.info(s" jar: ${f.name}")
            val fs = f.getParentFile.getParentFile / "srcs" / (f.name.stripSuffix(".jar") + "-sources.jar")
            if (fs.exists && fs.canRead) {
              IO.copyFile(fs, root / "lib.sources" / group / fs.name)

            }
            //              val fd = f.getParentFile.getParentFile / "docs" / (f.name.stripSuffix(".jar") + "-javadoc.jar")
            //              if (fd.exists && fd.canRead) {
            //                IO.copyFile(fd, root / "lib.javadoc" / group / fd.name)
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
          IO.copyFile(jar, root / "lib" / o / jar.name)
        }

        srcArtifacts.foreach { case (o, jar) =>
          s.log.info(s"* copying source: $o/${jar.name}")
          IO.copyFile(jar, root / "lib.sources" / o / jar.name)
        }

        docArtifacts.foreach { case (o, jar) =>
          s.log.info(s"* copying javadoc: $o/${jar.name}")
          IO.copyFile(jar, root / "lib.javadoc" / o / jar.name)
        }

        val jars: Seq[File] = jarArtifacts.map { case (o, jar) =>
          new File("lib") / o / jar.name
        }

        val weaverJar: File = {
          val weaverJars = ((root / "lib" / "imce.third_party.aspectj") * "aspectjweaver-*.jar").get
          require(1 == weaverJars.size,
            weaverJars
              .map(_.toString)
              .mkString(s"Got ${weaverJars.size} weaver libraries (there should be only 1):\n","\n","\n"))
          val relJar = weaverJars.head.relativeTo(root)
          require(relJar.isDefined,
            s"The weaver library, ${weaverJars.head} should be resolvable from the root: $root")
          relJar.get
        }

        val rtJar: File = {
          val rtJars = ((root / "lib" / "imce.third_party.aspectj") * "aspectjrt-*.jar").get
          require(1 == rtJars.size)
          val relJar = rtJars.head.relativeTo(root)
          require(relJar.isDefined)
          relJar.get
        }

        val aspectjJars: Seq[File] = {
          val ajJars = ((root / "lib" / "imce.third_party.aspectj") * "*.jar").get
          require(1 < ajJars.size)
          (for {
            jar <- ajJars
            relJar <- jar.relativeTo(root)
          } yield relJar).to[Seq]
        }

        val scalaLib: File = {
          val scalaLibs = ((root / "lib" / "imce.third_party.scala") * "scala-library-*.jar").get
          require(1 == scalaLibs.size)
          val relJar = scalaLibs.head.relativeTo(root)
          require(relJar.isDefined)
          relJar.get
        }

        val scalaJars: Seq[File] = {
          val sJars = ((root / "lib" / "imce.third_party.scala") * "*.jar").get
          require(1 < sJars.size)
          (for {
            jar <- sJars
            relJar <- jar.relativeTo(root)
          } yield relJar).to[Seq]
        }

        val otherJars: Seq[File] = {
          val oJars = ((root / "lib" / "imce.third_party.other_scala") * "*.jar").get
          require(1 < oJars.size)
          (for {
            jar <- oJars
            relJar <- jar.relativeTo(root)
          } yield relJar).to[Seq]
        }

        val bootJars = Seq(weaverJar, rtJar, scalaLib)

        val bootClasspathPrefix = bootJars.mkString("", "\\\\\\\\:", "\\\\\\\\:")

        val classpathPrefix =
          (aspectjJars.sorted ++ scalaJars.sorted ++ otherJars.sorted ++ jars.sorted)
            .mkString("", "\\\\\\\\:", "\\\\\\\\:")

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

        val magicdraw_imce_properties =
          s"""# The contents of this file will be created from bin/magicdraw.properties
             |# by the bin/magicdraw.imce.setup.sh script.
             |# Since this file is tracked as part of the IMCE DynamicScripts plugin resource,
             |# MagicDraw will delete it when that plugin is removed through MagicDraw's Resource/Plugin Manager.
             |"""
            .stripMargin
        IO.write(root / "bin" / "magicdraw.imce.properties", magicdraw_imce_properties)

        val configPattern =
          "\\-Dlocal\\.config\\.dir\\.ext\\(\\\\\\\\=\\|=\\)[a-zA-Z0-9_\\.\\\\-]*"
        val configReplace =
          "-Dlocal.config.dir.ext\\\\\\\\=${IMCE_CONFIG_DIR}"

        val magicdraw_imce_setup =
          s"""#!/usr/bin/env bash
             |
             |pushd `dirname $$0` > /dev/null
             |MD_INSTALL_BIN=`pwd -P`
             |popd > /dev/null
             |MD_INSTALL_DIR=$$(dirname $$MD_INSTALL_BIN)
             |
             |# The original 'bin/magicdraw.properties' file.
             |# The contents of this file varies depending on which MagicDraw
             |# package is installed (NoMagic's, CAE Vendor, CAE Lib_patches, CAE MDK)
             |MD_ORIG_PROPERTIES=$$MD_INSTALL_BIN/magicdraw.properties
             |
             |# The imce-specific MagicDraw properties adapted from 'bin/magicdraw.properties'
             |MD_IMCE_PROPERTIES=$$MD_INSTALL_BIN/magicdraw.imce.properties
             |
             |if test ! -e "$$MD_ORIG_PROPERTIES"; then
             | echo "There is no 'bin/magicdraw.properties' file!"
             | exit -1
             |fi
             |
             |IMCE_CONFIG_DIR="-imce-$ver"
             |
             |IMCE_JAVA_ARGS_PREFIX="\\
             |-javaagent:$weaverJar \\
             |-Daj.weaving.verbose\\\\\\\\=true \\
             |-Dorg.aspectj.weaver.showWeaveInfo\\\\\\\\=true \\
             |-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
             |
             |IMCE_BOOT_CLASSPATH_PREFIX="$bootClasspathPrefix"
             |
             |IMCE_CLASSPATH_PREFIX="$classpathPrefix"
             |
             |cat $$MD_ORIG_PROPERTIES \\
             | | sed -e "s!$configPattern!$configReplace!" \\
             | | sed -e "s|^JAVA_ARGS=|JAVA_ARGS=$${IMCE_JAVA_ARGS_PREFIX} |" \\
             | | sed -e "s|BOOT_CLASSPATH=|BOOT_CLASSPATH=$${IMCE_BOOT_CLASSPATH_PREFIX}|" \\
             | | sed -e "s|^CLASSPATH=|CLASSPATH=$${IMCE_CLASSPATH_PREFIX}|" \\
             | > $$MD_IMCE_PROPERTIES
             |
             |chmod 755 $$MD_IMCE_PROPERTIES
             |
             |echo "Wrote $$MD_IMCE_PROPERTIES"
             |
             |grep -q "log4j\\.category\\.i18n" $$MD_INSTALL_DIR/data/debug.properties
             |if test $$? -eq 1; then
             |  echo -e "\\nlog4j.category.i18n=OFF" >> $$MD_INSTALL_DIR/data/debug.properties
             |  echo "Turned off i18n logging"
             |fi
             |""".stripMargin

        val setup
        = root / "bin" / "magicdraw.imce.setup.sh"

        IO.write(setup, magicdraw_imce_setup)

        setup.toScala.addPermission(PosixFilePermission.OWNER_EXECUTE)
        setup.toScala.addPermission(PosixFilePermission.GROUP_EXECUTE)
        setup.toScala.addPermission(PosixFilePermission.OTHERS_EXECUTE)
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
