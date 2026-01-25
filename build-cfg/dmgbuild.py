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
icon = "build-cfg/ganttproject.icns"
background = "build-cfg/dmg-background.png"
window_rect = ((100, 100), (512, 512))
icon_size=96
text_size=14
icon_locations = {
  "GanttProject.app": (30, 210), "Applications": (280, 210), "HouseBuildingSample.gan": (10, 10), "LICENSE": (200, 10)
}
