package biz.ganttproject.app

import biz.ganttproject.core.option.ObservableString
import javafx.scene.control.TextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

class PropertySheetTest {
  private val coroutineScope = CoroutineScope(SupervisorJob())

  @Test fun `basic string property`() {
    coroutineScope.launch {
      try {
        withContext(Dispatchers.JavaFx) {
          val propertyPaneBuilder = PropertyPaneBuilder(DummyLocalizer, PropertyPane())
          val textProperty = ObservableString("prop1", "Lorem Ipsum")
          val editor = propertyPaneBuilder.createStringOptionEditor(textProperty)
          (editor as? TextField)?.let {
            it.text = "Hello"
            assert(textProperty.value == "Hello")

            textProperty.value = "Hello2"
            assert(it.text == "Hello2")
          }
        }
      } catch (ex: Exception) {
        ex.printStackTrace()
        this.cancel()
      }
    }
  }

}
