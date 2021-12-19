java -version
java -Xmx2048m -classpath lib/momime-server-${project.version}.jar:$(cat lib/classpaths/server-classpath-unix.txt) momime.server.MomServerKickOff server