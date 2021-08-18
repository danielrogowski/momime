title MoM IME Graphics XML Editor ${project.version}

set javapath=
if exist ..\jre\bin\java.exe set javapath=..\jre\bin\

for /F %%a in (..\lib\classpaths\editors-classpath-windows.txt) do set libcp=%%a
%javapath%java -Xmx1024m -classpath ..\lib\momime-editors-${project.version}.jar;%libcp% momime.editors.client.graphics.GraphicsEditor