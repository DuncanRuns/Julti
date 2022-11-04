--[[

    Julti OBS Link Script for OBS.
    
    The purpose of the OBS Link Script is to easily switch between scenes and change lock icon visibility based on the state of Julti.
    
    It is an alternative to OBS Websocket, or assigning hotkeys to each scene.
    It also replaces the complex system of replacing image files in instance folders with a simpler system of
    simply enabling/disabling the visibility of the lock icons.

    Follow these steps to setup:
    - Place the script in a safe place
    - Confirm you are in the correct wall scene collection
    - Add the script in OBS by going to Tools -> Scripts, then pressing the "+" at the bottom left
    - Fill in the script settings once added
    

    LICENSE BELOW:

    MIT License

    Copyright (c) 2022 DuncanRuns

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

]]

obs = obslua

-- Properties --

first_scene = ""
wall_scene  = ""
first_lock  = ""

-- Variables --

has_lock         = false
timers_activated = false
julti_dir        = os.getenv("UserProfile") .. "\\.Julti\\"
old_text         = ""

-- Functions --

function split_string(inp, sep)
    if sep == nil then
        sep = "%s"
    end
    local t = {}
    for str in string.gmatch(inp, "([^" .. sep .. "]+)") do
        table.insert(t, str)
    end
    return t
end

function read_file(filename)
    local rfile = io.open(filename, "r")
    if rfile == nil then
        return ""
    end
    io.input(rfile)
    local out = ""
    local next = io.read()
    while not (next == nil) do
        out = out .. next .. "\n"
        next = io.read()
    end
    io.close(rfile)
    return out
end

function write_file(filename, string)
    local wfile = io.open(filename, "w")
    io.output(wfile)
    io.write(string)
    io.close(wfile)
end

function get_julti_file(filename)
    return julti_dir .. filename
end

function can_take_lock()
    local last_update = tonumber(read_file(get_julti_file("scriptlock")))
    if (last_update == nil) then
        return true
    end
    if ((os.time() - last_update) > 3) then
        return true
    end
    return false
end

function write_new_lock()
    write_file(get_julti_file("scriptlock"), os.time())
end

-- Main OBS Functions --

function script_description()
    return "Links OBS to Julti.\n\nThe first OBS open using this script will actively switch to the relevant scene, as well as correctly enabling/disabling lock icons.\nAny extra opened OBS instances will remain on the wall scene.\n\nPlease note the lock image sources should just point to a single image file rather than one per instance as other macros tend to use.\n\nIf you don't want to use Wall, leave the wall option empty.\nIf you don't have any lock icons (or are using Julti's wall window), leave the first lock icon option empty."
end

function script_properties()
    local props = obs.obs_properties_create()

    local p = obs.obs_properties_add_list(props, "wall_scene", "Wall Scene", obs.OBS_COMBO_TYPE_EDITABLE,
        obs.OBS_COMBO_FORMAT_STRING)
    local scenes = obs.obs_frontend_get_scenes()
    if scenes ~= nil then
        for _, scene in ipairs(scenes) do
            local name = obs.obs_source_get_name(scene)
            obs.obs_property_list_add_string(p, name, name)
        end
    end
    obs.source_list_release(scenes)

    local p = obs.obs_properties_add_list(props, "first_scene", "First Instance Scene", obs.OBS_COMBO_TYPE_EDITABLE,
        obs.OBS_COMBO_FORMAT_STRING)
    local scenes = obs.obs_frontend_get_scenes()
    if scenes ~= nil then
        for _, scene in ipairs(scenes) do
            local name = obs.obs_source_get_name(scene)
            obs.obs_property_list_add_string(p, name, name)
        end
    end
    obs.source_list_release(scenes)



    local p = obs.obs_properties_add_list(props, "first_lock", "First Lock Source", obs.OBS_COMBO_TYPE_EDITABLE,
        obs.OBS_COMBO_FORMAT_STRING)
    local sources = obs.obs_enum_sources()
    if sources ~= nil then
        for _, source in ipairs(sources) do
            local source_id = obs.obs_source_get_id(source)
            if source_id == "image_source" then
                local name = obs.obs_source_get_name(source)
                obs.obs_property_list_add_string(p, name, name)
            end
        end
    end
    obs.source_list_release(sources)

    return props
