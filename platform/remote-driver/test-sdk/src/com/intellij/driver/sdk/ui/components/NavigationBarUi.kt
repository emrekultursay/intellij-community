package com.intellij.driver.sdk.ui.components

import com.intellij.driver.client.Remote
import com.intellij.driver.sdk.ui.QueryBuilder

val IdeaFrameUI.navigationBar get() = x(NavigationBarUi::class.java) { NavigationBarUi.locator }

class NavigationBarUi(data: ComponentData): UiComponent(data) {

  companion object {
    val locator = QueryBuilder().byClass("NewNavBarPanel")
  }

  val currentPath: List<String>
    get() = findAllText().sortedBy { it.point.x }.map { it.text }

  fun navBarItem(item: String): NavBarItemUi =
    x(NavBarItemUi::class.java) { and(byClass("NavBarItemComponent"), byAccessibleName(item)) }

  class NavBarItemUi(data: ComponentData): UiComponent(data) {
    val isSelected get() = driver.new(MethodInvocator::class, component.getClass(), "isSelected", null).invoke(component) as Boolean
  }

  @Remote("com.intellij.util.MethodInvocator")
  interface MethodInvocator {
    fun invoke(obj: Any, vararg args: Any): Any
  }
}