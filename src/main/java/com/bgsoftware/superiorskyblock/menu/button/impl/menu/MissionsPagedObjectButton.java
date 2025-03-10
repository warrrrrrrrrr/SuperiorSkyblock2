package com.bgsoftware.superiorskyblock.menu.button.impl.menu;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.missions.IMissionsHolder;
import com.bgsoftware.superiorskyblock.api.missions.Mission;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.menu.button.PagedObjectButton;
import com.bgsoftware.superiorskyblock.menu.impl.MenuMissionsCategory;
import com.bgsoftware.superiorskyblock.mission.MissionData;
import com.bgsoftware.superiorskyblock.utils.items.TemplateItem;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public final class MissionsPagedObjectButton extends PagedObjectButton<MenuMissionsCategory, Mission<?>> {

    private static final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();

    private final SoundWrapper completedSound;
    private final SoundWrapper notCompletedSound;
    private final SoundWrapper canCompleteSound;

    private MissionsPagedObjectButton(TemplateItem buttonItem, SoundWrapper completedSound, List<String> commands,
                                      String requiredPermission, SoundWrapper lackPermissionSound, TemplateItem nullItem,
                                      SoundWrapper notCompletedSound, SoundWrapper canCompleteSound, int objectIndex) {
        super(buttonItem, null, commands, requiredPermission, lackPermissionSound, nullItem, objectIndex);
        this.completedSound = completedSound;
        this.notCompletedSound = notCompletedSound;
        this.canCompleteSound = canCompleteSound;
    }

    @Override
    public ItemStack modifyButtonItem(ItemStack buttonItem, MenuMissionsCategory superiorMenu, Mission<?> mission) {
        Optional<MissionData> missionDataOptional = plugin.getMissions().getMissionData(mission);

        if (!missionDataOptional.isPresent())
            return buttonItem;

        SuperiorPlayer inventoryViewer = superiorMenu.getInventoryViewer();

        MissionData missionData = missionDataOptional.get();
        IMissionsHolder missionsHolder = mission.getIslandMission() ? inventoryViewer.getIsland() : inventoryViewer;

        if (missionsHolder == null)
            return new ItemStack(Material.AIR);

        boolean completed = !missionsHolder.canCompleteMissionAgain(mission);
        int percentage = getPercentage(mission.getProgress(inventoryViewer));
        int progressValue = mission.getProgressValue(inventoryViewer);
        int amountCompleted = missionsHolder.getAmountMissionCompleted(mission);

        ItemStack itemStack = completed ? missionData.getCompleted().build(inventoryViewer) :
                plugin.getMissions().canComplete(inventoryViewer, mission) ?
                        missionData.getCanComplete()
                                .replaceAll("{0}", percentage + "")
                                .replaceAll("{1}", progressValue + "")
                                .replaceAll("{2}", amountCompleted + "")
                                .build(inventoryViewer) :
                        missionData.getNotCompleted()
                                .replaceAll("{0}", percentage + "")
                                .replaceAll("{1}", progressValue + "")
                                .replaceAll("{2}", amountCompleted + "")
                                .build(inventoryViewer);

        mission.formatItem(inventoryViewer, itemStack);

        return itemStack;
    }

    @Override
    public void onButtonClick(SuperiorSkyblockPlugin plugin, MenuMissionsCategory superiorMenu,
                              InventoryClickEvent clickEvent) {
        SuperiorPlayer clickedPlayer = plugin.getPlayers().getSuperiorPlayer(clickEvent.getWhoClicked());
        Island island = clickedPlayer.getIsland();

        if (island == null)
            return;

        boolean completed = !island.canCompleteMissionAgain(pagedObject);
        boolean canComplete = plugin.getMissions().canComplete(clickedPlayer, pagedObject);

        SoundWrapper soundToPlay = completed ? completedSound : canComplete ? canCompleteSound : notCompletedSound;
        if (soundToPlay != null)
            soundToPlay.playSound(clickEvent.getWhoClicked());

        if (!canComplete || !plugin.getMissions().hasAllRequiredMissions(clickedPlayer, pagedObject))
            return;

        plugin.getMissions().rewardMission(pagedObject, clickedPlayer, false, false, result -> {
            if (result)
                superiorMenu.refreshPage();
        });
    }

    private static int getPercentage(double progress) {
        progress = Math.min(1.0, progress);
        return Math.round((float) progress * 100);
    }

    public static class Builder extends PagedObjectBuilder<Builder, MissionsPagedObjectButton, MenuMissionsCategory> {

        private SoundWrapper notCompletedSound = null;
        private SoundWrapper canCompleteSound = null;

        public Builder setCompletedSound(SoundWrapper completedSound) {
            this.clickSound = completedSound;
            return this;
        }

        public Builder setNotCompletedSound(SoundWrapper notCompletedSound) {
            this.notCompletedSound = notCompletedSound;
            return this;
        }

        public Builder setCanCompleteSound(SoundWrapper canCompleteSound) {
            this.canCompleteSound = canCompleteSound;
            return this;
        }

        @Override
        public MissionsPagedObjectButton build() {
            return new MissionsPagedObjectButton(buttonItem, clickSound, commands, requiredPermission,
                    lackPermissionSound, nullItem, notCompletedSound, canCompleteSound, getObjectIndex());
        }

    }

}
