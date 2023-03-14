package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.ArrayList;
import java.util.List;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class MultiResetManager extends ResetManager {
    public MultiResetManager(Julti julti) {
        super(julti);
    }

    @Override
    public List<ActionResult> doReset() {
        JultiOptions options = JultiOptions.getInstance();
        List<MinecraftInstance> instances = this.instanceManager.getInstances();
        List<ActionResult> actionResults = new ArrayList<>();

        // Return if no instances
        if (instances.size() == 0) {
            return actionResults;
        }

        // Get selected instance, return if no selected instance,
        MinecraftInstance selectedInstance = this.instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return actionResults;
        }

        boolean resetFirst = options.coopMode || selectedInstance.isFullscreen();

        // if there is only a single instance, reset it and return.
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            actionResults.add(ActionResult.INSTANCE_RESET);
            return actionResults;
        }

        int nextInstInd = (instances.indexOf(selectedInstance) + 1) % instances.size();
        int nextInsNum = nextInstInd + 1;
        MinecraftInstance nextInstance = instances.get(nextInstInd);

        if (resetFirst) {
            selectedInstance.reset(false);
            actionResults.add(ActionResult.INSTANCE_RESET);
            sleep(100);
        }
        nextInstance.activate(nextInsNum);
        actionResults.add(ActionResult.INSTANCE_ACTIVATED);
        if (!resetFirst) {
            selectedInstance.reset(false);
            actionResults.add(ActionResult.INSTANCE_RESET);
        }
        this.julti.switchScene(nextInstInd + 1);

        super.doReset();

        if (options.useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return actionResults;
    }


    @Override
    public List<ActionResult> doBGReset() {
        List<ActionResult> actionResults = new ArrayList<>();

        MinecraftInstance selectedInstance = this.instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return actionResults;
        }
        List<MinecraftInstance> instances = this.instanceManager.getInstances();

        for (MinecraftInstance instance : instances) {
            if (instance.equals(selectedInstance)) {
                continue;
            }
            if (this.resetInstance(instance)) {
                actionResults.add(ActionResult.INSTANCE_RESET);
            }

        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(this.julti);
        }
        return actionResults;
    }
}
