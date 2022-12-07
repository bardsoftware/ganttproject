# {
#   "title": "GanttProject-3.3-Beta",
#   "icon-size": 80,
#   "background": "./bg.png",
#  "contents": [
#    { "x": 192, "y": 344, "type": "file", "path": "build/GanttProject.app" },
#    { "x": 448, "y": 344, "type": "link", "path": "/Applications" }
#  ],
#  "window": {
#    "size": {
#      "width": 487,
#      "height": 487
#    }
#  }
#}

files = ['build/GanttProject.app', 'ganttproject-builder/HouseBuildingSample.gan', 'LICENSE']
symlinks = { "Applications": "/Applications" }
badge_icon = "build-cfg/ganttproject.icns"
background = "bg3.png"
window_rect = ((100, 100), (512, 512))
icon_size=96
icon_locations = {
  "GanttProject.app": (0, 200), "Applications": (275, 200), "HouseBuildingSample.gan": (256, 0), "LICENSE": (100, 0)
}
