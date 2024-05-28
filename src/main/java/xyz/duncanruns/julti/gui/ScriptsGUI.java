package xyz.duncanruns.julti.gui;

import org.apache.logging.log4j.Level;
import org.kohsuke.github.GHGist;
import org.kohsuke.github.GHGistFile;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.GitHubUtil;
import xyz.duncanruns.julti.util.OfficialScriptsUtil;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ScriptsGUI extends JFrame {
    private static OfficialScriptsBrowserGUI officialScriptsBrowserGUI;
    private static ScriptsGUI instance = null;
    private boolean closed = false;
    private JPanel panel;
    private JButton officialScriptsButton;

    public ScriptsGUI() {
        Point location = JultiGUI.getJultiGUI().getLocation();
        this.setLocation(location.x, location.y + 30);
        this.setupWindow();
        this.reload();
    }

    @Nullable
    public static ScriptsGUI getGUI() {
        return instance;
    }

    public static ScriptsGUI openGUI() {
        if (instance == null || instance.isClosed()) {
            instance = new ScriptsGUI();
        }
        instance.requestFocus();
        return instance;
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("Julti Scripts");
        this.setIconImage(JultiGUI.getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ScriptsGUI.this.onClose();
            }
        });
        this.setSize(420, 500);
        this.setVisible(true);
        this.panel = new JPanel();
        this.panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(this.panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.setContentPane(scrollPane);
    }

    public void reload() {
        JScrollBar verticalScrollBar = ((JScrollPane) this.getContentPane()).getVerticalScrollBar();
        int i = verticalScrollBar.getValue();
        this.panel.removeAll();

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(3, 2, 5, 5));

        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Import Script..."), a -> this.startImportDialog()));
        this.officialScriptsButton = GUIUtil.getButtonWithMethod(new JButton("Browse Official Scripts..."), a -> this.browseOfficialScriptsButton());
        buttonsPanel.add(this.officialScriptsButton);
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Edit Hotkeys..."), a -> {
            OptionsGUI optionsGUI = OptionsGUI.openGUI();
            optionsGUI.reload();
            optionsGUI.openTab("Hotkeys");
            optionsGUI.setScroll(100000);
            optionsGUI.requestFocus();
        }));
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Open Scripts Folder"), a -> {
            try {
                Desktop.getDesktop().browse(ScriptManager.SCRIPTS_FOLDER.toUri());
            } catch (IOException ignored) {
            }
        }));
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Cancel Running Scripts"), a -> ScriptManager.cancelAllScripts()));
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Reload Scripts"), a -> {
            ScriptManager.reload();
            this.reload();
        }));

        GUIUtil.setActualSize(buttonsPanel, 355, 85);
        this.panel.add(GUIUtil.leftJustify(buttonsPanel));
        this.panel.add(GUIUtil.createSpacer());

        this.panel.add(GUIUtil.createSpacer(15));

        this.panel.add(GUIUtil.leftJustify(new JLabel("(Right click for action menu)")));
        for (String name : ScriptManager.getScriptNames()) {
            this.panel.add(GUIUtil.leftJustify(new ScriptPanel(name, ScriptManager.getHotkeyContext(name), this::reload, () -> this.runCustomization(name, true))));
        }

        verticalScrollBar.setValue(i);
        this.revalidate();
        this.repaint();
    }

    private void startImportDialog() {
        String out = JOptionPane.showInputDialog(this, "Input a gist link/id or a legacy import code:", "Julti: Import Legacy Script", JOptionPane.PLAIN_MESSAGE);

        if (out == null || out.trim().isEmpty()) {
            return;
        }
        out = out.trim();

        if (ScriptManager.isLegacyImportCode(out)) {
            this.importLegacyScriptDialog(out);
            return;
        }

        if (!ScriptManager.isGist(out)) {
            out = "https://gist.github.com/DoesntMatter/" + out;
            if (!ScriptManager.isGist(out)) {
                JOptionPane.showMessageDialog(this, "Invalid import code! Please enter a gist link/id or a legacy import code to import a script!", "Julti: Import Script Failed", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        // Must be a gist now
        this.importGist(out);
    }

    private void importGist(String out) {
        String[] split = out.split("/");
        String gistId = split[split.length - 1];
        GHGist gist;
        try {
            gist = GitHubUtil.getGitHub().getGist(gistId);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to retrieve github gist of id " + gistId + "! Maybe you didn't enter a real gist id/link? (GitHub could also be rate limiting you!)", "Julti: Import Script Failed", JOptionPane.WARNING_MESSAGE);
            Julti.log(Level.ERROR, "Failed to retrieve github gist: " + ExceptionUtil.toDetailedString(e));
            return;
        }
        Optional<Map.Entry<String, GHGistFile>> ghFileOpt = gist.getFiles().entrySet().stream().filter(entry -> entry.getKey().endsWith(".txt") || entry.getKey().endsWith(".lua")).findAny();
        if (!ghFileOpt.isPresent()) {
            JOptionPane.showMessageDialog(this, "Github gist does not contain a Julti script!", "Julti: Import Script Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String scriptName = ghFileOpt.get().getKey();
        boolean isLegacy = scriptName.endsWith(".txt");
        scriptName = scriptName.split("\\.")[0];
        if (!this.checkCanWriteScript(scriptName)) {
            return;
        }
        try {
            ScriptManager.deleteScript(scriptName);
            ScriptManager.writeScript(scriptName, ghFileOpt.get().getValue().getContent(), isLegacy);
            ScriptManager.reload();
            this.runCustomization(scriptName, false);
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to write script: " + ExceptionUtil.toDetailedString(e));
            JOptionPane.showMessageDialog(this, "Failed to write script!", "Julti: Import Script Failed", JOptionPane.WARNING_MESSAGE);
        }
        this.reload();
    }

    private void runCustomization(String scriptName, boolean reportNone) {
        boolean hasCustomization = ScriptManager.runCustomization(scriptName);
        if (!hasCustomization && reportNone) {
            JOptionPane.showMessageDialog(this, "This script offers no customization! (or an error occurred)", "Julti: No Script Customization", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void importLegacyScriptDialog(String out) {
        Runnable showInvalid = () -> JOptionPane.showMessageDialog(this, "Invalid legacy script code!", "Julti: Import Script Failed", JOptionPane.WARNING_MESSAGE);

        String[] parts = out.split(";");
        if (parts.length < 2) {
            showInvalid.run();
            return;
        }
        String scriptName = parts[0];
        try {
            Byte.parseByte(parts[1]);
        } catch (NumberFormatException e) {
            showInvalid.run();
            return;
        }

        if (!this.checkCanWriteScript(scriptName)) {
            return;
        }
        try {
            ScriptManager.reload();
            ScriptManager.deleteScript(scriptName);
            ScriptManager.writeLegacyScript(out);
            ScriptManager.reload();
            this.runCustomization(scriptName, false);
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to write script: " + ExceptionUtil.toDetailedString(e));
        }
        this.reload();
    }

    private boolean checkCanWriteScript(String scriptName) {
        return !ScriptManager.getScriptNames().contains(scriptName) || 0 == JOptionPane.showConfirmDialog(this, "Overwrite existing script (" + scriptName + ")?", "Julti: Overwrite Legacy Script", JOptionPane.YES_NO_OPTION);
    }

    private void onClose() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }


    private void browseOfficialScriptsButton() {
        if (officialScriptsBrowserGUI != null && !officialScriptsBrowserGUI.isClosed()) {
            officialScriptsBrowserGUI.requestFocus();
            this.dispose();
            this.onClose();
            return;
        }

        this.officialScriptsButton.setEnabled(false);
        this.officialScriptsButton.setText("Loading...");
        Runnable cleanup = () -> {
            this.officialScriptsButton.setEnabled(true);
            this.officialScriptsButton.setText("Browse Official Scripts...");
        };
        new Thread(() -> {
            Set<String> fileNames;
            try {
                fileNames = OfficialScriptsUtil.retrieveOfficialScriptFileNames();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to retrieve official scripts! You could be rate limited by GitHub, so try again in a few minutes...", "Julti: Official Scripts Failure", JOptionPane.ERROR_MESSAGE);
                cleanup.run();
                return;
            }
            officialScriptsBrowserGUI = new OfficialScriptsBrowserGUI(fileNames);
            officialScriptsBrowserGUI.setLocation(this.getX(), this.getY());
            this.dispose();
            this.onClose();
            officialScriptsBrowserGUI.requestFocus();
        }).start();

        // TODO: show default scripts menu
    }
}
