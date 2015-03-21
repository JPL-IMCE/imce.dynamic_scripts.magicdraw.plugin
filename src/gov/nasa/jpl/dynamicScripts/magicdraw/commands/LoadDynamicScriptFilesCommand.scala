/**
 * Copyright 2013, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of
 * Technology Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws.
 * By acepting this software, the user agrees to comply with all applicable U.S. export laws
 * and regulations. User has the responsibility to obtain export licenses,
 * or other export authority as may be required before exprting such information
 * to foreign countries or providing access to foreign persons.
 *
 * Inquiries about this notice should be addressed to:
 *
 * JPL Software Release Authority
 * Phone: +1-818-393-3421
 * mailto:SoftwareRelease@jpl.nasa.gov
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.commands

import com.nomagic.task.RunnableWithProgress
import com.nomagic.task.ProgressStatus
import gov.nasa.jpl.dynamicScripts.magicdraw.DynamicScriptsPlugin
import com.nomagic.magicdraw.core.Application
import com.nomagic.magicdraw.utils.MDLog
import scala.util.Try
import scala.util.Success
import java.nio.file.Paths
import gov.nasa.jpl.dynamicScripts.magicdraw.utils.MDUML

/**
 * @author Nicolas F. Rouquette (JPL)
 */
class LoadDynamicScriptFilesCommand( refresh: () => Unit ) extends RunnableWithProgress {

  override def run( progressStatus: ProgressStatus ): Unit = {
    val p = DynamicScriptsPlugin.getInstance()
    val files = p.getDynamicScriptsOptions().getDynamicScriptConfigurationFiles()
    val guiLog = Application.getInstance().getGUILog()
    try {
      val installRoot = Paths.get( MDUML.getInstallRoot )
      val paths = ( for {
        file <- files
        path = Paths.get( file )
        dsPath = if ( path.isAbsolute ) path else installRoot.resolve( path )
      } yield dsPath.toFile.getAbsolutePath ).toList
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