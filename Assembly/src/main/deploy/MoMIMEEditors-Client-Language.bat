for /F %%a in (lib\momime-editors-${project.version}-classpath.txt) do set libcp=%%a
java -classpath lib\momime-editors-${project.version}.jar;%libcp% momime.editors.client.language.LanguageEditor
