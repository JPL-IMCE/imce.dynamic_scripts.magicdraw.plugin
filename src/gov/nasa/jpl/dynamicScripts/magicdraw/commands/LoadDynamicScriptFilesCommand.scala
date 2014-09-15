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

/**
 * @author Nicolas F. Rouquette (JPL)
 */
class LoadDynamicScriptFilesCommand( refresh: () => Unit) extends RunnableWithProgress {

  override def run( progressStatus: ProgressStatus ): Unit = {
    val p = DynamicScriptsPlugin.getInstance()
    val files = p.getDynamicScriptsOptions().getDynamicScriptConfigurationFiles()
    val message = p.updateRegistryForConfigurationFiles( files ) match {
      case None =>       
        refresh()
        "\nSuccess"
      case Some( m ) =>
        "\n" + m
    }

    val guiLog = Application.getInstance().getGUILog()
    guiLog.log( "=============================" )
    guiLog.log( s"Reloaded ${files.size} dynamic script files:" )
    files.foreach { f => guiLog.log( "\n => " + f ) }
    guiLog.log( message )
    guiLog.log( "=============================" )
  }
}