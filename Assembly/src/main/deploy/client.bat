title MoM IME Client ${momime.client.version}

set javapath=
if exist jre\bin\java.exe set javapath=jre\bin\

for /F %%a in (lib\classpaths\client-classpath-windows.txt) do set libcp=%%a
%javapath%java -Xmx1024m -classpath lib\momime-client-${momime.client.version}.jar;%libcp% momime.client.MomClientKickOff client