end

function script_update(settings)
    wall_scene = obs.obs_data_get_string(settings, "wall_scene")
    first_scene = obs.obs_data_get_string(settings, "first_scene")
    first_lock = obs.obs_data_get_string(settings, "first_lock")
end

function script_load(settings)
    wall_scene = obs.obs_data_get_string(settings, "wall_scene")
    first_scene = obs.obs_data_get_string(settings, "first_scene")
    first_lock = obs.obs_data_get_string(settings, "first_lock")

    if timers_activated then
        return
    end

    -- Schedule Activate Loop Functions --

    obs.timer_add(loop20thsec, 50)
    obs.timer_add(loophalfsec, 500)
    obs.timer_add(aftersec, 1000)

    timers_activated = true
end

-- Loop Functions --

function loop20thsec()
    if not has_lock then
        return
    end

    local new_text = read_file(get_julti_file("state"))


    if (new_text == nil) or (new_text == old_text) then
        return
    end
    old_text = new_text

    local scene_id = split_string(new_text," ")[1]

    if not (scene_id == "W") then
        if not (first_scene == "") then
            local to_switch = (first_scene:sub(1, -2)) .. (scene_id)
            local scene_source = obs.obs_get_source_by_name(to_switch)
            obs.obs_frontend_set_current_scene(scene_source)
            obs.obs_source_release(scene_source)
        end
        return
    end

    if (wall_scene == "") then
        return
    end

    local scene_source = obs.obs_get_source_by_name(wall_scene)
    obs.obs_frontend_set_current_scene(scene_source)
    obs.obs_source_release(scene_source)

    if (first_lock == "") then
        return
    end

    local i = 0
    for c in split_string(new_text," ")[2]:gmatch(".") do
        i = i + 1
        local lock_name = first_lock:sub(1, -2) .. tostring(i)
        local scene_source = obs.obs_get_source_by_name(wall_scene)
        local scene = obs.obs_scene_from_source(scene_source)
        local scene_items = obs.obs_scene_enum_items(scene)
        for _, scene_item in ipairs(scene_items) do
            if (lock_name == obs.obs_source_get_name(obs.obs_sceneitem_get_source(scene_item))) then
                obs.obs_sceneitem_set_visible(scene_item, (c == "1"))
            end
        end
        obs.sceneitem_list_release(scene_items)
        obs.obs_source_release(scene_source)
    end

end

function loophalfsec()
    if not has_lock then
        if can_take_lock() then
            has_lock = true
            obs.script_log(300, "Julti script lock obtained!")
        else
            return
        end
    end
    write_new_lock()
end

function aftersec()

    obs.timer_remove(aftersec)
    
    -- Switch to Wall Scene & Disable All Lock Icons --

    if (wall_scene == "") then
        return
    end

    local scene_source = obs.obs_get_source_by_name(wall_scene)
    obs.obs_frontend_set_current_scene(scene_source)
    obs.obs_source_release(scene_source)

    if (first_lock == "") then
        return
    end

    -- Should loop until finding a scene item that does not exist but checking 101 happens basically instantly anyway plus I'm lazy.
    local i = 0
    while true do
        i = i + 1
        local lock_name = first_lock:sub(1, -2) .. tostring(i)
        local scene_source = obs.obs_get_source_by_name(wall_scene)
        local scene = obs.obs_scene_from_source(scene_source)
        local scene_items = obs.obs_scene_enum_items(scene)
        for _, scene_item in ipairs(scene_items) do
            if (lock_name == obs.obs_source_get_name(obs.obs_sceneitem_get_source(scene_item))) then
                obs.obs_sceneitem_set_visible(scene_item, false)
            end
        end
        obs.sceneitem_list_release(scene_items)
        obs.obs_source_release(scene_source)
        if i > 100 then
            break
        end
    end
    obs.script_log(300,"Switched to wall scene and cleared locks.")
end