for /F %%a in (lib\classpaths\server-classpath.txt) do set libcp=%%a
java -classpath lib\momime-server-${project.version}.jar;%libcp% momime.server.MomServerKickOff server