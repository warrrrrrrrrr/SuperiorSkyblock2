package com.bgsoftware.superiorskyblock.menu.button.impl.menu;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.missions.MissionCategory;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.menu.button.SuperiorMenuButton;
import com.bgsoftware.superiorskyblock.menu.impl.MenuMissions;
import com.bgsoftware.superiorskyblock.utils.items.TemplateItem;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public final class OpenMissionCategoryButton extends SuperiorMenuButton<MenuMissions> {

    private final MissionCategory missionCategory;

    private OpenMissionCategoryButton(TemplateItem buttonItem, SoundWrapper clickSound, List<String> commands,
                                      String requiredPermission, SoundWrapper lackPermissionSound,
                                      MissionCategory missionCategory) {
        super(buttonItem, clickSound, commands, requiredPermission, lackPermissionSound);
        this.missionCategory = missionCategory;
    }

    @Override
    public void onButtonClick(SuperiorSkyblockPlugin plugin, MenuMissions superiorMenu, InventoryClickEvent clickEvent) {
        SuperiorPlayer clickedPlayer = plugin.getPlayers().getSuperiorPlayer(clickEvent.getWhoClicked());
        superiorMenu.setPreviousMove(false);
        plugin.getMenus().openMissionsCategory(clickedPlayer, superiorMenu, missionCategory);
    }

    public static class Builder extends AbstractBuilder<Builder, OpenMissionCategoryButton, MenuMissions> {

        private MissionCategory missionCategory;

        public Builder setMissionsCategory(MissionCategory missionCategory) {
            this.missionCategory = missionCategory;
            return this;
        }

        @Override
        public OpenMissionCategoryButton build() {
            return new OpenMissionCategoryButton(buttonItem, clickSound, commands, requiredPermission,
                    lackPermissionSound, missionCategory);
        }

    }

}
