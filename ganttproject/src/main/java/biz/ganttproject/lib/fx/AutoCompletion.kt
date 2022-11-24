/**
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Change History in GanttProject:
 * 2022-11-22 (dbarashev):
 * - translated to Kotlin and removed a dedicated thread for fetching completions
 *
 */
package biz.ganttproject.lib.fx

import biz.ganttproject.core.option.Completion
import biz.ganttproject.lib.fx.autocomplete.AutoCompletePopup
import biz.ganttproject.lib.fx.autocomplete.AutoCompletePopupSkin
import com.sun.javafx.event.EventHandlerManager
import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ObjectPropertyBase
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.event.*
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.util.Callback
import javafx.util.StringConverter
import java.util.*

/**
 * Represents a binding between a text field and a auto-completion popup
 *
 * @param <T>
 */
class AutoCompletionTextFieldBinding<T: Completion>(
  textField: TextField,
  suggestionProvider: Callback<ISuggestionRequest, Collection<T>>,
  converter: (T)->String = {it.toString()}
) : AutoCompletionBinding<T>(textField, suggestionProvider, converter) {
  /***************************************************************************
   * *
   * Private fields                                                          *
   * *
   */
  /***************************************************************************
   * *
   * Public API                                                              *
   * *
   */
  /** {@inheritDoc}  */
  override fun getCompletionTarget(): TextField {
    return super.getCompletionTarget() as TextField
  }

  /** {@inheritDoc}  */
  override fun dispose() {
    completionTarget.textProperty().removeListener(textChangeListener)
    completionTarget.focusedProperty().removeListener(focusChangedListener)
  }

  /** {@inheritDoc}  */
  override fun completeUserInput(completion: T) {
    val newText = completion.text
    completionTarget.replaceText(completion.posStart, completion.posEnd, completion.text)
    completionTarget.positionCaret(completion.posStart + newText.length)
  }

  /***************************************************************************
   * *
   * Event Listeners                                                         *
   * *
   */
  private val textChangeListener: ChangeListener<String> =
    ChangeListener<String> { obs, oldText, newText ->
      if (completionTarget.isFocused) {
        setUserInput(newText)
      }
    }
  private val focusChangedListener: ChangeListener<Boolean> =
    ChangeListener<Boolean> { obs, oldFocused, newFocused -> if (newFocused == false) hidePopup() }
  /**
   * Creates a new auto-completion binding between the given textField
   * and the given suggestion provider.
   *
   * @param textField
   * @param suggestionProvider
   */
  /***************************************************************************
   * *
   * Constructors                                                            *
   * *
   */
  /**
   * Creates a new auto-completion binding between the given textField
   * and the given suggestion provider.
   *
   * @param textField
   * @param suggestionProvider
   */
  init {
    completionTarget.textProperty().addListener(textChangeListener)
    completionTarget.focusedProperty().addListener(focusChangedListener)
  }

  companion object {
    /***************************************************************************
     * *
     * Static properties and methods                                           *
     * *
     */
    private fun <T> defaultStringConverter(): StringConverter<T> {
      return object : StringConverter<T>() {
        override fun toString(t: T): String {
          return t.toString()
        }

        override fun fromString(string: String): T {
          return string as T
        }
      }
    }
  }
}


