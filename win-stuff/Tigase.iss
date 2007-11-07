#define ver "3.1.1-b728";

[Setup]
AppName=Tigase Server
AppVersion={#ver}
AppVerName=Tigase XMPP Server {#ver}
AppPublisher=tigase.org
AppPublisherURL=http://www.tigase.org/
AppSupportURL=http://www.tigase.org/
AppUpdatesURL=http://www.tigase.org/
DefaultDirName={pf}\Tigase
DefaultGroupName=Tigase Server
AllowNoIcons=true
LicenseFile=Licence.txt
OutputDir=..\packages
OutputBaseFilename=tigase-server-{#ver}
Compression=lzma
SolidCompression=true
UninstallDisplayIcon={app}\Tigase.ico
VersionInfoCopyright=Artur Hefczyc

[Languages]
Name: english; MessagesFile: compiler:Default.isl
Name: basque; MessagesFile: compiler:Languages\Basque.isl
Name: brazilianportuguese; MessagesFile: compiler:Languages\BrazilianPortuguese.isl
Name: catalan; MessagesFile: compiler:Languages\Catalan.isl
Name: czech; MessagesFile: compiler:Languages\Czech.isl
Name: danish; MessagesFile: compiler:Languages\Danish.isl
Name: dutch; MessagesFile: compiler:Languages\Dutch.isl
Name: finnish; MessagesFile: compiler:Languages\Finnish.isl
Name: french; MessagesFile: compiler:Languages\French.isl
Name: german; MessagesFile: compiler:Languages\German.isl
Name: hungarian; MessagesFile: compiler:Languages\Hungarian.isl
Name: italian; MessagesFile: compiler:Languages\Italian.isl
Name: norwegian; MessagesFile: compiler:Languages\Norwegian.isl
Name: polish; MessagesFile: compiler:Languages\Polish.isl
Name: portuguese; MessagesFile: compiler:Languages\Portuguese.isl
Name: russian; MessagesFile: compiler:Languages\Russian.isl
Name: slovak; MessagesFile: compiler:Languages\Slovak.isl
Name: slovenian; MessagesFile: compiler:Languages\Slovenian.isl
Name: spanish; MessagesFile: compiler:Languages\Spanish.isl

[Files]
Source: wrapper\wrapper.exe; DestDir: {app}; Flags: ignoreversion; Languages: 
Source: ..\certs\*; DestDir: {app}\certs; Flags: ignoreversion recursesubdirs createallsubdirs
Source: ..\docs-tigase-server\*; DestDir: {app}\docs; Flags: ignoreversion recursesubdirs createallsubdirs
Source: ..\jars\*; DestDir: {app}\jars; Flags: ignoreversion recursesubdirs createallsubdirs
Source: ..\libs\*; DestDir: {app}\libs; Flags: ignoreversion recursesubdirs createallsubdirs
Source: ..\database\mysql-schema.sql; DestDir: {app}\database\mysql-schema.sql; Flags: ignoreversion
Source: ..\database\postgresql-schema.sql; DestDir: {app}\database\postgresql-schema.sql; Flags: ignoreversion
Source: wrapper\wrapper.conf; DestDir: {app}; Flags: ignoreversion
Source: scripts\InstallTigaseService.bat; DestDir: {app}; Flags: ignoreversion
Source: ..\README.html; DestDir: {app}; Flags: ignoreversion
Source: scripts\Run.bat; DestDir: {app}; Flags: ignoreversion
Source: scripts\Tigase.bat; DestDir: {app}; Flags: ignoreversion
Source: scripts\UninstallTigaseService.bat; DestDir: {app}; Flags: ignoreversion
Source: Tigase.ico; DestDir: {app}; Flags: ignoreversion
Source: scripts\Uninst.bat; DestDir: {app}; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files
Source: Licence.txt; DestDir: {app}; Flags: ignoreversion
Source: wrapper\wrapper.dll; DestDir: {app}\libs; Flags: ignoreversion
Source: wrapper\wrapper.jar; DestDir: {app}\libs; Flags: ignoreversion
Source: ..\etc\tigase.conf; DestDir: {app}\etc; Flags: ignoreversion
Source: ..\etc\initial.properties; DestDir: {app}\etc; Flags: ignoreversion

[Dirs]
Name: {app}\logs


[Icons]
Name: {group}\Start Tigase Server; Filename: {app}\Run.bat; WorkingDir: {app}; IconFilename: {app}\Tigase.ico
Name: {group}\Install Tigase Service; Filename: {app}\InstallTigaseService.bat
Name: {group}\Uninstall Tigase Service; Filename: {app}\UninstallTigaseService.bat
Name: {group}\{cm:ProgramOnTheWeb,Tigase}; Filename: http://www.tigase.org/

[UninstallRun]
Filename: {app}\Uninst.bat
