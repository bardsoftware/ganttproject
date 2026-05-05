Update the GanttProject version number and application title across all required files.

Start by asking the user for:
1. The new **build number** (e.g. `3393`) — a 4-digit integer
2. The new **application title** (e.g. `GanttProject 3.4 Beta IV`) — used in UI and packaging metadata
3. Is it a minor or a major update? (minor/major)

Once you have both values, derive the following from them:
- `VERSION_FULL` = `3.4.<build_number>` (e.g. `3.4.3393`)
- `VERSION_FULL_DOT4` = `3.4.<build_number>.0` (e.g. `3.4.3393.0`)
- `WINDOWS_FOLDER` = title with spaces replaced by hyphens (e.g. `GanttProject-3.4-Beta-IV`)
- `TODAY` = today's date in `YYYY-MM-DD` format

Then update **all** of the following files. Read each file before editing it.

---

### 1. `.github/workflows/build-packages.yml`
Update env vars near the top of the file:
- `BUILD_NUMBER` → new build number
- `VERSION` → `VERSION_FULL`
- `WINDOWS_APP_FOLDER_NAME` → `WINDOWS_FOLDER`
- `MAC_APP_NAME` → new application title

### 2. `CHANGELOG`
Update the first version entry line from the old `ganttproject (3.4.XXXX)` to `ganttproject (VERSION_FULL)`.

### 3. `build-bin/build-deb.properties`
Update both:
- `version` → `VERSION_FULL`
- `version_build` → `VERSION_FULL`

Note: these lines use a `3.3.` prefix pattern — match and replace the full value.

### 4. `ganttproject-builder/BUILD-HISTORY-MAJOR`
If it is a major update, append a new line at the bottom:
```
TODAY BUILD_NUMBER APPLICATION_TITLE.
```

### 5. `ganttproject-builder/BUILD-HISTORY-MINOR`
If it is a minor update, find out the modules that have been changed since the previous major or minor update using Git
commit history. Then build a comma-delimited list of module names `UPDATED_MODULES` and append a new line at the bottom:

```
TODAY BUILD_NUMBER UPDATED_MODULES
```

### 6. `ganttproject-builder/VERSION`
Replace the entire file content with `VERSION_FULL` (no trailing newline beyond what already exists).

### 7. `ganttproject-builder/ganttproject-launch4j.xml`
Update:
- `<fileVersion>` → `VERSION_FULL_DOT4`
- `<productVersion>` → `VERSION_FULL_DOT4`  *(this tag is often missed — make sure to update it)*
- `<txtFileVersion>` → new application title

### 8. `ganttproject/src/main/java/biz/ganttproject/app/Splash.kt`
Update the `Label("...")` call that shows the version on the splash screen.
The label text should be everything after `GanttProject ` in the application title (e.g. `3.4 Beta IV`).

### 9. `ganttproject/src/main/java/biz/ganttproject/platform/UpdateDialogModel.kt`
Find the hardcoded fallback version string (a string literal containing the old build number) and update it to `VERSION_FULL`.

---

After all edits, summarize what was changed in a short table: file path → what was updated.
