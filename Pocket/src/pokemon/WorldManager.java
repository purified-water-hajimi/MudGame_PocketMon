package pokemon;

import java.util.HashMap;
import java.util.Map;

public class WorldManager {

    // 共享地图数据（多线程/多玩家共用同一张地图）
    private static final Map<String, Room> rooms = new HashMap<>();

    // 静态初始化
    static {
        createRooms();
        setupRoomConnections();
    }

    // 1) 创建房间
    private static void createRooms() {
        // 家（起点）
        Room home = new Room(
                "home",
                "家",
                "你在自己的房间里，窗外阳光明媚。\n" +
                        "向北是 1号道路(north)，向东是训练镇(east)。"
        );
        rooms.put("home", home);

        // 训练镇：专门练习打斗的地方，不放野怪
        Room trainingTown = new Room(
                "training_town",
                "训练镇",
                "这里聚集了许多训练家，是专门练习战斗的地方。\n" +
                        "这里不会随机遇到野生宝可梦。\n" +
                        "向西回家(west)。"
        );
        rooms.put("training_town", trainingTown);

        // 1号道路
        Room route1 = new Room(
                "route_1",
                "1号道路",
                "道路两旁有草丛，可能会遇到野生宝可梦。\n" +
                        "向南回家(south)，向北是常青市(north)，向东是常青森林(east)。"
        );
        route1.addWildPokemon(new PocketMon("波波", PocketMon.Type.FLYING, 2), 1.0);
        route1.addWildPokemon(new PocketMon("小拉达", PocketMon.Type.NORMAL, 2), 1.0);
        rooms.put("route_1", route1);

        // 常青森林
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

        // 常青市
        Room viridianCity = new Room(
                "viridian_city",
                "常青市",
                "一个繁华的城市。这里有各种便利设施。\n" +
                        "南边回1号道路(south)，东边是宝可梦中心(east)，\n" +
                        "西边是友好商店(west)，北边是打工场所(north)。"
        );
        rooms.put("viridian_city", viridianCity);

        // 宝可梦中心
        Room pokemonCenter = new Room(
                "pokemon_center",
                "宝可梦中心",
                "乔伊小姐微笑着站在柜台后。这里可以免费治疗宝可梦。\n" +
                        "提示：你可以输入 heal 来治疗。\n" +
                        "向西回常青市(west)。"
        );
        rooms.put("pokemon_center", pokemonCenter);

        // 商店
        Room pokemart = new Room(
                "pokemart",
                "友好商店",
                "货架上摆满了各种道具。\n" +
                        "提示：你可以输入 shop 查看商品，buy [物品名] 购买。\n" +
                        "向东回常青市(east)。"
        );
        rooms.put("pokemart", pokemart);

        // 打工场所
        Room workPlace = new Room(
                "work_place",
                "打工场所",
                "这里可以通过工作赚取金币。\n" +
                        "提示：你可以输入 work 开始打工。\n" +
                        "向南回常青市(south)。"
        );
        rooms.put("work_place", workPlace);
    }

    // 2) 房间连接
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

    // ===================== 地图渲染（紧凑 + 正确对齐版） =====================

    // 横向连接线（更紧凑可改成 "─"；更显眼可改成 "───"）
    private static final String HORIZ_CONNECT = "──";
    // 连接线两侧空格（更紧凑设 0；更清晰设 1）
    private static final int GAP = 1;

    // 计算显示宽度（ASCII=1，全角/中文=2）
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

    // 动态计算每个单元格宽度：避免 CELL_W 固定过大导致“空太大”
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
        // +2 留边距；并给一个上限，避免名字过长把地图撑爆（按需调）
        int w = max + 2;
        return Math.min(w, 28);
    }

    private static String cell(String currentRoomId, String roomId, String roomName, int cellW) {
        String mark = roomId.equals(currentRoomId) ? "▶ " : "  ";
        String text = mark + roomName + "(" + roomId + ")";
        return padRightByDisplayWidth(text, cellW);
    }

    /**
     * 紧凑 + 对齐正确的地图：
     *
     *                 打工场所
     *                    │
     *   友好商店 ─ 常青市 ─ 宝可梦中心
     *                    │
     *                 1号道路 ─ 常青森林
     *                    │
     *                  家 ─ 训练镇
     */
    public static String getAsciiMap(String currentRoomId) {
        int cellW = computeCellW(currentRoomId);

        String cWork   = cell(currentRoomId, "work_place", "打工场所", cellW);
        String cMart   = cell(currentRoomId, "pokemart", "友好商店", cellW);
        String cCity   = cell(currentRoomId, "viridian_city", "常青市", cellW);
        String cCenter = cell(currentRoomId, "pokemon_center", "宝可梦中心", cellW);
        String cRoute1 = cell(currentRoomId, "route_1", "1号道路", cellW);
        String cForest = cell(currentRoomId, "viridian_forest", "常青森林", cellW);
        String cHome   = cell(currentRoomId, "home", "家", cellW);
        String cTrain  = cell(currentRoomId, "training_town", "训练镇", cellW);

        String conn = spaces(GAP) + HORIZ_CONNECT + spaces(GAP);

        // 中间格（常青市）起始列：左格 + conn
        int midStart = cellW + conn.length();

        // 竖线对齐到中间格中点（近似）
        int midAxis = midStart + (cellW / 2);

        String vLine = spaces(midAxis) + "│";

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════════════════════════════\n");
        sb.append("                                        世界地图                             \n");
        sb.append("═══════════════════════════════════════════════════════════════════════════════════════\n\n");

        // 打工场所（对齐到中间列起点）
        sb.append(spaces(midStart)).append(cWork).append("\n");
        sb.append(vLine).append("\n");

        // 友好商店 ─ 常青市 ─ 宝可梦中心
        sb.append(cMart).append(conn).append(cCity).append(conn).append(cCenter).append("\n");
        sb.append(vLine).append("\n");

        // 1号道路 ─ 常青森林（对齐到中间列起点）
        sb.append(spaces(midStart)).append(cRoute1).append(conn).append(cForest).append("\n");
        sb.append(vLine).append("\n");

        int homeShift = 6;

        // 家 ─ 训练镇（对齐到中间列起点）
        sb.append(spaces(midStart)).append(spaces(homeShift)).append(cHome).append(spaces(Math.max(0, conn.length()-homeShift))).append("——").append(cTrain).append("\n\n");

        sb.append("═══════════════════════════════════════════════════════════════════════════════════════\n");
        sb.append("图例: ▶ 当前位置\n");
        return sb.toString();
    }

    /**
     * 获取简化的文本地图，用于紧凑显示（保留你原来的写法也行）
     */
    public static String getSimpleMap(String currentRoomId) {
        return String.format(
                "世界地图 (当前位置: %s)\n" +
                        "  %s\n" +
                        "    ↓\n" +
                        "%s ← %s → %s\n" +
                        "    ↓\n" +
                        "  %s → %s\n" +
                        "    ↓\n" +
                        "  %s → %s\n",
                rooms.get(currentRoomId).getName(),
                rooms.get("work_place").getName(),
                rooms.get("pokemart").getName(),
                rooms.get("viridian_city").getName(),
                rooms.get("pokemon_center").getName(),
                rooms.get("route_1").getName(),
                rooms.get("viridian_forest").getName(),
                rooms.get("home").getName(),
                rooms.get("training_town").getName()
        );
    }
}
