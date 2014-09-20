for /F %%a in (..\lib\classpaths\editors-classpath.txt) do set libcp=%%a
java -classpath ..\lib\momime-editors-${project.version}.jar;%libcp% momime.editors.client.graphics.GraphicsEditor