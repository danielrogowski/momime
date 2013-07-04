for /F %%a in (lib\momime-server-${project.version}-classpath.txt) do set libcp=%%a
java -classpath lib\momime-server-${project.version}.jar;%libcp% momime.server.MomServer
