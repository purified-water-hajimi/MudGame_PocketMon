package pokemon;

import java.util.*;

public class PocketMon {
    private static final int[] EXP_REQUIREMENTS = {
            0, 50, 100, 200, 350, 500, 700, 900, 1150, 1400,
            1700, 2000, 2350, 2700, 3100, 3500, 4000, 4500, 5000, 5500, 6000
    };

    private String name;
    private Type type;
    private int level;
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private int exp;
    private List<Skill> skills;

    public enum Type {
        GRASS, FIRE, WATER, ELECTRIC, BUG, FLYING, NORMAL
    }

    public PocketMon(String name, Type type, int level) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.skills = new Vector<>();
        calculateStats();
        this.currentHp = this.maxHp;
        this.exp = level > 1 ? EXP_REQUIREMENTS[level - 1] : 0;
        initializeSkills();
    }

    private void calculateStats() {
        this.maxHp = 30 + (level * 3);
        this.attack = 10 + level;
        this.defense = 10 + level;
    }

    private void initializeSkills() {
        switch (this.type) {
            case GRASS:
                skills.add(new Skill("撞击", Type.NORMAL, 40, 100, 35, Skill.SkillCategory.PHYSICAL, 1));
                skills.add(new Skill("藤鞭", Type.GRASS, 45, 100, 25, Skill.SkillCategory.PHYSICAL, 3));
                break;
            case BUG:
                skills.add(new Skill("撞击", Type.NORMAL, 40, 100, 35, Skill.SkillCategory.PHYSICAL, 1));
                skills.add(new Skill("吐丝", Type.BUG, 0, 95, 40, Skill.SkillCategory.STATUS, 1));
                break;
            case FLYING:
                skills.add(new Skill("撞击", Type.NORMAL, 40, 100, 35, Skill.SkillCategory.PHYSICAL, 1));
                skills.add(new Skill("起风", Type.FLYING, 40, 100, 35, Skill.SkillCategory.SPECIAL, 1));
                break;
            default:
                skills.add(new Skill("撞击", Type.NORMAL, 40, 100, 35, Skill.SkillCategory.PHYSICAL, 1));
                break;
        }
    }

    public void gainExp(int gainedExp) {
        if (level >= EXP_REQUIREMENTS.length) return;
        this.exp += gainedExp;
        System.out.println(name + " 获得了 " + gainedExp + " 经验值！");
        while (level < EXP_REQUIREMENTS.length && exp >= EXP_REQUIREMENTS[level]) {
            levelUp();
        }
    }

    private void levelUp() {
        this.level++;
        int oldMaxHp = maxHp;
        calculateStats();
        double hpRatio = (double) currentHp / oldMaxHp;
        currentHp = (int) (maxHp * hpRatio);
        if (currentHp < 1) currentHp = 1;
        System.out.println("? " + name + " 升到了 Lv." + level + "！");
    }

    public int getExpToNextLevel() {
        if (level >= EXP_REQUIREMENTS.length) return -1;
        return EXP_REQUIREMENTS[level] - exp;
    }

    public String getExpInfo() {
        int expToNext = getExpToNextLevel();
        if (expToNext == -1) return "? 已达到最高等级";
        else if (expToNext == 0) return "? 可以升级了！";
        else return "距离下一级还需: " + expToNext + "经验";
    }

    public void takeDamage(int damage) {
        currentHp -= damage;
        if (currentHp < 0) currentHp = 0;
    }

    public void heal(int amount) {
        currentHp = Math.min(currentHp + amount, maxHp);
        System.out.println(name + " 恢复了 " + amount + " HP！");
    }

    public void fullHeal() {
        currentHp = maxHp;
    }

    public boolean isFainted() {
        return currentHp <= 0;
    }

    public Skill getSkill(int index) {
        return (index >= 0 && index < skills.size()) ? skills.get(index) : null;
    }

    public String getBattleStatus() {
        return String.format("%s Lv.%d ??%d/%d", name, level, currentHp, maxHp);
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public int getLevel() { return level; }
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public List<Skill> getSkills() { return skills; }
}
