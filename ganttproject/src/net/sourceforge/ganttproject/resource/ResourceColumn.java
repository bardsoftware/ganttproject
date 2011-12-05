package net.sourceforge.ganttproject.resource;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;

import org.eclipse.core.runtime.IStatus;
import org.jdesktop.swing.table.TableColumnExt;

/**@author nokiljevic
 *
 * Describes one column in the resources table. */
    public class ResourceColumn implements CustomPropertyDefinition, TableHeaderUIFacade.Column
    {
        /** Swing column component representing the column */
        TableColumnExt column;
        /** Datatype of the column */
        Class<?> type;
        /** Default value for the column */
        Object defaultVal;
        /** Visible on the screen */
        boolean visible;
        /** Default index in the table. When the column is shown
         *  that index will be forced. */
        int defaultIndex;
        private int myOrder;

        public ResourceColumn(TableColumnExt col, int index){
            this(col, index, String.class);
        }

        public ResourceColumn(TableColumnExt col, int index, Class<?> type){
            column = col;
            this.type = type;
            defaultIndex = index;
            visible = true;
        }

        public boolean nameCmp(String name) {
            System.out.println("comparing: "+name+" - "+column.getTitle()+" ");
            return column.getTitle().equals(name);
        }

        public void setTitle(String title) {
            column.setTitle(title);
        }

        public String getTitle() {
            return column.getTitle();
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void setVisible(boolean vis) {
            visible = vis;
        }

        public int getIndex() {
            return defaultIndex;
        }

        public TableColumnExt getColumn() {
            return column;
        }

        public Object getDefaultVal() {
            return defaultVal;
        }

        public void setDefaultVal(Object defaultVal) {
            this.defaultVal = defaultVal;
        }

        @Override
        public Class<?> getType() {
            return type;
        }

        public void setType(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object getDefaultValue() {
            return defaultVal;
        }

        @Override
        public String getDefaultValueAsString() {
            return HumanResourceManager.getValueAsString(defaultVal);
        }
        @Override
        public String getID() {
            return String.valueOf(defaultIndex);
        }

        @Override
        public String getName() {
            return getTitle();
        }

        @Override
        public String getTypeAsString() {
            return CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(type);
        }

        @Override
        public int getOrder() {
            return myOrder;
        }

        @Override
        public int getWidth() {
            return column.getWidth();
        }

        public void setWidth(int width) {
            column.setWidth(width);
        }

        @Override
        public void setOrder(int order) {
            myOrder = order;
        }

        @Override
        public IStatus canSetPropertyClass(CustomPropertyClass propertyClass) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CustomPropertyClass getPropertyClass() {
            return CustomPropertyClass.fromJavaClass(type);
        }

        @Override
        public void setDefaultValueAsString(String value) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setName(String name) {
            // TODO Auto-generated method stub
        }

        @Override
        public IStatus setPropertyClass(CustomPropertyClass propertyClass) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String toString() {
            return getTitle() + ", " + getTypeAsString();
        }


    }