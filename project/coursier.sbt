// Unfortunately, coursier 1.0.0-M15 doesn't work here...

// sbt extractArchives fails:
// imce.dynamic_scripts.magicdraw.plugin(master)> extractArchives
// [trace] Stack trace suppressed: run last *:update for the full output.
// [error] (*:update) coursier.ResolutionException: 1 not found
// [error]   https://dl.bintray.com/tiwg/org.omg.tiwg.vendor.nomagic/org/omg/tiwg/vendor/nomagic/com.nomagic.magicdraw.package/18.0-sp6.2/com.nomagic.magicdraw.package-18.0-sp6.2.jar

// https://github.com/alexarchambault/coursier
// addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")