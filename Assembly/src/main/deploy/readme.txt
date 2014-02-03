MoM IME v${project.version} configuration instructions
-----------------------------------------

 1) Install the Windows package momime${project.version}.msi

 2) The client requires the LBXes from the original MoM 1.31 since it reads many of the
    graphics directly from here.  So you need to tell it where these are located.  The
    installation adds a shortcut 'Set location of original MoM LBXes' onto your Start
    Menu, so find and run this.  This opens up a file browser asking for .LBX files.
    Browse to the location where your original MoM 1.31 stores all its .LBX files, select
    any .LBX file (doesn't matter which one) and click OK.

 3) The server also needs configuring.  In the main folder, the same location as this
    readme.txt, you'll notice two files with -sample in the name.  Copy both of these
    to the same location, removing the -sample (so for MoMIMEServerConfig-sample.xml
    gets copied to MoMIMEServerConfig.xml, and MoMIMEServerLogging-sample.properties
    gets copied to MoMIMEServerLogging.properties).

    The logging file doesn't need to be changed, its just there for obtaining more
    debug info you want to.  The MoMIMEServerConfig.xml file must be edited, you can
    use any text editor like Notepad for this.

3a) The server records details of user accounts (names and passwords) created on it,
    you need to tell it where this is in the <userRegistryFilename> entry.  The
    installation puts an empty user registry in

    C:\Users\Your Name\Documents\Master of Magic - Implode's Multiplayer Edition\Server\MoM IME server user registry.xml

    though the exact path varies between installing on different versions of
    Windows.  So you can either set <userRegistryFilename> to this location, or
    copy the XML file somewhere else you wish and point the <userRegistryFilename>
    entry to there instead.

3b) The server also needs to be able to find the unit, spell and other details in
    'Original Master of Magic 1.31 rules.Master of Magic Server.xml', or other custom
    files.  The installation puts this in

    C:\Users\Your Name\Documents\Master of Magic - Implode's Multiplayer Edition\Server\DB\

    though the exact path varies between installing on different versions of
    Windows.  So set <pathToServerXmlDatabases> to this location.  Do include the trailing \
    Do not specify the name of the actual XML file, just the folder it is in.
