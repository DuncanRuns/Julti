package xyz.duncanruns.julti.util;

import com.sun.jna.platform.win32.Win32VK;

import java.util.Hashtable;

public final class McKeyUtil {
    public static final Hashtable<String, Integer> TRANSLATIONS_TO_GLFW = mapTranslationsToGLFW();
    public static final Hashtable<Integer, Integer> GLFW_TO_VK = mapGLFWToVK();

    private McKeyUtil() {
    }

    private static Hashtable<String, Integer> mapTranslationsToGLFW() {
        Hashtable<String, Integer> table = new Hashtable<>();
        table.put("key.keyboard.unknown", -1);
        table.put("key.mouse.left", 0);
        table.put("key.mouse.right", 1);
        table.put("key.mouse.middle", 2);
        table.put("key.mouse.4", 3);
        table.put("key.mouse.5", 4);
        table.put("key.mouse.6", 5);
        table.put("key.mouse.7", 6);
        table.put("key.mouse.8", 7);
        table.put("key.keyboard.0", 48);
        table.put("key.keyboard.1", 49);
        table.put("key.keyboard.2", 50);
        table.put("key.keyboard.3", 51);
        table.put("key.keyboard.4", 52);
        table.put("key.keyboard.5", 53);
        table.put("key.keyboard.6", 54);
        table.put("key.keyboard.7", 55);
        table.put("key.keyboard.8", 56);
        table.put("key.keyboard.9", 57);
        table.put("key.keyboard.a", 65);
        table.put("key.keyboard.b", 66);
        table.put("key.keyboard.c", 67);
        table.put("key.keyboard.d", 68);
        table.put("key.keyboard.e", 69);
        table.put("key.keyboard.f", 70);
        table.put("key.keyboard.g", 71);
        table.put("key.keyboard.h", 72);
        table.put("key.keyboard.i", 73);
        table.put("key.keyboard.j", 74);
        table.put("key.keyboard.k", 75);
        table.put("key.keyboard.l", 76);
        table.put("key.keyboard.m", 77);
        table.put("key.keyboard.n", 78);
        table.put("key.keyboard.o", 79);
        table.put("key.keyboard.p", 80);
        table.put("key.keyboard.q", 81);
        table.put("key.keyboard.r", 82);
        table.put("key.keyboard.s", 83);
        table.put("key.keyboard.t", 84);
        table.put("key.keyboard.u", 85);
        table.put("key.keyboard.v", 86);
        table.put("key.keyboard.w", 87);
        table.put("key.keyboard.x", 88);
        table.put("key.keyboard.y", 89);
        table.put("key.keyboard.z", 90);
        table.put("key.keyboard.f1", 290);
        table.put("key.keyboard.f2", 291);
        table.put("key.keyboard.f3", 292);
        table.put("key.keyboard.f4", 293);
        table.put("key.keyboard.f5", 294);
        table.put("key.keyboard.f6", 295);
        table.put("key.keyboard.f7", 296);
        table.put("key.keyboard.f8", 297);
        table.put("key.keyboard.f9", 298);
        table.put("key.keyboard.f10", 299);
        table.put("key.keyboard.f11", 300);
        table.put("key.keyboard.f12", 301);
        table.put("key.keyboard.f13", 302);
        table.put("key.keyboard.f14", 303);
        table.put("key.keyboard.f15", 304);
        table.put("key.keyboard.f16", 305);
        table.put("key.keyboard.f17", 306);
        table.put("key.keyboard.f18", 307);
        table.put("key.keyboard.f19", 308);
        table.put("key.keyboard.f20", 309);
        table.put("key.keyboard.f21", 310);
        table.put("key.keyboard.f22", 311);
        table.put("key.keyboard.f23", 312);
        table.put("key.keyboard.f24", 313);
        table.put("key.keyboard.f25", 314);
        table.put("key.keyboard.num.lock", 282);
        table.put("key.keyboard.keypad.0", 320);
        table.put("key.keyboard.keypad.1", 321);
        table.put("key.keyboard.keypad.2", 322);
        table.put("key.keyboard.keypad.3", 323);
        table.put("key.keyboard.keypad.4", 324);
        table.put("key.keyboard.keypad.5", 325);
        table.put("key.keyboard.keypad.6", 326);
        table.put("key.keyboard.keypad.7", 327);
        table.put("key.keyboard.keypad.8", 328);
        table.put("key.keyboard.keypad.9", 329);
        table.put("key.keyboard.keypad.add", 334);
        table.put("key.keyboard.keypad.decimal", 330);
        table.put("key.keyboard.keypad.enter", 335);
        table.put("key.keyboard.keypad.equal", 336);
        table.put("key.keyboard.keypad.multiply", 332);
        table.put("key.keyboard.keypad.divide", 331);
        table.put("key.keyboard.keypad.subtract", 333);
        table.put("key.keyboard.down", 264);
        table.put("key.keyboard.left", 263);
        table.put("key.keyboard.right", 262);
        table.put("key.keyboard.up", 265);
        table.put("key.keyboard.apostrophe", 39);
        table.put("key.keyboard.backslash", 92);
        table.put("key.keyboard.comma", 44);
        table.put("key.keyboard.equal", 61);
        table.put("key.keyboard.grave.accent", 96);
        table.put("key.keyboard.left.bracket", 91);
        table.put("key.keyboard.minus", 45);
        table.put("key.keyboard.period", 46);
        table.put("key.keyboard.right.bracket", 93);
        table.put("key.keyboard.semicolon", 59);
        table.put("key.keyboard.slash", 47);
        table.put("key.keyboard.space", 32);
        table.put("key.keyboard.tab", 258);
        table.put("key.keyboard.left.alt", 342);
        table.put("key.keyboard.left.control", 341);
        table.put("key.keyboard.left.shift", 340);
        table.put("key.keyboard.left.win", 343);
        table.put("key.keyboard.right.alt", 346);
        table.put("key.keyboard.right.control", 345);
        table.put("key.keyboard.right.shift", 344);
        table.put("key.keyboard.right.win", 347);
        table.put("key.keyboard.enter", 257);
        table.put("key.keyboard.escape", 256);
        table.put("key.keyboard.backspace", 259);
        table.put("key.keyboard.delete", 261);
        table.put("key.keyboard.end", 269);
        table.put("key.keyboard.home", 268);
        table.put("key.keyboard.insert", 260);
        table.put("key.keyboard.page.down", 267);
        table.put("key.keyboard.page.up", 266);
        table.put("key.keyboard.caps.lock", 280);
        table.put("key.keyboard.pause", 284);
        table.put("key.keyboard.scroll.lock", 281);
        table.put("key.keyboard.menu", 348);
        table.put("key.keyboard.print.screen", 283);
        table.put("key.keyboard.world.1", 161);
        table.put("key.keyboard.world.2", 162);
        return table;
    }

