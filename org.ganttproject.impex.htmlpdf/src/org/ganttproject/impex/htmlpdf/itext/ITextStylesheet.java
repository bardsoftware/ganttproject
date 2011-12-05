package org.ganttproject.impex.htmlpdf.itext;

import java.util.List;

import org.ganttproject.impex.htmlpdf.Stylesheet;

public interface ITextStylesheet extends Stylesheet {
    List<String> getFontFamilies();
    void setFontSubstitutionModel(FontSubstitutionModel mySubstitutionModel);
}
