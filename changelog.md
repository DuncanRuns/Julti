Changes in v1.4.0:
- Added JuWaWi as a default plugin
  - JuWaWi (Julti Wall Window) provides a way to use Julti without having to open or set up OBS (or even have it installed)
  - Works with any wall style (should even work with draconix's custom wall plugin)
  - Customizable colors and slightly configurable locks (locks are just a border drawn on top of the instance with custom thickness and color)
  - Configuration saves to Julti profiles
  - Not as good as an OBS projector
    - Doesn't render instances at 60FPS, instead having no FPS and drawing single frames of instances at a time when they reach certain states
    - No custom images for locks or background, just basic shapes and solid colors only
- Added "Utility Mode", will be a main button on the GUI in the future but for now is at the top of experimental.
  - Instances list won't be saved, closed instances will be automatically removed from the instances list, any new opened instances will be added to the instances list
  - Multi instance functionality is mostly disabled
  - Only the main reset hotkey and script hotkeys will work
  - Useful for practice, ranked, or tournament playing instances (or a new secret scenario in the future)
- Added scripts action menu (right click scripts for more stuff)
  - Added script editing (@pants721 #91)
- Scripts system entirely reworked:
  - Scripts moved to a scripts folder rather than all being in scripts.txt
    - Any legacy scripts from scripts.txt will be converted to script files
  - Added lua scripting
    - Should be way more powerful than legacy scripts
    - .txt script files will be legacy scripts, .lua script files will be lua scripts
    - Added lua functions equivalents for all the Julti commands and a few more
    - Plugins are be able to register libraries for lua scripts to have access to
    - Added the `genluadocs` command which will convert all loaded libraries (`julti`, `benchmark`, possible `moveresize`) into `.lua` files which can be used like libraries while making scripts.
      -A vscode settings file will be generated for the scripts folder to support these library files.
  - Greatly improved script importing
    - You can now put a GitHub gist link or id and download scripts
      - The gist must contain a .lua file for a lua script or .txt file for a legacy script
    - You can still use the old legacy script codes to import
  - Added script customization.
      - Lua scripts can define customization functionality so that when importing (or right clicking and pressing customize) the script can prompt the user and ask for custom values (such as eye measuring width).
- Added illegal mod checker (will appear as warnings in log, you can ignore them if playing something like icarus)
- Added more sounds (bone sounds, specnr sounds)
- Added basic sound randomization (selecting a folder will pick a random sound from the folder)
- Move warnings for invalid states to debug messages
- Fullscreen fix for 1.19+ (@draconix6 #90)
- Added Z ordering support for reset styles. Custom reset styles (notably custom wall) can now specify the order that instances should overlap.
- Prevent instances from launching in fullscreen (sets `fullscreen:false` in options.txt before launch)
- Fix instance detection for weird legacy versions
- Update benchmark plugin
- Added lots of stuff for plugins to use, including saving custom data into Julti profiles
- Update PaceMan Tracker to v0.4.0, this will support item tracking so that pacemans can (attempt to) be more accurate
- Various tweaks/fixes

Changes in v1.3.1:
- Hotfix for Julti crashing when state output/world preview is not present

Changes in v1.3.0:
- Support for new StandardSettings option `f3PauseOnWorldLoad`
- Fix/tweaks to various warning messages
- Tweaks to the program launcher options (@pants721 #88)
- Added a `cancelif` command which can cancel scripts during execution, allowing for things like a reset hotkey that only works in the first 20 seconds
- Updated Standard Manager to be a little less annoying
- Script hotkeys can now be used even if some/all instances are closed

Changes in v1.2.0:
- Added a logo
  - Kinda scuffed and temporary, but I don't imagine replacing it ever 
- Updated PaceMan Tracker to v0.3.1
- Add program launching (@pants721 #85)
- Added tray icon and a minimize to tray icon option (@draconix6 #86)
- Added "resizeable" borderless (weird version of borderless that you can manually resize) (@draconix6 #86)
- Resource packs will now sync with "Sync Instances" button (@draconix6 #86)
- Various tweaks and fixes

Changes in v1.1.0:
- Updated PaceMan Tracker to v0.3.0
  - Update SpeedRunIGT to v14.1! 
- Some new window settings buttons to optimize for various playstyles (@draconix6 #84)
- Added more option descriptions (@draconix6 #84)
- Removed "Reset All After Playing", it was a verification feature for non wall recorders, which is now no longer needed. (@draconix6 #84)
- Removed other verification warnings because wall recording is no longer required. (@draconix6 #84)
- LAUNCH plugin event (devs might care, you surely don't) (@pants721 #83)
- Added `clearworlds` command
- Various tweaks and fixes

Changes in v1.0.2:
- Updated PaceMan Tracker (a few bug fixes)

Changes in v1.0.1:
- Fix issue for computers using a comma for decimal formatting

Changes in v1.0.0:
- **Added PaceMan.gg support! Access the tracker through the plugins menu.**
- Big changes to the OBS script (restart OBS after updating Julti!):
  - Options that don't affect scene generation have been moved into Julti's options (see the OBS section).
  - Added Multi scene generator into the main script (previously existed as a separate script). This generates a scene per instance so you can do silly transitions and stuff.
  - Added `[Generation Option]` to the start of all generation options in the script, to make it obvious that they only take effect when pressing `Generate Scenes` again.
  - Each instance in the Julti scene will generate with an instance number indicator at 15% opacity.
  - Lock Display being stuck with example instances on should no longer happen.
- Added a "Show Instance Number Indicators" option (found in OBS section). This is added along with all the options moved from the OBS script.
- Added "Active Instance Scaling" for the align active instance option. This is mainly used for when the OBS canvas size does not match the monitor resolution.
- Julti will now output a `currentlocation.txt` file in the `.Julti` folder, which is purely for the current instance number overlay in multi scenes.  
- Standard Manager will now tell Julti to reload instance options whenever you change an option. This means changing Julti-related Minecraft keybinds using Standard Manager will work immediately.
- Updated world clearing. The new conditions for a world to be deleted are:
  - The world name must not start with "_" and must not contain a "Reset Safe.txt" file.
  - The world name match a common speedrun world name (`New World...`, `Random/Set Speedrun #...`, `Practice Seed...`).
  - The world must not be within the last 6 most recently played worlds (6 deletable worlds will be kept).
- The Julti GUI will now show which instance is being played or was last played. (@draconix6 #78)
- Added minimize projector experimental option. (@draconix6 #78)
- Added detection for multiple Julti's to be open. (@draconix6 #78)
- Various fixes and tweaks. (@draconix6 #77)
  - Julti GUI is now forced back in bounds when it launches out of bounds.
  - Coop mode will work a little better when not using bypass.
- Added a "doaction" command which can replicate the exact functionality of hotkeys (the `reset all` command isn't exactly the same as full reset hotkey).
- Added experimental option "Activate Projector On Reset" for bypass + thin BT users. (@draconix6 #79)
- Added some option descriptions.(@draconix6 #79) 
- Added various standard options warnings. (@draconix6 #79)
- Added fullscreen delay experimental option. (@draconix6 #79)
- Updated default scripts to contain a new launch + mega warmup script, and remove the old warmup script and the dragon fight script.
- Added benchmark plugin as a default plugin.
- Updated plugin loading to choose the newest version out of the default plugins and folder plugins.
- Added "Package Files For Submission" option when right-clicking instances in the instances panel. This will collect necessary files for verification from that instance in one place. (@draconix6 #80)

Changes in v0.22.0:
- Added customizable delay between instance launches
- Changed default affinity values
- Added a new button to set affinity values to defaults (recommended)
- Added freeze filter support (in experimental) (#75 by @draconix6)
- Added f1 support (f1:true in SS can make the wall look "cleaner")
- Overhauled window management settings to give the user more power of the window behaviour
  - Window maximizing is now explicitly set by the user
  - Window position can now reference the center of the window
  - "Choose Monitor" will now use the new center position
  - Added "Fullscreen before Unpause" option to fix mouse issues when working with weird window sizes
- Reorganized some GUI stuff
- Various fixes

Changes in v0.21.4:
- Fixed a crash when Julti is placed in a path with spaces

Changes in v0.21.3:
- Fix some plugin loading stuff
- Check if `TheWall.ahk` is running and warn the user of the conflict
- Fixed some scene generation crashes in the OBS script
- Added session reset counter
    - Outputs to `sessionresets.txt` which is found in the `.Julti` folder (also where `resets.txt` is)
    - The command `sessionresets [num]` can be used to set the counter manually, in the case where you need to restart Julti or if Julti crashes.
- Profile options will now save every 100 resets (reset counter isn't just stored in `resets.txt`, it is stored in each profile as well)
    - This should mostly fix the issue of "my reset counter goes back by thousands"; Julti already saves the reset count when exiting, but clearly this doesn't fully work sometimes, so more saving has been added.

Changes in v0.21.2:
- Removed auto title setting after 5 seconds of inactivity
- Updated Plugin API
- Prevent a divide by 0 crash (probably still underlying issues with this)
- Fixed resetting window size on startup for fullscreen players
- Fixed spacing of texts and buttons on the Plugins menu

Changes in v0.21.1:
- Updated Standard Manager
    - Fixed reversed naming on render distance values

Changes in v0.21.0:
- Added plugins menu
- Added more plugin API
- Added Standard Manager as a default plugin

Changes in v0.20.1:
- Made the "Play Next Lock" and "Reset All" wall keys more compatible
- Added Plug-in API and Plug-in loading (Plug-in menu still needed)

Changes in v0.20.0:
Eventually there were too many changes to even keep track of, this list is certainly not complete.

Julti Functionality Changes in v0.20.0:
- Added Smart Switch: When enabled, attempts to switch to instances that are not yet loaded will be redirected to already locked and loaded instances if any exist.
- When suggesting to update, you can now automatically download and restart Julti without visiting the github page.

Option changes in v0.20.0:
- Dirt cover release % has been removed, world preview state output is reliable in v4.0.0, so there is only a checkbox for dirt covers now
- Wide Reset Squish has been replaced by multiple window size options (playing vs resetting window size)
- Pause on load option removed, worlds will pause on load by default now
- No cope mode removed
- "Clean wall" has been removed, clutters code and is problematic for too many people for not much benefit

OBS Script changes in v0.20.0:
- New verification scene generation for a single loading square due to world preview updates, and will also support multiple window sizes during resetting to improve verifiability of the "Unsquish on lock setting" (You will need to press generate scenes again to get this)
- Ability to have single instance scenes (`Playing 1`, `Playing 2`, etc...) (There are no scene generators for this yet)
- Checkbox to generate window captures on the Julti scene instead of game cap
- Checkbox to let verification scene share captures with Julti scene
- Checkbox to make dirt covers turn the instance invisible / tp it away to show a background image
- Checkbox to center active instances and show their actual size for a better viewing experience when using eye zoom macro
- A couple crash and error message fixes

Technical Changes in v0.20.0:
- Rewrote the core of Julti and copied most of the old code on top of it and then adjusted it to work with the new core (this was basically all of the work)
  - This means Julti should be more stable and consistent, including the bugs, so hopefully they are way more consistent and fixable.
  - Now uses the new world preview state output feature, meaning Julti can very easily determine what state an instance is in, leading to more stability and consistency
- Added affinity "jumping", which boosts the affinity of an instance that is about to be switched to, or boosts the affinity of instances that have been sent a reset input, making switching and resetting smoother. This is just a consistent optimization so there is no option for this.

Changes in v0.18.0:
- Added experimental features tab, which you can enable in the other options tab
  - This will let you access some hidden features from the GUI which you previously could only enable through the command line.
- Added new experimental feature: "Always On Top Projector"
  - This could help solve issues such as instances appearing on top of the wall while the wall is supposed to be focused. 

Changes in v0.17.2:
- Lots of bug fixes and changes for non-borderless users
- Removed F3 as a modifier key, allowing it to be used as a hotkey
- Add a warning for when a minecraft key in standard settings is badly formatted
- Fix scripts freezing when using a hotkey for a deleted script
- Fix launching instances with mismatching folder/name
- Added a warning to the "unsquish on lock" option since it may mess with verification
- Added a bunch of other warnings and errors to the log
- Better game version detection using MultiMC instance data rather than the window title of the game

Changes in v0.17.1:
- Massively sweeping code changes and refactors (thanks in part to @QuesiaSR)
- It turns out that for the past however many versions, julti has been using every reset method, meaning it has pressed the create new world key, leave preview key, and esc-shift-tab-enter to reset every instance every single time, so this has been fixed
- When instances are detected, they will now quickly activate and receive a click input, which fixes mouse teleports and not being able to reset from title screen

Changes in v0.17.0:
- Added Sounds (#5)
    - Settings are in the new Sound tab
    - Also added a playsound command
- Added the "Play Next Lock" hotkey, which will play the most loaded locked instance
- A fix for blazes and caves fix
- Several refactors and nerd stuff

Changes in v0.16.0:
- Completely rewritten OBS script and functionality (#45)
    - The lua script is now saved by Julti to your Documents folder and the .Julti folder
    - Only set names can be used, however everything can be set up with "Generate Scenes"
    - Everything is centered around the "Julti" scene
    - Sounds are automatically setup in the "Sounds" scene, and can be customized there
    - Locks are customized through changing the "Lock Display" scene
    - Dirt covers are customized through changing the "Dirt Cover Display" scene
    - Further "stream scenes" can be generated, and allow you to add some elements that you only want to appear while on the wall or only want to appear while playing an instance.
    - You can add borders to instances inside the OBS settings in the Julti App
    - The OBS setup for both regular wall and instance moving is the same
    - OBS Script is now automatically placed in .Julti and Documents folders
- Added Julti Scripts
    - Can be used to open to lan and go into spectator, start a dragon fight with items, etc.
    - New scripts can be made and imported using the scripts menu accessed from the main GUI
    - Hotkeys can be bound to scripts in the hotkey settings (hotkey bindings are per profile, but the scripts themselves are global)
- Added Dynamic Wall (Instance Moving)
    - Features a smaller wall (default 2x2 or set to custom wall size in wall settings)
    - Locked instance go to the "lock bar" at the bottom of the screen, height of the lock bar can be customized in wall settings
- Added "Reset Instance Data" button to clear any data stored about the instances such as detected standard options or mods
- Added customizable affinity "burst" (#54)
- Added offline instance launching (#53)
- Added World Loaded Affinity
- Added option to unsquish (unwiden) instances when locking to make switching smoother
- Removed Built-in Wall
- Removed fullscreen key, you can now just use the in-game key, and Julti will know (#49)
- Support for standard options, some values will automatically be set for the user to be compatible with Julti (#50, #51)
- Now can use F1 to make the wall cleaner (#9)
- Fixed world load issues (#57)
- Fixed unpause on switch issues

Changes in v0.15.2:
- Various code refactors and optimizations
- Julti now checks for a MultiMC instance config to get the instance name, helping to launch instances
- Fix log reading for other languages (#47)
- Now multi-threading log reading
- Added fullscreen hotkey warning when the "Go Fullscreen" key is set to the same as the in-game key

Changes in v0.15.1:
- Added a delay to world pause when pie chart is enabled
- Added some more dirt cover checks to make it more stable

Changes in v0.15.0:
- **Removed fullscreen mode** due to it being too slow and jank. Fullscreen can still be used during runs by using Julti's fullscreen hotkey. Refer to v0.14.0 patch notes.
- Added support for instances that do not use the World Preview mod (will perhaps allow for stuff like pre-1.9 in the future) (#43)
- Added Pie Chart On Load option, which is illegal for normal runs and is meant for superflat runs (apparently) (#42)
- "Launch All Instances" button will now only try to launch instances that are not open.
- Lots of code cleanup regarding resetting, some stuff slightly adjusted to match intended functionality
- Fixed instances failing to reset sometimes (#41)
- Fixed a bug where windows would lose their borderless-ness when resetting from a fullscreened window sometimes

Changes in v0.14.1:
- Made a work-around for #41, but still not solved. You shouldn't have to restart instances or Julti to continue using an instance which is stuck.

Changes in v0.14.0:
- Added some guide text to hotkey settings
- Added fullscreen hotkey
    - Should not be the same as the actual fullscreen hotkey
    - Lets Julti know that MC is in fullscreen so it can handle the resetting
    - Lets you turn off the "use fullscreen" option so you can have the benefits of ultra speed borderless resetting but still use fullscreen in the run
- Added option for Unpause on Switch (#40)
- Removed brine

Changes in v0.13.1:
- Now asking Windows where the mouse is because sometimes java doesn't have a good answer
- Made resetting a fullscreened instance potentially less jank as frick

Changes in v0.13.0:
- Added fullscreen mode (experimental)
- Cleaned up a some built in wall code and options

v0.12.0:
- Big refactor and changes to the way switching to wall works:
    - Added OBS projector name format option
    - Changed wall activation method for Julti wall to fix it sometimes failing to activate
- Refactor dirt cover logic and potentially fix a dirt cover issue (#36)
- Removed Hero

Changes in v0.11.5:
- Removed auto clear worlds option, replacing with a clear worlds button on the main GUI
- Sorted main menu buttons
- Addeded Herobrine

Changes in v0.11.4:
- Removed maximize option, now on by default for non-borderless
- Added Herobrine

Changes in v0.11.3:
- Overhauled the options GUI visuals
- Julti GUI will now remember the location it was in when re-opened
- Added more locations for auto-detecting MultiMC/Prism Launcher.

Changes in v0.11.2:
- Dirt Cover % setting now actually works

Changes in v0.11.1:
- Major dirt cover fix, whoopsie

Changes in v0.11.0:
- Added instance syncing (copy mods/config from instance 1 to all others)
- Added reset counter (#27)
- Dirt covers now disappear when spawn area is 1% or more (Customizable to any %) (Old default was 0%)
- Added "Open Folder" button to instance right click menu from the main GUI

Changes in v0.10.0:
- Added Coop Mode (#16)
- Solved mouse focus issues once and for all (#34)
- Changed dirt cover timing
- Adjust bypass order of operations to be marginally faster

Changes in v0.9.0:
- Added Dirt Covers (#30)
- Added Update Checker (#32)
- Large link script updates
    - More error checking and general code improvement
    - Support dirt covers
    - Main OBS will switch to wall scene upon finding that it is the main OBS

Changes in v0.8.1:
- Fixed MIDNIGHT BUG!!!! (#33)
- Added Sleep Background Lock support

Changes in v0.8.0:
- Fixed a crash when enabling OBS hotkeys
- Added better affinity with options (#6) (Thanks @jojoe77777)
- Julti Wall now works with more monitor sizes (you should still probably use OBS wall)

Changes in v0.7.0:
- Added option to lock instance instead of playing when the instance is still loading (#29)
- Added a bypass option to return to wall if there are no locked instances that are loaded
- Added a checkbox next to every hotkey to allow it to work regardless of extra pressed keys (#31)

Changes in v0.6.3:
- Link script now has 2nd obs scene choice
- Undone fix to #18, as it slows resets

Changes in v0.6.2:
- Attempt to fix some issues with mouse focus

Changes in v0.6.1:
- Speed up wide resets (#25)
- Prioritize loaded instances when doing bypass (#26)

Changes in v0.6.0:
- Added wide resets (#12)
- Added support for non-borderless windows (#10)
- Added support for maximized windows
- Added Focus Reset Key (#7)
- Added Basic background resetting on wall (#21)
- Added support for [Prism Launcher](https://prismlauncher.org/) in auto-detect
- Fix pie chart on wall bug (#18)

Changes in v0.5.1:
- Added darkening warning
- Lock Icon will now render exactly the same size as the file
- Default lock icon scaled down 2x
- Changed a few defaults

Changes in v0.5.0:
- MAJOR optimizations to the built-in wall (#1)
- Wall Bypass is now toggle-able (#15)
- Reorganized some options in the Wall tab

Changes in v0.4.1:
- Fixed obs link issues
- Fixed error logging for > 9 instances

Changes in v0.4.0:
- Added Affinity (Thanks to specnr wall)
- Created the Julti OBS Link Script (websocket but 1000x better and easier). Save it somewhere safe, and add it to OBS in Tools -> Scripts.

Changes in v0.3.0:
- Massive changes to the code to allow for more thingies to happen
- The ability to use obs wall
