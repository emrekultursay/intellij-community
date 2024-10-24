// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui.popup.list

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.actionSystem.impl.ActionMenu
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.popup.ActionPopupStep
import com.intellij.ui.popup.PopupFactoryImpl.ActionItem
import com.intellij.ui.popup.list.ListPopupImpl.ListWithInlineButtons
import java.awt.Point
import java.awt.event.InputEvent
import javax.swing.JComponent
import javax.swing.JList

class PopupInlineActionsSupportImpl(private val myListPopup: ListPopupImpl) : PopupInlineActionsSupport {

  private val myStep = myListPopup.listStep as ActionPopupStep

  override fun hasExtraButtons(element: Any?): Boolean = calcExtraButtonsCount(element) > 0

  override fun calcExtraButtonsCount(element: Any?): Int {
    if (!ExperimentalUI.isNewUI() || element !is ActionItem) return 0

    var res = 0
    res += myStep.getInlineItems(element).size
    if (hasMoreButton(element)) res++
    return res
  }

  override fun calcButtonIndex(element: Any?, point: Point): Int? {
    if (element == null) return null
    val buttonsCount: Int = calcExtraButtonsCount(element)
    if (buttonsCount <= 0) return null

    return calcButtonIndex(myListPopup.list, buttonsCount, point)
  }

  override fun getInlineAction(element: Any?, index: Int, event: InputEvent?) : InlineActionDescriptor =
    getExtraButtonsActions(element, event)[index]

  private fun getExtraButtonsActions(element: Any?, event: InputEvent?): List<InlineActionDescriptor> {
    if (!ExperimentalUI.isNewUI() || element !is ActionItem) return emptyList()

    val res: MutableList<InlineActionDescriptor> = mutableListOf()

    myStep.getInlineItems(element).forEach { item ->
      res.add(InlineActionDescriptor(
        createInlineActionRunnable(item, event), item.keepPopupOnPerform))
    }
    if (hasMoreButton(element)) {
      res.add(InlineActionDescriptor(
        createNextStepRunnable(element), KeepPopupOnPerform.Always))
    }
    return res
  }

  override fun getExtraButtons(list: JList<*>, value: Any?, isSelected: Boolean): List<JComponent> {
    if (value !is ActionItem) return emptyList()
    val inlineItems = myStep.getInlineItems(value)

    val res: MutableList<JComponent> = java.util.ArrayList()
    val activeIndex = if (isSelected) getActiveButtonIndex(list) else -1

    for (i in 0 until inlineItems.size) {
      val item = inlineItems[i]
      if (isSelected || item.getClientProperty(ActionMenu.ALWAYS_VISIBLE) == true) {
        res.add(createActionButton(item, i == activeIndex, isSelected))
      }
    }
    if (hasMoreButton(value) && isSelected) res.add(createSubmenuButton(value, res.size == activeIndex))

    return res
  }

  override fun getActiveButtonIndex(list: JList<*>): Int? = (list as? ListWithInlineButtons)?.selectedButtonIndex

  private fun createSubmenuButton(value: ActionItem, active: Boolean): JComponent {
    val icon = if (myStep.isFinal(value)) AllIcons.Actions.More else AllIcons.Icons.Ide.MenuArrow
    return createExtraButton(icon, active)
  }

  private fun createActionButton(item: ActionItem, active: Boolean, isSelected: Boolean): JComponent {
    val icon = item.getIcon(isSelected)
    if (icon == null) {
      throw AssertionError("null inline item icon for action '${item.action.javaClass.name}'")
    }
    return createExtraButton(icon, active)
  }

  override fun getActiveExtraButtonToolTipText(list: JList<*>, value: Any?): String? {
    if (value !is ActionItem) return null
    val inlineActions = myStep.getInlineItems(value)
    val activeButton = getActiveButtonIndex(list) ?: return null
    return if (activeButton == inlineActions.size)
      IdeBundle.message("inline.actions.more.actions.text")
    else
      inlineActions.getOrNull(activeButton)?.text
  }

  override fun isMoreButton(element: Any?, index: Int): Boolean {
    val count = calcExtraButtonsCount(element)
    return count > 0 && index == count - 1
  }

  private fun createNextStepRunnable(element: ActionItem) = Runnable { myListPopup.showNextStepPopup(myStep.onChosen(element, false), element) }

  private fun createInlineActionRunnable(item: ActionItem, inputEvent: InputEvent?) = Runnable {
    myStep.performActionItem(item, inputEvent)
    myStep.updateStepItems(myListPopup.list)
  }

  private fun hasMoreButton(element: ActionItem) = myStep.hasSubstep(element)
                                                                    && !myListPopup.isShowSubmenuOnHover
                                                                    && myStep.isFinal(element)
}