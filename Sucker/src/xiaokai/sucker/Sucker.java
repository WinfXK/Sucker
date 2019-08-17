package xiaokai.sucker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerGameModeChangeEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.Utils;

/**
 * @author Winfxk
 */
@SuppressWarnings("unchecked")
public class Sucker extends PluginBase implements Listener {
	private Config config;
	private boolean isThread = true;
	MyThread my;

	@Override
	public boolean onCommand(CommandSender player, Command command, String label, String[] a) {
		if (a.length < 1)
			return false;
		List<String> list;
		switch (a[0]) {
		case "load":
			if (player.isPlayer()) {
				player.sendMessage("§4请在控制台执行此命令！");
				return true;
			}
			config = new Config(new File(getDataFolder(), "Config.yml"), Config.YAML);
			player.sendMessage("§6已重新读取配置文件");
			return true;
		case "sl":
			if (player.isPlayer()) {
				player.sendMessage("§4请在控制台执行此命令！");
				return true;
			}
			if (a.length < 2) {
				player.sendMessage("§4请输入想要设置的间隔时间");
				return true;
			}
			try {
				int time = Float.valueOf(a[1]).intValue();
				config.set("异步间隔", time);
				config.save();
				my.异步间隔 = time;
				player.sendMessage("§6您已成功设置间隔时间！");
			} catch (Exception e) {
				player.sendMessage("§4您输入的间隔时间有误！请检查！！");
			}
			return true;
		case "th":
			if (player.isPlayer()) {
				player.sendMessage("§4请在控制台执行此命令！");
				return true;
			}
			if (!isThread) {
				isThread = true;
				my = new MyThread();
				my.start();
			} else
				isThread = false;
			config.set("后台异步", isThread);
			config.save();
			player.sendMessage("§6您成功" + (isThread ? "§a开启" : "§4关闭") + "§6异步线程");
			return true;
		case "ac":
			if (player.isPlayer()) {
				player.sendMessage("§4请在控制台执行此命令！");
				return true;
			}
			if (a.length < 2) {
				player.sendMessage("§4请输入想要添加或删除管理员权限的玩家名称");
				return true;
			}
			list = config.getList("管理员");
			if (list.contains(a[1])) {
				for (int i = 0; i < list.size(); i++)
					if (list.get(i).equals(a[1]))
						list.remove(i);
			} else
				list.add(a[1]);
			config.set("管理员", list);
			config.save();
			player.sendMessage("§6您成功" + (list.contains(a[1]) ? "§9添加" : "§4删除") + "§6一个管理员！");
			return true;
		case "am":
			if (!isAdmin(player.getName()) && player.isPlayer()) {
				player.sendMessage("§4???你这点逗比权限很难让我帮你办事啊！");
				return true;
			}
			if (a.length < 2) {
				player.sendMessage("§4请输入想要添加或删除创造权限的玩家名称");
				return true;
			}
			list = config.getList("创造白名单");
			if (list.contains(a[1])) {
				for (int i = 0; i < list.size(); i++)
					if (list.get(i).equals(a[1]))
						list.remove(i);
			} else
				list.add(a[1]);
			config.set("创造白名单", list);
			config.save();
			player.sendMessage("§6您成功" + (list.contains(a[1]) ? "§9添加" : "§4删除") + "§6一个白名单玩家！");
			return true;
		case "help":
		case "h":
		case "帮助":
		default:
			player.sendMessage("/am am <玩家名>  添加或删除创造权限");
			player.sendMessage("/am ac <玩家名>  添加或删除管理权限");
			player.sendMessage("/am th 启动异步或关闭异步");
			player.sendMessage("/am sl <秒数> 设置异步间隔时间");
			player.sendMessage("/am load 重新加载服务器");
			return true;
		}
	}

	@EventHandler
	public void onGame(PlayerGameModeChangeEvent e) {
		Player player = e.getPlayer();
		if (e.getNewGamemode() == 1 && !isSB(player.getName())) {
			player.sendMessage(config.getString("撤回模式提示"));
			e.setCancelled();
		}
	}

	public boolean isSB(String player) {
		List<String> list = config.getList("创造白名单");
		return list.contains(player) || isAdmin(player);
	}

