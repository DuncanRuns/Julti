package xyz.duncanruns.julti.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * JNA interface with Window's user32.dll
 *
 * @author Pete S & DuncanRuns & Lxnus
 */
@SuppressWarnings({ "UnusedDeclaration", "UnusedReturnValue" })
public interface User32 extends StdCallLibrary {
    User32 INSTANCE = Native.load("user32", User32.class);

    HWND GetForegroundWindow();

    boolean SetWindowPos(HWND hWnd, Pointer hwndInsertAfter, int x, int y, int cx, int cy, UINT flags);

    int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);

    boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer userData);

    boolean IsWindow(HWND hWnd);

    LONG GetWindowLongA(HWND hWnd, int nIndex);

    LONG SetWindowLongA(HWND hWnd, int nIndex, LONG dwNewLong);

    boolean PostMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    BOOL SendNotifyMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    BOOL SendMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    boolean ShowWindow(HWND hWnd, int nCmdShow);

    boolean SetWindowTextA(HWND hWnd, String lpString);

    int SetForegroundWindow(HWND hWnd);

    boolean BringWindowToTop(HWND hWnd);

    boolean MoveWindow(HWND hWnd, int x, int y, int nWidth, int nHeight, boolean bRepaint);

    boolean IsIconic(HWND hWnd);

    UINT MapVirtualKeyA(UINT uCode, UINT uMapType);

    int GetWindowThreadProcessId(HWND hWnd, IntByReference lpdwProcessId);

    short GetAsyncKeyState(int vKey);

    UINT SendInput(DWORD dword, INPUT[] inputs, int inputSize);

    HDC GetWindowDC(HWND hWnd);

    boolean GetClientRect(HWND hWnd, RECT rect);

    boolean GetWindowRect(HWND hWnd, RECT rect);

    HDC GetDC(HWND hWnd);

    int ReleaseDC(HWND hWnd, HDC hDC);

    int FillRect(HDC hdc, RECT rect, HBRUSH hbrush);

    boolean IsZoomed(HWND hWnd);

    BOOL GetCursorInfo(CURSORINFO pci);

    BOOL GetCursorPos(POINT point);

    interface WNDENUMPROC extends StdCallCallback {
        boolean callback(HWND hWnd, Pointer arg);
    }

    @Structure.FieldOrder({ "cbSize", "flags", "hCursor", "ptScreenPos" })
    class CURSORINFO extends Structure {
        public DWORD cbSize;
        public DWORD flags;
        public HCURSOR hCursor;
        public POINT ptScreenPos;
    }
}