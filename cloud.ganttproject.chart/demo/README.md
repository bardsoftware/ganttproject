# Chart library demo

A minimal browser app that consumes the `cloud.ganttproject.chart` Kotlin/JS library
and renders a small chart model with **two task rectangles connected by a finish-start
dependency** (an orthogonal arrowed connector).

The model is built as JSON — the same shape produced by the server-side `JsonPainterImpl`
(module `biz.ganttproject.mxgraph`) — parsed into a `ChartModel`, and drawn with the
library's exported `drawChart(model, canvas)` function. See
[`src/jsMain/kotlin/.../demo/Main.kt`](src/jsMain/kotlin/cloud/ganttproject/chart/demo/Main.kt).

## Run it

From the `cloud.ganttproject.chart` directory:

```bash
# Live dev server with hot reload (opens the page in a browser):
./gradlew :demo:jsBrowserDevelopmentRun

# Or produce a static bundle you can open / serve:
./gradlew :demo:jsBrowserDevelopmentExecutableDistribution
# output: demo/build/dist/js/developmentExecutable/{index.html, demo.js}
```

If a build fails with *"Lock file was changed"*, run `./gradlew kotlinUpgradeYarnLock` once
and retry.