	public boolean isAdmin(String player) {
		List<String> list = config.getList("管理员");
		return list.contains(player);
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("§6启动ing...");
	}

	@Override
	public void onLoad() {
		getLogger().info("§6初始化ing..");
		config = new Config(new File(getDataFolder(), "Config.yml"), Config.YAML);
		if (!getDataFolder().exists())
			getDataFolder().mkdirs();
		File file = new File(getDataFolder(), "Config.yml");
		InputStream ss = getClass().getResourceAsStream("/xiaokai/sucker/Config.yml");
		if (!file.exists())
			try {
				Utils.writeFile(file, ss);
			} catch (IOException e) {
				getLogger().error("§4初始化失败！" + e.getMessage());
				setEnabled(false);
				return;
			}
		try {
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(dumperOptions);
			String content = Utils.readFile(ss);
			LinkedHashMap<String, Object> map = new ConfigSection(yaml.loadAs(content, LinkedHashMap.class));
			Map<String, Object> cg = config.getAll();
			isMap(map, cg, config);
		} catch (IOException e) {
			getLogger().info("§4在检查数据中遇到错误！请尝试删除该文件§9[§dConfig.yml§9]\n§f" + e.getMessage());
			setEnabled(false);
			return;
		}
		my = new MyThread();
		my.start();
	}

	public void isMap(Map<String, Object> map, Map<String, Object> cg, Config config) {
		for (String ike : map.keySet())
			if (!cg.containsKey(ike)) {
				cg.put(ike, map.get(ike));
				getLogger().info("§6" + ike + "§4所属的数据错误！已回复默认");
				continue;
			} else if (!(((cg.get(ike) instanceof Map) || (map.get(ike) instanceof Map))
					|| ((cg.get(ike) instanceof List) && (map.get(ike) instanceof List)
							|| ((cg.get(ike) instanceof String) && (map.get(ike) instanceof String)))
					|| ((map.get(ike) instanceof Integer) && (cg.get(ike) instanceof Integer))
					|| ((map.get(ike) instanceof Boolean) && (cg.get(ike) instanceof Boolean))
					|| ((map.get(ike) instanceof Float) && (cg.get(ike) instanceof Float)))) {
				cg.put(ike, map.get(ike));
				getLogger().info("§6" + ike + "§4属性不匹配！已回复默认");
				continue;
			} else if (map.get(ike) instanceof Map)
				cg.put(ike, icMap((Map<String, Object>) map.get(ike), (Map<String, Object>) cg.get(ike)));
		config.setAll((LinkedHashMap<String, Object>) cg);
		config.save();
	}

	public Map<String, Object> icMap(Map<String, Object> map, Map<String, Object> cg) {
		for (String ike : map.keySet())
			if (!cg.containsKey(ike)) {
				cg.put(ike, map.get(ike));
				getLogger().info("§6" + ike + "§4所属的数据错误！已回复默认");
				continue;
			} else if (!(((cg.get(ike) instanceof Map) && (map.get(ike) instanceof Map))
					|| ((cg.get(ike) instanceof List) && (map.get(ike) instanceof List)
							|| ((cg.get(ike) instanceof String) && (map.get(ike) instanceof String))))) {
				cg.put(ike, map.get(ike));
				getLogger().info("§6" + ike + "§4属性不匹配！已回复默认");
				continue;
			} else if (map.get(ike) instanceof Map)
				cg.put(ike, icMap((Map<String, Object>) map.get(ike), (Map<String, Object>) cg.get(ike)));
		return cg;
	}

	private class MyThread extends Thread {
		protected int 异步间隔;

		public MyThread() {
			异步间隔 = config.getInt("异步间隔");
		}

		@Override
		public void run() {
			while (isThread) {
				try {
					Thread.sleep(1000);
					if (异步间隔 < 0) {
						异步间隔 = config.getInt("异步间隔");
						Map<UUID, Player> Players = getServer().getOnlinePlayers();
						Set<UUID> list = Players.keySet();
						for (UUID uuid : list) {
							Player player = Players.get(uuid);
							if (!isSB(player.getName()) && player.getGamemode() == 1) {
								player.sendMessage(config.getString("撤回模式提示"));
								player.setGamemode(0);
							}
						}
					} else
						异步间隔--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
