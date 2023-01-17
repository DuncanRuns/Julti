--[[

    Julti OBS Link v1.1.0
    
    The purpose of the OBS Link Script is to generate and control a Julti scene to assist with multi-instance speedrunning.

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

-- Variables --

julti_dir = os.getenv("UserProfile"):gsub("\\","/") .. "/.Julti/"
timers_activated = false
last_state_text = ""
last_scene_name = ""

total_width = 0
total_height = 0

-- File Functions --

function read_first_line(filename)
    local rfile = io.open(filename, "r")
    if rfile == nil then
        return ""
    end
    io.input(rfile)
    local out = io.read()
    io.close(rfile)
    return out
end

function write_file(filename, string)
    local wfile = io.open(filename, "w")
    io.output(wfile)
    io.write(string)
    io.close(wfile)
end

function get_state_file_string()
    local success, result = pcall(read_first_line, julti_dir .. "state")
    if success then
        return result
    end
    return nil
end

function get_square_crop_string()
    local success, result = pcall(read_first_line, julti_dir .. "loadingsquarecrop")
    if success then
        return result
    end
    return nil
end

-- Instance --

function set_instance_data(num, lock_visible, cover_visible, x, y, width, height)
    local group = get_group_as_scene("Instance " .. num)

    -- Lock Display: visibility and crop
    local item = obs.obs_scene_find_source(group, "Lock Display")
    obs.obs_sceneitem_set_visible(item, lock_visible)
    set_crop(item, 0, 0, total_width - width, total_height - height)

    -- Dirt Cover: visibility and bounds
    local item = obs.obs_scene_find_source(group, "Dirt Cover Display")
    obs.obs_sceneitem_set_visible(item, cover_visible)
    set_position_with_bounds(item, 0, 0, width, height)

    -- Minecraft capture: position and bounds
    local item = obs.obs_scene_find_source(group, "Minecraft Capture " .. num)
    set_position_with_bounds(item, 0, 0, width, height)

    -- Instance Group: position
    local scene = get_scene("Julti")
    local item = obs.obs_scene_find_source(scene, "Instance " .. num)
    set_position(item, x, y)
end

function set_instance_data_from_string(instance_num, data_string)
    -- data_string format: lock/cover state (1 = lock, 2 = cover, 3 = both, 0 = neither), x, y, w, h
    -- Example: "2,0,0,960,540"
    local nums = split_string(data_string, ",")
    set_instance_data(
        instance_num, --instance number
        (nums[1] == "1") or (nums[1] == "3"), -- lock visible
        (nums[1] == "2") or (nums[1] == "3"), -- cover visible
        tonumber(nums[2]), -- x
        tonumber(nums[3]), -- y
        tonumber(nums[4]), -- width
        tonumber(nums[5])) -- height
end

-- Misc Functions --

function split_string(input_string, split_char)
    local out = {}
    -- https://stackoverflow.com/questions/1426954/split-string-in-lua
    for str in input_string.gmatch(input_string, "([^" .. split_char .. "]+)") do
        table.insert(out, str)
    end
    return out
end

-- Obs Functions --

function get_scene(name)
    local source = get_source(name)
    if source == nil then
        return nil
    end
    local scene = obs.obs_scene_from_source(source)
    release_source(source)
    return scene
end

function get_group_as_scene(name)
    local source = get_source(name)
    if source == nil then
        return nil
    end
    local scene = obs.obs_group_from_source(source)
    release_source(source)
    return scene
end

function remove_source_or_scene(name)
    local source = get_source(name)
    obs.obs_source_remove(source)
    release_source(source)
end

--- Requires release after use
function get_source(name)
    return obs.obs_get_source_by_name(name)
end

function release_source(source)
    obs.obs_source_release(source)
end

function release_scene(scene)
    obs.obs_scene_release(scene)
end

function scene_exists(name)
    return get_scene(name) ~= nil
end

function create_scene(name)
    release_scene(obs.obs_scene_create(name))
end

function switch_to_scene(scene_name)
    local scene_source = get_source(scene_name)
    if (scene_source == nil) then return end
    obs.obs_frontend_set_current_scene(scene_source)
    release_source(scene_source)
end

function get_video_info()
    local video_info = obs.obs_video_info()
    obs.obs_get_video_info(video_info)
    return video_info
end

function set_position_with_bounds(scene_item, x, y, width, height)
    local bounds = obs.vec2()
    bounds.x = width
    bounds.y = height
    obs.obs_sceneitem_set_bounds_type(scene_item, obs.OBS_BOUNDS_STRETCH)
    set_position(scene_item, x, y)
    obs.obs_sceneitem_set_bounds(scene_item, bounds)
end

function set_position(scene_item, x, y)
    local pos = obs.vec2()
    pos.x = x
    pos.y = y
    obs.obs_sceneitem_set_pos(scene_item, pos)
end

function set_crop(scene_item, left, top, right, bottom)
    local crop = obs.obs_sceneitem_crop()
    crop.left = left
    crop.top = top
    crop.right = right
    crop.bottom = bottom
    obs.obs_sceneitem_set_crop(scene_item, crop)
end

function get_sceneitem_name(sceneitem)
    return obs.obs_source_get_name(obs.obs_sceneitem_get_source(sceneitem))
end

function bring_to_top(item)
    obs.obs_sceneitem_set_order(item, obs.OBS_ORDER_MOVE_TOP)
end

function bring_to_bottom(item)
    obs.obs_sceneitem_set_order(item, obs.OBS_ORDER_MOVE_BOTTOM)
end

-- Scene Generator --

function generate_stream_scenes()
    local julti_source = get_source("Julti")

    if julti_source == nil then
        obs.script_log(200, "You must press the regular \"Generate Scenes\" button first!")
    end

    if not scene_exists("Playing") then
        create_scene("Playing")
        obs.obs_scene_add(get_scene("Playing"), julti_source)
    end
    if not scene_exists("Walling") then
        create_scene("Walling")
        local scene = get_scene("Walling")

        obs.obs_scene_add(scene, julti_source)

        local settings = obs.obs_data_create_from_json('{"file":"' ..
            julti_dir ..
            'resets.txt","font":{"face":"Arial","flags":0,"size":48,"style":"Regular"},"opacity":50,"read_from_file":true}')
        local counter_source = obs.obs_source_create("text_gdiplus", "Reset Counter", settings, nil)
        obs.obs_scene_add(scene, counter_source)
        release_source(counter_source)
        obs.obs_data_release(settings)
    end

    release_source(julti_source)

end

function generate_scenes()

    if not scene_exists("Lock Display") then
        _setup_lock_scene()
    else
        obs.script_log(200,
            'Warning: The lock display scene already exists, if you want it to be remade, please delete it and press the "Generate Scenes" button again.')
    end

    if not scene_exists("Dirt Cover Display") then
        _setup_cover_scene()
    else
        obs.script_log(200,
            'Warning: The dirt cover display scene already exists, if you want it to be remade, please delete it and press the "Generate Scenes" button again.')
    end

    if not scene_exists("Sound") then
        _setup_sound_scene()
    else
        obs.script_log(200,
            'Warning: The sound scene already exists, if you want it to be remade, please delete it and press the "Generate Scenes" button again.')
    end

    if not scene_exists("Verification") then
        _setup_verification_scene()
    else
        obs.script_log(200,
            'Warning: The verification scene already exists, if you want it to be remade, please delete it and press the "Generate Scenes" button again.')
    end

    _setup_julti_scene()
end

function _setup_cover_scene()
    create_scene("Dirt Cover Display")
    local scene = get_scene("Dirt Cover Display")

    local cover_data = obs.obs_data_create_from_json('{"file":"' .. julti_dir .. 'dirtcover.png' .. '"}')
    local cover_source = obs.obs_source_create("image_source", "Dirt Cover Image", cover_data, nil)
    obs.obs_scene_add(scene, cover_source)
    release_source(cover_source)
    obs.obs_data_release(cover_data)

    local item = obs.obs_scene_find_source(scene, "Dirt Cover Image")
    set_position_with_bounds(item, 0, 0, total_width, total_height)
    obs.obs_sceneitem_set_scale_filter(item, obs.OBS_SCALE_POINT)

end

function _setup_verification_scene()
    create_scene("Verification")
    local scene = get_scene("Verification")

    local out = get_state_file_string()
    if (out == nil) or not (string.find(out, ";")) then
        return
    end

    local square_crop_string = get_square_crop_string()
    if square_crop_string == nil then
        square_crop_string = "1830,270"
        obs.script_log(200, "Warning: Could not a loading square crop, defaulting to 1920x1080 squish level 3 crop.")
    end
    local square_crop = split_string(square_crop_string, ",")

    local instance_count = (#(split_string(out, ";"))) - 1

    if instance_count == 0 then
        return
    end

    local total_rows = math.floor(math.sqrt(instance_count)) - 1
    local total_columns = 0

    ::increase_again::
    total_rows = total_rows + 1
    total_columns = math.ceil(instance_count / total_rows)

    size_ratio = math.floor(total_width / total_columns) / math.floor(total_height / total_rows)

    -- No need to make more rows if it's already just a single column
    if total_columns == 1 then
        goto done
    end

    -- Size ratio is the ratio between width and height
    -- If there is not enough width relative to the height, the loading square would take too much space
    if size_ratio < 2.5 then
        goto increase_again
    end

    -- If there is 17.5% or more empty space from unfilled grid spaces, add another row
    missing = (total_rows * total_columns - instance_count) / (total_rows * total_columns)
    if missing > 0.175 then
        goto increase_again
    end

    ::done::
    local i_width = math.floor(total_width / total_columns)
    local i_height = math.floor(total_height / total_rows)

    for instance_num = 1, instance_count, 1 do
        local instance_index = instance_num - 1
        local row = math.floor(instance_index / total_columns)
        local col = math.floor(instance_index % total_columns)

        local settings = obs.obs_data_create_from_json('{"priority": 1, "window": "Minecraft* - Instance ' ..
            instance_num .. ':GLFW30:javaw.exe"}')
        local source = obs.obs_source_create("window_capture", "Verification Capture " .. instance_num, settings, nil)
        local item = obs.obs_scene_add(scene, source)
        local item2 = obs.obs_scene_add(scene, source)
        set_position_with_bounds(item, col * i_width, row * i_height, i_width - i_height, i_height)
        set_position_with_bounds(item2, col * i_width + (i_width - i_height), row * i_height, i_height, i_height)
        set_crop(item2, 0, square_crop[2], square_crop[1], 0)
        obs.obs_sceneitem_set_scale_filter(item2, obs.OBS_SCALE_POINT)
        obs.obs_data_release(settings)
    end

    local source = get_source("Minecraft Audio")
    obs.obs_scene_add(scene, source)
    release_source(source)
end

function _setup_sound_scene()
    create_scene("Sound")
    local scene = get_scene("Sound")

    -- intuitively the scene item returned from add group would need to be released, but it does not
    if get_group_as_scene("Minecraft Audio") == nil then
        obs.obs_scene_add_group(scene, "Minecraft Audio")
    else
        local source = get_source("Minecraft Audio")
        obs.obs_scene_add(scene, source)
        release_source(source)
    end
    obs.obs_sceneitem_set_visible(obs.obs_scene_find_source(scene, "Minecraft Audio"), false)

    local desk_cap = obs.obs_source_create("wasapi_output_capture", "Desktop Audio", nil, nil)
    obs.obs_scene_add(scene, desk_cap)
    release_source(desk_cap)
end

function _setup_lock_scene()
    create_scene("Lock Display")
    local scene = get_scene("Lock Display")

    -- Example Instances Group
    -- intuitively the scene item returned from add group would need to be released, but it does not
    obs.obs_scene_add_group(scene, "Example Instances")
    local group = get_group_as_scene("Example Instances")
    obs.obs_sceneitem_set_locked(obs.obs_scene_find_source(scene, "Example Instances"), true)

    -- Blacksmith Example
    local blacksmith_data = obs.obs_data_create_from_json('{"file":"' ..
        julti_dir .. 'blacksmith_example.png' .. '"}')
    local blacksmith_source = obs.obs_source_create("image_source", "Blacksmith Example", blacksmith_data, nil)
    obs.obs_scene_add(group, blacksmith_source)
    release_source(blacksmith_source)
    obs.obs_data_release(blacksmith_data)

    -- Beach Example
    local beach_data = obs.obs_data_create_from_json('{"file":"' .. julti_dir .. 'beach_example.png' .. '"}')
    local beach_source = obs.obs_source_create("image_source", "Beach Example", beach_data, nil)
    obs.obs_scene_add(group, beach_source)
    release_source(beach_source)
    obs.obs_data_release(beach_data)

    -- Darken
    local darken_data = obs.obs_data_create_from_json('{"color": 3355443200}')
    local darken_source = obs.obs_source_create("color_source", "Darken", darken_data, nil)
    obs.obs_scene_add(scene, darken_source)
    release_source(darken_source)
    obs.obs_data_release(darken_data)
    local item = obs.obs_scene_find_source(scene, "Darken")
    obs.obs_sceneitem_set_visible(item, false)
    set_position_with_bounds(item, 0, 0, total_width, total_height)

    -- Lock image
    local lock_data = obs.obs_data_create_from_json('{"file":"' ..
        julti_dir .. 'lock.png' .. '"}')
    local lock_source = obs.obs_source_create("image_source", "Lock Image", lock_data, nil)
    obs.obs_scene_add(scene, lock_source)
    release_source(lock_source)
    obs.obs_data_release(lock_data)
    set_position_with_bounds(obs.obs_scene_find_source(scene, "Lock Image"), 20, 20, 130, 130)
end

function _setup_julti_scene()
    local out = get_state_file_string()
    if (out == nil) or not (string.find(out, ";")) then
        obs.script_log(100, "Julti has not yet been set up! Please setup Julti first!")
        return
    end

    local instance_count = (#(split_string(out, ";"))) - 1

    if instance_count == 0 then
        obs.script_log(100, "Julti has not yet been set up (No instances found)! Please setup Julti first!")
        return
    end

    if scene_exists("Julti") then
        local items = obs.obs_scene_enum_items(get_scene("Julti"))
        for _, item in ipairs(items) do
            if (string.find(get_sceneitem_name(item), "Instance ") ~= nil) or
                (string.find(get_sceneitem_name(item), "Sound") ~= nil) then
                obs.obs_sceneitem_remove(item)
            end
        end
        obs.sceneitem_list_release(items)
    else
        create_scene("Julti")
    end

    for i = 1, instance_count, 1 do
        make_minecraft_group(i, total_width, total_height)
    end

    local sound_scene_source = get_source("Sound")
    obs.obs_scene_add(get_scene("Julti"), sound_scene_source)
    release_source(sound_scene_source)
    bring_to_bottom(obs.obs_scene_find_source(get_scene("Julti"), "Sound"))

    _setup_minecraft_sounds(instance_count)

    switch_to_scene("Julti")
end

function _setup_minecraft_sounds(instance_count)
    local group = get_group_as_scene("Minecraft Audio")

    local items = obs.obs_scene_enum_items(group)
    for _, item in ipairs(items) do
        obs.obs_sceneitem_remove(item)
    end
    obs.sceneitem_list_release(items)

    for num = 1, instance_count, 1 do
        -- '{"priority": 1, "window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}'
        local settings = obs.obs_data_create_from_json('{"priority": 1, "window": "Minecraft* - Instance ' ..
            num .. ':GLFW30:javaw.exe"}')
        local source = obs.obs_source_create("wasapi_process_output_capture", "Minecraft Audio " .. num, settings, nil)
        obs.obs_scene_add(group, source)
        release_source(source)
        obs.obs_data_release(settings)
    end
end

function make_minecraft_group(num, width, height)
    local scene = get_scene("Julti")

    -- intuitively the scene item returned from add group would need to be released, but it does not
    local group_si = obs.obs_scene_add_group(scene, "Instance " .. num)

    local source = get_source("Lock Display")
    local ldsi = obs.obs_scene_add(scene, source)
    obs.obs_sceneitem_group_add_item(group_si, ldsi)
    release_source(source)

    local source = get_source("Dirt Cover Display")
    obs.obs_sceneitem_group_add_item(group_si, obs.obs_scene_add(scene, source))
    release_source(source)

    local settings = obs.obs_data_create_from_json('{"capture_mode": "window","priority": 1,"window": "Minecraft* - Instance '
        .. num .. ':GLFW30:javaw.exe"}')
    local source = obs.obs_source_create("game_capture", "Minecraft Capture " .. num, settings, nil)
    obs.obs_data_release(settings)
    local mcsi = obs.obs_scene_add(scene, source)
    obs.obs_sceneitem_group_add_item(group_si, mcsi)
    set_position_with_bounds(mcsi, 0, 0, width, height)
    set_instance_data(num, false, false, 0, 0, width, height)
    release_source(source)
end

-- Script Functions --

function script_description()
    return "<h1>Julti OBS Link</h1><p>Links OBS to Julti.</p>"
end

function script_properties()
    local properties = obs.obs_properties_create()
    local generate_scenes_button = obs.obs_properties_add_button(
        properties, "generate_scenes_button", "Generate Scenes", generate_scenes)
    local generate_stream_scenes_button = obs.obs_properties_add_button(
        properties, "generate_stream_scenes_button", "Generate Stream Scenes", generate_stream_scenes)
    return properties
end

function script_load(settings)
    local video_info = get_video_info()
    total_width = video_info.base_width
    total_height = video_info.base_height

    pcall(write_file, julti_dir .. "obsscenesize", total_width .. "," .. total_height)

    switch_to_scene("Julti")
end

function script_update(settings)
    if timers_activated then
        return
    end
    obs.timer_add(loop, 20)
end

function loop()

    -- Scene Change Check

    local current_scene_source = obs.obs_frontend_get_current_scene()
    local current_scene_name = obs.obs_source_get_name(current_scene_source)
    release_source(current_scene_source)
    if last_scene_name ~= current_scene_name then
        on_scene_change(last_scene_name, current_scene_name)
        last_scene_name = current_scene_name
    end

    -- Check on Julti scene before continuing

    local is_on_a_julti_scene = (current_scene_name == "Julti") or (current_scene_name == "Lock Display") or
        (current_scene_name == "Dirt Cover Display") or (current_scene_name == "Walling") or
        (current_scene_name == "Playing")

    if not is_on_a_julti_scene then
        return
    end

    -- Get state output

    local out = get_state_file_string()
    if out ~= nil and last_state_text ~= out then
        last_state_text = out
    else
        return
    end

    -- Process state data

    local data_strings = split_string(out, ";")
    local user_location = nil
    local instance_num = 0
    for k, data_string in pairs(data_strings) do
        if user_location == nil then
            user_location = data_string
        else
            instance_num = instance_num + 1
            set_instance_data_from_string(instance_num, data_string)
        end
    end

    if user_location == "W" then
        if (scene_exists("Walling")) then
            switch_to_scene("Walling")
        else
            switch_to_scene("Julti")
        end
    else
        if (scene_exists("Playing")) then
            switch_to_scene("Playing")
        else
            switch_to_scene("Julti")
        end
    end

    if user_location ~= "W" then
        local scene = get_scene("Julti")
        bring_to_top(obs.obs_scene_find_source(scene, "Instance " .. user_location))
        set_instance_data(tonumber(user_location), false, false, 0, 0, total_width, total_height)
    end

end

function on_scene_change(last_scene_name, new_scene_name)

    if new_scene_name == "Lock Display" then

        local state = get_state_file_string()
        if state == nil then
            return
        end
        local data_strings = split_string(state, ";")
        if #data_strings == 1 then
            return
        end
        local nums = split_string(data_strings[2], ",")

        local scene = get_scene("Lock Display")
        local item = obs.obs_scene_find_source(scene, "Example Instances")
        obs.obs_sceneitem_set_visible(item, true)
        set_position_with_bounds(item, 0, 0, tonumber(nums[4]), tonumber(nums[5]))

    elseif last_scene_name == "Lock Display" then

        local scene = get_scene("Lock Display")
        local item = obs.obs_scene_find_source(scene, "Example Instances")
        obs.obs_sceneitem_set_visible(item, false)

    end

end

