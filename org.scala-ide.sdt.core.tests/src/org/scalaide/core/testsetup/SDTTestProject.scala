package org.scalaide.core
package testsetup

import java.io.ByteArrayInputStream
import java.io.File
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.launching.JavaRuntime
import org.scalaide.core.SdtConstants

/** A test project, created from scratch.
 *
 */
class SDTTestProject(project : IProject) {
  val location = project.getLocation.toOSString

  val javaProject = JavaCore.create(project)
  addJavaNature
  addScalaNature
  javaProject.setRawClasspath(new Array[IClasspathEntry](0), null)
  addJavaSystemLibraries
  addScalaSystemLibraries
  val sourceFolder = createSourceFolder
  val binFolder = createBinFolder
  createOutputFolder(binFolder)

  def this(remove : Boolean = false, projectName : String = "Project-1") {
    this({
      val root = ResourcesPlugin.getWorkspace.getRoot
      val project = root.getProject(projectName)
      if (remove)
        project.delete(true, null)
      project.create(null)
      project.open(null)
      project
    })
  }

  def addJar(plugin : String, jar : String): Unit = {
    val result = findFileInPlugin(plugin, jar)
    addToClasspath(JavaCore.newLibraryEntry(result, null, null))
  }

  def createPackage(name : String) : IPackageFragment =
    sourceFolder.createPackageFragment(name, false, null)

  def createType(pack : IPackageFragment, cuName : String, source : String) : IType = {
    val  buf = new StringBuffer
    buf.append("package " + pack.getElementName() + ";\n")
    buf.append("\n")
    buf.append(source)
    val cu = pack.createCompilationUnit(cuName, buf.toString(), false, null)
    cu.getTypes()(0)
  }

  def createFile(name : String, content : Array[Byte]) : IFile = {
    val file = project.getFile(name)
    val inputStream = new ByteArrayInputStream(content)
    file.create(inputStream, true, null)
    file
  }

  def createFolder(name : String) : IFolder = {
    val folder = project.getFolder(name)
    folder.create(true, true, null)
    val keep = project.getFile(name + "/keep")
    keep.create(new ByteArrayInputStream(Array(0)), true, null)
    folder
  }

  def dispose(): Unit = {
    if (project.exists)
      project.delete(true, true, null)
    else
      SDTTestUtils.deleteRecursive(new File(location))
  }

  def createBinFolder() : IFolder = {
    val binFolder = project.getFolder("bin")
    binFolder.create(false, true, null)
    binFolder
  }

  def addJavaNature(): Unit = {
    addNature(JavaCore.NATURE_ID)
  }

  def addScalaNature(): Unit = {
    addNature(SdtConstants.NatureId)
  }

  def addNature(natureId : String): Unit = {
    val description = project.getDescription
    description.setNatureIds(natureId +: description.getNatureIds)
    project.setDescription(description, null)
  }

  def addToClasspath(entry : IClasspathEntry): Unit = {
    javaProject.setRawClasspath(entry +: javaProject.getRawClasspath, null)
  }

  def createOutputFolder(binFolder : IFolder): Unit = {
    val outputLocation = binFolder.getFullPath
    javaProject.setOutputLocation(outputLocation, null)
  }

  def createSourceFolder() : IPackageFragmentRoot = {
    val folder = project.getFolder("src")
    folder.create(false, true, null)
    val root = javaProject.getPackageFragmentRoot(folder)
    addToClasspath(JavaCore.newSourceEntry(root.getPath))
    root
  }

  def addJavaSystemLibraries(): Unit = {
    addToClasspath(JavaRuntime.getDefaultJREContainerEntry)
  }

  def addScalaSystemLibraries(): Unit = {
    addToClasspath(JavaCore.newContainerEntry(Path.fromPortableString(SdtConstants.ScalaLibContId)))
  }

  def findFileInPlugin(plugin : String, file : String) : Path = {
    val bundle = Platform.getBundle(plugin)
    val resource = bundle.getResource(file)
    new Path(resource.getPath)
  }

  def getFileContent(filepath : String) : String = {
    val file = project.getFile(filepath)
    val stream = file.getContents
    SDTTestUtils.slurpAndClose(stream)
  }
}
