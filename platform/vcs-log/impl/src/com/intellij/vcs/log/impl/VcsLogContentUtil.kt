// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.vcs.log.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.TabDescriptor
import com.intellij.ui.content.TabGroupId
import com.intellij.util.Consumer
import com.intellij.util.ContentUtilEx
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.vcs.log.VcsLogBundle
import com.intellij.vcs.log.VcsLogUi
import com.intellij.vcs.log.impl.VcsLogManager.VcsLogUiFactory
import com.intellij.vcs.log.ui.MainVcsLogUi
import com.intellij.vcs.log.ui.VcsLogNotificationIdsHolder
import com.intellij.vcs.log.ui.VcsLogPanel
import com.intellij.vcs.log.ui.VcsLogUiEx
import java.util.function.Function
import java.util.function.Supplier
import javax.swing.JComponent

/**
 * Utility methods to operate VCS Log tabs as [Content]s of the [ContentManager] of the VCS toolwindow.
 */
object VcsLogContentUtil {

  private fun getLogUi(c: JComponent): VcsLogUiEx? {
    val uis = VcsLogPanel.getLogUis(c)
    require(uis.size <= 1) { "Component $c has more than one log ui: $uis" }
    return uis.singleOrNull()
  }

  internal fun selectLogUi(project: Project, logUi: VcsLogUi, requestFocus: Boolean = true): Boolean {
    val toolWindow = getToolWindow(project) ?: return false
    val manager = toolWindow.contentManager
    val component = ContentUtilEx.findContentComponent(manager) { c -> getLogUi(c)?.id == logUi.id } ?: return false

    if (!toolWindow.isVisible) {
      toolWindow.activate(null)
    }
    return ContentUtilEx.selectContent(manager, component, requestFocus)
  }

  fun getId(content: Content): String? {
    return getLogUi(content.component)?.id
  }

  @JvmStatic
  fun <U : VcsLogUiEx> openLogTab(project: Project,
                                  logManager: VcsLogManager,
                                  tabGroupId: TabGroupId,
                                  tabDisplayName: Function<U, @NlsContexts.TabTitle String>,
                                  factory: VcsLogUiFactory<out U>,
                                  focus: Boolean): U {
    val toolWindow = getToolWindowOrThrow(project)
    return openLogTab(logManager, factory, toolWindow, tabGroupId, tabDisplayName, focus)
  }

  internal fun <U : VcsLogUiEx> openLogTab(logManager: VcsLogManager,
                                           factory: VcsLogUiFactory<out U>,
                                           toolWindow: ToolWindow,
                                           tabGroupId: TabGroupId,
                                           tabDisplayName: Function<U, @NlsContexts.TabTitle String>,
                                           focus: Boolean): U {
    val logUi = logManager.createLogUi(factory, VcsLogTabLocation.TOOL_WINDOW)
    ContentUtilEx.addTabbedContent(toolWindow.contentManager, tabGroupId,
                                   TabDescriptor(VcsLogPanel(logManager, logUi), Supplier { tabDisplayName.apply(logUi) }, logUi), focus)
    if (focus) {
      toolWindow.activate(null)
    }
    return logUi
  }

  fun closeLogTab(manager: ContentManager, tabId: String): Boolean {
    return ContentUtilEx.closeContentTab(manager) { c: JComponent ->
      getLogUi(c)?.id == tabId
    }
  }

  @JvmStatic
  fun runInMainLog(project: Project, consumer: Consumer<in MainVcsLogUi>) {
    val window = getToolWindow(project)
    if (window == null || !selectMainLog(window.contentManager)) {
      showLogIsNotAvailableMessage(project)
      return
    }

    val runConsumer = Runnable { getVcsLogContentProvider(project)!!.executeOnMainUiCreated(consumer) }
    if (!window.isVisible) {
      window.activate(runConsumer)
    }
    else {
      runConsumer.run()
    }
  }

  @RequiresEdt
  fun showLogIsNotAvailableMessage(project: Project) {
    VcsNotifier.getInstance(project).notifyWarning(VcsLogNotificationIdsHolder.LOG_NOT_AVAILABLE, "",
                                                   VcsLogBundle.message("vcs.log.is.not.available"))
  }

  internal fun findMainLog(cm: ContentManager): Content? {
    // here tab name is used instead of log ui id to select the correct tab
    // it's done this way since main log ui may not be created when this method is called
    return cm.contents.find { VcsLogContentProvider.TAB_NAME == it.tabName }
  }

  private fun selectMainLog(cm: ContentManager): Boolean {
    val mainContent = findMainLog(cm) ?: return false
    cm.setSelectedContent(mainContent)
    return true
  }

  fun selectMainLog(project: Project): Boolean {
    val toolWindow = getToolWindow(project) ?: return false
    return selectMainLog(toolWindow.contentManager)
  }

  @JvmStatic
  fun updateLogUiName(project: Project, ui: VcsLogUi) {
    val toolWindow = getToolWindow(project) ?: return
    val manager = toolWindow.contentManager
    val component = ContentUtilEx.findContentComponent(manager) { c: JComponent -> ui === getLogUi(c) } ?: return
    ContentUtilEx.updateTabbedContentDisplayName(manager, component)
  }

  internal fun getToolWindowOrThrow(project: Project): ToolWindow {
    val toolWindow = getToolWindow(project)
    if (toolWindow != null) return toolWindow
    throw IllegalStateException("Could not find tool window for id ${ChangesViewContentManager.TOOLWINDOW_ID}")
  }

  internal fun getToolWindow(project: Project): ToolWindow? {
    return ToolWindowManager.getInstance(project).getToolWindow(ChangesViewContentManager.TOOLWINDOW_ID)
  }
}