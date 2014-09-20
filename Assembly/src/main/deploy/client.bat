for /F %%a in (lib\classpaths\client-classpath.txt) do set libcp=%%a
java -classpath lib\momime-client-${project.version}.jar;%libcp% momime.client.MomClientKickOff client