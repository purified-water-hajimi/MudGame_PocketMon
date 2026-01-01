package pokemon;

import java.util.*;

public class Room {
    private String id;           // 房间唯一ID (如 "home")
    private String name;         // 房间显示名 (如 "真新镇")
    private String description;  // 描述
    private Map<String, String> exits; // 出口方向 -> 房间ID
    
    // 存放玩家列表 (联机用)
    private Vector<Player> playersInRoom = new Vector<>();
    // 存放野生宝可梦 (战斗用)
    private List<PocketMon> wildPokemons = new ArrayList<>();

    public Room(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.exits = new HashMap<>();
    }

    // === 这里的 getId 就是你报错缺少的那个！ ===
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setExits(String direction, String neighbor) {
        exits.put(direction, neighbor);
    }

    public void addExit(String direction, String neighborId) {
        exits.put(direction, neighborId);
    }

    public String getExit(String direction) {
        return exits.get(direction);
    }
    
    public String getDescription() {
        return description;
    }

    public String getFullDescription() {
        return "位置: " + name + "\n" + description;
    }

    public String getAvailableExits() {
        return exits.keySet().toString();
    }

    // === 联机功能: 玩家进出 ===
    public void addPlayer(Player p) {
        playersInRoom.add(p);
    }

    public void removePlayer(Player p) {
        playersInRoom.remove(p);
    }

    public String getPlayerNames(Player observer) {
        StringBuilder sb = new StringBuilder();
        for (Player p : playersInRoom) {
            if (p != observer) {
                sb.append(p.getName()).append(" ");
            }
        }
        return sb.length() == 0 ? "身边没有人。" : "身边的玩家: " + sb.toString();
    }

    // === 战斗功能: 野生宝可梦 ===
    public void addWildPokemon(PocketMon pm, double chance) {
        // 简化版：直接存进去，几率逻辑在 ClientHandler 处理
        wildPokemons.add(pm);
    }

    public PocketMon getRandomWildPokemon() {
        if (wildPokemons.isEmpty()) return null;
        // 随机返回一只
        return wildPokemons.get(new Random().nextInt(wildPokemons.size()));
    }
}