/**
 * The AutoCompletionBinding is the abstract base class of all auto-completion bindings.
 * This class is the core logic for the auto-completion feature but highly customizable.
 *
 *
 * To use the autocompletion functionality, refer to the [TextFields] class.
 *
 * The popup size can be modified through its [.setVisibleRowCount]
 * for the height and all the usual methods for the width.
 *
 * @param <T> Model-Type of the suggestions
 * @see TextFields
</T> */
abstract class AutoCompletionBinding<T> protected constructor(
  /***************************************************************************
   * *
   * Private fields                                                          *
   * *
   */
  internal val completionTarget: TextInputControl,
  private val suggestionProvider: Callback<ISuggestionRequest, Collection<T>>?,
  converter: (T)->String
) : EventTarget {
  /**
   * Get the [AutoCompletePopup] used by this binding. Note that this gives access to the
   * internal API and should be used with great care (and in the expectation that things may break in
   * the future). All relevant methods of the popup are already exposed in this class.
   *
   *
   * The only reason this is exposed is to allow custom skins for the popup.
   *
   * @return the [AutoCompletePopup] used by this binding
   */
  val autoCompletionPopup: AutoCompletePopup<T> = AutoCompletePopup()
  private val suggestionsTaskLock = Any()
  private var suggestionsTask: FetchSuggestionsTask? = null
  /**
   * Shall changes to the user input be ignored?
   * @return
   */
  /**
   * If IgnoreInputChanges is set to true, all changes to the user input are
   * ignored. This is primary used to avoid self triggering while
   * auto completing.
   * @param state
   */
  private var isIgnoreInputChanges = false
  private var delay: Long = 250
  /***************************************************************************
   * *
   * Public API                                                              *
   * *
   */
  /**
   * Specifies whether the PopupWindow should be hidden when an unhandled
   * escape key is pressed while the popup has focus.
   *
   * @param value
   */
  fun setHideOnEscape(value: Boolean) {
    autoCompletionPopup.isHideOnEscape = value
  }

  /**
   * Set the current text the user has entered
   * @param userText
   */
  fun setUserInput(userText: String) {
    if (!isIgnoreInputChanges) {
      onUserInputChanged(userText)
    }
  }

  /**
   * Sets the delay in ms between a key press and the suggestion popup being displayed.
   *
   * @param delay
   */
  fun setDelay(delay: Long) {
    this.delay = delay
  }

  /**
   * Gets the target node for auto completion
   * @return the target node for auto completion
   */
  open fun getCompletionTarget(): Node? {
    return completionTarget
  }

  /**
   * Disposes the binding.
   */
  abstract fun dispose()
  /**
   * Return the maximum number of rows to be visible in the popup when it is
   * showing.
   *
   * @return the maximum number of rows to be visible in the popup when it is
   * showing.
   */
  /**
   * Set the maximum number of rows to be visible in the popup when it is
   * showing.
   *
   * @param value
   */
  var visibleRowCount: Int
    get() = autoCompletionPopup.visibleRowCount
    set(value) {
      autoCompletionPopup.visibleRowCount = value
    }

  /**
   * Return an property representing the maximum number of rows to be visible
   * in the popup when it is showing.
   *
   * @return an property representing the maximum number of rows to be visible
   * in the popup when it is showing.
   */
  fun visibleRowCountProperty(): IntegerProperty {
    return autoCompletionPopup.visibleRowCountProperty()
  }
  /**
   * Return the pref width of the popup.
   *
   * @return the pref width of the popup.
   */
  /**
   * Sets the prefWidth of the popup.
   *
   * @param value
   */
  var prefWidth: Double
    get() = autoCompletionPopup.prefWidth
    set(value) {
      autoCompletionPopup.prefWidth = value
    }

  /**
   * Return the property associated with the pref width.
   * @return
   */
  fun prefWidthProperty(): DoubleProperty {
    return autoCompletionPopup.prefWidthProperty()
  }
  /**
   * Return the min width of the popup.
   *
   * @return the min width of the popup.
   */
  /**
   * Sets the minWidth of the popup.
   *
   * @param value
   */
  var minWidth: Double
    get() = autoCompletionPopup.minWidth
    set(value) {
      autoCompletionPopup.minWidth = value
    }

  /**
   * Return the property associated with the min width.
   * @return
   */
  fun minWidthProperty(): DoubleProperty {
    return autoCompletionPopup.minWidthProperty()
  }
  /**
   * Return the max width of the popup.
   *
   * @return the max width of the popup.
   */
  /**
   * Sets the maxWidth of the popup.
   *
   * @param value
   */
  var maxWidth: Double
    get() = autoCompletionPopup.maxWidth
    set(value) {
      autoCompletionPopup.maxWidth = value
    }

  /**
   * Return the property associated with the max width.
   * @return
   */
  fun maxWidthProperty(): DoubleProperty {
    return autoCompletionPopup.maxWidthProperty()
  }
  /***************************************************************************
   * *
   * Protected methods                                                       *
   * *
   */
  /**
   * Complete the current user-input with the provided completion.
   * Sub-classes have to provide a concrete implementation.
   * @param completion
   */
  protected abstract fun completeUserInput(completion: T)

  /**
   * Show the auto completion popup
   */
  protected fun showPopup() {
    autoCompletionPopup.show(completionTarget)
    selectFirstSuggestion(autoCompletionPopup)
  }

  /**
   * Hide the auto completion targets
   */
  protected fun hidePopup() {
    autoCompletionPopup.hide()
  }

  protected fun fireAutoCompletion(completion: T) {
    Event.fireEvent(this, AutoCompletionEvent(completion))
  }
  /***************************************************************************
   * *
   * Private methods                                                         *
   * *
   */
  /**
   * Selects the first suggestion (if any), so the user can choose it
   * by pressing enter immediately.
   */
  private fun selectFirstSuggestion(autoCompletionPopup: AutoCompletePopup<*>) {
    val skin = autoCompletionPopup.skin
    if (skin is AutoCompletePopupSkin<*>) {
      val li = skin.node as ListView<*>
      if (li.items != null && !li.items.isEmpty()) {
        li.selectionModel.select(0)
      }
    }
  }

  /**
   * Occurs when the user text has changed and the suggestions require an update
   * @param userText
   */
  private fun onUserInputChanged(userText: String) {
    fetchSuggestions(userText, 100, {false})
//    synchronized(suggestionsTaskLock) {
//      if (suggestionsTask != null && suggestionsTask!!.isRunning) {
//        // cancel the current running task
//        suggestionsTask!!.cancel()
//      }
//      // create a new fetcher task
//      suggestionsTask = FetchSuggestionsTask(userText, delay)
//      Thread(suggestionsTask).start()
//    }
  }
  /***************************************************************************
   * *
   * Inner classes and interfaces                                            *
   * *
   */
  /**
   * Represents a suggestion fetch request
   *
   */
  data class ISuggestionRequest(val userText: String)


  private fun fetchSuggestions(userText: String, delay: Long, isCancelled:()->Boolean) {
    val provider = suggestionProvider
    if (provider != null) {
//      val startTime = System.currentTimeMillis()
//      val sleepTime = startTime + delay - System.currentTimeMillis()
//      if (sleepTime > 0 && !isCancelled()) {
//        Thread.sleep(sleepTime)
//      }
      if (!isCancelled()) {
        val fetchedSuggestions = provider.call(ISuggestionRequest(userText))
        Platform.runLater {

          // check whether completionTarget is still valid
          val validNode = (completionTarget.scene != null
            && completionTarget.scene.window != null)
          if (fetchedSuggestions != null && !fetchedSuggestions.isEmpty() && validNode) {
            autoCompletionPopup.suggestions.setAll(fetchedSuggestions)
            showPopup()
          } else {
            // No suggestions found, so hide the popup
            hidePopup()
          }
        }
      }
    } else {
      // No suggestion provider
      hidePopup()
    }
  }
  /**
   * This task is responsible to fetch suggestions asynchronous
   * by using the current defined suggestionProvider
   *
   */
  private inner class FetchSuggestionsTask(private val userText: String, private val delay: Long) : Task<Void?>() {
    @Throws(Exception::class)
    override fun call(): Void? {
      fetchSuggestions(userText, delay, this::isCancelled)
      return null
    }
  }
  /***************************************************************************
   * *
   * Events                                                                  *
   * *
   */
  // --- AutoCompletionEvent
  /**
   * Represents an Event which is fired after an auto completion.
   */
  class AutoCompletionEvent<TE>
  /**
   * Creates a new event that can subsequently be fired.
   */(
    /**
     * Returns the chosen completion.
     */
    val completion: TE
  ) : Event(AUTO_COMPLETED) {

    companion object {
      /**
       * The event type that should be listened to by people interested in
       * knowing when an auto completion has been performed.
       */
      val AUTO_COMPLETED =
        EventType<AutoCompletionEvent<*>>("AUTO_COMPLETED" + UUID.randomUUID().toString()) //$NON-NLS-1$
    }
  }

  private val onAutoCompleted: ObjectProperty<EventHandler<AutoCompletionEvent<T>>> by lazy {
    object : ObjectPropertyBase<EventHandler<AutoCompletionEvent<T>>>() {
      override fun invalidated() {
        eventHandlerManager.setEventHandler(
          AutoCompletionEvent.AUTO_COMPLETED,
          get() as EventHandler<AutoCompletionEvent<*>?>?
        )
      }

      override fun getBean(): Any {
        return this@AutoCompletionBinding
      }

      override fun getName(): String {
        return "onAutoCompleted" //$NON-NLS-1$
      }
    }
  }

  /**
   * Set a event handler which is invoked after an auto completion.
   * @param value
   */
  fun setOnAutoCompleted(value: EventHandler<AutoCompletionEvent<T>>) {
    onAutoCompletedProperty().set(value)
  }

  fun getOnAutoCompleted(): EventHandler<AutoCompletionEvent<T>>? {
    return if (onAutoCompleted == null) null else onAutoCompleted!!.get()
  }

  fun onAutoCompletedProperty(): ObjectProperty<EventHandler<AutoCompletionEvent<T>>> {
    return onAutoCompleted
  }

  /***************************************************************************
   * *
   * EventTarget Implementation                                              *
   * *
   */
  val eventHandlerManager = EventHandlerManager(this)
  /***************************************************************************
   * *
   * Constructors                                                            *
   * *
   */
  /**
   * Creates a new AutoCompletionBinding
   *
   * @param completionTarget The target node to which auto-completion shall be added
   * @param suggestionProvider The strategy to retrieve suggestions
   * @param converter The converter to be used to convert suggestions to strings
   */
  init {
    autoCompletionPopup.converter = object: StringConverter<T>() {
      override fun toString(completion: T): String = converter(completion)

      override fun fromString(string: String?): T {
        TODO("Not yet implemented")
      }

    }
    autoCompletionPopup.onSuggestion = EventHandler { sce: AutoCompletePopup.SuggestionEvent<T> ->
      try {
        isIgnoreInputChanges = true
        completeUserInput(sce.suggestion)
        fireAutoCompletion(sce.suggestion)
        hidePopup()
      } finally {
        // Ensure that ignore is always set back to false
        isIgnoreInputChanges = false
      }
    }
  }

  /**
   * Registers an event handler to this EventTarget. The handler is called when the
   * menu item receives an `Event` of the specified type during the bubbling
   * phase of event delivery.
   *
   * @param <E> the specific event class of the handler
   * @param eventType the type of the events to receive by the handler
   * @param eventHandler the handler to register
   * @throws NullPointerException if the event type or handler is null
  </E> */
  fun <E : Event?> addEventHandler(eventType: EventType<E>?, eventHandler: EventHandler<E>?) {
    eventHandlerManager.addEventHandler(eventType, eventHandler)
  }

  /**
   * Unregisters a previously registered event handler from this EventTarget. One
   * handler might have been registered for different event types, so the
   * caller needs to specify the particular event type from which to
   * unregister the handler.
   *
   * @param <E> the specific event class of the handler
   * @param eventType the event type from which to unregister
   * @param eventHandler the handler to unregister
   * @throws NullPointerException if the event type or handler is null
  </E> */
  fun <E : Event?> removeEventHandler(eventType: EventType<E>?, eventHandler: EventHandler<E>?) {
    eventHandlerManager.removeEventHandler(eventType, eventHandler)
  }

  /** {@inheritDoc}  */
  override fun buildEventDispatchChain(tail: EventDispatchChain): EventDispatchChain {
    return tail.prepend(eventHandlerManager)
  }
}
