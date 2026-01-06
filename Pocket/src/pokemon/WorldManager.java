package pokemon;

import java.util.*;

public class WorldManager {
    private static Map<String, Room> rooms = new HashMap<>();

    static {
        createRooms();
        setupRoomConnections();
    }

    private static void createRooms() {
        // 家
        rooms.put("home", new Room("home", "真新镇 - 你的家",
                "这是你的房间，阳光透过窗户洒进来。墙上贴着宝可梦海报。\n楼下通往 east。"));

        Room home = new Room(
                "home",
                "家",
                "你在自己的房间里，窗外阳光明媚。\n" +
                        "向北是 1号道路(north)，向东是训练镇(east)。"
        );
        rooms.put("home", home);

        // 训练镇：专门练习战斗的地方，不放野怪
        Room trainingTown = new Room(
                "training_town", "训练镇",
                "这里聚集了许多训练家，是专门练习战斗的地方。\n" +
                        "这里不会随机遇到野生宝可梦。\n" +
                        "向西回家(west)。"
        );
        rooms.put("training_town", trainingTown);

        Room route1 = new Room(
                "route_1",
                "1号道路",
                "道路两旁有草丛，可能会遇到野生宝可梦。\n" +
                        "向南回家(south)，向北是常青市(north)，向东是常青森林(east)。"
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
                        "提示：你可以输入 shop 查看商品，buy [物品名] 购买。\n" +
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


    private static final int CELL_W = 28;
    private static final int GAP = 3;

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

    private static String padRightByDisplayWidth(String s, int targetWidth) {
        int w = displayWidth(s);
        if (w >= targetWidth) return s;
        return s + repeat(' ', targetWidth - w);
    }

    private static String cell(String currentRoomId, String roomId, String roomName) {
        String mark = roomId.equals(currentRoomId) ? ">>" : "  ";
        String text = mark + roomName + "(" + roomId + ")";
        return padRightByDisplayWidth(text, CELL_W);
    }

    private static String row3(String c0, String c1, String c2) {
        return c0 + repeat(' ', GAP) + c1 + repeat(' ', GAP) + c2;
    }

    private static String vLineAtCol1() {
        int col1Start = CELL_W + GAP;
        int x = col1Start + CELL_W / 2;
        return repeat(' ', x) + "|";
    }

    public static String getAsciiMap(String currentRoomId) {

        String cHome   = cell(currentRoomId, "home", "家");
        String cTrain  = cell(currentRoomId, "training_town", "训练镇");
        String cRoute1 = cell(currentRoomId, "route_1", "1号道路");
        String cForest = cell(currentRoomId, "viridian_forest", "常青森林");
        String cCity   = cell(currentRoomId, "viridian_city", "常青市");
        String cCenter = cell(currentRoomId, "pokemon_center", "宝可梦中心");
        String cMart   = cell(currentRoomId, "pokemart", "友好商店");
        String cWork   = cell(currentRoomId, "work_place", "打工场所");
        String blank   = padRightByDisplayWidth("", CELL_W);

        StringBuilder sb = new StringBuilder();
        sb.append("=========== 世界地图 ===========\n");

        // 顶部：work_place 在中间列
        sb.append(row3(blank, cWork, blank)).append("\n");
        sb.append(vLineAtCol1()).append("\n");

        sb.append(row3(cMart, cCity, cCenter)).append("\n");
        sb.append(vLineAtCol1()).append("\n");

        sb.append(row3(blank, cRoute1, cForest)).append("\n");

        int col0Center = CELL_W / 2;
        int col1Start = CELL_W + GAP;
        int col1Center = col1Start + CELL_W / 2;
        sb.append(repeat(' ', col0Center)).append("|")
                .append(repeat(' ', col1Center - col0Center - 1)).append("|")
                .append("\n");

        sb.append(row3(cHome, cTrain, blank)).append("\n");

        sb.append("================================\n");
        return sb.toString();
    }
}