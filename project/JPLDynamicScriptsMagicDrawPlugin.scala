//import java.io.File
//
//import com.banno.license.Plugin.LicenseKeys._
//import gov.nasa.jpl.sbt.MagicDrawEclipseClasspathPlugin._
//import com.typesafe.sbt.SbtGit._
//import net.virtualvoid.sbt.graph.Plugin.graphSettings
//import sbt.Keys._
//import sbt._
//import xerial.sbt.Pack._
//
//import com.typesafe.sbt.SbtAspectj._
//import com.typesafe.sbt.SbtAspectj.AspectjKeys._
//
//
///**
// * sbt \
// * -DJPL_MBEE_LOCAL_REPOSITORY=<directory path for a local Ivy2 repository (will be created if necessary)> \
// * -DMD_INSTALL_DIR=<dir where MagicDraw 18 is installed> \
// * publish
// */
//object JPLDynamicScriptsMagicDrawPlugin extends Build {
//
//  object Versions {
//    val scala = "2.11.6"
//  }
//
//  lazy val jplSettings = Seq(
//    scalaVersion := Versions.scala,
//    organization := "gov.nasa.jpl.mbee",
//    organizationName := "JPL, Caltech",
//    organizationHomepage := Some(url("https://mbse.jpl.nasa.gov")),
//    publishMavenStyle := false,
//    publishTo := {
//      Option.apply(System.getProperty("JPL_MBEE_LOCAL_REPOSITORY")) match {
//        case Some(dir) => Some(Resolver.file("file", new File(dir))(Resolver.ivyStylePatterns))
//        case None => sys.error("Set -DJPL_MBEE_LOCAL_REPOSITORY=<dir> where <dir> is a local Ivy repository directory")
//      }
//    },
//    resolvers += {
//      Option.apply(System.getProperty("JPL_MBEE_LOCAL_REPOSITORY")) match {
//        case Some(dir) => Resolver.file("file", new File(dir))(Resolver.ivyStylePatterns)
//        case None => sys.error("Set -DJPL_MBEE_LOCAL_REPOSITORY=<dir> where <dir> is a local Ivy repository directory")
//      }
//    }
//  )
//
//  lazy val commonSettings =
//    Defaults.coreDefaultSettings ++ Defaults.runnerSettings ++ Defaults.baseTasks ++ graphSettings
//
//  lazy val aggregateDependenciesPublishSettings = Seq(
//    // disable publishing the main jar produced by `package`
//    publishArtifact in(Compile, packageBin) := true,
//
//    // disable publishing the main API jar
//    publishArtifact in(Compile, packageDoc) := true,
//
//    // disable publishing the main sources jar
//    publishArtifact in(Compile, packageSrc) := true
//  )
//
//  def mappingFromProject(mappings: ((Seq[TaskKey[File]], Seq[Configuration]), String)*)(currentProject: ProjectRef, structure: BuildStructure): Task[Seq[(File, String)]] = {
//    (mappings flatMap { case ((targetTasks: Seq[TaskKey[File]], configs: Seq[Configuration]), where: String) =>
//      targetTasks flatMap { t: TaskKey[File] =>
//        configs map { c =>
//          Def.task {
//            val file = ((t in c) in currentProject).value
//            (file, where + "/" + file.getName)
//          } evaluate structure.data
//        }
//      }
//    }).join
//  }
//
//  lazy val dependenciesPackSettings = packSettings ++ Seq(
//    packExpandedClasspath := false,
//    packLibJars := Seq.empty,
//    packUpdateReports := Seq.empty,
//    mappings in pack <<= (thisProjectRef, buildStructure) flatMap mappingFromProject(
//      (Seq(packageBin), Seq(Compile, Test)) -> "lib",
//      (Seq(packageSrc), Seq(Compile, Test)) -> "lib.srcs",
//      (Seq(packageDoc), Seq(Compile, Test)) -> "lib.javadoc"
//    )
//  ) ++ publishPackZipArchive
//
//  lazy val jpl_dynamicScripts_magicDraw_plugin = Project("jpl-dynamicScripts-magicDraw-plugin", file(".")).
//    settings(versionWithGit: _*).
//    settings(showCurrentGitBranch: _*).
//    settings(jplSettings: _*).
//    settings(commonSettings: _*).
//    settings(aggregateDependenciesPublishSettings: _*).
//    settings(com.banno.license.Plugin.licenseSettings: _*).
//    settings(dependenciesPackSettings: _*).
//    settings(magicDrawEclipseClasspathSettings: _*).
//    settings(
//      removeExistingHeaderBlock := true,
//
//      scalacOptions += "-g:vars",
//
//      scalaSource in Compile := baseDirectory.value / "src",
//      scalaSource in Test := baseDirectory.value / "tests",
//      classDirectory in Compile := baseDirectory.value / "bin",
//      classDirectory in Test := baseDirectory.value / "bin.tests",
//      unmanagedClasspath in Compile <++= unmanagedJars in Compile
//    )
//}
