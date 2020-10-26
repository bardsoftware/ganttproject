/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Toolkit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFormattedTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * <p>
 * Title: GTextField
 * </p>
 * <p>
 * Description: This class is a text field enabling accurate character
 * authorization. You can specify the valid characters you want authorize in
 * edition or a regular expression pattern.
 * </p>
 * 
 * @author Benoit Baranne
 */
public class GTextField extends JFormattedTextField {
  public static final String PATTERN_DOUBLE = "[0-9]*\\.?[0-9]*";

  public static final String PATTERN_INTEGER = "[0-9]*";

  /**
   * Creates an instance of GTextField with a default format.
   * 
   * @param format
   *          Textfields format.
   */
  public GTextField(AbstractFormatter format) {
    super(format);
    // this.setDocument(new CPlainDocument(null));
    this.setHorizontalAlignment(SwingConstants.RIGHT);
  }

  /**
   * Creates an instance of GTextField with a default text.
   * 
   * @param text
   *          Text to be put into the text field.
   */
  public GTextField(String text) {
    super();
    this.setDocument(new GPlainDocument(null));
    this.setText(text);
    this.setHorizontalAlignment(SwingConstants.RIGHT);
  }

  /**
   * Creates an instance of GTextField.
   */
  public GTextField() {
    super();
    super.setColumns(15);
    this.setDocument(new GPlainDocument(null));
    this.setHorizontalAlignment(SwingConstants.RIGHT);
  }

  /**
   * Give the focus to the text field.
   */
  public void setFocus() {
    super.requestFocusInWindow();
  }

  /**
   * Indicates the valid characters to accept in edition.
   * 
   * @param valid_chars
   *          String containing all the valid characters.
   */
  public void setValidCharacters(String valid_chars) {
    Document d = this.getDocument();
    if (d instanceof GPlainDocument) {
      ((GPlainDocument) d).setValidCharacters(valid_chars);
    }
  }

  /**
   * Indicates the valid characters by giving an regular expression pattern.
   * 
   * @param pattern
   *          The regular expression with which the matching is done.
   */
  public void setPattern(String pattern) {
    Document d = this.getDocument();
    if (d instanceof GPlainDocument) {
      ((GPlainDocument) d).setPattern(pattern);
    }
  }
}

/**
 * <p>
 * Title: CPlainDocument
 * </p>
 * <p>
 * Description: This class enables you to specify the valid characters for the
 * document. If the pattern is set (i.e. not null) the valid characters refer to
 * it, otherwise it refers to the valid characters string.
 * </p>
 * 
 * @author Benoit Baranne
 */
class GPlainDocument extends PlainDocument {
  /**
   * String containing all the valid characters.
   */
  private String valid_characters = null;

  /**
   * Regular expression pattern to use.
   */
  private Pattern pattern = null;

  /**
   * Creates an instance of GPlainDocument with a valid character string.
   * 
   * @param validchars
   */
  public GPlainDocument(String validchars) {
    super();
    this.valid_characters = validchars;
  }

  /**
   * Inserts the string <code>str</code> into the document if it matches with
   * the valid characters.
   * 
   * @param offs
   *          Offset for the insertion.
   * @param str
   *          String to insert.
   * @param a
   *          AttibutSet.
   * @throws BadLocationException
   */
  @Override
  public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
    if (this.pattern != null) {
      String s = this.getText(0, this.getLength());
      Matcher m = this.pattern.matcher(s + str);

      if (m.matches())
        super.insertString(offs, str, a);
    } else {

      // //
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < str.length(); i++)
        if (this.valid_characters == null || this.valid_characters.indexOf(str.charAt(i)) != -1)
          sb.append(str.charAt(i));
        else
          Toolkit.getDefaultToolkit().beep();

      super.insertString(offs, sb.toString(), a);
    }
  }

  /**
   * Sets the valid characters.
   * 
   * @param valid_chars
   *          The valid characters to set.
   */
  protected void setValidCharacters(String valid_chars) {
    this.valid_characters = valid_chars;
  }

  /**
   * Sets the regular expression pattern.
   * 
   * @param pattern
   *          The regular expression pattern to set.
   */
  protected void setPattern(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }
}
