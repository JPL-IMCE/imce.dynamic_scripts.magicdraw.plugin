/*
 * Replace this with your license text!
 */
package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import com.nomagic.utils.Utilities
import com.nomagic.magicdraw.core.Application

/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class UncaughtExceptionHandler( val title: String, implicit val thread: Thread = Thread.currentThread ) extends Thread.UncaughtExceptionHandler {

  val parent: Option[Thread.UncaughtExceptionHandler] = {
    val p = Option.apply(thread.getUncaughtExceptionHandler)
    thread.setUncaughtExceptionHandler(this)    
    p
  }
  
  def uncaughtException( t: Thread, e: Throwable ): Unit = {
    Utilities.dumpThreads
    val message = s"${title} - ${e.getClass.getCanonicalName}: ${e.getMessage}"
    MDGUILogHelper.getMDPluginsLog.fatal( message, e )
    Application.getInstance.getGUILog.showError( message, e )
    
    parent match {
      case None => ()
      case Some( p ) =>
        p.uncaughtException(t, e)
    }    
  }
}