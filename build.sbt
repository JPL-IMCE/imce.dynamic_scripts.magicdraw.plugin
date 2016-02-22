import better.files.{File => BFile, _}
import java.io.File
import java.nio.file.Files

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

useGpg := true

developers := List(
  Developer(
    id="rouquett",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://gateway.jpl.nasa.gov/personal/rouquett/default.aspx")))

import scala.io.Source
import scala.util.control.Exception._

def docSettings(diagrams:Boolean): Seq[Setting[_]] =
  Seq(
    sources in (Compile,doc) <<= (git.gitUncommittedChanges, sources in (Compile,compile)) map {
      (uncommitted, compileSources) =>
        if (uncommitted)
          Seq.empty
        else
          compileSources
    },

    sources in (Test,doc) <<= (git.gitUncommittedChanges, sources in (Test,compile)) map {
      (uncommitted, testSources) =>
        if (uncommitted)
          Seq.empty
        else
          testSources
    },

    scalacOptions in (Compile,doc) ++=
      (if (diagrams)
        Seq("-diagrams")
      else
        Seq()
        ) ++
        Seq(
          "-doc-title", name.value,
          "-doc-root-content", baseDirectory.value + "/rootdoc.txt"
        ),
    autoAPIMappings := ! git.gitUncommittedChanges.value,
    apiMappings <++=
      ( git.gitUncommittedChanges,
        dependencyClasspath in Compile in doc,
        IMCEKeys.nexusJavadocRepositoryRestAPIURL2RepositoryName,
        IMCEKeys.pomRepositoryPathRegex,
        streams ) map { (uncommitted, deps, repoURL2Name, repoPathRegex, s) =>
        if (uncommitted)
          Map[File, URL]()
        else
          (for {
            jar <- deps
            url <- jar.metadata.get(AttributeKey[ModuleID]("moduleId")).flatMap { moduleID =>
              val urls = for {
                (repoURL, repoName) <- repoURL2Name
                (query, match2publishF) = IMCEPlugin.nexusJavadocPOMResolveQueryURLAndPublishURL(
                  repoURL, repoName, moduleID)
                url <- nonFatalCatch[Option[URL]]
                  .withApply { (_: java.lang.Throwable) => None }
                  .apply({
                    val conn = query.openConnection.asInstanceOf[java.net.HttpURLConnection]
                    conn.setRequestMethod("GET")
                    conn.setDoOutput(true)
                    repoPathRegex
                      .findFirstMatchIn(Source.fromInputStream(conn.getInputStream).getLines.mkString)
                      .map { m =>
                        val javadocURL = match2publishF(m)
                        s.log.info(s"Javadoc for: $moduleID")
                        s.log.info(s"= mapped to: $javadocURL")
                        javadocURL
                      }
                  })
              } yield url
              urls.headOption
            }
          } yield jar.data -> url).toMap
      }
  )

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

lazy val thisVersion = SettingKey[String]("this-version", "This Module's version")

cleanFiles <+= baseDirectory { base => base / "imce.md.package" }

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val zipInstall = TaskKey[File]("zip-install", "Zip the MD Installation directory")

lazy val imce_dynamic_scripts_magicdraw_plugin = Project("imce-dynamic_scripts-magicdraw-plugin", file("."))
  .enablePlugins(IMCEGitPlugin)
  .enablePlugins(IMCEReleasePlugin)
  .settings(IMCEReleasePlugin.libraryReleaseProcessSettings)
  .settings(IMCEPlugin.aspectJSettings)
  .settings(IMCEPlugin.strictScalacFatalWarningsSettings)
  .settings(docSettings(diagrams=true))
  .settings(
    IMCEKeys.licenseYearOrRange := "2014-2016",
    IMCEKeys.organizationInfo := IMCEPlugin.Organizations.cae,
    IMCEKeys.targetJDK := IMCEKeys.jdk18.value,
    git.baseVersion := Versions.version,

    thisVersion := version.value,

    buildInfoPackage := "gov.nasa.jpl.imce.dynamic_scripts.plugin",
    buildInfoKeys ++= Seq[BuildInfoKey](BuildInfoKey.action("buildDateUTC") { buildUTCDate.value }),

    name := "imce_md18_0_sp5_dynamic-scripts",
    organization := "gov.nasa.jpl.imce.magicdraw.plugins",

    projectID := {
      val previous = projectID.value
      previous.extra(
        "build.date.utc" -> buildUTCDate.value,
        "artifact.kind" -> "magicdraw.resource.plugin")
    },

    artifactZipFile := {
      baseDirectory.value /
        "target" /
        s"imce_md18_0_sp5_dynamic-scripts_resource_${scalaBinaryVersion.value}-${version.value}.zip"
    },

    addArtifact(
      Artifact("imce_md18_0_sp5_dynamic-scripts", "zip", "zip", Some("resource"), Seq(), None, Map()),
      artifactZipFile),

    IMCEKeys.nexusJavadocRepositoryRestAPIURL2RepositoryName := Map(
      "https://oss.sonatype.org/service/local" -> "releases",
      "https://cae-nexuspro.jpl.nasa.gov/nexus/service/local" -> "JPL"),
    IMCEKeys.pomRepositoryPathRegex := """\<repositoryPath\>\s*([^\"]*)\s*\<\/repositoryPath\>""".r,

    resourceDirectory in Compile := baseDirectory.value / "resources",

    scalaSource in Compile := baseDirectory.value / "src",

    unmanagedClasspath in Compile <++= unmanagedJars in Compile,

    libraryDependencies ++= Seq(

      "gov.nasa.jpl.imce.magicdraw.libraries" %% "imce-magicdraw-library-enhanced_api"
        % Versions_enhanced_api.version
        % "compile" withSources(),

      "gov.nasa.jpl.imce.dynamic_scripts" %% "imce-dynamic_scripts-generic_dsl"
        % Versions_dynamic_scripts_generic_dsl.version
        % "compile" withSources() withJavadoc(),

      "gov.nasa.jpl.imce.thirdParty" %% "scala-graph-libraries"
        % Versions_scala_graph_libraries.version artifacts
        Artifact("scala-graph-libraries", "zip", "zip", Some("resource"), Seq(), None, Map()),

      "gov.nasa.jpl.imce.thirdParty" %% "jena-libraries"
        % Versions_jena_libraries.version artifacts
        Artifact("jena-libraries", "zip", "zip", Some("resource"), Seq(), None, Map()),

      "gov.nasa.jpl.imce.thirdParty" %% "owlapi-libraries"
        % Versions_owlapi_libraries.version artifacts
        Artifact("owlapi-libraries", "zip", "zip", Some("resource"), Seq(), None, Map())

    ),

    extractArchives <<= (baseDirectory, update, streams) map {
      (base, up, s) =>

        val mdInstallDir = base / "target" / "imce.md.package"

        if (!mdInstallDir.exists) {

          val zfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean = {
              a.`type` == "zip" && a.extension == "zip" && a.name == "cae_md18_0_sp5_vendor"
            }
          }
          val zs: Seq[File] = up.matching(zfilter).to[Seq]
          zs.foreach { zip =>
            val files = IO.unzip(zip, mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip: ${zip.getName}")
          }

          val mdBinFolder = mdInstallDir / "bin"
          require(mdBinFolder.exists, "md bin: $mdBinFolder")

        } else {
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")
        }

    },

    unmanagedJars in Compile <++= (baseDirectory, update, streams, extractArchives) map {
      (base, up, s, _) =>

        val mdInstallDir = base / "target" / "imce.md.package"

        val mdBinFolder = mdInstallDir / "bin"
        require(mdBinFolder.exists, "md bin: $mdBinFolder")

        val libJars = ((mdInstallDir / "lib") ** "*.jar").get
        s.log.info(s"jar libraries: ${libJars.size}")

        val mdJars = libJars.map { jar => Attributed.blank(jar) }

        mdJars
    },

    compile <<= (compile in Compile) dependsOn extractArchives,

    publish <<= publish dependsOn zipInstall,
    PgpKeys.publishSigned <<= PgpKeys.publishSigned dependsOn zipInstall,

    publishM2 <<= publishM2 dependsOn zipInstall,
    PgpKeys.publishLocalSigned <<= PgpKeys.publishLocalSigned dependsOn zipInstall,

    zipInstall <<=
      (baseDirectory, update, streams,
        version,
        artifactZipFile,
        packageBin in Compile,
        packageSrc in Compile,
        packageDoc in Compile,
        makePom, buildUTCDate,
        scalaBinaryVersion) map {
        (base, up, s, ver, zip, libJar, libSrc, libDoc, pom, d, sbV) =>

          import com.typesafe.sbt.packager.universal._
          import java.nio.file.attribute.PosixFilePermission

          val mdInstallDir = base / "target" / "imce.md.package"
          val root = base / "target" / "imce_md18_0_sp5_dynamic-scripts"
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
                  g.nodes.find(m => m.id == tID && (m.jarFile.isDefined || includeZips))
                }
              }.to[Set]
              if (next.isEmpty)
                result
              else
                acc(next, result ++ next)
            }

            acc(modules, Set()).to[Seq].sortBy( m => m.id.organisation + m.id.name)
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

          val zipFiles: Map[Module, Set[File]] = zipAllFiles.map { case (r, (group, allFiles, includes)) =>
            val others: Set[File] = for {
              i <- includes
              (_, files, _) = zipAllFiles(i)
              f <- files
            } yield f


            val files: Set[File] = allFiles -- others
            s.log.info(s"group: $group root: ${r.id.organisation} ${r.id.name} ${r.id.version} => group files: ${files.size}")
            files.foreach { f =>
              IO.copyFile(f, root / "lib" / group / f.name)
              s.log.info(s" jar: ${f.name}")
              val fs = f.getParentFile.getParentFile / "srcs" / (f.name.stripSuffix(".jar") + "-sources.jar")
              if (fs.exists && fs.canRead) {
                IO.copyFile(fs, root / "lib.sources" / group / fs.name)

              }
              val fd = f.getParentFile.getParentFile / "docs" / (f.name.stripSuffix(".jar") + "-javadoc.jar")
              if (fd.exists && fd.canRead) {
                IO.copyFile(fd, root / "lib.javadoc" / group / fd.name)
              }
            }
            r -> files
          }

          val groupedFiles: Set[File] = zipFiles.values.flatten.to[Set]

          val fileArtifacts = for {
            oReport <- compileConfig.details
            mReport <- oReport.modules
            (artifact, file) <- mReport.artifacts
            if "jar" == artifact.extension && !groupedFiles.contains(file)
          } yield {
            s.log.info(s"additional: ${file.getParentFile.getParentFile.name}/${file.getParentFile.name}/${file.name}")
            (oReport.organization, oReport.name, file, artifact)
          }

          val fileArtifactsByType = fileArtifacts.groupBy { case (_, _, _, a) =>
            a.`classifier`.getOrElse(a.`type`)
          }

          // -----

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

          val weaverJar: File = {
            val weaverJars = (( root / "lib" / "aspectj" ) * "aspectjweaver-*.jar").get
            require(1 == weaverJars.size)
            val relJar = weaverJars.head.relativeTo(root)
            require(relJar.isDefined)
            relJar.get
          }

          val rtJar: File = {
            val rtJars = (( root / "lib" / "aspectj" ) * "aspectjrt-*.jar").get
            require(1 == rtJars.size)
            val relJar = rtJars.head.relativeTo(root)
            require(relJar.isDefined)
            relJar.get
          }

          val aspectjJars: Seq[File] = {
            val ajJars = (( root / "lib" / "aspectj" ) * "*.jar").get
            require(1 < ajJars.size)
            (for {
              jar <- ajJars
              relJar <- jar.relativeTo(root)
            } yield relJar).to[Seq]
          }

          val scalaLib: File = {
            val scalaLibs = ((root / "lib" / "scala" ) * "scala-library-*.jar").get
            require(1 == scalaLibs.size)
            val relJar = scalaLibs.head.relativeTo(root)
            require(relJar.isDefined)
            relJar.get
          }

          val scalaJars: Seq[File] = {
            val sJars = (( root / "lib" / "scala" ) * "*.jar").get
            require(1 < sJars.size)
            (for {
              jar <- sJars
              relJar <- jar.relativeTo(root)
            } yield relJar).to[Seq]
          }

          val otherJars: Seq[File] = {
            val oJars = (( root / "lib" / "other-scala" ) * "*.jar").get
            require(1 < oJars.size)
            (for {
              jar <- oJars
              relJar <- jar.relativeTo(root)
            } yield relJar).to[Seq]
          }

          val bootJars = Seq(weaverJar, rtJar, scalaLib)

          val bootClasspathPrefix = bootJars.mkString("", "\\\\\\\\:", "\\\\\\\\:")

          val classpathPrefix = (aspectjJars.sorted ++
            scalaJars.sorted ++
            otherJars.sorted ++
            jars.sorted).mkString("", "\\\\\\\\:", "\\\\\\\\:")

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

          val magicdraw_imce_properties =
            s"""# The contents of this file will be created from bin/magicdraw.properties
               |# by the bin/magicdraw.imce.setup.sh script.
               |# Since this file is tracked as part of the IMCE DynamicScripts plugin resource,
               |# MagicDraw will delete it when that plugin is removed through MagicDraw's Resource/Plugin Manager.
             """.stripMargin
          IO.write(root / "bin" / "magicdraw.imce.properties", magicdraw_imce_properties)

          val configPattern = "\\-Dlocal\\.config\\.dir\\.ext\\(\\\\\\\\=\\|=\\)[a-zA-Z0-9_\\.\\\\-]*"
          val configReplace = "-Dlocal.config.dir.ext\\\\\\\\=${IMCE_CONFIG_DIR}"

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
               |if test ! -e ; then
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
               |echo "Wrote $$MD_IMCE_PROPERTIES"
             """.stripMargin

          val setup = root / "bin" / "magicdraw.imce.setup.sh"
          IO.write(setup, magicdraw_imce_setup)
          setup.toScala.addPermission(PosixFilePermission.OWNER_EXECUTE)
          setup.toScala.addPermission(PosixFilePermission.GROUP_EXECUTE)
          setup.toScala.addPermission(PosixFilePermission.OTHERS_EXECUTE)

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
                    version={ver} internalVersion={ver + "0"}
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
              <version human={ver} internal={ver} resource={ver + "0"}/>
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
