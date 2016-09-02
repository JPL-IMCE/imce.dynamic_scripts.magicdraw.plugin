/*
 * Copyright 2014 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * License Terms
 */

package gov.nasa.jpl.dynamicScripts.magicdraw.commands

import java.io.File
import java.io.FilenameFilter
import java.lang.Throwable
import java.nio.file.Paths

import com.nomagic.task.RunnableWithProgress
import com.nomagic.task.ProgressStatus
import com.nomagic.magicdraw.core.Application

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.{MDUML, TraversePath}

import scala.collection.immutable._
import scala.{Boolean, None, Some, StringContext, Unit}
import scala.Predef.String

object LoadDynamicScriptFilesCommand {

  val exclude_parent_folder_names
  : Set[String]
  = Set(".git", "bin", "classes", "target")

}

class LoadDynamicScriptFilesCommand( refresh: () => Unit ) extends RunnableWithProgress {

  override def run( progressStatus: ProgressStatus ): Unit = {
    val p = DynamicScriptsPlugin.getInstance()
    val files = p.getDynamicScriptsOptions.getDynamicScriptConfigurationFiles
    val guiLog = Application.getInstance().getGUILog
    try {
      val installRoot = Paths.get( MDUML.getInstallRoot )
      val configPaths: List[String] = for {
        file <- files
        path = Paths.get( file )
        dsPath = if ( path.isAbsolute ) path else installRoot.resolve( path )
      } yield dsPath.toFile.getAbsolutePath

      val dsPath = installRoot.resolve("dynamicScripts")
      val dsRoot = dsPath.toFile
      val dsFilenameFilter = new FilenameFilter() {
        override def accept( file: File, name: String ): Boolean =
          file.isDirectory &&
            !LoadDynamicScriptFilesCommand.exclude_parent_folder_names.contains(file.getName) &&
            name.toLowerCase.endsWith(".dynamicscripts") &&
            new File(file, name).isFile
      }

      val dsPaths: List[String] =
        if ( dsRoot.exists() && dsRoot.canRead )
          TraversePath.listFilesRecursively( dsRoot, dsFilenameFilter ).map { file =>
            dsPath.resolve(file.toPath).toString
          }
        else
          List()

      val paths = (configPaths ::: dsPaths).sorted

      val message = p.updateRegistryForConfigurationFiles( paths ) match {
        case None =>
          refresh()
          "\nSuccess"
        case Some( m ) =>
          "\n"+m
      }

      guiLog.log( "=============================" )
      guiLog.log( s"Reloaded ${paths.size} dynamic script files:" )
      paths.foreach { f => guiLog.log( "\n => "+f ) }
      guiLog.log( message )
      guiLog.log( "=============================" )
    } catch {
      case t: Throwable =>
        guiLog.showError( s"Failed to load dynamicScript file", t )
    }
  }
}