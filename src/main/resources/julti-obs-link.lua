--[[

    Julti OBS Link v1.2.0
    
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

---- Variables ----

julti_dir = os.getenv("UserProfile"):gsub("\\", "/") .. "/.Julti/"
timers_activated = false
last_state_text = ""
last_scene_name = ""

gen_scenes_requested = false
gen_stream_scenes_requested = false

total_width = 0
total_height = 0


-- Script Settings
win_cap_instead = false
reuse_for_verification = false
invisible_dirt_covers = false
center_align_instances = false
show_indicators = false


-- Constants
ALIGN_TOP_LEFT = 5 -- equivalent to obs.OBS_ALIGN_TOP | obs.OBS_ALIGN_LEFT
ALIGN_CENTER = 0   -- equivalent to obs.OBS_ALIGN_CENTER

---- File Functions ----

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

function get_square_size_string()
    local success, result = pcall(read_first_line, julti_dir .. "loadingsquaresize")
    if success then
        return result
    end
    return nil
end

---- Instance ----

function teleport_off_canvas(num)
    local scene = get_scene("Julti")
    local item = obs.obs_scene_find_source(scene, "Instance " .. num)
    set_position(item, total_width + 1000, 0)
end

function set_instance_data(num, lock_visible, dirt_cover, freeze_active, x, y, width, height, center_align)
    center_align = center_align or false

    local group = get_group_as_scene("Instance " .. num)

    if invisible_dirt_covers and dirt_cover then
        teleport_off_canvas(num)
        return
    end

    -- Lock Display: visibility, position and crop
    local item = obs.obs_scene_find_source(group, "Lock Display")
    obs.obs_sceneitem_set_visible(item, lock_visible)
    set_position(item, 0, 0)
    set_crop(item, 0, 0, total_width - width, total_height - height)

    -- Dirt Cover: visibility and bounds
    local item = obs.obs_scene_find_source(group, "Dirt Cover Display")
    obs.obs_sceneitem_set_visible(item, dirt_cover)
    set_position_with_bounds(item, 0, 0, width, height, center_align)

    -- Minecraft capture: position, bounds
    local item = obs.obs_scene_find_source(group, "Minecraft Capture " .. num)
    set_position_with_bounds(item, 0, 0, width, height, center_align)

    -- Freeze filter activation for minecraft capture
    local source = obs.obs_sceneitem_get_source(item)
    local filter = obs.obs_source_get_filter_by_name(source, "Freeze filter")
    if filter == nil and freeze_active then
        local settings = obs.obs_data_create()
        filter = obs.obs_source_create_private("freeze_filter", "Freeze filter", settings)
        obs.obs_source_filter_add(source, filter)
    end
    if not (filter == nil) then
        obs.obs_source_set_enabled(filter, freeze_active and not lock_visible)
    end

    -- Instance Group: position
    local scene = get_scene("Julti")
    local item = obs.obs_scene_find_source(scene, "Instance " .. num)
    set_position(item, x, y)
end

function set_instance_data_from_string(instance_num, data_string)
    -- data_string format: lock/cover state (flag bits: locked, dirt cover, freeze filter), x, y, w, h
    -- Example: "2,0,0,960,540"
    local nums = split_string(data_string, ",")

    local flagBits = tonumber(nums[1])

    local freezeActive = flagBits >= 4
    if freezeActive then
        flagBits = flagBits - 4
    end

    local coverVisible = flagBits >= 2
    if coverVisible then
        flagBits = flagBits - 2
    end

    local lockVisible = flagBits >= 1
    if lockVisible then
        flagBits = flagBits - 1
    end

    set_instance_data(
        instance_num,      -- instance number
        lockVisible,       -- lock visible
        coverVisible,      -- cover visible
        freezeActive,      -- freeze filter active
        tonumber(nums[2]), -- x
        tonumber(nums[3]), -- y
        tonumber(nums[4]), -- width
        tonumber(nums[5])  -- height
    )
end

---- Instance Indicators ----

function disable_all_indicators()
    num = 0
    while true do
        num = num + 1
        local group = get_group_as_scene("Instance " .. num)
        if group == nil then
            return
        end
        local si = obs.obs_scene_find_source(group, "Instance " .. num .. " Indicator")
        if si ~= nil then
            obs.obs_sceneitem_set_visible(si, false)
        end
    end
end

function enable_indicators(instance_count)
    for num = 1, instance_count, 1 do
        local group = get_group_as_scene("Instance " .. num)
        if group == nil then
            return
        end
        local si = obs.obs_scene_find_source(group, "Instance " .. num .. " Indicator")
        if si ~= nil then
            obs.obs_sceneitem_set_visible(si, true)
        end
    end
end

function enable_indicator(num)
    local group = get_group_as_scene("Instance " .. num)
    if group == nil then
        return
    end
    local si = obs.obs_scene_find_source(group, "Instance " .. num .. " Indicator")
    if si ~= nil then
        obs.obs_sceneitem_set_visible(si, true)
    end
end

---- Misc Functions ----

function split_string(input_string, split_char)
    local out = {}
    -- https://stackoverflow.com/questions/1426954/split-string-in-lua
    for str in input_string.gmatch(input_string, "([^" .. split_char .. "]+)") do
        table.insert(out, str)
    end
    return out
end

---- Multi Scene Generator ----

function generate_multi_scenes()
    local instance_count = 0
    ::go_again::
    local temp_source = get_source("Minecraft Capture " .. (instance_count + 1))
    if temp_source ~= nil then
        instance_count = instance_count + 1
        release_source(temp_source)
        goto go_again
    end


    if instance_count == 0 then
        obs.script_log(100, "You have not generated regular scenes yet!")
        return
    end

    remove_individual_multi_captures()

    gen_overlay_scene()

    for i = 1, instance_count, 1 do
        generate_multi_playing_scene(i)
    end
end

function gen_overlay_scene()
    local scene = get_scene("Playing Overlay")
    if (scene ~= nil) then
        return
    end
    create_scene("Playing Overlay")
    local num_over = get_source("Current Location")
    if num_over == nil then
        local settings = obs.obs_data_create_from_json(
            '{"file":"' .. julti_dir ..
            'currentlocation.txt","font":{"face":"Arial","flags":0,"size":48,"style":"Regular"},"opacity":15,"read_from_file":true}'
        )
        num_over = obs.obs_source_create("text_gdiplus", "Current Location", settings, nil)
    end
    obs.obs_scene_add(get_scene("Playing Overlay"), num_over)
    release_source(num_over)
end

function generate_multi_playing_scene(i)
    local scene = get_scene("Playing " .. i)

    if (scene == nil) then
        create_scene("Playing " .. i)
        scene = get_scene("Playing " .. i)

        local source = get_source("Sound")
        obs.obs_scene_add(scene, source)
        release_source(source)

        local source = get_source("Playing Overlay")
        obs.obs_scene_add(scene, source)
        release_source(source)
    end

    local source = get_source("Minecraft Capture " .. i)
    local si = obs.obs_scene_add(scene, source)
    bring_to_bottom(si)
    release_source(source)
end

function remove_individual_multi_captures()
    local i = 0
    local scene = nil

    ::go_again::
    i = i + 1
    scene = get_scene("Playing " .. i)

    if scene == nil then
        return
    end

    local si = obs.obs_scene_find_source(scene, "Minecraft Capture " .. i)
    if (si == nil) then
        goto go_again
    end

    obs.obs_sceneitem_remove(si)

    goto go_again
end

function regenerate_multi_scenes()
    local scene = get_scene("Playing 1")
    if scene == nil then
        return
    end
    remove_individual_multi_captures()
    generate_multi_scenes()
end

---- Obs Functions ----

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
    if (scene_source == nil) then return false end
    obs.obs_frontend_set_current_scene(scene_source)
    release_source(scene_source)
    return true
end

function get_video_info()
    local video_info = obs.obs_video_info()
    obs.obs_get_video_info(video_info)
    return video_info
end

function set_position_with_bounds(scene_item, x, y, width, height, center_align)
    -- default value false
    center_align = center_align or false

    local bounds = obs.vec2()
    bounds.x = width
    bounds.y = height

    if center_align then
        obs.obs_sceneitem_set_bounds_type(scene_item, obs.OBS_BOUNDS_NONE)
    else
        obs.obs_sceneitem_set_bounds_type(scene_item, obs.OBS_BOUNDS_STRETCH)
        obs.obs_sceneitem_set_bounds(scene_item, bounds)
    end

    -- set alignment of the scene item to: center_align ? CENTER : TOP_LEFT
    obs.obs_sceneitem_set_alignment(scene_item, center_align and ALIGN_CENTER or ALIGN_TOP_LEFT)

    set_position(scene_item, x + (center_align and total_width / 2 or 0), y + (center_align and total_height / 2 or 0))
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

function delete_source(name)
    local source = get_source(name)
    if (source ~= nil) then
        obs.obs_source_remove(source)
        release_source(source)
    end
end

---- Scene Generator ----

function request_generate_stream_scenes()
    gen_stream_scenes_requested = true
end

function generate_stream_scenes()
    local julti_source = get_source("Julti")

    if julti_source == nil then
        obs.script_log(200, "You must press the regular \"Generate Scenes\" button first!")
        return
    end

    if not scene_exists("Playing") then
        create_scene("Playing")
        obs.obs_scene_add(get_scene("Playing"), julti_source)
    end
    if not scene_exists("Walling") then
        create_scene("Walling")
        local scene = get_scene("Walling")

        obs.obs_scene_add(scene, julti_source)

        local settings = obs.obs_data_create_from_json(
            '{"file":"' .. julti_dir ..
            'resets.txt","font":{"face":"Arial","flags":0,"size":48,"style":"Regular"},"opacity":50,"read_from_file":true}'
        )
        local counter_source = obs.obs_source_create("text_gdiplus", "Reset Counter", settings, nil)
        obs.obs_scene_add(scene, counter_source)
        release_source(counter_source)
        obs.obs_data_release(settings)
    end

    release_source(julti_source)
end

function request_generate_scenes()
    gen_scenes_requested = true
end

function generate_scenes()
    local already_existing = {}
    local found_ae = false

    local out = get_state_file_string()
    if (out == nil) or not (string.find(out, ";")) then
        obs.script_log(200, "------------------------------")
        obs.script_log(100, "Julti has not yet been set up! Please setup Julti first!")
        return
    end
    local instance_count = (#(split_string(out, ";"))) - 2

    if not scene_exists("Lock Display") then
        _setup_lock_scene()
    else
        table.insert(already_existing, "Lock Display")
        found_ae = true
    end

    -- Check if current scene is on Julti, which messes things up
    local current_scene_source = obs.obs_frontend_get_current_scene()
    local current_scene_name = obs.obs_source_get_name(current_scene_source)
    release_source(current_scene_source)
    if current_scene_name == "Julti" then
        switch_to_scene("Lock Display")
        gen_scenes_requested = true
        return
    end

    if not scene_exists("Dirt Cover Display") then
        _setup_cover_scene()
    else
        table.insert(already_existing, "Dirt Cover Display")
        found_ae = true
    end

    if not scene_exists("Sound") then
        _setup_sound_scene()
    else
        table.insert(already_existing, "Sound")
        found_ae = true
    end

    remove_individual_multi_captures()

    _ensure_empty_important_scenes()

    _setup_julti_scene()

    _setup_verification_scene()

    regenerate_multi_scenes()

    disable_all_indicators()


    -- Reset variables to have loop update stuff automatically
    last_state_text = ""
    last_scene_name = ""

    if found_ae then
        -- Report already existing scenes

        obs.script_log(200, "------------------------------")
        obs.script_log(200, "The following scenes already exist:")

        for _, v in pairs(already_existing) do
            obs.script_log(200, "- " .. v)
        end

        obs.script_log(200, "If you want to recreate these scenes,")
        obs.script_log(200, "delete them first before pressing Generate Scenes.")
    end
end

function _fix_bad_names(instance_count)
    for i = 1, instance_count, 1 do
        local source = get_source("Minecraft Capture " .. i)
        if source == nil then
            obs.script_log(200, "Fixing " .. i)
            source = get_source("Minecraft Capture " .. i .. " 2")
            local data = obs.obs_source_get_settings(source)
            local a = obs.obs_data_get_json(data)
            obs.script_log(200, "stuff " .. a)
            obs.obs_data_set_string(data, "name", "Minecraft Capture " .. i)
            obs.obs_data_release(data)
        end
        release_source(source)
    end
end

function _ensure_empty_important_scenes()
    for instance_num = 1, 100 do
        -- Empty square groups
        local group_scene = get_group_as_scene("Square " .. instance_num)
        if (group_scene ~= nil) then
            local items = obs.obs_scene_enum_items(group_scene)
            for _, item in ipairs(items) do
                obs.obs_sceneitem_remove(item)
            end
            obs.sceneitem_list_release(items)
        end

        -- Empty instance groups
        local group_scene = get_group_as_scene("Instance " .. instance_num)
        if (group_scene ~= nil) then
            local items = obs.obs_scene_enum_items(group_scene)
            for _, item in ipairs(items) do
                obs.obs_sceneitem_remove(item)
            end
            obs.sceneitem_list_release(items)
        end

        -- Delete sources
        delete_source("Minecraft Capture " .. instance_num)
        delete_source("Verification Capture " .. instance_num)
        delete_source("Minecraft Audio " .. instance_num)
        delete_source("Instance " .. instance_num)
        delete_source("Square " .. instance_num)
    end
    -- Empty sound group
    local group_scene = get_group_as_scene("Minecraft Audio")
    if (group_scene ~= nil) then
        local items = obs.obs_scene_enum_items(group_scene)
        for _, item in ipairs(items) do
            obs.obs_sceneitem_remove(item)
        end
        obs.sceneitem_list_release(items)
    end
    -- Empty Verification scene
    if scene_exists("Verification") then
        obs.script_log(200, "------------------------------")
        obs.script_log(200, "Verification scene already existed,")
        obs.script_log(200, "all sources will be replaced.")
        local scene = get_scene("Verification")
        local items = obs.obs_scene_enum_items(scene)
        for _, item in ipairs(items) do
            obs.obs_sceneitem_remove(item)
        end
        obs.sceneitem_list_release(items)
    else
        create_scene("Verification")
    end
    -- Empty Julti scene
    if scene_exists("Julti") then
        obs.script_log(200, "------------------------------")
        obs.script_log(200, "Julti scene already existed,")
        obs.script_log(200, "all sources will be replaced.")
        local scene = get_scene("Julti")
        local items = obs.obs_scene_enum_items(scene)
        for _, item in ipairs(items) do
            obs.obs_sceneitem_remove(item)
        end
        obs.sceneitem_list_release(items)
    else
        create_scene("Julti")
    end
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
    local scene = get_scene("Verification")

    local out = get_state_file_string()
    if (out == nil) or not (string.find(out, ";")) then
        return
    end

    local square_size_string = get_square_size_string()
    if square_size_string == nil then
        square_size_string = "90,90"
        obs.script_log(200, "Warning: Could not a loading square size, defaulting to 90x90.")
    end
    local square_size = split_string(square_size_string, ",")
    local square_width = square_size[1]
    local square_height = square_size[2]

    if square_width == nil or square_height == nil then
        square_width = 90
        square_height = 90
        obs.script_log(200, "Warning: Could not a loading square size, defaulting to 90x90.")
    end

    local instance_count = (#(split_string(out, ";"))) - 2

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

    local filler_source = obs.obs_source_create("color_source", "Filler", nil, nil)


    for instance_num = 1, instance_count, 1 do
        local instance_index = instance_num - 1
        local row = math.floor(instance_index / total_columns)
        local col = math.floor(instance_index % total_columns)

        local verif_cap_source = nil

        if reuse_for_verification then
            verif_cap_source = get_source("Minecraft Capture " .. instance_num)
        else
            local settings = obs.obs_data_create_from_json('{"priority": 1, "window": "Minecraft* - Instance ' ..
                instance_num .. ':GLFW30:javaw.exe"}')
            verif_cap_source = obs.obs_source_create("window_capture", "Verification Capture " .. instance_num,
                settings, nil)
        end

        local verif_item = obs.obs_scene_add(scene, verif_cap_source)
        set_position_with_bounds(verif_item, col * i_width, row * i_height, i_width - i_height, i_height)


        local square_group_item = obs.obs_scene_add_group(scene, "Square " .. instance_num)
        obs.obs_sceneitem_set_scale_filter(square_group_item, obs.OBS_SCALE_POINT)

        local square_group_scene = get_group_as_scene("Square " .. instance_num)

        local filler_item = obs.obs_scene_add(square_group_scene, filler_source)
        set_position_with_bounds(filler_item, 0, 0, 10000, 10000)
        obs.obs_sceneitem_set_visible(filler_item, false)

        local verif_item_2 = obs.obs_scene_add(square_group_scene, verif_cap_source)
        set_position(verif_item_2, 0, 10000)
        obs.obs_sceneitem_set_alignment(verif_item_2, 9)

        set_crop(square_group_item, 0, 10000 - square_height, 10000 - square_width, 0)
        set_position_with_bounds(square_group_item, col * i_width + (i_width - i_height), row * i_height, i_height,
            i_height)

        obs.obs_data_release(settings)
        release_source(verif_cap_source)
    end
    local audio_group_source = get_source("Minecraft Audio")
    obs.obs_scene_add(scene, audio_group_source)
    release_source(filler_source)
    release_source(audio_group_source)
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
    local group_source = obs.obs_scene_find_source(scene, "Example Instances")
    obs.obs_sceneitem_set_locked(group_source, true)
    obs.obs_sceneitem_set_visible(group_source, false)

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

    local instance_count = (#(split_string(out, ";"))) - 2

    if instance_count == 0 then
        obs.script_log(100, "Julti has not yet been set up (No instances found)! Please setup Julti first!")
        return
    end

    local y = 0
    local i_height = math.floor(total_height / instance_count)
    for i = 1, instance_count, 1 do
        make_minecraft_group(i, total_width, total_height, y, i_height)
        y = y + i_height
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

    for num = 1, instance_count, 1 do
        -- '{"priority": 1, "window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}'
        local settings = obs.obs_data_create_from_json('{"priority": 1, "window": "Minecraft* - Instance ' ..
            num .. ':GLFW30:javaw.exe"}')
        local source = get_source("Minecraft Audio " .. num)
        if (source == nil) then
            source = obs.obs_source_create("wasapi_process_output_capture", "Minecraft Audio " .. num, settings, nil)
        end
        obs.obs_scene_add(group, source)
        release_source(source)
        obs.obs_data_release(settings)
    end
end

function make_minecraft_group(num, total_width, total_height, y, i_height)
    local scene = get_scene("Julti")

    -- intuitively the scene item returned from add group would need to be released, but it does not
    local group_si = obs.obs_scene_add_group(scene, "Instance " .. num)

    local num_overlay_name = "Instance " .. num .. " Indicator"
    local source = get_source(num_overlay_name)
    if source == nil then
        local settings = obs.obs_data_create_from_json(
            '{"text": "' .. num .. '","font": {"face": "Arial","flags": 0,"size": 48,"style": "Regular"},"opacity": 15}'
        )
        source = obs.obs_source_create("text_gdiplus", num_overlay_name, settings, nil)
    end
    local indicator_item = obs.obs_scene_add(scene, source)
    obs.obs_sceneitem_group_add_item(group_si, indicator_item)
    set_position(indicator_item, 5, 0)
    release_source(source)

    local source = get_source("Lock Display")
    obs.obs_sceneitem_group_add_item(group_si, obs.obs_scene_add(scene, source))
    release_source(source)

    local source = get_source("Dirt Cover Display")
    obs.obs_sceneitem_group_add_item(group_si, obs.obs_scene_add(scene, source))
    release_source(source)

    local settings = nil
    local source = nil

    if win_cap_instead then
        settings = obs.obs_data_create_from_json('{"priority": 1, "window": "Minecraft* - Instance ' ..
            num .. ':GLFW30:javaw.exe"}')
        source = obs.obs_source_create("window_capture", "Minecraft Capture " .. num, settings, nil)
    else
        settings = obs.obs_data_create_from_json(
            '{"capture_mode": "window","priority": 1,"window": "Minecraft* - Instance '
            .. num .. ':GLFW30:javaw.exe"}')
        source = obs.obs_source_create("game_capture", "Minecraft Capture " .. num, settings, nil)
    end

    obs.obs_source_filter_remove(source, obs.obs_source_get_filter_by_name(source, "Freeze filter"))

    obs.obs_data_release(settings)
    local mcsi = obs.obs_scene_add(scene, source)
    obs.obs_sceneitem_group_add_item(group_si, mcsi)
    set_position_with_bounds(mcsi, 0, 0, total_width, total_height)
    set_instance_data(num, false, false, false, 0, y, total_width, i_height)
    release_source(source)
end

---- Script Functions ----

function script_description()
    return "<h1>Julti OBS Link</h1><p>Links OBS to Julti.</p>"
end

function script_properties()
    local props = obs.obs_properties_create()

    obs.obs_properties_add_bool(props, "win_cap_instead",
        "[Generation Option]\nUse Window Capture for Julti Scene Sources")
    obs.obs_properties_add_bool(props, "reuse_for_verification",
        "[Generation Option]\nReuse Julti Scene Sources for Verification Scene\n(Better for source record or window cap)")

    obs.obs_properties_add_button(
        props, "generate_scenes_button", "Generate Scenes", request_generate_scenes)
    obs.obs_properties_add_button(
        props, "generate_stream_scenes_button", "Generate Stream Scenes", request_generate_stream_scenes)
    obs.obs_properties_add_button(
        props, "generate_multi_scenes_button", "Generate Multi Scenes (1 scene per instance)", generate_multi_scenes)

    -- Moved into Julti options
    -- obs.obs_properties_add_bool(props, "invisible_dirt_covers", "Invisible Dirt Covers")
    -- obs.obs_properties_add_bool(props, "center_align_instances",
    --     "Align Active Instance to Center\n(for EyeZoom/stretched window users)")

    return props
end

function script_load(settings)
    local video_info = get_video_info()
    total_width = video_info.base_width
    total_height = video_info.base_height

    pcall(write_file, julti_dir .. "obsscenesize", total_width .. "," .. total_height)

    switch_to_scene("Julti")
end

function script_update(settings)
    win_cap_instead = obs.obs_data_get_bool(settings, "win_cap_instead")
    reuse_for_verification = obs.obs_data_get_bool(settings, "reuse_for_verification")
    center_align_instances = obs.obs_data_get_bool(settings, "center_align_instances")
    invisible_dirt_covers = obs.obs_data_get_bool(settings, "invisible_dirt_covers")

    if timers_activated then
        return
    end
    timers_activated = true
    obs.timer_add(loop, 20)
end

function loop()
    -- Check Gen Requests
    if gen_scenes_requested then
        gen_scenes_requested = false
        generate_scenes()
    end
    if gen_stream_scenes_requested then
        gen_stream_scenes_requested = false
        generate_stream_scenes()
    end

    -- Scene Change Check

    local current_scene_source = obs.obs_frontend_get_current_scene()
    local current_scene_name = obs.obs_source_get_name(current_scene_source)
    release_source(current_scene_source)
    if last_scene_name ~= current_scene_name then
        on_scene_change(last_scene_name, current_scene_name)
        last_scene_name = current_scene_name
    end

    -- Check doing stuff too early
    if current_scene_name == nil then
        return
    end

    -- Check on Julti scene before continuing

    local is_on_a_julti_scene = (current_scene_name == "Julti") or (current_scene_name == "Lock Display") or
        (current_scene_name == "Dirt Cover Display") or (current_scene_name == "Walling") or
        (current_scene_name == "Playing") or (string.find(current_scene_name, "Playing ") ~= nil)

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
    local instance_count = (#data_strings) - 2
    local user_location = nil
    local option_bits_unset = true
    local instance_num = 0
    for k, data_string in pairs(data_strings) do
        if user_location == nil then
            -- Should take first item from data_strings
            user_location = data_string
            -- Prevent wall updates if switching to a single instance scene to allow transitions to work
            if user_location ~= "W" and switch_to_scene("Playing " .. user_location) then
                return
            end
        elseif option_bits_unset then
            -- Should take second item from data_strings
            option_bits_unset = false
            set_globals_from_bits(tonumber(data_string))
        else
            instance_num = instance_num + 1
            set_instance_data_from_string(instance_num, data_string)
        end
    end

    disable_all_indicators()

    if user_location == "W" then
        if show_indicators then enable_indicators(instance_count) end
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
        if show_indicators then enable_indicator(user_location) end
        bring_to_top(obs.obs_scene_find_source(scene, "Instance " .. user_location))
        set_instance_data(tonumber(user_location), false, false, false, 0, 0, total_width, total_height,
            center_align_instances)

        -- hide bordering instances
        if not center_align_instances then
            return
        end
        for k, data_string in pairs(data_strings) do
            if k == tonumber(user_location) then
                goto continue
            end
            teleport_off_canvas(k)
            ::continue::
        end
    end
end

function set_globals_from_bits(flag_int)
    show_indicators = flag_int >= 4
    if show_indicators then
        flag_int = flag_int - 4
    end
    center_align_instances = flag_int >= 2
    if center_align_instances then
        flag_int = flag_int - 2
    end
    invisible_dirt_covers = flag_int >= 1
    if invisible_dirt_covers then
        flag_int = flag_int - 1
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
        local nums = split_string(data_strings[3], ",")

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

