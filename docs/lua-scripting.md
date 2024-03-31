# Julti Lua Scripting

Julti scripts can be written in lua and saved as a .lua file in the `%userprofile%\.Julti\scripts` folder.

Running lua scripts from java is possible because of [luaj](https://github.com/luaj/luaj).

## Importing Scripts

An example script for launching and benchmarking can be found [here](https://gist.github.com/DuncanRuns/bf35099b3192f797998ae3b9a126c4c5).
It can be imported by using the gist link or the gist id:
- `https://gist.github.com/DuncanRuns/bf35099b3192f797998ae3b9a126c4c5`
- `bf35099b3192f797998ae3b9a126c4c5`


## Script Attributes

A few settings for scripts can be specified in comments anywhere in the lua script.
These attributes can also be set in legacy scripts (except comments in legacy scripts start with `#`).

### Hotkey Context

Hotkey context controls where the script can be ran.
Possible values are `game` for when in an instance in the instance list,
`wall` for when on the wall, and `anywhere` for either on wall or in an instance.

Hotkey context can be set anywhere in the script using a comment on its own line:

```lua
-- hotkey-context=game
```

### Parallel Running

By default, only one instance of a script can run at any given time. You can change this by allowing parallel running,
which will let any amount of instances of the script be running at any given time.

You can set parallel running to true using a comment:

```lua
-- allow-parallel=true
```

## Default Modules

Julti uses `JsePlatform.standardGlobals()`, which provides the following modules to lua scripts:
- `bit32`
- `table`
- `string`
- `coroutine`
- `math`
- `io`
- `os`
- `luajava`

Half of these modules probably shouldn't be used like `luajava` and `coroutine` as scripts are mostly intended to be
fully contained in Julti and use the provided functionality, however it takes no effort to keep these modules included,
and it may open up more possibilities for scripting.

Julti also provides a `julti` module to lua scripts, which contains lots of useful functions relevant to Julti usage.

### The Julti Module

The `julti` module provided to lua scripts contains the following functions (formatted like java methods):
```java
void activateInstance(int instanceNum, boolean doSetupStyle)
void sendChatMessage(String message)
void clearWorlds()
void closeInstance(int instanceNum)
void closeAllInstances()
void replicateHotkey(String hotkeyCode, int mouseX, int mouseY)
void launchInstance(int instanceNum)
void launchAllInstances()
void lockInstance(int instanceNum)
void lockAllInstances()
void log(String message)
void openFile(String filePath)
void openInstanceToLan()
boolean trySetOption(String optionName, anything optionValue)
String getOptionAsString(String optionName)
boolean tryPlaySound(String soundLocation, float volume)
void resetInstance(int instanceNum)
void resetAllInstances()
void setSessionResets(int sessionResets)
void sleep(long millis)
void waitForInstanceLaunch(int instanceNum)
void waitForInstancePreviewLoad(int instanceNum)
void waitForInstanceLoad(int instanceNum)
void pressEscOnInstance(int instanceNum)
int getInstanceCount()
int getSelectedInstanceNum()
void runCommand(String command)
void focusWall()
void runScript(String scriptName)
void setGlobal(String key, anything val)
anything getGlobal(String key, anything def)
String getInstanceState(int instanceNum)
String getInstanceInWorldState(int instanceNum)
boolean isOnMinecraftWindow()
void keyDown(String key)
void keyUp(String key)
void pressKey(String key)
void holdKey(String key, int millis)
boolean isInstanceActive()
boolean isWallActive()
```
(Generated with [this python script](https://gist.github.com/DuncanRuns/764867339c17e713b28796a2cbb29e10))

A few notes on some of the parameters and return values:
- `keyDown`, `keyUp`, `pressKey`, and `holdKey`, can take a single character (`'a'`, `'b'`, `'c'`, `'1'`, `'2'`, `'3'`), or a VK constant (`'VK_TAB'`, `'VK_RETURN'`). Microsoft documentation has a [list for virtual key code constants](https://learn.microsoft.com/en-us/windows/win32/inputdev/virtual-key-codes).


- `getInstanceState(...)` will return one of the following strings:
  - `'WAITING'`
  - `'INWORLD'`
  - `'TITLE'`
  - `'GENERATING'`
  - `'PREVIEWING'`


- `getInstanceInWorldState(...)` will return one of the following strings:
  - `'UNPAUSED'`
  - `'PAUSED'`
  - `'GAMESCREENOPEN'`


- `isInstanceActive()` and `isOnMinecraftWindow()` differ in the following way: 
  - `isInstanceActive()` will return `true` only if a minecraft instance from Julti's instances list is active.
  - `isOnMinecraftWindow()` will return `true` if any minecraft window is active.