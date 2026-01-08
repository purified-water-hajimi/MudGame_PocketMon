package pokemon;

import java.util.*;

public class Room {
    private String id;
    private String name;
    private String description;
    private Map<String, String> exits;

    private Vector<Player> playersInRoom = new Vector<>();
    private List<PocketMon> wildPokemons = new ArrayList<>();

    public Room(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.exits = new HashMap<>();
    }

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
        if (direction == null || neighborId == null) return;

        String d = direction.trim().toLowerCase(Locale.ROOT);
        exits.put(d, neighborId);

        switch (d) {
            case "n":
                exits.put("north", neighborId);
                break;
            case "s":
                exits.put("south", neighborId);
                break;
            case "e":
                exits.put("east", neighborId);
                break;
            case "w":
                exits.put("west", neighborId);
                break;
            case "north":
                exits.put("n", neighborId);
                break;
            case "south":
                exits.put("s", neighborId);
                break;
            case "east":
                exits.put("e", neighborId);
                break;
            case "west":
                exits.put("w", neighborId);
                break;
            default:
                break;
        }
    }

    public String getExit(String direction) {
        if (direction == null) return null;
        return exits.get(direction.trim().toLowerCase(Locale.ROOT));
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

    public void addPlayer(Player p) {
        playersInRoom.add(p);
    }

    public void removePlayer(Player p) {
        playersInRoom.remove(p);
    }

    public String getPlayerNames(Player observer) {
        StringBuilder sb = new StringBuilder();
        for (Player p : getPlayersSnapshot()) {
            if (p != observer) {
                sb.append(p.getName()).append(" ");
            }
        }
        return sb.length() == 0 ? "身边没有人。" : "身边的玩家: " + sb.toString();
    }

    public List<Player> getPlayersSnapshot() {
        return new ArrayList<>(playersInRoom);
    }


    public void broadcastToRoom(String msg, Player exclude) {
        for (Player p : getPlayersSnapshot()) {
            if (exclude != null && p == exclude) continue;
            p.sendMessage(msg);
        }
    }


    public void addWildPokemon(PocketMon pm, double chance) {
        wildPokemons.add(pm);
    }

    public PocketMon getRandomWildPokemon(int playerLevel) {
        if (wildPokemons.isEmpty()) return null;

        PocketMon template =
                wildPokemons.get(new Random().nextInt(wildPokemons.size()));

        int delta = new Random().nextInt(3) - 1;
        int wildLevel = Math.max(1, playerLevel + delta);

        return new PocketMon(
                template.getName(),
                template.getType(),
                wildLevel
        );
    }



    public List<Player> getPlayersInRoom()
    {
        return new ArrayList
                <>(playersInRoom);
    }
}