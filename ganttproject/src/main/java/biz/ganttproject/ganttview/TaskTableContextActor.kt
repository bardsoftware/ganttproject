/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.ganttproject.ganttview

import biz.ganttproject.ganttview.NewTaskState.*
import com.google.common.collect.Queues
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.TreeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import java.util.concurrent.Executors

/**
 * A closed list of the incoming events.
 */
sealed class NewTaskMsg<T>

/** New task has been created. It is expected that soon we'll need to start editing its name,
 * but not until we get receive a TreeItemReady event */
data class TaskReady<T>(val task: T) : NewTaskMsg<T>()

/**
 * This event indicates that a new tree item has been inserted into the tree. If we have been waiting for this event,
 * we'll send StartScrolling command.
 */
data class TreeItemReady<T>(val treeItem: TreeItem<T>) : NewTaskMsg<T>()

/**
 * This event indicates that a tree item has been scrolled to the visible area
 * and we can now start editing.
 */
data class TreeItemScrolled<T>(val treeItem: TreeItem<T>): NewTaskMsg<T>()

/**
 * This event indicates that editing in the tree has been completed. We can now transition to other states, e.g.
 * start editing some other task.
 */
data class EditingCompleted<T>(val task: T) : NewTaskMsg<T>()

/**
 * A closed list of  commands.
 */
sealed class NewTaskActorCommand<T>

/**
 * This command is sent when we sequentially transition from IDLE => TASK_READY => TREE_ITEM_READY.
 */
data class StartEditing<T>(val treeItem: TreeItem<T>) : NewTaskActorCommand<T>()

/**
 * This command is sent when we receive TaskReady event in editing state. This may happen if user holds a key which
 * creates new tasks, thus creating many tasks in a row. We can start editing one of them, then receive a new task event
 * and switch to editing that new task.
 */
data class CommitEditing<T>(val treeItem: TreeItem<T>) : NewTaskActorCommand<T>()

/**
 * This command is sent when we receive TreeItemReady event in TASK_READY_STATE. It means that we
 * created a tree item and now we probably need to scroll the table to make it visible.
 */
data class StartScrolling<T>(val treeItem: TreeItem<T>) : NewTaskActorCommand<T>()


enum class NewTaskState { IDLE, TASK_READY, TREE_ITEM_READY, SCROLLING, EDIT_STARTING, EDIT_COMPLETING }

/**
 * Task table orchestrator synchronizes various events which happen to the task table, such as creation of a new task,
 * start or complete some cell editing.
 *
 * Internally it maintains a small state machine which transitions from one state to another when new events come from
 * the inbox channel. Some transitions may issue commands to be sent into the command channel.
 */
class NewTaskActor<T> {
  val inboxChannel = Channel<NewTaskMsg<T>>()
  val commandChannel = Channel<NewTaskActorCommand<T>>()
  private val inboxQueue = Queues.newConcurrentLinkedQueue<NewTaskMsg<T>>()

  private var state: NewTaskState = IDLE
  set(value) {
    LOG.debug("State $field => $value")
    field = value
    canAddTask.value = value == IDLE
  }
  val canAddTask = SimpleBooleanProperty(true)

  private var newTask: T? = null
  private var newTreeItem: TreeItem<T>? = null

  fun start() = ourCoroutineScope.launch {
    for (msg in inboxChannel) {
      processMessage(msg)
    }
  }

  private suspend fun processQueue() {
    do {
      val msg = inboxQueue.poll() ?: break
      processMessage(msg)
    } while (true)
  }

  private suspend fun processMessage(msg: NewTaskMsg<T>) {
    LOG.debug("Incoming message: {} current state: {}", msg, state)
    when (msg) {
      is TaskReady -> {
        when (state) {
          EDIT_COMPLETING -> {
            // We are about to finish with the previous task, and once we're done, we can proceed with this one
            inboxQueue.add(msg)
          }
          IDLE, TASK_READY, TREE_ITEM_READY, SCROLLING -> {
            // We're not doing any user interaction, so we are switching to this new task
            newTask = msg.task
            state = TASK_READY
          }
          EDIT_STARTING -> {
            // We are editing now, and we'll commit our edits and process the new task after that
            inboxQueue.add(msg)
            commandChannel.send(CommitEditing(newTreeItem!!))
            state = EDIT_COMPLETING
          }
        }
      }
      is TreeItemReady -> {
        when (state) {
          IDLE -> error("this must be error: we received a tree item prior to new task")
          TASK_READY -> {
            if (msg.treeItem.value === newTask) {
              // It is a tree item created for the latest task. Now let's make it visible in the tree.
              state = SCROLLING
              commandChannel.send(StartScrolling(msg.treeItem))
            } else {
              // This might be a tree item created for the previous task, if user hits INSERT quick enough
              // We can just ignore this message.
            }
          }
          TREE_ITEM_READY, SCROLLING, EDIT_STARTING -> {
            error("this must be error: we received a new tree item when we are editing or about to start editing")
          }
          EDIT_COMPLETING -> {
            // We created a new tree item while we were finishing editing the previous one. That's okay, we'll process
            // this message later
            inboxQueue.add(msg)
          }
        }
      }
      is TreeItemScrolled -> {
        when (state) {
          IDLE, TASK_READY, TREE_ITEM_READY, EDIT_STARTING -> {}
          SCROLLING -> {
            if (msg.treeItem.value === newTask) {
              // So we scrolled to the tree item with the task we just've created. Now let's start editing it.
              newTreeItem = msg.treeItem
              state = EDIT_STARTING
              commandChannel.send(StartEditing(msg.treeItem))
            } else {
              // This might be a tree item created for the previous task, if user hits INSERT quick enough
              // We can just ignore this message.
            }
          }
          EDIT_COMPLETING -> {
            // We created a new tree item while we were finishing editing the previous one. That's okay, we'll process
            // this message later
            inboxQueue.add(msg)
          }
        }
      }
      is EditingCompleted -> {
        when (state) {
          EDIT_STARTING -> {
            if (msg.task == newTask) {
              newTask = null
              newTreeItem = null
              state = IDLE
              processQueue()
            } else {
              // No action, because we completed editing of some other task, not the one
              // which we're editing now.
              // It could happen when we sent null task with editing completed. Now we don't
              // do it so I doubt that we may ever fall into this branch.
            }
          }
          EDIT_COMPLETING -> {
            newTask = null
            newTreeItem = null
            state = IDLE
            processQueue()
          }
          IDLE, TASK_READY-> {}
          else -> error("this must be error: editing completed when state is $state")
        }
      }
    }
  }
}

private val ourCoroutineScope = CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher())
private val LOG = GPLogger.create("TaskTable.Orchestrator")