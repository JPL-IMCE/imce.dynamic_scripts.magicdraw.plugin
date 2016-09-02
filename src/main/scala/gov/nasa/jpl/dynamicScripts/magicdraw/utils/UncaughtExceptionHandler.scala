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

package gov.nasa.jpl.dynamicScripts.magicdraw.utils

import java.lang.{Thread, Throwable}
import com.nomagic.utils.Utilities
import com.nomagic.magicdraw.core.Application

import scala.{Option, None, Some, StringContext, Unit}
import scala.Predef.String
/**
 * @author nicolas.f.rouquette@jpl.nasa.gov
 */
case class UncaughtExceptionHandler
( val title: String, implicit val thread: Thread = Thread.currentThread )
  extends Thread.UncaughtExceptionHandler {

  val parent: Option[Thread.UncaughtExceptionHandler] = {
    val p = Option.apply(thread.getUncaughtExceptionHandler)
    thread.setUncaughtExceptionHandler(this)    
    p
  }
  
  def uncaughtException( t: Thread, e: Throwable ): Unit = {
    Utilities.dumpThreads
    val message = s"$title - ${e.getClass.getCanonicalName}: ${e.getMessage}"
    val guiLog = Application.getInstance.getGUILog
    import MDGUILogHelper._

    guiLog.getMDPluginsLog.fatal( message, e )
    guiLog.showError( message, e )
    
    parent match {
      case None =>
        ()
      case Some( p ) =>
        p.uncaughtException(t, e)
    }    
  }
}