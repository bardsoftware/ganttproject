package org.ganttproject.impex.htmlpdf;

import java.util.List;

public interface ITextStylesheet extends Stylesheet {
    List getFontFamilies();
    void setFontSubstitutionModel(FontSubstitutionModel mySubstitutionModel);
}
