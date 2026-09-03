package soys.soyslinkteam.join.impl;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import soys.soyslinkteam.SOYSLinkTeam;
import soys.soyslinkteam.join.JoinContext;
import soys.soyslinkteam.join.JoinResult;
import soys.soyslinkteam.team.Team;
import soys.soyslinkteam.util.Placeholders;

/**
 * 经济消耗入队。
 * <p>队伍设置了入队费用（{@code /steam set cost <金额>}）且玩家余额足够时，
 * 扣除费用后允许加入。依赖 Vault 与任意经济插件（EssentialsX / CMI 等）。</p>
 * <p>Vault 不可用时本方式自动停用（{@link #isEnabled()} 返回 false），
 * 不影响其它入队方式。</p>
 */
public class EconomyJoinMethod extends AbstractJoinMethod {

    public static final String ID = "economy";

    private Economy economy;
    private boolean vaultChecked = false;

    public EconomyJoinMethod(SOYSLinkTeam plugin) {
        super(plugin);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "经济消耗";
    }

    @Override
    public boolean isEnabled() {
        // 配置关闭时直接停用
        if (!plugin.getConfigManager().isJoinMethodEnabled(ID)) {
            return false;
        }
        // 配置开启但 Vault 不可用时，记录一次警告并停用
        Economy econ = getEconomy();
        if (econ == null) {
            if (!vaultChecked) {
                vaultChecked = true;
                plugin.getLogger().warning("经济消耗入队已启用，但未检测到 Vault 经济服务，该入队方式已自动停用。"
                        + "请安装 Vault 及经济插件（如 EssentialsX）。");
            }
            return false;
        }
        vaultChecked = true;
        return true;
    }

    @Override
    public JoinResult attempt(JoinContext context) {
        Team team = context.getTeam();
        Player player = context.getPlayer();

        // 队伍未设置费用，交由其它方式尝试
        if (!team.getSettings().hasEconomyCost()) {
            return JoinResult.notApplicable();
        }

        double cost = team.getSettings().getEconomyCost();
        Economy econ = getEconomy();
        if (econ == null) {
            return JoinResult.notApplicable();
        }

        // 管理员 bypass 跳过费用
        if (plugin.getPermissionManager().canBypass(player)) {
            return JoinResult.success();
        }

        if (!econ.has(player, cost)) {
            return JoinResult.denied("join.economy.insufficient",
                    Placeholders.of("cost", formatCost(cost))
                            .and("balance", formatCost(econ.getBalance(player)))
                            .and("team", team.getName())
                            .build());
        }

        return JoinResult.success();
    }

    @Override
    public void onJoinSuccess(JoinContext context) {
        Team team = context.getTeam();
        Player player = context.getPlayer();

        if (!team.getSettings().hasEconomyCost()) {
            return;
        }
        if (plugin.getPermissionManager().canBypass(player)) {
            return;
        }

        Economy econ = getEconomy();
        if (econ == null) {
            return;
        }

        double cost = team.getSettings().getEconomyCost();
        // 异步扣款，避免经济插件的 IO 操作阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            EconomyResponse response = econ.withdrawPlayer(player, cost);
            if (response == null || !response.transactionSuccess()) {
                plugin.getLogger().warning("扣除玩家 " + player.getName()
                        + " 的入队费用 " + cost + " 失败: "
                        + (response == null ? "无响应" : response.errorMessage));
                // 扣款失败时通知玩家（回到主线程发送消息）
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getMessageManager().send(player, "join.economy.charge-failed",
                                Placeholders.of("cost", formatCost(cost)).build()));
            }
        });
    }

    /**
     * 从 Bukkit 服务管理器获取 Vault Economy 实例，不可用时返回 null。
     */
    private Economy getEconomy() {
        if (economy != null) {
            return economy;
        }
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        } catch (Throwable t) {
            // Vault 未安装时 ClassNotFoundException 等，静默处理
            economy = null;
        }
        return economy;
    }

    private String formatCost(double cost) {
        if (cost == Math.floor(cost)) {
            return String.valueOf((long) cost);
        }
        return String.format("%.2f", cost);
    }

    @Override
    public void reload() {
        // 重载时重置 economy 缓存与检测标记，允许运行中安装 Vault 后恢复
        this.economy = null;
        this.vaultChecked = false;
    }
}
