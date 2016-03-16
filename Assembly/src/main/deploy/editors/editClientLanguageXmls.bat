title MoM IME Language XML Editor ${momime.editors.version}
for /F %%a in (..\lib\classpaths\editors-classpath-windows.txt) do set libcp=%%a
java -Xmx1024m -classpath ..\lib\momime-editors-${momime.editors.version}.jar;%libcp% momime.editors.client.language.LanguageEditor