title MoM IME Client ${project.version}

set javapath=
if exist jre\bin\java.exe set javapath=jre\bin\

for /F %%a in (lib\classpaths\client-classpath-windows.txt) do set libcp=%%a
%javapath%java -version
%javapath%java -Xmx2048m -classpath lib\momime-client-${project.version}.jar;%libcp% momime.client.MomClientKickOff client