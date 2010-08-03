package org.ganttproject.impex.htmlpdf;

import java.util.List;

import com.lowagie.text.Font;

public interface ITextStylesheet extends Stylesheet {
    List getFontFamilies();
    void setFontSubstitutionModel(FontSubstitutionModel mySubstitutionModel);
}
