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

import java.io.File
import java.io.FilenameFilter
import java.lang.Throwable
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.immutable._
import scala.collection.Traversable
import scala.collection.JavaConversions._
import scala.Predef.ArrowAssoc
import scala.{Int, Unit}

/**
 * @see ttp://stackoverflow.com/a/8927040/1009693
 *
 * @param path The starting path of the traversal
 * @param options @see java.nio.file.FileVisitOption
 * @param maxDepth The maximum depth of the traversal from the starting path
 */
class TraversePath(path: Path, options: Set[FileVisitOption], maxDepth: Int) extends Traversable[(Path, BasicFileAttributes)] {

  override def foreach[U](f: ((Path, BasicFileAttributes)) => U): Unit = {

    class Visitor extends SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
        try {
          f(file -> attrs)
          FileVisitResult.CONTINUE
        } catch {
          case _: Throwable =>
            FileVisitResult.TERMINATE
        }
    }

    Files.walkFileTree(path, options, maxDepth, new Visitor)
    ()
  }

}

object TraversePath {

  def listFilesRecursively(dir: File,
                           filter: FilenameFilter,
                           options: Set[FileVisitOption] = Set(FileVisitOption.FOLLOW_LINKS),
                           maxDepth: Int = 5): List[File] = {
    val traversal = new TraversePath(dir.toPath, options, maxDepth)
    val matchingFiles = new scala.collection.mutable.ListBuffer[File]()
    traversal.foreach {
      case (p, _) =>
        val f = p.toFile
        if (filter.accept(f.getParentFile, f.getName))
          matchingFiles.add(f)
    }
    matchingFiles.toList
  }

}