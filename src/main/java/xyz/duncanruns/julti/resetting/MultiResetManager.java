package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.DoAllFastUtil;

import java.util.ArrayList;
import java.util.List;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public class MultiResetManager extends ResetManager {
    private static final MultiResetManager INSTANCE = new MultiResetManager();

    public static ResetManager getInstance() {
        return INSTANCE;
    }

    @Override
    public List<ActionResult> doReset() {
        JultiOptions options = JultiOptions.getInstance();
        List<ActionResult> actionResults = new ArrayList<>();

        int instanceCount = InstanceManager.getManager().getSize();

        // Return if no instances
        if (instanceCount == 0) {
            return actionResults;
        }

        // Get selected instance, return if no selected instance,
        MinecraftInstance selectedInstance = InstanceManager.getManager().getSelectedInstance();
        if (selectedInstance == null) {
            return actionResults;
        }

        boolean resetFirst = options.coopMode || selectedInstance.isFullscreen();

        // if there is only a single instance, reset it and return.
        if (instanceCount == 1) {
            selectedInstance.reset();
            actionResults.add(ActionResult.INSTANCE_RESET);
            return actionResults;
        }

        List<MinecraftInstance> instancePool = new ArrayList<>(InstanceManager.getManager().getInstances());
        instancePool.removeIf(instance -> instance.equals(selectedInstance));
        instancePool.sort((o1, o2) -> o2.getWallSortingNum() - o1.getWallSortingNum());
        MinecraftInstance nextInstance = instancePool.get(0);
        int instanceNum = InstanceManager.getManager().getInstanceNum(nextInstance);

        if (resetFirst) {
            selectedInstance.reset();
            actionResults.add(ActionResult.INSTANCE_RESET);
            sleep(100);
        }
        Julti.getInstance().activateInstance(nextInstance);
        actionResults.add(ActionResult.INSTANCE_ACTIVATED);
        if (!resetFirst) {
            selectedInstance.reset();
            actionResults.add(ActionResult.INSTANCE_RESET);
        }

        super.doReset();

        if (options.useAffinity) {
            AffinityManager.ping();
        }
        return actionResults;
    }


    @Override
    public List<ActionResult> doBGReset() {
        List<ActionResult> actionResults = new ArrayList<>();

        MinecraftInstance selectedInstance = InstanceManager.getManager().getSelectedInstance();
        if (selectedInstance == null) {
            return actionResults;
        }
        List<MinecraftInstance> instances = InstanceManager.getManager().getInstances();

        DoAllFastUtil.doAllFast(instances, instance -> {
            if (instance.equals(selectedInstance)) {
                return;
            }
            if (this.resetInstance(instance)) {
                actionResults.add(ActionResult.INSTANCE_RESET);
            }
        });
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping();
        }
        return actionResults;
    }
}
