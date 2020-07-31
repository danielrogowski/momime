title MoM IME Server ${momime.server.version}

set javapath=
if exist jre\bin\java.exe set javapath=jre\bin\

for /F %%a in (lib\classpaths\server-classpath-windows.txt) do set libcp=%%a
%javapath%java -Xmx1024m -classpath lib\momime-server-${momime.server.version}.jar;%libcp% momime.server.MomServerKickOff server