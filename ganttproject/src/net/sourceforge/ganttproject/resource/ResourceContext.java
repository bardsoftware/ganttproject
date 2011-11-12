package net.sourceforge.ganttproject.resource;

/**
 * This interface represents a kind of 'selection' in the application It is
 * designed for collaboration between actions which do something with selected
 * resources and UI components which implement UI specifics of selection
 * management (e.g. listen mouse events); This interface may be implemented,
 * e.g., by tables, trees and other UI components which allow to select
 * something
 *
 * @author dbarashev
 */
public interface ResourceContext {
    /** @return Resources selected at the moment */
    public HumanResource[] getResources();
}
