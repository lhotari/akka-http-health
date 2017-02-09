/*
 * Copyright 2017 the original author or authors.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package io.github.lhotari.akka.http.health

import java.io.ByteArrayOutputStream
import java.lang.System.getProperty
import java.net.{URL, URLClassLoader}

import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

case class ProcessResult(retval: Integer, output: String)

trait ProcessSpawner {
  lazy val classpath = resolveClassPath()
  val sep = getProperty("file.separator")
  val javaExecutablePath = getProperty("java.home") + sep + "bin" + sep + "java"

  private def resolveClassPath() = {
    getClass.getClassLoader match {
      case urlClassLoader: URLClassLoader =>
        urlClassLoader.getURLs.collect {
          case url: URL => url.getFile
        }.mkString(getProperty("path.separator"))
      case _ =>
        getProperty("java.class.path")
    }
  }

  def executeInSeparateProcess[T](mainClassType: T, maxMemoryMB: Integer = 100, extraJvmOpts: Seq[String] = Nil, args: Seq[String] = Nil)(implicit tag: WeakTypeTag[T]): ProcessResult = {
    val className = tag.tpe.termSymbol.fullName
    val processBuilder = new ProcessBuilder(javaExecutablePath).redirectErrorStream(true)
    val commands = processBuilder.command()
    commands.add(s"-Xmx${maxMemoryMB}m")
    commands.addAll(extraJvmOpts.asJava)
    commands.add("-cp")
    commands.add(classpath)
    commands.add(className)
    commands.addAll(args.asJava)
    println(String.join(" ", commands))
    val process = processBuilder.start()
    val output = new ByteArrayOutputStream()
    IOUtils.copy(process.getInputStream, output)
    ProcessResult(process.waitFor(), output.toString())
  }
}
