; Santulator Inno Setup File

#define MyAppName "GanttProject"
#define MyAppVersion "3.0.0"
#define MyAppPublisher "BarD Software s.r.o"
#define MyAppURL "https://www.ganttproject.biz"
#define MyAppExeName "GanttProject.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{174F6867-7486-4C20-96B3-0F57311A4BD9}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DisableProgramGroupPage=yes
DisableDirPage=no
OutputBaseFilename={#MyAppName}-{#MyAppVersion}
Compression=lzma
SolidCompression=yes
ChangesAssociations=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "@executable.location@"; DestDir: "{app}"; Flags: ignoreversion
Source: "@bundle.content@"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{commonprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Registry]
Root: HKLM; Subkey: "Software\Classes\.santa"; ValueType: string; ValueName: ""; ValueData: "SantulatorSession"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "Software\Classes\SantulatorSession"; ValueType: string; ValueName: ""; ValueData: "Santulator Session"; Flags: uninsdeletekey
Root: HKLM; Subkey: "Software\Classes\SantulatorSession\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\SantulatorSession.ico"
Root: HKLM; Subkey: "Software\Classes\SantulatorSession\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\Santulator.exe"" ""%1"""