package net.sourceforge.ganttproject.storage

/**
 * Storage for holding the current state of a Gantt project.
 */
interface ProjectStateStorage {
  /** Release the resources. */
  fun shutdown()
}