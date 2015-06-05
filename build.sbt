
import gov.nasa.jpl.mbee.sbt._
import sbt.Keys._
import sbt._

import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

lazy val jpl_dynamicScripts_magicDraw_plugin = Project("jpl-dynamicScripts-magicDraw-plugin", file(".")).
  settings(GitVersioning.buildSettings). // in principle, unnecessary; in practice: doesn't work without this
  enablePlugins(MBEEGitPlugin, MBEEMagicDrawEclipseClasspathPlugin).
  settings(aspectjSettings: _*).
  settings(
    MBEEKeys.mbeeLicenseYearOrRange := "2014-2015",
    MBEEKeys.mbeeOrganizationInfo := MBEEPlugin.MBEEOrganizations.imce,
    scalacOptions += "-g:vars",

    javacOptions += "-g:vars",

    extraAspectjOptions in Aspectj := Seq("-g"),

    // only compile the aspects (no weaving)
    compileOnly in Aspectj := true,

    // add the compiled aspects as products
    products in Compile <++= products in Aspectj,

    resourceDirectory in Compile := baseDirectory.value / "resources",

    aspectjSource in Aspectj := baseDirectory.value / "aspectj",

    compileOrder := CompileOrder.ScalaThenJava,
    javaSource in Compile := baseDirectory.value / "aspectj",

    scalaSource in Compile := baseDirectory.value / "src",
    scalaSource in Test := baseDirectory.value / "tests",
    classDirectory in Compile := baseDirectory.value / "bin",
    classDirectory in Test := baseDirectory.value / "bin.tests",
    unmanagedClasspath in Compile <++= unmanagedJars in Compile
  )
