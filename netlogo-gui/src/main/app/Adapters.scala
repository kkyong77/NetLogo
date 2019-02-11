// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app

import java.lang.reflect.{ Type => JType }

import org.nlogo.core.Dialect
import org.nlogo.api.{ AddableComponent, AutoConvertable, ConfigurableModelLoader, ModelLoader, Workspace => ApiWorkspace }
import org.nlogo.fileformat, fileformat.{ ModelConversion, ModelConverter }
import org.nlogo.nvm.{ DefaultCompilerServices, PresentationCompilerInterface }

import org.picocontainer.PicoContainer
import org.picocontainer.adapters.AbstractAdapter

import scala.collection.JavaConverters._

object Adapters {
  class ModelLoaderComponent extends AbstractAdapter[ModelLoader](classOf[ModelLoader], classOf[ConfigurableModelLoader]) {
    def getDescriptor(): String = "ModelLoaderComponent"
    def verify(p: PicoContainer): Unit = {}

    def getComponentInstance(container: org.picocontainer.PicoContainer, into: JType) = {
      val compiler         = container.getComponent(classOf[PresentationCompilerInterface])
      val compilerServices = new DefaultCompilerServices(compiler)

      val loader =
        fileformat.standardLoader(compilerServices)
      val additionalComponents =
        container.getComponents(classOf[AddableComponent]).asScala
      if (additionalComponents.nonEmpty) {
        additionalComponents.foldLeft(loader) {
          case (l, component) => component.addToLoader(l)
        }
      }
        else
          loader
    }
  }

  class ModelConverterComponent extends AbstractAdapter[ModelConversion](classOf[ModelConversion], classOf[ModelConverter]) {
    def getDescriptor(): String = "ModelConverterComponent"
    def verify(x: PicoContainer): Unit = {}

    def getComponentInstance(container: org.picocontainer.PicoContainer, into: JType) = {
      val workspace = container.getComponent(classOf[ApiWorkspace])

      val allAutoConvertables =
        fileformat.defaultAutoConvertables ++ container.getComponents(classOf[AutoConvertable]).asScala

      fileformat.converter( workspace.getExtensionManager, workspace.getLibraryManager
                          , workspace.getCompilationEnvironment, workspace, allAutoConvertables)(
                            container.getComponent(classOf[Dialect]))
    }
  }
}
