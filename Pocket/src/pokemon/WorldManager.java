package pokemon;

import java.util.*;

public class WorldManager {
    private static Map<String, Room> rooms = new HashMap<>();

    static {
        createRooms();
        setupRoomConnections();
    }

    private static void createRooms() {
        Room home = new Room(
                "home",
                "家",
                "你在自己的房间里，窗外阳光明媚。\n" +
                        "向北是 1号道路(north)，向东是训练镇(east)。"
        );
        rooms.put("home", home);

        Room trainingTown = new Room(
                "training_town", "训练镇",
                "这里聚集了许多训练家，是专门练习战斗的地方。\n" +
                        "这里不会随机遇到野生宝可梦。\n" +
                        "提示：你可以输入 train 来训练。\n"+
                        "向西回家(west)。"
        );
        rooms.put("training_town", trainingTown);

        Room route1 = new Room(
                "route_1",
                "1号道路",
                "道路两旁有草丛，可能会遇到野生宝可梦。\n" +
                        "向南回家(south)，向北是常青市(north)，向东是常青森林(east)，向西是交易市场(west)。"
        );
        route1.addWildPokemon(new PocketMon("波波", PocketMon.Type.FLYING, 2), 1.0);
        route1.addWildPokemon(new PocketMon("小拉达", PocketMon.Type.NORMAL, 2), 1.0);
        rooms.put("route_1", route1);

        Room viridianForest = new Room(
                "viridian_forest",
                "常青森林",
                "茂密的森林，阳光透过树叶洒下斑驳的光点。这里是虫系宝可梦的天堂。\n" +
                        "向西回1号道路(west)。"
        );
        viridianForest.addWildPokemon(new PocketMon("绿毛虫", PocketMon.Type.BUG, 2), 1.0);
        viridianForest.addWildPokemon(new PocketMon("独角虫", PocketMon.Type.BUG, 2), 1.0);
        viridianForest.addWildPokemon(new PocketMon("波波", PocketMon.Type.FLYING, 3), 1.0);
        rooms.put("viridian_forest", viridianForest);

        Room viridianCity = new Room(
                "viridian_city",
                "常青市",
                "一个繁华的城市。这里有各种便利设施。\n" +
                        "南边回1号道路(south)，东边是宝可梦中心(east)，\n" +
                        "西边是友好商店(west)，北边是打工场所(north)。"
        );
        rooms.put("viridian_city", viridianCity);

        Room pokemonCenter = new Room(
                "pokemon_center",
                "宝可梦中心",
                "乔伊小姐微笑着站在柜台后。这里可以免费治疗宝可梦。\n" +
                        "提示：你可以输入 heal 来治疗。\n" +
                        "向西回常青市(west)。"
        );
        rooms.put("pokemon_center", pokemonCenter);

        Room pokemart = new Room(
                "pokemart",
                "友好商店",
                "货架上摆满了各种道具。\n" +
                        "提示：你可以输入 shop 查看商品，buy [物品名] [数量]购买。\n" +
                        "向东回常青市(east)。"
        );
        rooms.put("pokemart", pokemart);

        Room workPlace = new Room(
                "work_place",
                "打工场所",
                "这里可以通过工作赚取金币。\n" +
                        "提示：你可以输入 work 开始打工。\n" +
                        "向南回常青市(south)。"
        );
        rooms.put("work_place", workPlace);

        Room market = new Room(
                "market",
                "交易市场",
                "这里是宝可梦世界的交易中心，摊位林立，玩家们在此自由买卖各种物品。\n" +
                        "提示：输入 market stall 创建摊位，market sell [物品名] [数量] [价格] 上架物品，\n" +
                        "market buy [物品名] [数量] [价格] 购买物品，market list 查看所有在售物品。\n" +
                        "向东回1号道路(east)。"
        );
        rooms.put("market", market);

    }