    private static Hashtable<Integer, Integer> mapGLFWToVK() {
        Hashtable<Integer, Integer> table = new Hashtable<>();
        for (int glfwKey = 0; glfwKey < 500; glfwKey++) {
            int virtualKey = getVkFromGLFWInternal(glfwKey);
            if (virtualKey != -1) {
                table.put(glfwKey, virtualKey);
            }
        }
        return table;
    }

    public static Integer getVkFromMCTranslation(String translationKey) {
        if (translationKey == null) {
            return null;
        }
        Integer glfwKey = TRANSLATIONS_TO_GLFW.get(translationKey);
        Integer vkKey = getVkFromGLFW(glfwKey);
        if (vkKey == null || vkKey <= 0) {
            return null;
        }
        return vkKey;
    }

    public static Integer getVkFromGLFW(Integer key) {
        if (key == null) {
            return null;
        }
        return GLFW_TO_VK.getOrDefault(key, -1);
    }

    public static Integer getVkFromGLFWInternal(Integer key) {
        if (key <= 7 && key >= -1)  // Unknown or Mouse
        {
            return -1;
        }
        if (key <= 57 && key >= 48)  // Number
        {
            return key;
        }
        if (key >= 65 && key <= 90)  // Letter
        {
            return key;
        }
        if (key >= 290 && key <= 313)  // Function keys
        {
            return key - 290 + Win32VK.VK_F1.code;
        }
        if (key == 282)  // Num Lock
        {
            return Win32VK.VK_NUMLOCK.code;
        }
        if (key >= 320 && key <= 329)  // Num keys
        {
            return key - 320 + Win32VK.VK_NUMPAD0.code;
        }
        if (key == 334)  // Add key
        {
            return Win32VK.VK_ADD.code;
        }
        if (key == 330)  // Decimal key
        {
            return Win32VK.VK_DECIMAL.code;
        }
        if (key == 335)  // Numpad enter key
        {
            return Win32VK.VK_RETURN.code; // definitely wrong lol
        }
        if (key == 336)  // Equals (on numpad) key
        {
            return 0xBB;  // definitely wrong lol
        }
        if (key == 332)  // Multiply key
        {
            return Win32VK.VK_MULTIPLY.code;
        }
        if (key == 331)  // Divide key
        {
            return Win32VK.VK_DIVIDE.code;
        }
        if (key == 333)  // Subtract key
        {
            return Win32VK.VK_SUBTRACT.code;
        }
        if (key == 264)  // Down key
        {
            return Win32VK.VK_DOWN.code;
        }
        if (key == 263)  // Left key
        {
            return Win32VK.VK_LEFT.code;
        }
        if (key == 262)  // Right key
        {
            return Win32VK.VK_RIGHT.code;
        }
        if (key == 265)  // Up key
        {
            return Win32VK.VK_UP.code;
        }
        if (key == 39)  // Apostrophe
        {
            return 0xDE;  // May be wrong
        }
        if (key == 92)  // Backslash
        {
            return 0xDC;  // May be wrong
        }
        if (key == 44)  // ,< key
        {
            return 0xBC;  // May be wrong
        }
        if (key == 61)  // += key
        {
            return 0xBB;  // May be wrong
        }
        if (key == 96)  // `~ key
        {
            return 0xC0;  // May be wrong
        }
        if (key == 91)  // [{ key
        {
            return 0xDB;  // May be wrong
        }
        if (key == 45)  // -_ key
        {
            return 0xBD;  // May be wrong
        }
        if (key == 46)  // .> key
        {
            return 0xBE;  // May be wrong
        }
        if (key == 93)  // ]} key
        {
            return 0xDD;  // May be wrong
        }
        if (key == 59)  // ;) key
        {
            return 0xBA;  // May be wrong
        }
        if (key == 47)  // /? key
        {
            return 0xBF;  // May be wrong
        }
        if (key == 32)  // Space
        {
            return Win32VK.VK_SPACE.code;
        }
        if (key == 258)  // Tab
        {
            return Win32VK.VK_TAB.code;
        }
        if (key == 342)  // Left alt
        {
            return Win32VK.VK_LMENU.code;
        }
        if (key == 341)  // Left ctrl
        {
            return Win32VK.VK_LCONTROL.code;
        }
        if (key == 340)  // Left shift
        {
            return Win32VK.VK_LSHIFT.code;
        }
        if (key == 343)  // Left win
        {
            return Win32VK.VK_LWIN.code;
        }
        if (key == 346)  // Right alt
        {
            return Win32VK.VK_RMENU.code;
        }
        if (key == 345)  // Right control
        {
            return Win32VK.VK_RCONTROL.code;
        }
        if (key == 344)  // Right shift
        {
            return Win32VK.VK_RSHIFT.code;
        }
        if (key == 347)  // Right win
        {
            return Win32VK.VK_RWIN.code;
        }
        if (key == 257)  // Enter
        {
            return Win32VK.VK_RETURN.code;
        }
        if (key == 256)  // Escape
        {
            return Win32VK.VK_ESCAPE.code;
        }
        if (key == 259)  // Backspace
        {
            return Win32VK.VK_BACK.code;
        }
        if (key == 261)  // Del
        {
            return Win32VK.VK_DELETE.code;
        }
        if (key == 269)  // End
        {
            return Win32VK.VK_END.code;
        }
        if (key == 268)  // Home
        {
            return Win32VK.VK_HOME.code;
        }
        if (key == 260)  // Insert
        {
            return Win32VK.VK_INSERT.code;
        }
        if (key == 267)  // PgDn
        {
            return Win32VK.VK_NEXT.code;
        }
        if (key == 266)  // PgUp
        {
            return Win32VK.VK_PRIOR.code;
        }
        if (key == 280)  // Caps lock
        {
            return Win32VK.VK_CAPITAL.code;
        }
        if (key == 284)  // Pause
        {
            return Win32VK.VK_PAUSE.code;
        }
        if (key == 281)  // Scroll lock
        {
            return Win32VK.VK_SCROLL.code;
        }
        if (key == 348)  // "Menu"
        {
            return Win32VK.VK_APPS.code;  // Might be wrong
        }
        if (key == 283)  // Print screen
        {
            return Win32VK.VK_SNAPSHOT.code;
        }
        if (key == 161 || key == 162)  // what even are these "world" keys
        {
            return -1; // Definitely wrong
        }
        return -1;
    }
}
