
# iDempiere Swing Client

## Projects:
* org.adempiere.report.jasper.swing -jasper report support
* org.adempiere.ui.swing - swing client
* org.adempiere.ui.swing.pluginlist - plugin to show installed plugins
* org.adempiere.ui.swing-feature - swing client feature project
* org.idempiere.swing.client.p2 - project to build p2 repository and swing client product
* org.idempiere.swing.client.parent - swing client parent pom project

## Folder layout:
* idempiere
* idempiere-swing-client

## Build
* mvn verify
* output at org.idempiere.swing.client.p2/target

## Eclipse
* at your idempiere workspace, import existing projects from idempiere-swing-client
* run using swingclient.launch

NOTE!!

If you have run the server variant before, your idempiere.properties file is in the server's home directory.

The Swing Client will look for your idempiere.properties in $USER_HOME