    private static void setupRoomConnections() {
        rooms.get("home").addExit("north", "route_1");
        rooms.get("route_1").addExit("south", "home");

        rooms.get("home").addExit("east", "training_town");
        rooms.get("training_town").addExit("west", "home");

        rooms.get("route_1").addExit("north", "viridian_city");
        rooms.get("viridian_city").addExit("south", "route_1");

        rooms.get("route_1").addExit("east", "viridian_forest");
        rooms.get("viridian_forest").addExit("west", "route_1");

        rooms.get("route_1").addExit("west", "market");
        rooms.get("market").addExit("east", "route_1");

        rooms.get("viridian_city").addExit("east", "pokemon_center");
        rooms.get("pokemon_center").addExit("west", "viridian_city");

        rooms.get("viridian_city").addExit("west", "pokemart");
        rooms.get("pokemart").addExit("east", "viridian_city");

        rooms.get("viridian_city").addExit("north", "work_place");
        rooms.get("work_place").addExit("south", "viridian_city");
    }

    public static Room getStartRoom() {
        return rooms.get("home");
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }


    private static final String HORIZ_CONNECT = "──";
    private static final int GAP = 1;

    private static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c <= 0x7F) ? 1 : 2;
        }
        return w;
    }

    private static String repeat(char ch, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ch);
        return sb.toString();
    }

    private static String spaces(int n) {
        return repeat(' ', n);
    }

    private static String padRightByDisplayWidth(String s, int targetWidth) {
        int w = displayWidth(s);
        if (w >= targetWidth) return s;
        return s + repeat(' ', targetWidth - w);
    }


    private static int computeCellW(String currentRoomId) {
        String[] ids = {
                "work_place", "pokemart", "viridian_city", "pokemon_center",
                "route_1", "viridian_forest", "home", "training_town"
        };
        int max = 0;
        for (String id : ids) {
            String name = rooms.get(id).getName();
            String mark = id.equals(currentRoomId) ? "▶ " : "  ";
            String text = mark + name + "(" + id + ")";
            max = Math.max(max, displayWidth(text));
        }
        int w = max + 2;
        return Math.min(w, 28);
    }

    private static String cell(String currentRoomId, String roomId, String roomName, int cellW) {
        String mark = roomId.equals(currentRoomId) ? "▶ " : "  ";
        String text = mark + roomName + "(" + roomId + ")";
        return padRightByDisplayWidth(text, cellW);
    }

    public static String getAsciiMap(String currentRoomId) {
        int cellW = computeCellW(currentRoomId);

        String cWork = cell(currentRoomId, "work_place", "打工场所", cellW);
        String cMart = cell(currentRoomId, "pokemart", "友好商店", cellW);
        String cCity = cell(currentRoomId, "viridian_city", "常青市", cellW);
        String cCenter = cell(currentRoomId, "pokemon_center", "宝可梦中心", cellW);
        String cMarket = cell(currentRoomId, "market", "交易市场", cellW);
        String cRoute1 = cell(currentRoomId, "route_1", "1号道路", cellW);
        String cForest = cell(currentRoomId, "viridian_forest", "常青森林", cellW);
        String cHome = cell(currentRoomId, "home", "家", cellW);
        String cTrain = cell(currentRoomId, "training_town", "训练镇", cellW);

        String conn = spaces(GAP) + HORIZ_CONNECT + spaces(GAP);

        int midStart = cellW + conn.length();

        int midAxis = midStart + (cellW / 2);

        String vLine = spaces(midAxis) + "│";

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════════════════════════════\n");
        sb.append("                                        世界地图                             \n");
        sb.append("═══════════════════════════════════════════════════════════════════════════════════════\n\n");

        sb.append(spaces(midStart)).append(cWork).append("\n");
        sb.append(vLine).append("\n");

        sb.append(cMart).append(conn).append(cCity).append(conn).append(cCenter).append("\n");
        sb.append(vLine).append("\n");

        sb.append(cMarket).append(conn).append(cRoute1).append(conn).append(cForest).append("\n");
        sb.append(vLine).append("\n");

        int homeShift = 6;

        sb.append(spaces(midStart)).append(spaces(homeShift)).append(cHome).append(spaces(Math.max(0, conn.length() - homeShift))).append("——").append(cTrain).append("\n\n");

        sb.append("═══════════════════════════════════════════════════════════════════════════════════════\n");
        sb.append("图例: ▶ 当前位置\n");
        return sb.toString();
    }
}