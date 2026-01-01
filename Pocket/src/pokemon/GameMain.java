// package pokemon;

// import java.util.Scanner;

// public class GameMain {
//     private WorldManager world;
//     private Player player;
//     private Scanner scanner;
//     private boolean gameRunning;

//     public GameMain() {
//         this.world = new WorldManager();
//         this.scanner = new Scanner(System.in);
//         this.gameRunning = true;
//     }

//     public void startGame() {
//         showGameIntroduction();
//         initializePlayer();

//         System.out.println("\n" + world.getCurrentRoomInfo());

//         while (gameRunning) {
//             System.out.print("\n> ");
//             String input = scanner.nextLine().trim().toLowerCase();

//             if (input.equals("quit") || input.equals("exit")) {
//                 System.out.println("再见！期待再次冒险！");
//                 break;
//             }

//             processCommand(input);
//         }

//         scanner.close();
//     }

//     private void showGameIntroduction() {
//         System.out.println("=== 宝可梦文字MUD游戏 ===");
//         sleep(1000);
//         System.out.println("\n欢迎来到宝可梦世界！这是一个充满冒险和友谊的世界。");
//         sleep(1200);
//         showHelp();
//         sleep(1000);
//         System.out.println("\n按下回车键开始你的冒险...");
//         scanner.nextLine();
//     }

//     private void initializePlayer() {
//         System.out.println("\n【真新镇 - 你的房间】");
//         sleep(800);
//         System.out.println("阳光透过窗户洒在地毯上。墙上贴着各种宝可梦海报，书包已经收拾好放在床边。");
//         sleep(1200);
//         System.out.println("妈妈在楼下喊道：\"快点，大木博士在等你！\"");
//         sleep(1000);

//         System.out.print("\n请输入你的训练家名字: ");
//         String playerName = scanner.nextLine().trim();
//         if (playerName.isEmpty()) playerName = "小智";

//         this.player = new Player(playerName);
//         sleep(800);
//         System.out.println("\n妈妈：\"终于下来了！这是给你的便当。大木博士说另外两个孩子已经到了，快去吧！\"");
//         sleep(1500);

//         System.out.println("\n你冲出家门，沿着小路奔跑...");
//         sleep(1200);
//         System.out.println("按下回车键继续...");
//         scanner.nextLine();

//         chooseStarterPokemon();
//     }

//     private void chooseStarterPokemon() {
//         System.out.println("\n【大木博士研究所】");
//         sleep(800);
//         System.out.println("你气喘吁吁地跑进研究所，看到大木博士和另外两个训练家站在实验台前。");
//         sleep(1500);
//         System.out.println("大木博士：\"啊，" + player.getName() + "！你来了！我们刚刚开始。来吧，选择你的伙伴。\"");
//         sleep(1500);

//         System.out.println("\n实验台上放着三个精灵球：");
//         sleep(800);
//         System.out.println("? 【小火龙】- 火焰宝可梦，性格勇敢，擅长特殊攻击");
//         sleep(1000);
//         System.out.println("? 【杰尼龟】- 龟甲宝可梦，性格顽皮，防御出众");
//         sleep(1000);
//         System.out.println("? 【妙蛙种子】- 种子宝可梦，性格冷静，能力均衡");
//         sleep(1000);
//         System.out.println("\n其他两个训练家期待地看着你，其中一个已经伸手向小火龙...");
//         sleep(1500);

//         boolean validChoice = false;
//         while (!validChoice) {
//             System.out.print("\n请选择你的伙伴（输入 妙蛙种子/杰尼龟/小火龙）: ");
//             String choice = scanner.nextLine().trim();

