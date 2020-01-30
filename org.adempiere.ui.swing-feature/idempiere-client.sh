#!/bin/sh
#
# script parameters are used as VM args. e.g.:
# ./idempiere-client.sh -DPropertyFile=/home/tbayen/idempiere-conf2.properties
# -- tbayen

if [ $JAVA_HOME ]; then
  JAVA=$JAVA_HOME/bin/java
else
  JAVA=java
  echo JAVA_HOME is not set.
  echo You may not be able to start the server
  echo Set JAVA_HOME to the directory of your local JDK.
fi

echo ===================================
echo Starting idempiere Client
echo ===================================

VMOPTS="-Dorg.osgi.framework.bootdelegation=sun.security.ssl,org.w3c.dom.events
-Dosgi.compatibility.bootdelegation=true
-Dosgi.noShutdown=true
-Dosgi.framework.activeThreadType=normal
-Dmail.mime.encodefilename=true
-Dmail.mime.decodefilename=true
-Dmail.mime.encodeparameters=true
-Dmail.mime.decodeparameters=true
--add-exports java.desktop/sun.awt=ALL-UNNAMED
--add-exports java.sql.rowset/com.sun.rowset=ALL-UNNAMED
--add-exports java.naming/com.sun.jndi.ldap=ALL-UNNAMED
--add-modules=ALL-SYSTEM
--add-modules java.se --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.sql.rowset/com.sun.rowset=ALL-UNNAMED --add-exports java.naming/com.sun.jndi.ldap=ALL-UNNAMED"


$JAVA $VMOPTS $@ -jar plugins/org.eclipse.equinox.launcher_1.*.jar -application org.adempiere.ui.swing.client
