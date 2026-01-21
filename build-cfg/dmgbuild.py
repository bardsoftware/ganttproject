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
background = "build-cfg/bg5.png"
window_rect = ((100, 100), (1080, 1080))
icon_size=96
text_size=14
icon_locations = {
  "GanttProject.app": (200, 580), "Applications": (880, 580), "HouseBuildingSample.gan": (200, 850), "LICENSE": (400, 850)
}
