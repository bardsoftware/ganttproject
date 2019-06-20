
set INPUT=%2
set OUTPUT=%1
set VERSION=%3

"C:\Program Files\Java\zulu-11-azure-jdk_11.29.3-11.0.2-win_x64\bin\jlink" ^
  --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,jdk.charsets,jdk.unsupported ^
  --no-header-files --no-man-pages ^
  --output "%OUTPUT%\runtime" ^
  --strip-debug ^
  --compress=2

dir "C:\Program Files\Java"
dir "C:\Program Files\Java\zulu-11-azure-jdk_11.29.3-11.0.2-win_x64"
dir "C:\Program Files\Java\zulu-11-azure-jdk_11.29.3-11.0.2-win_x64\bin"

"C:\Program Files\Java\zulu-11-azure-jdk_11.29.3-11.0.2-win_x64\bin\java" -version
