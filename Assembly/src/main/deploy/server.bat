title MoM IME Server ${momime.server.version}
for /F %%a in (lib\classpaths\server-classpath-windows.txt) do set libcp=%%a
java -Xmx1024m -classpath lib\momime-server-${momime.server.version}.jar;%libcp% momime.server.MomServerKickOff server