#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
APP_HOME=$(cd "`dirname "$0"`" && pwd)

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# Make sure JAVA_HOME is set
if [ -z "$JAVA_HOME" ] ; then
  echo "ERROR: JAVA_HOME is not set."
  exit 1
fi

# Execute Gradle wrapper jar
exec "$JAVA_HOME/bin/java" $DEFAULT_JVM_OPTS -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
