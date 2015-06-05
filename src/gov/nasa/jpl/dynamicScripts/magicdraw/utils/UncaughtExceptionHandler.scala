/*
 *
 * License Terms
 *
 * Copyright (c) 2014-2015, California Institute of Technology ("Caltech").
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