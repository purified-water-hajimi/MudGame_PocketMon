package pokemon;

import java.util.*;

public class WorldManager {
    // 1. 关键修改：改成 static，共享地图数据
    private static Map<String, Room> rooms = new HashMap<>();

    // 2. 静态初始化块
    static {
        createRooms();
        setupRoomConnections();
    }

    // 3. 创建房间
    private static void createRooms() {
        // 家
        rooms.put("home", new Room("home", "真新镇 - 你的家",
                "这是你的房间，阳光透过窗户洒进来。墙上贴着宝可梦海报。\n楼下通往 east。"));

        // 真新镇
        rooms.put("pallet_town", new Room("pallet_town", "真新镇",
                "一个宁静的小镇。空气中弥漫着青草的香气。\n西边是你家(west)，北边通往1号道路(north)，\n东边是大木博士的研究所(east)，南边通往常青森林(south)。"));

        // 1号道路
        Room route1 = new Room("route1", "1号道路",
                "连接真新镇和常青市的道路，两旁是茂盛的草丛。野生宝可梦在这里出没。\n南边回真新镇(south)，北边通往常青市(north)。");
        route1.addWildPokemon(new PocketMon("绿毛虫", PocketMon.Type.BUG, 3), 1.0);
        route1.addWildPokemon(new PocketMon("波波", PocketMon.Type.FLYING, 4), 1.0);
        route1.addWildPokemon(new PocketMon("小拉达", PocketMon.Type.NORMAL, 3), 1.0);
        rooms.put("route1", route1);

        // 大木研究所
        rooms.put("lab", new Room("lab", "大木研究所",
                "大木博士的研究所。各种研究设备摆放整齐，墙上挂着宝可梦的解剖图。\n西边回真新镇(west)。"));

        // 常青森林
        Room viridianForest = new Room("viridian_forest", "常青森林",
                "茂密的森林，阳光透过树叶洒下斑驳的光点。这里是虫系宝可梦的天堂。\n北边回真新镇(north)。");
        viridianForest.addWildPokemon(new PocketMon("绿毛虫", PocketMon.Type.BUG, 2), 1.0);
        viridianForest.addWildPokemon(new PocketMon("独角虫", PocketMon.Type.BUG, 2), 1.0);
        viridianForest.addWildPokemon(new PocketMon("波波", PocketMon.Type.FLYING, 3), 1.0);
        rooms.put("viridian_forest", viridianForest);

        // 常青市
        rooms.put("viridian_city", new Room("viridian_city", "常青市",
                "一个繁华的城市。这里有各种便利设施。\n南边回1号道路(south)，东边是宝可梦中心(east)，\n西边是友好商店(west)，北边是打工场所(north)。"));

        // 宝可梦中心
        rooms.put("pokemon_center", new Room("pokemon_center", "宝可梦中心",
                "乔伊小姐微笑着站在柜台后。这里可以免费治疗宝可梦。\n使用 'heal' 命令恢复所有宝可梦。\n西边回常青市(west)。"));

        // 友好商店
        rooms.put("pokemart", new Room("pokemart", "友好商店",
                "商店里摆满了各种宝可梦道具。\n使用 'shop' 命令查看商品，'buy [道具名]' 购买。\n东边回常青市(east)。"));

        // 打工场所
        rooms.put("work_place", new Room("work_place", "打工场所",
                "这里可以打工赚钱。使用 'work' 命令赚取金钱。\n南边回常青市(south)。"));
    }

    // 4. 设置连接
    private static void setupRoomConnections() {
        // 家
        rooms.get("home").addExit("east", "pallet_town");
        // 真新镇
        rooms.get("pallet_town").addExit("west", "home");
        rooms.get("pallet_town").addExit("north", "route1");
        rooms.get("pallet_town").addExit("east", "lab");
        rooms.get("pallet_town").addExit("south", "viridian_forest");
        // 1号道路
        rooms.get("route1").addExit("south", "pallet_town");
        rooms.get("route1").addExit("north", "viridian_city");
        // 研究所
        rooms.get("lab").addExit("west", "pallet_town");
        // 常青森林
        rooms.get("viridian_forest").addExit("north", "pallet_town");
        // 常青市
        rooms.get("viridian_city").addExit("south", "route1");
        rooms.get("viridian_city").addExit("east", "pokemon_center");
        rooms.get("viridian_city").addExit("west", "pokemart");
        rooms.get("viridian_city").addExit("north", "work_place");
        // 宝可梦中心
        rooms.get("pokemon_center").addExit("west", "viridian_city");
        // 友好商店
        rooms.get("pokemart").addExit("east", "viridian_city");
        // 打工场所
        rooms.get("work_place").addExit("south", "viridian_city");
    }

    // 5. 获取初始房间
    public static Room getStartRoom() {
        return rooms.get("home");
    }

    // 6. 根据ID获取房间
    public static Room getRoom(String id) {
        return rooms.get(id);
    }
}