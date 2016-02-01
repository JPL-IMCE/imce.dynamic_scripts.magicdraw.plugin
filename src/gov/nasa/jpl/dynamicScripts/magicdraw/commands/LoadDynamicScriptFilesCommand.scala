/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.commands

import java.io.File
import java.io.FilenameFilter
import java.lang.Throwable
import java.nio.file.Paths

import com.nomagic.task.RunnableWithProgress
import com.nomagic.task.ProgressStatus
import com.nomagic.magicdraw.utils.MDLog
import com.nomagic.magicdraw.core.Application

import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.{MDUML, TraversePath}

import scala.collection.immutable._
import scala.util.{Success,Try}
import scala.{Boolean, None, Some, StringContext, Unit}
import scala.Predef.String

/**
 * @author Nicolas F. Rouquette (JPL)
 */
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
          file.isFile && name.toLowerCase.endsWith(".dynamicscripts")
      }

      val dsPaths: List[String] =
        if ( dsRoot.exists() && dsRoot.canRead )
          TraversePath.listFilesRecursively( dsRoot, dsFilenameFilter ).map { file =>
            dsPath.relativize(file.toPath).toString
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