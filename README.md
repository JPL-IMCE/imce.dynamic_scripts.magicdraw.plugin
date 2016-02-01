# IMCE Dynamic Scripts plugin for MagicDraw 18

## Installation

1) Download the MagicDraw installable resource zip file

2) Start MagicDraw

3) Invoke the toolbar menu action: Help | Resource/Plugin Manager

4) In the "Resource/Plugin Manager" dialog, click the "Import" button at the bottom

5) In the file chooser dialog, select the IMCE Dynamic Scripts resource zip from (1)

6) Upon successful installation, MagicDraw will show a dialog that the Dynamic Scripts plugin
   will be enabled after restarting MagicDraw

## Starting MagicDraw (CAE configuration without dynamic scripts plugin)

Start MagicDraw as instructed by CAE.
The Dynamic Scripts plugin will not be enabled in this configuration.

## Starting MagicDraw (IMCE configuration with dynamic scripts plugin)

1) Add the executable flags to `bin/magicdraw.imce` and `bin/magicdraw.imce.exe`

2) Make sure the environment has a `JAVA_HOME` environment variable set for Java8

3) Execute `bin/magicdraw.imce` (linux, mac) or `bin/magicdraw.imce.exe` (windows)

The Dynamic Scripts plugin will be enabled in this configuration.

## Configuring Dynamic Scripts

- Start MagicDraw (IMCE configuration)

- Invoke the toolbar menu action: Options | Environment

- In the "Environment Options" dialog, select the "Dynamic Scripts Options" category in the left pane.

- In the "Environment Options" dialog, select the "Dynamic Scripts Configuration Files" entry in the right pane.

  Click the "..." button to the right of the entry to open a text editor dialog.

- In the "List of Dynamic Scripts Configuration Files", enter relative paths to "*.dynamicScripts" files

  The paths must be relative to MagicDraw's installation folder.
  If necessary, add symbolic links in the MagicDraw installation folder
  to refer to files/folders outside of it.


