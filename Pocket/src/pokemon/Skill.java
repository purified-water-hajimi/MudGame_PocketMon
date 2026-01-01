package pokemon;

public class Skill {
    private String name;
    private PocketMon.Type type;
    private int power;
    private int accuracy;
    private int pp;
    private int maxPp;
    private SkillCategory category;

    public enum SkillCategory {
        PHYSICAL("物理"),
        SPECIAL("特殊"),
        STATUS("变化");

        private final String chineseName;

        SkillCategory(String chineseName) {
            this.chineseName = chineseName;
        }

        public String getChineseName() {
            return chineseName;
        }
    }

    public Skill(String name, PocketMon.Type type, int power, int accuracy,
                 int maxPp, SkillCategory category, int learnLevel) {
        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
        this.maxPp = maxPp;
        this.pp = maxPp;
        this.category = category;
    }

    public boolean use() {
        if (pp <= 0) return false;
        pp--;
        return true;
    }

    public boolean checkHit() {
        return Math.random() * 100 < accuracy;
    }

    public int calculateDamage(PocketMon attacker, PocketMon defender) {
        if (power == 0 || category == SkillCategory.STATUS) return 0;

        double effectiveness = calculateTypeEffectiveness(defender.getType());
        
        // ? 删除了 System.out.println，防止服务器刷屏
        if (effectiveness == 0) {
            return 0;
        }

        int damage = (int) ((power * attacker.getAttack() / defender.getDefense()) / 2.0);
        damage = (int) (damage * effectiveness);
        damage = Math.max(1, damage);

        return damage;
    }

    private double calculateTypeEffectiveness(PocketMon.Type defenderType) {
        // 简单的属性克制表
        switch (this.type) {
            case GRASS:
                if (defenderType == PocketMon.Type.WATER) return 2.0;
                if (defenderType == PocketMon.Type.FIRE) return 0.5;
                break;
            case FIRE:
                if (defenderType == PocketMon.Type.GRASS) return 2.0;
                if (defenderType == PocketMon.Type.WATER) return 0.5;
                break;
            case WATER:
                if (defenderType == PocketMon.Type.FIRE) return 2.0;
                if (defenderType == PocketMon.Type.GRASS) return 0.5;
                break;
            case FLYING:
                if (defenderType == PocketMon.Type.BUG) return 2.0;
                break;
            case ELECTRIC:
                if (defenderType == PocketMon.Type.FLYING) return 2.0;
                if (defenderType == PocketMon.Type.GRASS) return 0.5;
                break;
            case BUG:
                if (defenderType == PocketMon.Type.GRASS) return 2.0;
                break;
            case NORMAL:
                return 1.0;
            default:
                return 1.0;
        }
        return 1.0;
    }

    public String getName() { return name; }
    public PocketMon.Type getType() { return type; }
    public int getPower() { return power; }
    public int getPp() { return pp; }
    public int getMaxPp() { return maxPp; }
}