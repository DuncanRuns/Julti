package xyz.duncanruns.julti.command;

import xyz.duncanruns.julti.Julti;

public class Action {
    public String name;
    public byte scenario; // 0 = in game, 1 = on wall, 2 = on either
    public String command;

    public Action(String name, byte scenario, String command) {
        this.name = name;
        this.scenario = scenario;
        this.command = command;
    }

    public static Action fromString(String string) {
        String name = string.substring(0, string.indexOf(';'));
        string = string.substring(string.indexOf(';') + 1);
        byte scenario = Byte.parseByte(string.substring(0, string.indexOf(';')));
        String command = string.substring(string.indexOf(';') + 1);
        return new Action(name, scenario, command);
    }

    public boolean run(Julti julti) {
        int currentScenario;
        if (julti.getInstanceManager().getSelectedInstance() != null) {
            currentScenario = 0;
        } else if (julti.isWallActive()) {
            currentScenario = 1;
        } else {
            return false;
        }
        if (scenario == 2 || currentScenario == scenario) {
            julti.runCommand(command);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return name + ";" + scenario + ";" + command;
    }
}
