package soys.soyslinkteam.join.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.team.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品 / 点券消耗入队。
 * <p>队伍设置了入队所需物品（{@code /steam set itemcost <MATERIAL:AMOUNT> ...}）时，
 * 玩家背包需具备全部物品方可加入，入队成功后自动扣减对应物品。</p>
 * <p>物品以 {@code MATERIAL:AMOUNT} 形式配置（如 {@code DIAMOND:5}），支持原版/模组物品名，
 * 由 {@link Material#matchMaterial(String)} 解析。管理员（soyslinkteam.admin.bypass）免消耗。</p>
 */
public class ItemJoinMethod extends AbstractJoinMethod {

    public static final String ID = "item";

    public ItemJoinMethod(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "物品消耗";
    }

    @Override
    public JoinResult attempt(JoinContext context) {
        Team team = context.getTeam();
        Player player = context.getPlayer();

        // 队伍未设置物品要求，交由其它方式尝试
        if (!team.getSettings().hasItemCost()) {
            return JoinResult.notApplicable();
        }

        // 管理员 bypass 跳过消耗
        if (plugin.getPermissionManager().canBypass(player)) {
            return JoinResult.success();
        }

        List<String> requirements = team.getSettings().getItemCost();
        List<String> missing = new ArrayList<>();
        List<String> requiredText = new ArrayList<>();
        for (String entry : requirements) {
            RequiredItem item = RequiredItem.parse(entry);
            if (item == null) {
                continue;
            }
            requiredText.add(item.describe());
            int have = countItem(player, item.material);
            if (have < item.amount) {
                missing.add(item.describe() + "(&f" + have + "/" + item.amount + "&r)");
            }
        }

        if (!missing.isEmpty()) {
            return JoinResult.denied("soyslinkteam.join.item.insufficient",
                    team.getName(), String.join(", ", requiredText),
                    String.join(", ", missing));
        }
        return JoinResult.success();
    }

    @Override
    public void onJoinSuccess(JoinContext context) {
        Team team = context.getTeam();
        Player player = context.getPlayer();

        if (!team.getSettings().hasItemCost()) {
            return;
        }
        if (plugin.getPermissionManager().canBypass(player)) {
            return;
        }

        List<RequiredItem> items = new ArrayList<>();
        for (String entry : team.getSettings().getItemCost()) {
            RequiredItem item = RequiredItem.parse(entry);
            if (item != null) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return;
        }

        // 背包修改必须在主线程执行
        if (Bukkit.isPrimaryThread()) {
            consume(player, items);
        } else {
            final List<RequiredItem> finalItems = items;
            Bukkit.getScheduler().runTask(plugin, () -> consume(player, finalItems));
        }
    }

    /**
     * 从玩家背包扣减物品（仅移除数量，不关心附魔/ Lore）。
     */
    private void consume(Player player, List<RequiredItem> items) {
        for (RequiredItem item : items) {
            int remaining = item.amount;
            for (ItemStack stack : player.getInventory().getContents()) {
                if (remaining <= 0) {
                    break;
                }
                if (stack == null || stack.getType() != item.material) {
                    continue;
                }
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
            }
        }
        player.updateInventory();
    }

    /**
     * 统计玩家背包中指定材质的总数量。
     */
    private int countItem(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * 单条物品需求：材质 + 数量。
     */
    private static class RequiredItem {
        final Material material;
        final int amount;

        RequiredItem(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        static RequiredItem parse(String entry) {
            if (entry == null || entry.trim().isEmpty()) {
                return null;
            }
            String[] parts = entry.trim().split(":", 2);
            Material material = Material.matchMaterial(parts[0].trim());
            if (material == null) {
                return null;
            }
            int amount = 1;
            if (parts.length > 1) {
                try {
                    amount = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    amount = 1;
                }
            }
            if (amount <= 0) {
                amount = 1;
            }
            return new RequiredItem(material, amount);
        }

        String describe() {
            return material.name() + ":" + amount;
        }
    }
}
