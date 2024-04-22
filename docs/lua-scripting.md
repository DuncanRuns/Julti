# Julti Lua Scripting

Julti scripts can be written in lua and saved as a .lua file in the `%userprofile%\.Julti\scripts` folder.

Running lua scripts from java is possible because of [luaj](https://github.com/luaj/luaj).

## Importing Scripts

An example script for launching and benchmarking can be
found [here](https://gist.github.com/DuncanRuns/bf35099b3192f797998ae3b9a126c4c5).
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

Julti provides the following default modules to lua scripts:

- `bit32`
- `table`
- `string`
- `math`

Julti also provides a `julti` module to lua scripts, which contains lots of useful functions relevant to Julti usage,
and plugins may also provide functions.

### Library Docs

Running the `genluadocs` command in the Julti command line will generate library lua files in
the `%userprofile%/.Julti/scripts/libs` folder. The generated files will contain function definitions with
descriptions (if provided by the plugin), parameter types, and return types.

# Making a Lua Library for Julti

Sample code here:
https://github.com/DuncanRuns/Julti-Benchmark/commit/884d853a0d34bfe537b99cd424379bae55c03fa6

1. Make a class that `extends LuaLibrary`
2. Use `@SuppressWarnings("unused")` on the class so that you don't get unused warnings
3. Make a constructor: `public YourLibraryClass(CancelRequester requester) {super(requester,"namethatluauseshere")}`
   replacing "namethatluauseshere" with the library name you want to use in lua scripts
4. Make some regular java methods to be magically turned into lua functions.

- Supported parameter types:
    - Basic java types: primitives (`int`, `double`), primitive wrappers (`Integer`, `Double`), and `String`
    - `LuaValue` which directly takes what is given by lua with no conversion to java types
- Supported return types include all supported parameter types as well as `void`

5. Register the library with `LuaLibraries.registerLuaLibrary(YourLibraryClass::new)`
6. Add documentation by using the `@LuaDocumentation(description =\"Description here.\")` annotation on your methods.