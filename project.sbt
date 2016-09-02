
sbtPlugin := false

name := "imce.dynamic_scripts.magicdraw.plugin"

description := "..."

moduleName := name.value

organization := "gov.nasa.jpl.imce"

homepage := Some(url(s"https://github.com/JPL-IMCE/${moduleName.value}"))

organizationName := "JPL-IMCE"

organizationHomepage := Some(url(s"https://github.com/JPL-IMCE"))

git.remoteRepo := s"git@github.com:JPL-IMCE/${moduleName.value}"

scmInfo := Some(ScmInfo(
  browseUrl = url(s"https://github.com/JPL-IMCE/${moduleName.value}"),
  connection = "scm:"+git.remoteRepo.value))

developers := List(
  Developer(
    id="NicolasRouquette",
    name="Nicolas F. Rouquette",
    email="nicolas.f.rouquette@jpl.nasa.gov",
    url=url("https://github.com/NicolasRouquette")))

