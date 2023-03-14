v0.17.0:
- Added Sounds (#5)
    - Settings are in the new Sound tab
    - Also added a playsound command
- Added the "Play Next Lock" hotkey, which will play the most loaded locked instance
- A fix for blazes and caves fix
- Several refactors and nerd stuff

v0.16.0:
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

v0.15.2:
- Various code refactors and optimizations
- Julti now checks for a MultiMC instance config to get the instance name, helping to launch instances
- Fix log reading for other languages (#47)
- Now multi-threading log reading
- Added fullscreen hotkey warning when the "Go Fullscreen" key is set to the same as the in-game key

v0.15.1:
- Added a delay to world pause when pie chart is enabled
- Added some more dirt cover checks to make it more stable

v0.15.0:
- **Removed fullscreen mode** due to it being too slow and jank. Fullscreen can still be used during runs by using Julti's fullscreen hotkey. Refer to v0.14.0 patch notes.
- Added support for instances that do not use the World Preview mod (will perhaps allow for stuff like pre-1.9 in the future) (#43)
- Added Pie Chart On Load option, which is illegal for normal runs and is meant for superflat runs (apparently) (#42)
- "Launch All Instances" button will now only try to launch instances that are not open.
- Lots of code cleanup regarding resetting, some stuff slightly adjusted to match intended functionality
- Fixed instances failing to reset sometimes (#41)
- Fixed a bug where windows would lose their borderless-ness when resetting from a fullscreened window sometimes

v0.14.1:
- Made a work-around for #41, but still not solved. You shouldn't have to restart instances or Julti to continue using an instance which is stuck.

v0.14.0:
- Added some guide text to hotkey settings
- Added fullscreen hotkey
    - Should not be the same as the actual fullscreen hotkey
    - Lets Julti know that MC is in fullscreen, so it can handle the resetting
    - Lets you turn off the "use fullscreen" option, so you can have the benefits of ultra speed borderless resetting but still use fullscreen in the run
- Added option for Unpause on Switch (#40)
- Removed brine

v0.13.1:
- Now asking Windows where the mouse is because sometimes java doesn't have a good answer
- Made resetting a fullscreened instance potentially less jank as frick

v0.13.0:
- Added fullscreen mode (experimental)
- Cleaned up a some built in wall code and options

v0.12.0:
- Big refactor and changes to the way switching to wall works:
    - Added OBS projector name format option
    - Changed wall activation method for Julti wall to fix it sometimes failing to activate
- Refactor dirt cover logic and potentially fix a dirt cover issue (#36)
- Removed Hero

v0.11.5:
- Removed auto clear worlds option, replacing with a clear worlds button on the main GUI
- Sorted main menu buttons
- Addeded Herobrine

v0.11.4:
- Removed maximize option, now on by default for non-borderless
- Added Herobrine

v0.11.3:
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
