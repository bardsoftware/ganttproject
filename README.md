GanttProject
============

GanttProject is a free project management app for desktops. It comes with:

* Task hierarchy and dependencies, milestones, baselines.
* Gantt chart with an option to generate PERT chart.
* Resource load chart.
* Task cost calculation.
* Export to PDF, HTML, PNG.
* Interoperability with MS Project, Excel and other spreadsheet apps.
* Project collaboration using WebDAV and a commercial collaboration service [GanttProject Cloud](https://ganttproject.cloud).

Visit http://ganttproject.biz to learn more.


## License
GanttProject is free and open-source software, distributed under GNU General Public License v3.

## Check out, build and run

Clone the repository using `git clone https://github.com/bardsoftware/ganttproject.git` and checkout the submodules
with `git submodule update` from the repository root.

You can build and run the core part of GanttProject, with no export/import features, using `gradle run`.

If you want to build the complete app, use `gradle runapp` or `gradle distbin && cd ganttproject-builder/dist-bin && ./ganttproject` (on Linux and macOS)