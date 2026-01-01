package pokemon;

import java.util.*;

public class Room {
    private String name;
    private String description;
    private Map<String, String> exits;
    private List<WildPokemonInfo> wildPokemons;
    private Random random;

    private static class WildPokemonInfo {
        PocketMon pokemon;
        WildPokemonInfo(PocketMon pokemon, double probability) {
            this.pokemon = pokemon;
        }
    }

    public Room(String roomId, String name, String description) {
        this.name = name;
        this.description = description;
        this.exits = new HashMap<>();
        this.wildPokemons = new ArrayList<>();
        this.random = new Random();
    }

    public void addExit(String direction, String targetRoomId) {
        exits.put(direction.toLowerCase(), targetRoomId);
    }

    public String getExit(String direction) {
        return exits.get(direction.toLowerCase());
    }

    public void addWildPokemon(PocketMon pokemon, double probability) {
        wildPokemons.add(new WildPokemonInfo(pokemon, probability));
    }

    public PocketMon getRandomWildPokemon() {
        if (wildPokemons.isEmpty()) return null;
        int index = random.nextInt(wildPokemons.size());
        WildPokemonInfo info = wildPokemons.get(index);
        return new PocketMon(info.pokemon.getName(), info.pokemon.getType(), info.pokemon.getLevel());
    }

    public List<String> getAvailableExits() {
        return new ArrayList<>(exits.keySet());
    }

    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(name).append("】\n");
        sb.append(description).append("\n\n");

        if (exits.isEmpty()) sb.append("这里没有明显的出口。");
        else sb.append("出口: ").append(String.join("、", getAvailableExits()));

        if (!wildPokemons.isEmpty()) sb.append("\n\n🎯 这里有野生宝可梦！使用 'battle' 命令开始战斗。");
        else sb.append("\n\n🏠 这里很安全，没有野生宝可梦。");

        return sb.toString();
    }

    public String getName() { return name; }
}
