for /F %%a in (lib\classpaths\client-classpath-windows.txt) do set libcp=%%a
java -Xmx1024m -classpath lib\momime-client-${momime.client.version}.jar;%libcp% momime.client.MomClientKickOff client