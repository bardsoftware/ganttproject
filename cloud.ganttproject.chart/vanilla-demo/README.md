# Vanilla JS demo

A plain HTML + JavaScript page (no build step, no bundler) that uses the **compiled**
`cloud.ganttproject.chart` library to draw **two task rectangles connected by a
finish-start dependency**.

It loads the compiled UMD modules directly as classic `<script>` tags and calls the
exported `drawChart(model, canvas)` — the model is a plain JS object with the same shape
produced by the server-side `JsonPainterImpl` (module `biz.ganttproject.mxgraph`): a single
`primitives` list where every entry is tagged with its `type` and the entries are painted in
the listed order.

## Run it

1. Build the library once (from the `cloud.ganttproject.chart` directory):

   ```bash
   ./gradlew jsBrowserProductionLibraryDistribution
   ```

   This produces the compiled modules in `build/dist/js/productionLibrary/`, which
   `index.html` references via relative paths.

2. Open `vanilla-demo/index.html` in a browser (double-click it, or serve the module
   directory with any static server, e.g. `python3 -m http.server`).

## How it hooks up

The library is a UMD module. Loaded as a classic script it attaches itself to
`globalThis` under its module name, and the exported function is nested under its Kotlin
package:

```html
<script src="../build/dist/js/productionLibrary/kotlin-kotlin-stdlib.js"></script>
<script src="../build/dist/js/productionLibrary/cloud.ganttproject.chart.js"></script>
<script>
  const drawChart = globalThis['cloud.ganttproject.chart'].cloud.ganttproject.chart.drawChart;
  drawChart(model, document.getElementById('chart'));
</script>
```

The Kotlin stdlib script must be loaded **before** the chart library.

> Tip: consumers using a bundler (webpack/vite/rollup) or TypeScript can instead
> `import { drawChart } from 'cloud.ganttproject.chart'` and get full typings from the
> generated `cloud.ganttproject.chart.d.ts`.