//             switch (choice) {
//                 case "妙蛙种子":
//                     System.out.println("\n你坚定地指向妙蛙种子：\"我选择它！\"");
//                     sleep(1000);
//                     System.out.println("妙蛙种子跳出精灵球，亲昵地蹭了蹭你的腿。");
//                     sleep(1500);
//                     System.out.println("? 妙蛙种子加入了你的队伍！");
//                     sleep(1000);
//                     System.out.println("\n大木博士：\"出色的选择！妙蛙种子是值得信赖的伙伴。\"");
//                     sleep(1500);
//                     PocketMon baseurl = new PocketMon("妙蛙种子", PocketMon.Type.GRASS, 5);
//                     player.setStarterPokemon(baseurl);
//                     validChoice = true;
//                     break;
//                 case "杰尼龟":
//                     System.out.println("\n你选择了杰尼龟，但是另一个训练家抢先拿走了它...");
//                     sleep(1500);
//                     System.out.println("大木博士：\"抱歉，杰尼龟已经被选走了。请重新选择。\"");
//                     sleep(1200);
//                     break;
//                 case "小火龙":
//                     System.out.println("\n你选择了小火龙，但是另一个训练家抢先拿走了它...");
//                     sleep(1500);
//                     System.out.println("大木博士：\"抱歉，小火龙已经被选走了。请重新选择。\"");
//                     sleep(1200);
//                     break;
//                 default:
//                     System.out.println("请选择正确的宝可梦！");
//                     sleep(800);
//                     break;
//             }
//         }

//         System.out.println("\n大木博士递给你：");
//         sleep(800);
//         System.out.println("- ? 宝可梦图鉴（已激活）");
//         sleep(600);
//         System.out.println("- ? 精灵球 x5（追加）");
//         sleep(600);
//         System.out.println("- ? 伤药 x3");
//         sleep(600);
//         System.out.println("- ? 1000元启动资金");
//         sleep(1000);
//         System.out.println("\n大木博士：\"现在开始你们的冒险吧！记住，要填满图鉴，成为宝可梦大师！\"");
//         sleep(2000);

//         System.out.println("\n按下回车键开始冒险...");
//         scanner.nextLine();

//         world.movePlayer("east");
//         world.movePlayer("east");
//     }

//     private void processCommand(String input) {
//         switch (input) {
//             case "n": case "north": world.movePlayer("north"); showCurrentLocation(); break;
//             case "s": case "south": world.movePlayer("south"); showCurrentLocation(); break;
//             case "e": case "east": world.movePlayer("east"); showCurrentLocation(); break;
//             case "w": case "west": world.movePlayer("west"); showCurrentLocation(); break;
//             case "look": showCurrentLocation(); break;
//             case "map": world.showMap(); break;
//             case "status": player.showStatus(); break;
//             case "bag": player.showBag(); break;
//             case "heal": player.healTeam(); break;
//             case "battle": startWildBattle(); break;
//             case "shop": showShop(); break;
//             case "work": player.work(); break;
//             case "help": showHelp(); break;
//             default:
//                 if (input.startsWith("use ")) {
//                     String itemName = input.substring(4).trim();
//                     player.useItem(itemName);
//                 } else if (input.startsWith("buy ")) {
//                     String itemName = input.substring(4).trim();
//                     buyItem(itemName);
//                 } else {
//                     System.out.println("未知命令。输入 'help' 查看帮助。");
//                 }
//                 break;
//         }
//     }

//     private void showCurrentLocation() {
//         System.out.println("\n" + world.getCurrentRoomInfo());
//     }

//     private void showShop() {
//         System.out.println("\n=== 友好商店 ===");
//         sleep(500);
//         System.out.println("欢迎！这里有各种宝可梦道具：");
//         sleep(500);
//         System.out.println("1. 伤药 - 恢复20HP | 价格: 200元");
//         sleep(300);
//         System.out.println("2. 好伤药 - 恢复50HP | 价格: 500元");
//         sleep(300);
//         System.out.println("3. 精灵球 - 捕捉宝可梦 | 价格: 200元");
//         sleep(300);
//         System.out.println("4. 经验糖果 - 获得100经验 | 价格: 300元");
//         sleep(300);
//         System.out.println("5. 攻击强化剂 - 暂时提升攻击 | 价格: 400元");
//         sleep(300);
//         System.out.println("6. 防御强化剂 - 暂时提升防御 | 价格: 400元");  // 新增
//         sleep(500);
//         System.out.println("\n使用 'buy [道具名]' 命令购买道具");
//         System.out.println("你的金钱: " + player.getMoney() + "元");
//     }

