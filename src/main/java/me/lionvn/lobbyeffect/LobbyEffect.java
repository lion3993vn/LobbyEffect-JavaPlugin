package me.lionvn.lobbyeffect;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public final class LobbyEffect extends JavaPlugin implements Listener {

    private Location corner1;
    private Location corner2;
    private Location teleportLocation;
    private boolean isEnabled = false;
    private boolean isEnableSnow;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Tạo file config nếu chưa có
        isEnabled = getConfig().getBoolean("isEnable");
        isEnableSnow = getConfig().getBoolean("isEnableSnow");
        if (isEnabled) {
            loadLocationsFromConfig(); // Đọc tọa độ từ file config
            Bukkit.getPluginManager().registerEvents(this, this);
            particleSnow();
            particleLobby();
        }
        getLogger().info("LobbyFly đã được kích hoạt!");
    }

    @Override
    public void onDisable() {
        getLogger().info("LobbyFly đã tắt!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (corner1 == null || corner2 == null || teleportLocation == null) return;

        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();

        if(player.getWorld().getName().equals("world") && !isInsideArea(playerLocation)){
            player.teleport(teleportLocation);
            player.sendTitle("§bBạn đã quay về spawn", "");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Lệnh này chỉ dành cho người chơi!");
            return true;
        }

        Player player = (Player) sender;
        Location playerLocation = player.getLocation();
        FileConfiguration config = getConfig();

        if (command.getName().equalsIgnoreCase("setcorner1")) {
            corner1 = playerLocation;
            saveLocationToConfig("corner1", corner1);
            player.sendMessage("Góc thứ nhất đã được đặt và lưu tại: " + formatLocation(corner1));
            return true;
        }

        if (command.getName().equalsIgnoreCase("setcorner2")) {
            corner2 = playerLocation;
            saveLocationToConfig("corner2", corner2);
            player.sendMessage("Góc thứ hai đã được đặt và lưu tại: " + formatLocation(corner2));
            return true;
        }

        if (command.getName().equalsIgnoreCase("setteleport")) {
            teleportLocation = player.getLocation();
            saveLocationToConfig("teleport", teleportLocation); // Lưu cả vị trí và góc nhìn
            player.sendMessage("Vị trí dịch chuyển đã được đặt và lưu tại: " + formatLocation(teleportLocation));
            return true;
        }

        if (command.getName().equalsIgnoreCase("snowoff")) {
            player.sendMessage("Đã tắt hiệu ứng snow");
            isEnableSnow = false;
            config.set("isEnableSnow", false);
            saveConfig();
            return true;
        }

        if (command.getName().equalsIgnoreCase("snowon")) {
            player.sendMessage("Đã bật hiệu ứng snow");
            isEnableSnow = true;
            config.set("isEnableSnow", true);
            saveConfig();
            return true;
        }

        return false;
    }

    private boolean isInsideArea(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private void loadLocationsFromConfig() {
        FileConfiguration config = getConfig();
        // Đọc tọa độ từ config
        if (config.contains("corner1")) {
            corner1 = loadLocationFromConfig("corner1");
        }
        if (config.contains("corner2")) {
            corner2 = loadLocationFromConfig("corner2");
        }
        if (config.contains("teleport")) {
            teleportLocation = loadLocationFromConfig("teleport");
        }
    }

    private Location loadLocationFromConfig(String path) {
        FileConfiguration config = getConfig();
        String worldName = config.getString(path + ".world");
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw"); // Lấy yaw từ config
        float pitch = (float) config.getDouble(path + ".pitch"); // Lấy pitch từ config

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch); // Tạo lại Location với yaw và pitch
    }

    private void saveLocationToConfig(String path, Location location) {
        FileConfiguration config = getConfig();
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());   // Lưu yaw (góc quay theo trục Y)
        config.set(path + ".pitch", location.getPitch()); // Lưu pitch (góc quay theo trục X)
        saveConfig();
    }

    private String formatLocation(Location location) {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", location.getX(), location.getY(), location.getZ());
    }

    public void particleSnow() {
        // Task định kỳ kiểm tra vị trí người chơi
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if(player.getWorld().getName().equals("world") && isInsideArea(player.getLocation()) && isEnableSnow){
                            createParticleSnowEffect(player);
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 10L); // Lặp lại mỗi 10 tick (0.5 giây)
    }

    private void createParticleSnowEffect(Player player) {
        // Lấy vị trí của người chơi
        Location centerHead = player.getLocation().add(0, 4, 0); // Tăng vị trí để hạt rơi từ trên đầu + 4 block
        Location centerMiddle = player.getLocation().add(0, 1, 0); // Tăng vị trí để hạt rơi từ giữa người chơi + 1 block
        double radius = 5; // Giảm bán kính hạt rơi
        int particleCount = 20; // Số lượng điểm phát sinh hạt (chỉ 2 điểm: trước và sau)

        for (int i = 0; i < particleCount; i++) {
            // Tính vị trí theo vòng tròn xung quanh người chơi
            double angle = Math.PI * i; // Chỉ tạo hạt ở 0 độ (trước mặt) và 180 độ (sau lưng)
            double offsetX = radius * Math.cos(angle); // Tọa độ X
            double offsetZ = radius * Math.sin(angle); // Tọa độ Z

            // Tạo hạt FIREWORKS_SPARK ở trên đầu
            player.getWorld().spawnParticle(
                    Particle.FIREWORKS_SPARK, // Đổi thành hạt FIREWORKS_SPARK
                    centerHead.clone().add(offsetX, 0, offsetZ), // Vị trí quanh người chơi
                    2, // Số lượng hạt mỗi lần
                    5,5,5, // Giảm độ phân tán
                    0.00000001 // Giảm tốc độ hạt
            );

            // Tạo hạt FIREWORKS_SPARK ở giữa người chơi
            player.getWorld().spawnParticle(
                    Particle.FIREWORKS_SPARK, // Đổi thành hạt FIREWORKS_SPARK
                    centerMiddle.clone().add(offsetX, 0, offsetZ), // Vị trí quanh người chơi
                    2, // Số lượng hạt mỗi lần
                    5,5,5, // Giảm độ phân tán
                    0.00000001 // Giảm tốc độ hạt
            );
        }
    }

    private void particleLobby() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    if (isInsideArea(player.getLocation()) && player.getWorld().getName().equals("world")) {
                        // Thêm hiệu ứng chạy nhanh vĩnh viễn
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));

                        // Thêm hiệu ứng night vision vĩnh viễn
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, true, false));

                        player.setFoodLevel(20);
                        player.setSaturation(20);
                    }
                }
            }
        }.runTaskTimer(this, 0, 100); // 100 ticks = 5 giây
    }
}

