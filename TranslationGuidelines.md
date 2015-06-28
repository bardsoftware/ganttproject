# Translation guidelines #
#### Preamble ####
GanttProject interface is translated to 20+ languages thanks to community efforts. If you volunteer to create a translation to your language, please read the text below.

### Short introduction ###
_If you know how localization works in Java and what resource bundle is, you may skip this section_

Strings which appear in GanttProject interface are stored in .properties files, one file per language. Such files are called _resource bundles_. A .properties file is a text file with key-value pairs. GP code uses keys to fetch values from the file matching the selected interface language.

### How to translate ###
In GanttProject 2.6 release cycle we're using [Crowdin](http://crowdin.net) - a web service for software localization. You don't need any software except a web browser (and probably GanttProject itself)

To edit a translation, you need to create an account on CrowdIn (they also have an option of signing in with Google and Facebook accounts). Then go to GanttProject page: http://crowdin.net/project/ganttproject find your favorite language, click it, choose "Gp 2.6 Brno" in the file list and start translating. You don't need any special permission or access rights, but I will appreciate if you [drop me a note](http://www.ganttproject.biz/about) before you start your work.


CrowdIn shows you a list of keys and their English values, which are grouped by translation state (untranslated go first and are marked with red bullets) and sorted alphabetically by key name inside each group. Your task is basically to scan through a list of untranslated keys and type value for your language in a _TRANSLATION_ box (don't forget to press _COMMIT TRANSLATION_). To do it properly, you need to know the context where a string is used. Very often you may guess the context from the key name (you see the name just above the translation text area), e.g. _baseline.dialog.hide_ means that it is used in a baseline dialog on a button which hides), but sometimes the context is not clear. Don't hesitate to ask about the context in the comments. There are already comments for some keys. Find the context in GanttProject UI and make sure that your translations will fit into the layout and will look consistent with the surrounding elements.

In some values you may find placeholders for parameters (e.g. _File {0} was modified since the last access_). You need to keep all placeholders, as they will be filled with values in the runtime. Don't hesitate to ask if their meaning looks unclear.

### How to test ###
You can download a resource bundle for any language. You will get a ZIP file with i18n.properties file inside. You need to copy it to the right place in your GanttProject installation. This place is`plugins/net.sourceforge.ganttproject/data/resources/language/`

There are many resource bundles in this directory and their names are standardized. They look like  `<prefix>_<language_code>[_<country_code>].properties`, where `<prefix>` is a resource bundle name (which is _i18n_ in GanttProject), and `<language_code>` and optional `<country_code>` identify your language and country for some languages. For instance, for German language the file name is `i18n_de.properties`, and for Traditional Chinese its name is `i18n_zh_TW.properties`. Replace the bundle for you language with `i18n.properties` which you just have downloaded.


### Editing resource bundles in offline editor ###
Values in the resource bundles are written is ASCII representation of Unicode symbols (like \u0000), so although you may use your favorite text editor to edit them, it is likely that you won't be able to read the values.  There is a number of special editors for working with resource bundle and we use this tool: https://prbeditor.dev.java.net/  Unfortunately there is no precompiled package on their page, so I built [my own](http://code.google.com/p/ganttproject/downloads/detail?name=prbeditor-0.9.7_2.zip).

Run it and open a resource bundle for your language from `plugins/net.sourceforge.ganttproject/data/resources/language/` directory of GanttProject installation. You'll see the keys and their translations in a table interface. If some translation is missing, you'll see the default English translation on its place. Edit the values, save your work an send us the changed file.

### Worth noticing ###

Some labels might be composed from more than one keys by just appending the corresponding values. It is wrong way in general and causes problems in some languages. However, we don't fix it proactively. If you find that something sounds absolutely wrong in your language, please let us know and we'll fix it.