MoM IME v${project.version} quick instructions
-----------------------------------

Here's some quick notes on how to get a game up and running in case some parts aren't obvious:

 1) Unzip momime${project.version}.zip somewhere.  I recommend you don't put this under
    C:\Program Files, or anywhere else Windows doesn't like files being modified.
    Straight under C:\, in your user home folder, or on another drive letter are fine.

 2) You must have a Java Runtime Environment (JRE) 7 or higher.

 3) Run the server.  After a few seconds it'll say
    "Listening for client connection requests on port 18250".
    Nothing else to do here, just leave the window open.

 4) Run the client.  This takes significantly longer to start up (1 minute+) as it
    reads in a lot of the graphics files and performs a lot of consistency checks
    on them before starting up.

 5) Click connect to server.  Click localhost (for the server running on your own PC).
    Enter a player name and password.  Since this is the first time you're connecting to
    your server, tick "This is a new account".  In the future, leave this box unticked and
    make sure you use the same name and password.

 6) Click "New Game".  Note the "OK" button is disabled.  To get it enabled you have to add
    at least one opponent (Human and/or AI) and enter a game name at the bottom.
    Feel free to change any of the other options, and you can tick "customize" to manually
    specify any of the game values.  Don't set turns to "Simultaneous" since this hasn't
    been added back into 0.9.5.2 yet.

 7) Pick a predefined wizard or "Custom".  For custom wizards, you then choose a portrait, and
    picking choose "Custom" again will let you pick any GIF/PNG/JPG file for your wizard
    portrait, up to a max size of 218x250.
 
 8) Pick starting spells and a race.  If you chose no Human opponents, the game will now start
    up automatically; if you chose human opponents, you have to wait for them to join and
    go through their wizard and race choices before the game will start.

 9) Afer the game starts up I hope it should be self explanatory to anyone used to the original MoM.
    A lot of features aren't implemented yet, e.g. keyboard shortcuts, and on the "Info" menu the
    only Advisors that work so far are the Surveyor and Tax Collector.

NB. All the windows in the client can be moved around by dragging an empty piece of the window
    away from any buttons or other controls.  Only the overland map window can be resized.