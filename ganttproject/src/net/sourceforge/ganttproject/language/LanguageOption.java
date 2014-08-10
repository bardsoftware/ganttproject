package net.sourceforge.ganttproject.language;

import java.util.Locale;

import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import biz.ganttproject.core.option.DefaultEnumerationOption;

public abstract class LanguageOption extends DefaultEnumerationOption<Locale> implements GP1XOptionConverter {
  public LanguageOption() {
    this("language", GanttLanguage.getInstance().getAvailableLocales().toArray(new Locale[0]));
  }

  private LanguageOption(String id, Locale[] locales) {
    super(id, locales);
  }

  @Override
  protected String objectToString(Locale locale) {
    return GanttLanguage.getInstance().formatLanguageAndCountry(locale);
  }

  @Override
  public void commit() {
    super.commit();
    applyLocale(stringToObject(getValue()));
  }

  protected abstract void applyLocale(Locale locale);

  @Override
  public String getTagName() {
    return "language";
  }

  @Override
  public String getAttributeName() {
    return "selection";
  }

  @Override
  public void loadValue(String legacyValue) {
    loadPersistentValue(legacyValue);
  }

  @Override
  public String getPersistentValue() {
    Locale l = stringToObject(getValue());
    if (l == null) {
      l = GanttLanguage.getInstance().getLocale();
    }
    assert l != null;
    String result = l.getLanguage();
    if (!l.getCountry().isEmpty()) {
      result += "_" + l.getCountry();
    }
    return result;
  }

  @Override
  public void loadPersistentValue(String value) {
    String[] lang_country = value.split("_");
    Locale l;
    if (lang_country.length == 2) {
      l = new Locale(lang_country[0], lang_country[1]);
    } else {
      l = new Locale(lang_country[0]);
    }
    value = objectToString(l);
    if (value != null) {
      resetValue(value, true);
      applyLocale(l);
    }
  }
}