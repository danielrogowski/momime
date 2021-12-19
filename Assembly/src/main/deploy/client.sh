java -version
java -Xmx2048m -classpath lib/momime-client-${project.version}.jar:$(cat lib/classpaths/client-classpath-unix.txt) momime.client.MomClientKickOff client