MBSE Asset Catalog: [MagicDraw DynamicScripts Plugin](https://cae-cop.jpl.nasa.gov/assetcatalog-admin/index.html#/view/tool/155)

# IMCE Dynamic Scripts plugin for MagicDraw 18

The IMCE Dynamic Scripts plugin is compatible with any edition of MagicDraw 18, including all CAE MagicDraw 18 packages.

## 1) Installation, Update & Removal

### 1.1) Installing or Updating IMCE Dynamic Scripts

1) Download the MagicDraw installable resource zip file

2) Start MagicDraw

3) Invoke the toolbar menu action: Help | Resource/Plugin Manager

4) In the "Resource/Plugin Manager" dialog:

   4.1) Click the "Import" button at the bottom

   4.2) In the file chooser dialog, select the IMCE Dynamic Scripts resource zip downloaded from (1)

5) MagicDraw will show a dialog indicating the actions that will be performed after restart:

  - Uninstall the previous version (if applicable)

  - Install the downloaded version

6) Restart MagicDraw & Quit

7) Setup MagicDraw.imce

  7.1) For Linux, MacOSX & Windows with [cygwin](http://cygwin.com)

   7.1.1) Open a linux, mac or cygwin terminal shell in MagicDraw's installation folder

   7.1.2) Execute `chmod 755 bin/magicdraw.imce`

   7.1.3) For Windows/cygwin, execute `chmod 755 bin/magicdraw.imce.exe`

   7.1.4) Execute `chmod 755 bin/magicdraw.imce.setup.sh`

   7.1.5) Execute `bin/magicdraw.imce.setup.sh`

   This setup script uses the current contents of `bin/magicdraw.properties` to
   overwrite the file `bin/magicdraw.imce.properties` such that:

   - `bin/magicdraw.imce` starts MagicDraw the configuration specified in `bin/magicdraw.imce.properties`
   - `bin/magicdraw.imce.exe` starts MagicDraw the configuration specified in `bin/magicdraw.imce.properties`

   - `bin/magicdraw` starts MagicDraw the configuration specified in `bin/magicdraw.properties`
   - `bin/magicdraw.exe` starts MagicDraw the configuration specified in `bin/magicdraw.properties`
   - On MacOSX, `MagicDraw.app` starts MagicDraw the configuration specified in `bin/magicdraw.properties`
     (Note: There is no `MagicDraw.imce.app` per se.)

  7.2) For Windows systems without [cygwin](http://cygwin.com)

   7.2.1) Manually copy `bin/magicdraw.properties` from MagicDraw's installation folder to `bin/magicdraw.imce.properties`

   7.2.2) Manually edit `bin/magicdraw.imce.properties` according to the `bin/magicdraw.imce.setup.sh` script:

   7.2.3) Replace `-D.local.config.dir.ext\=...` with `-Dlocal.config.dir.ext\=$IMCE_CONFIG_DIR`
        (see the setup script for the value of `$IMCE_CONFIG_DIR`)

   7.2.4) Prepend `$IMCE_JAVA_ARGS_PREFIX` to the `JAVA_ARGS` variable
        (see the setup script for the value of `$IMCE_JAVA_ARGS_PREFIX`)

   7.2.5) Prepend `$IMCE_BOOT_CLASSPATH_PREFIX` to the `BOOT_CLASSPATH` variable
        (see the setup script for the value of `$IMCE_BOOT_CLASSPATH_PREFIX`)

   7.2.6) Prepend `$IMCE_CLASSPATH_PREFIX` to the `CLASSPATH` variable
        (see the setup script for the value of `$IMCE_CLASSPATH_PREFIX`)

   7.2.7) Manually add the executable permission flags to `bin/magicdraw.imce.exe`

### 1.2) Removing IMCE Dynamic Scripts

1) Start MagicDraw (not the MagicDraw.imce configuration!)

2) Invoke the toolbar menu action: Help | Resource/Plugin Manager

3) In the "Resource/Plugin Manager" dialog, select the "IMCE Dynamic Scripts Plugin"

4) Click Remove

5) Quit and Restart MagicDraw (not the MagicDraw.imce configuration!)

## 2) MagicDraw.imce vs. MagicDraw

Differences are:

2.1) local configuration directory

  `-Dlocal.config.dir.ext` in `bin/magicdraw.imce.properties` vs. `bin/magicdraw.properties`

  Note that MagicDraw uses the local configuration directory for several purposes, including but not limited to:

  - environment options

  - local plugins (in addition to the plugins in the installation folder)

  - local templates (in addition to the templates in the installation folder)

  - local reports (in addition to the reports in the installation folder)

2.2) Boot Classpath

  MagicDraw.imce adds the AspectJ weaver, AspectJ runtime and Scala runtime libraries to enable the so-called
  [load-time weaving](https://eclipse.org/aspectj/doc/released/devguide/ltw.html) strategy for Aspects specified
  in Java or Scala.

2.3) Application Classpath

  MagicDraw.imce adds several third-party libraries for AspectJ, Scala, Jena and the OWL API & implementation.
  Although this adds a minimal overhead to the MagicDraw application startup, the significant advantage is
  in minimizing the startup of dynamic scripts since these libraries do not have to be loaded at each invocation.

## 3) Configuring Dynamic Scripts

By default, the IMCE Dynamic Script plugin scans the `dynamicScripts` folder in MagicDraw's installation for
files with the '.dynamicScript' or '.dynamicscript' extension. Such files are interpreted as specifications
of MagicDraw Dynamic Scripts.

MagicDraw Dynamic Script files outside the `dynamicScripts` folder must be explicitly registered as follows:

3.1) Start MagicDraw (IMCE configuration)

3.2) Invoke the toolbar menu action: Options | Environment

3.3) In the "Environment Options" dialog, select the "Dynamic Scripts Options" category in the left pane.

3.4) In the "Environment Options" dialog, select the "Dynamic Scripts Configuration Files" entry in the right pane.

3.5)  Click the "..." button to the right of the entry to open a text editor dialog.

3.6) In the "List of Dynamic Scripts Configuration Files", enter fully-qualified path
   to one or more '.dynamicScript' or '.dynamicscript' files.

## 4) MagicDraw Dynamic Scripts Specifications

See [DynamicScripts Generic DSL](https://github.jpl.nasa.gov/imce/imce.dynamic_scripts.generic_dsl/tree/cae_md18_0_sp5)