//     private void buyItem(String itemName) {
//         switch (itemName) {
//             case "伤药": player.buyItem("伤药", 200); break;
//             case "好伤药": player.buyItem("好伤药", 500); break;
//             case "精灵球": player.buyItem("精灵球", 200); break;
//             case "经验糖果": player.buyItem("经验糖果", 300); break;
//             case "攻击强化剂": player.buyItem("攻击强化剂", 400); break;
//             case "防御强化剂": player.buyItem("防御强化剂", 400); break;  // 新增
//             default: System.out.println("没有这个商品！"); break;
//         }
//     }

//     private void startWildBattle() {
//         System.out.println("正在寻找野生宝可梦...");
//         sleep(1000);
//         PocketMon wildPokemon = world.getRandomWildPokemon();
//         if (wildPokemon != null) {
//             BattleSystem battle = new BattleSystem(player, wildPokemon);
//             battle.startBattle();
//         } else {
//             System.out.println("这里没有野生宝可梦。");
//             sleep(800);
//             System.out.println("提示：只有在野外区域才能遇到野生宝可梦。");
//             sleep(800);
//             System.out.println("有野生宝可梦的区域：1号道路(north)、常青森林(south)");
//         }
//     }

//     private void showHelp() {
//         System.out.println("\n=== 游戏命令指引 ===");
//         sleep(800);
//         System.out.println("??♂? 移动命令:");
//         sleep(600);
//         System.out.println("  north(n)     - 向北移动");
//         sleep(400);
//         System.out.println("  south(s)     - 向南移动");
//         sleep(400);
//         System.out.println("  east(e)      - 向东移动");
//         sleep(400);
//         System.out.println("  west(w)      - 向西移动");
//         sleep(600);
//         System.out.println("\n? 状态命令:");
//         sleep(600);
//         System.out.println("  status       - 查看训练家和宝可梦状态");
//         sleep(400);
//         System.out.println("  bag          - 查看背包物品");
//         sleep(400);
//         System.out.println("  look         - 查看当前位置详情");
//         sleep(400);
//         System.out.println("  map          - 查看当前地图");
//         sleep(600);
//         System.out.println("\n?? 战斗命令:");
//         sleep(600);
//         System.out.println("  battle       - 与野生宝可梦战斗（在野外区域）");
//         sleep(400);
//         System.out.println("  heal         - 恢复所有宝可梦的HP");
//         sleep(600);
//         System.out.println("\n? 商店命令:");
//         sleep(600);
//         System.out.println("  shop         - 查看商店商品");
//         sleep(400);
//         System.out.println("  buy [道具名] - 购买指定道具");
//         sleep(600);
//         System.out.println("\n? 经济命令:");
//         sleep(600);
//         System.out.println("  work         - 打工赚钱");
//         sleep(400);
//         System.out.println("\n? 道具命令:");
//         sleep(600);
//         System.out.println("  use [道具名] - 使用指定道具");
//         sleep(600);
//         System.out.println("\n? 其他命令:");
//         sleep(600);
//         System.out.println("  help         - 显示此帮助信息");
//         sleep(400);
//         System.out.println("  quit         - 退出游戏");
//     }

//     private void sleep(int milliseconds) {
//         try {
//             Thread.sleep(milliseconds);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }
//     }

//     public static void main(String[] args) {
//         GameMain game = new GameMain();
//         game.startGame();
//     }
// }
