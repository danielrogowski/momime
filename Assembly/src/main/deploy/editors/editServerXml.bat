for /F %%a in (..\lib\classpaths\editors-classpath.txt) do set libcp=%%a
java -Xmx1024m -classpath ..\lib\momime-editors-${momime.editors.version}.jar;%libcp% momime.editors.server.ServerEditor