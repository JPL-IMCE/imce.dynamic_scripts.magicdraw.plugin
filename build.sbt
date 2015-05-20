enablePlugins(GitVersioning, GitBranchPrompt)

enablePlugins(gov.nasa.jpl.sbt.MagicDrawEclipseClasspathPlugin)

// the prefix for git-based versioning of the published artifacts
git.baseVersion in ThisBuild := "1800.02"

// turn on version detection
git.useGitDescribe := true

seq(versionWithGit: _*)
