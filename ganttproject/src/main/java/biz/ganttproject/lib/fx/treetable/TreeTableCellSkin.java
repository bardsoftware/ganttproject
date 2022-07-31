/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package biz.ganttproject.lib.fx.treetable;

import com.google.common.base.MoreObjects;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.TreeTableCellBehavior;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default skin implementation for the {@link TreeTableCell} control.
 *
 * @param <S> The type of the UI control (e.g. the type of the 'row'), this is wrapped in a TreeItem.
 * @param <T> The type of the content in the cell, based on its {@link TreeTableColumn}.
 * @see TreeTableCell
 * @since 9
 */
public class TreeTableCellSkin<S,T> extends TableCellSkinBase<TreeItem<S>, T, TreeTableCell<S,T>> {

  /* *************************************************************************
   *                                                                         *
   * Private Fields                                                          *
   *                                                                         *
   **************************************************************************/

  private final BehaviorBase<TreeTableCell<S,T>> behavior;



  /* *************************************************************************
   *                                                                         *
   * Constructors                                                            *
   *                                                                         *
   **************************************************************************/

  /**
   * Creates a new TreeTableCellSkin instance, installing the necessary child
   * nodes into the Control {Control::getChildren() children} list, as
   * well as the necessary input mappings for handling key, mouse, etc events.
   *
   * @param control The control that this skin should be installed onto.
   */
  public TreeTableCellSkin(TreeTableCell<S,T> control, Runnable onProperties) {
    super(control);

    // install default input map for the TreeTableCell control
    // -- BEGIN OF GanttProject changes --
    behavior = new TreeTableCellBehavior<>(control) {
      private final AtomicBoolean isEditingStartExpected = new AtomicBoolean(false);
      private final ScheduledExecutorService myEditCellExecutor = Executors.newSingleThreadScheduledExecutor();
      private final Integer myDoubleClickInterval = (Integer) MoreObjects.firstNonNull(
          Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval"), 500
      );

      protected void handleClicks(MouseButton button, int clickCount, boolean isAlreadySelected) {
        if (button == MouseButton.PRIMARY) {
          if (clickCount == 1) {
            if (isAlreadySelected) {
              isEditingStartExpected.set(true);
              myEditCellExecutor.schedule(() -> {
                if (isEditingStartExpected.get()) {
                  Platform.runLater(() -> edit(getNode()));
                }
              }, myDoubleClickInterval, TimeUnit.MILLISECONDS);
              return;
            }
          } else if (clickCount == 2) {
            isEditingStartExpected.set(false);
            onProperties.run();
            return;
          }
        }
        super.handleClicks(button, clickCount, isAlreadySelected);
      }
    };
    // -- END OF GanttProject changes --
//        control.setInputMap(behavior.getInputMap());
  }



  /* *************************************************************************
   *                                                                         *
   * Public API                                                              *
   *                                                                         *
   **************************************************************************/

  /** {@inheritDoc} */
  @Override public void dispose() {
    super.dispose();

    if (behavior != null) {
      behavior.dispose();
    }
  }



  /* *************************************************************************
   *                                                                         *
   * Private implementation                                                  *
   *                                                                         *
   **************************************************************************/

  /** {@inheritDoc} */
  @Override public ReadOnlyObjectProperty<TreeTableColumn<S,T>> tableColumnProperty() {
    return getSkinnable().tableColumnProperty();
  }

  /** {@inheritDoc} */
  @Override double leftLabelPadding() {
    double leftPadding = super.leftLabelPadding();

    // RT-27167: we must take into account the disclosure node and the
    // indentation (which is not taken into account by the LabeledSkinBase.
    final double height = getCellSize();

    TreeTableCell<S,T> cell = getSkinnable();

    TreeTableColumn<S,T> tableColumn = cell.getTableColumn();
    if (tableColumn == null) return leftPadding;

    // check if this column is the TreeTableView treeColumn (i.e. the
    // column showing the disclosure node and graphic).
    TreeTableView<S> treeTable = cell.getTreeTableView();
    if (treeTable == null) return leftPadding;

    int columnIndex = treeTable.getVisibleLeafIndex(tableColumn);

    TreeTableColumn<S,?> treeColumn = treeTable.getTreeColumn();
    if ((treeColumn == null && columnIndex != 0) || (treeColumn != null && ! tableColumn.equals(treeColumn))) {
      return leftPadding;
    }

    TreeTableRow<S> treeTableRow = cell.getTableRow();
    if (treeTableRow == null) return leftPadding;

    TreeItem<S> treeItem = treeTableRow.getTreeItem();
    if (treeItem == null) return leftPadding;

    int nodeLevel = treeTable.getTreeItemLevel(treeItem);
    if (! treeTable.isShowRoot()) nodeLevel--;

    // -- BEGIN OF GanttProject changes --
    double indentPerLevel = 13.0;
    leftPadding += 10 + nodeLevel * indentPerLevel;
    // -- END OF GanttProject changes --

    // add in the width of the disclosure node, if one exists
    Map<TableColumnBase<?,?>, Double> mdwp = TableRowSkinBase.maxDisclosureWidthMap;
    leftPadding += mdwp.containsKey(treeColumn) ? mdwp.get(treeColumn) : 0;

    // adding in the width of the graphic on the tree item
    Node graphic = treeItem.getGraphic();
    leftPadding += graphic == null ? 0 : graphic.prefWidth(height);

    return leftPadding;
  }
}
