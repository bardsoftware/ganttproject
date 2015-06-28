Good reports have more chances to be fixed than bad ones. We understand that you are not a professional software developer or quality control engineer, but still we encourage you to use some techniques of writing a good report:

  * **Summary** of your report should be brief but as precise as possible. It should summarize the problem in few words yet providing a lot of information.
    * Summary  _"Bugs"_ is not informative because we understand that bug reports are usually about bugs, not about money donations.
    * Summary _"Problem with version 2.0"_ is also not informative because we know that you're writing a bug report because you have some problems and we assume that you're probably using the latest version of our product (and also a special field in a bug report form which indicates the version where problem was found)
    * Summary _"Chart is not repainted on closing task properties dialog"_ is perfect!

  * **Description** should provide more or less exact scenario of reproducing your problem, especially if reproducing is not straightforward. It should also describe an expected and actual results. A good description looks similar to the following:

  1. Create 5 new tasks: task1, task2,..., task5
  1. Indent task2 and task 3 into task1 and indent task5 into task4
  1. Draw a dependency from task2 to task5 using mouse
  1. Expected: dependency takes effect and task5 immediatly shifts to the right, together with it's supertask task4.
  1. Actually: task5 gets shifted only when I save and reopen project

  * **Additional information** is very useful. First of all, we are interested in console output and/or logs.
    * There must be `.ganttproject.log` file in your home directory, please attach its contents. You can also view the log with _Help->View log_ menu, provided that GanttProject UI in general is operational
    * If you see any error messages, please copy them to your bug report.
    * If you started GanttProject console window (double click `ganttproject.bat` on Windows systems or `ganttproject.command` on MacOSX) and you see anything suspicious there, paste the console output to the bugreport.
    * Operating system, JRE version, your locale and look'n'feel (aka 'appearance') may also be important, please specify them.

Some problems can be reproduced on **your** projects only, so we may ask you to send a sample project, unless it is strictly confidential. If it is confidential but you trust us, send it by email. The contact email is available here: http://www.ganttproject.biz/about